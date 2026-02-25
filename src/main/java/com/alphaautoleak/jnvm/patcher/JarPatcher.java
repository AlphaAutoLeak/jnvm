package com.alphaautoleak.jnvm.patcher;

import com.alphaautoleak.jnvm.asm.MethodInfo;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.io.*;
import java.util.*;
import java.util.jar.*;

/**
 * 将被保护方法的方法体重写为调用 VMBridge.execute()。
 *
 * 原始方法体:
 *   public int calc(int a, int b) { return a + b; }
 *
 * 重写后:
 *   public int calc(int a, int b) {
 *       Object[] args = new Object[] { Integer.valueOf(a), Integer.valueOf(b) };
 *       Object result = VMBridge.execute(METHOD_ID, this, args);
 *       return ((Integer)result).intValue();
 *   }
 */
public class JarPatcher {

    /** 被保护方法列表 */
    private final List<MethodInfo> protectedMethods;

    /** 受影响的类 */
    private final Set<String> affectedClasses;

    /** VMBridge 类的内部名 */
    private static final String BRIDGE_CLASS = "com/alphaautoleak/jnvm/runtime/VMBridge";

    /** execute 方法描述符 */
    private static final String EXECUTE_DESC =
            "(ILjava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;";

    /** 方法查找表: "owner.name.desc" → methodId */
    private final Map<String, Integer> methodIdMap = new HashMap<>();

    public JarPatcher(List<MethodInfo> protectedMethods, Set<String> affectedClasses) {
        this.protectedMethods = protectedMethods;
        this.affectedClasses = affectedClasses;

        for (MethodInfo m : protectedMethods) {
            String key = m.getOwner() + "." + m.getName() + "." + m.getDescriptor();
            methodIdMap.put(key, m.getMethodId());
        }
    }

    /**
     * 处理整个 JAR：读取 → patch → 写入
     */
    public void patch(File inputJar, File outputJar) throws IOException {
        System.out.println("[PATCH] Input:  " + inputJar);
        System.out.println("[PATCH] Output: " + outputJar);

        int patchedCount = 0;

        try (JarFile jar = new JarFile(inputJar);
             JarOutputStream jos = new JarOutputStream(
                     new FileOutputStream(outputJar), jar.getManifest())) {

            Enumeration<JarEntry> entries = jar.entries();
            Set<String> written = new HashSet<>();

            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();

                // 跳过 manifest（已通过构造函数写入）
                if (entry.getName().equalsIgnoreCase("META-INF/MANIFEST.MF")) {
                    continue;
                }

                try (InputStream is = jar.getInputStream(entry)) {
                    if (entry.getName().endsWith(".class") && isAffected(entry.getName())) {
                        // 需要 patch 的 class
                        byte[] original = readAll(is);
                        byte[] patched = patchClass(original);

                        JarEntry newEntry = new JarEntry(entry.getName());
                        jos.putNextEntry(newEntry);
                        jos.write(patched);
                        jos.closeEntry();
                        patchedCount++;

                    } else {
                        // 原样复制
                        JarEntry newEntry = new JarEntry(entry.getName());
                        if (!entry.isDirectory()) {
                            jos.putNextEntry(newEntry);
                            copyStream(is, jos);
                            jos.closeEntry();
                        } else {
                            jos.putNextEntry(newEntry);
                            jos.closeEntry();
                        }
                    }
                    written.add(entry.getName());
                }
            }

            // 注入 VMBridge.class
            String bridgePath = BRIDGE_CLASS + ".class";
            if (!written.contains(bridgePath)) {
                byte[] bridgeBytes = generateVMBridgeClass();
                JarEntry bridgeEntry = new JarEntry(bridgePath);
                jos.putNextEntry(bridgeEntry);
                jos.write(bridgeBytes);
                jos.closeEntry();
                System.out.println("[PATCH] Injected VMBridge.class");
            }
        }

