package com.alphaautoleak.jnvm.codegen.emitter;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Generates vm_bridge.c - JNI bridge layer
 * Generates separate native functions for each return type to avoid boxing/unboxing
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

            // Generate execute functions for each return type
            emitExecuteFunctions(w);
            w.println();

            // Generate RegisterNatives
            emitRegisterNatives(w);
            w.println();

            // Generate JNI_OnLoad
            emitJNIOnLoad(w);
        }
    }

    /**
     * Generate execute functions for each return type
     */
    private void emitExecuteFunctions(PrintWriter w) {
        // void
        w.println("/* void return type */");
        w.println("static void JNICALL native_execute_void(JNIEnv* env, jclass cls,");
        w.println("                                        jint methodId, jobject instance,");
        w.println("                                        jobjectArray args, jclass callerClass) {");
        w.println("    vm_execute_method_void(env, methodId, instance, args, callerClass);");
        w.println("}");
        w.println();

        // int (including boolean, byte, char, short, int)
        w.println("/* int return type (boolean/byte/char/short/int) */");
        w.println("static jint JNICALL native_execute_int(JNIEnv* env, jclass cls,");
        w.println("                                         jint methodId, jobject instance,");
        w.println("                                         jobjectArray args, jclass callerClass) {");
        w.println("    return vm_execute_method_int(env, methodId, instance, args, callerClass);");
        w.println("}");
        w.println();

        // long
        w.println("/* long return type */");
        w.println("static jlong JNICALL native_execute_long(JNIEnv* env, jclass cls,");
        w.println("                                           jint methodId, jobject instance,");
        w.println("                                           jobjectArray args, jclass callerClass) {");
        w.println("    return vm_execute_method_long(env, methodId, instance, args, callerClass);");
        w.println("}");
        w.println();

        // float
        w.println("/* float return type */");
        w.println("static jfloat JNICALL native_execute_float(JNIEnv* env, jclass cls,");
        w.println("                                             jint methodId, jobject instance,");
        w.println("                                             jobjectArray args, jclass callerClass) {");
        w.println("    return vm_execute_method_float(env, methodId, instance, args, callerClass);");
        w.println("}");
        w.println();

        // double
        w.println("/* double return type */");
        w.println("static jdouble JNICALL native_execute_double(JNIEnv* env, jclass cls,");
        w.println("                                              jint methodId, jobject instance,");
        w.println("                                              jobjectArray args, jclass callerClass) {");
        w.println("    return vm_execute_method_double(env, methodId, instance, args, callerClass);");
        w.println("}");
        w.println();

        // object
        w.println("/* object return type */");
        w.println("static jobject JNICALL native_execute_object(JNIEnv* env, jclass cls,");
        w.println("                                               jint methodId, jobject instance,");
        w.println("                                               jobjectArray args, jclass callerClass) {");
        w.println("    return vm_execute_method_object(env, methodId, instance, args, callerClass);");
        w.println("}");
    }

    /**
     * Generates RegisterNatives table
     */
    private void emitRegisterNatives(PrintWriter w) {
        w.println("/* JNI method registration table */");
        w.println("static JNINativeMethod native_methods[] = {");
        w.println("    { \"executeVoid\",   \"(ILjava/lang/Object;[Ljava/lang/Object;Ljava/lang/Class;)V\",       (void*)native_execute_void },");
        w.println("    { \"executeInt\",    \"(ILjava/lang/Object;[Ljava/lang/Object;Ljava/lang/Class;)I\",       (void*)native_execute_int },");
        w.println("    { \"executeLong\",   \"(ILjava/lang/Object;[Ljava/lang/Object;Ljava/lang/Class;)J\",       (void*)native_execute_long },");
        w.println("    { \"executeFloat\",  \"(ILjava/lang/Object;[Ljava/lang/Object;Ljava/lang/Class;)F\",       (void*)native_execute_float },");
        w.println("    { \"executeDouble\", \"(ILjava/lang/Object;[Ljava/lang/Object;Ljava/lang/Class;)D\",       (void*)native_execute_double },");
        w.println("    { \"executeObject\", \"(ILjava/lang/Object;[Ljava/lang/Object;Ljava/lang/Class;)Ljava/lang/Object;\", (void*)native_execute_object }");
        w.println("};");
        w.println();

        w.println("/* Register native methods */");
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
     * Generates JNI_OnLoad
     */
    private void emitJNIOnLoad(PrintWriter w) {
        w.println("/* JNI_OnLoad - initialize on library load */");
        w.println("JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {");
        w.println("    JNIEnv* env = NULL;");
        w.println("    if ((*vm)->GetEnv(vm, (void**)&env, JNI_VERSION_1_8) != JNI_OK) {");
        w.println("        return JNI_ERR;");
        w.println("    }");
        w.println();
        w.println("    // Initialize frame memory pool");
        w.println("    frame_pool_init();");
        w.println();
        w.println("    // Initialize VM method lookup table (for direct VM-to-VM calls)");
        w.println("    vm_init_method_lookup();");
        w.println();
        if (encryptStrings) {
            w.println("    // Initialize string pool (decrypt all strings)");
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