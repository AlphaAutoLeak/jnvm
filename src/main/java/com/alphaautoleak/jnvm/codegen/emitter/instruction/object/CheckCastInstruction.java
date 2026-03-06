package com.alphaautoleak.jnvm.codegen.emitter.instruction.object;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * CHECKCAST instruction
 */
public class CheckCastInstruction extends Instruction {
    public CheckCastInstruction() {
        super(0xc0, "CHECKCAST");
    }
    
    @Override
    public boolean needsMeta() {
        return true;  // 需要类信息
    }

    @Override
    protected void generateBody(PrintWriter w) {
        w.println("                // Checkcast: no action needed, JVM handles at runtime");
        pcIncBreak(w);
    }
}
