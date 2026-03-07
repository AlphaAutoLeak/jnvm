package com.alphaautoleak.jnvm.codegen.emitter.types;

import java.io.PrintWriter;

/**
 * 生成 MetaType 枚举和 MetaEntry 结构定义
 */
public class MetaType {
    
    public static void generate(PrintWriter w) {
        emitMetaTypeEnum(w);
        emitMetaEntry(w);
    }
    
    private static void emitMetaTypeEnum(PrintWriter w) {
        w.println("/* 元数据类型 */");
        w.println("typedef enum {");
        w.println("    META_NONE = 0,");
        w.println("    META_INT,        // 整数常量");
        w.println("    META_LONG,       // 长整数常量");
        w.println("    META_FLOAT,      // 浮点常量");
        w.println("    META_DOUBLE,     // 双精度常量");
        w.println("    META_STRING,     // 字符串常量");
        w.println("    META_CLASS,      // 类引用 (new, checkcast, etc)");
        w.println("    META_FIELD,      // 字段引用 (owner, name, desc)");
        w.println("    META_METHOD,     // 方法引用 (owner, name, desc)");
        w.println("    META_INVOKE_DYNAMIC, // invokedynamic");
        w.println("    META_JUMP,       // 跳转偏移");
        w.println("    META_SWITCH,     // switch 表");
        w.println("    META_LOCAL,      // 局部变量索引");
        w.println("    META_IINC,       // iinc (index, const)");
        w.println("    META_NEWARRAY,   // newarray 类型");
        w.println("    META_TYPE,       // 类型描述符");
        w.println("} MetaType;");
        w.println();
    }
    
    private static void emitMetaEntry(PrintWriter w) {
        w.println("/* 元数据条目 - 指令的操作数 */");
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
        w.println("    // META_STRING: 字符串索引和长度");
        w.println("    int strIdx;");
        w.println("    int strLen;");
        w.println("    // META_CLASS: 类名索引和长度");
        w.println("    int classIdx;");
        w.println("    int classLen;");
        w.println("    // META_FIELD, META_METHOD: 引用信息");
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
