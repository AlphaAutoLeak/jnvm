package com.alphaautoleak.jnvm.codegen.emitter;

import com.alphaautoleak.jnvm.codegen.emitter.helper.VMHelper;
import com.alphaautoleak.jnvm.codegen.emitter.helper.VMHelpers;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Generates vm_interpreter.h and vm_interpreter.c - VM interpreter core
 * Generates separate functions for each return type to avoid boxing/unboxing
 */
public class VmInterpreterGenerator {
    
    private final File dir;
    private final boolean debug;
    private final boolean encryptStrings;
    private final Instructions instructions;
    private final VMHelpers helpers;
    private final int methodIdXorKey;
    
    public VmInterpreterGenerator(File dir, boolean debug, boolean encryptStrings, int methodIdXorKey) {
        this.dir = dir;
        this.debug = debug;
        this.encryptStrings = encryptStrings;
        this.instructions = new Instructions();
        this.helpers = new VMHelpers(encryptStrings);
        this.methodIdXorKey = methodIdXorKey;
    }
    
    public void generate() throws IOException {
        generateHeader();
        generateSource();
    }
    
    private void generateHeader() throws IOException {
        try (PrintWriter w = new PrintWriter(new java.io.FileWriter(new File(dir, "vm_interpreter.h")))) {
            w.println("#ifndef VM_INTERPRETER_H");
            w.println("#define VM_INTERPRETER_H");
            w.println("#include \"vm_types.h\"");
            w.println();
            
            // Frame memory pool initialization
            w.println("// Frame memory pool initialization (called in JNI_OnLoad)");
            w.println("void frame_pool_init(void);");
            w.println();

            // Helper function declarations
            for (VMHelper helper : helpers.getAllHelpers()) {
                helper.generateHeader(w);
            }

            w.println();
            // Execution function declarations for each return type
            w.println("void vm_execute_method_void(JNIEnv* env, int methodId, jobject instance, jobjectArray args, jclass callerClass);");
            w.println("jint vm_execute_method_int(JNIEnv* env, int methodId, jobject instance, jobjectArray args, jclass callerClass);");
            w.println("jlong vm_execute_method_long(JNIEnv* env, int methodId, jobject instance, jobjectArray args, jclass callerClass);");
            w.println("jfloat vm_execute_method_float(JNIEnv* env, int methodId, jobject instance, jobjectArray args, jclass callerClass);");
            w.println("jdouble vm_execute_method_double(JNIEnv* env, int methodId, jobject instance, jobjectArray args, jclass callerClass);");
            w.println("jobject vm_execute_method_object(JNIEnv* env, int methodId, jobject instance, jobjectArray args, jclass callerClass);");
            w.println();
            w.println("#endif");
        }
    }
    
    private void generateSource() throws IOException {
        try (PrintWriter w = new PrintWriter(new java.io.FileWriter(new File(dir, "vm_interpreter.c")))) {
            w.println("#include \"vm_interpreter.h\"");
            w.println("#include \"vm_data.h\"");
            w.println("#include \"chacha20.h\"");
            w.println("#include <stdio.h>");
            w.println("#include <stdlib.h>");
            w.println("#include <string.h>");
            w.println();
            
            // XOR key
            w.println("#define METHOD_ID_XOR_KEY 0x" + Integer.toHexString(methodIdXorKey));
            w.println();
            
            // Debug macros
            if (debug) {
                w.println("#define VM_LOG(fmt, ...) printf(\"[VM] \" fmt, ##__VA_ARGS__)");
                w.println("#define VM_DEBUG_LOG(fmt, ...) printf(\"[VM-DEBUG] \" fmt, ##__VA_ARGS__)");
            } else {
                w.println("#define VM_LOG(fmt, ...)");
                w.println("#define VM_DEBUG_LOG(fmt, ...)");
            }
            w.println();
            
            // === Branch prediction hint macros ===
            w.println("// === Branch prediction hints ===");
            w.println("#define LIKELY(x)   __builtin_expect(!!(x), 1)");
            w.println("#define UNLIKELY(x) __builtin_expect(!!(x), 0)");
            w.println();

            // === Class and method cache system ===
            emitCachingSystem(w);

            // Helper function implementations
            for (VMHelper helper : helpers.getAllHelpers()) {
                helper.generateSource(w);
            }

            // String decryption function
            w.println("static void vm_decrypt_string(const unsigned char* enc, int len, char* out, const unsigned char* key) {");
            w.println("    for (int i = 0; i < len; i++) {");
            w.println("        out[i] = (char)((enc[i] - (i & 0xFF)) ^ key[i % 8]);");
            w.println("    }");
            w.println("    out[len] = '\\0';");
            w.println("}");
            w.println();
            
            // Main interpreter function (with return type parameter)
            emitExecuteCommon(w);

            // Wrapper functions for each return type
            emitExecuteWrappers(w);
        }
    }
    
