package com.alphaautoleak.jnvm.codegen.emitter;

import com.alphaautoleak.jnvm.codegen.emitter.helper.VMHelper;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

final class VmInterpreterHeaderEmitter {

    private VmInterpreterHeaderEmitter() {
    }

    static void emit(File dir, Iterable<VMHelper> helpers, List<VmInterpreterWrapperSpec> wrapperSpecs) throws IOException {
        try (PrintWriter w = new PrintWriter(new java.io.FileWriter(new File(dir, "vm_interpreter.h")))) {
            w.println("#ifndef VM_INTERPRETER_H");
            w.println("#define VM_INTERPRETER_H");
            w.println("#include \"vm_types.h\"");
            w.println();
            w.println("// Frame memory pool initialization (called in JNI_OnLoad)");
            w.println("void frame_pool_init(void);");
            w.println();
            w.println("// VM method lookup initialization (called in JNI_OnLoad)");
            w.println("void vm_init_method_lookup(void);");
            w.println();

            for (VMHelper helper : helpers) {
                helper.generateHeader(w);
            }

            w.println();
            w.println("// Execution result struct (for return value and type)");
            w.println("typedef struct {");
            w.println("    VMValue value;");
            w.println("    char returnType;  // 'V', 'I', 'J', 'F', 'D', 'L'");
            w.println("} ExecuteResult;");
            w.println();

            for (VmInterpreterWrapperSpec spec : wrapperSpecs) {
                w.println(spec.returnType + " " + spec.functionName + "(JNIEnv* env, int methodId, jobjectArray args);");
            }
            w.println();
            w.println("// Internal: direct VM-to-VM call with pre-built locals");
            w.println("ExecuteResult vm_execute_common(JNIEnv* env, int methodId, jobjectArray args, VMValue* directLocals, int directLocalSlots, jclass callerClass);");
            w.println("#endif");
        }
    }
}
