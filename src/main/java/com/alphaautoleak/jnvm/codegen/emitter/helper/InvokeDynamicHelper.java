package com.alphaautoleak.jnvm.codegen.emitter.helper;

import java.io.PrintWriter;

/**
 * InvokeDynamic 辅助函数（Lambda 支持）
 */
public class InvokeDynamicHelper extends VMHelper {
    
    @Override
    public String[] getIncludes() {
        return new String[] { "vm_types.h", "vm_data.h", "<jni.h>", "<string.h>", "<stdio.h>" };
    }
    
    @Override
    public void generateHeader(PrintWriter w) {
        w.println("jobject vm_invoke_dynamic(JNIEnv* env, VMFrame* frame, MetaEntry* meta);");
    }
    
    @Override
    public void generateSource(PrintWriter w) {
        // 静态缓存 - 类和方法ID
        w.println("// === InvokeDynamic 静态缓存 ===");
        w.println("static jclass id_mhClass = NULL;");
        w.println("static jmethodID id_lookupMid = NULL;");
        w.println("static jmethodID id_privateLookupInMid = NULL;");
        w.println("static jclass id_mtClass = NULL;");
        w.println("static jmethodID id_fromDescMid = NULL;");
        w.println("static jclass id_classClass = NULL;");
        w.println("static jmethodID id_getClassLoaderMid = NULL;");
        w.println("static jmethodID id_forNameMid = NULL;");
        w.println("static jclass id_lookupClass = NULL;");
        w.println("static jmethodID id_findStaticMid = NULL;");
        w.println("static jmethodID id_findVirtualMid = NULL;");
        w.println("static jmethodID id_findSpecialMid = NULL;");
        w.println("static jclass id_lmfClass = NULL;");
        w.println("static jmethodID id_metafactoryMid = NULL;");
        w.println("static jclass id_callSiteClass = NULL;");
        w.println("static jmethodID id_getTargetMid = NULL;");
        w.println("static jmethodID id_invokeWithArgsMid = NULL;");
        w.println("static jclass id_objectClass = NULL;");
        w.println("static jclass id_intClass = NULL;");
        w.println("static jclass id_longClass = NULL;");
        w.println("static jclass id_floatClass = NULL;");
        w.println("static jclass id_doubleClass = NULL;");
        w.println("static jclass id_boolClass = NULL;");
        w.println("static jmethodID id_intValueOfMid = NULL;");
        w.println("static jmethodID id_longValueOfMid = NULL;");
        w.println("static jmethodID id_floatValueOfMid = NULL;");
        w.println("static jmethodID id_doubleValueOfMid = NULL;");
        w.println("static jmethodID id_boolValueOfMid = NULL;");
        w.println();
        
        w.println("jobject vm_invoke_dynamic(JNIEnv* env, VMFrame* frame, MetaEntry* meta) {");
        w.println("    if (!meta) { VM_LOG(\"INVOKEDYNAMIC: meta is NULL\\n\"); return NULL; }");
        w.println("    const char* methodName = vm_get_string(meta->nameIdx);");
        w.println("    const char* methodDesc = vm_get_string(meta->descIdx);");
        w.println("    VM_LOG(\"INVOKEDYNAMIC: name=%s, desc=%s, bsmIdx=%d\\n\", methodName, methodDesc, meta->bsmIdx);");
        w.println();
        w.println("    if (meta->bsmIdx < 0 || meta->bsmIdx >= vm_bootstrap_count) {");
        w.println("        VM_LOG(\"INVOKEDYNAMIC: Invalid bsmIdx=%d\\n\", meta->bsmIdx); return NULL;");
        w.println("    }");
        w.println("    VMBootstrapMethod* bsm = &vm_bootstrap_methods[meta->bsmIdx];");
        w.println("    const char* bsmClass = vm_get_string(bsm->ownerIdx);");
        w.println();
        w.println("    // 初始化静态缓存");
        w.println("    if (!id_mhClass) {");
        w.println("        id_mhClass = vm_find_class(env, \"java/lang/invoke/MethodHandles\");");
        w.println("        if (id_mhClass) id_lookupMid = (*env)->GetStaticMethodID(env, id_mhClass, \"lookup\", \"()Ljava/lang/invoke/MethodHandles$Lookup;\");");
        w.println("        if (id_mhClass) id_privateLookupInMid = (*env)->GetStaticMethodID(env, id_mhClass, \"privateLookupIn\",");
        w.println("            \"(Ljava/lang/Class;Ljava/lang/invoke/MethodHandles$Lookup;)Ljava/lang/invoke/MethodHandles$Lookup;\");");
        w.println("    }");
        w.println("    if (!id_mtClass) {");
        w.println("        id_mtClass = vm_find_class(env, \"java/lang/invoke/MethodType\");");
        w.println("        if (id_mtClass) id_fromDescMid = (*env)->GetStaticMethodID(env, id_mtClass, \"fromMethodDescriptorString\",");
        w.println("            \"(Ljava/lang/String;Ljava/lang/ClassLoader;)Ljava/lang/invoke/MethodType;\");");
        w.println("    }");
        w.println("    if (!id_classClass) {");
        w.println("        id_classClass = vm_find_class(env, \"java/lang/Class\");");
        w.println("        if (id_classClass) id_getClassLoaderMid = (*env)->GetMethodID(env, id_classClass, \"getClassLoader\", \"()Ljava/lang/ClassLoader;\");");
        w.println("        if (id_classClass) id_forNameMid = (*env)->GetStaticMethodID(env, id_classClass, \"forName\", \"(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;\");");
        w.println("    }");
        w.println("    if (!id_lookupClass) {");
        w.println("        id_lookupClass = vm_find_class(env, \"java/lang/invoke/MethodHandles$Lookup\");");
        w.println("        if (id_lookupClass) id_findStaticMid = (*env)->GetMethodID(env, id_lookupClass, \"findStatic\",");
        w.println("            \"(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;\");");
        w.println("        if (id_lookupClass) id_findVirtualMid = (*env)->GetMethodID(env, id_lookupClass, \"findVirtual\",");
        w.println("            \"(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;\");");
        w.println("        if (id_lookupClass) id_findSpecialMid = (*env)->GetMethodID(env, id_lookupClass, \"findSpecial\",");
        w.println("            \"(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;\");");
        w.println("    }");
        w.println("    if (!id_lmfClass) {");
        w.println("        id_lmfClass = vm_find_class(env, \"java/lang/invoke/LambdaMetafactory\");");
        w.println("        if (id_lmfClass) id_metafactoryMid = (*env)->GetStaticMethodID(env, id_lmfClass, \"metafactory\",");
        w.println("            \"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;\");");
        w.println("    }");
        w.println("    if (!id_callSiteClass) {");
        w.println("        id_callSiteClass = vm_find_class(env, \"java/lang/invoke/CallSite\");");
        w.println("        if (id_callSiteClass) id_getTargetMid = (*env)->GetMethodID(env, id_callSiteClass, \"getTarget\", \"()Ljava/lang/invoke/MethodHandle;\");");
        w.println("    }");
        w.println("    if (!id_invokeWithArgsMid) {");
        w.println("        jclass mhClass = vm_find_class(env, \"java/lang/invoke/MethodHandle\");");
        w.println("        if (mhClass) id_invokeWithArgsMid = (*env)->GetMethodID(env, mhClass, \"invokeWithArguments\", \"([Ljava/lang/Object;)Ljava/lang/Object;\");");
        w.println("    }");
        w.println("    if (!id_objectClass) {");
        w.println("        id_objectClass = vm_find_class(env, \"java/lang/Object\");");
        w.println("        id_intClass = vm_find_class(env, \"java/lang/Integer\");");
        w.println("        id_longClass = vm_find_class(env, \"java/lang/Long\");");
        w.println("        id_floatClass = vm_find_class(env, \"java/lang/Float\");");
        w.println("        id_doubleClass = vm_find_class(env, \"java/lang/Double\");");
        w.println("        id_boolClass = vm_find_class(env, \"java/lang/Boolean\");");
        w.println("        if (id_intClass) id_intValueOfMid = (*env)->GetStaticMethodID(env, id_intClass, \"valueOf\", \"(I)Ljava/lang/Integer;\");");
        w.println("        if (id_longClass) id_longValueOfMid = (*env)->GetStaticMethodID(env, id_longClass, \"valueOf\", \"(J)Ljava/lang/Long;\");");
        w.println("        if (id_floatClass) id_floatValueOfMid = (*env)->GetStaticMethodID(env, id_floatClass, \"valueOf\", \"(F)Ljava/lang/Float;\");");
        w.println("        if (id_doubleClass) id_doubleValueOfMid = (*env)->GetStaticMethodID(env, id_doubleClass, \"valueOf\", \"(D)Ljava/lang/Double;\");");
        w.println("        if (id_boolClass) id_boolValueOfMid = (*env)->GetStaticMethodID(env, id_boolClass, \"valueOf\", \"(Z)Ljava/lang/Boolean;\");");
        w.println("    }");
        w.println();
        emitLambdaFactory(w);
        w.println("}");
        w.println();
    }
    