    private void emitCachingSystem(PrintWriter w) {
        // === Frame memory pool ===
        w.println("// === Frame memory pool (avoid calloc/free on each call) ===");
        w.println("#define FRAME_POOL_SIZE (4 * 1024 * 1024)  // 4MB pool");
        w.println("#define MAX_FRAME_DEPTH 256                 // max call nesting depth");
        w.println();
        w.println("typedef struct {");
        w.println("    uint8_t* base;                         // pool base address");
        w.println("    size_t offset;                         // current allocation offset");
        w.println("    size_t frameOffsets[MAX_FRAME_DEPTH];  // per-frame start offset stack");
        w.println("    int depth;                             // current frame depth");
        w.println("} FramePool;");
        w.println();
        w.println("static FramePool framePool;");
        w.println();
        w.println("// Initialize frame pool (called in JNI_OnLoad)");
        w.println("void frame_pool_init(void) {");
        w.println("    framePool.base = (uint8_t*)malloc(FRAME_POOL_SIZE);");
        w.println("    framePool.offset = 0;");
        w.println("    framePool.depth = 0;");
        w.println("}");
        w.println();
        w.println("// Enter method frame - save position and allocate (no zeroing, caller handles if needed)");
        w.println("static VMValue* frame_pool_push(int count) {");
        w.println("    size_t size = count * sizeof(VMValue);");
        w.println("    size = (size + 15) & ~(size_t)15;  // 16-byte alignment");
        w.println("    if (framePool.offset + size > FRAME_POOL_SIZE || framePool.depth >= MAX_FRAME_DEPTH) {");
        w.println("        return (VMValue*)calloc(count, sizeof(VMValue));  // fallback when pool full (calloc zeros)");
        w.println("    }");
        w.println("    framePool.frameOffsets[framePool.depth++] = framePool.offset;");
        w.println("    VMValue* ptr = (VMValue*)(framePool.base + framePool.offset);");
        w.println("    framePool.offset += size;");
        w.println("    return ptr;  // not zeroed, caller handles if needed");
        w.println("}");
        w.println();
        w.println("// Exit method frame - restore pointer");
        w.println("static void frame_pool_pop(VMValue* ptr) {");
        w.println("    if (ptr >= (VMValue*)framePool.base && ptr < (VMValue*)(framePool.base + FRAME_POOL_SIZE)) {");
        w.println("        framePool.depth--;");
        w.println("        framePool.offset = framePool.frameOffsets[framePool.depth];");
        w.println("    } else {");
        w.println("        free(ptr);  // allocated outside pool, free directly");
        w.println("    }");
        w.println("}");
        w.println();

        // === Hash cache system ===
        w.println("// === Hash cache system (O(1) lookup) ===");
        w.println("#define CLASS_CACHE_SIZE 256    // must be power of 2");
        w.println("#define METHOD_CACHE_SIZE 1024  // must be power of 2");
        w.println("#define FIELD_CACHE_SIZE 512    // must be power of 2");
        w.println();

        // Hash function
        w.println("static inline uint32_t ptr_hash(const void* p) {");
        w.println("    return (uint32_t)((uintptr_t)p >> 3);  // ignore low alignment bits");
        w.println("}");
        w.println();

        // Combined hash: three pointers
        w.println("static inline uint32_t triple_hash(const void* a, const void* b, const void* c) {");
        w.println("    return ptr_hash(a) ^ (ptr_hash(b) << 5) ^ (ptr_hash(c) << 11);");
        w.println("}");
        w.println();

        // Class cache entry
        w.println("typedef struct {");
        w.println("    const char* key;    // className");
        w.println("    jclass value;");
        w.println("} ClassCacheEntry;");
        w.println();

        // Method cache entry
        w.println("typedef struct {");
        w.println("    const char* owner;");
        w.println("    const char* name;");
        w.println("    const char* desc;");
        w.println("    jmethodID mid;");
        w.println("} MethodCacheEntry;");
        w.println();

        // Field cache entry
        w.println("typedef struct {");
        w.println("    const char* owner;");
        w.println("    const char* name;");
        w.println("    const char* desc;");
        w.println("    jfieldID fid;");
        w.println("} FieldCacheEntry;");
        w.println();

        // Static cache arrays
        w.println("static ClassCacheEntry classCache[CLASS_CACHE_SIZE];");
        w.println("static MethodCacheEntry methodCache[METHOD_CACHE_SIZE];");
        w.println("static FieldCacheEntry fieldCache[FIELD_CACHE_SIZE];");
        w.println();

        // Class lookup - O(1) hash
        w.println("static jclass vm_find_class(JNIEnv* env, const char* className) {");
        w.println("    uint32_t idx = ptr_hash(className) & (CLASS_CACHE_SIZE - 1);");
        w.println("    ClassCacheEntry* e = &classCache[idx];");
        w.println("    if (LIKELY(e->key == className && e->value != NULL)) {");
        w.println("        return e->value;  // cache hit");
        w.println("    }");
        w.println("    jclass localCls = (*env)->FindClass(env, className);");
        w.println("    if (localCls) {");
        w.println("        jclass globalCls = (*env)->NewGlobalRef(env, localCls);");
        w.println("        if (globalCls) {");
        w.println("            e->key = className;");
        w.println("            e->value = globalCls;");
        w.println("        }");
        w.println("    }");
        w.println("    return localCls;");
        w.println("}");
        w.println();

        // Method lookup - O(1) hash
        w.println("static jmethodID vm_get_method_id(JNIEnv* env, jclass cls, const char* owner, const char* name, const char* desc) {");
        w.println("    uint32_t idx = triple_hash(owner, name, desc) & (METHOD_CACHE_SIZE - 1);");
        w.println("    MethodCacheEntry* e = &methodCache[idx];");
        w.println("    if (LIKELY(e->owner == owner && e->name == name && e->desc == desc && e->mid != NULL)) {");
        w.println("        return e->mid;  // cache hit");
        w.println("    }");
        w.println("    jmethodID mid = (*env)->GetMethodID(env, cls, name, desc);");
        w.println("    if (mid) {");
        w.println("        e->owner = owner;");
        w.println("        e->name = name;");
        w.println("        e->desc = desc;");
        w.println("        e->mid = mid;");
        w.println("    }");
        w.println("    return mid;");
        w.println("}");
        w.println();

        w.println("static jmethodID vm_get_static_method_id(JNIEnv* env, jclass cls, const char* owner, const char* name, const char* desc) {");
        w.println("    uint32_t idx = triple_hash(owner, name, desc) & (METHOD_CACHE_SIZE - 1);");
        w.println("    MethodCacheEntry* e = &methodCache[idx];");
        w.println("    if (LIKELY(e->owner == owner && e->name == name && e->desc == desc && e->mid != NULL)) {");
        w.println("        return e->mid;  // cache hit");
        w.println("    }");
        w.println("    jmethodID mid = (*env)->GetStaticMethodID(env, cls, name, desc);");
        w.println("    if (mid) {");
        w.println("        e->owner = owner;");
        w.println("        e->name = name;");
        w.println("        e->desc = desc;");
        w.println("        e->mid = mid;");
        w.println("    }");
        w.println("    return mid;");
        w.println("}");
        w.println();

        // Field lookup - O(1) hash
        w.println("static jfieldID vm_get_field_id(JNIEnv* env, jclass cls, const char* owner, const char* name, const char* desc) {");
        w.println("    uint32_t idx = triple_hash(owner, name, desc) & (FIELD_CACHE_SIZE - 1);");
        w.println("    FieldCacheEntry* e = &fieldCache[idx];");
        w.println("    if (LIKELY(e->owner == owner && e->name == name && e->desc == desc && e->fid != NULL)) {");
        w.println("        return e->fid;  // cache hit");
        w.println("    }");
        w.println("    jfieldID fid = (*env)->GetFieldID(env, cls, name, desc);");
        w.println("    if (fid) {");
        w.println("        e->owner = owner;");
        w.println("        e->name = name;");
        w.println("        e->desc = desc;");
        w.println("        e->fid = fid;");
        w.println("    }");
        w.println("    return fid;");
        w.println("}");
        w.println();

        w.println("static jfieldID vm_get_static_field_id(JNIEnv* env, jclass cls, const char* owner, const char* name, const char* desc) {");
        w.println("    uint32_t idx = triple_hash(owner, name, desc) & (FIELD_CACHE_SIZE - 1);");
        w.println("    FieldCacheEntry* e = &fieldCache[idx];");
        w.println("    if (LIKELY(e->owner == owner && e->name == name && e->desc == desc && e->fid != NULL)) {");
        w.println("        return e->fid;  // cache hit");
        w.println("    }");
        w.println("    jfieldID fid = (*env)->GetStaticFieldID(env, cls, name, desc);");
        w.println("    if (fid) {");
        w.println("        e->owner = owner;");
        w.println("        e->name = name;");
        w.println("        e->desc = desc;");
        w.println("        e->fid = fid;");
        w.println("    }");
        w.println("    return fid;");
        w.println("}");
        w.println();
    }
    
