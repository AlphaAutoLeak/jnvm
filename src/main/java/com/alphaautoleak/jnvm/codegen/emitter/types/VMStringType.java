package com.alphaautoleak.jnvm.codegen.emitter.types;

import java.io.PrintWriter;

/**
 * 生成 VMString 类型定义
 * 支持 ChaCha20 加密字符串
 */
public class VMStringType {
    
    public static void generate(PrintWriter w, boolean encryptStrings) {
        w.println("/* 字符串池 */");
        w.println("typedef struct {");
        if (encryptStrings) {
            w.println("    const unsigned char* encData;  // 加密数据");
            w.println("    char* decData;                 // 解密缓存（运行时分配）");
            w.println("    int len;                       // 原始长度");
            w.println("    int encrypted;                 // 是否加密 (1=是, 0=否)");
        } else {
            // 非加密模式：简化结构
            w.println("    const unsigned char* encData;  // 字符串数据");
            w.println("    char* decData;                 // 未使用");
            w.println("    int len;                       // 字符串长度");
            w.println("    int encrypted;                 // 始终为 0");
        }
        w.println("} VMString;");
        w.println();
    }
}
