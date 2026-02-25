package com.alphaautoleak.jnvm.asm;

import java.util.ArrayList;
import java.util.List;

/**
 * 一个被保护方法的完整元数据。
 * 转换器从 ASM 中提取，后续传给 C 代码生成器。
 */
public class MethodInfo {

    /** 全局唯一方法编号（构建时分配） */
    private int methodId;

    /** 所属类的内部名 e.g. "com/example/service/UserService" */
    private String owner;

    /** 方法名 */
    private String name;

    /** 方法描述符 e.g. "(Ljava/lang/String;I)V" */
    private String descriptor;

    /** 访问标志 (ACC_PUBLIC, ACC_STATIC, ...) */
    private int access;

    /** 原始字节码（instruction bytes from Code attribute） */
    private byte[] bytecode;

    /** max_stack */
    private int maxStack;

    /** max_locals */
    private int maxLocals;

    /** 异常表 */
    private List<ExceptionEntry> exceptionTable = new ArrayList<>();

    /** 自定义常量池条目（该方法引用的所有常量） */
    private List<CPEntry> constantPool = new ArrayList<>();

    /** Bootstrap 方法表（invokedynamic 用） */
    private List<BootstrapEntry> bootstrapMethods = new ArrayList<>();

    /** 方法签名（泛型，可选） */
    private String signature;

    /** 是否为静态方法 */
    public boolean isStatic() {
        return (access & 0x0008) != 0;
    }

    /** 是否为同步方法 */
    public boolean isSynchronized() {
        return (access & 0x0020) != 0;
    }

    /** 是否为构造方法 */
    public boolean isConstructor() {
        return "<init>".equals(name);
    }

    /** 是否为类初始化 */
    public boolean isClassInit() {
        return "<clinit>".equals(name);
    }

    /**
     * 解析描述符，返回参数类型列表
     */
    public List<String> getParameterTypes() {
        List<String> params = new ArrayList<>();
        String desc = descriptor;
        int i = 1; // skip '('
        while (i < desc.length() && desc.charAt(i) != ')') {
            int start = i;
            switch (desc.charAt(i)) {
                case 'B': case 'C': case 'D': case 'F':
                case 'I': case 'J': case 'S': case 'Z':
                    params.add(desc.substring(start, i + 1));
                    i++;
                    break;
                case 'L':
                    int semi = desc.indexOf(';', i);
                    params.add(desc.substring(start, semi + 1));
                    i = semi + 1;
                    break;
                case '[':
                    while (desc.charAt(i) == '[') i++;
                    if (desc.charAt(i) == 'L') {
                        int s = desc.indexOf(';', i);
                        params.add(desc.substring(start, s + 1));
                        i = s + 1;
                    } else {
                        params.add(desc.substring(start, i + 1));
                        i++;
                    }
                    break;
                default:
                    i++;
            }
        }
        return params;
    }

    /**
     * 返回值类型描述符
     */
    public String getReturnType() {
        int idx = descriptor.indexOf(')');
        return descriptor.substring(idx + 1);
    }

    // ===== Getters / Setters =====

    public int getMethodId() { return methodId; }
    public void setMethodId(int methodId) { this.methodId = methodId; }

    public String getOwner() { return owner; }
    public void setOwner(String owner) { this.owner = owner; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescriptor() { return descriptor; }
    public void setDescriptor(String descriptor) { this.descriptor = descriptor; }

    public int getAccess() { return access; }
    public void setAccess(int access) { this.access = access; }

    public byte[] getBytecode() { return bytecode; }
    public void setBytecode(byte[] bytecode) { this.bytecode = bytecode; }

    public int getMaxStack() { return maxStack; }
    public void setMaxStack(int maxStack) { this.maxStack = maxStack; }

    public int getMaxLocals() { return maxLocals; }
    public void setMaxLocals(int maxLocals) { this.maxLocals = maxLocals; }

    public List<ExceptionEntry> getExceptionTable() { return exceptionTable; }
    public void setExceptionTable(List<ExceptionEntry> exceptionTable) { this.exceptionTable = exceptionTable; }

    public List<CPEntry> getConstantPool() { return constantPool; }
    public void setConstantPool(List<CPEntry> constantPool) { this.constantPool = constantPool; }

    public List<BootstrapEntry> getBootstrapMethods() { return bootstrapMethods; }
    public void setBootstrapMethods(List<BootstrapEntry> bootstrapMethods) { this.bootstrapMethods = bootstrapMethods; }

    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }

    @Override
    public String toString() {
        return String.format("MethodInfo{id=%d, %s.%s%s, stack=%d, locals=%d, bytecode=%d bytes, cp=%d entries, exceptions=%d}",
                methodId, owner, name, descriptor, maxStack, maxLocals,
                bytecode != null ? bytecode.length : 0,
                constantPool.size(), exceptionTable.size());
    }
}