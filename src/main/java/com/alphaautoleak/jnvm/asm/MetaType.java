package com.alphaautoleak.jnvm.asm;

/**
 * Metadata type enumeration for bytecode operands
 */
public enum MetaType {
    META_NONE(0),
    META_INT(1),
    META_LONG(2),
    META_FLOAT(3),
    META_DOUBLE(4),
    META_STRING(5),
    META_CLASS(6),
    META_FIELD(7),
    META_METHOD(8),
    META_INVOKE_DYNAMIC(9),
    META_JUMP(10),
    META_SWITCH(11),
    META_LOCAL(12),
    META_IINC(13),
    META_NEWARRAY(14),
    META_TYPE(15);

    public final int value;
    
    MetaType(int v) { 
        this.value = v; 
    }
}
