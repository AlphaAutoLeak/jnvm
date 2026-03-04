package com.alphaautoleak.jnvm.codegen.emitter.instruction;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * 栈操作指�? */
public class StackInstructions {
    
    /** DUP 指令 */
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
    
    /** DUP_X1 指令 */
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
    
    /** DUP2 指令 */
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
    
    /** SWAP 指令 */
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
     * 注册所有栈操作指令
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
