package com.alphaautoleak.jnvm.codegen.emitter;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * 生成 vm_types.h - VM 类型定义
 * 
 * 新的指令格式设计：
 * - 每条指令由 opcode + 操作数组成
 * - 操作数直接存储在该指令后面的元数据数组中
 * - 不再使用常量池，指令直接引用元数据索引
 */
public class VmTypesGenerator {
    
    private final File dir;
    
    public VmTypesGenerator(File dir) {
        this.dir = dir;
    }
    
    public void generate() throws IOException {
        try (PrintWriter w = new PrintWriter(new java.io.FileWriter(new File(dir, "vm_types.h")))) {
            w.println("#ifndef VM_TYPES_H");
            w.println("#define VM_TYPES_H");
            w.println();
            w.println("#include <jni.h>");
            w.println("#include <stdint.h>");
            w.println("#include <string.h>");
            w.println("#include <stdlib.h>");
            w.println();

            // Value union - 栈值
            w.println("/* 栈值 - 统一 64 位宽 */");
            w.println("typedef union {");
            w.println("    jint     i;");
            w.println("    jlong    j;");
            w.println("    jfloat   f;");
            w.println("    jdouble  d;");
            w.println("    jobject  l;");
            w.println("    int64_t  raw;");
            w.println("} VMValue;");
            w.println();

            // 帧结构
            w.println("/* 执行帧 */");
            w.println("typedef struct {");
            w.println("    int pc;           // 程序计数器");
            w.println("    int sp;           // 栈指针");
            w.println("    VMValue* stack;   // 操作栈");
            w.println("    VMValue* locals;  // 局部变量表");
            w.println("} VMFrame;");
            w.println();

            // 元数据类型枚举
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

            // 元数据条目 - 使用扁平化字段
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

            // 方法结构
            w.println("/* 方法定义 */");
            w.println("typedef struct {");
            w.println("    int methodId;");
            w.println("    int maxStack;");
            w.println("    int maxLocals;");
            w.println("    uint8_t* bytecode;       // 加密的指令序列");
            w.println("    int bytecodeLen;");
            w.println("    uint8_t key[32];         // ChaCha20 密钥");
            w.println("    uint8_t nonce[12];       // ChaCha20 nonce");
            w.println("    MetaEntry* metadata;     // 元数据数组");
            w.println("    int metadataCount;");
            w.println("    int* pcToMetaIdx;        // PC -> 元数据索引映射");
            w.println("    int descIdx;             // 方法描述符索引");
            w.println("    int descLen;             // 方法描述符长度");
            w.println("    int isStatic;            // 是否静态方法");
            w.println("} VMMethod;");
            w.println();

            // 字符串池
            w.println("/* 字符串池 */");
            w.println("typedef struct {");
            w.println("    const char* data;");
            w.println("    int len;");
            w.println("} VMString;");
            w.println();

            // Bootstrap 方法参数类型
            w.println("/* Bootstrap 方法参数类型 */");
            w.println("typedef enum {");
            w.println("    BSM_ARG_STRING = 0,");
            w.println("    BSM_ARG_INTEGER,");
            w.println("    BSM_ARG_LONG,");
            w.println("    BSM_ARG_FLOAT,");
            w.println("    BSM_ARG_DOUBLE,");
            w.println("    BSM_ARG_METHOD_TYPE,");
            w.println("    BSM_ARG_METHOD_HANDLE");
            w.println("} BsmArgType;");
            w.println();

            // Bootstrap 方法参数
            w.println("/* Bootstrap 方法参数 */");
            w.println("typedef struct {");
            w.println("    BsmArgType type;");
            w.println("    int strIdx;       // 字符串/MethodType/MethodHandle 的字符串索引");
            w.println("    int intVal;");
            w.println("    long longVal;");
            w.println("    float floatVal;");
            w.println("    double doubleVal;");
            w.println("    int handleTag;    // MethodHandle tag (仅用于 METHOD_HANDLE)");
            w.println("} BsmArg;");
            w.println();

            // Bootstrap 方法定义
            w.println("/* Bootstrap 方法定义 */");
            w.println("typedef struct {");
            w.println("    int handleTag;           // MethodHandle tag (REF_invokeStatic=6, etc)");
            w.println("    int ownerIdx;            // bootstrap 方法所属类");
            w.println("    int nameIdx;             // bootstrap 方法名");
            w.println("    int descIdx;             // bootstrap 方法描述符");
            w.println("    BsmArg* args;            // bootstrap 参数数组");
            w.println("    int argCount;            // 参数数量");
            w.println("} VMBootstrapMethod;");
            w.println();

            // 全局 Bootstrap 方法表
            w.println("/* 全局 Bootstrap 方法表 */");
            w.println("extern VMBootstrapMethod vm_bootstrap_methods[];");
            w.println("extern const int vm_bootstrap_count;");
            w.println();

            w.println("#endif");
        }
    }
}
