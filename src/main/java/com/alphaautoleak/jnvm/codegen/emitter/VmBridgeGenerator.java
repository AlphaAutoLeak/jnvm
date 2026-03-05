package com.alphaautoleak.jnvm.codegen.emitter;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * 生成 vm_bridge.c - JNI 桥接层
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
            
            emitJNIOnLoad(w);
            emitExecute(w);
        }
    }
    
    private void emitJNIOnLoad(PrintWriter w) {
        w.println("JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {");
        w.println("    return JNI_VERSION_1_8;");
        w.println("}");
        w.println();
    }
    
    private void emitExecute(PrintWriter w) {
        // 将包名路径转换为 JNI 函数名格式: com/example/Foo -> Java_com_example_Foo
        String jniName = "Java_" + bridgeClass.replace('/', '_');
        
        w.println("JNIEXPORT jobject JNICALL");
        w.println(jniName + "_execute(JNIEnv* env, jclass cls,");
        w.println("                                                    jint methodId, jobject instance,");
        w.println("                                                    jobjectArray args) {");
        w.println("    return vm_execute_method(env, methodId, instance, args);");
        w.println("}");
    }
}
