package com.alphaautoleak.jnvm.codegen.emitter.types;

import java.io.PrintWriter;

/**
 * 生成 VMString 类型定义
 * 支持 ChaCha20 加密字符串
 */
public class VMStringType {
    
    public static void generate(PrintWriter w) {
        w.println("/* 加密字符串池 */");
        w.println("typedef struct {");
        w.println("    const unsigned char* encData;  // 加密数据");
        w.println("    char* decData;                 // 解密缓存（运行时分配）");
        w.println("    int len;                       // 原始长度");
        w.println("    int encrypted;                 // 是否加密 (1=是, 0=否)");
        w.println("} VMString;");
        w.println();
    }
}
