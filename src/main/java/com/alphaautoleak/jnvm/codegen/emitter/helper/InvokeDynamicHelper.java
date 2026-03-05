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
        w.println("jobject vm_invoke_dynamic(JNIEnv* env, VMFrame* frame, MetaEntry* meta) {");
        w.println("    if (!meta) { VM_LOG(\"INVOKEDYNAMIC: meta is NULL\\n\"); return NULL; }");
        w.println("    const char* methodName = vm_strings[meta->nameIdx].data;");
        w.println("    const char* methodDesc = vm_strings[meta->descIdx].data;");
        w.println("    VM_LOG(\"INVOKEDYNAMIC: name=%s, desc=%s, bsmIdx=%d\\n\", methodName, methodDesc, meta->bsmIdx);");
        w.println();
        w.println("    if (meta->bsmIdx < 0 || meta->bsmIdx >= vm_bootstrap_count) {");
        w.println("        VM_LOG(\"INVOKEDYNAMIC: Invalid bsmIdx=%d\\n\", meta->bsmIdx); return NULL;");
        w.println("    }");
        w.println("    VMBootstrapMethod* bsm = &vm_bootstrap_methods[meta->bsmIdx];");
        w.println("    const char* bsmClass = vm_strings[bsm->ownerIdx].data;");
        w.println();
        w.println("    if (strstr(bsmClass, \"LambdaMetafactory\") == NULL) {");
        w.println("        VM_LOG(\"INVOKEDYNAMIC: Unsupported bootstrap: %s\\n\", bsmClass); return NULL;");
        w.println("    }");
        w.println();
        emitLambdaFactory(w);
        w.println("}");
        w.println();
    }
    
    private void emitLambdaFactory(PrintWriter w) {
        w.println("    // Extract interface class from methodDesc");
        w.println("    char interfaceClass[256] = {0};");
        w.println("    const char* paren = strchr(methodDesc, ')');");
        w.println("    if (!paren) { VM_LOG(\"INVOKEDYNAMIC: No closing paren\\n\"); return NULL; }");
        w.println("    paren++;");
        w.println("    if (*paren == 'L') {");
        w.println("        paren++;");
        w.println("        const char* semicolon = strchr(paren, ';');");
        w.println("        if (!semicolon) { VM_LOG(\"INVOKEDYNAMIC: No semicolon\\n\"); return NULL; }");
        w.println("        int len = semicolon - paren;");
        w.println("        strncpy(interfaceClass, paren, len);");
        w.println("    } else { VM_LOG(\"INVOKEDYNAMIC: Unsupported return type\\n\"); return NULL; }");
        w.println();
        w.println("    if (bsm->argCount < 3) { VM_LOG(\"INVOKEDYNAMIC: Not enough args\\n\"); return NULL; }");
        w.println("    const char* samMethodTypeStr = vm_strings[bsm->args[0].strIdx].data;");
        w.println("    const char* implMethodStr = vm_strings[bsm->args[1].strIdx].data;");
        w.println("    const char* instantiatedMethodTypeStr = vm_strings[bsm->args[2].strIdx].data;");
        w.println("    int handleTag = bsm->args[1].handleTag;");
        w.println();
        w.println("    // Parse implMethod: owner.name(desc)");
        w.println("    char implOwner[256] = {0}, implName[256] = {0}, implDesc[512] = {0};");
        w.println("    const char* implParen = strchr(implMethodStr, '(');");
        w.println("    if (!implParen) { VM_LOG(\"INVOKEDYNAMIC: No impl paren\\n\"); return NULL; }");
        w.println("    strncpy(implDesc, implParen, sizeof(implDesc) - 1);");
        w.println("    const char* lastDot = NULL;");
        w.println("    for (const char* p = implMethodStr; p < implParen; p++) { if (*p == '.') lastDot = p; }");
        w.println("    if (!lastDot) { VM_LOG(\"INVOKEDYNAMIC: No impl dot\\n\"); return NULL; }");
        w.println("    strncpy(implOwner, implMethodStr, lastDot - implMethodStr);");
        w.println("    strncpy(implName, lastDot + 1, implParen - lastDot - 1);");
        w.println();
        w.println("    jclass ownerClass = (*env)->FindClass(env, implOwner);");
        w.println("    if (!ownerClass) { VM_LOG(\"INVOKEDYNAMIC: Owner not found: %s\\n\", implOwner); (*env)->ExceptionClear(env); return NULL; }");
        w.println();
        w.println("    // Get MethodHandles.lookup()");
        w.println("    jclass mhClass = (*env)->FindClass(env, \"java/lang/invoke/MethodHandles\");");
        w.println("    jmethodID lookupMid = (*env)->GetStaticMethodID(env, mhClass, \"lookup\", \"()Ljava/lang/invoke/MethodHandles$Lookup;\");");
        w.println("    jobject callerLookup = (*env)->CallStaticObjectMethod(env, mhClass, lookupMid);");
        w.println("    jmethodID privateLookupInMid = (*env)->GetStaticMethodID(env, mhClass, \"privateLookupIn\",");
        w.println("        \"(Ljava/lang/Class;Ljava/lang/invoke/MethodHandles$Lookup;)Ljava/lang/invoke/MethodHandles$Lookup;\");");
        w.println("    jobject lookup = (*env)->CallStaticObjectMethod(env, mhClass, privateLookupInMid, ownerClass, callerLookup);");
        w.println("    if (!lookup) { VM_LOG(\"INVOKEDYNAMIC: privateLookupIn failed\\n\"); (*env)->ExceptionClear(env); return NULL; }");
        w.println();
        w.println("    // Create MethodTypes");
        w.println("    jclass mtClass = (*env)->FindClass(env, \"java/lang/invoke/MethodType\");");
        w.println("    jmethodID fromDescMid = (*env)->GetStaticMethodID(env, mtClass, \"fromMethodDescriptorString\",");
        w.println("        \"(Ljava/lang/String;Ljava/lang/ClassLoader;)Ljava/lang/invoke/MethodType;\");");
        w.println("    jclass classClass = (*env)->FindClass(env, \"java/lang/Class\");");
        w.println("    jmethodID getClassLoaderMid = (*env)->GetMethodID(env, classClass, \"getClassLoader\", \"()Ljava/lang/ClassLoader;\");");
        w.println("    jobject classLoader = (*env)->CallObjectMethod(env, ownerClass, getClassLoaderMid);");
        w.println();
        w.println("    jstring samDescJStr = (*env)->NewStringUTF(env, samMethodTypeStr);");
        w.println("    jobject samMethodType = (*env)->CallStaticObjectMethod(env, mtClass, fromDescMid, samDescJStr, classLoader);");
        w.println("    jstring implDescJStr = (*env)->NewStringUTF(env, implDesc);");
        w.println("    jobject implMethodType = (*env)->CallStaticObjectMethod(env, mtClass, fromDescMid, implDescJStr, classLoader);");
        w.println("    jstring instDescJStr = (*env)->NewStringUTF(env, instantiatedMethodTypeStr);");
        w.println("    jobject instantiatedMethodType = (*env)->CallStaticObjectMethod(env, mtClass, fromDescMid, instDescJStr, classLoader);");
        w.println();
        w.println("    // Find method handle");
        w.println("    jclass lookupClass = (*env)->FindClass(env, \"java/lang/invoke/MethodHandles$Lookup\");");
        w.println("    jobject implMethodHandle = NULL;");
        w.println("    jstring implNameJStr = (*env)->NewStringUTF(env, implName);");
        w.println();
        w.println("    if (handleTag == 6) { // invokeStatic");
        w.println("        jmethodID findStaticMid = (*env)->GetMethodID(env, lookupClass, \"findStatic\",");
        w.println("            \"(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;\");");
        w.println("        implMethodHandle = (*env)->CallObjectMethod(env, lookup, findStaticMid, ownerClass, implNameJStr, implMethodType);");
        w.println("    } else if (handleTag == 5 || handleTag == 9) { // invokeVirtual/Interface");
        w.println("        jmethodID findVirtualMid = (*env)->GetMethodID(env, lookupClass, \"findVirtual\",");
        w.println("            \"(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;\");");
        w.println("        implMethodHandle = (*env)->CallObjectMethod(env, lookup, findVirtualMid, ownerClass, implNameJStr, implMethodType);");
        w.println("    } else if (handleTag == 7) { // invokeSpecial");
        w.println("        jmethodID findSpecialMid = (*env)->GetMethodID(env, lookupClass, \"findSpecial\",");
        w.println("            \"(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;\");");
        w.println("        implMethodHandle = (*env)->CallObjectMethod(env, lookup, findSpecialMid, ownerClass, implNameJStr, implMethodType, ownerClass);");
        w.println("    } else { VM_LOG(\"INVOKEDYNAMIC: Unsupported handleTag=%d\\n\", handleTag); return NULL; }");
        w.println();
        w.println("    if (!implMethodHandle) { VM_LOG(\"INVOKEDYNAMIC: Failed to get MethodHandle\\n\"); (*env)->ExceptionClear(env); return NULL; }");
        w.println();
        w.println("    // Call LambdaMetafactory.metafactory");
        w.println("    jclass lmfClass = (*env)->FindClass(env, \"java/lang/invoke/LambdaMetafactory\");");
        w.println("    jmethodID metafactoryMid = (*env)->GetStaticMethodID(env, lmfClass, \"metafactory\",");
        w.println("        \"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;\");");
        w.println("    jstring methodNameJStr = (*env)->NewStringUTF(env, methodName);");
        w.println("    jstring methodDescJStr = (*env)->NewStringUTF(env, methodDesc);");
        w.println("    jobject invokedType = (*env)->CallStaticObjectMethod(env, mtClass, fromDescMid, methodDescJStr, classLoader);");
        w.println("    jobject callSite = (*env)->CallStaticObjectMethod(env, lmfClass, metafactoryMid,");
        w.println("        lookup, methodNameJStr, invokedType, samMethodType, implMethodHandle, instantiatedMethodType);");
        w.println();
        w.println("    if (!callSite) { VM_LOG(\"INVOKEDYNAMIC: metafactory returned NULL\\n\"); (*env)->ExceptionClear(env); return NULL; }");
        w.println();
        w.println("    // Count captured args");
        w.println("    int capturedCount = 0;");
        w.println("    const char* p = methodDesc + 1;");
        w.println("    while (*p && *p != ')') {");
        w.println("        if (*p == 'L') { while (*p && *p != ';') p++; if (*p) p++; }");
        w.println("        else if (*p == '[') { while (*p == '[') p++; if (*p == 'L') { while (*p && *p != ';') p++; } if (*p) p++; }");
        w.println("        else { p++; }");
        w.println("        capturedCount++;");
        w.println("    }");
        w.println();
        w.println("    // Get target and invoke");
        w.println("    jclass callSiteClass = (*env)->FindClass(env, \"java/lang/invoke/CallSite\");");
        w.println("    jmethodID getTargetMid = (*env)->GetMethodID(env, callSiteClass, \"getTarget\", \"()Ljava/lang/invoke/MethodHandle;\");");
        w.println("    jobject targetHandle = (*env)->CallObjectMethod(env, callSite, getTargetMid);");
        w.println("    jclass mhClass2 = (*env)->FindClass(env, \"java/lang/invoke/MethodHandle\");");
        w.println("    jmethodID invokeMid = (*env)->GetMethodID(env, mhClass2, \"invokeWithArguments\", \"([Ljava/lang/Object;)Ljava/lang/Object;\");");
        w.println();
        w.println("    jclass objectClass = (*env)->FindClass(env, \"java/lang/Object\");");
        w.println("    jobjectArray invokeArgs = (*env)->NewObjectArray(env, capturedCount, objectClass, NULL);");
        w.println("    for (int i = capturedCount - 1; i >= 0; i--) {");
        w.println("        jobject arg = frame->stack[--frame->sp].l;");
        w.println("        (*env)->SetObjectArrayElement(env, invokeArgs, i, arg);");
        w.println("    }");
        w.println();
        w.println("    jobject result = (*env)->CallObjectMethod(env, targetHandle, invokeMid, invokeArgs);");
        w.println("    if ((*env)->ExceptionCheck(env)) { VM_LOG(\"INVOKEDYNAMIC: Exception\\n\"); (*env)->ExceptionClear(env); return NULL; }");
        w.println("    return result;");
    }
}
