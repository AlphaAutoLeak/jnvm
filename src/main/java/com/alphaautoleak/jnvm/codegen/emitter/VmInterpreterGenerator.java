package com.alphaautoleak.jnvm.codegen.emitter;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * 生成 vm_interpreter.h 和 vm_interpreter.c - VM 解释器核心
 * 
 * 重构后的版本：使用 Instruction 类封装每个指令的生成逻辑
 */
public class VmInterpreterGenerator {
    
    private final File dir;
    private final boolean debug;
    private final Instructions instructions;
    
    public VmInterpreterGenerator(File dir, boolean debug) {
        this.dir = dir;
        this.debug = debug;
        this.instructions = new Instructions();
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
            w.println("jobject vm_execute_method(JNIEnv* env, int methodId, jobject instance, jobjectArray args);");
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
            
            if (debug) {
                w.println("#define VM_LOG(fmt, ...) printf(\"[VM] \" fmt, ##__VA_ARGS__)");
            } else {
                w.println("#define VM_LOG(fmt, ...)");
            }
            w.println();
            
            // 辅助函数：从字符串池获取字符串
            w.println("static const char* get_string(VMString* pool, int idx) {");
            w.println("    return pool[idx].data;");
            w.println("}");
            w.println();
            
            // 辅助函数：获取当前指令的元数据
            w.println("static MetaEntry* get_meta(VMMethod* m, int pc) {");
            w.println("    int idx = m->pcToMetaIdx[pc];");
            w.println("    return idx >= 0 ? &m->metadata[idx] : NULL;");
            w.println("}");
            w.println();
            
            // 解密函数
            w.println("static void vm_decrypt_string(const unsigned char* enc, int len, char* out, const unsigned char* key) {");
            w.println("    for (int i = 0; i < len; i++) {");
            w.println("        out[i] = (char)((enc[i] - (i & 0xFF)) ^ key[i % 8]);");
            w.println("    }");
            w.println("    out[len] = '\\0';");
            w.println("}");
            w.println();
            
            // 解析方法描述符：获取参数数量和返回类型
            w.println("static void parse_method_desc(const char* desc, int* argCount, char* returnType) {");
            w.println("    *argCount = 0;");
            w.println("    const char* p = desc + 1; // skip '('");
            w.println("    while (*p && *p != ')') {");
            w.println("        if (*p == 'L') { while (*p && *p != ';') p++; if (*p) p++; }"); // 对象类型
            w.println("        else if (*p == '[') { while (*p == '[') p++; if (*p == 'L') { while (*p && *p != ';') p++; } p++; }"); // 数组类型
            w.println("        else { p++; }"); // 基本类型
            w.println("        (*argCount)++;");
            w.println("    }");
            w.println("    if (*p == ')') p++;");
            w.println("    *returnType = *p ? *p : 'V';");
            w.println("}");
            w.println();
            
            // invokedynamic 实现 - 简化版本
            w.println("// invokedynamic 辅助函数 - 通过反射调用 bootstrap method");
            w.println("jobject vm_invoke_dynamic(JNIEnv* env, VMFrame* frame, MetaEntry* meta) {");
            w.println("    // 获取 bootstrap method 信息");
            w.println("    const char* bsmClass = vm_strings[meta->ownerIdx].data;");
            w.println("    const char* bsmName = vm_strings[meta->nameIdx].data;");
            w.println("    const char* bsmDesc = vm_strings[meta->descIdx].data;");
            w.println("    VM_LOG(\"INVOKEDYNAMIC: bsm=%s.%s%s\\n\", bsmClass, bsmName, bsmDesc);");
            w.println("    ");
            w.println("    // 查找 bootstrap method 类");
            w.println("    jclass cls = (*env)->FindClass(env, bsmClass);");
            w.println("    if (!cls) {");
            w.println("        VM_LOG(\"INVOKEDYNAMIC: Class not found: %s\\n\", bsmClass);");
            w.println("        return NULL;");
            w.println("    }");
            w.println("    ");
            w.println("    // 获取 bootstrap method");
            w.println("    jmethodID bsmMethod = (*env)->GetStaticMethodID(env, cls, bsmName, bsmDesc);");
            w.println("    if (!bsmMethod) {");
            w.println("        VM_LOG(\"INVOKEDYNAMIC: Method not found: %s%s\\n\", bsmName, bsmDesc);");
            w.println("        (*env)->ExceptionClear(env);");
            w.println("        return NULL;");
            w.println("    }");
            w.println("    ");
            w.println("    // 对于 Lambda，直接调用 JVM 内置的 LambdaMetafactory");
            w.println("    // 这里简化处理：假设 bootstrap method 返回一个 CallSite");
            w.println("    // 我们调用它并获取目标 MethodHandle");
            w.println("    ");
            w.println("    // 创建简化的参数 (实际需要: Lookup, name, MethodType, args...)");
            w.println("    // 当前仅支持简单的无参 Lambda");
            w.println("    jobject result = (*env)->CallStaticObjectMethod(env, cls, bsmMethod);");
            w.println("    ");
            w.println("    if ((*env)->ExceptionCheck(env)) {");
            w.println("        VM_LOG(\"INVOKEDYNAMIC: Exception during bootstrap\\n\");");
            w.println("        (*env)->ExceptionDescribe(env);");
            w.println("        (*env)->ExceptionClear(env);");
            w.println("        return NULL;");
            w.println("    }");
            w.println("    ");
            w.println("    return result;");
            w.println("}");
            w.println();
            
            // 解释器主函数
            emitExecuteMethod(w);
        }
    }
    
    private void emitExecuteMethod(PrintWriter w) {
        w.println("jobject vm_execute_method(JNIEnv* env, int methodId, jobject instance, jobjectArray args) {");
        w.println("    if (methodId < 0 || methodId >= vm_method_count) return NULL;");
        w.println("    VMMethod* m = &vm_methods[methodId];");
        w.println();
        
        // 解密字节码
        w.println("    // Decrypt bytecode");
        w.println("    uint8_t* bytecode = (uint8_t*)malloc(m->bytecodeLen);");
        w.println("    chacha20_encrypt(m->key, m->nonce, m->bytecode, bytecode, m->bytecodeLen);");
        w.println();
        
        w.println("    // Initialize frame");
        w.println("    VMFrame frame = { .pc = 0, .sp = 0 };");
        w.println("    frame.stack = (VMValue*)calloc(m->maxStack, sizeof(VMValue));");
        w.println("    frame.locals = (VMValue*)calloc(m->maxLocals, sizeof(VMValue));");
        w.println();
        
        w.println("    // Set this and args");
        w.println("    frame.locals[0].l = instance;");
        w.println("    if (args) {");
        w.println("        jsize len = (*env)->GetArrayLength(env, args);");
        w.println("        for (jsize i = 0; i < len; i++) {");
        w.println("            frame.locals[i + (instance ? 1 : 0)].l = (*env)->GetObjectArrayElement(env, args, i);");
        w.println("        }");
        w.println("    }");
        w.println();
        
        w.println("    // Main interpreter loop");
        w.println("    VM_LOG(\"Executing method %d, bytecodeLen=%d\\n\", methodId, m->bytecodeLen);");
        w.println();
        w.println("    while (frame.pc < m->bytecodeLen) {");
        w.println("        uint8_t opcode = bytecode[frame.pc];");
        w.println("        MetaEntry* meta = get_meta(m, frame.pc);");
        w.println("        VM_LOG(\"pc=%d op=0x%02x sp=%d\\n\", frame.pc, opcode, frame.sp);");
        w.println();
        w.println("        switch (opcode) {");
        
        // 生成所有指令
        for (Instruction inst : instructions.getAllInstructions()) {
            inst.generate(w);
        }
        
        w.println("            default:");
        w.println("                VM_LOG(\"Unknown opcode: 0x%02x at pc=%d\\n\", opcode, frame.pc);");
        w.println("                frame.pc++;");
        w.println("                break;");
        w.println("        }");
        w.println("        if ((*env)->ExceptionCheck(env)) {");
        w.println("            VM_LOG(\"Exception thrown at pc=%d\\n\", frame.pc);");
        w.println("            break;");
        w.println("        }");
        w.println("    }");
        w.println();
        w.println("    jobject result = (frame.sp > 0) ? frame.stack[--frame.sp].l : NULL;");
        w.println("    VM_LOG(\"Method %d finished, result=%p\\n\", methodId, result);");
        w.println("    free(frame.locals);");
        w.println("    free(frame.stack);");
        w.println("    free(bytecode);");
        w.println("    return result;");
        w.println("}");
    }
}