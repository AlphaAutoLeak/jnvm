package com.alphaautoleak.jnvm.codegen.emitter;

import java.io.PrintWriter;

final class VmBridgeOnLoadEmitter {

    private VmBridgeOnLoadEmitter() {
    }

    static void emitJNIOnLoad(PrintWriter w, boolean encryptStrings) {
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
        if (encryptStrings) {
            w.println("    // Initialize string pool (decrypt all strings)");
            w.println("    vm_init_strings();");
            w.println();
        }
        w.println("    // Reconstruct VM method table from segmented storage");
        w.println("    vm_init_method_table();");
        w.println();
        w.println("    // Decode obfuscated metadata once at startup");
        w.println("    vm_init_meta_all();");
        w.println();
        w.println("    // Initialize VM method lookup table (for direct VM-to-VM calls)");
        w.println("    vm_init_method_lookup();");
        w.println();
        w.println("    // Pre-initialize boxed primitive unbox cache");
        w.println("    vm_init_unbox_cache(env);");
        w.println();
        w.println("    if (register_native_methods(env) != JNI_OK) {");
        w.println("        return JNI_ERR;");
        w.println("    }");
        w.println();
        w.println("    return JNI_VERSION_1_8;");
        w.println("}");
    }
}
