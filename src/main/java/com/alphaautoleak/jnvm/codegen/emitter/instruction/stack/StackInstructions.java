package com.alphaautoleak.jnvm.codegen.emitter.instruction.stack;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;
import com.alphaautoleak.jnvm.codegen.emitter.instruction.BaseInstructions;
import com.alphaautoleak.jnvm.codegen.emitter.instruction.InstructionRegistry;

import java.io.PrintWriter;

/**
 * Stack operation instructions (64-bit only - all types occupy 1 slot)
 */
public class StackInstructions {
    
    /** DUP instruction - duplicates top stack value */
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
    
    /** DUP_X1 instruction - duplicates top and inserts below two values */
    public static class DupX1Instruction extends Instruction {
        public DupX1Instruction() {
            super(0x5a, "DUP_X1");
        }
        
        @Override
        protected void generateBody(PrintWriter w) {
            // 64-bit: ..., value2, value1 → ..., value1, value2, value1
            w.println("                { VMValue v1 = frame.stack[frame.sp - 1];");
            w.println("                  frame.stack[frame.sp] = v1;");
            w.println("                  frame.stack[frame.sp - 1] = frame.stack[frame.sp - 2];");
            w.println("                  frame.stack[frame.sp - 2] = v1; }");
            w.println("                frame.sp++;");
            pcIncBreak(w);
        }
    }
    
    /** DUP_X2 instruction - duplicates top and inserts below three values */
    public static class DupX2Instruction extends Instruction {
        public DupX2Instruction() {
            super(0x5b, "DUP_X2");
        }
        
        @Override
        protected void generateBody(PrintWriter w) {
            // 64-bit: ..., value3, value2, value1 → ..., value1, value3, value2, value1
            w.println("                { VMValue v1 = frame.stack[frame.sp - 1];");
            w.println("                  frame.stack[frame.sp] = v1;");
            w.println("                  frame.stack[frame.sp - 1] = frame.stack[frame.sp - 2];");
            w.println("                  frame.stack[frame.sp - 2] = frame.stack[frame.sp - 3];");
            w.println("                  frame.stack[frame.sp - 3] = v1; }");
            w.println("                frame.sp++;");
            pcIncBreak(w);
        }
    }
    
    /** DUP2 instruction - duplicates top two values */
    public static class Dup2Instruction extends Instruction {
        public Dup2Instruction() {
            super(0x5c, "DUP2");
        }
        
        @Override
        protected void generateBody(PrintWriter w) {
            // 64-bit: ..., value2, value1 → ..., value2, value1, value2, value1
            w.println("                frame.stack[frame.sp] = frame.stack[frame.sp - 2];");
            w.println("                frame.stack[frame.sp + 1] = frame.stack[frame.sp - 1];");
            w.println("                frame.sp += 2;");
            pcIncBreak(w);
        }
    }
    
    /** DUP2_X1 instruction - duplicates top two and inserts below one value */
    public static class Dup2X1Instruction extends Instruction {
        public Dup2X1Instruction() {
            super(0x5d, "DUP2_X1");
        }
        
        @Override
        protected void generateBody(PrintWriter w) {
            // 64-bit: ..., value3, value2, value1 → ..., value2, value1, value3, value2, value1
            w.println("                { VMValue v1 = frame.stack[frame.sp - 1];");
            w.println("                  VMValue v2 = frame.stack[frame.sp - 2];");
            w.println("                  frame.stack[frame.sp + 1] = v1;");
            w.println("                  frame.stack[frame.sp] = v2;");
            w.println("                  frame.stack[frame.sp - 1] = frame.stack[frame.sp - 3];");
            w.println("                  frame.stack[frame.sp - 2] = v1;");
            w.println("                  frame.stack[frame.sp - 3] = v2; }");
            w.println("                frame.sp += 2;");
            pcIncBreak(w);
        }
    }
    
    /** DUP2_X2 instruction - duplicates top two and inserts below two values */
    public static class Dup2X2Instruction extends Instruction {
        public Dup2X2Instruction() {
            super(0x5e, "DUP2_X2");
        }
        
        @Override
        protected void generateBody(PrintWriter w) {
            // 64-bit: ..., value4, value3, value2, value1 → ..., value2, value1, value4, value3, value2, value1
            w.println("                { VMValue v1 = frame.stack[frame.sp - 1];");
            w.println("                  VMValue v2 = frame.stack[frame.sp - 2];");
            w.println("                  frame.stack[frame.sp + 1] = v1;");
            w.println("                  frame.stack[frame.sp] = v2;");
            w.println("                  frame.stack[frame.sp - 1] = frame.stack[frame.sp - 3];");
            w.println("                  frame.stack[frame.sp - 2] = frame.stack[frame.sp - 4];");
            w.println("                  frame.stack[frame.sp - 3] = v1;");
            w.println("                  frame.stack[frame.sp - 4] = v2; }");
            w.println("                frame.sp += 2;");
            pcIncBreak(w);
        }
    }
    
    /** SWAP instruction - swaps top two values */
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
    
    /** POP2 instruction - pops two values (64-bit: always 2) */
    public static class Pop2Instruction extends Instruction {
        public Pop2Instruction() {
            super(0x58, "POP2");
        }
        
        @Override
        protected void generateBody(PrintWriter w) {
            w.println("                frame.sp -= 2;");
            pcIncBreak(w);
        }
    }
    
    /**
     * Register all stack operation instructions
     */
    public static void registerAll(InstructionRegistry registry) {
        registry.register(new BaseInstructions.SimpleInstruction(0x57, "POP", "frame.sp--;"));
        registry.register(new Pop2Instruction());
        registry.register(new DupInstruction(0x59, "DUP"));
        registry.register(new DupX1Instruction());
        registry.register(new DupX2Instruction());
        registry.register(new Dup2Instruction());
        registry.register(new Dup2X1Instruction());
        registry.register(new Dup2X2Instruction());
        registry.register(new SwapInstruction());
    }
}