package com.alphaautoleak.jnvm.asm;

/**
 * Metadata entry for bytecode instruction operands.
 * Each instruction may have associated metadata stored separately from bytecode.
 */
public class MetaEntry {
    public MetaType type = MetaType.META_NONE;
    
    // META_INT, META_LOCAL, META_NEWARRAY
    public int intVal;
    
    // META_LONG
    public long longVal;
    
    // META_FLOAT
    public float floatVal;
    
    // META_DOUBLE
    public double doubleVal;
    
    // META_STRING
    public int strIdx;
    public int strLen;
    
    // META_CLASS
    public int classIdx;
    public int classLen;
    
    // META_FIELD, META_METHOD
    public int ownerIdx;
    public int ownerLen;
    public int nameIdx;
    public int nameLen;
    public int descIdx;
    public int descLen;
    
    // META_INVOKE_DYNAMIC
    public int bsmIdx;
    
    // META_JUMP
    public int jumpOffset;
    
    // META_IINC
    public int iincIndex;
    public int iincConst;
    
    // META_SWITCH
    public int switchLow;
    public int switchHigh;
    public int[] switchKeys;
    public int[] switchOffsets;
    
    // META_TYPE (multianewarray)
    public int dims;
}
