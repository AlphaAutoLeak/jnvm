package com.alphaautoleak.jnvm.codegen;

import com.alphaautoleak.jnvm.asm.*;
import com.alphaautoleak.jnvm.config.ProtectConfig;
import com.alphaautoleak.jnvm.crypto.CryptoUtils;
import com.alphaautoleak.jnvm.crypto.EncryptedMethodData;
import com.alphaautoleak.jnvm.crypto.StringEncryptor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * 生成所有 C 源文件 + build.zig
 */
public class NativeCodeGenerator {

    private final ProtectConfig config;
    private final List<EncryptedMethodData> methods;
    private final byte[] stringKey;

    public NativeCodeGenerator(ProtectConfig config, List<EncryptedMethodData> methods) {
        this.config = config;
        this.methods = methods;
        this.stringKey = StringEncryptor.generateStringKey();
    }

    /**
     * 生成所有文件
     */
    public void generate() throws IOException {
        File dir = config.getNativeDir();
        if (!dir.exists()) dir.mkdirs();

        System.out.println("[CODEGEN] Output directory: " + dir.getAbsolutePath());

        generateVmTypes(dir);
        System.out.println("  [+] vm_types.h");

        generateChacha20(dir);
        System.out.println("  [+] chacha20.h / chacha20.c");

        generateVmData(dir);
        System.out.println("  [+] vm_data.h / vm_data.c");

        generateVmInterpreter(dir);
        System.out.println("  [+] vm_interpreter.h / vm_interpreter.c");

        generateVmBridge(dir);
        System.out.println("  [+] vm_bridge.c");

        generateBuildZig(dir);
        System.out.println("  [+] build.zig");

        System.out.println("[CODEGEN] Generated " + 8 + " files.");
    }

    // ========================================================
    // vm_types.h — 共用类型定义
    // ========================================================
    private void generateVmTypes(File dir) throws IOException {
        try (PrintWriter w = new PrintWriter(new FileWriter(new File(dir, "vm_types.h")))) {
            w.println("#ifndef VM_TYPES_H");
            w.println("#define VM_TYPES_H");
            w.println();
            w.println("#include <jni.h>");
            w.println("#include <stdint.h>");
            w.println("#include <string.h>");
            w.println("#include <stdlib.h>");
            w.println();

            // Value union
            w.println("/* 栈值 — 统一 64 位宽 */");
            w.println("typedef union {");
            w.println("    jint     i;");
            w.println("    jlong    j;");
            w.println("    jfloat   f;");
            w.println("    jdouble  d;");
            w.println("    jobject  l;");
            w.println("    int64_t  raw;");
            w.println("} VMValue;");
            w.println();

            // CP Entry types
            w.println("/* 常量池条目类型 */");
            w.println("typedef enum {");
            w.println("    CP_INTEGER = 0,");
            w.println("    CP_LONG,");
            w.println("    CP_FLOAT,");
            w.println("    CP_DOUBLE,");
            w.println("    CP_STRING,");
            w.println("    CP_CLASS,");
            w.println("    CP_METHOD_REF,");
            w.println("    CP_IMETHOD_REF,");
            w.println("    CP_FIELD_REF,");
            w.println("    CP_INVOKE_DYNAMIC,");
            w.println("    CP_METHOD_HANDLE");
            w.println("} CPEntryType;");
            w.println();

            // CP Entry struct
            w.println("/* 常量池条目 */");
            w.println("typedef struct {");
            w.println("    CPEntryType type;");
            w.println("    union {");
            w.println("        jint     int_val;");
            w.println("        jlong    long_val;");
            w.println("        jfloat   float_val;");
            w.println("        jdouble  double_val;");
            w.println("        struct { const unsigned char* data; int len; } str;");
            w.println("        struct { const unsigned char* data; int len; } class_name;");
            w.println("        struct {");
            w.println("            const unsigned char* owner; int owner_len;");
            w.println("            const unsigned char* name;  int name_len;");
            w.println("            const unsigned char* desc;  int desc_len;");
            w.println("        } ref;");
            w.println("        struct {");
            w.println("            int bsm_index;");
            w.println("            const unsigned char* name;  int name_len;");
            w.println("            const unsigned char* desc;  int desc_len;");
            w.println("        } indy;");
            w.println("        struct {");
            w.println("            int tag;");
            w.println("            const unsigned char* owner; int owner_len;");
            w.println("            const unsigned char* name;  int name_len;");
            w.println("            const unsigned char* desc;  int desc_len;");
            w.println("        } handle;");
            w.println("    } val;");
            w.println("    /* 运行时缓存 */");
            w.println("    jclass    cached_class;");
            w.println("    jmethodID cached_method;");
            w.println("    jfieldID  cached_field;");
            w.println("    jstring   cached_string;");
            w.println("    int       resolved;");
            w.println("} VMCPEntry;");
            w.println();

            // Exception entry
            w.println("/* 异常表条目 */");
            w.println("typedef struct {");
            w.println("    int start_pc;");
            w.println("    int end_pc;");
            w.println("    int handler_pc;");
            w.println("    int catch_type_cp_index; /* -1 = catch-all */");
            w.println("} VMExceptionEntry;");
            w.println();

            // Bootstrap entry
            w.println("/* Bootstrap 方法条目 */");
            w.println("typedef struct {");
            w.println("    int tag;");
            w.println("    const unsigned char* owner; int owner_len;");
            w.println("    const unsigned char* name;  int name_len;");
            w.println("    const unsigned char* desc;  int desc_len;");
            w.println("    int arg_count;");
            w.println("    /* bootstrap 参数（简化：仅存字符串描述） */");
            w.println("    const unsigned char** arg_data;");
            w.println("    int* arg_lens;");
            w.println("} VMBootstrapEntry;");
            w.println();

            // Method descriptor
            w.println("/* 方法描述 */");
            w.println("typedef struct {");
            w.println("    int method_id;");
            w.println("    const unsigned char* owner; int owner_len;");
            w.println("    const unsigned char* name;  int name_len;");
            w.println("    const unsigned char* desc;  int desc_len;");
            w.println("    int access;");
            w.println("    int is_static;");
            w.println("    int is_synchronized;");
            w.println("    int max_stack;");
            w.println("    int max_locals;");
            w.println("    /* 加密字节码 */");
            w.println("    const unsigned char* encrypted_code;");
            w.println("    int code_length;");
            w.println("    const unsigned char* key;   /* 32 bytes */");
            w.println("    const unsigned char* nonce;  /* 12 bytes */");
            w.println("    /* 常量池 */");
            w.println("    VMCPEntry* cp;");
            w.println("    int cp_count;");
            w.println("    /* 异常表 */");
            w.println("    VMExceptionEntry* exceptions;");
            w.println("    int exception_count;");
            w.println("    /* Bootstrap */");
            w.println("    VMBootstrapEntry* bootstraps;");
            w.println("    int bootstrap_count;");
            w.println("    /* invokedynamic CallSite 缓存 */");
            w.println("    jobject* indy_cache;");
            w.println("    int indy_cache_count;");
            w.println("} VMMethodInfo;");
            w.println();

            // Frame
            w.println("/* 执行帧 */");
            w.println("typedef struct {");
            w.println("    VMValue*      stack;");
            w.println("    int           sp;");
            w.println("    VMValue*      locals;");
            w.println("    int           pc;");
            w.println("    VMMethodInfo* method;");
            w.println("    unsigned char* code; /* 解密后的字节码 */");
            w.println("} VMFrame;");
            w.println();

            w.println("#define VM_METHOD_COUNT " + methods.size());
            w.println();

            w.println("#endif /* VM_TYPES_H */");
        }
    }

    // ========================================================
    // chacha20.h / chacha20.c — 纯 C ChaCha20
    // ========================================================
    private void generateChacha20(File dir) throws IOException {
        // Header
        try (PrintWriter w = new PrintWriter(new FileWriter(new File(dir, "chacha20.h")))) {
            w.println("#ifndef CHACHA20_H");
            w.println("#define CHACHA20_H");
            w.println();
            w.println("#include <stdint.h>");
            w.println("#include <stddef.h>");
            w.println();
            w.println("void chacha20_decrypt(const uint8_t key[32], const uint8_t nonce[12],");
            w.println("                      uint32_t counter, const uint8_t* input,");
            w.println("                      uint8_t* output, size_t len);");
            w.println();
            w.println("#endif");
        }

        // Implementation
        try (PrintWriter w = new PrintWriter(new FileWriter(new File(dir, "chacha20.c")))) {
            w.println("#include \"chacha20.h\"");
            w.println("#include <string.h>");
            w.println();
            w.println("#define ROTL32(v, n) (((v) << (n)) | ((v) >> (32 - (n))))");
            w.println();
            w.println("static void quarter_round(uint32_t* s, int a, int b, int c, int d) {");
            w.println("    s[a] += s[b]; s[d] ^= s[a]; s[d] = ROTL32(s[d], 16);");
            w.println("    s[c] += s[d]; s[b] ^= s[c]; s[b] = ROTL32(s[b], 12);");
            w.println("    s[a] += s[b]; s[d] ^= s[a]; s[d] = ROTL32(s[d], 8);");
            w.println("    s[c] += s[d]; s[b] ^= s[c]; s[b] = ROTL32(s[b], 7);");
            w.println("}");
            w.println();
            w.println("static uint32_t le32(const uint8_t* p) {");
            w.println("    return (uint32_t)p[0] | ((uint32_t)p[1]<<8) |");
            w.println("           ((uint32_t)p[2]<<16) | ((uint32_t)p[3]<<24);");
            w.println("}");
            w.println();
            w.println("static void chacha20_block(const uint32_t input[16], uint32_t output[16]) {");
            w.println("    memcpy(output, input, 64);");
            w.println("    for (int i = 0; i < 10; i++) {");
            w.println("        quarter_round(output, 0,4,8,12);");
            w.println("        quarter_round(output, 1,5,9,13);");
            w.println("        quarter_round(output, 2,6,10,14);");
            w.println("        quarter_round(output, 3,7,11,15);");
            w.println("        quarter_round(output, 0,5,10,15);");
            w.println("        quarter_round(output, 1,6,11,12);");
            w.println("        quarter_round(output, 2,7,8,13);");
            w.println("        quarter_round(output, 3,4,9,14);");
            w.println("    }");
            w.println("    for (int i = 0; i < 16; i++) output[i] += input[i];");
            w.println("}");
            w.println();
            w.println("void chacha20_decrypt(const uint8_t key[32], const uint8_t nonce[12],");
            w.println("                      uint32_t counter, const uint8_t* input,");
            w.println("                      uint8_t* output, size_t len) {");
            w.println("    uint32_t state[16];");
            w.println("    state[0]=0x61707865; state[1]=0x3320646e;");
            w.println("    state[2]=0x79622d32; state[3]=0x6b206574;");
            w.println("    for (int i=0;i<8;i++) state[4+i]=le32(key+i*4);");
            w.println("    state[13]=le32(nonce); state[14]=le32(nonce+4); state[15]=le32(nonce+8);");
            w.println();
            w.println("    size_t off = 0;");
            w.println("    while (off < len) {");
            w.println("        state[12] = counter++;");
            w.println("        uint32_t ks[16];");
            w.println("        chacha20_block(state, ks);");
            w.println("        uint8_t* kb = (uint8_t*)ks;");
            w.println("        size_t chunk = (len - off < 64) ? (len - off) : 64;");
            w.println("        for (size_t i = 0; i < chunk; i++) {");
            w.println("            output[off+i] = input[off+i] ^ kb[i];");
            w.println("        }");
            w.println("        off += chunk;");
            w.println("    }");
            w.println("}");
        }
    }

    // ========================================================
    // vm_data.h / vm_data.c — 加密数据 + 常量池
    // ========================================================
    private void generateVmData(File dir) throws IOException {
        // Header
        try (PrintWriter w = new PrintWriter(new FileWriter(new File(dir, "vm_data.h")))) {
            w.println("#ifndef VM_DATA_H");
            w.println("#define VM_DATA_H");
            w.println();
            w.println("#include \"vm_types.h\"");
            w.println();
            w.println("/* 字符串解密密钥 */");
            w.println("extern const unsigned char vm_str_key[8];");
            w.println();
            w.println("/* 字符串解密函数 */");
            w.println("void vm_decrypt_str(const unsigned char* enc, int len, char* out);");
            w.println();
            w.println("/* 全局方法表 */");
            w.println("extern VMMethodInfo vm_methods[VM_METHOD_COUNT];");
            w.println();
            w.println("/* 初始化方法表（分配可变缓存等） */");
            w.println("void vm_data_init(void);");
            w.println("void vm_data_destroy(void);");
            w.println();
            w.println("#endif");
        }

        // Implementation
        try (PrintWriter w = new PrintWriter(new FileWriter(new File(dir, "vm_data.c")))) {
            w.println("#include \"vm_data.h\"");
            w.println("#include <stdlib.h>");
            w.println("#include <string.h>");
            w.println();

            // 字符串加密密钥
            w.println("const unsigned char vm_str_key[8] = " +
                    CryptoUtils.toCArrayLiteral(stringKey) + ";");
            w.println();

            // 字符串解密函数
            w.println("void vm_decrypt_str(const unsigned char* enc, int len, char* out) {");
            w.println("    for (int i = 0; i < len; i++) {");
            w.println("        out[i] = (char)((enc[i] - (i & 0xFF)) ^ vm_str_key[i % 8]);");
            w.println("    }");
            w.println("    out[len] = '\\0';");
            w.println("}");
            w.println();

            // ===== 每个方法的数据 =====
            for (EncryptedMethodData m : methods) {
                int id = m.getMethodId();
                String prefix = "m" + id;

                // 加密字节码
                w.println("/* Method " + id + ": " + m.getOwner() + "." + m.getName() + " */");
                w.println("static const unsigned char " + prefix + "_code[] = " +
                        CryptoUtils.toCArrayLiteral(m.getEncryptedBytecode()) + ";");

                // 密钥
                w.println("static const unsigned char " + prefix + "_key[32] = " +
                        CryptoUtils.toCArrayLiteral(m.getKey()) + ";");

                // Nonce
                w.println("static const unsigned char " + prefix + "_nonce[12] = " +
                        CryptoUtils.toCArrayLiteral(m.getNonce()) + ";");

                // 加密字符串数据
                emitEncryptedStrings(w, prefix, m);

                // 常量池数组
                emitConstantPool(w, prefix, m);

                // 异常表
                emitExceptionTable(w, prefix, m);

                // Bootstrap 表
                emitBootstrapTable(w, prefix, m);

                w.println();
            }

            // ===== 全局方法表 =====
            // ===== 在 generateVmData 方法中，全局方法表部分替换为 =====

// 全局方法表 — 也用函数初始化
            w.println("VMMethodInfo vm_methods[VM_METHOD_COUNT];");
            w.println();

            w.println("static void vm_init_methods(void) {");
            w.println("    memset(vm_methods, 0, sizeof(vm_methods));");

            for (int i = 0; i < methods.size(); i++) {
                EncryptedMethodData m = methods.get(i);
                String prefix = "m" + m.getMethodId();
                String base = "vm_methods[" + i + "]";

                w.println();
                w.println("    /* Method " + i + ": " + m.getOwner() + "." + m.getName() + " */");
                w.println("    " + prefix + "_cp_init();");
                w.println("    " + base + ".method_id = " + m.getMethodId() + ";");
                w.println("    " + base + ".owner = " + prefix + "_owner;");
                w.println("    " + base + ".owner_len = " + prefix + "_owner_len;");
                w.println("    " + base + ".name = " + prefix + "_name;");
                w.println("    " + base + ".name_len = " + prefix + "_name_len;");
                w.println("    " + base + ".desc = " + prefix + "_desc;");
                w.println("    " + base + ".desc_len = " + prefix + "_desc_len;");
                w.println("    " + base + ".access = " + m.getAccess() + ";");
                w.println("    " + base + ".is_static = " + (m.isStatic() ? 1 : 0) + ";");
                w.println("    " + base + ".is_synchronized = " + (m.isSynchronized() ? 1 : 0) + ";");
                w.println("    " + base + ".max_stack = " + m.getMaxStack() + ";");
                w.println("    " + base + ".max_locals = " + m.getMaxLocals() + ";");
                w.println("    " + base + ".encrypted_code = " + prefix + "_code;");
                w.println("    " + base + ".code_length = " + m.getOriginalLength() + ";");
                w.println("    " + base + ".key = " + prefix + "_key;");
                w.println("    " + base + ".nonce = " + prefix + "_nonce;");
                w.println("    " + base + ".cp = " + prefix + "_cp;");
                w.println("    " + base + ".cp_count = " + m.getConstantPool().size() + ";");

                if (m.getExceptionTable().isEmpty()) {
                    w.println("    " + base + ".exceptions = NULL;");
                } else {
                    w.println("    " + base + ".exceptions = " + prefix + "_exc;");
                }
                w.println("    " + base + ".exception_count = " + m.getExceptionTable().size() + ";");

                if (m.getBootstrapMethods().isEmpty()) {
                    w.println("    " + base + ".bootstraps = NULL;");
                } else {
                    w.println("    " + base + ".bootstraps = " + prefix + "_bsm;");
                }
                w.println("    " + base + ".bootstrap_count = " + m.getBootstrapMethods().size() + ";");
                w.println("    " + base + ".indy_cache = NULL;");
                w.println("    " + base + ".indy_cache_count = 0;");
            }

            w.println("}");
            w.println();

// vm_data_init
            w.println("void vm_data_init(void) {");
            w.println("    vm_init_methods();");
            w.println("    for (int i = 0; i < VM_METHOD_COUNT; i++) {");
            w.println("        int indy_count = 0;");
            w.println("        for (int j = 0; j < vm_methods[i].cp_count; j++) {");
            w.println("            if (vm_methods[i].cp[j].type == CP_INVOKE_DYNAMIC) indy_count++;");
            w.println("        }");
            w.println("        if (indy_count > 0) {");
            w.println("            vm_methods[i].indy_cache = (jobject*)calloc(indy_count, sizeof(jobject));");
            w.println("            vm_methods[i].indy_cache_count = indy_count;");
            w.println("        }");
            w.println("    }");
            w.println("}");
            w.println();
            w.println("void vm_data_destroy(void) {");
            w.println("    for (int i = 0; i < VM_METHOD_COUNT; i++) {");
            w.println("        if (vm_methods[i].indy_cache) {");
            w.println("            free(vm_methods[i].indy_cache);");
            w.println("            vm_methods[i].indy_cache = NULL;");
            w.println("        }");
            w.println("    }");
            w.println("}");
        }
    }

