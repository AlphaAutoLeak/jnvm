package com.alphaautoleak.jnvm.codegen.emitter;

import java.io.PrintWriter;

final class VmBridgeLegacyEmitter {

    private VmBridgeLegacyEmitter() {
    }

    static void emitExecuteFunctions(PrintWriter w) {
        w.println("/* void return type */");
        w.println("static void JNICALL native_execute_void(JNIEnv* env, jclass cls,");
        w.println("                                        jint methodId, jobjectArray args) {");
        w.println("    (void)cls;");
        w.println("    vm_execute_method_void(env, methodId, args);");
        w.println("}");
        w.println();

        w.println("/* int return type (boolean/byte/char/short/int) */");
        w.println("static jint JNICALL native_execute_int(JNIEnv* env, jclass cls,");
        w.println("                                         jint methodId, jobjectArray args) {");
        w.println("    (void)cls;");
        w.println("    return vm_execute_method_int(env, methodId, args);");
        w.println("}");
        w.println();

        w.println("/* long return type */");
        w.println("static jlong JNICALL native_execute_long(JNIEnv* env, jclass cls,");
        w.println("                                           jint methodId, jobjectArray args) {");
        w.println("    (void)cls;");
        w.println("    return vm_execute_method_long(env, methodId, args);");
        w.println("}");
        w.println();

        w.println("/* float return type */");
        w.println("static jfloat JNICALL native_execute_float(JNIEnv* env, jclass cls,");
        w.println("                                             jint methodId, jobjectArray args) {");
        w.println("    (void)cls;");
        w.println("    return vm_execute_method_float(env, methodId, args);");
        w.println("}");
        w.println();

        w.println("/* double return type */");
        w.println("static jdouble JNICALL native_execute_double(JNIEnv* env, jclass cls,");
        w.println("                                              jint methodId, jobjectArray args) {");
        w.println("    (void)cls;");
        w.println("    return vm_execute_method_double(env, methodId, args);");
        w.println("}");
        w.println();

        w.println("/* object return type */");
        w.println("static jobject JNICALL native_execute_object(JNIEnv* env, jclass cls,");
        w.println("                                               jint methodId, jobjectArray args) {");
        w.println("    (void)cls;");
        w.println("    return vm_execute_method_object(env, methodId, args);");
        w.println("}");
    }
}
