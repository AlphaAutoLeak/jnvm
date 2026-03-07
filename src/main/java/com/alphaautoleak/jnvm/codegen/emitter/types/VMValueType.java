package com.alphaautoleak.jnvm.codegen.emitter.types;

import java.io.PrintWriter;

/**
 * Generates VMValue and VMFrame type definitions
 * 64-bit only - all types occupy 1 slot, no type tracking needed
 */
public class VMValueType {
    
    public static void generate(PrintWriter w) {
        emitVMValue(w);
        emitVMFrame(w);
    }
    
    private static void emitVMValue(PrintWriter w) {
        w.println("/* Stack value - unified 64-bit width (64-bit only) */");
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
        w.println("/* Execution frame (64-bit only - no type tracking needed) */");
        w.println("typedef struct {");
        w.println("    int pc;           // program counter");
        w.println("    int sp;           // stack pointer");
        w.println("    VMValue* stack;   // operand stack");
        w.println("    VMValue* locals;  // local variable table");
        w.println("    jclass callerClass;  // caller class (for classloader consistency)");
        w.println("} VMFrame;");
        w.println();
    }
}