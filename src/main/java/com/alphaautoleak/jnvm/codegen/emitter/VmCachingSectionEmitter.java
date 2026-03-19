package com.alphaautoleak.jnvm.codegen.emitter;

import java.io.PrintWriter;

/**
 * Emits C source sections related to frame/temp buffers, JNI lookup caches,
 * and VM direct-call lookup table for vm_interpreter.c.
 */
class VmCachingSectionEmitter {

    void emit(PrintWriter w) {
        emitFramePoolAndTempBuffer(w);
        emitJniHashCaches(w);
        emitVmMethodLookupCache(w);
    }

    private void emitFramePoolAndTempBuffer(PrintWriter w) {
        w.println("// === Frame memory pool (thread-local bump allocator) ===");
        w.println("#define FRAME_POOL_SIZE (4 * 1024 * 1024)  // 4MB per thread");
        w.println("#define TMP_BUF_MAX 4096  // max temp buffer size per allocation");
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

        w.println("// === Temporary string buffer (byte-level, 8-byte aligned) ===");
        w.println("// For safe string operations with dynamic length");
        w.println("static inline char* tmp_buf_alloc(int bytes) {");
        w.println("    // Round up to 8-byte alignment (sizeof(VMValue))");
        w.println("    int aligned = (bytes + 7) / 8;");
        w.println("    VMValue* ptr = _frameBase + _frameOffset;");
        w.println("    _frameOffset += aligned;");
        w.println("    return (char*)ptr;");
        w.println("}");
        w.println();
        w.println("static inline void tmp_buf_free(int bytes) {");
        w.println("    int aligned = (bytes + 7) / 8;");
        w.println("    _frameOffset -= aligned;");
        w.println("}");
        w.println();

        w.println("// Safe string copy with overflow protection");
        w.println("#define TMP_STRCPY(dst, dst_size, src) do { \\");
        w.println("    size_t _len = strlen(src); \\");
        w.println("    if (_len >= (size_t)(dst_size)) { \\");
        w.println("        _len = (dst_size) - 1; \\");
        w.println("    } \\");
        w.println("    memcpy((dst), (src), _len); \\");
        w.println("    (dst)[_len] = '\\0'; \\");
        w.println("} while(0)");
        w.println();
        w.println("#define TMP_STRNCPY(dst, src, max_len, dst_size) do { \\");
        w.println("    size_t _len = (max_len) < (size_t)(dst_size) ? (max_len) : (size_t)(dst_size) - 1; \\");
        w.println("    memcpy((dst), (src), _len); \\");
        w.println("    (dst)[_len] = '\\0'; \\");
        w.println("} while(0)");
        w.println();

        w.println("// === Function-level temp buffer save/restore ===");
        w.println("// Usage: TMP_SAVE at function start, TMP_RESTORE before every return");
        w.println("#define TMP_SAVE int _savedFrameOffset = _frameOffset");
        w.println("#define TMP_RESTORE _frameOffset = _savedFrameOffset");
        w.println();
    }

    private void emitJniHashCaches(PrintWriter w) {
        emitHashCachePreamble(w);
        emitHashFunctions(w);
        emitCacheEntryStructs(w);
        emitCacheArrays(w);
        emitClassLookupFunction(w);
        emitMethodLookupFunctions(w);
        emitFieldLookupFunctions(w);
    }

    private void emitHashCachePreamble(PrintWriter w) {
        w.println("// === Hash cache system (O(1) lookup) ===");
        w.println("#define CLASS_CACHE_SIZE 256    // must be power of 2");
        w.println("#define METHOD_CACHE_SIZE 1024  // must be power of 2");
        w.println("#define FIELD_CACHE_SIZE 512    // must be power of 2");
        w.println();
    }

    private void emitHashFunctions(PrintWriter w) {
        w.println("__attribute__((const))");
        w.println("static inline uint32_t ptr_hash(const void* p) {");
        w.println("    return (uint32_t)((uintptr_t)p >> 3);  // ignore low alignment bits");
        w.println("}");
        w.println();

        w.println("__attribute__((const))");
        w.println("static inline uint32_t triple_hash(const void* a, const void* b, const void* c) {");
        w.println("    return ptr_hash(a) ^ (ptr_hash(b) << 5) ^ (ptr_hash(c) << 11);");
        w.println("}");
        w.println();
    }

    private void emitCacheEntryStructs(PrintWriter w) {
        w.println("typedef struct {");
        w.println("    const char* key;    // className");
        w.println("    _Atomic jclass value;  // atomic for thread-safe caching");
        w.println("} ClassCacheEntry;");
        w.println();

        w.println("typedef struct {");
        w.println("    const char* owner;");
        w.println("    const char* name;");
        w.println("    const char* desc;");
        w.println("    _Atomic jmethodID mid;  // atomic for thread-safe caching");
        w.println("} MethodCacheEntry;");
        w.println();

        w.println("typedef struct {");
        w.println("    const char* owner;");
        w.println("    const char* name;");
        w.println("    const char* desc;");
        w.println("    _Atomic jfieldID fid;  // atomic for thread-safe caching");
        w.println("} FieldCacheEntry;");
        w.println();
    }

    private void emitCacheArrays(PrintWriter w) {
        w.println("static ClassCacheEntry classCache[CLASS_CACHE_SIZE];");
        w.println("static MethodCacheEntry methodCache[METHOD_CACHE_SIZE];");
        w.println("static FieldCacheEntry fieldCache[FIELD_CACHE_SIZE];");
        w.println();
    }

