package com.alphaautoleak.jnvm.codegen.emitter.instruction;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;
import java.security.SecureRandom;

/**
 * Base instruction classes
 */
public class BaseInstructions {
    private static final int HANDLER_POLY_SALT = new SecureRandom().nextInt();

    private static int pickVariant(int opcode, String name, int count) {
        if (count <= 1) {
            return 0;
        }
        int x = HANDLER_POLY_SALT ^ (opcode * 0x9e3779b9) ^ (name == null ? 0 : name.hashCode());
        x ^= (x >>> 16);
        x *= 0x7feb352d;
        x ^= (x >>> 15);
        x *= 0x846ca68b;
        x ^= (x >>> 16);
        return (x & 0x7fffffff) % count;
    }

    private static void emitCodeLine(PrintWriter w, String code) {
        if (!code.isEmpty()) {
            if (code.contains("jint ") || code.contains("jlong ") || code.contains("jfloat ") ||
                    code.contains("jdouble ") || code.contains("jobject ") || code.contains("jbyte ") ||
                    code.contains("jchar ") || code.contains("jshort ")) {
                w.println("                { " + code + " }");
            } else {
                w.println("                " + code);
            }
        }
    }
    
    /**
     * Simple instruction - single line code (no metadata needed)
     */
    public static class SimpleInstruction extends Instruction {
        private final String code;
        
        public SimpleInstruction(int opcode, String name, String code) {
            super(opcode, name);
            this.code = code;
        }
        
        @Override
        protected void generateBody(PrintWriter w) {
            emitCodeLine(w, code);
            pcIncBreak(w);
        }
        
        @Override
        public boolean needsMeta() {
            return false;
        }
    }

    /**
     * Polymorphic simple instruction - choose one semantic-equivalent template at generation time.
     */
    public static class PolymorphicSimpleInstruction extends Instruction {
        private final String[] variants;

        public PolymorphicSimpleInstruction(int opcode, String name, String... variants) {
            super(opcode, name);
            if (variants == null || variants.length == 0) {
                throw new IllegalArgumentException("variants must not be empty");
            }
            this.variants = variants;
        }

        @Override
        protected void generateBody(PrintWriter w) {
            int vid = pickVariant(opcode, name, variants.length);
            emitCodeLine(w, variants[vid]);
            pcIncBreak(w);
        }

        @Override
        public boolean needsMeta() {
            return false;
        }
    }
    
    /**
     * Meta instruction - uses metadata (metadata required)
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
            return true;
        }
    }
}
