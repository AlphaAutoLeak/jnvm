package com.alphaautoleak.jnvm.codegen.emitter;

import com.alphaautoleak.jnvm.codegen.emitter.helper.VMHelper;
import com.alphaautoleak.jnvm.codegen.emitter.helper.VMHelpers;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * 生成 vm_interpreter.h 和 vm_interpreter.c - VM 解释器核心
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
            w.println("jobject vm_execute_method(JNIEnv* env, int methodId, jobject instance, jobjectArray args, jclass callerClass);");
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
            
            // === 类和方法缓存系统（必须在辅助函数之前定义）===
            w.println("// === 类和方法ID缓存 ===");
            w.println("#define CLASS_CACHE_SIZE 256");
            w.println("#define METHOD_CACHE_SIZE 1024");
            w.println();
            
            // 类缓存结构
            w.println("typedef struct {");
            w.println("    const char* className;  // 类名指针（指向 vm_strings）");
            w.println("    jclass cls;             // 缓存的 jclass");
            w.println("} ClassCacheEntry;");
            w.println();
            
            // 方法缓存结构
            w.println("typedef struct {");
            w.println("    const char* owner;      // 类名");
            w.println("    const char* name;       // 方法名");
            w.println("    const char* desc;       // 方法描述符");
            w.println("    jmethodID mid;          // 缓存的 jmethodID");
            w.println("    jclass cls;             // 关联的 jclass（避免重复查找）");
            w.println("} MethodCacheEntry;");
            w.println();
            
            // 缓存数组
            w.println("static ClassCacheEntry classCache[CLASS_CACHE_SIZE];");
            w.println("static int classCacheCount = 0;");
            w.println("static MethodCacheEntry methodCache[METHOD_CACHE_SIZE];");
            w.println("static int methodCacheCount = 0;");
            w.println();
            
            // 缓存的 FindClass
            w.println("static jclass vm_find_class(JNIEnv* env, const char* className) {");
            w.println("    // 先查缓存");
            w.println("    for (int i = 0; i < classCacheCount; i++) {");
            w.println("        if (classCache[i].className == className && classCache[i].cls != NULL) {");
            w.println("            return classCache[i].cls;");
            w.println("        }");
            w.println("    }");
            w.println("    // 缓存未命中，调用 JNI");
            w.println("    jclass localCls = (*env)->FindClass(env, className);");
            w.println("    if (localCls && classCacheCount < CLASS_CACHE_SIZE) {");
            w.println("        jclass globalCls = (*env)->NewGlobalRef(env, localCls);");  // 创建全局引用防止GC回收
            w.println("        if (globalCls) {");
            w.println("            classCache[classCacheCount].className = className;");
            w.println("            classCache[classCacheCount].cls = globalCls;");
            w.println("            classCacheCount++;");
            w.println("        }");
            w.println("    }");
            w.println("    return localCls;");  // 返回局部引用（调用者可以继续使用）
            w.println("}");
            w.println();
            
            // 缓存的 GetMethodID
            w.println("static jmethodID vm_get_method_id(JNIEnv* env, jclass cls, const char* owner, const char* name, const char* desc) {");
            w.println("    // 先查缓存");
            w.println("    for (int i = 0; i < methodCacheCount; i++) {");
            w.println("        if (methodCache[i].owner == owner && methodCache[i].name == name && ");
            w.println("            methodCache[i].desc == desc && methodCache[i].mid != NULL) {");
            w.println("            return methodCache[i].mid;");
            w.println("        }");
            w.println("    }");
            w.println("    // 缓存未命中，调用 JNI");
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
            
            // 缓存的 GetStaticMethodID
            w.println("static jmethodID vm_get_static_method_id(JNIEnv* env, jclass cls, const char* owner, const char* name, const char* desc) {");
            w.println("    // 先查缓存");
            w.println("    for (int i = 0; i < methodCacheCount; i++) {");
            w.println("        if (methodCache[i].owner == owner && methodCache[i].name == name && ");
            w.println("            methodCache[i].desc == desc && methodCache[i].mid != NULL) {");
            w.println("            return methodCache[i].mid;");
            w.println("        }");
            w.println("    }");
            w.println("    // 缓存未命中，调用 JNI");
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
                        
                        // 主解释器函数
                        emitExecuteMethod(w);        }
    }
    
    private void emitExecuteMethod(PrintWriter w) {
        // 字节码缓存 - 避免每次调用都解密
        w.println("// 字节码缓存（延迟初始化）");
        w.println("static uint8_t* vm_bytecode_cache = NULL;");
        w.println("static int vm_bytecode_cache_size = 0;");
        w.println();
        
        w.println("jobject vm_execute_method(JNIEnv* env, int methodId, jobject instance, jobjectArray args, jclass callerClass) {");
        w.println("    methodId ^= METHOD_ID_XOR_KEY;");
        w.println("    if (methodId < 0 || methodId >= vm_method_count) return NULL;");
        w.println("    VMMethod* m = &vm_methods[methodId];");
        w.println();
        
        // 解密字节码（使用缓存）
        w.println("    // 使用方法结构中的缓存指针");
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
        w.println("        // 不加密时直接使用原始字节码");
        w.println("        bytecode = m->bytecode;");
        w.println("    }");
        w.println();
        
        // 初始化帧
        w.println("    VMValue result = {0};");
        w.println("    char resultType = 'V';  // 'V'=void, 'I'=int, 'J'=long, 'F'=float, 'D'=double, 'L'=object");
        w.println("    VMFrame frame = { .pc = 0, .sp = 0, .callerClass = callerClass };");
        w.println("    frame.stack = (VMValue*)calloc(m->maxStack, sizeof(VMValue));");
        w.println("    frame.locals = (VMValue*)calloc(m->maxLocals, sizeof(VMValue));");
        w.println("    frame.stackTypes = (VMType*)calloc(m->maxStack, sizeof(VMType));");
        w.println();
        
        // 设置 this 和参数
        w.println("    frame.locals[0].l = instance;");
        w.println("    const char* methodDesc = (m->descIdx >= 0) ? vm_get_string(m->descIdx) : NULL;");
        w.println("    vm_unbox_args(env, &frame, args, methodDesc, instance ? 1 : 0);");
        w.println();
        
        // 主循环
        w.println("    VM_LOG(\"Executing method %d, bytecodeLen=%d\\n\", methodId, m->bytecodeLen);");
        w.println("    fflush(stdout);");
        w.println("    while (frame.pc < m->bytecodeLen) {");
        w.println("        uint8_t opcode = bytecode[frame.pc];");
        w.println("        MetaEntry* meta = vm_get_meta(m, frame.pc);");
        w.println("        VM_LOG(\"m%d: pc=%d op=0x%02x sp=%d\\n\", methodId, frame.pc, opcode, frame.sp);");
        w.println();
        w.println("        switch (opcode) {");
        
        for (Instruction inst : instructions.getAllInstructions()) {
            inst.generate(w);
        }
        
        w.println("            default:");
        w.println("                VM_LOG(\"Unknown opcode: 0x%02x at pc=%d\\n\", opcode, frame.pc);");
        w.println("                frame.pc++; break;");
        w.println("        }");
        w.println();
        
        // 异常处理
        w.println("        if ((*env)->ExceptionCheck(env)) {");
        w.println("            VM_LOG(\"Exception thrown at pc=%d\\n\", frame.pc);");
        w.println("            jthrowable exception = (*env)->ExceptionOccurred(env);");
        w.println("            (*env)->ExceptionClear(env);");
        w.println("            int handlerPc = vm_find_exception_handler(env, m, frame.pc, exception);");
        w.println("            if (handlerPc >= 0) {");
        w.println("                frame.sp = 0;");
        w.println("                frame.stack[frame.sp++].l = exception;");
        w.println("                frame.pc = handlerPc;");
        w.println("                continue;");
        w.println("            }");
        w.println("            VM_LOG(\"No handler found, rethrowing\\n\");");
        w.println("            (*env)->Throw(env, exception);");
        w.println("            break;");
        w.println("        }");
        w.println("    }");
        w.println();
        
        // 返回结果
        w.println("method_exit:");
        w.println("    ;");
        w.println("    char methodReturnType = 'V';");
        w.println("    if (methodDesc) {");
        w.println("        const char* p = methodDesc;");
        w.println("        while (*p && *p != ')') p++;");
        w.println("        if (*p == ')') methodReturnType = *(p + 1);");
        w.println("    }");
        w.println("    VM_LOG(\"Method %d finished, returnType=%c\\n\", methodId, methodReturnType);");
        w.println("    fflush(stdout);");
        w.println("    jobject resultObj = vm_box_result(env, result, methodReturnType);");
        w.println("    free(frame.locals);");
        w.println("    free(frame.stack);");
        w.println("    free(frame.stackTypes);");
        w.println("    // 注意: bytecode 不应该在这里释放:");
        w.println("    // - 加密方法: bytecode 是缓存在 m->cachedBytecode 中，后续调用会重用");
        w.println("    // - 非加密方法: bytecode 指向 m->bytecode (静态数据)，不能释放");
        w.println("    return resultObj;");
        w.println("}");
    }
}
