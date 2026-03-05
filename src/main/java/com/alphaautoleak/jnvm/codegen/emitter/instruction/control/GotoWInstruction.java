package com.alphaautoleak.jnvm.codegen.emitter.instruction.control;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * GOTO_W instruction (0xc8)
 * Wide branch instruction with 32-bit signed offset
 * jumpOffset is now an absolute PC value (same as GOTO)
 */
public class GotoWInstruction extends Instruction {
    public GotoWInstruction() {
        super(0xc8, "GOTO_W");
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
