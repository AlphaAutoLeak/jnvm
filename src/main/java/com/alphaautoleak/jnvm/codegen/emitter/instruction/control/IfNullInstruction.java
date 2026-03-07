package com.alphaautoleak.jnvm.codegen.emitter.instruction.control;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * IFNULL/IFNONNULL instruction
 * jumpOffset is now an absolute PC value
 */
public class IfNullInstruction extends Instruction {
    private final String condition;

    public IfNullInstruction(int opcode, String name, String condition) {
        super(opcode, name);
        this.condition = condition;
    }
    
    @Override
    public boolean needsMeta() {
        return true;
    }

    @Override
    protected void generateBody(PrintWriter w) {
        w.println("                if (frame.stack[--frame.sp].l " + condition + ") frame.pc = meta->jumpOffset;");
        w.println("                else frame.pc++;");
        w.println("                break;");
    }

    @Override
    protected boolean needsPcIncrement() {
        return false;
    }

    @Override
    public void generate(PrintWriter w) {
        w.printf("            case 0x%02x: /* %s */\n", opcode, comment);
        generateBody(w);
    }

    @Override
    public void generateComputedGoto(PrintWriter w) {
        w.printf("        OP_%02x:  /* %s */\n", opcode, comment);
        w.println("            if (frame.stack[--frame.sp].l " + condition + ") { frame.pc = meta->jumpOffset; DISPATCH_NEXT; }");
        w.println("            else { frame.pc++; DISPATCH_NEXT; }");
    }
}
