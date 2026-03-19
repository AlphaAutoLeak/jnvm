package com.alphaautoleak.jnvm.codegen.emitter.instruction.arithmetic;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * Negation instruction (INEG, LNEG, FNEG, DNEG) - 64-bit only
 */
public class NegInstruction extends Instruction {
    private final String type;

    public NegInstruction(int opcode, String name, String type) {
        super(opcode, name);
        this.type = type;
    }

    @Override
    protected void generateBody(PrintWriter w) {
        if ("i".equals(type)) {
            w.println("                frame.stack[frame.sp-1].i = (jint)(0u - (uint32_t)frame.stack[frame.sp-1].i);");
        } else if ("j".equals(type)) {
            w.println("                frame.stack[frame.sp-1].j = (jlong)(0ull - (uint64_t)frame.stack[frame.sp-1].j);");
        } else {
            w.println("                frame.stack[frame.sp-1]." + type + " = -frame.stack[frame.sp-1]." + type + ";");
        }
        pcIncBreak(w);
    }
}