    /**
     * 发射加密字符串（owner, name, desc 以及常量池中的字符串）
     */
    private void emitEncryptedStrings(PrintWriter w, String prefix, EncryptedMethodData m) {
        // owner
        byte[] ownerEnc = StringEncryptor.encrypt(m.getOwner(), stringKey);
        w.println("static const unsigned char " + prefix + "_owner[] = " +
                CryptoUtils.toCArrayLiteral(ownerEnc) + ";");
        w.println("static const int " + prefix + "_owner_len = " + ownerEnc.length + ";");

        // name
        byte[] nameEnc = StringEncryptor.encrypt(m.getName(), stringKey);
        w.println("static const unsigned char " + prefix + "_name[] = " +
                CryptoUtils.toCArrayLiteral(nameEnc) + ";");
        w.println("static const int " + prefix + "_name_len = " + nameEnc.length + ";");

        // desc
        byte[] descEnc = StringEncryptor.encrypt(m.getDescriptor(), stringKey);
        w.println("static const unsigned char " + prefix + "_desc[] = " +
                CryptoUtils.toCArrayLiteral(descEnc) + ";");
        w.println("static const int " + prefix + "_desc_len = " + descEnc.length + ";");
    }

    /**
     * 发射常量池数组
     */
    private void emitConstantPool(PrintWriter w, String prefix, EncryptedMethodData m) {
        List<CPEntry> cp = m.getConstantPool();

        if (cp.isEmpty()) {
            w.println("static VMCPEntry " + prefix + "_cp[1] = {{0}};");
            w.println("static void " + prefix + "_cp_init(void) {}");
            return;
        }

        // 先发射所有字符串数据
        for (CPEntry e : cp) {
            String ePrefix = prefix + "_cp" + e.getIndex();
            switch (e.getType()) {
                case STRING:
                    emitEncStr(w, ePrefix + "_str", e.getStringValue());
                    break;
                case CLASS:
                    emitEncStr(w, ePrefix + "_cls", e.getClassName());
                    break;
                case METHOD_REF:
                case INTERFACE_METHOD_REF:
                case FIELD_REF:
                    emitEncStr(w, ePrefix + "_ro", e.getRefOwner());
                    emitEncStr(w, ePrefix + "_rn", e.getRefName());
                    emitEncStr(w, ePrefix + "_rd", e.getRefDescriptor());
                    break;
                case INVOKE_DYNAMIC:
                    emitEncStr(w, ePrefix + "_in", e.getDynamicName());
                    emitEncStr(w, ePrefix + "_id", e.getDynamicDescriptor());
                    break;
                case METHOD_HANDLE:
                    emitEncStr(w, ePrefix + "_ho", e.getHandleOwner());
                    emitEncStr(w, ePrefix + "_hn", e.getHandleName());
                    emitEncStr(w, ePrefix + "_hd", e.getHandleDescriptor());
                    break;
                default:
                    break;
            }
        }

        // 常量池数组 — 用函数初始化代替 designated initializer
        w.println("static VMCPEntry " + prefix + "_cp[" + cp.size() + "];");
        w.println("static void " + prefix + "_cp_init(void) {");

        for (CPEntry e : cp) {
            String ePrefix = prefix + "_cp" + e.getIndex();
            int idx = e.getIndex();
            String base = prefix + "_cp[" + idx + "]";

            w.println("    memset(&" + base + ", 0, sizeof(VMCPEntry));");

            switch (e.getType()) {
                case INTEGER:
                    w.println("    " + base + ".type = CP_INTEGER;");
                    w.println("    " + base + ".val.int_val = " + e.getIntValue() + ";");
                    break;
                case LONG:
                    w.println("    " + base + ".type = CP_LONG;");
                    w.println("    " + base + ".val.long_val = " + e.getLongValue() + "LL;");
                    break;
                case FLOAT:
                    w.println("    " + base + ".type = CP_FLOAT;");
                    w.println("    " + base + ".val.float_val = " + formatFloat(e.getFloatValue()) + ";");
                    break;
                case DOUBLE:
                    w.println("    " + base + ".type = CP_DOUBLE;");
                    w.println("    " + base + ".val.double_val = " + formatDouble(e.getDoubleValue()) + ";");
                    break;
                case STRING:
                    w.println("    " + base + ".type = CP_STRING;");
                    w.println("    " + base + ".val.str.data = " + ePrefix + "_str;");
                    w.println("    " + base + ".val.str.len = " + ePrefix + "_str_len;");
                    break;
                case CLASS:
                    w.println("    " + base + ".type = CP_CLASS;");
                    w.println("    " + base + ".val.class_name.data = " + ePrefix + "_cls;");
                    w.println("    " + base + ".val.class_name.len = " + ePrefix + "_cls_len;");
                    break;
                case METHOD_REF:
                    w.println("    " + base + ".type = CP_METHOD_REF;");
                    w.println("    " + base + ".val.ref.owner = " + ePrefix + "_ro;");
                    w.println("    " + base + ".val.ref.owner_len = " + ePrefix + "_ro_len;");
                    w.println("    " + base + ".val.ref.name = " + ePrefix + "_rn;");
                    w.println("    " + base + ".val.ref.name_len = " + ePrefix + "_rn_len;");
                    w.println("    " + base + ".val.ref.desc = " + ePrefix + "_rd;");
                    w.println("    " + base + ".val.ref.desc_len = " + ePrefix + "_rd_len;");
                    break;
                case INTERFACE_METHOD_REF:
                    w.println("    " + base + ".type = CP_IMETHOD_REF;");
                    w.println("    " + base + ".val.ref.owner = " + ePrefix + "_ro;");
                    w.println("    " + base + ".val.ref.owner_len = " + ePrefix + "_ro_len;");
                    w.println("    " + base + ".val.ref.name = " + ePrefix + "_rn;");
                    w.println("    " + base + ".val.ref.name_len = " + ePrefix + "_rn_len;");
                    w.println("    " + base + ".val.ref.desc = " + ePrefix + "_rd;");
                    w.println("    " + base + ".val.ref.desc_len = " + ePrefix + "_rd_len;");
                    break;
                case FIELD_REF:
                    w.println("    " + base + ".type = CP_FIELD_REF;");
                    w.println("    " + base + ".val.ref.owner = " + ePrefix + "_ro;");
                    w.println("    " + base + ".val.ref.owner_len = " + ePrefix + "_ro_len;");
                    w.println("    " + base + ".val.ref.name = " + ePrefix + "_rn;");
                    w.println("    " + base + ".val.ref.name_len = " + ePrefix + "_rn_len;");
                    w.println("    " + base + ".val.ref.desc = " + ePrefix + "_rd;");
                    w.println("    " + base + ".val.ref.desc_len = " + ePrefix + "_rd_len;");
                    break;
                case INVOKE_DYNAMIC:
                    w.println("    " + base + ".type = CP_INVOKE_DYNAMIC;");
                    w.println("    " + base + ".val.indy.bsm_index = " + e.getBootstrapMethodIndex() + ";");
                    w.println("    " + base + ".val.indy.name = " + ePrefix + "_in;");
                    w.println("    " + base + ".val.indy.name_len = " + ePrefix + "_in_len;");
                    w.println("    " + base + ".val.indy.desc = " + ePrefix + "_id;");
                    w.println("    " + base + ".val.indy.desc_len = " + ePrefix + "_id_len;");
                    break;
                case METHOD_HANDLE:
                    w.println("    " + base + ".type = CP_METHOD_HANDLE;");
                    w.println("    " + base + ".val.handle.tag = " + e.getHandleTag() + ";");
                    w.println("    " + base + ".val.handle.owner = " + ePrefix + "_ho;");
                    w.println("    " + base + ".val.handle.owner_len = " + ePrefix + "_ho_len;");
                    w.println("    " + base + ".val.handle.name = " + ePrefix + "_hn;");
                    w.println("    " + base + ".val.handle.name_len = " + ePrefix + "_hn_len;");
                    w.println("    " + base + ".val.handle.desc = " + ePrefix + "_hd;");
                    w.println("    " + base + ".val.handle.desc_len = " + ePrefix + "_hd_len;");
                    break;
            }
        }

        w.println("}");
    }
    /**
     * 发射异常表
     */
    private void emitExceptionTable(PrintWriter w, String prefix, EncryptedMethodData m) {
        List<ExceptionEntry> exc = m.getExceptionTable();
        if (exc.isEmpty()) return;

        w.println("static VMExceptionEntry " + prefix + "_exc[] = {");
        for (int i = 0; i < exc.size(); i++) {
            ExceptionEntry e = exc.get(i);
            w.println("    {" + e.getStartPc() + ", " + e.getEndPc() + ", " +
                    e.getHandlerPc() + ", " + e.getCatchTypeCpIndex() + "}" +
                    (i < exc.size() - 1 ? "," : ""));
        }
        w.println("};");
    }

    /**
     * 发射 Bootstrap 方法表
     */
    private void emitBootstrapTable(PrintWriter w, String prefix, EncryptedMethodData m) {
        List<BootstrapEntry> bsm = m.getBootstrapMethods();
        if (bsm.isEmpty()) return;

        // 先发射字符串
        for (int i = 0; i < bsm.size(); i++) {
            BootstrapEntry b = bsm.get(i);
            String bPrefix = prefix + "_bsm" + i;
            emitEncStr(w, bPrefix + "_o", b.getHandleOwner());
            emitEncStr(w, bPrefix + "_n", b.getHandleName());
            emitEncStr(w, bPrefix + "_d", b.getHandleDescriptor());

            // Bootstrap 参数
            List<Object> args = b.getArguments();
            if (!args.isEmpty()) {
                // 参数数据数组
                w.println("static const unsigned char* " + bPrefix + "_args[] = {");
                for (int j = 0; j < args.size(); j++) {
                    String argStr = argToString(args.get(j));
                    String aName = bPrefix + "_a" + j;
                    emitEncStr(w, aName, argStr);
                }
                // 重新打开数组
                w.println("};"); // 先关掉
                // 实际用 forward decl 方式处理
                // 简化：将参数存为字符串描述
                w.print("static const unsigned char* " + bPrefix + "_argp[] = {");
                for (int j = 0; j < args.size(); j++) {
                    w.print(bPrefix + "_a" + j);
                    if (j < args.size() - 1) w.print(",");
                }
                w.println("};");
                w.print("static int " + bPrefix + "_argl[] = {");
                for (int j = 0; j < args.size(); j++) {
                    w.print(bPrefix + "_a" + j + "_len");
                    if (j < args.size() - 1) w.print(",");
                }
                w.println("};");
            }
        }

        w.println("static VMBootstrapEntry " + prefix + "_bsm[] = {");
        for (int i = 0; i < bsm.size(); i++) {
            BootstrapEntry b = bsm.get(i);
            String bPrefix = prefix + "_bsm" + i;
            w.print("    {" + b.getHandleTag() + ",");
            w.print(bPrefix + "_o," + bPrefix + "_o_len,");
            w.print(bPrefix + "_n," + bPrefix + "_n_len,");
            w.print(bPrefix + "_d," + bPrefix + "_d_len,");
            w.print(b.getArguments().size() + ",");
            if (b.getArguments().isEmpty()) {
                w.print("NULL,NULL");
            } else {
                w.print(bPrefix + "_argp," + bPrefix + "_argl");
            }
            w.println("}" + (i < bsm.size() - 1 ? "," : ""));
        }
        w.println("};");
    }

    // ========================================================
    // 工具方法
    // ========================================================

    private void emitEncStr(PrintWriter w, String varName, String value) {
        byte[] enc = StringEncryptor.encrypt(value, stringKey);
        w.println("static const unsigned char " + varName + "[] = " +
                CryptoUtils.toCArrayLiteral(enc) + ";");
        w.println("static const int " + varName + "_len = " + enc.length + ";");
    }

    @SuppressWarnings("unchecked")
    private String argToString(Object arg) {
        if (arg instanceof String) return (String) arg;
        if (arg instanceof Integer) return "I:" + arg;
        if (arg instanceof Long) return "J:" + arg;
        if (arg instanceof Float) return "F:" + arg;
        if (arg instanceof Double) return "D:" + arg;
        if (arg instanceof java.util.Map) {
            java.util.Map<String, Object> map = (java.util.Map<String, Object>) arg;
            return "H:" + map.get("tag") + ":" + map.get("owner") + "." +
                    map.get("name") + map.get("desc");
        }
        return arg.toString();
    }

    private String formatFloat(float f) {
        if (Float.isNaN(f)) return "(0.0f/0.0f)";
        if (Float.isInfinite(f)) return f > 0 ? "(1.0f/0.0f)" : "(-1.0f/0.0f)";
        return f + "f";
    }

    private String formatDouble(double d) {
        if (Double.isNaN(d)) return "(0.0/0.0)";
        if (Double.isInfinite(d)) return d > 0 ? "(1.0/0.0)" : "(-1.0/0.0)";
        return d + "";
    }

    // ========================================================
    // vm_interpreter — 占位，下一条消息输出
    // ========================================================
// 在 NativeCodeGenerator.java 中替换 generateVmInterpreter 方法

