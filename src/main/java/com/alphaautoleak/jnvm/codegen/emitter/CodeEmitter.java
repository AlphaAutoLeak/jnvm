package com.alphaautoleak.jnvm.codegen.emitter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * 代码生成器基类 - 提供通用工具方法
 */
public abstract class CodeEmitter {
    
    protected final PrintWriter w;
    
    public CodeEmitter(File dir, String filename) throws IOException {
        File file = new File(dir, filename);
        this.w = new PrintWriter(new FileWriter(file));
    }
    
    /** 输出一行代码 */
    protected void emit(String line) {
        w.println(line);
    }
    
    /** 输出空行 */
    protected void emit() {
        w.println();
    }
    
    /** 输出格式化代码 */
    protected void emitf(String fmt, Object... args) {
        w.printf(fmt, args);
    }
    
    /** 开始头文件保护 */
    protected void beginGuard(String name) {
        emit("#ifndef " + name);
        emit("#define " + name);
        emit();
    }
    
    /** 结束头文件保护 */
    protected void endGuard() {
        emit("#endif");
    }
    
    /** 输出注释块 */
    protected void emitComment(String comment) {
        emit("/* " + comment + " */");
    }
    
    /** 输出单行注释 */
    protected void emitLineComment(String comment) {
        emit("// " + comment);
    }
    
    /** 输出函数签名 */
    protected void emitFunction(String returnType, String name, String params) {
        emit(returnType + " " + name + "(" + params + ") {");
    }
    
    /** 结束函数 */
    protected void endFunction() {
        emit("}");
    }
    
    /** 输出 return 语句 */
    protected void emitReturn(String value) {
        emit("    return " + value + ";");
    }
    
    /** 输出 return; */
    protected void emitReturn() {
        emit("    return;");
    }
    
    /** 输出 if 语句 */
    protected void emitIf(String condition) {
        emit("    if(" + condition + "){");
    }
    
    /** 输出 else */
    protected void emitElse() {
        emit("    } else {");
    }
    
    /** 结束 if/else 块 */
    protected void endIf() {
        emit("    }");
    }
    
    /** 输出 for 循环 */
    protected void emitFor(String init, String cond, String update) {
        emit("    for(" + init + "; " + cond + "; " + update + "){");
    }
    
    /** 结束 for 循环 */
    protected void endFor() {
        emit("    }");
    }
    
    /** 输出 switch 语句 */
    protected void emitSwitch(String expr) {
        emit("    switch(" + expr + "){");
    }
    
    /** 输出 case */
    protected void emitCase(String value) {
        emit("        case " + value + ":");
    }
    
    /** 输出 break */
    protected void emitBreak() {
        emit("            break;");
    }
    
    /** 结束 switch */
    protected void endSwitch() {
        emit("    }");
    }
    
    /** 关闭写入器 */
    public void close() {
        w.close();
    }
    
    /** 生成代码 - 子类实现 */
    public abstract void generate() throws IOException;
}
