package com.alphaautoleak.jnvm.codegen.emitter.instruction.object;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * INVOKEDYNAMIC 指令
 */
public class InvokeDynamicInstruction extends Instruction {
    public InvokeDynamicInstruction() {
        super(0xba, "INVOKEDYNAMIC");
    }

    @Override
    protected void generateBody(PrintWriter w) {
        w.println("                { // INVOKEDYNAMIC - Lambda 支持");
        w.println("                  jobject result = vm_invoke_dynamic(env, &frame, meta);");
        w.println("                  if (result != NULL) frame.stack[frame.sp++].l = result; }");
        w.println("                frame.pc++;");
    }
}
