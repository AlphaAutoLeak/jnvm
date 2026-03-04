package com.alphaautoleak.jnvm.codegen.emitter;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * 生成 vm_bridge.c - JNI 桥接层
 */
public class VmBridgeGenerator {
    
    private final File dir;
    private final int methodCount;
    
    public VmBridgeGenerator(File dir, int methodCount) {
        this.dir = dir;
        this.methodCount = methodCount;
    }
    
    public void generate() throws IOException {
        try (PrintWriter w = new PrintWriter(new java.io.FileWriter(new File(dir, "vm_bridge.c")))) {
            w.println("#include \"vm_types.h\"");
            w.println("#include \"vm_data.h\"");
            w.println("#include \"vm_interpreter.h\"");
            w.println();
            
            emitJNIOnLoad(w);
            emitExecute(w);
        }
    }
    
    private void emitJNIOnLoad(PrintWriter w) {
        w.println("JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {");
        w.println("    printf(\"[JNVM] Native VM loaded. " + methodCount + " methods protected.\\n\");");
        w.println("    return JNI_VERSION_1_8;");
        w.println("}");
        w.println();
    }
    
    private void emitExecute(PrintWriter w) {
        w.println("JNIEXPORT jobject JNICALL");
        w.println("Java_com_alphaautoleak_jnvm_runtime_VMBridge_execute(JNIEnv* env, jclass cls,");
        w.println("                                                    jint methodId, jobject instance,");
        w.println("                                                    jobjectArray args) {");
        w.println("    return vm_execute_method(env, methodId, instance, args);");
        w.println("}");
    }
}
