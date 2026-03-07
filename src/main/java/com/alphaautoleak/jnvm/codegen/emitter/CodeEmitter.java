package com.alphaautoleak.jnvm.codegen.emitter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Base code generator - provides utility methods
 */
public abstract class CodeEmitter {
    
    protected final PrintWriter w;
    
    public CodeEmitter(File dir, String filename) throws IOException {
        File file = new File(dir, filename);
        this.w = new PrintWriter(new FileWriter(file));
    }
    
    /** Outputs a line of code */
    protected void emit(String line) {
        w.println(line);
    }
    
    /** Outputs a blank line */
    protected void emit() {
        w.println();
    }
    
    /** Outputs formatted code */
    protected void emitf(String fmt, Object... args) {
        w.printf(fmt, args);
    }
    
    /** Begins header guard */
    protected void beginGuard(String name) {
        emit("#ifndef " + name);
        emit("#define " + name);
        emit();
    }
    
    /** Ends header guard */
    protected void endGuard() {
        emit("#endif");
    }
    
    /** Closes writer */
    public void close() {
        w.close();
    }
    
    /** Generates code - implemented by subclasses */
    public abstract void generate() throws IOException;
}