    private void generateVmInterpreter(File dir) throws IOException {
        // Header
        try (PrintWriter w = new PrintWriter(new FileWriter(new File(dir, "vm_interpreter.h")))) {
            w.println("#ifndef VM_INTERPRETER_H");
            w.println("#define VM_INTERPRETER_H");
            w.println();
            w.println("#include \"vm_types.h\"");
            w.println();
            w.println("/**");
            w.println(" * 解释执行一个被保护的方法。");
            w.println(" * @param env       JNI 环境");
            w.println(" * @param method    方法元数据");
            w.println(" * @param args      参数数组（已按 locals 布局排列）");
            w.println(" * @param arg_count locals 数量");
            w.println(" * @return          返回值");
            w.println(" */");
            w.println("VMValue vm_interpret(JNIEnv* env, VMMethodInfo* method,");
            w.println("                     VMValue* args, int arg_count);");
            w.println();
            w.println("#endif /* VM_INTERPRETER_H */");
        }

        // Implementation
        try (PrintWriter w = new PrintWriter(new FileWriter(new File(dir, "vm_interpreter.c")))) {
            writeInterpreterSource(w);
        }
    }
    private void writeInterpreterSource(PrintWriter w) {
        w.println("/*");
        w.println(" * vm_interpreter.c — JNVM 栈式字节码解释器");
        w.println(" * 自动生成，请勿手动修改");
        w.println(" */");
        w.println();
        w.println("#include \"vm_interpreter.h\"");
        w.println("#include \"vm_data.h\"");
        w.println("#include \"chacha20.h\"");
        w.println("#include <string.h>");
        w.println("#include <stdlib.h>");
        w.println("#include <stdio.h>");
        w.println("#include <math.h>");

        w.println();

        // ============ 读取宏 ============
        w.println("/* ===== 字节码读取宏 ===== */");
        w.println("#define READ_U1(code, pc) ((uint8_t)(code[(pc)++]))");
        w.println("#define READ_U2(code, pc) \\");
        w.println("    (((uint16_t)(code[(pc)])<<8) | (uint16_t)(code[(pc)+1])); pc+=2");
        w.println("#define READ_S2(code, pc) \\");
        w.println("    ((int16_t)(((uint16_t)(code[(pc)])<<8) | (uint16_t)(code[(pc)+1]))); pc+=2");
        w.println("#define READ_S4(code, pc) \\");
        w.println("    ((int32_t)(((uint32_t)(code[(pc)])<<24)|((uint32_t)(code[(pc)+1])<<16)|\\");
        w.println("    ((uint32_t)(code[(pc)+2])<<8)|(uint32_t)(code[(pc)+3]))); pc+=4");
        w.println();

        // ============ 栈操作宏 ============
        w.println("/* ===== 栈操作宏 ===== */");
        w.println("#define PUSH_I(v)  do { frame.stack[frame.sp].i = (jint)(v); frame.sp++; } while(0)");
        w.println("#define PUSH_J(v)  do { frame.stack[frame.sp].j = (jlong)(v); frame.sp++; } while(0)");
        w.println("#define PUSH_F(v)  do { frame.stack[frame.sp].f = (jfloat)(v); frame.sp++; } while(0)");
        w.println("#define PUSH_D(v)  do { frame.stack[frame.sp].d = (jdouble)(v); frame.sp++; } while(0)");
        w.println("#define PUSH_L(v)  do { frame.stack[frame.sp].l = (jobject)(v); frame.sp++; } while(0)");
        w.println("#define PUSH_RAW(v) do { frame.stack[frame.sp] = (v); frame.sp++; } while(0)");
        w.println();
        w.println("#define POP()      (frame.stack[--frame.sp])");
        w.println("#define POP_I()    (frame.stack[--frame.sp].i)");
        w.println("#define POP_J()    (frame.stack[--frame.sp].j)");
        w.println("#define POP_F()    (frame.stack[--frame.sp].f)");
        w.println("#define POP_D()    (frame.stack[--frame.sp].d)");
        w.println("#define POP_L()    (frame.stack[--frame.sp].l)");
        w.println();
        w.println("#define PEEK()     (frame.stack[frame.sp - 1])");
        w.println("#define LOCAL_GET(idx) (frame.locals[(idx)])");
        w.println("#define LOCAL_SET(idx, val) (frame.locals[(idx)] = (val))");
        w.println();

        // ============ JVM Opcodes ============
        w.println("/* ===== JVM Opcodes ===== */");
        writeOpcodeDefines(w);
        w.println();

        // ============ CP 解析辅助函数 ============
        w.println("/* ===== 常量池解析 ===== */");
        w.println();
        writeResolveClassFunction(w);
        writeResolveMethodFunction(w);
        writeResolveFieldFunction(w);
        writeResolveStringFunction(w);
        w.println();

        // ============ 异常处理辅助 ============
        w.println("/* ===== 异常处理 ===== */");
        writeExceptionHandler(w);
        w.println();

        // ============ invokedynamic 辅助 ============
        w.println("/* ===== invokedynamic ===== */");
        writeIndyHandler(w);
        w.println();

        // ============ 方法调用辅助 ============
        w.println("/* ===== 方法调用辅助 ===== */");
        writeInvokeHelper(w);
        w.println();

        // ============ 描述符解析辅助 ============
        w.println("/* ===== 描述符解析 ===== */");
        writeDescriptorParser(w);
        w.println();

        // ============ 主解释循环 ============
        w.println("/* ===== 主解释器 ===== */");
        writeMainInterpreter(w);
    }
    private void writeOpcodeDefines(PrintWriter w) {
        w.println("#define OP_NOP             0x00");
        w.println("#define OP_ACONST_NULL     0x01");
        w.println("#define OP_ICONST_M1       0x02");
        w.println("#define OP_ICONST_0        0x03");
        w.println("#define OP_ICONST_1        0x04");
        w.println("#define OP_ICONST_2        0x05");
        w.println("#define OP_ICONST_3        0x06");
        w.println("#define OP_ICONST_4        0x07");
        w.println("#define OP_ICONST_5        0x08");
        w.println("#define OP_LCONST_0        0x09");
        w.println("#define OP_LCONST_1        0x0A");
        w.println("#define OP_FCONST_0        0x0B");
        w.println("#define OP_FCONST_1        0x0C");
        w.println("#define OP_FCONST_2        0x0D");
        w.println("#define OP_DCONST_0        0x0E");
        w.println("#define OP_DCONST_1        0x0F");
        w.println("#define OP_BIPUSH          0x10");
        w.println("#define OP_SIPUSH          0x11");
        w.println("#define OP_LDC             0x12");
        w.println("#define OP_ILOAD           0x15");
        w.println("#define OP_LLOAD           0x16");
        w.println("#define OP_FLOAD           0x17");
        w.println("#define OP_DLOAD           0x18");
        w.println("#define OP_ALOAD           0x19");
        w.println("#define OP_ILOAD_0         0x1A");
        w.println("#define OP_ILOAD_1         0x1B");
        w.println("#define OP_ILOAD_2         0x1C");
        w.println("#define OP_ILOAD_3         0x1D");
        w.println("#define OP_LLOAD_0         0x1E");
        w.println("#define OP_LLOAD_1         0x1F");
        w.println("#define OP_LLOAD_2         0x20");
        w.println("#define OP_LLOAD_3         0x21");
        w.println("#define OP_FLOAD_0         0x22");
        w.println("#define OP_FLOAD_1         0x23");
        w.println("#define OP_FLOAD_2         0x24");
        w.println("#define OP_FLOAD_3         0x25");
        w.println("#define OP_DLOAD_0         0x26");
        w.println("#define OP_DLOAD_1         0x27");
        w.println("#define OP_DLOAD_2         0x28");
        w.println("#define OP_DLOAD_3         0x29");
        w.println("#define OP_ALOAD_0         0x2A");
        w.println("#define OP_ALOAD_1         0x2B");
        w.println("#define OP_ALOAD_2         0x2C");
        w.println("#define OP_ALOAD_3         0x2D");
        w.println("#define OP_IALOAD          0x2E");
        w.println("#define OP_LALOAD          0x2F");
        w.println("#define OP_FALOAD          0x30");
        w.println("#define OP_DALOAD          0x31");
        w.println("#define OP_AALOAD          0x32");
        w.println("#define OP_BALOAD          0x33");
        w.println("#define OP_CALOAD          0x34");
        w.println("#define OP_SALOAD          0x35");
        w.println("#define OP_ISTORE          0x36");
        w.println("#define OP_LSTORE          0x37");
        w.println("#define OP_FSTORE          0x38");
        w.println("#define OP_DSTORE          0x39");
        w.println("#define OP_ASTORE          0x3A");
        w.println("#define OP_ISTORE_0        0x3B");
        w.println("#define OP_ISTORE_1        0x3C");
        w.println("#define OP_ISTORE_2        0x3D");
        w.println("#define OP_ISTORE_3        0x3E");
        w.println("#define OP_LSTORE_0        0x3F");
        w.println("#define OP_LSTORE_1        0x40");
        w.println("#define OP_LSTORE_2        0x41");
        w.println("#define OP_LSTORE_3        0x42");
        w.println("#define OP_FSTORE_0        0x43");
        w.println("#define OP_FSTORE_1        0x44");
        w.println("#define OP_FSTORE_2        0x45");
        w.println("#define OP_FSTORE_3        0x46");
        w.println("#define OP_DSTORE_0        0x47");
        w.println("#define OP_DSTORE_1        0x48");
        w.println("#define OP_DSTORE_2        0x49");
        w.println("#define OP_DSTORE_3        0x4A");
        w.println("#define OP_ASTORE_0        0x4B");
        w.println("#define OP_ASTORE_1        0x4C");
        w.println("#define OP_ASTORE_2        0x4D");
        w.println("#define OP_ASTORE_3        0x4E");
        w.println("#define OP_IASTORE         0x4F");
        w.println("#define OP_LASTORE         0x50");
        w.println("#define OP_FASTORE         0x51");
        w.println("#define OP_DASTORE         0x52");
        w.println("#define OP_AASTORE         0x53");
        w.println("#define OP_BASTORE         0x54");
        w.println("#define OP_CASTORE         0x55");
        w.println("#define OP_SASTORE         0x56");
        w.println("#define OP_POP             0x57");
        w.println("#define OP_POP2            0x58");
        w.println("#define OP_DUP             0x59");
        w.println("#define OP_DUP_X1          0x5A");
        w.println("#define OP_DUP_X2          0x5B");
        w.println("#define OP_DUP2            0x5C");
        w.println("#define OP_DUP2_X1         0x5D");
        w.println("#define OP_DUP2_X2         0x5E");
        w.println("#define OP_SWAP            0x5F");
        w.println("#define OP_IADD            0x60");
        w.println("#define OP_LADD            0x61");
        w.println("#define OP_FADD            0x62");
        w.println("#define OP_DADD            0x63");
        w.println("#define OP_ISUB            0x64");
        w.println("#define OP_LSUB            0x65");
        w.println("#define OP_FSUB            0x66");
        w.println("#define OP_DSUB            0x67");
        w.println("#define OP_IMUL            0x68");
        w.println("#define OP_LMUL            0x69");
        w.println("#define OP_FMUL            0x6A");
        w.println("#define OP_DMUL            0x6B");
        w.println("#define OP_IDIV            0x6C");
        w.println("#define OP_LDIV            0x6D");
        w.println("#define OP_FDIV            0x6E");
        w.println("#define OP_DDIV            0x6F");
        w.println("#define OP_IREM            0x70");
        w.println("#define OP_LREM            0x71");
        w.println("#define OP_FREM            0x72");
        w.println("#define OP_DREM            0x73");
        w.println("#define OP_INEG            0x74");
        w.println("#define OP_LNEG            0x75");
        w.println("#define OP_FNEG            0x76");
        w.println("#define OP_DNEG            0x77");
        w.println("#define OP_ISHL            0x78");
        w.println("#define OP_LSHL            0x79");
        w.println("#define OP_ISHR            0x7A");
        w.println("#define OP_LSHR            0x7B");
        w.println("#define OP_IUSHR           0x7C");
        w.println("#define OP_LUSHR           0x7D");
        w.println("#define OP_IAND            0x7E");
        w.println("#define OP_LAND            0x7F");
        w.println("#define OP_IOR             0x80");
        w.println("#define OP_LOR             0x81");
        w.println("#define OP_IXOR            0x82");
        w.println("#define OP_LXOR            0x83");
        w.println("#define OP_IINC            0x84");
        w.println("#define OP_I2L             0x85");
        w.println("#define OP_I2F             0x86");
        w.println("#define OP_I2D             0x87");
        w.println("#define OP_L2I             0x88");
        w.println("#define OP_L2F             0x89");
        w.println("#define OP_L2D             0x8A");
        w.println("#define OP_F2I             0x8B");
        w.println("#define OP_F2L             0x8C");
        w.println("#define OP_F2D             0x8D");
        w.println("#define OP_D2I             0x8E");
        w.println("#define OP_D2L             0x8F");
        w.println("#define OP_D2F             0x90");
        w.println("#define OP_I2B             0x91");
        w.println("#define OP_I2C             0x92");
        w.println("#define OP_I2S             0x93");
        w.println("#define OP_LCMP            0x94");
        w.println("#define OP_FCMPL           0x95");
        w.println("#define OP_FCMPG           0x96");
        w.println("#define OP_DCMPL           0x97");
        w.println("#define OP_DCMPG           0x98");
        w.println("#define OP_IFEQ            0x99");
        w.println("#define OP_IFNE            0x9A");
        w.println("#define OP_IFLT            0x9B");
        w.println("#define OP_IFGE            0x9C");
        w.println("#define OP_IFGT            0x9D");
        w.println("#define OP_IFLE            0x9E");
        w.println("#define OP_IF_ICMPEQ       0x9F");
        w.println("#define OP_IF_ICMPNE       0xA0");
        w.println("#define OP_IF_ICMPLT       0xA1");
        w.println("#define OP_IF_ICMPGE       0xA2");
        w.println("#define OP_IF_ICMPGT       0xA3");
        w.println("#define OP_IF_ICMPLE       0xA4");
        w.println("#define OP_IF_ACMPEQ       0xA5");
        w.println("#define OP_IF_ACMPNE       0xA6");
        w.println("#define OP_GOTO            0xA7");
        w.println("#define OP_TABLESWITCH     0xAA");
        w.println("#define OP_LOOKUPSWITCH    0xAB");
        w.println("#define OP_IRETURN         0xAC");
        w.println("#define OP_LRETURN         0xAD");
        w.println("#define OP_FRETURN         0xAE");
        w.println("#define OP_DRETURN         0xAF");
        w.println("#define OP_ARETURN         0xB0");
        w.println("#define OP_RETURN          0xB1");
        w.println("#define OP_GETSTATIC       0xB2");
        w.println("#define OP_PUTSTATIC       0xB3");
        w.println("#define OP_GETFIELD        0xB4");
        w.println("#define OP_PUTFIELD        0xB5");
        w.println("#define OP_INVOKEVIRTUAL   0xB6");
        w.println("#define OP_INVOKESPECIAL   0xB7");
        w.println("#define OP_INVOKESTATIC    0xB8");
        w.println("#define OP_INVOKEINTERFACE 0xB9");
        w.println("#define OP_INVOKEDYNAMIC   0xBA");
        w.println("#define OP_NEW             0xBB");
        w.println("#define OP_NEWARRAY        0xBC");
        w.println("#define OP_ANEWARRAY       0xBD");
        w.println("#define OP_ARRAYLENGTH     0xBE");
        w.println("#define OP_ATHROW          0xBF");
        w.println("#define OP_CHECKCAST       0xC0");
        w.println("#define OP_INSTANCEOF      0xC1");
        w.println("#define OP_MONITORENTER    0xC2");
        w.println("#define OP_MONITOREXIT     0xC3");
        w.println("#define OP_MULTIANEWARRAY  0xC5");
        w.println("#define OP_IFNULL          0xC6");
        w.println("#define OP_IFNONNULL       0xC7");
    }
    private void writeResolveClassFunction(PrintWriter w) {
        w.println("static jclass vm_resolve_class(JNIEnv* env, VMCPEntry* entry) {");
        w.println("    if (entry->resolved && entry->cached_class) return entry->cached_class;");
        w.println("    char buf[512];");
        w.println("    vm_decrypt_str(entry->val.class_name.data, entry->val.class_name.len, buf);");
        w.println("    jclass cls = (*env)->FindClass(env, buf);");
        w.println("    if (!cls) return NULL;");
        w.println("    entry->cached_class = (jclass)(*env)->NewGlobalRef(env, cls);");
        w.println("    (*env)->DeleteLocalRef(env, cls);");
        w.println("    entry->resolved = 1;");
        w.println("    return entry->cached_class;");
        w.println("}");
        w.println();

        // 通用解析类名
        w.println("static jclass vm_find_class(JNIEnv* env, const unsigned char* enc, int len) {");
        w.println("    char buf[512];");
        w.println("    vm_decrypt_str(enc, len, buf);");
        w.println("    return (*env)->FindClass(env, buf);");
        w.println("}");
        w.println();
    }

    private void writeResolveMethodFunction(PrintWriter w) {
        w.println("static jmethodID vm_resolve_method(JNIEnv* env, VMCPEntry* entry, int is_static) {");
        w.println("    if (entry->resolved && entry->cached_method) return entry->cached_method;");
        w.println("    char owner_buf[512], name_buf[256], desc_buf[512];");
        w.println("    vm_decrypt_str(entry->val.ref.owner, entry->val.ref.owner_len, owner_buf);");
        w.println("    vm_decrypt_str(entry->val.ref.name, entry->val.ref.name_len, name_buf);");
        w.println("    vm_decrypt_str(entry->val.ref.desc, entry->val.ref.desc_len, desc_buf);");
        w.println("    jclass cls = (*env)->FindClass(env, owner_buf);");
        w.println("    if (!cls) return NULL;");
        w.println("    jmethodID mid;");
        w.println("    if (is_static) {");
        w.println("        mid = (*env)->GetStaticMethodID(env, cls, name_buf, desc_buf);");
        w.println("    } else {");
        w.println("        mid = (*env)->GetMethodID(env, cls, name_buf, desc_buf);");
        w.println("    }");
        w.println("    if (!mid) { (*env)->DeleteLocalRef(env, cls); return NULL; }");
        w.println("    entry->cached_class = (jclass)(*env)->NewGlobalRef(env, cls);");
        w.println("    (*env)->DeleteLocalRef(env, cls);");
        w.println("    entry->cached_method = mid;");
        w.println("    entry->resolved = 1;");
        w.println("    return mid;");
        w.println("}");
        w.println();
    }

    private void writeResolveFieldFunction(PrintWriter w) {
        w.println("static void vm_resolve_field(JNIEnv* env, VMCPEntry* entry, int is_static) {");
        w.println("    if (entry->resolved) return;");
        w.println("    char owner_buf[512], name_buf[256], desc_buf[256];");
        w.println("    vm_decrypt_str(entry->val.ref.owner, entry->val.ref.owner_len, owner_buf);");
        w.println("    vm_decrypt_str(entry->val.ref.name, entry->val.ref.name_len, name_buf);");
        w.println("    vm_decrypt_str(entry->val.ref.desc, entry->val.ref.desc_len, desc_buf);");
        w.println("    jclass cls = (*env)->FindClass(env, owner_buf);");
        w.println("    if (!cls) return;");
        w.println("    if (is_static) {");
        w.println("        entry->cached_field = (*env)->GetStaticFieldID(env, cls, name_buf, desc_buf);");
        w.println("    } else {");
        w.println("        entry->cached_field = (*env)->GetFieldID(env, cls, name_buf, desc_buf);");
        w.println("    }");
        w.println("    entry->cached_class = (jclass)(*env)->NewGlobalRef(env, cls);");
        w.println("    (*env)->DeleteLocalRef(env, cls);");
        w.println("    entry->resolved = 1;");
        w.println("}");
        w.println();
    }

