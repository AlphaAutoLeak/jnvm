package com.alphaautoleak.jnvm.codegen.emitter.instruction.stack;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;
import com.alphaautoleak.jnvm.codegen.emitter.instruction.BaseInstructions;
import com.alphaautoleak.jnvm.codegen.emitter.instruction.InstructionRegistry;

import java.io.PrintWriter;

/**
 * Stack operation instructions
 */
public class StackInstructions {
    
    /** DUP instruction */
    public static class DupInstruction extends Instruction {
        public DupInstruction(int opcode, String name) {
            super(opcode, name);
        }
        
        @Override
        protected void generateBody(PrintWriter w) {
            w.println("                frame.stack[frame.sp] = frame.stack[frame.sp - 1];");
            w.println("                frame.sp++;");
            pcIncBreak(w);
        }
    }
    
    /** DUP_X1 instruction */
    public static class DupX1Instruction extends Instruction {
        public DupX1Instruction() {
            super(0x5a, "DUP_X1");
        }
        
        @Override
        protected void generateBody(PrintWriter w) {
            w.println("                frame.stack[frame.sp] = frame.stack[frame.sp - 1];");
            w.println("                frame.stack[frame.sp - 1] = frame.stack[frame.sp - 2];");
            w.println("                frame.stack[frame.sp - 2] = frame.stack[frame.sp];");
            w.println("                frame.sp++;");
            pcIncBreak(w);
        }
    }
    
    /** DUP2 instruction */
    public static class Dup2Instruction extends Instruction {
        public Dup2Instruction() {
            super(0x5c, "DUP2");
        }
        
        @Override
        protected void generateBody(PrintWriter w) {
            w.println("                frame.stack[frame.sp] = frame.stack[frame.sp - 2];");
            w.println("                frame.stack[frame.sp + 1] = frame.stack[frame.sp - 1];");
            w.println("                frame.sp += 2;");
            pcIncBreak(w);
        }
    }
    
    /** SWAP instruction */
    public static class SwapInstruction extends Instruction {
        public SwapInstruction() {
            super(0x5f, "SWAP");
        }
        
        @Override
        protected void generateBody(PrintWriter w) {
            w.println("                { VMValue tmp = frame.stack[frame.sp - 1];");
            w.println("                  frame.stack[frame.sp - 1] = frame.stack[frame.sp - 2];");
            w.println("                  frame.stack[frame.sp - 2] = tmp; }");
            pcIncBreak(w);
        }
    }
    
    /**
     * Register all stack operation instructions
     */
    public static void registerAll(InstructionRegistry registry) {
        registry.register(new BaseInstructions.SimpleInstruction(0x57, "POP", "frame.sp--;"));
        registry.register(new BaseInstructions.SimpleInstruction(0x58, "POP2", "frame.sp -= 2;"));
        registry.register(new DupInstruction(0x59, "DUP"));
        registry.register(new DupX1Instruction());
        registry.register(new Dup2Instruction());
        registry.register(new SwapInstruction());
    }
}