package com.alphaautoleak.jnvm.crypto;

import com.alphaautoleak.jnvm.asm.BootstrapEntry;
import com.alphaautoleak.jnvm.asm.BytecodeExtractor.MetaEntry;
import com.alphaautoleak.jnvm.asm.ExceptionEntry;
import com.alphaautoleak.jnvm.asm.MethodInfo;

import java.util.List;

/**
 * 一个方法加密后的完整数据包。
 * 所有字段将被序列化到 C 源码中。
 */
public class EncryptedMethodData {

    /** 方法 ID */
    private final int methodId;

    /** 所属类（内部名） */
    private final String owner;

    /** 方法名 */
    private final String name;

    /** 描述符 */
    private final String descriptor;

    /** 访问标志 */
    private final int access;

    /** 加密后的字节码 */
    private final byte[] encryptedBytecode;

    /** 原始字节码长度（解密时需要） */
    private final int originalLength;

    /** 加密密钥 (32 bytes) */
    private final byte[] key;

    /** 加密 nonce (12 bytes) */
    private final byte[] nonce;

    /** max_stack */
    private final int maxStack;

    /** max_locals */
    private final int maxLocals;

    /** 元数据列表（新格式） */
    private final List<MetaEntry> metadata;

    /** PC -> 元数据索引映射 */
    private final int[] pcToMetaIdx;

    /** 字符串池 */
    private final List<String> stringPool;

    /** 异常表 */
    private final List<ExceptionEntry> exceptionTable;

    /** Bootstrap 方法表 */
    private final List<BootstrapEntry> bootstrapMethods;

    /** 是否静态 */
    private final boolean isStatic;

    /** 是否同步 */
    private final boolean isSynchronized;

    public EncryptedMethodData(MethodInfo info, byte[] encryptedBytecode,
                               byte[] key, byte[] nonce) {
        this.methodId = info.getMethodId();
        this.owner = info.getOwner();
        this.name = info.getName();
        this.descriptor = info.getDescriptor();
        this.access = info.getAccess();
        this.encryptedBytecode = encryptedBytecode;
        this.originalLength = info.getBytecode().length;
        this.key = key;
        this.nonce = nonce;
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
    public byte[] getEncryptedBytecode() { return encryptedBytecode; }
    public int getOriginalLength() { return originalLength; }
    public byte[] getKey() { return key; }
    public byte[] getNonce() { return nonce; }
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
                "Encrypted{id=%d, %s.%s, bytecode=%d→%d bytes, meta=%d, strings=%d, exc=%d, bsm=%d}",
                methodId, owner, name,
                originalLength, encryptedBytecode.length,
                metadata.size(), stringPool.size(), exceptionTable.size(), bootstrapMethods.size()
        );
    }
}
