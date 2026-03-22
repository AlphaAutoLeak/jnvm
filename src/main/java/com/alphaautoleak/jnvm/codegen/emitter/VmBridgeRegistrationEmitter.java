package com.alphaautoleak.jnvm.codegen.emitter;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

final class VmBridgeRegistrationEmitter {

    private VmBridgeRegistrationEmitter() {
    }

    static void emitRegisterNatives(PrintWriter w, String bridgeClass) {
        List<String> nativeRegistrations = new ArrayList<>();
        nativeRegistrations.add("{ \"__jnvm$registerClassNatives\", \"(Ljava/lang/Class;)V\",       (void*)native_register_class_natives }");
        nativeRegistrations.add("{ \"executeVoid\",   \"(I[Ljava/lang/Object;)V\",                  (void*)native_execute_void }");
        nativeRegistrations.add("{ \"executeInt\",    \"(I[Ljava/lang/Object;)I\",                  (void*)native_execute_int }");
        nativeRegistrations.add("{ \"executeLong\",   \"(I[Ljava/lang/Object;)J\",                  (void*)native_execute_long }");
        nativeRegistrations.add("{ \"executeFloat\",  \"(I[Ljava/lang/Object;)F\",                  (void*)native_execute_float }");
        nativeRegistrations.add("{ \"executeDouble\", \"(I[Ljava/lang/Object;)D\",                  (void*)native_execute_double }");
        nativeRegistrations.add("{ \"executeObject\", \"(I[Ljava/lang/Object;)Ljava/lang/Object;\", (void*)native_execute_object }");

        w.println("/* JNI method registration table */");
        w.println("static JNINativeMethod native_methods[] = {");
        for (int i = 0; i < nativeRegistrations.size(); i++) {
            String line = nativeRegistrations.get(i);
            if (i + 1 < nativeRegistrations.size()) {
                w.println("    " + line + ",");
            } else {
                w.println("    " + line);
            }
        }
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
}
