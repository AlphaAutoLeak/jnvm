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
    
    /** Outputs comment block */
    protected void emitComment(String comment) {
        emit("/* " + comment + " */");
    }
    
    /** Outputs single-line comment */
    protected void emitLineComment(String comment) {
        emit("// " + comment);
    }
    
    /** Outputs function signature */
    protected void emitFunction(String returnType, String name, String params) {
        emit(returnType + " " + name + "(" + params + ") {");
    }
    
    /** Ends function */
    protected void endFunction() {
        emit("}");
    }
    
    /** Outputs return statement */
    protected void emitReturn(String value) {
        emit("    return " + value + ";");
    }
    
    /** Outputs return; */
    protected void emitReturn() {
        emit("    return;");
    }
    
    /** Outputs if statement */
    protected void emitIf(String condition) {
        emit("    if(" + condition + "){");
    }
    
    /** Outputs else */
    protected void emitElse() {
        emit("    } else {");
    }
    
    /** Ends if/else block */
    protected void endIf() {
        emit("    }");
    }
    
    /** Outputs for loop */
    protected void emitFor(String init, String cond, String update) {
        emit("    for(" + init + "; " + cond + "; " + update + "){");
    }
    
    /** Ends for loop */
    protected void endFor() {
        emit("    }");
    }
    
    /** Outputs switch statement */
    protected void emitSwitch(String expr) {
        emit("    switch(" + expr + "){");
    }
    
    /** Outputs case */
    protected void emitCase(String value) {
        emit("        case " + value + ":");
    }
    
    /** Outputs break */
    protected void emitBreak() {
        emit("            break;");
    }
    
    /** Ends switch */
    protected void endSwitch() {
        emit("    }");
    }
    
    /** Closes writer */
    public void close() {
        w.close();
    }
    
    /** Generates code - implemented by subclasses */
    public abstract void generate() throws IOException;
}
