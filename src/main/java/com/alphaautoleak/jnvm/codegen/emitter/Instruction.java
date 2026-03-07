package com.alphaautoleak.jnvm.codegen.emitter;

import java.io.PrintWriter;

/**
 * Base class for JVM instructions
 * Each instruction generates its own C code implementation
 */
public abstract class Instruction {
    
    protected final int opcode;
    protected final String name;
    protected final String comment;
    
    public Instruction(int opcode, String name) {
        this.opcode = opcode;
        this.name = name;
        this.comment = name;
    }
    
    public Instruction(int opcode, String name, String comment) {
        this.opcode = opcode;
        this.name = name;
        this.comment = comment;
    }
    
    /**
     * Generates case branch code (traditional switch-case)
     */
    public void generate(PrintWriter w) {
        w.printf("            case 0x%02x: /* %s */\n", opcode, comment);
        generateBody(w);
        w.println("                break;");
    }
    
    /**
     * Generates computed goto label and code
     * Default: calls generateBodyWithoutPcInc(), then pc++ based on needsPcIncrement()
     */
    public void generateComputedGoto(PrintWriter w) {
        w.printf("        OP_%02x:  /* %s */\n", opcode, comment);
        generateBodyWithoutPcInc(w);
        if (needsPcIncrement()) {
            w.println("            frame.pc++;");
        }
        w.println("            DISPATCH_NEXT;");
    }
    
    /**
     * Whether pc++ is needed (default false, generateBody usually handles it)
     */
    protected boolean needsPcIncrement() {
        return false;
    }
    
    /**
     * Generates instruction body (subclass implementation)
     */
    protected abstract void generateBody(PrintWriter w);
    
    /**
     * Generates instruction body (without pc++)
     * Default: calls generateBody, assuming subclass handles pc++
     * Override this if generateBody calls pcIncBreak to avoid duplication
     */
    protected void generateBodyWithoutPcInc(PrintWriter w) {
        generateBody(w);
    }
    
    /**
     * Generates pc++ (break is auto-added by generate())
     */
    protected void pcIncBreak(PrintWriter w) {
        w.println("                frame.pc++;");
    }
    
    /**
     * Returns the opcode
     */
    public int getOpcode() {
        return opcode;
    }
    
    /**
     * Returns the instruction name
     */
    public String getName() {
        return name;
    }
    
    /**
     * Whether metadata is needed (default false, only instructions with operands need it)
     * Performance optimization: avoid unnecessary metadata lookup for simple instructions
     */
    public boolean needsMeta() {
        return false;
    }
}
