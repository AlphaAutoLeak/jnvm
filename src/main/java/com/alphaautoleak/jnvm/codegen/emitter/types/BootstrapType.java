package com.alphaautoleak.jnvm.codegen.emitter.types;

import java.io.PrintWriter;

/**
 * 生成 Bootstrap 方法相关类型定义
 */
public class BootstrapType {
    
    public static void generate(PrintWriter w) {
        emitBsmArgType(w);
        emitBsmArg(w);
        emitVMBootstrapMethod(w);
        emitExternals(w);
    }
    
    private static void emitBsmArgType(PrintWriter w) {
        w.println("/* Bootstrap 方法参数类型 */");
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
        w.println("/* Bootstrap 方法参数 */");
        w.println("typedef struct {");
        w.println("    BsmArgType type;");
        w.println("    int strIdx;       // 字符串/MethodType/MethodHandle 的字符串索引");
        w.println("    int intVal;");
        w.println("    long longVal;");
        w.println("    float floatVal;");
        w.println("    double doubleVal;");
        w.println("    int handleTag;    // MethodHandle tag (仅用于 METHOD_HANDLE)");
        w.println("} BsmArg;");
        w.println();
    }
    
    private static void emitVMBootstrapMethod(PrintWriter w) {
        w.println("/* Bootstrap 方法定义 */");
        w.println("typedef struct {");
        w.println("    int handleTag;           // MethodHandle tag (REF_invokeStatic=6, etc)");
        w.println("    int ownerIdx;            // bootstrap 方法所属类");
        w.println("    int nameIdx;             // bootstrap 方法名");
        w.println("    int descIdx;             // bootstrap 方法描述符");
        w.println("    BsmArg* args;            // bootstrap 参数数组");
        w.println("    int argCount;            // 参数数量");
        w.println("} VMBootstrapMethod;");
        w.println();
    }
    
    private static void emitExternals(PrintWriter w) {
        w.println("/* 全局 Bootstrap 方法表 */");
        w.println("extern VMBootstrapMethod vm_bootstrap_methods[];");
        w.println("extern const int vm_bootstrap_count;");
        w.println();
    }
}