    private void emitClassLookupFunction(PrintWriter w) {
        w.println("__attribute__((const, hot))");
        w.println("static jclass vm_find_class(JNIEnv* env, const char* className) {");
        w.println("    uint32_t hash = ptr_hash(className);");
        w.println("    for (int probe = 0; probe < 8; probe++) {");
            w.println("        uint32_t idx = (hash + probe) & (CLASS_CACHE_SIZE - 1);");
            w.println("        ClassCacheEntry* e = &classCache[idx];");
        w.println("        jclass cached = atomic_load_explicit(&e->value, memory_order_relaxed);");
        w.println("        if (LIKELY(e->key == className && cached != NULL)) {");
        w.println("            return cached;");
        w.println("        }");
        w.println("        if (e->key == NULL) {");
        w.println("            jclass localCls = (*env)->FindClass(env, className);");
        w.println("            if (!localCls) return NULL;");
        w.println("            jclass globalCls = (*env)->NewGlobalRef(env, localCls);");
        w.println("            if (!globalCls) return NULL;  // never return local refs");
        w.println("            jclass expected = NULL;");
        w.println("            if (atomic_compare_exchange_strong_explicit(&e->value, &expected, globalCls, memory_order_relaxed, memory_order_relaxed)) {");
        w.println("                e->key = className;");
        w.println("                return globalCls;");
        w.println("            }");
        w.println("            (*env)->DeleteGlobalRef(env, globalCls);");
        w.println("            cached = atomic_load_explicit(&e->value, memory_order_relaxed);");
        w.println("            if (e->key == className && cached != NULL) {");
        w.println("                return cached;");
        w.println("            }");
        w.println("        }");
        w.println("    }");
        w.println("    // Fallback when probe budget is exhausted: no cache insert, but still return global ref");
        w.println("    jclass localCls = (*env)->FindClass(env, className);");
        w.println("    if (!localCls) return NULL;");
        w.println("    return (*env)->NewGlobalRef(env, localCls);");
        w.println("}");
        w.println();
    }

    private void emitMethodLookupFunctions(PrintWriter w) {
        w.println("__attribute__((hot))");
        w.println("static jmethodID vm_get_method_id(JNIEnv* env, jclass cls, const char* owner, const char* name, const char* desc) {");
        w.println("    uint32_t idx = triple_hash(owner, name, desc) & (METHOD_CACHE_SIZE - 1);");
        w.println("    MethodCacheEntry* e = &methodCache[idx];");
        w.println("    jmethodID cached = atomic_load_explicit(&e->mid, memory_order_relaxed);");
        w.println("    if (LIKELY(e->owner == owner && e->name == name && e->desc == desc && cached != NULL)) {");
        w.println("        return cached;");
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
        w.println("    jmethodID cached = atomic_load_explicit(&e->mid, memory_order_relaxed);");
        w.println("    if (LIKELY(e->owner == owner && e->name == name && e->desc == desc && cached != NULL)) {");
        w.println("        return cached;");
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
    }

    private void emitFieldLookupFunctions(PrintWriter w) {
        w.println("__attribute__((hot))");
        w.println("static jfieldID vm_get_field_id(JNIEnv* env, jclass cls, const char* owner, const char* name, const char* desc) {");
        w.println("    uint32_t idx = triple_hash(owner, name, desc) & (FIELD_CACHE_SIZE - 1);");
        w.println("    FieldCacheEntry* e = &fieldCache[idx];");
        w.println("    jfieldID cached = atomic_load_explicit(&e->fid, memory_order_relaxed);");
        w.println("    if (LIKELY(e->owner == owner && e->name == name && e->desc == desc && cached != NULL)) {");
        w.println("        return cached;");
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
        w.println("    jfieldID cached = atomic_load_explicit(&e->fid, memory_order_relaxed);");
        w.println("    if (LIKELY(e->owner == owner && e->name == name && e->desc == desc && cached != NULL)) {");
        w.println("        return cached;");
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
    }

    private void emitVmMethodLookupCache(PrintWriter w) {
        w.println("// === VM method lookup (direct VM-to-VM calls) ===");
        w.println("#define VM_METHOD_LOOKUP_SIZE 1024");
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
        w.println("        m->argTypesStr = (m->argTypesIdx >= 0) ? vm_get_string(m->argTypesIdx) : NULL;");
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
        w.println("            me->flags = 0;");
        w.println("            me->classStr = (me->classIdx >= 0) ? vm_get_string(me->classIdx) : NULL;");
        w.println("            me->ownerStr = (me->ownerIdx >= 0) ? vm_get_string(me->ownerIdx) : NULL;");
        w.println("            me->nameStr = (me->nameIdx >= 0) ? vm_get_string(me->nameIdx) : NULL;");
        w.println("            me->descStr = (me->descIdx >= 0) ? vm_get_string(me->descIdx) : NULL;");
        w.println("            me->argTypesStr = (me->argTypesIdx >= 0) ? vm_get_string(me->argTypesIdx) : NULL;");
        w.println("            if (me->type == META_METHOD && me->ownerStr && me->nameStr && me->descStr) {");
        w.println("                me->vmTargetId = vm_lookup_method(me->ownerStr, me->nameStr, me->descStr);");
        w.println("                if (strcmp(me->ownerStr, \"java/lang/invoke/MethodHandle\") == 0 &&");
        w.println("                    (strcmp(me->nameStr, \"invoke\") == 0 || strcmp(me->nameStr, \"invokeExact\") == 0)) {");
        w.println("                    me->flags |= META_FLAG_MH_POLY_INVOKE;");
        w.println("                }");
        w.println("            } else {");
        w.println("                me->vmTargetId = -1;");
        w.println("            }");
        w.println("        }");
        w.println("    }");
        w.println("}");
        w.println();
    }
}