    private void emitExecuteCommon(PrintWriter w) {
        w.println("// Execution result struct (for return value and type)");
        w.println("typedef struct {");
        w.println("    VMValue value;");
        w.println("    char returnType;  // 'V', 'I', 'J', 'F', 'D', 'L'");
        w.println("} ExecuteResult;");
        w.println();

        w.println("static ExecuteResult vm_execute_common(JNIEnv* env, int methodId, jobject instance, jobjectArray args, jclass callerClass) {");
        w.println("    ExecuteResult execResult = { .returnType = 'V' };");
        w.println("    methodId ^= METHOD_ID_XOR_KEY;");
        w.println("    if (methodId < 0 || methodId >= vm_method_count) {");
        w.println("        execResult.returnType = 'E';  // Error");
        w.println("        return execResult;");
        w.println("    }");
        w.println("    VMMethod* m = &vm_methods[methodId];");
        w.println();

        // Bytecode used directly (no longer encrypted)
        w.println("    uint8_t* bytecode = m->bytecode;");
        w.println();

        // Initialize frame (using memory pool)
        w.println("    VMFrame frame = { .pc = 0, .sp = 0, .callerClass = callerClass };");
        w.println("    frame.stack = frame_pool_push(m->maxStack);  // stack doesn't need zeroing, will be overwritten");
        w.println("    frame.locals = frame_pool_push(m->maxLocals);");
        w.println("    memset(frame.locals, 0, m->maxLocals * sizeof(VMValue));  // locals must be zeroed");
        w.println();

        // Set up arguments
        w.println("    frame.locals[0].l = instance;");
        w.println("    const char* methodDesc = (m->descIdx >= 0) ? vm_get_string(m->descIdx) : NULL;");
        w.println("    const char* argTypes = (m->argTypesIdx >= 0) ? vm_get_string(m->argTypesIdx) : NULL;");
        w.println("    vm_unbox_args_fast(env, &frame, args, argTypes, m->argCount, instance ? 1 : 0);");
        w.println();

        // Lookup table for whether instruction needs metadata (0=no, 1=yes)
        w.println("    // Metadata requirement table: marks which instructions need metadata lookup");
        w.println("    static const uint8_t needs_meta[256] = {");
        StringBuilder metaTable = new StringBuilder();
        for (int i = 0; i < 256; i++) {
            final int op = i;
            Instruction inst = instructions.getAllInstructions().stream()
                .filter(ins -> ins.getOpcode() == op)
                .findFirst().orElse(null);
            boolean needsMeta = inst != null && inst.needsMeta();
            if (i % 32 == 0) metaTable.append("        ");
            metaTable.append(needsMeta ? "1" : "0");
            metaTable.append(i < 255 ? "," : "");
            if ((i + 1) % 32 == 0) {
                w.println(metaTable.toString());
                metaTable = new StringBuilder();
            }
        }
        if (metaTable.length() > 0) {
            w.println(metaTable.toString());
        }
        w.println("    };");
        w.println();
        
        // Computed Goto dispatch table
        w.println("    static const void* dispatch_table[256] = {");
        for (int i = 0; i < 256; i++) {
            final int op = i;
            Instruction inst = instructions.getAllInstructions().stream()
                .filter(ins -> ins.getOpcode() == op)
                .findFirst().orElse(null);
            if (inst != null) {
                w.printf("        &&OP_%02x,  // 0x%02x %s%n", i, i, inst.getName());
            } else {
                w.printf("        &&OP_DEFAULT,  // 0x%02x%n", i);
            }
        }
        w.println("    };");
        w.println();

        // Optimized DISPATCH_NEXT: decode obfuscated opcode, then dispatch
        w.println("    #define DISPATCH_NEXT \\");
        w.println("        do { \\");
        w.println("            if (UNLIKELY(frame.pc >= m->bytecodeLen)) goto method_exit; \\");
        w.println("            uint8_t _obfuscated = bytecode[frame.pc]; \\");
        w.println("            uint8_t _op = DECODE_OPCODE(_obfuscated); \\");
        w.println("            if (needs_meta[_op]) { \\");
        w.println("                int _metaIdx = m->pcToMetaIdx[frame.pc]; \\");
        w.println("                meta = (_metaIdx >= 0) ? &m->metadata[_metaIdx] : NULL; \\");
        w.println("            } else { \\");
        w.println("                meta = NULL; \\");
        w.println("            } \\");
        w.println("            goto *dispatch_table[_op]; \\");
        w.println("        } while(0)");
        w.println();

        w.println("    MetaEntry* meta = NULL;");
        w.println("    DISPATCH_NEXT;");
        w.println();

        // Generate all instruction handling code
        for (Instruction inst : instructions.getAllInstructions()) {
            inst.generateComputedGoto(w);
            w.println();
        }

        
        w.println("        OP_DEFAULT:");
        w.println("            VM_LOG(\"Unknown opcode: 0x%02x at pc=%d\\n\", bytecode[frame.pc], frame.pc);");
        w.println("            frame.pc++;");
        w.println("            DISPATCH_NEXT;");
        w.println();

        // Return result on exit
        w.println("    method_exit:");
        w.println("    ;");
        w.println("    // Get return type from method descriptor");
        w.println("    if (methodDesc) {");
        w.println("        const char* p = methodDesc;");
        w.println("        while (*p && *p != ')') p++;");
        w.println("        if (*p == ')') execResult.returnType = *(p + 1);");
        w.println("    }");
        w.println("    // Get return value from top of stack");
        w.println("    if (frame.sp > 0) {");
        w.println("        execResult.value = frame.stack[frame.sp - 1];");
        w.println("    }");
        w.println("    frame_pool_pop(frame.locals);");
        w.println("    frame_pool_pop(frame.stack);");
        w.println("    return execResult;");
        w.println("}");
        w.println();
    }

