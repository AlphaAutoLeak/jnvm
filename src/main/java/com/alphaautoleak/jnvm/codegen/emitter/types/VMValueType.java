package com.alphaautoleak.jnvm.codegen.emitter.types;

import java.io.PrintWriter;

/**
 * 生成 VMValue 和 VMFrame 类型定义
 */
public class VMValueType {
    
    public static void generate(PrintWriter w) {
        emitVMValue(w);
        emitVMFrame(w);
    }
    
    private static void emitVMValue(PrintWriter w) {
        w.println("/* 栈值 - 统一 64 位宽 */");
        w.println("typedef union {");
        w.println("    jint     i;");
        w.println("    jlong    j;");
        w.println("    jfloat   f;");
        w.println("    jdouble  d;");
        w.println("    jobject  l;");
        w.println("    int64_t  raw;");
        w.println("} VMValue;");
        w.println();
    }
    
    private static void emitVMFrame(PrintWriter w) {
        w.println("/* 类型标记 - 用于跟踪栈上的值类型 */");
        w.println("typedef enum {");
        w.println("    TYPE_UNKNOWN = 0,");
        w.println("    TYPE_INT,       // int, short, byte, char, boolean");
        w.println("    TYPE_LONG,      // long (category 2)");
        w.println("    TYPE_FLOAT,     // float");
        w.println("    TYPE_DOUBLE,    // double (category 2)");
        w.println("    TYPE_REF        // object reference");
        w.println("} VMType;");
        w.println();
        w.println("/* 执行帧 */");
        w.println("typedef struct {");
        w.println("    int pc;           // 程序计数器");
        w.println("    int sp;           // 栈指针");
        w.println("    VMValue* stack;   // 操作栈");
        w.println("    VMValue* locals;  // 局部变量表");
        w.println("    VMType* stackTypes;  // 栈类型跟踪");
        w.println("    jclass callerClass;  // 调用者类（用于类加载器一致性）");
        w.println("} VMFrame;");
        w.println();
    }
}
