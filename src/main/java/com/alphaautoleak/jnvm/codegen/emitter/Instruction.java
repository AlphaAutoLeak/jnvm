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
     * Generates case branch code (switch-case mode)
     */
    public void generate(PrintWriter w) {
        w.printf("            case 0x%02x: /* %s */\n", opcode, comment);
        generateBody(w);
        w.println("                break;");
    }
    
    /**
     * Generates computed goto label and code
     */
    public void generateComputedGoto(PrintWriter w) {
        w.printf("        OP_%02x:  /* %s */\n", opcode, comment);
        generateBody(w);
        if (needsPcIncrement()) {
            w.println("            frame.pc++;");
        }
        w.println("            DISPATCH_NEXT;");
    }
    
    /**
     * Generates instruction body (subclass implementation)
     */
    protected abstract void generateBody(PrintWriter w);
    
    /**
     * Generates pc++ instruction
     */
    protected void pcIncBreak(PrintWriter w) {
        w.println("            frame.pc++;");
    }
    
    /**
     * Whether pc++ is needed after generateBody (default false)
     */
    protected boolean needsPcIncrement() {
        return false;
    }
    
    public int getOpcode() {
        return opcode;
    }
    
    public String getName() {
        return name;
    }
    
    /**
     * Whether metadata is needed (default false)
     */
    public boolean needsMeta() {
        return false;
    }
}