    private void writeResolveStringFunction(PrintWriter w) {
        w.println("static jstring vm_resolve_string(JNIEnv* env, VMCPEntry* entry) {");
        w.println("    if (entry->resolved && entry->cached_string) return entry->cached_string;");
        w.println("    char buf[4096];");
        w.println("    vm_decrypt_str(entry->val.str.data, entry->val.str.len, buf);");
        w.println("    jstring s = (*env)->NewStringUTF(env, buf);");
        w.println("    entry->cached_string = (jstring)(*env)->NewGlobalRef(env, s);");
        w.println("    (*env)->DeleteLocalRef(env, s);");
        w.println("    entry->resolved = 1;");
        w.println("    return entry->cached_string;");
        w.println("}");
        w.println();
    }
    private void writeExceptionHandler(PrintWriter w) {
        w.println("/**");
        w.println(" * 在异常表中查找匹配的 handler。");
        w.println(" * @return handler PC，或 -1 表示未找到（需向上传播）");
        w.println(" */");
        w.println("static int vm_find_exception_handler(JNIEnv* env, VMMethodInfo* method,");
        w.println("                                      int pc, jthrowable ex) {");
        w.println("    for (int i = 0; i < method->exception_count; i++) {");
        w.println("        VMExceptionEntry* e = &method->exceptions[i];");
        w.println("        if (pc >= e->start_pc && pc < e->end_pc) {");
        w.println("            if (e->catch_type_cp_index < 0) {");
        w.println("                /* catch-all (finally) */");
        w.println("                return e->handler_pc;");
        w.println("            }");
        w.println("            /* 检查异常类型 */");
        w.println("            VMCPEntry* cpEntry = &method->cp[e->catch_type_cp_index];");
        w.println("            jclass catchClass = vm_resolve_class(env, cpEntry);");
        w.println("            if (catchClass && (*env)->IsInstanceOf(env, ex, catchClass)) {");
        w.println("                return e->handler_pc;");
        w.println("            }");
        w.println("        }");
        w.println("    }");
        w.println("    return -1;");
        w.println("}");
        w.println();

        // 异常检查宏（在解释循环中使用）
        w.println("#define CHECK_EXCEPTION() \\");
        w.println("    do { \\");
        w.println("        if ((*env)->ExceptionCheck(env)) { \\");
        w.println("            jthrowable _ex = (*env)->ExceptionOccurred(env); \\");
        w.println("            (*env)->ExceptionClear(env); \\");
        w.println("            int _handler = vm_find_exception_handler(env, method, instr_pc, _ex); \\");
        w.println("            if (_handler >= 0) { \\");
        w.println("                frame.sp = 0; \\");
        w.println("                PUSH_L(_ex); \\");
        w.println("                frame.pc = _handler; \\");
        w.println("                continue; \\");
        w.println("            } else { \\");
        w.println("                (*env)->Throw(env, _ex); \\");
        w.println("                goto cleanup; \\");
        w.println("            } \\");
        w.println("        } \\");
        w.println("    } while(0)");
        w.println();
    }
    private void writeIndyHandler(PrintWriter w) {
        w.println("/**");
        w.println(" * 执行 invokedynamic：解析 bootstrap → 获取 CallSite → 调用 target");
        w.println(" */");
        w.println("static jobject vm_invoke_dynamic(JNIEnv* env, VMMethodInfo* method,");
        w.println("                                  VMCPEntry* cpEntry, VMValue* stack, int* sp) {");
        w.println("    /* 获取 bootstrap 信息 */");
        w.println("    int bsm_idx = cpEntry->val.indy.bsm_index;");
        w.println("    VMBootstrapEntry* bsm = &method->bootstraps[bsm_idx];");
        w.println();
        w.println("    char bsm_owner[512], bsm_name[256], bsm_desc[512];");
        w.println("    vm_decrypt_str(bsm->owner, bsm->owner_len, bsm_owner);");
        w.println("    vm_decrypt_str(bsm->name, bsm->name_len, bsm_name);");
        w.println("    vm_decrypt_str(bsm->desc, bsm->desc_len, bsm_desc);");
        w.println();
        w.println("    char indy_name[256], indy_desc[512];");
        w.println("    vm_decrypt_str(cpEntry->val.indy.name, cpEntry->val.indy.name_len, indy_name);");
        w.println("    vm_decrypt_str(cpEntry->val.indy.desc, cpEntry->val.indy.desc_len, indy_desc);");
        w.println();
        w.println("    /* MethodHandles.Lookup */");
        w.println("    jclass mhLookup = (*env)->FindClass(env, \"java/lang/invoke/MethodHandles$Lookup\");");
        w.println("    jclass mhClass = (*env)->FindClass(env, \"java/lang/invoke/MethodHandles\");");
        w.println("    jmethodID lookupMid = (*env)->GetStaticMethodID(env, mhClass,");
        w.println("        \"lookup\", \"()Ljava/lang/invoke/MethodHandles$Lookup;\");");
        w.println("    jobject lookup = (*env)->CallStaticObjectMethod(env, mhClass, lookupMid);");
        w.println();
        w.println("    /* 解析 bootstrap method class */");
        w.println("    jclass bsmClass = (*env)->FindClass(env, bsm_owner);");
        w.println("    if (!bsmClass) return NULL;");
        w.println();
        w.println("    /* 找到 bsm 方法 */");
        w.println("    jmethodID bsmMid = (*env)->GetStaticMethodID(env, bsmClass, bsm_name, bsm_desc);");
        w.println("    if (!bsmMid) return NULL;");
        w.println();
        w.println("    /* 调用名和类型 */");
        w.println("    jstring indyNameStr = (*env)->NewStringUTF(env, indy_name);");
        w.println();
        w.println("    /* MethodType.fromMethodDescriptorString */");
        w.println("    jclass mtClass = (*env)->FindClass(env, \"java/lang/invoke/MethodType\");");
        w.println("    jmethodID fromDesc = (*env)->GetStaticMethodID(env, mtClass,");
        w.println("        \"fromMethodDescriptorString\",");
        w.println("        \"(Ljava/lang/String;Ljava/lang/ClassLoader;)Ljava/lang/invoke/MethodType;\");");
        w.println("    jstring descStr = (*env)->NewStringUTF(env, indy_desc);");
        w.println("    jobject methodType = (*env)->CallStaticObjectMethod(env, mtClass, fromDesc,");
        w.println("        descStr, NULL);");
        w.println();
        w.println("    /* 调用 BSM (简化：假设 3 个标准参数 lookup, name, type) */");
        w.println("    jobject callSite = (*env)->CallStaticObjectMethod(env, bsmClass, bsmMid,");
        w.println("        lookup, indyNameStr, methodType);");
        w.println("    if ((*env)->ExceptionCheck(env)) return NULL;");
        w.println();
        w.println("    /* CallSite.getTarget() → MethodHandle */");
        w.println("    jclass csClass = (*env)->FindClass(env, \"java/lang/invoke/CallSite\");");
        w.println("    jmethodID getTarget = (*env)->GetMethodID(env, csClass,");
        w.println("        \"getTarget\", \"()Ljava/lang/invoke/MethodHandle;\");");
        w.println("    jobject mh = (*env)->CallObjectMethod(env, callSite, getTarget);");
        w.println();
        w.println("    return mh; /* 返回 MethodHandle，由调用者 invokeExact */");
        w.println("}");
        w.println();
    }


