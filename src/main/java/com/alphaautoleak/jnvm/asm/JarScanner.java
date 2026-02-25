package com.alphaautoleak.jnvm.asm;

import com.alphaautoleak.jnvm.config.ProtectConfig;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

/**
 * 扫描输入 JAR，收集所有需要保护的方法元数据。
 */
public class JarScanner {

    private final ProtectConfig config;

    /** 全局方法 ID 计数器 */
    private int nextMethodId = 0;

    /** 收集到的所有需要保护的方法 */
    private final List<MethodInfo> protectedMethods = new ArrayList<>();

    /** 记录哪些类包含被保护方法（后续 patch 用） */
    private final Set<String> affectedClasses = new HashSet<>();

    /** 注解规则的描述符列表 */
    private final List<String> annotationDescs;

    public JarScanner(ProtectConfig config) {
        this.config = config;
        this.annotationDescs = config.getAnnotationRules();
    }

    /**
     * 扫描 JAR 文件，返回所有需要保护的方法信息
     */
    public List<MethodInfo> scan(File jarFile) throws IOException {
        System.out.println("[SCAN] Opening JAR: " + jarFile.getAbsolutePath());

        try (JarFile jar = new JarFile(jarFile)) {
            Enumeration<JarEntry> entries = jar.entries();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();

                // 只处理 .class 文件
                if (!entry.getName().endsWith(".class")) {
                    continue;
                }

                // 跳过 module-info 和 package-info
                String entryName = entry.getName();
                if (entryName.equals("module-info.class") ||
                        entryName.endsWith("package-info.class")) {
                    continue;
                }

                try (InputStream is = jar.getInputStream(entry)) {
                    processClass(is);
                } catch (Exception e) {
                    System.err.println("[WARN] Failed to process: " + entryName + " - " + e.getMessage());
                }
            }
        }

        System.out.println("[SCAN] Found " + protectedMethods.size() + " methods to protect in "
                + affectedClasses.size() + " classes.");
        return protectedMethods;
    }

    /**
     * 处理单个 class 文件
     */
    private void processClass(InputStream classBytes) throws IOException {
        ClassReader cr = new ClassReader(classBytes);
        ClassNode cn = new ClassNode(Opcodes.ASM9);
        cr.accept(cn, 0); // 不跳过任何内容

        String className = cn.name; // 内部格式

        // 跳过接口（没有方法体）和合成类
        if ((cn.access & Opcodes.ACC_INTERFACE) != 0 &&
                (cn.access & Opcodes.ACC_ABSTRACT) != 0) {
            // 接口中可能有 default 方法，也需要检查
        }

        // 检查类级别注解
        boolean classAnnotated = false;
        if (!annotationDescs.isEmpty() && cn.visibleAnnotations != null) {
            for (AnnotationNode ann : cn.visibleAnnotations) {
                if (annotationDescs.contains(ann.desc)) {
                    classAnnotated = true;
                    break;
                }
            }
        }

        for (MethodNode mn : cn.methods) {
            // 跳过抽象方法和 native 方法（没有字节码）
            if ((mn.access & Opcodes.ACC_ABSTRACT) != 0 ||
                    (mn.access & Opcodes.ACC_NATIVE) != 0) {
                continue;
            }

            // 跳过没有指令的方法
            if (mn.instructions == null || mn.instructions.size() == 0) {
                continue;
            }

            // 跳过构造函数 (<init>) - 它们需要正确的 this 引用初始化
            if (mn.name.equals("<init>")) {
                continue;
            }

            // 跳过实例方法 - 只保护静态方法
            // 实例方法需要正确的 this 引用，原生调用会破坏它
            if ((mn.access & Opcodes.ACC_STATIC) == 0) {
                continue;
            }

            // 跳过 lambda 方法 (lambda$...) - 它们使用 invokedynamic
            if (mn.name.contains("lambda$")) {
                continue;
            }

            // 跳过包含 invokedynamic 的方法
            // invokedynamic 使用 bootstrap methods，原生调用对它的支持还不完善
            // 需要完整的 LambdaMetafactory.metafactory 支持（6个参数）
            boolean hasInvokeDynamic = false;
            if (mn.instructions != null) {
                for (Object insn : mn.instructions) {
                    if (insn instanceof InvokeDynamicInsnNode) {
                        hasInvokeDynamic = true;
                        break;
                    }
                }
            }
            if (hasInvokeDynamic) {
                continue;
            }

            // 跳过返回对象引用的方法
            // 原生调用可能会破坏对象引用，只保护 void 和基本类型返回的方法
            char returnType = mn.desc.charAt(mn.desc.indexOf(')') + 1);
            if (returnType == 'L' || returnType == '[') {
                // 返回对象或数组，跳过
                continue;
            }

            // 跳过接收对象参数的方法
            // 原生调用可能会破坏对象参数的引用
            boolean hasObjectParam = false;
            int paramEnd = mn.desc.indexOf(')');
            if (paramEnd > 0) {
                String params = mn.desc.substring(1, paramEnd); // skip '('
                for (int i = 0; i < params.length(); i++) {
                    char c = params.charAt(i);
                    if (c == 'L' || c == '[') {
                        hasObjectParam = true;
                        break;
                    }
                }
            }
            if (hasObjectParam) {
                continue;
            }

            // 判断是否需要保护
            boolean shouldProtect = false;

            // 规则匹配
            if (config.shouldProtect(className, mn.name)) {
                shouldProtect = true;
            }

            // 类级别注解
            if (classAnnotated) {
                shouldProtect = true;
            }

            // 方法级别注解
            if (!shouldProtect && !annotationDescs.isEmpty() && mn.visibleAnnotations != null) {
                for (AnnotationNode ann : mn.visibleAnnotations) {
                    if (annotationDescs.contains(ann.desc)) {
                        shouldProtect = true;
                        break;
                    }
                }
            }

            if (!shouldProtect) {
                continue;
            }

            // 收集方法信息
            MethodInfo info = extractMethodInfo(cn, mn);
            if (info != null) {
                protectedMethods.add(info);
                affectedClasses.add(className);
                System.out.println("  [+] " + info);
            }
        }
    }

    /**
     * 从 ASM MethodNode 提取完整的方法元数据
     */
    private MethodInfo extractMethodInfo(ClassNode cn, MethodNode mn) {
        MethodInfo info = new MethodInfo();
        info.setMethodId(nextMethodId++);
        info.setOwner(cn.name);
        info.setName(mn.name);
        info.setDescriptor(mn.desc);
        info.setAccess(mn.access);
        info.setMaxStack(mn.maxStack);
        info.setMaxLocals(mn.maxLocals);
        info.setSignature(mn.signature);

        // ===== 提取字节码 + 构建自定义常量池 =====
        BytecodeExtractor extractor = new BytecodeExtractor(cn, mn);
        extractor.extract();

        info.setBytecode(extractor.getBytecodeBytes());
        info.setConstantPool(extractor.getConstantPool());
        info.setExceptionTable(extractor.getExceptionTable());
        info.setBootstrapMethods(extractor.getBootstrapMethods());

        return info;
    }

    public Set<String> getAffectedClasses() {
        return affectedClasses;
    }
}