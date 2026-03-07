package com.alphaautoleak.jnvm.codegen.emitter.helper;

import java.io.PrintWriter;

/**
 * Base class for VM helper function generators
 */
public abstract class VMHelper {
    
    /**
     * Generates function declarations to header file
     */
    public abstract void generateHeader(PrintWriter w);
    
    /**
     * Generates function implementations to source file
     */
    public abstract void generateSource(PrintWriter w);
    
    /**
     * Gets required header files
     */
    public abstract String[] getIncludes();
    
    /**
     * Helper method: prints multiple lines of code
     */
    protected void emit(PrintWriter w, String... lines) {
        for (String line : lines) {
            w.println(line);
        }
    }
}
