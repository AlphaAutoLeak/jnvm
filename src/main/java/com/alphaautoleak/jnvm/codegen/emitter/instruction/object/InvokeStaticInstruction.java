package com.alphaautoleak.jnvm.codegen.emitter.instruction.object;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * INVOKESTATIC instruction
 */
public class InvokeStaticInstruction extends Instruction {
    public InvokeStaticInstruction() {
        super(0xb8, "INVOKESTATIC");
    }
    
    @Override
    public boolean needsMeta() {
        return true;  // 需要方法信息
    }

    @Override
    protected void generateBody(PrintWriter w) {
        InvokeHelper.generate(w, true);
    }

    @Override
    public void generateComputedGoto(PrintWriter w) {
        InvokeHelper.generateComputedGoto(w, true, opcode, comment);
    }
}