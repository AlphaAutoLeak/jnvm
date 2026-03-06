package com.alphaautoleak.jnvm.codegen.emitter.instruction;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * Base instruction classes
 */
public class BaseInstructions {
    
    /**
     * Simple instruction - single line code (不需要元数据)
     */
    public static class SimpleInstruction extends Instruction {
        private final String code;
        
        public SimpleInstruction(int opcode, String name, String code) {
            super(opcode, name);
            this.code = code;
        }
        
        @Override
        protected void generateBody(PrintWriter w) {
            if (!code.isEmpty()) {
                if (code.contains("jint ") || code.contains("jlong ") || code.contains("jfloat ") || 
                    code.contains("jdouble ") || code.contains("jobject ") || code.contains("jbyte ") ||
                    code.contains("jchar ") || code.contains("jshort ")) {
                    w.println("                { " + code + " }");
                } else {
                    w.println("                " + code);
                }
            }
            pcIncBreak(w);
        }
        
        @Override
        public boolean needsMeta() {
            return false;  // 简单指令不需要元数据
        }
    }
    
    /**
     * Meta instruction - uses metadata (需要元数据)
     */
    public static class MetaInstruction extends Instruction {
        protected final String code;
        
        public MetaInstruction(int opcode, String name, String code) {
            super(opcode, name);
            this.code = code;
        }
        
        @Override
        protected void generateBody(PrintWriter w) {
            w.println("                " + code);
            pcIncBreak(w);
        }
        
        @Override
        public boolean needsMeta() {
            return true;  // 需要元数据
        }
    }
}