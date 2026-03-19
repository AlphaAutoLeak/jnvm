package com.alphaautoleak.jnvm.codegen.emitter.instruction.arithmetic;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * Shift instruction (ISHL, LSHL, ISHR, LSHR, IUSHR, LUSHR) - 64-bit only
 */
public class ShiftInstruction extends Instruction {
    private final String type;
    private final String op;
    private final boolean unsigned;

    public ShiftInstruction(int opcode, String name, String type, String op, boolean unsigned) {
        super(opcode, name);
        this.type = type;
        this.op = op;
        this.unsigned = unsigned;
    }

    @Override
    protected void generateBody(PrintWriter w) {
        if ("i".equals(type)) {
            w.println("                { jint s = frame.stack[frame.sp-1].i & 31;");
            if ("<<".equals(op)) {
                // Java int left shift: wrap-around two's complement semantics.
                w.println("                  frame.stack[frame.sp-2].i = (jint)((uint32_t)frame.stack[frame.sp-2].i << s); }");
            } else if (unsigned) {
                w.println("                  frame.stack[frame.sp-2].i = (jint)((uint32_t)frame.stack[frame.sp-2].i >> s); }");
            } else {
                w.println("                  frame.stack[frame.sp-2].i = frame.stack[frame.sp-2].i >> s; }");
            }
        } else {
            w.println("                { jint s = frame.stack[frame.sp-1].i & 63;");
            if ("<<".equals(op)) {
                // Java long left shift: wrap-around two's complement semantics.
                w.println("                  frame.stack[frame.sp-2].j = (jlong)((uint64_t)frame.stack[frame.sp-2].j << s); }");
            } else if (unsigned) {
                w.println("                  frame.stack[frame.sp-2].j = (jlong)((uint64_t)frame.stack[frame.sp-2].j >> s); }");
            } else {
                w.println("                  frame.stack[frame.sp-2].j = frame.stack[frame.sp-2].j >> s; }");
            }
        }
        w.println("                frame.sp--;");
        pcIncBreak(w);
    }
}
