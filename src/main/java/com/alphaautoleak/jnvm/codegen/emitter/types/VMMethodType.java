package com.alphaautoleak.jnvm.codegen.emitter.types;

import java.io.PrintWriter;

/**
 * Generates VMMethod and VMExceptionEntry type definitions
 */
public class VMMethodType {
    
    public static void generate(PrintWriter w) {
        emitExceptionEntry(w);
        emitVMMethod(w);
    }
    
    private static void emitExceptionEntry(PrintWriter w) {
        w.println("/* Exception table entry */");
        w.println("typedef struct __attribute__((packed)) {  // only ints, safe to pack");
        w.println("    int startPc;             // try block start PC (inclusive)");
        w.println("    int endPc;               // try block end PC (exclusive)");
        w.println("    int handlerPc;           // catch handler start PC");
        w.println("    int catchTypeIdx;        // caught exception type index (-1 for catch-all/finally)");
        w.println("} VMExceptionEntry;");
        w.println();
    }

    private static void emitVMMethod(PrintWriter w) {
        w.println("/* Method definition */");
        w.println("typedef struct __attribute__((aligned(16))) {  // cache-line friendly alignment");
        w.println("    int methodId;");
        w.println("    int maxStack;");
        w.println("    int maxLocals;");
        w.println("    uint8_t* bytecode;       // instruction sequence");
        w.println("    int bytecodeLen;");
        w.println("    MetaEntry* metadata;     // metadata array");
        w.println("    int metadataCount;");
        w.println("    int* pcToMetaIdx;        // PC to metadata index mapping");
        w.println("    int descIdx;             // method descriptor index");
        w.println("    int descLen;             // method descriptor length");
        w.println("    int isStatic;            // is static method");
        w.println("    VMExceptionEntry* exceptionTable;  // exception table");
        w.println("    int exceptionTableLength;         // exception table length");
        w.println("    // pre-parsed method argument info (performance optimization)");
        w.println("    int argCount;            // argument count");
        w.println("    int argTypesIdx;         // argument types string index (e.g. \"IJDL\")");
        w.println("    // method identity (for direct VM-to-VM call optimization)");
        w.println("    int ownerIdx;            // owner class name string index");
        w.println("    int nameIdx;             // method name string index");
        w.println("} VMMethod;");
        w.println();
    }
}