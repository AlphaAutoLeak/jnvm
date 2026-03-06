package com.alphaautoleak.jnvm.codegen.emitter.types;

import java.io.PrintWriter;

/**
 * 生成 VMMethod 和 VMExceptionEntry 类型定义
 */
public class VMMethodType {
    
    public static void generate(PrintWriter w) {
        emitExceptionEntry(w);
        emitVMMethod(w);
    }
    
    private static void emitExceptionEntry(PrintWriter w) {
        w.println("/* 异常表条目 */");
        w.println("typedef struct {");
        w.println("    int startPc;             // try 块起始 PC（含）");
        w.println("    int endPc;               // try 块结束 PC（不含）");
        w.println("    int handlerPc;           // catch handler 起始 PC");
        w.println("    int catchTypeIdx;        // 捕获的异常类型索引（-1 表示 catch-all/finally）");
        w.println("} VMExceptionEntry;");
        w.println();
    }
    
    private static void emitVMMethod(PrintWriter w) {
        w.println("/* 方法定义 */");
        w.println("typedef struct {");
        w.println("    int methodId;");
        w.println("    int maxStack;");
        w.println("    int maxLocals;");
        w.println("    uint8_t* bytecode;       // 指令序列（加密或明文）");
        w.println("    int bytecodeLen;");
        w.println("    uint8_t key[32];         // ChaCha20 密钥（仅当 encrypted=1 时有效）");
        w.println("    uint8_t nonce[12];       // ChaCha20 nonce（仅当 encrypted=1 时有效）");
        w.println("    int encrypted;           // 字节码是否加密");
        w.println("    MetaEntry* metadata;     // 元数据数组");
        w.println("    int metadataCount;");
        w.println("    int* pcToMetaIdx;        // PC -> 元数据索引映射");
        w.println("    int descIdx;             // 方法描述符索引");
        w.println("    int descLen;             // 方法描述符长度");
        w.println("    int isStatic;            // 是否静态方法");
        w.println("    VMExceptionEntry* exceptionTable;  // 异常表");
        w.println("    int exceptionTableLength;         // 异常表长度");
        w.println("    uint8_t* cachedBytecode; // 解密后的字节码缓存（延迟初始化，仅当 encrypted=1 时使用）");
        w.println("} VMMethod;");
        w.println();
    }
}
