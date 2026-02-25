package com.alphaautoleak.jnvm.asm;

/**
 * 对应 JVM Code 属性中的 exception_table 条目。
 * startPc / endPc / handlerPc 是自定义字节码中的偏移量。
 */
public class ExceptionEntry {

    /** try 块起始 PC（含） */
    private int startPc;

    /** try 块结束 PC（不含） */
    private int endPc;

    /** catch handler 起始 PC */
    private int handlerPc;

    /**
     * 捕获的异常类型（内部名，e.g. "java/lang/Exception"）
     * null 表示 catch-all（finally）
     */
    private String catchType;

    /** 对应自定义常量池中的索引（catchType 的 class ref） */
    private int catchTypeCpIndex;

    public ExceptionEntry() {}

    public ExceptionEntry(int startPc, int endPc, int handlerPc, String catchType) {
        this.startPc = startPc;
        this.endPc = endPc;
        this.handlerPc = handlerPc;
        this.catchType = catchType;
        this.catchTypeCpIndex = -1;
    }

    public int getStartPc() { return startPc; }
    public void setStartPc(int startPc) { this.startPc = startPc; }

    public int getEndPc() { return endPc; }
    public void setEndPc(int endPc) { this.endPc = endPc; }

    public int getHandlerPc() { return handlerPc; }
    public void setHandlerPc(int handlerPc) { this.handlerPc = handlerPc; }

    public String getCatchType() { return catchType; }
    public void setCatchType(String catchType) { this.catchType = catchType; }

    public int getCatchTypeCpIndex() { return catchTypeCpIndex; }
    public void setCatchTypeCpIndex(int catchTypeCpIndex) { this.catchTypeCpIndex = catchTypeCpIndex; }

    @Override
    public String toString() {
        return String.format("ExceptionEntry{start=%d, end=%d, handler=%d, catch=%s}",
                startPc, endPc, handlerPc, catchType != null ? catchType : "<any>");
    }
}