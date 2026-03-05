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
    
    /** DUP_X2 instruction */
    public static class DupX2Instruction extends Instruction {
        public DupX2Instruction() {
            super(0x5b, "DUP_X2");
        }
        
        @Override
        protected void generateBody(PrintWriter w) {
            // DUP_X2 has two forms:
            // Form 1 (value2 is cat1): ..., value3, value2, value1 → ..., value1, value3, value2, value1
            // Form 2 (value2 is cat2): ..., value2, value1 → ..., value1, value2, value1
            // We detect at runtime based on stackTypes
            w.println("                { VMType t2 = frame.stackTypes[frame.sp - 2];");
            w.println("                  if (t2 == TYPE_LONG || t2 == TYPE_DOUBLE) {");
            w.println("                      // Form 2: value2 is category 2");
            w.println("                      VMValue v1 = frame.stack[frame.sp - 1];");
            w.println("                      VMValue v2 = frame.stack[frame.sp - 2];");
            w.println("                      VMType t1 = frame.stackTypes[frame.sp - 1];");
            w.println("                      frame.stack[frame.sp] = v1;");
            w.println("                      frame.stack[frame.sp - 1] = v2;");
            w.println("                      frame.stack[frame.sp - 2] = v1;");
            w.println("                      frame.stackTypes[frame.sp] = t1;");
            w.println("                      frame.stackTypes[frame.sp - 1] = t2;");
            w.println("                      frame.stackTypes[frame.sp - 2] = t1;");
            w.println("                      frame.sp++;");
            w.println("                  } else {");
            w.println("                      // Form 1: value2 is category 1");
            w.println("                      VMValue v1 = frame.stack[frame.sp - 1];");
            w.println("                      VMValue v2 = frame.stack[frame.sp - 2];");
            w.println("                      VMValue v3 = frame.stack[frame.sp - 3];");
            w.println("                      VMType t1 = frame.stackTypes[frame.sp - 1];");
            w.println("                      VMType t2b = frame.stackTypes[frame.sp - 2];");
            w.println("                      VMType t3 = frame.stackTypes[frame.sp - 3];");
            w.println("                      frame.stack[frame.sp] = v1;");
            w.println("                      frame.stack[frame.sp - 1] = v3;");
            w.println("                      frame.stack[frame.sp - 2] = v2;");
            w.println("                      frame.stack[frame.sp - 3] = v1;");
            w.println("                      frame.stackTypes[frame.sp] = t1;");
            w.println("                      frame.stackTypes[frame.sp - 1] = t3;");
            w.println("                      frame.stackTypes[frame.sp - 2] = t2b;");
            w.println("                      frame.stackTypes[frame.sp - 3] = t1;");
            w.println("                      frame.sp++;");
            w.println("                  } }");
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
            // DUP2 has two forms:
            // Form 1 (top is cat1): ..., value2, value1 → ..., value2, value1, value2, value1
            // Form 2 (top is cat2): ..., value1 → ..., value1, value1
            // We detect at runtime based on stackTypes
            w.println("                { VMType topType = frame.stackTypes[frame.sp - 1];");
            w.println("                  if (topType == TYPE_LONG || topType == TYPE_DOUBLE) {");
            w.println("                      // Form 2: top is category 2, duplicate one value");
            w.println("                      frame.stack[frame.sp] = frame.stack[frame.sp - 1];");
            w.println("                      frame.stackTypes[frame.sp] = topType;");
            w.println("                      frame.sp++;");
            w.println("                  } else {");
            w.println("                      // Form 1: top is category 1, duplicate two values");
            w.println("                      frame.stack[frame.sp] = frame.stack[frame.sp - 2];");
            w.println("                      frame.stack[frame.sp + 1] = frame.stack[frame.sp - 1];");
            w.println("                      frame.stackTypes[frame.sp] = frame.stackTypes[frame.sp - 2];");
            w.println("                      frame.stackTypes[frame.sp + 1] = topType;");
            w.println("                      frame.sp += 2;");
            w.println("                  } }");
            pcIncBreak(w);
        }
    }
    
    /** DUP2_X1 instruction */
    public static class Dup2X1Instruction extends Instruction {
        public Dup2X1Instruction() {
            super(0x5d, "DUP2_X1");
        }
        
        @Override
        protected void generateBody(PrintWriter w) {
            // DUP2_X1 has two forms:
            // Form 1 (all cat1): ..., value3, value2, value1 → ..., value2, value1, value3, value2, value1
            // Form 2 (value1 is cat2): ..., value2, value1 → ..., value1, value2, value1
            // We detect at runtime based on stackTypes
            w.println("                { VMType topType = frame.stackTypes[frame.sp - 1];");
            w.println("                  if (topType == TYPE_LONG || topType == TYPE_DOUBLE) {");
            w.println("                      // Form 2: value1 is category 2 (long/double)");
            w.println("                      VMValue v1 = frame.stack[frame.sp - 1];");
            w.println("                      VMValue v2 = frame.stack[frame.sp - 2];");
            w.println("                      VMType t1 = frame.stackTypes[frame.sp - 1];");
            w.println("                      VMType t2 = frame.stackTypes[frame.sp - 2];");
            w.println("                      frame.stack[frame.sp] = v1;");
            w.println("                      frame.stack[frame.sp - 1] = v2;");
            w.println("                      frame.stack[frame.sp - 2] = v1;");
            w.println("                      frame.stackTypes[frame.sp] = t1;");
            w.println("                      frame.stackTypes[frame.sp - 1] = t2;");
            w.println("                      frame.stackTypes[frame.sp - 2] = t1;");
            w.println("                      frame.sp++;");
            w.println("                  } else {");
            w.println("                      // Form 1: all category 1");
            w.println("                      VMValue v1 = frame.stack[frame.sp - 1];");
            w.println("                      VMValue v2 = frame.stack[frame.sp - 2];");
            w.println("                      VMValue v3 = frame.stack[frame.sp - 3];");
            w.println("                      VMType t1 = frame.stackTypes[frame.sp - 1];");
            w.println("                      VMType t2 = frame.stackTypes[frame.sp - 2];");
            w.println("                      VMType t3 = frame.stackTypes[frame.sp - 3];");
            w.println("                      frame.stack[frame.sp + 1] = v1;");
            w.println("                      frame.stack[frame.sp] = v2;");
            w.println("                      frame.stack[frame.sp - 1] = v3;");
            w.println("                      frame.stack[frame.sp - 2] = v1;");
            w.println("                      frame.stack[frame.sp - 3] = v2;");
            w.println("                      frame.stackTypes[frame.sp + 1] = t1;");
            w.println("                      frame.stackTypes[frame.sp] = t2;");
            w.println("                      frame.stackTypes[frame.sp - 1] = t3;");
            w.println("                      frame.stackTypes[frame.sp - 2] = t1;");
            w.println("                      frame.stackTypes[frame.sp - 3] = t2;");
            w.println("                      frame.sp += 2;");
            w.println("                  } }");
            pcIncBreak(w);
        }
    }
    
    /** DUP2_X2 instruction */
    public static class Dup2X2Instruction extends Instruction {
        public Dup2X2Instruction() {
            super(0x5e, "DUP2_X2");
        }
        
        @Override
        protected void generateBody(PrintWriter w) {
            // DUP2_X2 has 4 forms depending on types:
            // Form 1 (all cat1): ..., v4, v3, v2, v1 → ..., v2, v1, v4, v3, v2, v1
            // Form 2 (v1 is cat2, v2/v3 cat1): ..., v3, v2, v1 → ..., v1, v3, v2, v1
            // Form 3 (v2 is cat2, v1 cat1): ..., v3, v2, v1 → ..., v2, v1, v3, v2, v1
            // Form 4 (both v1/v2 cat2): ..., v2, v1 → ..., v1, v2, v1
            w.println("                { VMType topType = frame.stackTypes[frame.sp - 1];");
            w.println("                  VMType nextType = frame.stackTypes[frame.sp - 2];");
            w.println("                  if (topType == TYPE_LONG || topType == TYPE_DOUBLE) {");
            w.println("                      if (nextType == TYPE_LONG || nextType == TYPE_DOUBLE) {");
            w.println("                          // Form 4: both v1 and v2 are category 2");
            w.println("                          VMValue v1 = frame.stack[frame.sp - 1];");
            w.println("                          VMValue v2 = frame.stack[frame.sp - 2];");
            w.println("                          VMType t1 = frame.stackTypes[frame.sp - 1];");
            w.println("                          VMType t2 = frame.stackTypes[frame.sp - 2];");
            w.println("                          frame.stack[frame.sp] = v1;");
            w.println("                          frame.stack[frame.sp - 1] = v2;");
            w.println("                          frame.stack[frame.sp - 2] = v1;");
            w.println("                          frame.stackTypes[frame.sp] = t1;");
            w.println("                          frame.stackTypes[frame.sp - 1] = t2;");
            w.println("                          frame.stackTypes[frame.sp - 2] = t1;");
            w.println("                          frame.sp++;");
            w.println("                      } else {");
            w.println("                          // Form 2: v1 is category 2, v2/v3 are category 1");
            w.println("                          VMValue v1 = frame.stack[frame.sp - 1];");
            w.println("                          VMValue v2 = frame.stack[frame.sp - 2];");
            w.println("                          VMValue v3 = frame.stack[frame.sp - 3];");
            w.println("                          VMType t1 = frame.stackTypes[frame.sp - 1];");
            w.println("                          VMType t2 = frame.stackTypes[frame.sp - 2];");
            w.println("                          VMType t3 = frame.stackTypes[frame.sp - 3];");
            w.println("                          frame.stack[frame.sp] = v1;");
            w.println("                          frame.stack[frame.sp - 1] = v3;");
            w.println("                          frame.stack[frame.sp - 2] = v2;");
            w.println("                          frame.stack[frame.sp - 3] = v1;");
            w.println("                          frame.stackTypes[frame.sp] = t1;");
            w.println("                          frame.stackTypes[frame.sp - 1] = t3;");
            w.println("                          frame.stackTypes[frame.sp - 2] = t2;");
            w.println("                          frame.stackTypes[frame.sp - 3] = t1;");
            w.println("                          frame.sp++;");
            w.println("                      }");
            w.println("                  } else if (nextType == TYPE_LONG || nextType == TYPE_DOUBLE) {");
            w.println("                      // Form 3: v2 is category 2, v1 is category 1");
            w.println("                      VMValue v1 = frame.stack[frame.sp - 1];");
            w.println("                      VMValue v2 = frame.stack[frame.sp - 2];");
            w.println("                      VMValue v3 = frame.stack[frame.sp - 3];");
            w.println("                      VMType t1 = frame.stackTypes[frame.sp - 1];");
            w.println("                      VMType t2 = frame.stackTypes[frame.sp - 2];");
            w.println("                      VMType t3 = frame.stackTypes[frame.sp - 3];");
            w.println("                      frame.stack[frame.sp + 1] = v1;");
            w.println("                      frame.stack[frame.sp] = v2;");
            w.println("                      frame.stack[frame.sp - 1] = v3;");
            w.println("                      frame.stack[frame.sp - 2] = v1;");
            w.println("                      frame.stack[frame.sp - 3] = v2;");
            w.println("                      frame.stackTypes[frame.sp + 1] = t1;");
            w.println("                      frame.stackTypes[frame.sp] = t2;");
            w.println("                      frame.stackTypes[frame.sp - 1] = t3;");
            w.println("                      frame.stackTypes[frame.sp - 2] = t1;");
            w.println("                      frame.stackTypes[frame.sp - 3] = t2;");
            w.println("                      frame.sp += 2;");
            w.println("                  } else {");
            w.println("                      // Form 1: all category 1");
            w.println("                      VMValue v1 = frame.stack[frame.sp - 1];");
            w.println("                      VMValue v2 = frame.stack[frame.sp - 2];");
            w.println("                      VMValue v3 = frame.stack[frame.sp - 3];");
            w.println("                      VMValue v4 = frame.stack[frame.sp - 4];");
            w.println("                      VMType t1 = frame.stackTypes[frame.sp - 1];");
            w.println("                      VMType t2 = frame.stackTypes[frame.sp - 2];");
            w.println("                      VMType t3 = frame.stackTypes[frame.sp - 3];");
            w.println("                      VMType t4 = frame.stackTypes[frame.sp - 4];");
            w.println("                      frame.stack[frame.sp + 1] = v1;");
            w.println("                      frame.stack[frame.sp] = v2;");
            w.println("                      frame.stack[frame.sp - 1] = v3;");
            w.println("                      frame.stack[frame.sp - 2] = v4;");
            w.println("                      frame.stack[frame.sp - 3] = v1;");
            w.println("                      frame.stack[frame.sp - 4] = v2;");
            w.println("                      frame.stackTypes[frame.sp + 1] = t1;");
            w.println("                      frame.stackTypes[frame.sp] = t2;");
            w.println("                      frame.stackTypes[frame.sp - 1] = t3;");
            w.println("                      frame.stackTypes[frame.sp - 2] = t4;");
            w.println("                      frame.stackTypes[frame.sp - 3] = t1;");
            w.println("                      frame.stackTypes[frame.sp - 4] = t2;");
            w.println("                      frame.sp += 2;");
            w.println("                  } }");
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
    
    /** POP2 instruction - pop one or two values depending on type */
    public static class Pop2Instruction extends Instruction {
        public Pop2Instruction() {
            super(0x58, "POP2");
        }
        
        @Override
        protected void generateBody(PrintWriter w) {
            // POP2 has two forms:
            // Form 1 (top is cat1): pop two category 1 values, sp -= 2
            // Form 2 (top is cat2): pop one category 2 value, sp -= 1
            w.println("                { VMType topType = frame.stackTypes[frame.sp - 1];");
            w.println("                  if (topType == TYPE_LONG || topType == TYPE_DOUBLE) {");
            w.println("                      frame.sp--;"); 
            w.println("                  } else {");
            w.println("                      frame.sp -= 2;");
            w.println("                  } }");
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