    private void writeInvokeHelper(PrintWriter w) {
        w.println("/**");
        w.println(" * 通过 JNI 调用方法，根据返回类型装箱/拆箱。");
        w.println(" * 参数从栈上弹出，结果压栈。");
        w.println(" */");
        w.println("static void vm_invoke_method(JNIEnv* env, VMCPEntry* cpEntry,");
        w.println("    int invoke_type, VMFrame* frame) {");
        w.println();
        w.println("    int is_static = (invoke_type == OP_INVOKESTATIC);");
        w.println("    jmethodID mid = vm_resolve_method(env, cpEntry, is_static);");
        w.println("    if (!mid) return;");
        w.println("    jclass cls = cpEntry->cached_class;");
        w.println();
        w.println("    /* 解析描述符获取参数和返回类型 */");
        w.println("    char desc_buf[512];");
        w.println("    vm_decrypt_str(cpEntry->val.ref.desc, cpEntry->val.ref.desc_len, desc_buf);");
        w.println();
        w.println("    /* 计算参数个数 */");
        w.println("    int param_count = 0;");
        w.println("    int param_slots[256]; /* 0=ref,1=int,2=long,3=float,4=double */");
        w.println("    const char* p = desc_buf + 1; /* skip '(' */");
        w.println("    while (*p != ')') {");
        w.println("        switch (*p) {");
        w.println("            case 'B': case 'C': case 'S': case 'Z': case 'I':");
        w.println("                param_slots[param_count++] = 1; p++; break;");
        w.println("            case 'J':");
        w.println("                param_slots[param_count++] = 2; p++; break;");
        w.println("            case 'F':");
        w.println("                param_slots[param_count++] = 3; p++; break;");
        w.println("            case 'D':");
        w.println("                param_slots[param_count++] = 4; p++; break;");
        w.println("            case 'L':");
        w.println("                param_slots[param_count++] = 0;");
        w.println("                while (*p != ';') p++; p++; break;");
        w.println("            case '[':");
        w.println("                param_slots[param_count] = 0;");
        w.println("                while (*p == '[') p++;");
        w.println("                if (*p == 'L') { while (*p != ';') p++; p++; }");
        w.println("                else p++;");
        w.println("                param_count++; break;");
        w.println("            default: p++; break;");
        w.println("        }");
        w.println("    }");
        w.println();
        w.println("    /* 构建 jvalue 参数数组 */");
        w.println("    jvalue jargs[256];");
        w.println("    /* 从栈上逆序弹出参数 */");
        w.println("    for (int i = param_count - 1; i >= 0; i--) {");
        w.println("        VMValue v = frame->stack[--frame->sp];");
        w.println("        switch (param_slots[i]) {");
        w.println("            case 0: jargs[i].l = v.l; break;");
        w.println("            case 1: jargs[i].i = v.i; break;");
        w.println("            case 2: jargs[i].j = v.j; break;");
        w.println("            case 3: jargs[i].f = v.f; break;");
        w.println("            case 4: jargs[i].d = v.d; break;");
        w.println("        }");
        w.println("    }");
        w.println();
        w.println("    /* objectref for non-static */");
        w.println("    jobject obj = NULL;");
        w.println("    if (!is_static) {");
        w.println("        obj = frame->stack[--frame->sp].l;");
        w.println("    }");
        w.println();
        w.println("    /* 返回类型 */");
        w.println("    p++; /* skip ')' */");
        w.println("    char ret_type = *p;");
        w.println();
        w.println("    /* 调用 */");
        w.println("    switch (ret_type) {");
        w.println("    case 'V':");
        w.println("        if (is_static) (*env)->CallStaticVoidMethodA(env, cls, mid, jargs);");
        w.println("        else if (invoke_type == OP_INVOKESPECIAL)");
        w.println("            (*env)->CallNonvirtualVoidMethodA(env, obj, cls, mid, jargs);");
        w.println("        else (*env)->CallVoidMethodA(env, obj, mid, jargs);");
        w.println("        break;");
        w.println("    case 'I': case 'B': case 'C': case 'S': case 'Z': {");
        w.println("        jint r;");
        w.println("        if (is_static) r = (*env)->CallStaticIntMethodA(env, cls, mid, jargs);");
        w.println("        else if (invoke_type == OP_INVOKESPECIAL)");
        w.println("            r = (*env)->CallNonvirtualIntMethodA(env, obj, cls, mid, jargs);");
        w.println("        else r = (*env)->CallIntMethodA(env, obj, mid, jargs);");
        w.println("        PUSH_I(r); break;");
        w.println("    }");
        w.println("    case 'J': {");
        w.println("        jlong r;");
        w.println("        if (is_static) r = (*env)->CallStaticLongMethodA(env, cls, mid, jargs);");
        w.println("        else if (invoke_type == OP_INVOKESPECIAL)");
        w.println("            r = (*env)->CallNonvirtualLongMethodA(env, obj, cls, mid, jargs);");
        w.println("        else r = (*env)->CallLongMethodA(env, obj, mid, jargs);");
        w.println("        PUSH_J(r); break;");
        w.println("    }");
        w.println("    case 'F': {");
        w.println("        jfloat r;");
        w.println("        if (is_static) r = (*env)->CallStaticFloatMethodA(env, cls, mid, jargs);");
        w.println("        else if (invoke_type == OP_INVOKESPECIAL)");
        w.println("            r = (*env)->CallNonvirtualFloatMethodA(env, obj, cls, mid, jargs);");
        w.println("        else r = (*env)->CallFloatMethodA(env, obj, mid, jargs);");
        w.println("        PUSH_F(r); break;");
        w.println("    }");
        w.println("    case 'D': {");
        w.println("        jdouble r;");
        w.println("        if (is_static) r = (*env)->CallStaticDoubleMethodA(env, cls, mid, jargs);");
        w.println("        else if (invoke_type == OP_INVOKESPECIAL)");
        w.println("            r = (*env)->CallNonvirtualDoubleMethodA(env, obj, cls, mid, jargs);");
        w.println("        else r = (*env)->CallDoubleMethodA(env, obj, mid, jargs);");
        w.println("        PUSH_D(r); break;");
        w.println("    }");
        w.println("    default: { /* L or [ → object */");
        w.println("        jobject r;");
        w.println("        if (is_static) r = (*env)->CallStaticObjectMethodA(env, cls, mid, jargs);");
        w.println("        else if (invoke_type == OP_INVOKESPECIAL)");
        w.println("            r = (*env)->CallNonvirtualObjectMethodA(env, obj, cls, mid, jargs);");
        w.println("        else r = (*env)->CallObjectMethodA(env, obj, mid, jargs);");
        w.println("        PUSH_L(r); break;");
        w.println("    }");
        w.println("    }");
        w.println("}");
        w.println();
    }
    private void writeDescriptorParser(PrintWriter w) {
        w.println("/* 获取描述符的返回类型字符 */");
        w.println("static char vm_get_return_type(const char* desc) {");
        w.println("    const char* p = desc;");
        w.println("    while (*p != ')') p++;");
        w.println("    return *(p + 1);");
        w.println("}");
        w.println();
        w.println("/* 获取字段描述符的类型字符 */");
        w.println("static char vm_get_field_type(const unsigned char* enc_desc, int len) {");
        w.println("    char buf[256];");
        w.println("    vm_decrypt_str(enc_desc, len, buf);");
        w.println("    return buf[0];");
        w.println("}");
        w.println();
    }
    private void writeMainInterpreter(PrintWriter w) {
        w.println("VMValue vm_interpret(JNIEnv* env, VMMethodInfo* method,");
        w.println("                     VMValue* args, int arg_count) {");
        w.println("    VMValue result;");
        w.println("    result.j = 0;");
        w.println();
        w.println("    /* 解密字节码到栈上 */");
        w.println("    unsigned char* code = (unsigned char*)alloca(method->code_length);");
        w.println("    chacha20_decrypt(method->key, method->nonce, 0,");
        w.println("                    method->encrypted_code, code, method->code_length);");
        w.println();
        w.println("    /* 分配帧 */");
        w.println("    VMFrame frame;");
        w.println("    frame.stack  = (VMValue*)alloca(sizeof(VMValue) * (method->max_stack + 16));");
        w.println("    frame.locals = (VMValue*)alloca(sizeof(VMValue) * (method->max_locals + 4));");
        w.println("    frame.sp = 0;");
        w.println("    frame.pc = 0;");
        w.println("    frame.method = method;");
        w.println("    frame.code = code;");
        w.println();
        w.println("    memset(frame.stack, 0, sizeof(VMValue) * (method->max_stack + 16));");
        w.println("    memset(frame.locals, 0, sizeof(VMValue) * (method->max_locals + 4));");
        w.println();
        w.println("    /* 复制参数到 locals */");
        w.println("    for (int i = 0; i < arg_count && i < method->max_locals; i++) {");
        w.println("        frame.locals[i] = args[i];");
        w.println("    }");
        w.println();
        w.println("    /* 主解释循环 */");
        w.println("    while (frame.pc < method->code_length) {");
        w.println("        int instr_pc = frame.pc;");
        w.println("        uint8_t opcode = READ_U1(code, frame.pc);");
        w.println();
        w.println("        switch (opcode) {");
        w.println();

        // NOP
        w.println("        case OP_NOP: break;");
        w.println();

        // 常量加载
        w.println("        /* ===== 常量 ===== */");
        w.println("        case OP_ACONST_NULL: PUSH_L(NULL); break;");
        w.println("        case OP_ICONST_M1: PUSH_I(-1); break;");
        w.println("        case OP_ICONST_0:  PUSH_I(0);  break;");
        w.println("        case OP_ICONST_1:  PUSH_I(1);  break;");
        w.println("        case OP_ICONST_2:  PUSH_I(2);  break;");
        w.println("        case OP_ICONST_3:  PUSH_I(3);  break;");
        w.println("        case OP_ICONST_4:  PUSH_I(4);  break;");
        w.println("        case OP_ICONST_5:  PUSH_I(5);  break;");
        w.println("        case OP_LCONST_0:  PUSH_J(0L); break;");
        w.println("        case OP_LCONST_1:  PUSH_J(1L); break;");
        w.println("        case OP_FCONST_0:  PUSH_F(0.0f); break;");
        w.println("        case OP_FCONST_1:  PUSH_F(1.0f); break;");
        w.println("        case OP_FCONST_2:  PUSH_F(2.0f); break;");
        w.println("        case OP_DCONST_0:  PUSH_D(0.0);  break;");
        w.println("        case OP_DCONST_1:  PUSH_D(1.0);  break;");
        w.println();
        w.println("        case OP_BIPUSH: { int8_t val = (int8_t)READ_U1(code,frame.pc); PUSH_I(val); break; }");
        w.println("        case OP_SIPUSH: { int16_t val = (int16_t)READ_S2(code,frame.pc); PUSH_I(val); break; }");
        w.println();

        // LDC
        w.println("        case OP_LDC: {");
        w.println("            uint16_t idx = READ_U2(code, frame.pc);");
        w.println("            VMCPEntry* e = &method->cp[idx];");
        w.println("            switch (e->type) {");
        w.println("                case CP_INTEGER: PUSH_I(e->val.int_val); break;");
        w.println("                case CP_LONG:    PUSH_J(e->val.long_val); break;");
        w.println("                case CP_FLOAT:   PUSH_F(e->val.float_val); break;");
        w.println("                case CP_DOUBLE:  PUSH_D(e->val.double_val); break;");
        w.println("                case CP_STRING:  PUSH_L(vm_resolve_string(env, e)); break;");
        w.println("                case CP_CLASS:   PUSH_L(vm_resolve_class(env, e)); break;");
        w.println("                default: PUSH_I(0); break;");
        w.println("            }");
        w.println("            break;");
        w.println("        }");
        w.println();

        // LOAD
        w.println("        /* ===== LOAD ===== */");
        w.println("        case OP_ILOAD: case OP_FLOAD: case OP_ALOAD: {");
        w.println("            uint16_t idx = READ_U2(code,frame.pc);");
        w.println("            PUSH_RAW(LOCAL_GET(idx)); break;");
        w.println("        }");
        w.println("        case OP_LLOAD: case OP_DLOAD: {");
        w.println("            uint16_t idx = READ_U2(code,frame.pc);");
        w.println("            PUSH_RAW(LOCAL_GET(idx)); break;");
        w.println("        }");
        w.println("        case OP_ILOAD_0: case OP_FLOAD_0: case OP_ALOAD_0: PUSH_RAW(LOCAL_GET(0)); break;");
        w.println("        case OP_ILOAD_1: case OP_FLOAD_1: case OP_ALOAD_1: PUSH_RAW(LOCAL_GET(1)); break;");
        w.println("        case OP_ILOAD_2: case OP_FLOAD_2: case OP_ALOAD_2: PUSH_RAW(LOCAL_GET(2)); break;");
        w.println("        case OP_ILOAD_3: case OP_FLOAD_3: case OP_ALOAD_3: PUSH_RAW(LOCAL_GET(3)); break;");
        w.println("        case OP_LLOAD_0: case OP_DLOAD_0: PUSH_RAW(LOCAL_GET(0)); break;");
        w.println("        case OP_LLOAD_1: case OP_DLOAD_1: PUSH_RAW(LOCAL_GET(1)); break;");
        w.println("        case OP_LLOAD_2: case OP_DLOAD_2: PUSH_RAW(LOCAL_GET(2)); break;");
        w.println("        case OP_LLOAD_3: case OP_DLOAD_3: PUSH_RAW(LOCAL_GET(3)); break;");
        w.println();

        // STORE
        w.println("        /* ===== STORE ===== */");
        w.println("        case OP_ISTORE: case OP_FSTORE: case OP_ASTORE: {");
        w.println("            uint16_t idx = READ_U2(code,frame.pc);");
        w.println("            LOCAL_SET(idx, POP()); break;");
        w.println("        }");
        w.println("        case OP_LSTORE: case OP_DSTORE: {");
        w.println("            uint16_t idx = READ_U2(code,frame.pc);");
        w.println("            LOCAL_SET(idx, POP()); break;");
        w.println("        }");
        w.println("        case OP_ISTORE_0: case OP_FSTORE_0: case OP_ASTORE_0: LOCAL_SET(0, POP()); break;");
        w.println("        case OP_ISTORE_1: case OP_FSTORE_1: case OP_ASTORE_1: LOCAL_SET(1, POP()); break;");
        w.println("        case OP_ISTORE_2: case OP_FSTORE_2: case OP_ASTORE_2: LOCAL_SET(2, POP()); break;");
        w.println("        case OP_ISTORE_3: case OP_FSTORE_3: case OP_ASTORE_3: LOCAL_SET(3, POP()); break;");
        w.println("        case OP_LSTORE_0: case OP_DSTORE_0: LOCAL_SET(0, POP()); break;");
        w.println("        case OP_LSTORE_1: case OP_DSTORE_1: LOCAL_SET(1, POP()); break;");
        w.println("        case OP_LSTORE_2: case OP_DSTORE_2: LOCAL_SET(2, POP()); break;");
        w.println("        case OP_LSTORE_3: case OP_DSTORE_3: LOCAL_SET(3, POP()); break;");
        w.println();

        // 数组 LOAD
        w.println("        /* ===== 数组 LOAD ===== */");
        w.println("        case OP_IALOAD: { jint idx=POP_I(); jobject arr=POP_L();");
        w.println("            PUSH_I((*env)->GetIntArrayElements(env,arr,NULL)[idx]);break;}");
        // 简化版：用 Get<Type>ArrayRegion
        w.println("        case OP_LALOAD: { jint idx=POP_I(); jobject arr=POP_L();");
        w.println("            jlong v; (*env)->GetLongArrayRegion(env,arr,idx,1,&v); PUSH_J(v);break;}");
        w.println("        case OP_FALOAD: { jint idx=POP_I(); jobject arr=POP_L();");
        w.println("            jfloat v; (*env)->GetFloatArrayRegion(env,arr,idx,1,&v); PUSH_F(v);break;}");
        w.println("        case OP_DALOAD: { jint idx=POP_I(); jobject arr=POP_L();");
        w.println("            jdouble v; (*env)->GetDoubleArrayRegion(env,arr,idx,1,&v); PUSH_D(v);break;}");
        w.println("        case OP_AALOAD: { jint idx=POP_I(); jobject arr=POP_L();");
        w.println("            PUSH_L((*env)->GetObjectArrayElement(env,arr,idx));break;}");
        w.println("        case OP_BALOAD: { jint idx=POP_I(); jobject arr=POP_L();");
        w.println("            jbyte v; (*env)->GetByteArrayRegion(env,arr,idx,1,&v); PUSH_I(v);break;}");
        w.println("        case OP_CALOAD: { jint idx=POP_I(); jobject arr=POP_L();");
        w.println("            jchar v; (*env)->GetCharArrayRegion(env,arr,idx,1,&v); PUSH_I(v);break;}");
        w.println("        case OP_SALOAD: { jint idx=POP_I(); jobject arr=POP_L();");
        w.println("            jshort v; (*env)->GetShortArrayRegion(env,arr,idx,1,&v); PUSH_I(v);break;}");
        w.println();

        // 数组 STORE
        w.println("        /* ===== 数组 STORE ===== */");
        w.println("        case OP_IASTORE: { jint v=POP_I(); jint idx=POP_I(); jobject arr=POP_L();");
        w.println("            (*env)->SetIntArrayRegion(env,arr,idx,1,&v);break;}");
        w.println("        case OP_LASTORE: { jlong v=POP_J(); jint idx=POP_I(); jobject arr=POP_L();");
        w.println("            (*env)->SetLongArrayRegion(env,arr,idx,1,&v);break;}");
        w.println("        case OP_FASTORE: { jfloat v=POP_F(); jint idx=POP_I(); jobject arr=POP_L();");
        w.println("            (*env)->SetFloatArrayRegion(env,arr,idx,1,&v);break;}");
        w.println("        case OP_DASTORE: { jdouble v=POP_D(); jint idx=POP_I(); jobject arr=POP_L();");
        w.println("            (*env)->SetDoubleArrayRegion(env,arr,idx,1,&v);break;}");
        w.println("        case OP_AASTORE: { jobject v=POP_L(); jint idx=POP_I(); jobject arr=POP_L();");
        w.println("            (*env)->SetObjectArrayElement(env,arr,idx,v);break;}");
        w.println("        case OP_BASTORE: { jbyte v=(jbyte)POP_I(); jint idx=POP_I(); jobject arr=POP_L();");
        w.println("            (*env)->SetByteArrayRegion(env,arr,idx,1,&v);break;}");
        w.println("        case OP_CASTORE: { jchar v=(jchar)POP_I(); jint idx=POP_I(); jobject arr=POP_L();");
        w.println("            (*env)->SetCharArrayRegion(env,arr,idx,1,&v);break;}");
        w.println("        case OP_SASTORE: { jshort v=(jshort)POP_I(); jint idx=POP_I(); jobject arr=POP_L();");
        w.println("            (*env)->SetShortArrayRegion(env,arr,idx,1,&v);break;}");
        w.println();

        // 栈操作
        w.println("        /* ===== 栈操作 ===== */");
        w.println("        case OP_POP:  frame.sp--; break;");
        w.println("        case OP_POP2: frame.sp -= 2; break;");
        w.println("        case OP_DUP:  { VMValue v=PEEK(); PUSH_RAW(v); break; }");
        w.println("        case OP_DUP_X1: {");
        w.println("            VMValue a=POP(), b=POP();");
        w.println("            PUSH_RAW(a); PUSH_RAW(b); PUSH_RAW(a); break; }");
        w.println("        case OP_DUP_X2: {");
        w.println("            VMValue a=POP(),b=POP(),c=POP();");
        w.println("            PUSH_RAW(a);PUSH_RAW(c);PUSH_RAW(b);PUSH_RAW(a);break;}");
        w.println("        case OP_DUP2: {");
        w.println("            VMValue a=frame.stack[frame.sp-1],b=frame.stack[frame.sp-2];");
        w.println("            PUSH_RAW(b);PUSH_RAW(a);break;}");
        w.println("        case OP_DUP2_X1: {");
        w.println("            VMValue a=POP(),b=POP(),c=POP();");
        w.println("            PUSH_RAW(b);PUSH_RAW(a);PUSH_RAW(c);PUSH_RAW(b);PUSH_RAW(a);break;}");
        w.println("        case OP_DUP2_X2: {");
        w.println("            VMValue a=POP(),b=POP(),c=POP(),d=POP();");
        w.println("            PUSH_RAW(b);PUSH_RAW(a);PUSH_RAW(d);PUSH_RAW(c);PUSH_RAW(b);PUSH_RAW(a);break;}");
        w.println("        case OP_SWAP: {");
        w.println("            VMValue a=POP(),b=POP(); PUSH_RAW(a);PUSH_RAW(b);break;}");
        w.println();

        // 算术
        w.println("        /* ===== 算术 ===== */");
        w.println("        case OP_IADD: { jint b=POP_I(),a=POP_I(); PUSH_I(a+b);break;}");
        w.println("        case OP_LADD: { jlong b=POP_J(),a=POP_J(); PUSH_J(a+b);break;}");
        w.println("        case OP_FADD: { jfloat b=POP_F(),a=POP_F(); PUSH_F(a+b);break;}");
        w.println("        case OP_DADD: { jdouble b=POP_D(),a=POP_D(); PUSH_D(a+b);break;}");
        w.println("        case OP_ISUB: { jint b=POP_I(),a=POP_I(); PUSH_I(a-b);break;}");
        w.println("        case OP_LSUB: { jlong b=POP_J(),a=POP_J(); PUSH_J(a-b);break;}");
        w.println("        case OP_FSUB: { jfloat b=POP_F(),a=POP_F(); PUSH_F(a-b);break;}");
        w.println("        case OP_DSUB: { jdouble b=POP_D(),a=POP_D(); PUSH_D(a-b);break;}");
        w.println("        case OP_IMUL: { jint b=POP_I(),a=POP_I(); PUSH_I(a*b);break;}");
        w.println("        case OP_LMUL: { jlong b=POP_J(),a=POP_J(); PUSH_J(a*b);break;}");
        w.println("        case OP_FMUL: { jfloat b=POP_F(),a=POP_F(); PUSH_F(a*b);break;}");
        w.println("        case OP_DMUL: { jdouble b=POP_D(),a=POP_D(); PUSH_D(a*b);break;}");
        w.println("        case OP_IDIV: { jint b=POP_I(),a=POP_I(); PUSH_I(a/b);break;}");
        w.println("        case OP_LDIV: { jlong b=POP_J(),a=POP_J(); PUSH_J(a/b);break;}");
        w.println("        case OP_FDIV: { jfloat b=POP_F(),a=POP_F(); PUSH_F(a/b);break;}");
        w.println("        case OP_DDIV: { jdouble b=POP_D(),a=POP_D(); PUSH_D(a/b);break;}");
        w.println("        case OP_IREM: { jint b=POP_I(),a=POP_I(); PUSH_I(a%b);break;}");
        w.println("        case OP_LREM: { jlong b=POP_J(),a=POP_J(); PUSH_J(a%b);break;}");
        w.println("        case OP_FREM: { jfloat b=POP_F(),a=POP_F(); PUSH_F((jfloat)fmod(a,b));break;}");
        w.println("        case OP_DREM: { jdouble b=POP_D(),a=POP_D(); PUSH_D(fmod(a,b));break;}");
        w.println("        case OP_INEG: { PUSH_I(-POP_I()); break;}");
        w.println("        case OP_LNEG: { PUSH_J(-POP_J()); break;}");
        w.println("        case OP_FNEG: { PUSH_F(-POP_F()); break;}");
        w.println("        case OP_DNEG: { PUSH_D(-POP_D()); break;}");
        w.println();

        // 位运算
        w.println("        /* ===== 位运算 ===== */");
        w.println("        case OP_ISHL:  { jint b=POP_I(),a=POP_I(); PUSH_I(a<<(b&0x1f));break;}");
        w.println("        case OP_LSHL:  { jint b=POP_I(); jlong a=POP_J(); PUSH_J(a<<(b&0x3f));break;}");
        w.println("        case OP_ISHR:  { jint b=POP_I(),a=POP_I(); PUSH_I(a>>(b&0x1f));break;}");
        w.println("        case OP_LSHR:  { jint b=POP_I(); jlong a=POP_J(); PUSH_J(a>>(b&0x3f));break;}");
        w.println("        case OP_IUSHR: { jint b=POP_I(); jint a=POP_I(); PUSH_I((int)((unsigned int)a>>(b&0x1f)));break;}");
        w.println("        case OP_LUSHR: { jint b=POP_I(); jlong a=POP_J(); PUSH_J((jlong)((uint64_t)a>>(b&0x3f)));break;}");
        w.println("        case OP_IAND: { jint b=POP_I(),a=POP_I(); PUSH_I(a&b);break;}");
        w.println("        case OP_LAND: { jlong b=POP_J(),a=POP_J(); PUSH_J(a&b);break;}");
        w.println("        case OP_IOR:  { jint b=POP_I(),a=POP_I(); PUSH_I(a|b);break;}");
        w.println("        case OP_LOR:  { jlong b=POP_J(),a=POP_J(); PUSH_J(a|b);break;}");
        w.println("        case OP_IXOR: { jint b=POP_I(),a=POP_I(); PUSH_I(a^b);break;}");
        w.println("        case OP_LXOR: { jlong b=POP_J(),a=POP_J(); PUSH_J(a^b);break;}");
        w.println();

        // IINC
        w.println("        case OP_IINC: {");
        w.println("            uint16_t idx = READ_U2(code,frame.pc);");
        w.println("            int16_t incr = (int16_t)READ_S2(code,frame.pc);");
        w.println("            frame.locals[idx].i += incr; break;");
        w.println("        }");
        w.println();

        // 类型转换
        w.println("        /* ===== 类型转换 ===== */");
        w.println("        case OP_I2L: { PUSH_J((jlong)POP_I()); break;}");
        w.println("        case OP_I2F: { PUSH_F((jfloat)POP_I()); break;}");
        w.println("        case OP_I2D: { PUSH_D((jdouble)POP_I()); break;}");
        w.println("        case OP_L2I: { PUSH_I((jint)POP_J()); break;}");
        w.println("        case OP_L2F: { PUSH_F((jfloat)POP_J()); break;}");
        w.println("        case OP_L2D: { PUSH_D((jdouble)POP_J()); break;}");
        w.println("        case OP_F2I: { PUSH_I((jint)POP_F()); break;}");
        w.println("        case OP_F2L: { PUSH_J((jlong)POP_F()); break;}");
        w.println("        case OP_F2D: { PUSH_D((jdouble)POP_F()); break;}");
        w.println("        case OP_D2I: { PUSH_I((jint)POP_D()); break;}");
        w.println("        case OP_D2L: { PUSH_J((jlong)POP_D()); break;}");
        w.println("        case OP_D2F: { PUSH_F((jfloat)POP_D()); break;}");
        w.println("        case OP_I2B: { PUSH_I((jbyte)POP_I()); break;}");
        w.println("        case OP_I2C: { PUSH_I((jchar)POP_I()); break;}");
        w.println("        case OP_I2S: { PUSH_I((jshort)POP_I()); break;}");
        w.println();

        // 比较
        w.println("        /* ===== 比较 ===== */");
        w.println("        case OP_LCMP: { jlong b=POP_J(),a=POP_J(); PUSH_I(a>b?1:(a<b?-1:0));break;}");
        w.println("        case OP_FCMPL: { jfloat b=POP_F(),a=POP_F(); PUSH_I(a>b?1:(a==b?0:-1));break;}");
        w.println("        case OP_FCMPG: { jfloat b=POP_F(),a=POP_F(); PUSH_I(a<b?-1:(a==b?0:1));break;}");
        w.println("        case OP_DCMPL: { jdouble b=POP_D(),a=POP_D(); PUSH_I(a>b?1:(a==b?0:-1));break;}");
        w.println("        case OP_DCMPG: { jdouble b=POP_D(),a=POP_D(); PUSH_I(a<b?-1:(a==b?0:1));break;}");
        w.println();

        // 条件分支
        w.println("        /* ===== 条件分支 ===== */");
        w.println("        case OP_IFEQ: { int32_t off=READ_S4(code,frame.pc); if(POP_I()==0) frame.pc=instr_pc+off; break;}");
        w.println("        case OP_IFNE: { int32_t off=READ_S4(code,frame.pc); if(POP_I()!=0) frame.pc=instr_pc+off; break;}");
        w.println("        case OP_IFLT: { int32_t off=READ_S4(code,frame.pc); if(POP_I()<0)  frame.pc=instr_pc+off; break;}");
        w.println("        case OP_IFGE: { int32_t off=READ_S4(code,frame.pc); if(POP_I()>=0) frame.pc=instr_pc+off; break;}");
        w.println("        case OP_IFGT: { int32_t off=READ_S4(code,frame.pc); if(POP_I()>0)  frame.pc=instr_pc+off; break;}");
        w.println("        case OP_IFLE: { int32_t off=READ_S4(code,frame.pc); if(POP_I()<=0) frame.pc=instr_pc+off; break;}");
        w.println();
        w.println("        case OP_IF_ICMPEQ: { int32_t off=READ_S4(code,frame.pc); jint b=POP_I(),a=POP_I(); if(a==b) frame.pc=instr_pc+off; break;}");
        w.println("        case OP_IF_ICMPNE: { int32_t off=READ_S4(code,frame.pc); jint b=POP_I(),a=POP_I(); if(a!=b) frame.pc=instr_pc+off; break;}");
        w.println("        case OP_IF_ICMPLT: { int32_t off=READ_S4(code,frame.pc); jint b=POP_I(),a=POP_I(); if(a<b)  frame.pc=instr_pc+off; break;}");
        w.println("        case OP_IF_ICMPGE: { int32_t off=READ_S4(code,frame.pc); jint b=POP_I(),a=POP_I(); if(a>=b) frame.pc=instr_pc+off; break;}");
        w.println("        case OP_IF_ICMPGT: { int32_t off=READ_S4(code,frame.pc); jint b=POP_I(),a=POP_I(); if(a>b)  frame.pc=instr_pc+off; break;}");
        w.println("        case OP_IF_ICMPLE: { int32_t off=READ_S4(code,frame.pc); jint b=POP_I(),a=POP_I(); if(a<=b) frame.pc=instr_pc+off; break;}");
        w.println("        case OP_IF_ACMPEQ: { int32_t off=READ_S4(code,frame.pc); jobject b=POP_L(),a=POP_L();");
        w.println("            if((*env)->IsSameObject(env,a,b)) frame.pc=instr_pc+off; break;}");
        w.println("        case OP_IF_ACMPNE: { int32_t off=READ_S4(code,frame.pc); jobject b=POP_L(),a=POP_L();");
        w.println("            if(!(*env)->IsSameObject(env,a,b)) frame.pc=instr_pc+off; break;}");
        w.println("        case OP_IFNULL:    { int32_t off=READ_S4(code,frame.pc); if(POP_L()==NULL) frame.pc=instr_pc+off; break;}");
        w.println("        case OP_IFNONNULL: { int32_t off=READ_S4(code,frame.pc); if(POP_L()!=NULL) frame.pc=instr_pc+off; break;}");
        w.println();

        // GOTO
        w.println("        case OP_GOTO: { int32_t off=READ_S4(code,frame.pc); frame.pc=instr_pc+off; break;}");
        w.println();

        // TABLESWITCH
        w.println("        /* ===== TABLESWITCH ===== */");
        w.println("        case OP_TABLESWITCH: {");
        w.println("            int32_t def_off = READ_S4(code,frame.pc);");
        w.println("            int32_t low = READ_S4(code,frame.pc);");
        w.println("            int32_t high = READ_S4(code,frame.pc);");
        w.println("            jint key = POP_I();");
        w.println("            if (key >= low && key <= high) {");
        w.println("                int idx = key - low;");
        w.println("                int save_pc = frame.pc;");
        w.println("                frame.pc += idx * 4;");
        w.println("                int32_t off = READ_S4(code,frame.pc);");
        w.println("                frame.pc = instr_pc + off;");
        w.println("            } else {");
        w.println("                frame.pc = instr_pc + def_off;");
        w.println("            }");
        w.println("            break;");
        w.println("        }");
        w.println();

        // LOOKUPSWITCH
        w.println("        /* ===== LOOKUPSWITCH ===== */");
        w.println("        case OP_LOOKUPSWITCH: {");
        w.println("            int32_t def_off = READ_S4(code,frame.pc);");
        w.println("            int32_t npairs = READ_S4(code,frame.pc);");
        w.println("            jint key = POP_I();");
        w.println("            int found = 0;");
        w.println("            int pairs_start = frame.pc;");
        w.println("            for (int i = 0; i < npairs; i++) {");
        w.println("                int32_t match = READ_S4(code,frame.pc);");
        w.println("                int32_t off = READ_S4(code,frame.pc);");
        w.println("                if (key == match) {");
        w.println("                    frame.pc = instr_pc + off;");
        w.println("                    found = 1;");
        w.println("                    break;");
        w.println("                }");
        w.println("            }");
        w.println("            if (!found) frame.pc = instr_pc + def_off;");
        w.println("            break;");
        w.println("        }");
        w.println();

        // RETURN
        w.println("        /* ===== RETURN ===== */");
        w.println("        case OP_IRETURN: result.i = POP_I(); goto cleanup;");
        w.println("        case OP_LRETURN: result.j = POP_J(); goto cleanup;");
        w.println("        case OP_FRETURN: result.f = POP_F(); goto cleanup;");
        w.println("        case OP_DRETURN: result.d = POP_D(); goto cleanup;");
        w.println("        case OP_ARETURN: result.l = POP_L(); goto cleanup;");
        w.println("        case OP_RETURN:  goto cleanup;");
        w.println();

        // 字段访问
        w.println("        /* ===== 字段 ===== */");
        w.println("        case OP_GETSTATIC: {");
        w.println("            uint16_t idx = READ_U2(code,frame.pc);");
        w.println("            VMCPEntry* e = &method->cp[idx];");
        w.println("            vm_resolve_field(env, e, 1);");
        w.println("            char ft = vm_get_field_type(e->val.ref.desc, e->val.ref.desc_len);");
        w.println("            switch(ft) {");
        w.println("                case 'I':case 'B':case 'C':case 'S':case 'Z': PUSH_I((*env)->GetStaticIntField(env,e->cached_class,e->cached_field));break;");
        w.println("                case 'J': PUSH_J((*env)->GetStaticLongField(env,e->cached_class,e->cached_field));break;");
        w.println("                case 'F': PUSH_F((*env)->GetStaticFloatField(env,e->cached_class,e->cached_field));break;");
        w.println("                case 'D': PUSH_D((*env)->GetStaticDoubleField(env,e->cached_class,e->cached_field));break;");
        w.println("                default:  PUSH_L((*env)->GetStaticObjectField(env,e->cached_class,e->cached_field));break;");
        w.println("            } break;");
        w.println("        }");
        w.println("        case OP_PUTSTATIC: {");
        w.println("            uint16_t idx = READ_U2(code,frame.pc);");
        w.println("            VMCPEntry* e = &method->cp[idx];");
        w.println("            vm_resolve_field(env, e, 1);");
        w.println("            char ft = vm_get_field_type(e->val.ref.desc, e->val.ref.desc_len);");
        w.println("            switch(ft) {");
        w.println("                case 'I':case 'B':case 'C':case 'S':case 'Z': (*env)->SetStaticIntField(env,e->cached_class,e->cached_field,POP_I());break;");
        w.println("                case 'J': (*env)->SetStaticLongField(env,e->cached_class,e->cached_field,POP_J());break;");
        w.println("                case 'F': (*env)->SetStaticFloatField(env,e->cached_class,e->cached_field,POP_F());break;");
        w.println("                case 'D': (*env)->SetStaticDoubleField(env,e->cached_class,e->cached_field,POP_D());break;");
        w.println("                default:  (*env)->SetStaticObjectField(env,e->cached_class,e->cached_field,POP_L());break;");
        w.println("            } break;");
        w.println("        }");
        w.println("        case OP_GETFIELD: {");
        w.println("            uint16_t idx = READ_U2(code,frame.pc);");
        w.println("            VMCPEntry* e = &method->cp[idx];");
        w.println("            vm_resolve_field(env, e, 0);");
        w.println("            jobject obj = POP_L();");
        w.println("            char ft = vm_get_field_type(e->val.ref.desc, e->val.ref.desc_len);");
        w.println("            switch(ft) {");
        w.println("                case 'I':case 'B':case 'C':case 'S':case 'Z': PUSH_I((*env)->GetIntField(env,obj,e->cached_field));break;");
        w.println("                case 'J': PUSH_J((*env)->GetLongField(env,obj,e->cached_field));break;");
        w.println("                case 'F': PUSH_F((*env)->GetFloatField(env,obj,e->cached_field));break;");
        w.println("                case 'D': PUSH_D((*env)->GetDoubleField(env,obj,e->cached_field));break;");
        w.println("                default:  PUSH_L((*env)->GetObjectField(env,obj,e->cached_field));break;");
        w.println("            } break;");
        w.println("        }");
        w.println("        case OP_PUTFIELD: {");
        w.println("            uint16_t idx = READ_U2(code,frame.pc);");
        w.println("            VMCPEntry* e = &method->cp[idx];");
        w.println("            vm_resolve_field(env, e, 0);");
        w.println("            char ft = vm_get_field_type(e->val.ref.desc, e->val.ref.desc_len);");
        w.println("            VMValue val = POP();");
        w.println("            jobject obj = POP_L();");
        w.println("            switch(ft) {");
        w.println("                case 'I':case 'B':case 'C':case 'S':case 'Z': (*env)->SetIntField(env,obj,e->cached_field,val.i);break;");
        w.println("                case 'J': (*env)->SetLongField(env,obj,e->cached_field,val.j);break;");
        w.println("                case 'F': (*env)->SetFloatField(env,obj,e->cached_field,val.f);break;");
        w.println("                case 'D': (*env)->SetDoubleField(env,obj,e->cached_field,val.d);break;");
        w.println("                default:  (*env)->SetObjectField(env,obj,e->cached_field,val.l);break;");
        w.println("            } break;");
        w.println("        }");
        w.println();

        // 方法调用
        w.println("        /* ===== 方法调用 ===== */");
        w.println("        case OP_INVOKEVIRTUAL:");
        w.println("        case OP_INVOKESPECIAL:");
        w.println("        case OP_INVOKESTATIC: {");
        w.println("            uint16_t idx = READ_U2(code,frame.pc);");
        w.println("            vm_invoke_method(env, &method->cp[idx], opcode, &frame);");
        w.println("            CHECK_EXCEPTION();");
        w.println("            break;");
        w.println("        }");
        w.println("        case OP_INVOKEINTERFACE: {");
        w.println("            uint16_t idx = READ_U2(code,frame.pc);");
        w.println("            frame.pc += 2; /* skip count + padding */");
        w.println("            vm_invoke_method(env, &method->cp[idx], OP_INVOKEVIRTUAL, &frame);");
        w.println("            CHECK_EXCEPTION();");
        w.println("            break;");
        w.println("        }");
        w.println("        case OP_INVOKEDYNAMIC: {");
        w.println("            uint16_t idx = READ_U2(code,frame.pc);");
        w.println("            frame.pc += 2; /* skip padding */");
        w.println("            VMCPEntry* e = &method->cp[idx];");
        w.println("            /* TODO: CallSite 缓存 + MethodHandle.invokeExact */");
        w.println("            jobject mh = vm_invoke_dynamic(env, method, e, frame.stack, &frame.sp);");
        w.println("            CHECK_EXCEPTION();");
        w.println("            if (mh) {");
        w.println("                /* 简化：通过 MethodHandle.invoke 调用 */");
        w.println("                /* 完整实现需要解析 indy_desc 的参数 */");
        w.println("            }");
        w.println("            break;");
        w.println("        }");
        w.println();

        // 对象创建
        w.println("        /* ===== 对象/数组 ===== */");
        w.println("        case OP_NEW: {");
        w.println("            uint16_t idx = READ_U2(code,frame.pc);");
        w.println("            jclass cls = vm_resolve_class(env, &method->cp[idx]);");
        w.println("            jobject obj = (*env)->AllocObject(env, cls);");
        w.println("            PUSH_L(obj);");
        w.println("            CHECK_EXCEPTION();");
        w.println("            break;");
        w.println("        }");
        w.println("        case OP_NEWARRAY: {");
        w.println("            uint8_t atype = READ_U1(code,frame.pc);");
        w.println("            jint count = POP_I();");
        w.println("            jobject arr = NULL;");
        w.println("            switch(atype) {");
        w.println("                case 4:  arr=(*env)->NewBooleanArray(env,count);break;");
        w.println("                case 5:  arr=(*env)->NewCharArray(env,count);break;");
        w.println("                case 6:  arr=(*env)->NewFloatArray(env,count);break;");
        w.println("                case 7:  arr=(*env)->NewDoubleArray(env,count);break;");
        w.println("                case 8:  arr=(*env)->NewByteArray(env,count);break;");
        w.println("                case 9:  arr=(*env)->NewShortArray(env,count);break;");
        w.println("                case 10: arr=(*env)->NewIntArray(env,count);break;");
        w.println("                case 11: arr=(*env)->NewLongArray(env,count);break;");
        w.println("            }");
        w.println("            PUSH_L(arr); break;");
        w.println("        }");
        w.println("        case OP_ANEWARRAY: {");
        w.println("            uint16_t idx = READ_U2(code,frame.pc);");
        w.println("            jint count = POP_I();");
        w.println("            jclass cls = vm_resolve_class(env, &method->cp[idx]);");
        w.println("            jobject arr = (*env)->NewObjectArray(env,count,cls,NULL);");
        w.println("            PUSH_L(arr); break;");
        w.println("        }");
        w.println("        case OP_ARRAYLENGTH: {");
        w.println("            jobject arr = POP_L();");
        w.println("            PUSH_I((*env)->GetArrayLength(env,arr)); break;");
        w.println("        }");
        w.println("        case OP_MULTIANEWARRAY: {");
        w.println("            uint16_t idx = READ_U2(code,frame.pc);");
        w.println("            uint8_t dims = READ_U1(code,frame.pc);");
        w.println("            /* 简化：只支持2维 */");
        w.println("            jclass cls = vm_resolve_class(env, &method->cp[idx]);");
        w.println("            jint* sizes = (jint*)alloca(sizeof(jint)*dims);");
        w.println("            for (int i = dims-1; i >= 0; i--) sizes[i] = POP_I();");
        w.println("            /* 递归创建 — 简化为 JNI NewObjectArray */");
        w.println("            jobject arr = (*env)->NewObjectArray(env, sizes[0], cls, NULL);");
        w.println("            PUSH_L(arr); break;");
        w.println("        }");
        w.println();

        // ATHROW
        w.println("        /* ===== ATHROW ===== */");
        w.println("        case OP_ATHROW: {");
        w.println("            jthrowable ex = (jthrowable)POP_L();");
        w.println("            int handler = vm_find_exception_handler(env, method, instr_pc, ex);");
        w.println("            if (handler >= 0) {");
        w.println("                frame.sp = 0;");
        w.println("                PUSH_L(ex);");
        w.println("                frame.pc = handler;");
        w.println("            } else {");
        w.println("                (*env)->Throw(env, ex);");
        w.println("                goto cleanup;");
        w.println("            }");
        w.println("            break;");
        w.println("        }");
        w.println();

        // CHECKCAST / INSTANCEOF
        w.println("        /* ===== 类型检查 ===== */");
        w.println("        case OP_CHECKCAST: {");
        w.println("            uint16_t idx = READ_U2(code,frame.pc);");
        w.println("            jclass cls = vm_resolve_class(env, &method->cp[idx]);");
        w.println("            jobject obj = PEEK().l;");
        w.println("            if (obj && !(*env)->IsInstanceOf(env, obj, cls)) {");
        w.println("                jclass ccex = (*env)->FindClass(env, \"java/lang/ClassCastException\");");
        w.println("                (*env)->ThrowNew(env, ccex, \"ClassCastException in VM\");");
        w.println("                CHECK_EXCEPTION();");
        w.println("            }");
        w.println("            break;");
        w.println("        }");
        w.println("        case OP_INSTANCEOF: {");
        w.println("            uint16_t idx = READ_U2(code,frame.pc);");
        w.println("            jclass cls = vm_resolve_class(env, &method->cp[idx]);");
        w.println("            jobject obj = POP_L();");
        w.println("            PUSH_I(obj ? (*env)->IsInstanceOf(env, obj, cls) : 0);");
        w.println("            break;");
        w.println("        }");
        w.println();

        // MONITOR
        w.println("        /* ===== 同步 ===== */");
        w.println("        case OP_MONITORENTER: {");
        w.println("            jobject obj = POP_L();");
        w.println("            (*env)->MonitorEnter(env, obj); break;");
        w.println("        }");
        w.println("        case OP_MONITOREXIT: {");
        w.println("            jobject obj = POP_L();");
        w.println("            (*env)->MonitorExit(env, obj); break;");
        w.println("        }");
        w.println();

        // DEFAULT
        w.println("        default:");
        w.println("            fprintf(stderr, \"[JNVM] Unknown opcode 0x%02x at pc=%d\\n\", opcode, instr_pc);");
        w.println("            goto cleanup;");
        w.println();

        w.println("        } /* end switch */");
        w.println("    } /* end while */");
        w.println();
        w.println("cleanup:");
        w.println("    /* 清除解密后的字节码 */");
        w.println("    memset(code, 0, method->code_length);");
        w.println("    return result;");
        w.println("}");
    }
    private void generateVmBridge(File dir) throws IOException {
        try (PrintWriter w = new PrintWriter(new FileWriter(new File(dir, "vm_bridge.c")))) {
            writeVmBridgeSource(w);
        }
    }

