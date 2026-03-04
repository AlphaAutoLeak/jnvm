package com.alphaautoleak.jnvm.codegen.emitter.instruction.control;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * ATHROW instruction - throw exception
 */
public class AThrowInstruction extends Instruction {
    public AThrowInstruction() {
        super(0xbf, "ATHROW");
    }

    @Override
    protected void generateBody(PrintWriter w) {
        // Pop exception object from stack
        w.println("                jobject exception = frame.stack[--frame.sp].l;");
        w.println("                if (exception != NULL) {");
        w.println("                    (*env)->Throw(env, (jthrowable)exception);");
        w.println("                }");
        w.println("                return NULL;  // Exit interpreter loop");
    }
    
    @Override
    public void generate(PrintWriter w) {
        w.printf("            case 0x%02x: /* %s */\n", opcode, comment);
        generateBody(w);
        // No break - We return directly
    }
}
