package com.alphaautoleak.jnvm.codegen.emitter;

import com.alphaautoleak.jnvm.codegen.emitter.helper.VMHelper;
import com.alphaautoleak.jnvm.codegen.emitter.helper.VMHelpers;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * 生成 vm_interpreter.h 和 vm_interpreter.c - VM 解释器核心
 * 为每种返回类型生成不同的函数，避免装箱/拆箱
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
            
            // 辅助函数声明
            for (VMHelper helper : helpers.getAllHelpers()) {
                helper.generateHeader(w);
            }
            
            w.println();
            // 各返回类型的执行函数声明
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
            
            // === 分支预测提示宏 ===
            w.println("// === 分支预测提示 ===");
            w.println("#define LIKELY(x)   __builtin_expect(!!(x), 1)");
            w.println("#define UNLIKELY(x) __builtin_expect(!!(x), 0)");
            w.println();
            
            // === 类和方法缓存系统 ===
            emitCachingSystem(w);
            
            // 辅助函数实现
            for (VMHelper helper : helpers.getAllHelpers()) {
                helper.generateSource(w);
            }
            
            // 字符串解密函数
            w.println("static void vm_decrypt_string(const unsigned char* enc, int len, char* out, const unsigned char* key) {");
            w.println("    for (int i = 0; i < len; i++) {");
            w.println("        out[i] = (char)((enc[i] - (i & 0xFF)) ^ key[i % 8]);");
            w.println("    }");
            w.println("    out[len] = '\\0';");
            w.println("}");
            w.println();
            
            // 主解释器函数（带返回类型参数）
            emitExecuteCommon(w);
            
            // 各返回类型的包装函数
            emitExecuteWrappers(w);
        }
    }
    
    private void emitCachingSystem(PrintWriter w) {
        w.println("// === 类、方法ID、字段ID缓存 ===");
        w.println("#define CLASS_CACHE_SIZE 256");
        w.println("#define METHOD_CACHE_SIZE 1024");
        w.println("#define FIELD_CACHE_SIZE 512");
        w.println();
        
        w.println("typedef struct {");
        w.println("    const char* className;");
        w.println("    jclass cls;");
        w.println("} ClassCacheEntry;");
        w.println();
        
        w.println("typedef struct {");
        w.println("    const char* owner;");
        w.println("    const char* name;");
        w.println("    const char* desc;");
        w.println("    jmethodID mid;");
        w.println("    jclass cls;");
        w.println("} MethodCacheEntry;");
        w.println();
        
        w.println("typedef struct {");
        w.println("    const char* owner;");
        w.println("    const char* name;");
        w.println("    const char* desc;");
        w.println("    jfieldID fid;");
        w.println("    jclass cls;");
        w.println("    int isStatic;");
        w.println("} FieldCacheEntry;");
        w.println();
        
        w.println("static ClassCacheEntry classCache[CLASS_CACHE_SIZE];");
        w.println("static int classCacheCount = 0;");
        w.println("static MethodCacheEntry methodCache[METHOD_CACHE_SIZE];");
        w.println("static int methodCacheCount = 0;");
        w.println("static FieldCacheEntry fieldCache[FIELD_CACHE_SIZE];");
        w.println("static int fieldCacheCount = 0;");
        w.println();
        
        w.println("static jclass vm_find_class(JNIEnv* env, const char* className) {");
        w.println("    for (int i = 0; i < classCacheCount; i++) {");
        w.println("        if (classCache[i].className == className && classCache[i].cls != NULL) {");
        w.println("            return classCache[i].cls;");
        w.println("        }");
        w.println("    }");
        w.println("    jclass localCls = (*env)->FindClass(env, className);");
        w.println("    if (localCls && classCacheCount < CLASS_CACHE_SIZE) {");
        w.println("        jclass globalCls = (*env)->NewGlobalRef(env, localCls);");
        w.println("        if (globalCls) {");
        w.println("            classCache[classCacheCount].className = className;");
        w.println("            classCache[classCacheCount].cls = globalCls;");
        w.println("            classCacheCount++;");
        w.println("        }");
        w.println("    }");
        w.println("    return localCls;");
        w.println("}");
        w.println();
        
        w.println("static jmethodID vm_get_method_id(JNIEnv* env, jclass cls, const char* owner, const char* name, const char* desc) {");
        w.println("    for (int i = 0; i < methodCacheCount; i++) {");
        w.println("        if (methodCache[i].owner == owner && methodCache[i].name == name && ");
        w.println("            methodCache[i].desc == desc && methodCache[i].mid != NULL) {");
        w.println("            return methodCache[i].mid;");
        w.println("        }");
        w.println("    }");
        w.println("    jmethodID mid = (*env)->GetMethodID(env, cls, name, desc);");
        w.println("    if (mid && methodCacheCount < METHOD_CACHE_SIZE) {");
        w.println("        methodCache[methodCacheCount].owner = owner;");
        w.println("        methodCache[methodCacheCount].name = name;");
        w.println("        methodCache[methodCacheCount].desc = desc;");
        w.println("        methodCache[methodCacheCount].mid = mid;");
        w.println("        methodCache[methodCacheCount].cls = cls;");
        w.println("        methodCacheCount++;");
        w.println("    }");
        w.println("    return mid;");
        w.println("}");
        w.println();
        
        w.println("static jmethodID vm_get_static_method_id(JNIEnv* env, jclass cls, const char* owner, const char* name, const char* desc) {");
        w.println("    for (int i = 0; i < methodCacheCount; i++) {");
        w.println("        if (methodCache[i].owner == owner && methodCache[i].name == name && ");
        w.println("            methodCache[i].desc == desc && methodCache[i].mid != NULL) {");
        w.println("            return methodCache[i].mid;");
        w.println("        }");
        w.println("    }");
        w.println("    jmethodID mid = (*env)->GetStaticMethodID(env, cls, name, desc);");
        w.println("    if (mid && methodCacheCount < METHOD_CACHE_SIZE) {");
        w.println("        methodCache[methodCacheCount].owner = owner;");
        w.println("        methodCache[methodCacheCount].name = name;");
        w.println("        methodCache[methodCacheCount].desc = desc;");
        w.println("        methodCache[methodCacheCount].mid = mid;");
        w.println("        methodCache[methodCacheCount].cls = cls;");
        w.println("        methodCacheCount++;");
        w.println("    }");
        w.println("    return mid;");
        w.println("}");
        w.println();
        
        w.println("static jfieldID vm_get_field_id(JNIEnv* env, jclass cls, const char* owner, const char* name, const char* desc) {");
        w.println("    for (int i = 0; i < fieldCacheCount; i++) {");
        w.println("        if (fieldCache[i].owner == owner && fieldCache[i].name == name && ");
        w.println("            fieldCache[i].desc == desc && fieldCache[i].fid != NULL) {");
        w.println("            return fieldCache[i].fid;");
        w.println("        }");
        w.println("    }");
        w.println("    jfieldID fid = (*env)->GetFieldID(env, cls, name, desc);");
        w.println("    if (fid && fieldCacheCount < FIELD_CACHE_SIZE) {");
        w.println("        fieldCache[fieldCacheCount].owner = owner;");
        w.println("        fieldCache[fieldCacheCount].name = name;");
        w.println("        fieldCache[fieldCacheCount].desc = desc;");
        w.println("        fieldCache[fieldCacheCount].fid = fid;");
        w.println("        fieldCache[fieldCacheCount].cls = cls;");
        w.println("        fieldCache[fieldCacheCount].isStatic = 0;");
        w.println("        fieldCacheCount++;");
        w.println("    }");
        w.println("    return fid;");
        w.println("}");
        w.println();
        
        w.println("static jfieldID vm_get_static_field_id(JNIEnv* env, jclass cls, const char* owner, const char* name, const char* desc) {");
        w.println("    for (int i = 0; i < fieldCacheCount; i++) {");
        w.println("        if (fieldCache[i].owner == owner && fieldCache[i].name == name && ");
        w.println("            fieldCache[i].desc == desc && fieldCache[i].fid != NULL) {");
        w.println("            return fieldCache[i].fid;");
        w.println("        }");
        w.println("    }");
        w.println("    jfieldID fid = (*env)->GetStaticFieldID(env, cls, name, desc);");
        w.println("    if (fid && fieldCacheCount < FIELD_CACHE_SIZE) {");
        w.println("        fieldCache[fieldCacheCount].owner = owner;");
        w.println("        fieldCache[fieldCacheCount].name = name;");
        w.println("        fieldCache[fieldCacheCount].desc = desc;");
        w.println("        fieldCache[fieldCacheCount].fid = fid;");
        w.println("        fieldCache[fieldCacheCount].cls = cls;");
        w.println("        fieldCache[fieldCacheCount].isStatic = 1;");
        w.println("        fieldCacheCount++;");
        w.println("    }");
        w.println("    return fid;");
        w.println("}");
        w.println();
    }
    
    private void emitExecuteCommon(PrintWriter w) {
        w.println("// 执行结果结构（用于返回值和类型）");
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
        
        // 解密字节码
        w.println("    uint8_t* bytecode;");
        w.println("    if (m->encrypted) {");
        w.println("        if (m->cachedBytecode == NULL) {");
        w.println("            bytecode = (uint8_t*)malloc(m->bytecodeLen);");
        w.println("            chacha20_encrypt(m->key, m->nonce, m->bytecode, bytecode, m->bytecodeLen);");
        w.println("            m->cachedBytecode = bytecode;");
        w.println("        } else {");
        w.println("            bytecode = m->cachedBytecode;");
        w.println("        }");
        w.println("    } else {");
        w.println("        bytecode = m->bytecode;");
        w.println("    }");
        w.println();
        
        // 初始化帧
        w.println("    VMFrame frame = { .pc = 0, .sp = 0, .callerClass = callerClass };");
        w.println("    frame.stack = (VMValue*)calloc(m->maxStack, sizeof(VMValue));");
        w.println("    frame.locals = (VMValue*)calloc(m->maxLocals, sizeof(VMValue));");
        w.println("    frame.stackTypes = (VMType*)calloc(m->maxStack, sizeof(VMType));");
        w.println();
        
        // 设置参数
        w.println("    frame.locals[0].l = instance;");
        w.println("    const char* methodDesc = (m->descIdx >= 0) ? vm_get_string(m->descIdx) : NULL;");
        w.println("    vm_unbox_args(env, &frame, args, methodDesc, instance ? 1 : 0);");
        w.println();
        
        // Computed Goto 跳转表
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
        
        w.println("    #define DISPATCH_NEXT \\");
        w.println("        do { \\");
        w.println("            if (UNLIKELY(frame.pc >= m->bytecodeLen)) goto method_exit; \\");
        w.println("            uint8_t _op = bytecode[frame.pc]; \\");
        w.println("            int _metaIdx = m->pcToMetaIdx[frame.pc]; \\");
        w.println("            meta = (_metaIdx >= 0) ? &m->metadata[_metaIdx] : NULL; \\");
        w.println("            goto *dispatch_table[_op]; \\");
        w.println("        } while(0)");
        w.println();
        
        w.println("    MetaEntry* meta = NULL;");
        w.println("    DISPATCH_NEXT;");
        w.println();
        
        // 生成所有指令处理代码
        for (Instruction inst : instructions.getAllInstructions()) {
            inst.generateComputedGoto(w);
            w.println();
        }
        
        w.println("        OP_DEFAULT:");
        w.println("            VM_LOG(\"Unknown opcode: 0x%02x at pc=%d\\n\", bytecode[frame.pc], frame.pc);");
        w.println("            frame.pc++;");
        w.println("            DISPATCH_NEXT;");
        w.println();
        
        // 退出时返回结果
        w.println("    method_exit:");
        w.println("    ;");
        w.println("    // 从方法描述符获取返回类型");
        w.println("    if (methodDesc) {");
        w.println("        const char* p = methodDesc;");
        w.println("        while (*p && *p != ')') p++;");
        w.println("        if (*p == ')') execResult.returnType = *(p + 1);");
        w.println("    }");
        w.println("    // 从栈顶获取返回值");
        w.println("    if (frame.sp > 0) {");
        w.println("        execResult.value = frame.stack[frame.sp - 1];");
        w.println("    }");
        w.println("    free(frame.locals);");
        w.println("    free(frame.stack);");
        w.println("    free(frame.stackTypes);");
        w.println("    return execResult;");
        w.println("}");
        w.println();
    }
    
    private void emitExecuteWrappers(PrintWriter w) {
        // void
        w.println("void vm_execute_method_void(JNIEnv* env, int methodId, jobject instance, jobjectArray args, jclass callerClass) {");
        w.println("    ExecuteResult r = vm_execute_common(env, methodId, instance, args, callerClass);");
        w.println("    (void)r;  // 忽略返回值");
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