    private void writeVmBridgeSource(PrintWriter w) {
        w.println("/*");
        w.println(" * vm_bridge.c — JNI 桥接层 + Anti-Debug");
        w.println(" * 自动生成，请勿手动修改");
        w.println(" */");
        w.println();
        w.println("#include <jni.h>");
        w.println("#include <string.h>");
        w.println("#include <stdlib.h>");
        w.println("#include <stdio.h>");
        w.println("#include \"vm_types.h\"");
        w.println("#include \"vm_data.h\"");
        w.println("#include \"vm_interpreter.h\"");
        w.println();

        // 平台检测
        w.println("/* 平台检测 */");
        w.println("#if defined(__linux__) || defined(__ANDROID__)");
        w.println("  #define VM_PLATFORM_LINUX 1");
        w.println("  #include <unistd.h>");
        w.println("  #include <sys/ptrace.h>");
        w.println("  #include <pthread.h>");
        w.println("  #include <signal.h>");
        w.println("  #include <fcntl.h>");
        w.println("#elif defined(_WIN32) || defined(_WIN64)");
        w.println("  #define VM_PLATFORM_WINDOWS 1");
        w.println("  #include <windows.h>");
        w.println("#elif defined(__APPLE__)");
        w.println("  #define VM_PLATFORM_MACOS 1");
        w.println("  #include <unistd.h>");
        w.println("  #include <sys/types.h>");
        w.println("  #include <sys/sysctl.h>");
        w.println("  #include <pthread.h>");
        w.println("#endif");
        w.println();

        // Anti-debug
        if (config.isAntiDebug()) {
            writeAntiDebug(w);
        }

        // 装箱/拆箱辅助
        writeBoxingHelpers(w);

        // 核心 execute 函数
        writeExecuteFunction(w);

        // JNI_OnLoad
        writeJniOnLoad(w);
    }
    private void writeAntiDebug(PrintWriter w) {
        w.println("/* ========================================= */");
        w.println("/* ===== Anti-Debug / Anti-Tamper ========== */");
        w.println("/* ========================================= */");
        w.println();
        w.println("static volatile int vm_integrity_ok = 1;");
        w.println();

        // ===== Linux / Android =====
        w.println("#ifdef VM_PLATFORM_LINUX");
        w.println();

        // ptrace 检测
        w.println("static int vm_check_ptrace(void) {");
        w.println("    if (ptrace(PTRACE_TRACEME, 0, NULL, NULL) == -1) {");
        w.println("        return 1; /* 被调试 */");
        w.println("    }");
        w.println("    ptrace(PTRACE_DETACH, 0, NULL, NULL);");
        w.println("    return 0;");
        w.println("}");
        w.println();

        // /proc/self/status TracerPid 检测
        w.println("static int vm_check_tracer_pid(void) {");
        w.println("    char buf[4096];");
        w.println("    int fd = open(\"/proc/self/status\", O_RDONLY);");
        w.println("    if (fd < 0) return 0;");
        w.println("    int n = read(fd, buf, sizeof(buf) - 1);");
        w.println("    close(fd);");
        w.println("    if (n <= 0) return 0;");
        w.println("    buf[n] = '\\0';");
        w.println("    char* p = strstr(buf, \"TracerPid:\");");
        w.println("    if (!p) return 0;");
        w.println("    p += 10; /* strlen(\"TracerPid:\") */");
        w.println("    while (*p == ' ' || *p == '\\t') p++;");
        w.println("    int pid = atoi(p);");
        w.println("    return (pid != 0) ? 1 : 0;");
        w.println("}");
        w.println();

        // Frida 检测：检查常见端口和 maps
        w.println("static int vm_check_frida(void) {");
        w.println("    /* 检查 /proc/self/maps 中是否有 frida 相关映射 */");
        w.println("    char buf[8192];");
        w.println("    int fd = open(\"/proc/self/maps\", O_RDONLY);");
        w.println("    if (fd < 0) return 0;");
        w.println("    int detected = 0;");
        w.println("    int n;");
        w.println("    while ((n = read(fd, buf, sizeof(buf) - 1)) > 0) {");
        w.println("        buf[n] = '\\0';");
        w.println("        /* 加密比较，避免明文字符串 */");
        w.println("        const char* patterns[] = {");
        w.println("            \"frida\", \"gadget\", \"linjector\", NULL");
        w.println("        };");
        w.println("        for (int i = 0; patterns[i]; i++) {");
        w.println("            if (strstr(buf, patterns[i])) {");
        w.println("                detected = 1;");
        w.println("                break;");
        w.println("            }");
        w.println("        }");
        w.println("        if (detected) break;");
        w.println("    }");
        w.println("    close(fd);");
        w.println("    return detected;");
        w.println("}");
        w.println();

        // Xposed 检测（Android）
        w.println("#ifdef __ANDROID__");
        w.println("static int vm_check_xposed(JNIEnv* env) {");
        w.println("    /* 尝试加载 Xposed 相关类 */");
        w.println("    jclass cls = (*env)->FindClass(env, \"de/robv/android/xposed/XposedBridge\");");
        w.println("    if (cls) {");
        w.println("        (*env)->DeleteLocalRef(env, cls);");
        w.println("        (*env)->ExceptionClear(env);");
        w.println("        return 1;");
        w.println("    }");
        w.println("    (*env)->ExceptionClear(env);");
        w.println("    return 0;");
        w.println("}");
        w.println("#endif");
        w.println();

        // 后台监控线程
        w.println("static void* vm_watchdog_thread(void* arg) {");
        w.println("    (void)arg;");
        w.println("    while (vm_integrity_ok) {");
        w.println("        /* 每 500ms 检查一次 */");
        w.println("        usleep(500000);");
        w.println("        if (vm_check_tracer_pid()) {");
        w.println("            vm_integrity_ok = 0;");
        w.println("            fprintf(stderr, \"[JNVM] Integrity violation detected\\n\");");
        w.println("            _exit(1);");
        w.println("        }");
        w.println("        if (vm_check_frida()) {");
        w.println("            vm_integrity_ok = 0;");
        w.println("            _exit(1);");
        w.println("        }");
        w.println("    }");
        w.println("    return NULL;");
        w.println("}");
        w.println();

        w.println("static void vm_start_watchdog(void) {");
        w.println("    pthread_t tid;");
        w.println("    pthread_attr_t attr;");
        w.println("    pthread_attr_init(&attr);");
        w.println("    pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_DETACHED);");
        w.println("    pthread_create(&tid, &attr, vm_watchdog_thread, NULL);");
        w.println("    pthread_attr_destroy(&attr);");
        w.println("}");
        w.println();
        w.println("#endif /* VM_PLATFORM_LINUX */");
        w.println();

        // ===== Windows =====
        w.println("#ifdef VM_PLATFORM_WINDOWS");
        w.println();
        w.println("static int vm_check_debugger_win(void) {");
        w.println("    return IsDebuggerPresent() ? 1 : 0;");
        w.println("}");
        w.println();
        w.println("static int vm_check_remote_debugger_win(void) {");
        w.println("    BOOL present = FALSE;");
        w.println("    CheckRemoteDebuggerPresent(GetCurrentProcess(), &present);");
        w.println("    return present ? 1 : 0;");
        w.println("}");
        w.println();
        w.println("static DWORD WINAPI vm_watchdog_thread_win(LPVOID arg) {");
        w.println("    (void)arg;");
        w.println("    while (vm_integrity_ok) {");
        w.println("        Sleep(500);");
        w.println("        if (vm_check_debugger_win() || vm_check_remote_debugger_win()) {");
        w.println("            vm_integrity_ok = 0;");
        w.println("            ExitProcess(1);");
        w.println("        }");
        w.println("    }");
        w.println("    return 0;");
        w.println("}");
        w.println();
        w.println("static void vm_start_watchdog(void) {");
        w.println("    CreateThread(NULL, 0, vm_watchdog_thread_win, NULL, 0, NULL);");
        w.println("}");
        w.println();
        w.println("#endif /* VM_PLATFORM_WINDOWS */");
        w.println();

        // ===== macOS =====
        w.println("#ifdef VM_PLATFORM_MACOS");
        w.println();
        w.println("static int vm_check_debugger_mac(void) {");
        w.println("    int mib[4] = { CTL_KERN, KERN_PROC, KERN_PROC_PID, getpid() };");
        w.println("    struct kinfo_proc info;");
        w.println("    size_t size = sizeof(info);");
        w.println("    memset(&info, 0, sizeof(info));");
        w.println("    sysctl(mib, 4, &info, &size, NULL, 0);");
        w.println("    return (info.kp_proc.p_flag & P_TRACED) ? 1 : 0;");
        w.println("}");
        w.println();
        w.println("static void* vm_watchdog_thread(void* arg) {");
        w.println("    (void)arg;");
        w.println("    while (vm_integrity_ok) {");
        w.println("        usleep(500000);");
        w.println("        if (vm_check_debugger_mac()) {");
        w.println("            vm_integrity_ok = 0;");
        w.println("            _exit(1);");
        w.println("        }");
        w.println("    }");
        w.println("    return NULL;");
        w.println("}");
        w.println();
        w.println("static void vm_start_watchdog(void) {");
        w.println("    pthread_t tid;");
        w.println("    pthread_attr_t attr;");
        w.println("    pthread_attr_init(&attr);");
        w.println("    pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_DETACHED);");
        w.println("    pthread_create(&tid, &attr, vm_watchdog_thread, NULL);");
        w.println("    pthread_attr_destroy(&attr);");
        w.println("}");
        w.println();
        w.println("#endif /* VM_PLATFORM_MACOS */");
        w.println();

        // 统一初始化入口
        w.println("static void vm_anti_debug_init(JNIEnv* env) {");
        w.println("    (void)env;");
        w.println("#ifdef VM_PLATFORM_LINUX");
        w.println("    if (vm_check_ptrace()) {");
        w.println("        fprintf(stderr, \"[JNVM] Debug detected (ptrace)\\n\");");
        w.println("        _exit(1);");
        w.println("    }");
        w.println("    if (vm_check_tracer_pid()) {");
        w.println("        fprintf(stderr, \"[JNVM] Debug detected (TracerPid)\\n\");");
        w.println("        _exit(1);");
        w.println("    }");
        w.println("    if (vm_check_frida()) {");
        w.println("        fprintf(stderr, \"[JNVM] Hook framework detected\\n\");");
        w.println("        _exit(1);");
        w.println("    }");
        w.println("#ifdef __ANDROID__");
        w.println("    if (vm_check_xposed(env)) {");
        w.println("        _exit(1);");
        w.println("    }");
        w.println("#endif");
        w.println("    vm_start_watchdog();");
        w.println("#endif");
        w.println();
        w.println("#ifdef VM_PLATFORM_WINDOWS");
        w.println("    if (vm_check_debugger_win() || vm_check_remote_debugger_win()) {");
        w.println("        ExitProcess(1);");
        w.println("    }");
        w.println("    vm_start_watchdog();");
        w.println("#endif");
        w.println();
        w.println("#ifdef VM_PLATFORM_MACOS");
        w.println("    if (vm_check_debugger_mac()) {");
        w.println("        _exit(1);");
        w.println("    }");
        w.println("    vm_start_watchdog();");
        w.println("#endif");
        w.println("}");
        w.println();
    }
    private void writeBoxingHelpers(PrintWriter w) {
        w.println("/* ========================================= */");
        w.println("/* ===== 装箱 / 拆箱 辅助 ================= */");
        w.println("/* ========================================= */");
        w.println();

        // 缓存的类和方法 ID
        w.println("static jclass cls_Integer   = NULL;");
        w.println("static jclass cls_Long      = NULL;");
        w.println("static jclass cls_Float     = NULL;");
        w.println("static jclass cls_Double    = NULL;");
        w.println("static jclass cls_Boolean   = NULL;");
        w.println("static jclass cls_Byte      = NULL;");
        w.println("static jclass cls_Short     = NULL;");
        w.println("static jclass cls_Character = NULL;");
        w.println();
        w.println("static jmethodID mid_Integer_intValue     = NULL;");
        w.println("static jmethodID mid_Long_longValue       = NULL;");
        w.println("static jmethodID mid_Float_floatValue     = NULL;");
        w.println("static jmethodID mid_Double_doubleValue   = NULL;");
        w.println("static jmethodID mid_Boolean_booleanValue = NULL;");
        w.println("static jmethodID mid_Byte_byteValue       = NULL;");
        w.println("static jmethodID mid_Short_shortValue     = NULL;");
        w.println("static jmethodID mid_Character_charValue  = NULL;");
        w.println();
        w.println("static jmethodID mid_Integer_valueOf   = NULL;");
        w.println("static jmethodID mid_Long_valueOf      = NULL;");
        w.println("static jmethodID mid_Float_valueOf     = NULL;");
        w.println("static jmethodID mid_Double_valueOf    = NULL;");
        w.println("static jmethodID mid_Boolean_valueOf   = NULL;");
        w.println("static jmethodID mid_Byte_valueOf      = NULL;");
        w.println("static jmethodID mid_Short_valueOf     = NULL;");
        w.println("static jmethodID mid_Character_valueOf = NULL;");
        w.println();

        // 初始化缓存
        w.println("static void vm_init_boxing(JNIEnv* env) {");
        w.println("    #define CACHE_CLASS(var, name) \\");
        w.println("        { jclass c = (*env)->FindClass(env, name); \\");
        w.println("          var = (jclass)(*env)->NewGlobalRef(env, c); \\");
        w.println("          (*env)->DeleteLocalRef(env, c); }");
        w.println();
        w.println("    CACHE_CLASS(cls_Integer,   \"java/lang/Integer\");");
        w.println("    CACHE_CLASS(cls_Long,      \"java/lang/Long\");");
        w.println("    CACHE_CLASS(cls_Float,     \"java/lang/Float\");");
        w.println("    CACHE_CLASS(cls_Double,    \"java/lang/Double\");");
        w.println("    CACHE_CLASS(cls_Boolean,   \"java/lang/Boolean\");");
        w.println("    CACHE_CLASS(cls_Byte,      \"java/lang/Byte\");");
        w.println("    CACHE_CLASS(cls_Short,     \"java/lang/Short\");");
        w.println("    CACHE_CLASS(cls_Character, \"java/lang/Character\");");
        w.println();
        w.println("    mid_Integer_intValue     = (*env)->GetMethodID(env, cls_Integer,   \"intValue\",     \"()I\");");
        w.println("    mid_Long_longValue       = (*env)->GetMethodID(env, cls_Long,      \"longValue\",    \"()J\");");
        w.println("    mid_Float_floatValue     = (*env)->GetMethodID(env, cls_Float,     \"floatValue\",   \"()F\");");
        w.println("    mid_Double_doubleValue   = (*env)->GetMethodID(env, cls_Double,    \"doubleValue\",  \"()D\");");
        w.println("    mid_Boolean_booleanValue = (*env)->GetMethodID(env, cls_Boolean,   \"booleanValue\", \"()Z\");");
        w.println("    mid_Byte_byteValue       = (*env)->GetMethodID(env, cls_Byte,      \"byteValue\",    \"()B\");");
        w.println("    mid_Short_shortValue     = (*env)->GetMethodID(env, cls_Short,     \"shortValue\",   \"()S\");");
        w.println("    mid_Character_charValue  = (*env)->GetMethodID(env, cls_Character, \"charValue\",    \"()C\");");
        w.println();
        w.println("    mid_Integer_valueOf   = (*env)->GetStaticMethodID(env, cls_Integer,   \"valueOf\", \"(I)Ljava/lang/Integer;\");");
        w.println("    mid_Long_valueOf      = (*env)->GetStaticMethodID(env, cls_Long,      \"valueOf\", \"(J)Ljava/lang/Long;\");");
        w.println("    mid_Float_valueOf     = (*env)->GetStaticMethodID(env, cls_Float,     \"valueOf\", \"(F)Ljava/lang/Float;\");");
        w.println("    mid_Double_valueOf    = (*env)->GetStaticMethodID(env, cls_Double,    \"valueOf\", \"(D)Ljava/lang/Double;\");");
        w.println("    mid_Boolean_valueOf   = (*env)->GetStaticMethodID(env, cls_Boolean,   \"valueOf\", \"(Z)Ljava/lang/Boolean;\");");
        w.println("    mid_Byte_valueOf      = (*env)->GetStaticMethodID(env, cls_Byte,      \"valueOf\", \"(B)Ljava/lang/Byte;\");");
        w.println("    mid_Short_valueOf     = (*env)->GetStaticMethodID(env, cls_Short,     \"valueOf\", \"(S)Ljava/lang/Short;\");");
        w.println("    mid_Character_valueOf = (*env)->GetStaticMethodID(env, cls_Character, \"valueOf\", \"(C)Ljava/lang/Character;\");");
        w.println();
        w.println("    #undef CACHE_CLASS");
        w.println("}");
        w.println();

        // 拆箱：Object → VMValue（根据描述符类型）
        w.println("/**");
        w.println(" * 将装箱的 Object 参数拆箱为 VMValue");
        w.println(" * @param type_char 描述符字符: I,J,F,D,Z,B,S,C,L,[");
        w.println(" */");
        w.println("static VMValue vm_unbox(JNIEnv* env, jobject obj, char type_char) {");
        w.println("    VMValue v;");
        w.println("    v.j = 0;");
        w.println("    if (obj == NULL) { v.l = NULL; return v; }");
        w.println("    switch (type_char) {");
        w.println("        case 'I': v.i = (*env)->CallIntMethod(env, obj, mid_Integer_intValue); break;");
        w.println("        case 'J': v.j = (*env)->CallLongMethod(env, obj, mid_Long_longValue); break;");
        w.println("        case 'F': v.f = (*env)->CallFloatMethod(env, obj, mid_Float_floatValue); break;");
        w.println("        case 'D': v.d = (*env)->CallDoubleMethod(env, obj, mid_Double_doubleValue); break;");
        w.println("        case 'Z': v.i = (*env)->CallBooleanMethod(env, obj, mid_Boolean_booleanValue); break;");
        w.println("        case 'B': v.i = (*env)->CallByteMethod(env, obj, mid_Byte_byteValue); break;");
        w.println("        case 'S': v.i = (*env)->CallShortMethod(env, obj, mid_Short_shortValue); break;");
        w.println("        case 'C': v.i = (*env)->CallCharMethod(env, obj, mid_Character_charValue); break;");
        w.println("        default:  v.l = obj; break; /* L or [ */");
        w.println("    }");
        w.println("    return v;");
        w.println("}");
        w.println();

        // 装箱：VMValue → Object
        w.println("/**");
        w.println(" * 将 VMValue 返回值装箱为 Object");
        w.println(" */");
        w.println("static jobject vm_box(JNIEnv* env, VMValue val, char type_char) {");
        w.println("    switch (type_char) {");
        w.println("        case 'I': return (*env)->CallStaticObjectMethod(env, cls_Integer,   mid_Integer_valueOf,   val.i);");
        w.println("        case 'J': return (*env)->CallStaticObjectMethod(env, cls_Long,      mid_Long_valueOf,      val.j);");
        w.println("        case 'F': return (*env)->CallStaticObjectMethod(env, cls_Float,     mid_Float_valueOf,     val.f);");
        w.println("        case 'D': return (*env)->CallStaticObjectMethod(env, cls_Double,    mid_Double_valueOf,    val.d);");
        w.println("        case 'Z': return (*env)->CallStaticObjectMethod(env, cls_Boolean,   mid_Boolean_valueOf,   (jboolean)val.i);");
        w.println("        case 'B': return (*env)->CallStaticObjectMethod(env, cls_Byte,      mid_Byte_valueOf,      (jbyte)val.i);");
        w.println("        case 'S': return (*env)->CallStaticObjectMethod(env, cls_Short,     mid_Short_valueOf,     (jshort)val.i);");
        w.println("        case 'C': return (*env)->CallStaticObjectMethod(env, cls_Character, mid_Character_valueOf, (jchar)val.i);");
        w.println("        case 'V': return NULL;");
        w.println("        default:  return val.l; /* L or [ */");
        w.println("    }");
        w.println("}");
        w.println();

        // 解析描述符提取参数类型字符列表
        w.println("/**");
        w.println(" * 解析方法描述符，提取参数类型字符和返回类型字符");
        w.println(" * @param desc      方法描述符 e.g. \"(ILjava/lang/String;D)V\"");
        w.println(" * @param param_types 输出：参数类型字符数组");
        w.println(" * @return 参数个数");
        w.println(" */");
        w.println("static int vm_parse_descriptor(const char* desc, char* param_types, char* ret_type) {");
        w.println("    int count = 0;");
        w.println("    const char* p = desc + 1; /* skip '(' */");
        w.println("    while (*p != ')') {");
        w.println("        char c = *p;");
        w.println("        switch (c) {");
        w.println("            case 'B': case 'C': case 'D': case 'F':  ");
        w.println("            case 'I': case 'J': case 'S': case 'Z':");
        w.println("                param_types[count++] = c;");
        w.println("                p++;");
        w.println("                break;");
        w.println("            case 'L':");
        w.println("                param_types[count++] = 'L';");
        w.println("                while (*p != ';') p++;");
        w.println("                p++;");
        w.println("                break;");
        w.println("            case '[':");
        w.println("                param_types[count++] = '[';");
        w.println("                while (*p == '[') p++;");
        w.println("                if (*p == 'L') { while (*p != ';') p++; p++; }");
        w.println("                else p++;");
        w.println("                break;");
        w.println("            default:");
        w.println("                p++;");
        w.println("                break;");
        w.println("        }");
        w.println("    }");
        w.println("    p++; /* skip ')' */");
        w.println("    *ret_type = *p;");
        w.println("    return count;");
        w.println("}");
        w.println();
    }
    private void writeExecuteFunction(PrintWriter w) {
        w.println("/* ========================================= */");
        w.println("/* ===== 核心 execute 入口 ================= */");
        w.println("/* ========================================= */");
        w.println();

        w.println("/**");
        w.println(" * 全局单一 native 入口。");
        w.println(" * Java 签名: static native Object execute(int methodId, Object instance, Object[] args)");
        w.println(" */");
        w.println("static jobject JNICALL vm_execute(JNIEnv* env, jclass clazz,");
        w.println("                                   jint methodId, jobject instance,");
        w.println("                                   jobjectArray args) {");
        w.println("    (void)clazz;");
        w.println();

        // 完整性检查
        if (config.isAntiDebug()) {
            w.println("    /* Anti-debug 运行时检查 */");
            w.println("    if (!vm_integrity_ok) {");
            w.println("        jclass ex = (*env)->FindClass(env, \"java/lang/SecurityException\");");
            w.println("        (*env)->ThrowNew(env, ex, \"VM integrity check failed\");");
            w.println("        return NULL;");
            w.println("    }");
            w.println();
        }

        // 查找方法
        w.println("    /* 查找方法 */");
        w.println("    if (methodId < 0 || methodId >= VM_METHOD_COUNT) {");
        w.println("        jclass ex = (*env)->FindClass(env, \"java/lang/IllegalArgumentException\");");
        w.println("        (*env)->ThrowNew(env, ex, \"Invalid VM method ID\");");
        w.println("        return NULL;");
        w.println("    }");
        w.println("    VMMethodInfo* method = &vm_methods[methodId];");
        w.println();

        // 解析描述符
        w.println("    /* 解析方法描述符 */");
        w.println("    char desc_buf[512];");
        w.println("    vm_decrypt_str(method->desc, method->desc_len, desc_buf);");
        w.println();
        w.println("    char param_types[256];");
        w.println("    char ret_type;");
        w.println("    int param_count = vm_parse_descriptor(desc_buf, param_types, &ret_type);");
        w.println();

        // 构建 locals 数组
        w.println("    /* 构建 locals 数组 */");
        w.println("    VMValue locals[256];");
        w.println("    memset(locals, 0, sizeof(locals));");
        w.println("    int local_idx = 0;");
        w.println();
        w.println("    /* 非静态方法：locals[0] = this */");
        w.println("    if (!method->is_static) {");
        w.println("        locals[local_idx].l = instance;");
        w.println("        local_idx++;");
        w.println("    }");
        w.println();
        w.println("    /* 拆箱参数到 locals */");
        w.println("    int args_len = args ? (*env)->GetArrayLength(env, args) : 0;");
        w.println("    for (int i = 0; i < param_count && i < args_len; i++) {");
        w.println("        jobject arg = (*env)->GetObjectArrayElement(env, args, i);");
        w.println("        locals[local_idx] = vm_unbox(env, arg, param_types[i]);");
        w.println("        local_idx++;");
        w.println("        /* long/double 占两个 slots */");
        w.println("        if (param_types[i] == 'J' || param_types[i] == 'D') {");
        w.println("            local_idx++; /* 跳过第二个 slot */");
        w.println("        }");
        w.println("        if (arg) (*env)->DeleteLocalRef(env, arg);");
        w.println("    }");
        w.println();

        // 同步方法处理
        w.println("    /* 同步方法：进入 monitor */");
        w.println("    jobject monitor_obj = NULL;");
        w.println("    if (method->is_synchronized) {");
        w.println("        if (method->is_static) {");
        w.println("            /* 静态同步：锁类对象 */");
        w.println("            char owner_buf[512];");
        w.println("            vm_decrypt_str(method->owner, method->owner_len, owner_buf);");
        w.println("            monitor_obj = (*env)->FindClass(env, owner_buf);");
        w.println("        } else {");
        w.println("            monitor_obj = instance;");
        w.println("        }");
        w.println("        if (monitor_obj) (*env)->MonitorEnter(env, monitor_obj);");
        w.println("    }");
        w.println();

        // 调用解释器
        w.println("    /* 调用解释器 */");
        w.println("    VMValue result = vm_interpret(env, method, locals, local_idx);");
        w.println();

        // 同步方法：退出 monitor
        w.println("    /* 同步方法：退出 monitor */");
        w.println("    if (method->is_synchronized && monitor_obj) {");
        w.println("        (*env)->MonitorExit(env, monitor_obj);");
        w.println("    }");
        w.println();

        // 异常传播
        w.println("    /* 检查是否有异常需要传播 */");
        w.println("    if ((*env)->ExceptionCheck(env)) {");
        w.println("        return NULL;");
        w.println("    }");
        w.println();

        // 装箱返回值
        w.println("    /* 装箱返回值 */");
        w.println("    return vm_box(env, result, ret_type);");
        w.println("}");
        w.println();
    }
    private void writeJniOnLoad(PrintWriter w) {
        w.println("/* ========================================= */");
        w.println("/* ===== JNI_OnLoad ======================== */");
        w.println("/* ========================================= */");
        w.println();

        // VMBridge 类名（加密）
        String bridgeClass = "com/alphaautoleak/jnvm/runtime/VMBridge";
        byte[] bridgeEnc = com.alphaautoleak.jnvm.crypto.StringEncryptor.encrypt(bridgeClass, stringKey);
        w.println("static const unsigned char vm_bridge_class_enc[] = " +
                com.alphaautoleak.jnvm.crypto.CryptoUtils.toCArrayLiteral(bridgeEnc) + ";");
        w.println("static const int vm_bridge_class_enc_len = " + bridgeEnc.length + ";");
        w.println();

        // native 方法签名（加密）
        String execSig = "(ILjava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;";
        byte[] sigEnc = com.alphaautoleak.jnvm.crypto.StringEncryptor.encrypt(execSig, stringKey);
        w.println("static const unsigned char vm_exec_sig_enc[] = " +
                com.alphaautoleak.jnvm.crypto.CryptoUtils.toCArrayLiteral(sigEnc) + ";");
        w.println("static const int vm_exec_sig_enc_len = " + sigEnc.length + ";");
        w.println();

        String execName = "execute";
        byte[] nameEnc = com.alphaautoleak.jnvm.crypto.StringEncryptor.encrypt(execName, stringKey);
        w.println("static const unsigned char vm_exec_name_enc[] = " +
                com.alphaautoleak.jnvm.crypto.CryptoUtils.toCArrayLiteral(nameEnc) + ";");
        w.println("static const int vm_exec_name_enc_len = " + nameEnc.length + ";");
        w.println();

        // JNIEXPORT JNI_OnLoad
        w.println("JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {");
        w.println("    (void)reserved;");
        w.println("    JNIEnv* env;");
        w.println("    if ((*vm)->GetEnv(vm, (void**)&env, JNI_VERSION_1_6) != JNI_OK) {");
        w.println("        return JNI_ERR;");
        w.println("    }");
        w.println();

        // Anti-debug 初始化
        if (config.isAntiDebug()) {
            w.println("    /* Anti-debug 初始化 */");
            w.println("    vm_anti_debug_init(env);");
            w.println();
        }

        // 初始化数据
        w.println("    /* 初始化方法数据 */");
        w.println("    vm_data_init();");
        w.println();

        // 初始化装箱辅助
        w.println("    /* 初始化装箱/拆箱缓存 */");
        w.println("    vm_init_boxing(env);");
        w.println();

        // RegisterNatives
        w.println("    /* 解密桥接类名和方法签名 */");
        w.println("    char bridge_class_buf[256];");
        w.println("    vm_decrypt_str(vm_bridge_class_enc, vm_bridge_class_enc_len, bridge_class_buf);");
        w.println();
        w.println("    char exec_name_buf[64];");
        w.println("    vm_decrypt_str(vm_exec_name_enc, vm_exec_name_enc_len, exec_name_buf);");
        w.println();
        w.println("    char exec_sig_buf[256];");
        w.println("    vm_decrypt_str(vm_exec_sig_enc, vm_exec_sig_enc_len, exec_sig_buf);");
        w.println();
        w.println("    /* 查找桥接类 */");
        w.println("    jclass bridgeClass = (*env)->FindClass(env, bridge_class_buf);");
        w.println("    if (!bridgeClass) {");
        w.println("        fprintf(stderr, \"[JNVM] Cannot find bridge class: %s\\n\", bridge_class_buf);");
        w.println("        return JNI_ERR;");
        w.println("    }");
        w.println();
        w.println("    /* 注册 native 方法 — 只注册一个！ */");
        w.println("    JNINativeMethod methods[] = {");
        w.println("        { exec_name_buf, exec_sig_buf, (void*)vm_execute }");
        w.println("    };");
        w.println();
        w.println("    if ((*env)->RegisterNatives(env, bridgeClass, methods, 1) < 0) {");
        w.println("        fprintf(stderr, \"[JNVM] RegisterNatives failed\\n\");");
        w.println("        return JNI_ERR;");
        w.println("    }");
        w.println();

        // 清除解密缓冲区
        w.println("    /* 清除敏感缓冲区 */");
        w.println("    memset(bridge_class_buf, 0, sizeof(bridge_class_buf));");
        w.println("    memset(exec_name_buf, 0, sizeof(exec_name_buf));");
        w.println("    memset(exec_sig_buf, 0, sizeof(exec_sig_buf));");
        w.println();
        w.println("    fprintf(stdout, \"[JNVM] Native VM loaded. %d methods protected.\\n\", VM_METHOD_COUNT);");
        w.println("    return JNI_VERSION_1_6;");
        w.println("}");
        w.println();

        // JNI_OnUnload
        w.println("JNIEXPORT void JNICALL JNI_OnUnload(JavaVM* vm, void* reserved) {");
        w.println("    (void)vm;");
        w.println("    (void)reserved;");
        w.println("    vm_integrity_ok = 0;");
        w.println("    vm_data_destroy();");
        w.println("}");
    }

