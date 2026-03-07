package com.alphaautoleak.jnvm.asm;

/**
 * Corresponds to exception_table entry in JVM Code attribute.
 * startPc / endPc / handlerPc are offsets in custom bytecode.
 */
public class ExceptionEntry {

    /** try block start PC (inclusive) */
    private int startPc;

    /** try block end PC (exclusive) */
    private int endPc;

    /** catch handler start PC */
    private int handlerPc;

    /**
     * Caught exception type (internal name, e.g. "java/lang/Exception")
     * null means catch-all (finally)
     */
    private String catchType;

    /** Index in custom constant pool (catchType class ref) */
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