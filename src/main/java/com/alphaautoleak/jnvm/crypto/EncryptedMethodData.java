package com.alphaautoleak.jnvm.crypto;

import com.alphaautoleak.jnvm.asm.BootstrapEntry;
import com.alphaautoleak.jnvm.asm.MetaEntry;
import com.alphaautoleak.jnvm.asm.ExceptionEntry;
import com.alphaautoleak.jnvm.asm.MethodInfo;

import java.util.List;

/**
 * Complete data package for a method (bytecode stored in plaintext, ChaCha20 only for string encryption)
 * All fields will be serialized to C source.
 */
public class EncryptedMethodData {

    /** Method ID */
    private final int methodId;

    /** Owner class (internal name) */
    private final String owner;

    /** Method name */
    private final String name;

    /** Descriptor */
    private final String descriptor;

    /** Access flags */
    private final int access;

    /** Bytecode */
    private final byte[] bytecode;

    /** max_stack */
    private final int maxStack;

    /** max_locals */
    private final int maxLocals;

    /** Metadata list (new format) */
    private final List<MetaEntry> metadata;

    /** PC to metadata index mapping */
    private final int[] pcToMetaIdx;

    /** String pool */
    private final List<String> stringPool;

    /** Exception table */
    private final List<ExceptionEntry> exceptionTable;

    /** Bootstrap method table */
    private final List<BootstrapEntry> bootstrapMethods;

    /** Is static */
    private final boolean isStatic;

    /** Is synchronized */
    private final boolean isSynchronized;

    public EncryptedMethodData(MethodInfo info) {
        this.methodId = info.getMethodId();
        this.owner = info.getOwner();
        this.name = info.getName();
        this.descriptor = info.getDescriptor();
        this.access = info.getAccess();
        this.bytecode = info.getBytecode();
        this.maxStack = info.getMaxStack();
        this.maxLocals = info.getMaxLocals();
        this.metadata = info.getMetadata();
        this.pcToMetaIdx = info.getPcToMetaIdx();
        this.stringPool = info.getStringPool();
        this.exceptionTable = info.getExceptionTable();
        this.bootstrapMethods = info.getBootstrapMethods();
        this.isStatic = info.isStatic();
        this.isSynchronized = info.isSynchronized();
    }

    // ===== Getters =====

    public int getMethodId() { return methodId; }
    public String getOwner() { return owner; }
    public String getName() { return name; }
    public String getDescriptor() { return descriptor; }
    public int getAccess() { return access; }
    public byte[] getEncryptedBytecode() { return bytecode; }
    public int getMaxStack() { return maxStack; }
    public int getMaxLocals() { return maxLocals; }
    public List<MetaEntry> getMetadata() { return metadata; }
    public int[] getPcToMetaIdx() { return pcToMetaIdx; }
    public List<String> getStringPool() { return stringPool; }
    public List<ExceptionEntry> getExceptionTable() { return exceptionTable; }
    public List<BootstrapEntry> getBootstrapMethods() { return bootstrapMethods; }
    public boolean isStatic() { return isStatic; }
    public boolean isSynchronized() { return isSynchronized; }

    @Override
    public String toString() {
        return String.format(
                "Encrypted{id=%d, %s.%s, bytecode=%d bytes, meta=%d, strings=%d, exc=%d, bsm=%d}",
                methodId, owner, name,
                bytecode.length,
                metadata.size(), stringPool.size(), exceptionTable.size(), bootstrapMethods.size()
        );
    }
}