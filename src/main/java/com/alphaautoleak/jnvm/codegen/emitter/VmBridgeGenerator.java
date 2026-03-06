package com.alphaautoleak.jnvm.codegen.emitter;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * 生成 vm_bridge.c - JNI 桥接层
 * 为每种返回类型生成不同的 native 函数，避免装箱/拆箱
 */
public class VmBridgeGenerator {

    private final File dir;
    private final String bridgeClass;
    private final boolean encryptStrings;

    public VmBridgeGenerator(File dir, String bridgeClass, boolean encryptStrings) {
        this.dir = dir;
        this.bridgeClass = bridgeClass;
        this.encryptStrings = encryptStrings;
    }

    public void generate() throws IOException {
        try (PrintWriter w = new PrintWriter(new java.io.FileWriter(new File(dir, "vm_bridge.c")))) {
            w.println("#include \"vm_types.h\"");
            w.println("#include \"vm_data.h\"");
            w.println("#include \"vm_interpreter.h\"");
            w.println();

            // 生成各种返回类型的 execute 函数
            emitExecuteFunctions(w);
            w.println();

            // 生成 RegisterNatives
            emitRegisterNatives(w);
            w.println();

            // 生成 JNI_OnLoad
            emitJNIOnLoad(w);
        }
    }

    /**
     * 生成各种返回类型的 execute 函数
     */
    private void emitExecuteFunctions(PrintWriter w) {
        // void
        w.println("/* void 返回类型 */");
        w.println("static void JNICALL native_execute_void(JNIEnv* env, jclass cls,");
        w.println("                                        jint methodId, jobject instance,");
        w.println("                                        jobjectArray args, jclass callerClass) {");
        w.println("    vm_execute_method_void(env, methodId, instance, args, callerClass);");
        w.println("}");
        w.println();

        // int (包括 boolean, byte, char, short, int)
        w.println("/* int 返回类型 (boolean/byte/char/short/int) */");
        w.println("static jint JNICALL native_execute_int(JNIEnv* env, jclass cls,");
        w.println("                                         jint methodId, jobject instance,");
        w.println("                                         jobjectArray args, jclass callerClass) {");
        w.println("    return vm_execute_method_int(env, methodId, instance, args, callerClass);");
        w.println("}");
        w.println();

        // long
        w.println("/* long 返回类型 */");
        w.println("static jlong JNICALL native_execute_long(JNIEnv* env, jclass cls,");
        w.println("                                           jint methodId, jobject instance,");
        w.println("                                           jobjectArray args, jclass callerClass) {");
        w.println("    return vm_execute_method_long(env, methodId, instance, args, callerClass);");
        w.println("}");
        w.println();

        // float
        w.println("/* float 返回类型 */");
        w.println("static jfloat JNICALL native_execute_float(JNIEnv* env, jclass cls,");
        w.println("                                             jint methodId, jobject instance,");
        w.println("                                             jobjectArray args, jclass callerClass) {");
        w.println("    return vm_execute_method_float(env, methodId, instance, args, callerClass);");
        w.println("}");
        w.println();

        // double
        w.println("/* double 返回类型 */");
        w.println("static jdouble JNICALL native_execute_double(JNIEnv* env, jclass cls,");
        w.println("                                              jint methodId, jobject instance,");
        w.println("                                              jobjectArray args, jclass callerClass) {");
        w.println("    return vm_execute_method_double(env, methodId, instance, args, callerClass);");
        w.println("}");
        w.println();

        // object
        w.println("/* object 返回类型 */");
        w.println("static jobject JNICALL native_execute_object(JNIEnv* env, jclass cls,");
        w.println("                                               jint methodId, jobject instance,");
        w.println("                                               jobjectArray args, jclass callerClass) {");
        w.println("    return vm_execute_method_object(env, methodId, instance, args, callerClass);");
        w.println("}");
    }

    /**
     * 生成 RegisterNatives 表
     */
    private void emitRegisterNatives(PrintWriter w) {
        w.println("/* JNI 方法注册表 */");
        w.println("static JNINativeMethod native_methods[] = {");
        w.println("    { \"executeVoid\",   \"(ILjava/lang/Object;[Ljava/lang/Object;Ljava/lang/Class;)V\",       (void*)native_execute_void },");
        w.println("    { \"executeInt\",    \"(ILjava/lang/Object;[Ljava/lang/Object;Ljava/lang/Class;)I\",       (void*)native_execute_int },");
        w.println("    { \"executeLong\",   \"(ILjava/lang/Object;[Ljava/lang/Object;Ljava/lang/Class;)J\",       (void*)native_execute_long },");
        w.println("    { \"executeFloat\",  \"(ILjava/lang/Object;[Ljava/lang/Object;Ljava/lang/Class;)F\",       (void*)native_execute_float },");
        w.println("    { \"executeDouble\", \"(ILjava/lang/Object;[Ljava/lang/Object;Ljava/lang/Class;)D\",       (void*)native_execute_double },");
        w.println("    { \"executeObject\", \"(ILjava/lang/Object;[Ljava/lang/Object;Ljava/lang/Class;)Ljava/lang/Object;\", (void*)native_execute_object }");
        w.println("};");
        w.println();

        w.println("/* 注册本地方法 */");
        w.println("static int register_native_methods(JNIEnv* env) {");
        w.println("    jclass cls = (*env)->FindClass(env, \"" + bridgeClass + "\");");
        w.println("    if (cls == NULL) {");
        w.println("        return JNI_ERR;");
        w.println("    }");
        w.println();
        w.println("    if ((*env)->RegisterNatives(env, cls, native_methods,");
        w.println("                                 sizeof(native_methods) / sizeof(native_methods[0])) < 0) {");
        w.println("        return JNI_ERR;");
        w.println("    }");
        w.println();
        w.println("    return JNI_OK;");
        w.println("}");
    }

    /**
     * 生成 JNI_OnLoad
     */
    private void emitJNIOnLoad(PrintWriter w) {
        w.println("/* JNI_OnLoad - 库加载时初始化 */");
        w.println("JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {");
        w.println("    JNIEnv* env = NULL;");
        w.println("    if ((*vm)->GetEnv(vm, (void**)&env, JNI_VERSION_1_8) != JNI_OK) {");
        w.println("        return JNI_ERR;");
        w.println("    }");
        w.println();
        if (encryptStrings) {
            w.println("    // 初始化字符串池（解密所有字符串）");
            w.println("    vm_init_strings();");
            w.println();
        }
        w.println("    if (register_native_methods(env) != JNI_OK) {");
        w.println("        return JNI_ERR;");
        w.println("    }");
        w.println();
        w.println("    return JNI_VERSION_1_8;");
        w.println("}");
    }
}