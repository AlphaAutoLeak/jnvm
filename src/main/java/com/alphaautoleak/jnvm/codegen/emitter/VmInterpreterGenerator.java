package com.alphaautoleak.jnvm.codegen.emitter;

import com.alphaautoleak.jnvm.codegen.emitter.helper.VMHelper;
import com.alphaautoleak.jnvm.codegen.emitter.helper.VMHelpers;
import com.alphaautoleak.jnvm.crypto.OpcodeObfuscator;

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
    private final OpcodeObfuscator opcodeObfuscator;
    
    public VmInterpreterGenerator(File dir, boolean debug, boolean encryptStrings, 
                                  int methodIdXorKey, OpcodeObfuscator opcodeObfuscator) {
        this.dir = dir;
        this.debug = debug;
        this.encryptStrings = encryptStrings;
        this.instructions = new Instructions();
        this.helpers = new VMHelpers(encryptStrings);
        this.methodIdXorKey = methodIdXorKey;
        this.opcodeObfuscator = opcodeObfuscator;
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

            // VM method lookup initialization
            w.println("// VM method lookup initialization (called in JNI_OnLoad)");
            w.println("void vm_init_method_lookup(void);");
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
            w.println("#define _POSIX_C_SOURCE 200809L");
            w.println("#include \"vm_interpreter.h\"");
            w.println("#include \"vm_data.h\"");
            w.println("#include \"chacha20.h\"");
            w.println("#include <stdio.h>");
            w.println("#include <stdlib.h>");
            w.println("#include <string.h>");
            w.println("#include <stdatomic.h>  // for atomic cache operations");
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
        // === Frame memory pool (thread-local bump allocator) ===
        w.println("// === Frame memory pool (thread-local bump allocator) ===");
        w.println("#define FRAME_POOL_SIZE (4 * 1024 * 1024)  // 4MB per thread");
        w.println();
        w.println("static __thread VMValue* _frameBase;");
        w.println("static __thread int _frameOffset;  // offset in VMValue units");
        w.println();
        w.println("void frame_pool_init(void) {");
        w.println("    _frameBase = (VMValue*)malloc(FRAME_POOL_SIZE);");
        w.println("    _frameOffset = 0;");
        w.println("}");
        w.println();
        w.println("__attribute__((cold))");
        w.println("static inline void frame_pool_ensure_init(void) {");
        w.println("    if (UNLIKELY(_frameBase == NULL)) {");
        w.println("        _frameBase = (VMValue*)malloc(FRAME_POOL_SIZE);");
        w.println("        _frameOffset = 0;");
        w.println("    }");
        w.println("}");
        w.println();
        w.println("static inline VMValue* frame_pool_push(int count) {");
        w.println("    VMValue* ptr = _frameBase + _frameOffset;");
        w.println("    _frameOffset += count;");
        w.println("    return ptr;");
        w.println("}");
        w.println();
        w.println("static inline void frame_pool_pop(int count) {");
        w.println("    _frameOffset -= count;");
        w.println("}");
        w.println();

        // === Hash cache system ===
        w.println("// === Hash cache system (O(1) lookup) ===");
        w.println("#define CLASS_CACHE_SIZE 256    // must be power of 2");
        w.println("#define METHOD_CACHE_SIZE 1024  // must be power of 2");
        w.println("#define FIELD_CACHE_SIZE 512    // must be power of 2");
        w.println();

        // Hash function
        w.println("__attribute__((const))");
        w.println("static inline uint32_t ptr_hash(const void* p) {");
        w.println("    return (uint32_t)((uintptr_t)p >> 3);  // ignore low alignment bits");
        w.println("}");
        w.println();

        // Combined hash: three pointers
        w.println("__attribute__((const))");
        w.println("static inline uint32_t triple_hash(const void* a, const void* b, const void* c) {");
        w.println("    return ptr_hash(a) ^ (ptr_hash(b) << 5) ^ (ptr_hash(c) << 11);");
        w.println("}");
        w.println();

        // Class cache entry
        w.println("typedef struct {");
        w.println("    const char* key;    // className");
        w.println("    _Atomic jclass value;  // atomic for thread-safe caching");
        w.println("} ClassCacheEntry;");
        w.println();

        // Method cache entry
        w.println("typedef struct {");
        w.println("    const char* owner;");
        w.println("    const char* name;");
        w.println("    const char* desc;");
        w.println("    _Atomic jmethodID mid;  // atomic for thread-safe caching");
        w.println("} MethodCacheEntry;");
        w.println();

        // Field cache entry
        w.println("typedef struct {");
        w.println("    const char* owner;");
        w.println("    const char* name;");
        w.println("    const char* desc;");
        w.println("    _Atomic jfieldID fid;  // atomic for thread-safe caching");
        w.println("} FieldCacheEntry;");
        w.println();

        // Static cache arrays
        w.println("static ClassCacheEntry classCache[CLASS_CACHE_SIZE];");
        w.println("static MethodCacheEntry methodCache[METHOD_CACHE_SIZE];");
        w.println("static FieldCacheEntry fieldCache[FIELD_CACHE_SIZE];");
        w.println();

        // Class lookup - O(1) hash with atomic cache and CAS for race condition handling
        w.println("__attribute__((const, hot))");
        w.println("static jclass vm_find_class(JNIEnv* env, const char* className) {");
        w.println("    uint32_t idx = ptr_hash(className) & (CLASS_CACHE_SIZE - 1);");
        w.println("    ClassCacheEntry* e = &classCache[idx];");
        w.println("    // Atomic load for thread-safe cache read");
        w.println("    jclass cached = atomic_load_explicit(&e->value, memory_order_relaxed);");
        w.println("    if (LIKELY(e->key == className && cached != NULL)) {");
        w.println("        return cached;  // cache hit");
        w.println("    }");
        w.println("    // Cache miss - find and create global ref");
        w.println("    jclass localCls = (*env)->FindClass(env, className);");
        w.println("    if (!localCls) return NULL;");
        w.println("    jclass globalCls = (*env)->NewGlobalRef(env, localCls);");
        w.println("    if (!globalCls) return localCls;");
        w.println("    // Check if key matches (different className may hash to same slot)");
        w.println("    if (e->key != className && e->key != NULL) {");
        w.println("        return globalCls;  // hash collision, don't cache");
        w.println("    }");
        w.println("    // Use CAS to handle race condition");
        w.println("    if (atomic_compare_exchange_strong_explicit(&e->value, &cached, globalCls, memory_order_relaxed, memory_order_relaxed)) {");
        w.println("        e->key = className;  // we won the race");
        w.println("        return globalCls;");
        w.println("    }");
        w.println("    // Another thread won - delete our global ref and use theirs");
        w.println("    (*env)->DeleteGlobalRef(env, globalCls);");
        w.println("    return cached;");
        w.println("}");
        w.println();

        // Method lookup - O(1) hash with atomic cache
        w.println("__attribute__((hot))");
        w.println("static jmethodID vm_get_method_id(JNIEnv* env, jclass cls, const char* owner, const char* name, const char* desc) {");
        w.println("    uint32_t idx = triple_hash(owner, name, desc) & (METHOD_CACHE_SIZE - 1);");
        w.println("    MethodCacheEntry* e = &methodCache[idx];");
        w.println("    // Atomic load for thread-safe cache read");
        w.println("    jmethodID cached = atomic_load_explicit(&e->mid, memory_order_relaxed);");
        w.println("    if (LIKELY(e->owner == owner && e->name == name && e->desc == desc && cached != NULL)) {");
        w.println("        return cached;  // cache hit");
        w.println("    }");
        w.println("    jmethodID mid = (*env)->GetMethodID(env, cls, name, desc);");
        w.println("    if (mid) {");
        w.println("        e->owner = owner;");
        w.println("        e->name = name;");
        w.println("        e->desc = desc;");
        w.println("        atomic_store_explicit(&e->mid, mid, memory_order_relaxed);");
        w.println("    }");
        w.println("    return mid;");
        w.println("}");
        w.println();

        w.println("__attribute__((hot))");
        w.println("static jmethodID vm_get_static_method_id(JNIEnv* env, jclass cls, const char* owner, const char* name, const char* desc) {");
        w.println("    uint32_t idx = triple_hash(owner, name, desc) & (METHOD_CACHE_SIZE - 1);");
        w.println("    MethodCacheEntry* e = &methodCache[idx];");
        w.println("    // Atomic load for thread-safe cache read");
        w.println("    jmethodID cached = atomic_load_explicit(&e->mid, memory_order_relaxed);");
        w.println("    if (LIKELY(e->owner == owner && e->name == name && e->desc == desc && cached != NULL)) {");
        w.println("        return cached;  // cache hit");
        w.println("    }");
        w.println("    jmethodID mid = (*env)->GetStaticMethodID(env, cls, name, desc);");
        w.println("    if (mid) {");
        w.println("        e->owner = owner;");
        w.println("        e->name = name;");
        w.println("        e->desc = desc;");
        w.println("        atomic_store_explicit(&e->mid, mid, memory_order_relaxed);");
        w.println("    }");
        w.println("    return mid;");
        w.println("}");
        w.println();

        // Field lookup - O(1) hash with atomic cache
        w.println("__attribute__((hot))");
        w.println("static jfieldID vm_get_field_id(JNIEnv* env, jclass cls, const char* owner, const char* name, const char* desc) {");
        w.println("    uint32_t idx = triple_hash(owner, name, desc) & (FIELD_CACHE_SIZE - 1);");
        w.println("    FieldCacheEntry* e = &fieldCache[idx];");
        w.println("    // Atomic load for thread-safe cache read");
        w.println("    jfieldID cached = atomic_load_explicit(&e->fid, memory_order_relaxed);");
        w.println("    if (LIKELY(e->owner == owner && e->name == name && e->desc == desc && cached != NULL)) {");
        w.println("        return cached;  // cache hit");
        w.println("    }");
        w.println("    jfieldID fid = (*env)->GetFieldID(env, cls, name, desc);");
        w.println("    if (fid) {");
        w.println("        e->owner = owner;");
        w.println("        e->name = name;");
        w.println("        e->desc = desc;");
        w.println("        atomic_store_explicit(&e->fid, fid, memory_order_relaxed);");
        w.println("    }");
        w.println("    return fid;");
        w.println("}");
        w.println();

        w.println("__attribute__((hot))");
        w.println("static jfieldID vm_get_static_field_id(JNIEnv* env, jclass cls, const char* owner, const char* name, const char* desc) {");
        w.println("    uint32_t idx = triple_hash(owner, name, desc) & (FIELD_CACHE_SIZE - 1);");
        w.println("    FieldCacheEntry* e = &fieldCache[idx];");
        w.println("    // Atomic load for thread-safe cache read");
        w.println("    jfieldID cached = atomic_load_explicit(&e->fid, memory_order_relaxed);");
        w.println("    if (LIKELY(e->owner == owner && e->name == name && e->desc == desc && cached != NULL)) {");
        w.println("        return cached;  // cache hit");
        w.println("    }");
        w.println("    jfieldID fid = (*env)->GetStaticFieldID(env, cls, name, desc);");
        w.println("    if (fid) {");
        w.println("        e->owner = owner;");
        w.println("        e->name = name;");
        w.println("        e->desc = desc;");
        w.println("        atomic_store_explicit(&e->fid, fid, memory_order_relaxed);");
        w.println("    }");
        w.println("    return fid;");
        w.println("}");
        w.println();

        // === VM method lookup for direct calls ===
        w.println("// === VM method lookup (direct VM-to-VM calls) ===");
        w.println("#define VM_METHOD_LOOKUP_SIZE 256");
        w.println();
        w.println("typedef struct {");
        w.println("    const char* owner;");
        w.println("    const char* name;");
        w.println("    const char* desc;");
        w.println("    int methodId;");
        w.println("} VMMethodLookupEntry;");
        w.println();
        w.println("static VMMethodLookupEntry vmMethodLookup[VM_METHOD_LOOKUP_SIZE];");
        w.println();
        w.println("__attribute__((const, always_inline))");
        w.println("static inline int vm_lookup_method(const char* owner, const char* name, const char* desc) {");
        w.println("    uint32_t hash = triple_hash(owner, name, desc);");
        w.println("    for (int probe = 0; probe < 8; probe++) {");
        w.println("        uint32_t idx = (hash + probe) & (VM_METHOD_LOOKUP_SIZE - 1);");
        w.println("        VMMethodLookupEntry* e = &vmMethodLookup[idx];");
        w.println("        if (e->owner == NULL) return -1;");
        w.println("        if (e->owner == owner && e->name == name && e->desc == desc) return e->methodId;");
        w.println("    }");
        w.println("    return -1;");
        w.println("}");
        w.println();
        w.println("__attribute__((cold))");
        w.println("void vm_init_method_lookup(void) {");
        w.println("    memset(vmMethodLookup, 0, sizeof(vmMethodLookup));");
        w.println("    for (int i = 0; i < vm_method_count; i++) {");
        w.println("        VMMethod* m = &vm_methods[i];");
        w.println("        if (m->ownerIdx < 0 || m->nameIdx < 0 || m->descIdx < 0) continue;");
        w.println("        const char* o = vm_get_string(m->ownerIdx);");
        w.println("        const char* n = vm_get_string(m->nameIdx);");
        w.println("        const char* d = vm_get_string(m->descIdx);");
        w.println("        uint32_t hash = triple_hash(o, n, d);");
        w.println("        for (int probe = 0; probe < 8; probe++) {");
        w.println("            uint32_t idx = (hash + probe) & (VM_METHOD_LOOKUP_SIZE - 1);");
        w.println("            if (vmMethodLookup[idx].owner == NULL) {");
        w.println("                vmMethodLookup[idx].owner = o;");
        w.println("                vmMethodLookup[idx].name = n;");
        w.println("                vmMethodLookup[idx].desc = d;");
        w.println("                vmMethodLookup[idx].methodId = i;");
        w.println("                break;");
        w.println("            }");
        w.println("        }");
        w.println("    }");
        w.println("    // Pre-cache vmTargetId in all MetaEntry structs for invoke instructions");
        w.println("    for (int i = 0; i < vm_method_count; i++) {");
        w.println("        VMMethod* m = &vm_methods[i];");
        w.println("        for (int j = 0; j < m->metadataCount; j++) {");
        w.println("            MetaEntry* me = &m->metadata[j];");
        w.println("            if (me->type == META_METHOD && me->ownerIdx >= 0 && me->nameIdx >= 0 && me->descIdx >= 0) {");
        w.println("                me->vmTargetId = vm_lookup_method(vm_get_string(me->ownerIdx), vm_get_string(me->nameIdx), vm_get_string(me->descIdx));");
        w.println("            } else {");
        w.println("                me->vmTargetId = -1;");
        w.println("            }");
        w.println("        }");
        w.println("    }");
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

        w.println("__attribute__((hot))");
        w.println("static ExecuteResult vm_execute_common(JNIEnv* env, int methodId, jobject instance, jobjectArray args, jclass callerClass, VMValue* directLocals, int directLocalSlots) {");
        w.println("    frame_pool_ensure_init();");
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

        // Initialize frame
        w.println("    VMFrame frame = { .pc = 0, .sp = 0, .callerClass = callerClass };");
        w.println("    frame.stack = frame_pool_push(m->maxStack);");
        w.println();

        // Set up arguments - direct path (zero copy) or unboxing path
        w.println("    const char* methodDesc = (m->descIdx >= 0) ? vm_get_string(m->descIdx) : NULL;");
        w.println("    if (directLocals) {");
        w.println("        frame.locals = directLocals;  // reuse caller's buffer directly (zero copy)");
        w.println("    } else {");
        w.println("        frame.locals = frame_pool_push(m->maxLocals);");
        w.println("        memset(frame.locals, 0, m->maxLocals * sizeof(VMValue));");
        w.println("        frame.locals[0].l = instance;");
        w.println("        const char* argTypes = (m->argTypesIdx >= 0) ? vm_get_string(m->argTypesIdx) : NULL;");
        w.println("        vm_unbox_args_fast(env, &frame, args, argTypes, m->argCount, instance ? 1 : 0);");
        w.println("    }");
        w.println();

        // Lookup table for whether instruction needs metadata (indexed by OBFUSCATED opcode)
        w.println("    // Metadata requirement table (indexed by obfuscated opcode)");
        w.println("    static const uint8_t needs_meta[256] = {");
        StringBuilder metaTable = new StringBuilder();
        for (int i = 0; i < 256; i++) {
            // i is the OBFUSCATED opcode, decode to get original
            int originalOp = opcodeObfuscator.decode(i);
            Instruction inst = instructions.getAllInstructions().stream()
                .filter(ins -> ins.getOpcode() == originalOp)
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
        
        // Computed Goto dispatch table (indexed by OBFUSCATED opcode)
        w.println("    // Dispatch table (indexed by obfuscated opcode)");
        w.println("    static const void* dispatch_table[256] = {");
        for (int i = 0; i < 256; i++) {
            // i is the OBFUSCATED opcode, decode to get original
            int originalOp = opcodeObfuscator.decode(i);
            Instruction inst = instructions.getAllInstructions().stream()
                .filter(ins -> ins.getOpcode() == originalOp)
                .findFirst().orElse(null);
            if (inst != null) {
                // Label uses original opcode, but table is indexed by obfuscated opcode
                w.printf("        &&OP_%02x,%n", originalOp);
            } else {
                w.printf("        &&OP_DEFAULT,%n");
            }
        }
        w.println("    };");
        w.println();

        // DISPATCH_NEXT: directly use obfuscated opcode as index (NO decoding!)
        // Unconditional meta lookup (branchless, pcToMetaIdx returns -1 for non-meta instructions)
        w.println("    #define DISPATCH_NEXT \\");
        w.println("        do { \\");
        w.println("            uint8_t _op = bytecode[frame.pc]; \\");
        w.println("            int _metaIdx = m->pcToMetaIdx[frame.pc]; \\");
        w.println("            meta = (_metaIdx >= 0) ? &m->metadata[_metaIdx] : NULL; \\");
        w.println("            goto *dispatch_table[_op]; \\");
        w.println("        } while(0)");
        w.println();

        w.println("    int _hasException = 0;  // set to 1 when unhandled exception causes exit");
        w.println("    MetaEntry* meta = NULL;");
        w.println("    DISPATCH_NEXT;");
        w.println();

        // Generate instruction handling code in RANDOM order (obfuscated)
        // The order is determined by the obfuscator's shuffle
        for (int i = 0; i < 256; i++) {
            // Get original opcode at this position in the shuffled order
            int originalOp = opcodeObfuscator.decode(i);
            Instruction inst = instructions.getAllInstructions().stream()
                .filter(ins -> ins.getOpcode() == originalOp)
                .findFirst().orElse(null);
            if (inst != null) {
                inst.generateComputedGoto(w);
                w.println();
            }
        }

        
        w.println("        OP_DEFAULT:");
        w.println("            VM_LOG(\"Unknown opcode: 0x%02x at pc=%d\\n\", bytecode[frame.pc], frame.pc);");
        w.println("            frame.pc++;");
        w.println("            DISPATCH_NEXT;");
        w.println();

        // Return result on exit
        w.println("    method_exit:");
        w.println("    ;");
        w.println("    if (UNLIKELY(_hasException)) {");
        w.println("        execResult.returnType = 'X';  // signal unhandled exception");
        w.println("    } else {");
        w.println("        // Get return type from method descriptor");
        w.println("        if (methodDesc) {");
        w.println("            const char* p = methodDesc;");
        w.println("            while (*p && *p != ')') p++;");
        w.println("            if (*p == ')') execResult.returnType = *(p + 1);");
        w.println("        }");
        w.println("        // Get return value from top of stack");
        w.println("        if (frame.sp > 0) {");
        w.println("            execResult.value = frame.stack[frame.sp - 1];");
        w.println("        }");
        w.println("    }");
        w.println("    if (!directLocals) frame_pool_pop(m->maxLocals);");
        w.println("    frame_pool_pop(m->maxStack);");
        w.println("    return execResult;");
        w.println("}");
        w.println();
    }

    private void emitExecuteWrappers(PrintWriter w) {
        // void
        w.println("__attribute__((hot))");
        w.println("void vm_execute_method_void(JNIEnv* env, int methodId, jobject instance, jobjectArray args, jclass callerClass) {");
        w.println("    ExecuteResult r = vm_execute_common(env, methodId, instance, args, callerClass, NULL, 0);");
        w.println("    (void)r;  // ignore return value");
        w.println("}");
        w.println();

        // int
        w.println("__attribute__((hot))");
        w.println("jint vm_execute_method_int(JNIEnv* env, int methodId, jobject instance, jobjectArray args, jclass callerClass) {");
        w.println("    ExecuteResult r = vm_execute_common(env, methodId, instance, args, callerClass, NULL, 0);");
        w.println("    return r.value.i;");
        w.println("}");
        w.println();

        // long
        w.println("__attribute__((hot))");
        w.println("jlong vm_execute_method_long(JNIEnv* env, int methodId, jobject instance, jobjectArray args, jclass callerClass) {");
        w.println("    ExecuteResult r = vm_execute_common(env, methodId, instance, args, callerClass, NULL, 0);");
        w.println("    return r.value.j;");
        w.println("}");
        w.println();

        // float
        w.println("__attribute__((hot))");
        w.println("jfloat vm_execute_method_float(JNIEnv* env, int methodId, jobject instance, jobjectArray args, jclass callerClass) {");
        w.println("    ExecuteResult r = vm_execute_common(env, methodId, instance, args, callerClass, NULL, 0);");
        w.println("    return r.value.f;");
        w.println("}");
        w.println();

        // double
        w.println("__attribute__((hot))");
        w.println("jdouble vm_execute_method_double(JNIEnv* env, int methodId, jobject instance, jobjectArray args, jclass callerClass) {");
        w.println("    ExecuteResult r = vm_execute_common(env, methodId, instance, args, callerClass, NULL, 0);");
        w.println("    return r.value.d;");
        w.println("}");
        w.println();

        // object
        w.println("__attribute__((hot))");
        w.println("jobject vm_execute_method_object(JNIEnv* env, int methodId, jobject instance, jobjectArray args, jclass callerClass) {");
        w.println("    ExecuteResult r = vm_execute_common(env, methodId, instance, args, callerClass, NULL, 0);");
        w.println("    return r.value.l;");
        w.println("}");
    }
}