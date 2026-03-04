package com.alphaautoleak.jnvm.codegen.emitter.instruction.control;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * Return instruction (RETURN, IRETURN, LRETURN, FRETURN, DRETURN, ARETURN)
 */
public class ReturnInstruction extends Instruction {
    private final boolean hasValue;

    public ReturnInstruction(int opcode, String name, boolean hasValue) {
        super(opcode, name);
        this.hasValue = hasValue;
    }

    @Override
    protected void generateBody(PrintWriter w) {
        if (hasValue) {
            w.println("                return frame.stack[--frame.sp].l;");
        } else {
            w.println("                return NULL;");
        }
    }

    @Override
    public void generate(PrintWriter w) {
        w.printf("            case 0x%02x: /* %s */\n", opcode, comment);
        generateBody(w);
    }
}