        System.out.println("[PATCH] Patched " + patchedCount + " classes.");
    }

    /**
     * 判断 entry 是否对应受影响的类
     */
    private boolean isAffected(String entryName) {
        // entry: "com/example/MyClass.class" → "com/example/MyClass"
        String className = entryName.replace(".class", "");
        return affectedClasses.contains(className);
    }

    /**
     * Patch 单个 class：重写所有被保护方法的方法体
     */
    private byte[] patchClass(byte[] classBytes) {
        ClassReader cr = new ClassReader(classBytes);
        ClassNode cn = new ClassNode(Opcodes.ASM9);
        cr.accept(cn, 0);

        for (MethodNode mn : cn.methods) {
            String key = cn.name + "." + mn.name + "." + mn.desc;
            Integer methodId = methodIdMap.get(key);
            if (methodId == null) continue;

            // 重写方法体
            rewriteMethodBody(cn, mn, methodId);
        }

        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        cn.accept(cw);
        return cw.toByteArray();
    }

    /**
     * 重写单个方法体为调用 VMBridge.execute
     */
    private void rewriteMethodBody(ClassNode cn, MethodNode mn, int methodId) {
        boolean isStatic = (mn.access & Opcodes.ACC_STATIC) != 0;

        // 解析参数类型
        Type[] argTypes = Type.getArgumentTypes(mn.desc);
        Type retType = Type.getReturnType(mn.desc);

        InsnList insns = new InsnList();

        // 1. 压入 methodId
        insns.add(new LdcInsnNode(methodId));

        // 2. 压入 this 或 null
        if (isStatic) {
            insns.add(new InsnNode(Opcodes.ACONST_NULL));
        } else {
            insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
        }

        // 3. 创建参数数组 Object[]
        insns.add(new LdcInsnNode(argTypes.length));
        insns.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/Object"));

        int localIdx = isStatic ? 0 : 1;
        for (int i = 0; i < argTypes.length; i++) {
            insns.add(new InsnNode(Opcodes.DUP));
            insns.add(new LdcInsnNode(i));

            // 加载参数并装箱
            Type t = argTypes[i];
            loadAndBox(insns, t, localIdx);

            insns.add(new InsnNode(Opcodes.AASTORE));
            localIdx += t.getSize();
        }

        // 4. 调用 VMBridge.execute(methodId, instance, args)
        insns.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                BRIDGE_CLASS,
                "execute",
                EXECUTE_DESC,
                false));

        // 5. 处理返回值
        generateReturn(insns, retType);

        // 替换方法体
        mn.instructions.clear();
        mn.instructions.add(insns);

        // 清除异常表和局部变量表
        mn.tryCatchBlocks.clear();
        if (mn.localVariables != null) mn.localVariables.clear();

        // 重新计算
        mn.maxStack = 0;
        mn.maxLocals = 0;
    }

    /**
     * 加载局部变量并装箱为 Object
     */
    private void loadAndBox(InsnList insns, Type type, int localIdx) {
        switch (type.getSort()) {
            case Type.BOOLEAN:
                insns.add(new VarInsnNode(Opcodes.ILOAD, localIdx));
                insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                        "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false));
                break;
            case Type.BYTE:
                insns.add(new VarInsnNode(Opcodes.ILOAD, localIdx));
                insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                        "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false));
                break;
            case Type.CHAR:
                insns.add(new VarInsnNode(Opcodes.ILOAD, localIdx));
                insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                        "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false));
                break;
            case Type.SHORT:
                insns.add(new VarInsnNode(Opcodes.ILOAD, localIdx));
                insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                        "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false));
                break;
            case Type.INT:
                insns.add(new VarInsnNode(Opcodes.ILOAD, localIdx));
                insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                        "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false));
                break;
            case Type.LONG:
                insns.add(new VarInsnNode(Opcodes.LLOAD, localIdx));
                insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                        "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false));
                break;
            case Type.FLOAT:
                insns.add(new VarInsnNode(Opcodes.FLOAD, localIdx));
                insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                        "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false));
                break;
            case Type.DOUBLE:
                insns.add(new VarInsnNode(Opcodes.DLOAD, localIdx));
                insns.add(new MethodInsnNode(Opcodes.INVOKESTATIC,
                        "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false));
                break;
            case Type.OBJECT:
            case Type.ARRAY:
                insns.add(new VarInsnNode(Opcodes.ALOAD, localIdx));
                break;
            default:
                insns.add(new VarInsnNode(Opcodes.ALOAD, localIdx));
                break;
        }
    }

    /**
     * 生成返回指令（拆箱 Object 返回值）
     */
    private void generateReturn(InsnList insns, Type retType) {
        switch (retType.getSort()) {
            case Type.VOID:
                insns.add(new InsnNode(Opcodes.POP)); // 丢弃 null 返回值
                insns.add(new InsnNode(Opcodes.RETURN));
                break;
            case Type.BOOLEAN:
                insns.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Boolean"));
                insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                        "java/lang/Boolean", "booleanValue", "()Z", false));
                insns.add(new InsnNode(Opcodes.IRETURN));
                break;
            case Type.BYTE:
                insns.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Byte"));
                insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                        "java/lang/Byte", "byteValue", "()B", false));
                insns.add(new InsnNode(Opcodes.IRETURN));
                break;
            case Type.CHAR:
                insns.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Character"));
                insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                        "java/lang/Character", "charValue", "()C", false));
                insns.add(new InsnNode(Opcodes.IRETURN));
                break;
            case Type.SHORT:
                insns.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Short"));
                insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                        "java/lang/Short", "shortValue", "()S", false));
                insns.add(new InsnNode(Opcodes.IRETURN));
                break;
            case Type.INT:
                insns.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Integer"));
                insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                        "java/lang/Integer", "intValue", "()I", false));
                insns.add(new InsnNode(Opcodes.IRETURN));
                break;
            case Type.LONG:
                insns.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Long"));
                insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                        "java/lang/Long", "longValue", "()J", false));
                insns.add(new InsnNode(Opcodes.LRETURN));
                break;
            case Type.FLOAT:
                insns.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Float"));
                insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                        "java/lang/Float", "floatValue", "()F", false));
                insns.add(new InsnNode(Opcodes.FRETURN));
                break;
            case Type.DOUBLE:
                insns.add(new TypeInsnNode(Opcodes.CHECKCAST, "java/lang/Double"));
                insns.add(new MethodInsnNode(Opcodes.INVOKEVIRTUAL,
                        "java/lang/Double", "doubleValue", "()D", false));
                insns.add(new InsnNode(Opcodes.DRETURN));
                break;
            case Type.OBJECT:
            case Type.ARRAY:
                insns.add(new TypeInsnNode(Opcodes.CHECKCAST,
                        retType.getInternalName()));
                insns.add(new InsnNode(Opcodes.ARETURN));
                break;
            default:
                insns.add(new InsnNode(Opcodes.ARETURN));
                break;
        }
    }
    /**
     * 生成 VMBridge.class — 用 ASM 生成，包含完整的 native 加载逻辑
     */
    private byte[] generateVMBridgeClass() {
        ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);

        cw.visit(Opcodes.V1_8,
                Opcodes.ACC_PUBLIC | Opcodes.ACC_FINAL | Opcodes.ACC_SUPER,
                BRIDGE_CLASS,
                null,
                "java/lang/Object",
                null);

        // static native Object execute(int, Object, Object[])
        cw.visitMethod(
                Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC | Opcodes.ACC_NATIVE,
                "execute",
                EXECUTE_DESC,
                null,
                null).visitEnd();

        // static { loadNativeLibrary(); }
        // 简化版：直接在 clinit 中内联加载逻辑
        MethodVisitor mv = cw.visitMethod(
                Opcodes.ACC_STATIC,
                "<clinit>",
                "()V",
                null,
                null);
        mv.visitCode();

        /*
         * 生成等价于:
         *
         * static {
         *     try {
         *         System.loadLibrary("customvm");
         *     } catch (UnsatisfiedLinkError e) {
         *         // 从 JAR 提取
         *         String libName = detectLibName();
         *         String resPath = "META-INF/native/" + detectTarget() + "/" + libName;
         *         InputStream is = VMBridge.class.getClassLoader().getResourceAsStream(resPath);
         *         Path tmp = Files.createTempFile("jnvm-", libName);
         *         tmp.toFile().deleteOnExit();
         *         Files.copy(is, tmp, StandardCopyOption.REPLACE_EXISTING);
         *         is.close();
         *         System.load(tmp.toString());
         *     }
         * }
         *
         * 但 ASM 生成这么复杂的逻辑很冗长，
         * 所以我们使用一个 helper 方法。
         * 实际上更好的做法是直接把完整的 VMBridge.java 编译后嵌入。
         */

        // 简化：先 loadLibrary，失败后调用 extractAndLoad
        Label tryStart = new Label();
        Label tryEnd = new Label();
        Label catchHandler = new Label();
        Label end = new Label();

        mv.visitTryCatchBlock(tryStart, tryEnd, catchHandler, "java/lang/UnsatisfiedLinkError");

        // try { System.loadLibrary("customvm"); }
        mv.visitLabel(tryStart);
        mv.visitLdcInsn("customvm");
        mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                "java/lang/System", "loadLibrary",
                "(Ljava/lang/String;)V", false);
        mv.visitLabel(tryEnd);
        mv.visitJumpInsn(Opcodes.GOTO, end);

        // catch (UnsatisfiedLinkError e) { extractAndLoad(); }
        mv.visitLabel(catchHandler);
        mv.visitVarInsn(Opcodes.ASTORE, 0); // store exception

        // 调用 extractAndLoad 静态方法
        mv.visitMethodInsn(Opcodes.INVOKESTATIC,
                BRIDGE_CLASS, "extractAndLoad", "()V", false);

        mv.visitLabel(end);
        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        // private static void extractAndLoad()
        generateExtractAndLoadMethod(cw);

        // private static String detectTarget()
        generateDetectTargetMethod(cw);

        // private static String detectLibName()
        generateDetectLibNameMethod(cw);

        // private constructor
        mv = cw.visitMethod(Opcodes.ACC_PRIVATE, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL,
                "java/lang/Object", "<init>", "()V", false);
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/UnsupportedOperationException");
        mv.visitInsn(Opcodes.DUP);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL,
                "java/lang/UnsupportedOperationException", "<init>", "()V", false);
        mv.visitInsn(Opcodes.ATHROW);
        mv.visitMaxs(2, 1);
        mv.visitEnd();

        cw.visitEnd();
        return cw.toByteArray();
    }

    private void generateExtractAndLoadMethod(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(
                Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                "extractAndLoad", "()V", null, null);
        mv.visitCode();

        /*
         * String target = detectTarget();
         * String libName = detectLibName();
         * String resPath = "META-INF/native/" + target + "/" + libName;
         * InputStream is = VMBridge.class.getClassLoader().getResourceAsStream(resPath);
         * if (is == null) throw new UnsatisfiedLinkError(...);
         * Path tmp = Files.createTempDirectory("jnvm-");
         * File tmpFile = new File(tmp.toFile(), libName);
         * tmpFile.deleteOnExit();
         * tmp.toFile().deleteOnExit();
         * FileOutputStream fos = new FileOutputStream(tmpFile);
         * byte[] buf = new byte[8192];
         * int n;
         * while ((n = is.read(buf)) != -1) fos.write(buf, 0, n);
         * fos.close();
         * is.close();
         * System.load(tmpFile.getAbsolutePath());
         */

        // String target = detectTarget();
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, BRIDGE_CLASS,
                "detectTarget", "()Ljava/lang/String;", false);
        mv.visitVarInsn(Opcodes.ASTORE, 0); // target

        // String libName = detectLibName();
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, BRIDGE_CLASS,
                "detectLibName", "()Ljava/lang/String;", false);
        mv.visitVarInsn(Opcodes.ASTORE, 1); // libName

        // StringBuilder sb = new StringBuilder("META-INF/native/");
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
        mv.visitInsn(Opcodes.DUP);
        mv.visitLdcInsn("META-INF/native/");
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder",
                "<init>", "(Ljava/lang/String;)V", false);
        mv.visitVarInsn(Opcodes.ALOAD, 0); // target
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder",
                "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        mv.visitLdcInsn("/");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder",
                "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        mv.visitVarInsn(Opcodes.ALOAD, 1); // libName
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder",
                "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder",
                "toString", "()Ljava/lang/String;", false);
        mv.visitVarInsn(Opcodes.ASTORE, 2); // resPath

        // InputStream is = VMBridge.class.getClassLoader().getResourceAsStream(resPath);
        mv.visitLdcInsn(org.objectweb.asm.Type.getObjectType(BRIDGE_CLASS));
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/Class",
                "getClassLoader", "()Ljava/lang/ClassLoader;", false);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/ClassLoader",
                "getResourceAsStream", "(Ljava/lang/String;)Ljava/io/InputStream;", false);
        mv.visitVarInsn(Opcodes.ASTORE, 3); // is

        // if (is == null) throw new UnsatisfiedLinkError
        mv.visitVarInsn(Opcodes.ALOAD, 3);
        Label notNull = new Label();
        mv.visitJumpInsn(Opcodes.IFNONNULL, notNull);
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/UnsatisfiedLinkError");
        mv.visitInsn(Opcodes.DUP);
        // "Native lib not found: " + resPath
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
        mv.visitInsn(Opcodes.DUP);
        mv.visitLdcInsn("JNVM native library not found in JAR: ");
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder",
                "<init>", "(Ljava/lang/String;)V", false);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder",
                "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder",
                "toString", "()Ljava/lang/String;", false);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/UnsatisfiedLinkError",
                "<init>", "(Ljava/lang/String;)V", false);
        mv.visitInsn(Opcodes.ATHROW);

        mv.visitLabel(notNull);

        // File tmpDir = Files.createTempDirectory("jnvm-").toFile();
        mv.visitLdcInsn("jnvm-");
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitTypeInsn(Opcodes.ANEWARRAY, "java/nio/file/attribute/FileAttribute");
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/nio/file/Files",
                "createTempDirectory",
                "(Ljava/lang/String;[Ljava/nio/file/attribute/FileAttribute;)Ljava/nio/file/Path;",
                false);
        mv.visitMethodInsn(Opcodes.INVOKEINTERFACE, "java/nio/file/Path",
                "toFile", "()Ljava/io/File;", true);
        mv.visitVarInsn(Opcodes.ASTORE, 4); // tmpDir

        // tmpDir.deleteOnExit();
        mv.visitVarInsn(Opcodes.ALOAD, 4);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/File",
                "deleteOnExit", "()V", false);

        // File tmpFile = new File(tmpDir, libName);
        mv.visitTypeInsn(Opcodes.NEW, "java/io/File");
        mv.visitInsn(Opcodes.DUP);
        mv.visitVarInsn(Opcodes.ALOAD, 4);
        mv.visitVarInsn(Opcodes.ALOAD, 1); // libName
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/io/File",
                "<init>", "(Ljava/io/File;Ljava/lang/String;)V", false);
        mv.visitVarInsn(Opcodes.ASTORE, 5); // tmpFile

        // tmpFile.deleteOnExit();
        mv.visitVarInsn(Opcodes.ALOAD, 5);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/File",
                "deleteOnExit", "()V", false);

        // FileOutputStream fos = new FileOutputStream(tmpFile);
        mv.visitTypeInsn(Opcodes.NEW, "java/io/FileOutputStream");
        mv.visitInsn(Opcodes.DUP);
        mv.visitVarInsn(Opcodes.ALOAD, 5);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/io/FileOutputStream",
                "<init>", "(Ljava/io/File;)V", false);
        mv.visitVarInsn(Opcodes.ASTORE, 6); // fos

        // byte[] buf = new byte[8192];
        mv.visitIntInsn(Opcodes.SIPUSH, 8192);
        mv.visitIntInsn(Opcodes.NEWARRAY, Opcodes.T_BYTE);
        mv.visitVarInsn(Opcodes.ASTORE, 7); // buf

        // while ((n = is.read(buf)) != -1) { fos.write(buf, 0, n); }
        Label loopStart = new Label();
        Label loopEnd = new Label();
        mv.visitLabel(loopStart);
        mv.visitVarInsn(Opcodes.ALOAD, 3); // is
        mv.visitVarInsn(Opcodes.ALOAD, 7); // buf
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/InputStream",
                "read", "([B)I", false);
        mv.visitVarInsn(Opcodes.ISTORE, 8); // n
        mv.visitVarInsn(Opcodes.ILOAD, 8);
        mv.visitInsn(Opcodes.ICONST_M1);
        mv.visitJumpInsn(Opcodes.IF_ICMPEQ, loopEnd);

        mv.visitVarInsn(Opcodes.ALOAD, 6); // fos
        mv.visitVarInsn(Opcodes.ALOAD, 7); // buf
        mv.visitInsn(Opcodes.ICONST_0);
        mv.visitVarInsn(Opcodes.ILOAD, 8); // n
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/FileOutputStream",
                "write", "([BII)V", false);
        mv.visitJumpInsn(Opcodes.GOTO, loopStart);

        mv.visitLabel(loopEnd);

        // fos.close(); is.close();
        mv.visitVarInsn(Opcodes.ALOAD, 6);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/FileOutputStream",
                "close", "()V", false);
        mv.visitVarInsn(Opcodes.ALOAD, 3);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/InputStream",
                "close", "()V", false);

        // System.load(tmpFile.getAbsolutePath());
        mv.visitVarInsn(Opcodes.ALOAD, 5);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/io/File",
                "getAbsolutePath", "()Ljava/lang/String;", false);
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System",
                "load", "(Ljava/lang/String;)V", false);

        mv.visitInsn(Opcodes.RETURN);
        mv.visitMaxs(5, 9);
        mv.visitEnd();
    }

    private void generateDetectTargetMethod(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(
                Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                "detectTarget", "()Ljava/lang/String;", null, null);
        mv.visitCode();

        /*
         * String arch = System.getProperty("os.arch").toLowerCase();
         * String os = System.getProperty("os.name").toLowerCase();
         * String zigArch, zigOs;
         * if (arch.contains("amd64") || arch.contains("x86_64")) zigArch = "x86_64";
         * else if (arch.contains("aarch64")) zigArch = "aarch64";
         * else zigArch = arch;
         * if (os.contains("windows")) zigOs = "windows-gnu";
         * else if (os.contains("linux")) zigOs = "linux-gnu";
         * else if (os.contains("mac")) zigOs = "macos";
         * else zigOs = "linux-gnu";
         * return zigArch + "-" + zigOs;
         */

        // 简化：用 StringBuilder 拼接
        // arch
        mv.visitLdcInsn("os.arch");
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System",
                "getProperty", "(Ljava/lang/String;)Ljava/lang/String;", false);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String",
                "toLowerCase", "()Ljava/lang/String;", false);
        mv.visitVarInsn(Opcodes.ASTORE, 0); // arch

        // os
        mv.visitLdcInsn("os.name");
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System",
                "getProperty", "(Ljava/lang/String;)Ljava/lang/String;", false);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String",
                "toLowerCase", "()Ljava/lang/String;", false);
        mv.visitVarInsn(Opcodes.ASTORE, 1); // os

        // zigArch
        mv.visitLdcInsn("x86_64"); // default
        mv.visitVarInsn(Opcodes.ASTORE, 2);

        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitLdcInsn("amd64");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String",
                "contains", "(Ljava/lang/CharSequence;)Z", false);
        Label notAmd64 = new Label();
        mv.visitJumpInsn(Opcodes.IFEQ, notAmd64);
        mv.visitLdcInsn("x86_64");
        mv.visitVarInsn(Opcodes.ASTORE, 2);
        Label archDone = new Label();
        mv.visitJumpInsn(Opcodes.GOTO, archDone);

        mv.visitLabel(notAmd64);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitLdcInsn("aarch64");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String",
                "contains", "(Ljava/lang/CharSequence;)Z", false);
        Label notAarch64 = new Label();
        mv.visitJumpInsn(Opcodes.IFEQ, notAarch64);
        mv.visitLdcInsn("aarch64");
        mv.visitVarInsn(Opcodes.ASTORE, 2);
        mv.visitJumpInsn(Opcodes.GOTO, archDone);

        mv.visitLabel(notAarch64);
        // keep default x86_64

        mv.visitLabel(archDone);

        // zigOs
        mv.visitLdcInsn("linux-gnu"); // default
        mv.visitVarInsn(Opcodes.ASTORE, 3);

        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitLdcInsn("windows");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String",
                "contains", "(Ljava/lang/CharSequence;)Z", false);
        Label notWindows = new Label();
        mv.visitJumpInsn(Opcodes.IFEQ, notWindows);
        mv.visitLdcInsn("windows-gnu");
        mv.visitVarInsn(Opcodes.ASTORE, 3);
        Label osDone = new Label();
        mv.visitJumpInsn(Opcodes.GOTO, osDone);

        mv.visitLabel(notWindows);
        mv.visitVarInsn(Opcodes.ALOAD, 1);
        mv.visitLdcInsn("mac");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String",
                "contains", "(Ljava/lang/CharSequence;)Z", false);
        Label notMac = new Label();
        mv.visitJumpInsn(Opcodes.IFEQ, notMac);
        mv.visitLdcInsn("macos");
        mv.visitVarInsn(Opcodes.ASTORE, 3);
        mv.visitJumpInsn(Opcodes.GOTO, osDone);

        mv.visitLabel(notMac);
        // keep default linux-gnu

        mv.visitLabel(osDone);

        // return zigArch + "-" + zigOs
        mv.visitTypeInsn(Opcodes.NEW, "java/lang/StringBuilder");
        mv.visitInsn(Opcodes.DUP);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/lang/StringBuilder",
                "<init>", "()V", false);
        mv.visitVarInsn(Opcodes.ALOAD, 2);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder",
                "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        mv.visitLdcInsn("-");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder",
                "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        mv.visitVarInsn(Opcodes.ALOAD, 3);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder",
                "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/StringBuilder",
                "toString", "()Ljava/lang/String;", false);
        mv.visitInsn(Opcodes.ARETURN);
        mv.visitMaxs(3, 4);
        mv.visitEnd();
    }

    private void generateDetectLibNameMethod(ClassWriter cw) {
        MethodVisitor mv = cw.visitMethod(
                Opcodes.ACC_PRIVATE | Opcodes.ACC_STATIC,
                "detectLibName", "()Ljava/lang/String;", null, null);
        mv.visitCode();

        // String os = System.getProperty("os.name").toLowerCase();
        mv.visitLdcInsn("os.name");
        mv.visitMethodInsn(Opcodes.INVOKESTATIC, "java/lang/System",
                "getProperty", "(Ljava/lang/String;)Ljava/lang/String;", false);
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String",
                "toLowerCase", "()Ljava/lang/String;", false);
        mv.visitVarInsn(Opcodes.ASTORE, 0);

        // if (os.contains("windows")) return "customvm.dll";
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitLdcInsn("windows");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String",
                "contains", "(Ljava/lang/CharSequence;)Z", false);
        Label notWin = new Label();
        mv.visitJumpInsn(Opcodes.IFEQ, notWin);
        mv.visitLdcInsn("customvm.dll");
        mv.visitInsn(Opcodes.ARETURN);

        // if (os.contains("mac")) return "libcustomvm.dylib";
        mv.visitLabel(notWin);
        mv.visitVarInsn(Opcodes.ALOAD, 0);
        mv.visitLdcInsn("mac");
        mv.visitMethodInsn(Opcodes.INVOKEVIRTUAL, "java/lang/String",
                "contains", "(Ljava/lang/CharSequence;)Z", false);
        Label notMac = new Label();
        mv.visitJumpInsn(Opcodes.IFEQ, notMac);
        mv.visitLdcInsn("libcustomvm.dylib");
        mv.visitInsn(Opcodes.ARETURN);

        // else return "libcustomvm.so";
        mv.visitLabel(notMac);
        mv.visitLdcInsn("libcustomvm.so");
        mv.visitInsn(Opcodes.ARETURN);

        mv.visitMaxs(2, 1);
        mv.visitEnd();
    }

    // ===== IO 工具 =====

    private byte[] readAll(InputStream is) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream(4096);
        byte[] buf = new byte[4096];
        int n;
        while ((n = is.read(buf)) != -1) {
            baos.write(buf, 0, n);
        }
        return baos.toByteArray();
    }

    private void copyStream(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) != -1) {
            out.write(buf, 0, n);
        }
    }
}