    private void emitExecuteWrappers(PrintWriter w) {
        // void
        w.println("void vm_execute_method_void(JNIEnv* env, int methodId, jobject instance, jobjectArray args, jclass callerClass) {");
        w.println("    ExecuteResult r = vm_execute_common(env, methodId, instance, args, callerClass);");
        w.println("    (void)r;  // ignore return value");
        w.println("}");
        w.println();
        
        // int
        w.println("jint vm_execute_method_int(JNIEnv* env, int methodId, jobject instance, jobjectArray args, jclass callerClass) {");
        w.println("    ExecuteResult r = vm_execute_common(env, methodId, instance, args, callerClass);");
        w.println("    return r.value.i;");
        w.println("}");
        w.println();
        
        // long
        w.println("jlong vm_execute_method_long(JNIEnv* env, int methodId, jobject instance, jobjectArray args, jclass callerClass) {");
        w.println("    ExecuteResult r = vm_execute_common(env, methodId, instance, args, callerClass);");
        w.println("    return r.value.j;");
        w.println("}");
        w.println();
        
        // float
        w.println("jfloat vm_execute_method_float(JNIEnv* env, int methodId, jobject instance, jobjectArray args, jclass callerClass) {");
        w.println("    ExecuteResult r = vm_execute_common(env, methodId, instance, args, callerClass);");
        w.println("    return r.value.f;");
        w.println("}");
        w.println();
        
        // double
        w.println("jdouble vm_execute_method_double(JNIEnv* env, int methodId, jobject instance, jobjectArray args, jclass callerClass) {");
        w.println("    ExecuteResult r = vm_execute_common(env, methodId, instance, args, callerClass);");
        w.println("    return r.value.d;");
        w.println("}");
        w.println();
        
        // object
        w.println("jobject vm_execute_method_object(JNIEnv* env, int methodId, jobject instance, jobjectArray args, jclass callerClass) {");
        w.println("    ExecuteResult r = vm_execute_common(env, methodId, instance, args, callerClass);");
        w.println("    return r.value.l;");
        w.println("}");
    }
}