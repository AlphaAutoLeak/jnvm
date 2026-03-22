package com.alphaautoleak.jnvm.codegen.emitter;

import java.io.PrintWriter;
import java.util.List;

final class VmInterpreterWrapperEmitter {

    private VmInterpreterWrapperEmitter() {
    }

    static void emit(PrintWriter w, List<VmInterpreterWrapperSpec> wrapperSpecs) {
        for (VmInterpreterWrapperSpec spec : wrapperSpecs) {
            w.println("__attribute__((hot))");
            w.println(spec.returnType + " " + spec.functionName + "(JNIEnv* env, int methodId, jobjectArray args) {");
            w.println("    ExecuteResult r = vm_execute_common(env, methodId, args, NULL, 0, NULL);");
            w.println("    " + spec.returnStatement);
            w.println("}");
            w.println();
        }
    }
}
