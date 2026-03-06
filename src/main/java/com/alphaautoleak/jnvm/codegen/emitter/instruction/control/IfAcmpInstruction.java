package com.alphaautoleak.jnvm.codegen.emitter.instruction.control;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * IF_ACMP conditional jump instruction
 * jumpOffset is now an absolute PC value
 */
public class IfAcmpInstruction extends Instruction {
    private final String op;

    public IfAcmpInstruction(int opcode, String name, String op) {
        super(opcode, name);
        this.op = op;
    }

    @Override
    protected void generateBody(PrintWriter w) {
        w.println("                { jobject b = frame.stack[--frame.sp].l, a = frame.stack[--frame.sp].l;");
        w.println("                  if (a " + op + " b) frame.pc = meta->jumpOffset;");
        w.println("                  else frame.pc++; }");
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
        w.println("            { jobject b = frame.stack[--frame.sp].l, a = frame.stack[--frame.sp].l;");
        w.println("              if (a " + op + " b) { frame.pc = meta->jumpOffset; DISPATCH_NEXT; }");
        w.println("              else { frame.pc++; DISPATCH_NEXT; } }");
    }
}
