package com.alphaautoleak.jnvm.codegen.emitter.types;

import java.io.PrintWriter;

/**
 * 生成 VMString 类型定义
 */
public class VMStringType {
    
    public static void generate(PrintWriter w) {
        w.println("/* 字符串池 */");
        w.println("typedef struct {");
        w.println("    const char* data;");
        w.println("    int len;");
        w.println("} VMString;");
        w.println();
    }
}
