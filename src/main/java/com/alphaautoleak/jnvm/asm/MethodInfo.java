package com.alphaautoleak.jnvm.asm;

import com.alphaautoleak.jnvm.asm.BytecodeExtractor.MetaEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * Complete metadata for a protected method.
 * Extracted from ASM by converter, passed to C code generator.
 */
public class MethodInfo {

    /** Globally unique method ID (assigned at build time) */
    private int methodId;

    /** Owner class internal name e.g. "com/example/service/UserService" */
    private String owner;

    /** Method name */
    private String name;

    /** Method descriptor e.g. "(Ljava/lang/String;I)V" */
    private String descriptor;

    /** Access flags (ACC_PUBLIC, ACC_STATIC, ...) */
    private int access;

    /** Raw bytecode (instruction bytes from Code attribute) */
    private byte[] bytecode;

    /** max_stack */
    private int maxStack;

    /** max_locals */
    private int maxLocals;

    /** Exception table */
    private List<ExceptionEntry> exceptionTable = new ArrayList<>();

    /** Metadata list (new format: operands for each instruction) */
    private List<MetaEntry> metadata = new ArrayList<>();

    /** PC to metadata index mapping */
    private int[] pcToMetaIdx;

    /** String pool */
    private List<String> stringPool = new ArrayList<>();

    /** Bootstrap method table (for invokedynamic) */
    private List<BootstrapEntry> bootstrapMethods = new ArrayList<>();

    /** Method signature (generic, optional) */
    private String signature;

    /** Whether this is a static method */
    public boolean isStatic() {
        return (access & 0x0008) != 0;
    }

    /** Whether this is a synchronized method */
    public boolean isSynchronized() {
        return (access & 0x0020) != 0;
    }

    /** Whether this is a constructor */
    public boolean isConstructor() {
        return "<init>".equals(name);
    }

    /** Whether this is a class initializer */
    public boolean isClassInit() {
        return "<clinit>".equals(name);
    }

    /**
     * Parses descriptor, returns parameter type list
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
     * Return type descriptor
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

    public List<MetaEntry> getMetadata() { return metadata; }
    public void setMetadata(List<MetaEntry> metadata) { this.metadata = metadata; }

    public int[] getPcToMetaIdx() { return pcToMetaIdx; }
    public void setPcToMetaIdx(int[] pcToMetaIdx) { this.pcToMetaIdx = pcToMetaIdx; }

    public List<String> getStringPool() { return stringPool; }
    public void setStringPool(List<String> stringPool) { this.stringPool = stringPool; }

    public List<BootstrapEntry> getBootstrapMethods() { return bootstrapMethods; }
    public void setBootstrapMethods(List<BootstrapEntry> bootstrapMethods) { this.bootstrapMethods = bootstrapMethods; }

    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }

    @Override
    public String toString() {
        return String.format("MethodInfo{id=%d, %s.%s%s, stack=%d, locals=%d, bytecode=%d bytes, meta=%d, strings=%d, exceptions=%d}",
                methodId, owner, name, descriptor, maxStack, maxLocals,
                bytecode != null ? bytecode.length : 0,
                metadata.size(), stringPool.size(), exceptionTable.size());
    }
}
