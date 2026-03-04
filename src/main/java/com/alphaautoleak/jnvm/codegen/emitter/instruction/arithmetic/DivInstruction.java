package com.alphaautoleak.jnvm.codegen.emitter.instruction.arithmetic;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * 除法指令 (IDIV, LDIV, IREM, LREM)
 */
public class DivInstruction extends Instruction {
    private final String type;
    private final boolean isRem;

    public DivInstruction(int opcode, String name, String type) {
        this(opcode, name, type, false);
    }

    public DivInstruction(int opcode, String name, String type, boolean isRem) {
        super(opcode, name);
        this.type = type;
        this.isRem = isRem;
    }

    @Override
    protected void generateBody(PrintWriter w) {
        String op = isRem ? "%" : "/";
        w.println("                if (frame.stack[frame.sp-1]." + type + " != 0)");
        w.println("                    frame.stack[frame.sp-2]." + type + " " + op + "= frame.stack[frame.sp-1]." + type + ";");
        w.println("                frame.sp--;");
        pcIncBreak(w);
    }
}
