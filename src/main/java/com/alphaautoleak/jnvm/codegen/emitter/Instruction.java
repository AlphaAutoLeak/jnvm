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
     * 生成 case 分支代码（传统 switch-case 方式）
     */
    public void generate(PrintWriter w) {
        w.printf("            case 0x%02x: /* %s */\n", opcode, comment);
        generateBody(w);
        w.println("                break;");
    }
    
    /**
     * 生成 computed goto 标签和代码
     * 默认实现：调用 generateBodyWithoutPcInc()，然后根据 needsPcIncrement() 决定是否 pc++
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
     * 是否需要 pc++（默认 false，因为 generateBody 通常已经调用了 pcIncBreak）
     */
    protected boolean needsPcIncrement() {
        return false;
    }
    
    /**
     * 生成指令体（子类实现）
     */
    protected abstract void generateBody(PrintWriter w);
    
    /**
     * 生成指令体（不含 pc++）
     * 默认实现直接调用 generateBody，假设子类在 generateBody 中已经处理了 pc++
     * 如果子类的 generateBody 调用了 pcIncBreak，应该覆盖此方法避免重复
     */
    protected void generateBodyWithoutPcInc(PrintWriter w) {
        generateBody(w);
    }
    
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
    
    /**
     * 是否需要元数据（默认 false，只有带操作数的指令需要）
     * 这是性能优化关键：避免对简单指令进行不必要的元数据查找
     */
    public boolean needsMeta() {
        return false;
    }
}