    // ========================================================
    // build.zig
    // ========================================================
    private void generateBuildZig(File dir) throws IOException {
        try (PrintWriter w = new PrintWriter(new FileWriter(new File(dir, "build.zig")))) {
            w.println("const std = @import(\"std\");");
            w.println();
            w.println("pub fn build(b: *std.Build) void {");
            w.println("    const target = b.standardTargetOptions(.{});");
            w.println("    const optimize = b.standardOptimizeOption(.{});");
            w.println();
            w.println("    const lib = b.addSharedLibrary(.{");
            w.println("        .name = \"customvm\",");
            w.println("        .target = target,");
            w.println("        .optimize = optimize,");
            w.println("    });");
            w.println();
            w.println("    lib.addCSourceFiles(.{");
            w.println("        .files = &.{");
            w.println("            \"vm_data.c\",");
            w.println("            \"vm_interpreter.c\",");
            w.println("            \"vm_bridge.c\",");
            w.println("            \"chacha20.c\",");
            w.println("        },");
            w.println("        .flags = &.{");
            w.println("            \"-std=c11\",");
            w.println("            \"-fvisibility=hidden\",");
            w.println("            \"-fstack-protector-strong\",");
            w.println("            \"-O2\",");
            w.println("            \"-Wall\",");
            w.println("            \"-Wextra\",");
            w.println("        },");
            w.println("    });");
            w.println();

            // JNI include paths
            w.println("    // JNI headers — 根据目标平台选择");
            w.println("    const java_home = std.process.getEnvVarOwned(b.allocator, \"JAVA_HOME\") catch \"\";");
            w.println("    if (java_home.len > 0) {");
            w.println("        lib.addIncludePath(.{ .cwd_relative = b.fmt(\"{s}/include\", .{java_home}) });");
            w.println("        // 平台子目录");
            w.println("        const target_triple = target.result.osTagName();");
            w.println("        if (std.mem.indexOf(u8, target_triple, \"linux\") != null) {");
            w.println("            lib.addIncludePath(.{ .cwd_relative = b.fmt(\"{s}/include/linux\", .{java_home}) });");
            w.println("        } else if (std.mem.indexOf(u8, target_triple, \"windows\") != null) {");
            w.println("            lib.addIncludePath(.{ .cwd_relative = b.fmt(\"{s}/include/win32\", .{java_home}) });");
            w.println("        } else if (std.mem.indexOf(u8, target_triple, \"macos\") != null or");
            w.println("                  std.mem.indexOf(u8, target_triple, \"darwin\") != null) {");
            w.println("            lib.addIncludePath(.{ .cwd_relative = b.fmt(\"{s}/include/darwin\", .{java_home}) });");
            w.println("        }");
            w.println("    }");
            w.println();

            w.println("    lib.linker_set_lazy_binding = false;");
            w.println("    lib.link_libc = true;");
            w.println();
            w.println("    b.installArtifact(lib);");
            w.println("}");
        }
    }
}