    private void emitLambdaFactory(PrintWriter w) {
        // ===== Step 1: Parse BSM args =====
        w.println("    // LambdaMetafactory.metafactory(Lookup, String, MethodType, MethodType, MethodHandle, MethodType)");
        w.println("    // BSM args: [0]=samMethodType, [1]=implMethod (MethodHandle), [2]=instantiatedMethodType");
        w.println("    if (bsm->argCount < 3) {");
        w.println("        VM_LOG(\"INVOKEDYNAMIC: Not enough BSM args (need 3, got %d)\\n\", bsm->argCount);");
        w.println("        return NULL;");
        w.println("    }");
        w.println();
        
        // ===== Step 2: Get BSM argument strings =====
        w.println("    const char* samMethodTypeStr = vm_get_string(bsm->args[0].strIdx);");
        w.println("    const char* implMethodStr = vm_get_string(bsm->args[1].strIdx);");
        w.println("    const char* instantiatedMethodTypeStr = vm_get_string(bsm->args[2].strIdx);");
        w.println("    int handleTag = bsm->args[1].handleTag; // implMethod handle tag (REF_invokeStatic=6, etc)");
        w.println("    VM_LOG(\"INVOKEDYNAMIC: samType=%s, implMethod=%s, instType=%s, handleTag=%d\\n\",");
        w.println("        samMethodTypeStr, implMethodStr, instantiatedMethodTypeStr, handleTag);");
        w.println();
        
        // ===== Step 3: Parse implMethod string =====
        w.println("    // Parse implMethod: format is \"owner.name(desc)\"");
        w.println("    char implOwner[256] = {0}, implName[256] = {0}, implDesc[512] = {0};");
        w.println("    const char* implParen = strchr(implMethodStr, '(');");
        w.println("    if (!implParen) { VM_LOG(\"INVOKEDYNAMIC: No impl paren in %s\\n\", implMethodStr); return NULL; }");
        w.println("    strncpy(implDesc, implParen, sizeof(implDesc) - 1);");
        w.println("    const char* lastDot = NULL;");
        w.println("    for (const char* p = implMethodStr; p < implParen; p++) { if (*p == '.') lastDot = p; }");
        w.println("    if (!lastDot) { VM_LOG(\"INVOKEDYNAMIC: No dot in implMethod %s\\n\", implMethodStr); return NULL; }");
        w.println("    strncpy(implOwner, implMethodStr, lastDot - implMethodStr);");
        w.println("    strncpy(implName, lastDot + 1, implParen - lastDot - 1);");
        w.println("    VM_LOG(\"INVOKEDYNAMIC: implOwner=%s, implName=%s, implDesc=%s\\n\", implOwner, implName, implDesc);");
        w.println();
        
        // ===== Step 4: Calculate captured arguments (inline, not nested function) =====
        w.println("    // Calculate captured arguments from invokedType (methodDesc)");
        w.println("    int capturedCount = 0;");
        w.println("    const char* p = methodDesc + 1; // skip '('");
        w.println("    while (*p && *p != ')') {");
        w.println("        if (*p == 'L') { while (*p && *p != ';') p++; if (*p) p++; }");
        w.println("        else if (*p == '[') { while (*p == '[') p++; if (*p == 'L') { while (*p && *p != ';') p++; } if (*p) p++; }");
        w.println("        else { p++; }");
        w.println("        capturedCount++;");
        w.println("    }");
        w.println("    VM_LOG(\"INVOKEDYNAMIC: methodDesc=%s, capturedCount=%d, sp=%d\\n\", methodDesc, capturedCount, frame->sp);");
        w.println();
        w.println("    // Validate we have enough stack elements");
        w.println("    if (frame->sp < capturedCount) {");
        w.println("        VM_LOG(\"INVOKEDYNAMIC: Stack underflow! sp=%d, need=%d\\n\", frame->sp, capturedCount);");
        w.println("        return NULL;");
        w.println("    }");
        w.println();
        
        // ===== Step 5: Get caller class for Lookup =====
        w.println("    jclass ownerClass = (*env)->FindClass(env, implOwner);");
        w.println("    if (!ownerClass) { VM_LOG(\"INVOKEDYNAMIC: Owner class not found: %s\\n\", implOwner); (*env)->ExceptionClear(env); return NULL; }");
        w.println();
        
        // ===== Step 6: Create Lookup with proper caller class =====
        w.println("    // Get MethodHandles.Lookup for the caller class (使用缓存)");
        w.println("    jobject publicLookup = (*env)->CallStaticObjectMethod(env, id_mhClass, id_lookupMid);");
        w.println();
        w.println("    // Use privateLookupIn to get lookup with correct caller class");
        w.println("    jobject lookup = (*env)->CallStaticObjectMethod(env, id_mhClass, id_privateLookupInMid, ownerClass, publicLookup);");
        w.println("    if (!lookup) {");
        w.println("        VM_LOG(\"INVOKEDYNAMIC: privateLookupIn failed\\n\");");
        w.println("        if ((*env)->ExceptionCheck(env)) { (*env)->ExceptionDescribe(env); (*env)->ExceptionClear(env); }");
        w.println("        return NULL;");
        w.println("    }");
        w.println();
        
        // ===== Step 7: Create MethodType objects =====
        w.println("    // Create MethodTypes from descriptor strings (使用缓存)");
        w.println("    jobject classLoader = (*env)->CallObjectMethod(env, ownerClass, id_getClassLoaderMid);");
        w.println();
        w.println("    jstring samDescJStr = (*env)->NewStringUTF(env, samMethodTypeStr);");
        w.println("    jobject samMethodType = (*env)->CallStaticObjectMethod(env, id_mtClass, id_fromDescMid, samDescJStr, classLoader);");
        w.println("    if (!samMethodType) { VM_LOG(\"INVOKEDYNAMIC: Failed to create samMethodType\\n\"); (*env)->ExceptionClear(env); return NULL; }");
        w.println();
        w.println("    jstring implDescJStr = (*env)->NewStringUTF(env, implDesc);");
        w.println("    jobject implMethodType = (*env)->CallStaticObjectMethod(env, id_mtClass, id_fromDescMid, implDescJStr, classLoader);");
        w.println("    if (!implMethodType) { VM_LOG(\"INVOKEDYNAMIC: Failed to create implMethodType\\n\"); (*env)->ExceptionClear(env); return NULL; }");
        w.println();
        w.println("    jstring instDescJStr = (*env)->NewStringUTF(env, instantiatedMethodTypeStr);");
        w.println("    jobject instantiatedMethodType = (*env)->CallStaticObjectMethod(env, id_mtClass, id_fromDescMid, instDescJStr, classLoader);");
        w.println("    if (!instantiatedMethodType) { VM_LOG(\"INVOKEDYNAMIC: Failed to create instantiatedMethodType\\n\"); (*env)->ExceptionClear(env); return NULL; }");
        w.println();
        
        // ===== Step 8: Find the implementation method handle =====
        w.println("    // Find the implementation method handle (使用缓存)");
        w.println("    jobject implMethodHandle = NULL;");
        w.println("    jstring implNameJStr = (*env)->NewStringUTF(env, implName);");
        w.println();
        w.println("    VM_LOG(\"INVOKEDYNAMIC: Finding method handle, tag=%d\\n\", handleTag);");
        w.println("    if (handleTag == 6) { // REF_invokeStatic");
        w.println("        implMethodHandle = (*env)->CallObjectMethod(env, lookup, id_findStaticMid, ownerClass, implNameJStr, implMethodType);");
        w.println("    } else if (handleTag == 5 || handleTag == 9) { // REF_invokeVirtual || REF_invokeInterface");
        w.println("        implMethodHandle = (*env)->CallObjectMethod(env, lookup, id_findVirtualMid, ownerClass, implNameJStr, implMethodType);");
        w.println("    } else if (handleTag == 7) { // REF_invokeSpecial");
        w.println("        implMethodHandle = (*env)->CallObjectMethod(env, lookup, id_findSpecialMid, ownerClass, implNameJStr, implMethodType, ownerClass);");
        w.println("    } else {");
        w.println("        VM_LOG(\"INVOKEDYNAMIC: Unsupported handleTag=%d\\n\", handleTag);");
        w.println("        return NULL;");
        w.println("    }");
        w.println();
        w.println("    if (!implMethodHandle) {");
        w.println("        VM_LOG(\"INVOKEDYNAMIC: Failed to get MethodHandle for %s.%s\\n\", implOwner, implName);");
        w.println("        if ((*env)->ExceptionCheck(env)) { (*env)->ExceptionDescribe(env); (*env)->ExceptionClear(env); }");
        w.println("        return NULL;");
        w.println("    }");
        w.println();
        
        // ===== Step 9: Create invokedType (MethodType of the lambda factory) =====
        w.println("    // Create invokedType from methodDesc");
        w.println("    jstring methodDescJStr = (*env)->NewStringUTF(env, methodDesc);");
        w.println("    jobject invokedType = (*env)->CallStaticObjectMethod(env, id_mtClass, id_fromDescMid, methodDescJStr, classLoader);");
        w.println("    if (!invokedType) { VM_LOG(\"INVOKEDYNAMIC: Failed to create invokedType\\n\"); (*env)->ExceptionClear(env); return NULL; }");
        w.println();
        
        // ===== Step 10: Call LambdaMetafactory.metafactory =====
        w.println("    // Call LambdaMetafactory.metafactory (使用缓存)");
        w.println("    jstring methodNameJStr = (*env)->NewStringUTF(env, methodName);");
        w.println();
        w.println("    VM_LOG(\"INVOKEDYNAMIC: Calling metafactory: name=%s, invokedType=%s\\n\", methodName, methodDesc);");
        w.println("    jobject callSite = (*env)->CallStaticObjectMethod(env, id_lmfClass, id_metafactoryMid,");
        w.println("        lookup, methodNameJStr, invokedType, samMethodType, implMethodHandle, instantiatedMethodType);");
        w.println();
        w.println("    if (!callSite) {");
        w.println("        VM_LOG(\"INVOKEDYNAMIC: metafactory returned NULL\\n\");");
        w.println("        if ((*env)->ExceptionCheck(env)) { (*env)->ExceptionDescribe(env); (*env)->ExceptionClear(env); }");
        w.println("        return NULL;");
        w.println("    }");
        w.println();
        
        // ===== Step 11: Get target MethodHandle from CallSite =====
        w.println("    // Get target MethodHandle from CallSite (使用缓存)");
        w.println("    jobject targetHandle = (*env)->CallObjectMethod(env, callSite, id_getTargetMid);");
        w.println("    if (!targetHandle) { VM_LOG(\"INVOKEDYNAMIC: getTarget returned NULL\\n\"); (*env)->ExceptionClear(env); return NULL; }");
        w.println();
        
        // ===== Step 12: If no captured args, just return the lambda instance =====
        w.println("    // If no captured arguments, we can invoke with empty args");
        w.println("    if (capturedCount == 0) {");
        w.println("        VM_LOG(\"INVOKEDYNAMIC: No captured args, returning lambda instance\\n\");");
        w.println("        jobject result = (*env)->CallObjectMethod(env, targetHandle, id_invokeWithArgsMid, NULL);");
        w.println("        if ((*env)->ExceptionCheck(env)) { (*env)->ExceptionDescribe(env); (*env)->ExceptionClear(env); return NULL; }");
        w.println("        return result;");
        w.println("    }");
        w.println();
        
        // ===== Step 13: Collect captured arguments from stack =====
        w.println("    // Collect captured arguments from stack (使用缓存的类和方法)");
        w.println("    jobjectArray invokeArgs = (*env)->NewObjectArray(env, capturedCount, id_objectClass, NULL);");
        w.println("    if (!invokeArgs) { VM_LOG(\"INVOKEDYNAMIC: Failed to create args array\\n\"); (*env)->ExceptionClear(env); return NULL; }");
        w.println();
        w.println("    VM_LOG(\"INVOKEDYNAMIC: Collecting %d args from stack, sp=%d\\n\", capturedCount, frame->sp);");
        w.println("    const char* argp = methodDesc + 1;");
        w.println("    for (int i = capturedCount - 1; i >= 0; i--) {");
        w.println("        jobject boxedArg = NULL;");
        w.println("        char argType = *argp;");
        w.println("        VM_LOG(\"INVOKEDYNAMIC:   arg[%d] type=%c\\n\", i, argType);");
        w.println("        if (argType == 'L' || argType == '[') {");
        w.println("            boxedArg = frame->stack[--frame->sp].l;");
        w.println("            if (argType == 'L') { while (*argp && *argp != ';') argp++; if (*argp) argp++; }");
        w.println("            else { while (*argp == '[') argp++; if (*argp == 'L') { while (*argp && *argp != ';') argp++; } if (*argp) argp++; }");
        w.println("        } else if (argType == 'I') {");
        w.println("            jint val = frame->stack[--frame->sp].i;");
        w.println("            boxedArg = (*env)->CallStaticObjectMethod(env, id_intClass, id_intValueOfMid, val);");
        w.println("            argp++;");
        w.println("        } else if (argType == 'J') {");
        w.println("            jlong val = frame->stack[--frame->sp].j;");
        w.println("            boxedArg = (*env)->CallStaticObjectMethod(env, id_longClass, id_longValueOfMid, val);");
        w.println("            argp++;");
        w.println("        } else if (argType == 'F') {");
        w.println("            jfloat val = frame->stack[--frame->sp].f;");
        w.println("            boxedArg = (*env)->CallStaticObjectMethod(env, id_floatClass, id_floatValueOfMid, val);");
        w.println("            argp++;");
        w.println("        } else if (argType == 'D') {");
        w.println("            jdouble val = frame->stack[--frame->sp].d;");
        w.println("            boxedArg = (*env)->CallStaticObjectMethod(env, id_doubleClass, id_doubleValueOfMid, val);");
        w.println("            argp++;");
        w.println("        } else if (argType == 'Z') {");
        w.println("            jint val = frame->stack[--frame->sp].i;");
        w.println("            boxedArg = (*env)->CallStaticObjectMethod(env, id_boolClass, id_boolValueOfMid, val);");
        w.println("            argp++;");
        w.println("        } else if (argType == 'B' || argType == 'S' || argType == 'C') {");
        w.println("            jint val = frame->stack[--frame->sp].i;");
        w.println("            boxedArg = (*env)->CallStaticObjectMethod(env, id_intClass, id_intValueOfMid, val);");
        w.println("            argp++;");
        w.println("        } else {");
        w.println("            VM_LOG(\"INVOKEDYNAMIC: Unknown arg type: %c\\n\", argType);");
        w.println("            argp++;");
        w.println("        }");
        w.println("        VM_LOG(\"INVOKEDYNAMIC:   arg[%d] boxed=%p\\n\", i, boxedArg);");
        w.println("        (*env)->SetObjectArrayElement(env, invokeArgs, i, boxedArg);");
        w.println("    }");
        w.println();
        
        // ===== Step 14: Invoke with captured arguments =====
        w.println("    // Invoke the lambda factory with captured arguments (使用缓存)");
        w.println("    jobject result = (*env)->CallObjectMethod(env, targetHandle, id_invokeWithArgsMid, invokeArgs);");
        w.println();
        w.println("    if ((*env)->ExceptionCheck(env)) {");
        w.println("        VM_LOG(\"INVOKEDYNAMIC: Exception during invoke\\n\");");
        w.println("        (*env)->ExceptionDescribe(env);");
        w.println("        (*env)->ExceptionClear(env);");
        w.println("        return NULL;");
        w.println("    }");
        w.println("    VM_LOG(\"INVOKEDYNAMIC: Success, result=%p\\n\", result);");
        w.println("    return result;");
    }
}
