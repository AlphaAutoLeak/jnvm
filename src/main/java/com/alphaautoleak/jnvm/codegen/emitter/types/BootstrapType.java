package com.alphaautoleak.jnvm.codegen.emitter.types;

import java.io.PrintWriter;

/**
 * Generates Bootstrap method related type definitions
 */
public class BootstrapType {
    
    public static void generate(PrintWriter w) {
        emitBsmArgType(w);
        emitBsmArg(w);
        emitVMBootstrapMethod(w);
        emitExternals(w);
    }
    
    private static void emitBsmArgType(PrintWriter w) {
        w.println("/* Bootstrap method argument type */");
        w.println("typedef enum {");
        w.println("    BSM_ARG_STRING = 0,");
        w.println("    BSM_ARG_INTEGER,");
        w.println("    BSM_ARG_LONG,");
        w.println("    BSM_ARG_FLOAT,");
        w.println("    BSM_ARG_DOUBLE,");
        w.println("    BSM_ARG_METHOD_TYPE,");
        w.println("    BSM_ARG_METHOD_HANDLE");
        w.println("} BsmArgType;");
        w.println();
    }

    private static void emitBsmArg(PrintWriter w) {
        w.println("/* Bootstrap method argument */");
        w.println("typedef struct {");
        w.println("    BsmArgType type;");
        w.println("    int strIdx;       // string index for String/MethodType/MethodHandle");
        w.println("    int intVal;");
        w.println("    long longVal;");
        w.println("    float floatVal;");
        w.println("    double doubleVal;");
        w.println("    int handleTag;    // MethodHandle tag (only for METHOD_HANDLE)");
        w.println("} BsmArg;");
        w.println();
    }

    private static void emitVMBootstrapMethod(PrintWriter w) {
        w.println("/* Bootstrap method definition */");
        w.println("typedef struct {");
        w.println("    int handleTag;           // MethodHandle tag (REF_invokeStatic=6, etc)");
        w.println("    int ownerIdx;            // bootstrap method owner class");
        w.println("    int nameIdx;             // bootstrap method name");
        w.println("    int descIdx;             // bootstrap method descriptor");
        w.println("    BsmArg* args;            // bootstrap argument array");
        w.println("    int argCount;            // argument count");
        w.println("} VMBootstrapMethod;");
        w.println();
    }

    private static void emitExternals(PrintWriter w) {
        w.println("/* Global Bootstrap method table */");
        w.println("extern VMBootstrapMethod vm_bootstrap_methods[];");
        w.println("extern const int vm_bootstrap_count;");
        w.println();
    }
}
