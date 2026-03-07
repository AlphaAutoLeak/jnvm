package com.alphaautoleak.jnvm.codegen.emitter.instruction.control;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * Return instruction (RETURN, IRETURN, LRETURN, FRETURN, DRETURN, ARETURN)
 * 
 * Stores return value in execResult.value, sets execResult.returnType, then jumps to method_exit
 */
public class ReturnInstruction extends Instruction {
    private final String returnType; // "void", "int", "long", "float", "double", "object"

    public ReturnInstruction(int opcode, String name, String returnType) {
        super(opcode, name);
        this.returnType = returnType;
    }

    @Override
    protected void generateBody(PrintWriter w) {
        switch (returnType) {
            case "void":
                w.println("                goto method_exit;");
                break;
            case "int":
                w.println("                execResult.value.i = frame.stack[--frame.sp].i;");
                w.println("                execResult.returnType = 'I';");
                w.println("                goto method_exit;");
                break;
            case "long":
                w.println("                execResult.value.j = frame.stack[--frame.sp].j;");
                w.println("                execResult.returnType = 'J';");
                w.println("                goto method_exit;");
                break;
            case "float":
                w.println("                execResult.value.f = frame.stack[--frame.sp].f;");
                w.println("                execResult.returnType = 'F';");
                w.println("                goto method_exit;");
                break;
            case "double":
                w.println("                execResult.value.d = frame.stack[--frame.sp].d;");
                w.println("                execResult.returnType = 'D';");
                w.println("                goto method_exit;");
                break;
            case "object":
            default:
                w.println("                execResult.value.l = frame.stack[--frame.sp].l;");
                w.println("                execResult.returnType = 'L';");
                w.println("                goto method_exit;");
                break;
        }
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
        generateBody(w);
    }
}
