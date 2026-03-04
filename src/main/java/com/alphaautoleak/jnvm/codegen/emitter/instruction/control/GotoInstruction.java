package com.alphaautoleak.jnvm.codegen.emitter.instruction.control;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * GOTO instruction
 * jumpOffset is now an absolute PC value
 */
public class GotoInstruction extends Instruction {
    public GotoInstruction() {
        super(0xa7, "GOTO");
    }

    @Override
    protected void generateBody(PrintWriter w) {
        w.println("                frame.pc = meta->jumpOffset;");
        w.println("                break;");
    }

    @Override
    public void generate(PrintWriter w) {
        w.printf("            case 0x%02x: /* %s */\n", opcode, comment);
        generateBody(w);
    }
}
