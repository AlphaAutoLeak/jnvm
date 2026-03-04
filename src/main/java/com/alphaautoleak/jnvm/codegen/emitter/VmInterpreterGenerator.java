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
                w.println("#define VM_DEBUG_LOG(fmt, ...) printf(\"[VM-DEBUG] \" fmt, ##__VA_ARGS__)"); 
            } else {
                w.println("#define VM_LOG(fmt, ...)");
                w.println("#define VM_DEBUG_LOG(fmt, ...)");
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
            w.println("        else if (*p == '[') { while (*p == '[') p++; if (*p == 'L') { while (*p && *p != ';') p++; } if (*p) p++; }"); // 数组类型
            w.println("        else { p++; }"); // 基本类型
            w.println("        (*argCount)++;");
            w.println("    }");
            w.println("    if (*p == ')') p++;");
            w.println("    *returnType = *p ? *p : 'V';");
            w.println("}");
            w.println();
            
            // 从方法描述符中获取第 n 个参数的类型
            w.println("static char get_arg_type(const char* desc, int argIndex) {");
            w.println("    const char* p = desc + 1; // skip '('");
            w.println("    int current = 0;");
            w.println("    while (*p && *p != ')') {");
            w.println("        if (current == argIndex) {");
            w.println("            if (*p == 'L' || *p == '[') return 'L';");
            w.println("            return *p;");
            w.println("        }");
            w.println("        if (*p == 'L') { while (*p && *p != ';') p++; if (*p) p++; }");
            w.println("        else if (*p == '[') { while (*p == '[') p++; if (*p == 'L') { while (*p && *p != ';') p++; } if (*p) p++; }");
            w.println("        else { p++; }");
            w.println("        current++;");
            w.println("    }");
            w.println("    return 'L';");
            w.println("}");
            w.println();
            
            // invokedynamic 实现 - 纯 C 实现，使用反射
            w.println("// invokedynamic 辅助函数 - 纯 C 实现");
            w.println("jobject vm_invoke_dynamic(JNIEnv* env, VMFrame* frame, MetaEntry* meta) {");
            w.println("    const char* methodName = vm_strings[meta->nameIdx].data;");
            w.println("    const char* methodDesc = vm_strings[meta->descIdx].data;");
            w.println("    VM_LOG(\"INVOKEDYNAMIC: name=%s, desc=%s, bsmIdx=%d\\n\", methodName, methodDesc, meta->bsmIdx);");
            w.println("    ");
            w.println("    if (meta->bsmIdx < 0 || meta->bsmIdx >= vm_bootstrap_count) {");
            w.println("        VM_LOG(\"INVOKEDYNAMIC: Invalid bsmIdx=%d\\n\", meta->bsmIdx);");
            w.println("        return NULL;");
            w.println("    }");
            w.println("    VMBootstrapMethod* bsm = &vm_bootstrap_methods[meta->bsmIdx];");
            w.println("    const char* bsmClass = vm_strings[bsm->ownerIdx].data;");
            w.println("    ");
            w.println("    // LambdaMetafactory 处理");
            w.println("    if (strstr(bsmClass, \"LambdaMetafactory\") != NULL) {");
            w.println("        // 从 methodDesc 提取接口类型");
            w.println("        char interfaceClass[256] = {0};");
            w.println("        const char* paren = strchr(methodDesc, ')');");
            w.println("        if (!paren) { VM_LOG(\"INVOKEDYNAMIC: No closing paren\\n\"); return NULL; }");
            w.println("        paren++;");
            w.println("        if (*paren == 'L') {");
            w.println("            paren++;");
            w.println("            const char* semicolon = strchr(paren, ';');");
            w.println("            if (!semicolon) { VM_LOG(\"INVOKEDYNAMIC: No semicolon\\n\"); return NULL; }");
            w.println("            int len = semicolon - paren;");
            w.println("            strncpy(interfaceClass, paren, len);");
            w.println("        } else {");
            w.println("            VM_LOG(\"INVOKEDYNAMIC: Unsupported return type\\n\"); return NULL;");
            w.println("        }");
            w.println("        ");
            w.println("        // 获取 BSM args");
            w.println("        // args[0] = samMethodType, args[1] = implMethod, args[2] = instantiatedMethodType");
            w.println("        if (bsm->argCount < 3) {");
            w.println("            VM_LOG(\"INVOKEDYNAMIC: Not enough args\\n\"); return NULL;");
            w.println("        }");
            w.println("        ");
            w.println("        const char* samMethodTypeStr = vm_strings[bsm->args[0].strIdx].data;");
            w.println("        const char* implMethodStr = vm_strings[bsm->args[1].strIdx].data;");
            w.println("        const char* instantiatedMethodTypeStr = vm_strings[bsm->args[2].strIdx].data;");
            w.println("        VM_LOG(\"INVOKEDYNAMIC: samType=%s, implMethod=%s, instType=%s\\n\", samMethodTypeStr, implMethodStr, instantiatedMethodTypeStr);");
            w.println("        VM_LOG(\"INVOKEDYNAMIC: handleTag=%d\\n\", bsm->args[1].handleTag);");
            w.println("        ");
            w.println("        // 解析 implMethod: owner.name(desc)");
            w.println("        char implOwner[256] = {0}, implName[256] = {0}, implDesc[512] = {0};");
            w.println("        const char* implParen = strchr(implMethodStr, '(');");
            w.println("        if (!implParen) { VM_LOG(\"INVOKEDYNAMIC: No impl paren\\n\"); return NULL; }");
            w.println("        strncpy(implDesc, implParen, sizeof(implDesc) - 1);");
            w.println("        const char* lastDot = NULL;");
            w.println("        for (const char* p = implMethodStr; p < implParen; p++) { if (*p == '.') lastDot = p; }");
            w.println("        if (!lastDot) { VM_LOG(\"INVOKEDYNAMIC: No impl dot\\n\"); return NULL; }");
            w.println("        strncpy(implOwner, implMethodStr, lastDot - implMethodStr);");
            w.println("        strncpy(implName, lastDot + 1, implParen - lastDot - 1);");
            w.println("        VM_LOG(\"INVOKEDYNAMIC: parsed implOwner=%s, implName=%s, implDesc=%s\\n\", implOwner, implName, implDesc);");
            w.println("        ");
            w.println("        // 获取 owner 类");
            w.println("        jclass ownerClass = (*env)->FindClass(env, implOwner);");
            w.println("        if (!ownerClass) {");
            w.println("            VM_LOG(\"INVOKEDYNAMIC: Owner class not found: %s\\n\", implOwner);");
            w.println("            (*env)->ExceptionClear(env);");
            w.println("            return NULL;");
            w.println("        }");
            w.println("        ");
            w.println("        // 获取 MethodHandles.lookup()");
            w.println("        jclass mhClass = (*env)->FindClass(env, \"java/lang/invoke/MethodHandles\");");
            w.println("        jmethodID lookupMid = (*env)->GetStaticMethodID(env, mhClass, \"lookup\",");
            w.println("            \"()Ljava/lang/invoke/MethodHandles$Lookup;\");");
            w.println("        jobject callerLookup = (*env)->CallStaticObjectMethod(env, mhClass, lookupMid);");
            w.println("        ");
            w.println("        // privateLookupIn 获取对目标类的访问权限");
            w.println("        jmethodID privateLookupInMid = (*env)->GetStaticMethodID(env, mhClass, \"privateLookupIn\",");
            w.println("            \"(Ljava/lang/Class;Ljava/lang/invoke/MethodHandles$Lookup;)Ljava/lang/invoke/MethodHandles$Lookup;\");");
            w.println("        jobject lookup = (*env)->CallStaticObjectMethod(env, mhClass, privateLookupInMid, ownerClass, callerLookup);");
            w.println("        if (!lookup) {");
            w.println("            VM_LOG(\"INVOKEDYNAMIC: privateLookupIn failed\\n\");");
            w.println("            (*env)->ExceptionClear(env);");
            w.println("            return NULL;");
            w.println("        }");
            w.println("        ");
            w.println("        // 使用 MethodType.fromMethodDescriptorString 创建 MethodType");
            w.println("        jclass mtClass = (*env)->FindClass(env, \"java/lang/invoke/MethodType\");");
            w.println("        jmethodID fromDescMid = (*env)->GetStaticMethodID(env, mtClass, \"fromMethodDescriptorString\",");
            w.println("            \"(Ljava/lang/String;Ljava/lang/ClassLoader;)Ljava/lang/invoke/MethodType;\");");
            w.println("        jclass classClass = (*env)->FindClass(env, \"java/lang/Class\");");
            w.println("        jmethodID getClassLoaderMid = (*env)->GetMethodID(env, classClass, \"getClassLoader\", \"()Ljava/lang/ClassLoader;\");");
            w.println("        jobject classLoader = (*env)->CallObjectMethod(env, ownerClass, getClassLoaderMid);");
            w.println("        ");
            w.println("        jstring samDescJStr = (*env)->NewStringUTF(env, samMethodTypeStr);");
            w.println("        jobject samMethodType = (*env)->CallStaticObjectMethod(env, mtClass, fromDescMid, samDescJStr, classLoader);");
            w.println("        ");
            w.println("        jstring implDescJStr = (*env)->NewStringUTF(env, implDesc);");
            w.println("        jobject implMethodType = (*env)->CallStaticObjectMethod(env, mtClass, fromDescMid, implDescJStr, classLoader);");
            w.println("        ");
            w.println("        jstring instDescJStr = (*env)->NewStringUTF(env, instantiatedMethodTypeStr);");
            w.println("        jobject instantiatedMethodType = (*env)->CallStaticObjectMethod(env, mtClass, fromDescMid, instDescJStr, classLoader);");
            w.println("        ");
            w.println("        // 使用 Lookup.findStatic 或 findVirtual 获取 MethodHandle");
            w.println("        jclass lookupClass = (*env)->FindClass(env, \"java/lang/invoke/MethodHandles$Lookup\");");
            w.println("        int handleTag = bsm->args[1].handleTag;");
            w.println("        jobject implMethodHandle = NULL;");
            w.println("        ");
            w.println("        // handleTag: 5=invokeVirtual, 6=invokeStatic, 7=invokeSpecial, 9=invokeInterface");
            w.println("        if (handleTag == 6) { // invokeStatic");
            w.println("            jmethodID findStaticMid = (*env)->GetMethodID(env, lookupClass, \"findStatic\",");
            w.println("                \"(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;\");");
            w.println("            jstring implNameJStr = (*env)->NewStringUTF(env, implName);");
            w.println("            implMethodHandle = (*env)->CallObjectMethod(env, lookup, findStaticMid, ownerClass, implNameJStr, implMethodType);");
            w.println("        } else if (handleTag == 5 || handleTag == 9) { // invokeVirtual or invokeInterface");
            w.println("            jmethodID findVirtualMid = (*env)->GetMethodID(env, lookupClass, \"findVirtual\",");
            w.println("                \"(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;\");");
            w.println("            jstring implNameJStr = (*env)->NewStringUTF(env, implName);");
            w.println("            implMethodHandle = (*env)->CallObjectMethod(env, lookup, findVirtualMid, ownerClass, implNameJStr, implMethodType);");
            w.println("        } else if (handleTag == 7) { // invokeSpecial");
            w.println("            jmethodID findSpecialMid = (*env)->GetMethodID(env, lookupClass, \"findSpecial\",");
            w.println("                \"(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;\");");
            w.println("            jstring implNameJStr = (*env)->NewStringUTF(env, implName);");
            w.println("            implMethodHandle = (*env)->CallObjectMethod(env, lookup, findSpecialMid, ownerClass, implNameJStr, implMethodType, ownerClass);");
            w.println("        } else {");
            w.println("            VM_LOG(\"INVOKEDYNAMIC: Unsupported handleTag=%d\\n\", handleTag);");
            w.println("            return NULL;");
            w.println("        }");
            w.println("        ");
            w.println("        if (!implMethodHandle) {");
            w.println("            VM_LOG(\"INVOKEDYNAMIC: Failed to get MethodHandle\\n\");");
            w.println("            (*env)->ExceptionClear(env);");
            w.println("            return NULL;");
            w.println("        }");
            w.println("        ");
            w.println("        // 调用 LambdaMetafactory.metafactory");
            w.println("        jclass lmfClass = (*env)->FindClass(env, \"java/lang/invoke/LambdaMetafactory\");");
            w.println("        jmethodID metafactoryMid = (*env)->GetStaticMethodID(env, lmfClass, \"metafactory\",");
            w.println("            \"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;\");");
            w.println("        ");
            w.println("        // invokedType 是 INVOKEDYNAMIC 的方法类型");
            w.println("        jstring methodNameJStr = (*env)->NewStringUTF(env, methodName);");
            w.println("        jstring methodDescJStr = (*env)->NewStringUTF(env, methodDesc);");
            w.println("        jobject invokedType = (*env)->CallStaticObjectMethod(env, mtClass, fromDescMid, methodDescJStr, classLoader);");
            w.println("        ");
            w.println("        jobject callSite = (*env)->CallStaticObjectMethod(env, lmfClass, metafactoryMid,");
            w.println("            lookup, methodNameJStr, invokedType,");
            w.println("            samMethodType, implMethodHandle, instantiatedMethodType);");
            w.println("        ");
            w.println("        if (!callSite) {");
            w.println("            VM_LOG(\"INVOKEDYNAMIC: metafactory returned NULL\\n\");");
            w.println("            (*env)->ExceptionClear(env);");
            w.println("            return NULL;");
            w.println("        }");
            w.println("        ");
            w.println("        // 计算捕获参数数量（methodDesc 中 ')' 之前的参数数量）");
            w.println("        int capturedCount = 0;");
            w.println("        const char* p = methodDesc + 1; // skip '('");
            w.println("        while (*p && *p != ')') {");
            w.println("            if (*p == 'L') { while (*p && *p != ';') p++; if (*p) p++; }");
            w.println("            else if (*p == '[') { while (*p == '[') p++; if (*p == 'L') { while (*p && *p != ';') p++; } if (*p) p++; }");
            w.println("            else { p++; }");
            w.println("            capturedCount++;");
            w.println("        }");
            w.println("        ");
            w.println("        // 从 CallSite 获取目标 MethodHandle 并调用");
            w.println("        jclass callSiteClass = (*env)->FindClass(env, \"java/lang/invoke/CallSite\");");
            w.println("        jmethodID getTargetMid = (*env)->GetMethodID(env, callSiteClass, \"getTarget\", \"()Ljava/lang/invoke/MethodHandle;\");");
            w.println("        jobject targetHandle = (*env)->CallObjectMethod(env, callSite, getTargetMid);");
            w.println("        ");
            w.println("        // 使用 invokeWithArguments() 调用 MethodHandle");
            w.println("        jclass mhClass2 = (*env)->FindClass(env, \"java/lang/invoke/MethodHandle\");");
            w.println("        jmethodID invokeMid = (*env)->GetMethodID(env, mhClass2, \"invokeWithArguments\", \"([Ljava/lang/Object;)Ljava/lang/Object;\");");
            w.println("        ");
            w.println("        // 从栈上弹出捕获参数并构建参数数组");
            w.println("        jclass objectClass = (*env)->FindClass(env, \"java/lang/Object\");");
            w.println("        jobjectArray args = (*env)->NewObjectArray(env, capturedCount, objectClass, NULL);");
            w.println("        for (int i = capturedCount - 1; i >= 0; i--) {");
            w.println("            jobject arg = frame->stack[--frame->sp].l;");
            w.println("            (*env)->SetObjectArrayElement(env, args, i, arg);");
            w.println("        }");
            w.println("        ");
            w.println("        jobject result = (*env)->CallObjectMethod(env, targetHandle, invokeMid, args);");
            w.println("        ");
            w.println("        if ((*env)->ExceptionCheck(env)) {");
            w.println("            VM_LOG(\"INVOKEDYNAMIC: Exception invoking target\\n\");");
            w.println("            (*env)->ExceptionClear(env);");
            w.println("            return NULL;");
            w.println("        }");
            w.println("        ");
            w.println("        return result;");
            w.println("    }");
            w.println("    ");
            w.println("    VM_LOG(\"INVOKEDYNAMIC: Unsupported bootstrap\\n\");");
            w.println("    return NULL;");
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