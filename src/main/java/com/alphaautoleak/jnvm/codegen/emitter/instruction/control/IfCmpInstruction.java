package com.alphaautoleak.jnvm.codegen.emitter.instruction.control;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * IF_ICMP 条件跳转指令 (IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE)
 */
public class IfCmpInstruction extends Instruction {
    private final String op;

    public IfCmpInstruction(int opcode, String name, String op) {
        super(opcode, name);
        this.op = op;
    }

    @Override
    protected void generateBody(PrintWriter w) {
        w.println("                { jint b = frame.stack[--frame.sp].i, a = frame.stack[--frame.sp].i;");
        w.println("                  if (a " + op + " b) frame.pc += meta->jumpOffset;");
        w.println("                  else frame.pc++; }");
        w.println("                break;");
    }

    @Override
    public void generate(PrintWriter w) {
        w.printf("            case 0x%02x: /* %s */\n", opcode, comment);
        generateBody(w);
    }
}
