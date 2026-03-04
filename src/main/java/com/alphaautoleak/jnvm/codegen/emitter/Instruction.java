package com.alphaautoleak.jnvm.codegen.emitter;

import java.io.PrintWriter;

/**
 * JVM 指令基类
 * 每个指令负责生成自己的 C 代码实现
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
     * 生成 case 分支代码（子类可覆盖）
     */
    public void generate(PrintWriter w) {
        w.printf("            case 0x%02x: /* %s */\n", opcode, comment);
        generateBody(w);
        w.println("                break;");
    }
    
    /**
     * 生成指令体（子类实现）
     */
    protected abstract void generateBody(PrintWriter w);
    
    /**
     * 生成 pc++ （break 由 generate() 自动添加）
     */
    protected void pcIncBreak(PrintWriter w) {
        w.println("                frame.pc++;");
    }
    
    /**
     * 获取 opcode
     */
    public int getOpcode() {
        return opcode;
    }
    
    /**
     * 获取指令名
     */
    public String getName() {
        return name;
    }
}
