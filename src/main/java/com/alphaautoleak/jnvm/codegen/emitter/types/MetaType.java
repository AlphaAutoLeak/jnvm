package com.alphaautoleak.jnvm.codegen.emitter.types;

import java.io.PrintWriter;

/**
 * Generates MetaType enum and MetaEntry struct definitions
 */
public class MetaType {
    
    public static void generate(PrintWriter w) {
        emitMetaTypeEnum(w);
        emitMetaEntry(w);
    }
    
    private static void emitMetaTypeEnum(PrintWriter w) {
        w.println("/* Metadata type */");
        w.println("typedef enum {");
        w.println("    META_NONE = 0,");
        w.println("    META_INT,        // integer constant");
        w.println("    META_LONG,       // long constant");
        w.println("    META_FLOAT,      // float constant");
        w.println("    META_DOUBLE,     // double constant");
        w.println("    META_STRING,     // string constant");
        w.println("    META_CLASS,      // class reference (new, checkcast, etc)");
        w.println("    META_FIELD,      // field reference (owner, name, desc)");
        w.println("    META_METHOD,     // method reference (owner, name, desc)");
        w.println("    META_INVOKE_DYNAMIC, // invokedynamic");
        w.println("    META_JUMP,       // jump offset");
        w.println("    META_SWITCH,     // switch table");
        w.println("    META_LOCAL,      // local variable index");
        w.println("    META_IINC,       // iinc (index, const)");
        w.println("    META_NEWARRAY,   // newarray type");
        w.println("    META_TYPE,       // type descriptor");
        w.println("} MetaType;");
        w.println();
    }

    private static void emitMetaEntry(PrintWriter w) {
        w.println("/* Metadata entry - instruction operand */");
        w.println("typedef struct {");
        w.println("    MetaType type;");
        w.println("    // META_INT, META_LOCAL, META_NEWARRAY");
        w.println("    jint intVal;");
        w.println("    // META_LONG");
        w.println("    jlong longVal;");
        w.println("    // META_FLOAT");
        w.println("    jfloat floatVal;");
        w.println("    // META_DOUBLE");
        w.println("    jdouble doubleVal;");
        w.println("    // META_JUMP");
        w.println("    int jumpOffset;");
        w.println("    // META_STRING: string index and length");
        w.println("    int strIdx;");
        w.println("    int strLen;");
        w.println("    // META_CLASS: class name index and length");
        w.println("    int classIdx;");
        w.println("    int classLen;");
        w.println("    // META_FIELD, META_METHOD: reference info");
        w.println("    int ownerIdx;");
        w.println("    int ownerLen;");
        w.println("    int nameIdx;");
        w.println("    int nameLen;");
        w.println("    int descIdx;");
        w.println("    int descLen;");
        w.println("    // META_METHOD: pre-parsed invoke meta (optimization)");
        w.println("    int argCount;           // argument count (pre-computed)");
        w.println("    char returnTypeChar;    // return type char (pre-computed)");
        w.println("    int argTypesIdx;        // pre-parsed arg types string index (e.g., \"IJB\" for int, long, boolean)");
        w.println("    // META_INVOKE_DYNAMIC");
        w.println("    int bsmIdx;");
        w.println("    // META_IINC");
        w.println("    int iincIndex;");
        w.println("    int iincConst;");
        w.println("    // META_SWITCH");
        w.println("    int switchLow;");
        w.println("    int switchHigh;");
        w.println("    int* switchOffsets;");
        w.println("    int* switchKeys;        // LOOKUPSWITCH keys (NULL for TABLESWITCH)");
        w.println("    // META_TYPE (multianewarray)");
        w.println("    int dims;");
        w.println("} MetaEntry;");
        w.println();
    }
}
