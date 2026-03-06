package com.alphaautoleak.jnvm.codegen.emitter.instruction.control;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * IINC instruction
 */
public class IincInstruction extends Instruction {
    public IincInstruction() {
        super(0x84, "IINC");
    }
    
    @Override
    public boolean needsMeta() {
        return true;  // 需要索引和常量
    }

    @Override
    protected void generateBody(PrintWriter w) {
        w.println("                { int _idx = meta->iincIndex; int _c = meta->iincConst;");
        w.println("                  int _old = frame.locals[_idx].i;");
        w.println("                  frame.locals[_idx].i += _c;");
        w.println("                  VM_LOG(\"IINC: local[%d] += %d = %d -> %d\\n\", _idx, _c, _old, frame.locals[_idx].i);");
        w.println("                }");
        pcIncBreak(w);
    }
}