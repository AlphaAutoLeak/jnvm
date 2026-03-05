package com.alphaautoleak.jnvm.codegen.emitter;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * 生成 vm_bridge.c - JNI 桥接层
 * 使用 RegisterNatives 方式注册本地方法
 */
public class VmBridgeGenerator {

    private final File dir;
    private final String bridgeClass;

    public VmBridgeGenerator(File dir, String bridgeClass) {
        this.dir = dir;
        this.bridgeClass = bridgeClass;
    }

    public void generate() throws IOException {
        try (PrintWriter w = new PrintWriter(new java.io.FileWriter(new File(dir, "vm_bridge.c")))) {
            w.println("#include \"vm_types.h\"");
            w.println("#include \"vm_data.h\"");
            w.println("#include \"vm_interpreter.h\"");
            w.println();

            emitExecuteFunction(w);
            emitRegisterNatives(w);
            emitJNIOnLoad(w);
        }
    }

    /**
     * 生成实际的 execute 函数实现
     */
    private void emitExecuteFunction(PrintWriter w) {
        w.println("/* 本地方法实现 */");
        w.println("static jobject JNICALL native_execute(JNIEnv* env, jclass cls,");
        w.println("                                     jint methodId, jobject instance,");
        w.println("                                     jobjectArray args) {");
        w.println("    return vm_execute_method(env, methodId, instance, args);");
        w.println("}");
        w.println();
    }

    /**
     * 生成 RegisterNatives 表和注册函数
     */
    private void emitRegisterNatives(PrintWriter w) {
        // JNI 方法签名: (int, Object, Object[])Object
        w.println("/* JNI 方法注册表 */");
        w.println("static JNINativeMethod native_methods[] = {");
        w.println("    { \"execute\", \"(ILjava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;\", (void*)native_execute }");
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
        w.println();
    }

    /**
     * 生成 JNI_OnLoad，在加载时注册本地方法
     */
    private void emitJNIOnLoad(PrintWriter w) {
        w.println("JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {");
        w.println("    JNIEnv* env = NULL;");
        w.println();
        w.println("    if ((*vm)->GetEnv(vm, (void**)&env, JNI_VERSION_1_8) != JNI_OK) {");
        w.println("        return JNI_ERR;");
        w.println("    }");
        w.println();
        w.println("    if (register_native_methods(env) != JNI_OK) {");
        w.println("        return JNI_ERR;");
        w.println("    }");
        w.println();
        w.println("    return JNI_VERSION_1_8;");
        w.println("}");
    }
}
