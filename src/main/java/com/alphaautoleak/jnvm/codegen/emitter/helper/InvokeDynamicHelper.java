package com.alphaautoleak.jnvm.codegen.emitter.helper;

import java.io.PrintWriter;

/**
 * InvokeDynamic helper functions
 * Supports: LambdaMetafactory, StringConcatFactory, generic BSM fallback
 */
public class InvokeDynamicHelper extends VMHelper {

    @Override
    public String[] getIncludes() {
        return new String[] { "vm_types.h", "vm_data.h", "<jni.h>", "<string.h>", "<stdio.h>", "<stdlib.h>" };
    }

    @Override
    public void generateHeader(PrintWriter w) {
        w.println("jobject vm_invoke_dynamic(JNIEnv* env, VMFrame* frame, MetaEntry* meta);");
    }

    @Override
    public void generateSource(PrintWriter w) {
        emitStaticCache(w);
        emitHelperFunctions(w);
        emitMainFunction(w);
    }

    private void emitStaticCache(PrintWriter w) {
        w.println("// === InvokeDynamic static cache ===");
        // MethodHandles / Lookup
        w.println("static jclass id_mhClass = NULL;");
        w.println("static jmethodID id_lookupMid = NULL;");
        w.println("static jmethodID id_privateLookupInMid = NULL;");
        // MethodType
        w.println("static jclass id_mtClass = NULL;");
        w.println("static jmethodID id_fromDescMid = NULL;");
        // Class
        w.println("static jclass id_classClass = NULL;");
        w.println("static jmethodID id_getClassLoaderMid = NULL;");
        w.println("static jmethodID id_forNameMid = NULL;");
        // Lookup
        w.println("static jclass id_lookupClass = NULL;");
        w.println("static jmethodID id_findStaticMid = NULL;");
        w.println("static jmethodID id_findVirtualMid = NULL;");
        w.println("static jmethodID id_findSpecialMid = NULL;");
        w.println("static jmethodID id_findConstructorMid = NULL;");
        // LambdaMetafactory
        w.println("static jclass id_lmfClass = NULL;");
        w.println("static jmethodID id_metafactoryMid = NULL;");
        w.println("static jmethodID id_altMetafactoryMid = NULL;");
        // StringConcatFactory
        w.println("static jclass id_scfClass = NULL;");
        w.println("static jmethodID id_makeConcatMid = NULL;");
        w.println("static jmethodID id_makeConcatConstantsMid = NULL;");
        // CallSite / MethodHandle
        w.println("static jclass id_callSiteClass = NULL;");
        w.println("static jmethodID id_getTargetMid = NULL;");
        w.println("static jmethodID id_invokeWithArgsMid = NULL;");
        // Boxing
        w.println("static jclass id_objectClass = NULL;");
        w.println("static jclass id_intClass = NULL;");
        w.println("static jclass id_longClass = NULL;");
        w.println("static jclass id_floatClass = NULL;");
        w.println("static jclass id_doubleClass = NULL;");
        w.println("static jclass id_boolClass = NULL;");
        w.println("static jclass id_byteClass = NULL;");
        w.println("static jclass id_shortClass = NULL;");
        w.println("static jclass id_charClass = NULL;");
        w.println("static jmethodID id_intValueOfMid = NULL;");
        w.println("static jmethodID id_longValueOfMid = NULL;");
        w.println("static jmethodID id_floatValueOfMid = NULL;");
        w.println("static jmethodID id_doubleValueOfMid = NULL;");
        w.println("static jmethodID id_boolValueOfMid = NULL;");
        w.println("static jmethodID id_byteValueOfMid = NULL;");
        w.println("static jmethodID id_shortValueOfMid = NULL;");
        w.println("static jmethodID id_charValueOfMid = NULL;");
        // String
        w.println("static jclass id_stringClass = NULL;");
        w.println("static int id_indy_initialized = 0;");
        w.println();
    }

    private void emitHelperFunctions(PrintWriter w) {
        // Cache initialization
        w.println("static void vm_indy_init_cache(JNIEnv* env) {");
        w.println("    if (id_indy_initialized) return;");
        w.println("    id_indy_initialized = 1;");
        w.println();
        w.println("    id_mhClass = vm_find_class(env, \"java/lang/invoke/MethodHandles\");");
        w.println("    if (id_mhClass) {");
        w.println("        id_lookupMid = (*env)->GetStaticMethodID(env, id_mhClass, \"lookup\", \"()Ljava/lang/invoke/MethodHandles$Lookup;\");");
        w.println("        id_privateLookupInMid = (*env)->GetStaticMethodID(env, id_mhClass, \"privateLookupIn\",");
        w.println("            \"(Ljava/lang/Class;Ljava/lang/invoke/MethodHandles$Lookup;)Ljava/lang/invoke/MethodHandles$Lookup;\");");
        w.println("    }");
        w.println("    id_mtClass = vm_find_class(env, \"java/lang/invoke/MethodType\");");
        w.println("    if (id_mtClass) id_fromDescMid = (*env)->GetStaticMethodID(env, id_mtClass, \"fromMethodDescriptorString\",");
        w.println("        \"(Ljava/lang/String;Ljava/lang/ClassLoader;)Ljava/lang/invoke/MethodType;\");");
        w.println("    id_classClass = vm_find_class(env, \"java/lang/Class\");");
        w.println("    if (id_classClass) {");
        w.println("        id_getClassLoaderMid = (*env)->GetMethodID(env, id_classClass, \"getClassLoader\", \"()Ljava/lang/ClassLoader;\");");
        w.println("        id_forNameMid = (*env)->GetStaticMethodID(env, id_classClass, \"forName\", \"(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;\");");
        w.println("    }");
        w.println("    id_lookupClass = vm_find_class(env, \"java/lang/invoke/MethodHandles$Lookup\");");
        w.println("    if (id_lookupClass) {");
        w.println("        id_findStaticMid = (*env)->GetMethodID(env, id_lookupClass, \"findStatic\",");
        w.println("            \"(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;\");");
        w.println("        id_findVirtualMid = (*env)->GetMethodID(env, id_lookupClass, \"findVirtual\",");
        w.println("            \"(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;\");");
        w.println("        id_findSpecialMid = (*env)->GetMethodID(env, id_lookupClass, \"findSpecial\",");
        w.println("            \"(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;\");");
        w.println("        id_findConstructorMid = (*env)->GetMethodID(env, id_lookupClass, \"findConstructor\",");
        w.println("            \"(Ljava/lang/Class;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;\");");
        w.println("    }");
        w.println("    id_lmfClass = vm_find_class(env, \"java/lang/invoke/LambdaMetafactory\");");
        w.println("    if (id_lmfClass) {");
        w.println("        id_metafactoryMid = (*env)->GetStaticMethodID(env, id_lmfClass, \"metafactory\",");
        w.println("            \"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodType;Ljava/lang/invoke/MethodHandle;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;\");");
        w.println("        id_altMetafactoryMid = (*env)->GetStaticMethodID(env, id_lmfClass, \"altMetafactory\",");
        w.println("            \"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;\");");
        w.println("    }");
        // StringConcatFactory - may not exist on Java 8
        w.println("    id_scfClass = (*env)->FindClass(env, \"java/lang/invoke/StringConcatFactory\");");
        w.println("    if ((*env)->ExceptionCheck(env)) { (*env)->ExceptionClear(env); id_scfClass = NULL; }");
        w.println("    if (id_scfClass) {");
        w.println("        id_scfClass = (jclass)(*env)->NewGlobalRef(env, id_scfClass);");
        w.println("        id_makeConcatMid = (*env)->GetStaticMethodID(env, id_scfClass, \"makeConcat\",");
        w.println("            \"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/CallSite;\");");
        w.println("        if ((*env)->ExceptionCheck(env)) { (*env)->ExceptionClear(env); id_makeConcatMid = NULL; }");
        w.println("        id_makeConcatConstantsMid = (*env)->GetStaticMethodID(env, id_scfClass, \"makeConcatWithConstants\",");
        w.println("            \"(Ljava/lang/invoke/MethodHandles$Lookup;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/String;[Ljava/lang/Object;)Ljava/lang/invoke/CallSite;\");");
        w.println("        if ((*env)->ExceptionCheck(env)) { (*env)->ExceptionClear(env); id_makeConcatConstantsMid = NULL; }");
        w.println("    }");
        w.println("    id_callSiteClass = vm_find_class(env, \"java/lang/invoke/CallSite\");");
        w.println("    if (id_callSiteClass) id_getTargetMid = (*env)->GetMethodID(env, id_callSiteClass, \"getTarget\", \"()Ljava/lang/invoke/MethodHandle;\");");
        w.println("    jclass mhHandleClass = vm_find_class(env, \"java/lang/invoke/MethodHandle\");");
        w.println("    if (mhHandleClass) id_invokeWithArgsMid = (*env)->GetMethodID(env, mhHandleClass, \"invokeWithArguments\", \"([Ljava/lang/Object;)Ljava/lang/Object;\");");
        w.println("    id_objectClass = vm_find_class(env, \"java/lang/Object\");");
        w.println("    id_stringClass = vm_find_class(env, \"java/lang/String\");");
        w.println("    id_intClass = vm_find_class(env, \"java/lang/Integer\");");
        w.println("    id_longClass = vm_find_class(env, \"java/lang/Long\");");
        w.println("    id_floatClass = vm_find_class(env, \"java/lang/Float\");");
        w.println("    id_doubleClass = vm_find_class(env, \"java/lang/Double\");");
        w.println("    id_boolClass = vm_find_class(env, \"java/lang/Boolean\");");
        w.println("    id_byteClass = vm_find_class(env, \"java/lang/Byte\");");
        w.println("    id_shortClass = vm_find_class(env, \"java/lang/Short\");");
        w.println("    id_charClass = vm_find_class(env, \"java/lang/Character\");");
        w.println("    if (id_intClass) id_intValueOfMid = (*env)->GetStaticMethodID(env, id_intClass, \"valueOf\", \"(I)Ljava/lang/Integer;\");");
        w.println("    if (id_longClass) id_longValueOfMid = (*env)->GetStaticMethodID(env, id_longClass, \"valueOf\", \"(J)Ljava/lang/Long;\");");
        w.println("    if (id_floatClass) id_floatValueOfMid = (*env)->GetStaticMethodID(env, id_floatClass, \"valueOf\", \"(F)Ljava/lang/Float;\");");
        w.println("    if (id_doubleClass) id_doubleValueOfMid = (*env)->GetStaticMethodID(env, id_doubleClass, \"valueOf\", \"(D)Ljava/lang/Double;\");");
        w.println("    if (id_boolClass) id_boolValueOfMid = (*env)->GetStaticMethodID(env, id_boolClass, \"valueOf\", \"(Z)Ljava/lang/Boolean;\");");
        w.println("    if (id_byteClass) id_byteValueOfMid = (*env)->GetStaticMethodID(env, id_byteClass, \"valueOf\", \"(B)Ljava/lang/Byte;\");");
        w.println("    if (id_shortClass) id_shortValueOfMid = (*env)->GetStaticMethodID(env, id_shortClass, \"valueOf\", \"(S)Ljava/lang/Short;\");");
        w.println("    if (id_charClass) id_charValueOfMid = (*env)->GetStaticMethodID(env, id_charClass, \"valueOf\", \"(C)Ljava/lang/Character;\");");
        w.println("}");
        w.println();

        // Box a stack value to jobject
        w.println("static jobject vm_indy_box(JNIEnv* env, char type, VMValue val) {");
        w.println("    switch (type) {");
        w.println("        case 'I': return (*env)->CallStaticObjectMethod(env, id_intClass, id_intValueOfMid, val.i);");
        w.println("        case 'J': return (*env)->CallStaticObjectMethod(env, id_longClass, id_longValueOfMid, val.j);");
        w.println("        case 'F': return (*env)->CallStaticObjectMethod(env, id_floatClass, id_floatValueOfMid, val.f);");
        w.println("        case 'D': return (*env)->CallStaticObjectMethod(env, id_doubleClass, id_doubleValueOfMid, val.d);");
        w.println("        case 'Z': return (*env)->CallStaticObjectMethod(env, id_boolClass, id_boolValueOfMid, val.i);");
        w.println("        case 'B': return (*env)->CallStaticObjectMethod(env, id_byteClass, id_byteValueOfMid, (jbyte)val.i);");
        w.println("        case 'S': return (*env)->CallStaticObjectMethod(env, id_shortClass, id_shortValueOfMid, (jshort)val.i);");
        w.println("        case 'C': return (*env)->CallStaticObjectMethod(env, id_charClass, id_charValueOfMid, (jchar)val.i);");
        w.println("        default:  return val.l;");
        w.println("    }");
        w.println("}");
        w.println();

        // Parse descriptor argument types into array, return count
        w.println("static int vm_indy_parse_arg_types(const char* desc, char* outTypes, int maxTypes) {");
        w.println("    int count = 0;");
        w.println("    const char* p = desc + 1; // skip '('");
        w.println("    while (*p && *p != ')' && count < maxTypes) {");
        w.println("        if (*p == 'L') { outTypes[count++] = 'L'; while (*p && *p != ';') p++; if (*p) p++; }");
        w.println("        else if (*p == '[') { outTypes[count++] = 'L'; while (*p == '[') p++; if (*p == 'L') { while (*p && *p != ';') p++; } if (*p) p++; }");
        w.println("        else { outTypes[count++] = *p; p++; }");
        w.println("    }");
        w.println("    return count;");
        w.println("}");
        w.println();

        // Pop captured args from stack into boxed Object array (correct order)
        w.println("static jobjectArray vm_indy_pop_args(JNIEnv* env, VMFrame* frame, const char* desc, int capturedCount) {");
        w.println("    if (capturedCount == 0) return NULL;");
        w.println("    TMP_SAVE;  // Save for cleanup");
        w.println("    // Dynamic allocation for argTypes (each arg needs 1 char, plus safety margin)");
        w.println("    int argTypesSize = capturedCount + 8;");
        w.println("    char* argTypes = tmp_buf_alloc(argTypesSize);");
        w.println("    vm_indy_parse_arg_types(desc, argTypes, argTypesSize);");
        w.println("    jobjectArray arr = (*env)->NewObjectArray(env, capturedCount, id_objectClass, NULL);");
        w.println("    if (!arr) { TMP_RESTORE; return NULL; }");
        w.println("    // Pop from stack in reverse (top = last arg), store at correct position");
        w.println("    for (int i = capturedCount - 1; i >= 0; i--) {");
        w.println("        VMValue val = frame->stack[--frame->sp];");
        w.println("        jobject boxed = vm_indy_box(env, argTypes[i], val);");
        w.println("        (*env)->SetObjectArrayElement(env, arr, i, boxed);");
        w.println("    }");
        w.println("    TMP_RESTORE;  // Cleanup before return");
        w.println("    return arr;");
        w.println("}");
        w.println();
    }

    private void emitMainFunction(PrintWriter w) {
        w.println("jobject vm_invoke_dynamic(JNIEnv* env, VMFrame* frame, MetaEntry* meta) {");
        w.println("    TMP_SAVE;  // Save frame offset for automatic cleanup on return");
        w.println("    if (!meta) { VM_LOG(\"INVOKEDYNAMIC: meta is NULL\\n\"); TMP_RESTORE; return NULL; }");
        w.println("    vm_indy_init_cache(env);");
        w.println();
        w.println("    const char* methodName = vm_get_string(meta->nameIdx);");
        w.println("    const char* methodDesc = vm_get_string(meta->descIdx);");
        w.println("    VM_LOG(\"INVOKEDYNAMIC: name=%s, desc=%s, bsmIdx=%d\\n\", methodName, methodDesc, meta->bsmIdx);");
        w.println();

        // Calculate captured arg count from methodDesc - use dynamic allocation
        w.println("    int descLen = strlen(methodDesc);");
        w.println("    int capturedTypesSize = descLen + 8;  // at most 1 char per descriptor char");
        w.println("    char* capturedTypes = tmp_buf_alloc(capturedTypesSize);");
        w.println("    int capturedCount = vm_indy_parse_arg_types(methodDesc, capturedTypes, capturedTypesSize);");
        w.println("    if (frame->sp < capturedCount) {");
        w.println("        VM_LOG(\"INVOKEDYNAMIC: Stack underflow! sp=%d, need=%d\\n\", frame->sp, capturedCount);");
        w.println("        TMP_RESTORE; return NULL;");
        w.println("    }");
        w.println();

        // Check cache: if we already have a linked MethodHandle, just invoke it
        w.println("    if (meta->cachedIndyResult != NULL) {");
        w.println("        VM_LOG(\"INVOKEDYNAMIC: Using cached target\\n\");");
        w.println("        TMP_RESTORE;  // Restore before recursive call");
        w.println("        jobjectArray invokeArgs = vm_indy_pop_args(env, frame, methodDesc, capturedCount);");
        w.println("        jobject result = (*env)->CallObjectMethod(env, meta->cachedIndyResult, id_invokeWithArgsMid, invokeArgs);");
        w.println("        if ((*env)->ExceptionCheck(env)) return NULL;");
        w.println("        return result;");
        w.println("    }");
        w.println();

        // Validate BSM
        w.println("    if (meta->bsmIdx < 0 || meta->bsmIdx >= vm_bootstrap_count) {");
        w.println("        VM_LOG(\"INVOKEDYNAMIC: Invalid bsmIdx=%d\\n\", meta->bsmIdx); TMP_RESTORE; return NULL;");
        w.println("    }");
        w.println("    VMBootstrapMethod* bsm = &vm_bootstrap_methods[meta->bsmIdx];");
        w.println("    const char* bsmOwner = vm_get_string(bsm->ownerIdx);");
        w.println("    const char* bsmName = vm_get_string(bsm->nameIdx);");
        w.println("    VM_LOG(\"INVOKEDYNAMIC: BSM=%s.%s\\n\", bsmOwner, bsmName);");
        w.println();

        // Dispatch based on BSM class
        w.println("    jobject callSite = NULL;");
        w.println();

        // --- StringConcatFactory ---
        emitStringConcatPath(w);

        // --- LambdaMetafactory ---
        emitLambdaMetafactoryPath(w);

        // --- Fallback: unknown BSM ---
        w.println("    else {");
        w.println("        VM_LOG(\"INVOKEDYNAMIC: Unsupported BSM: %s.%s\\n\", bsmOwner, bsmName);");
        w.println("        // Pop captured args to keep stack balanced");
        w.println("        for (int i = 0; i < capturedCount; i++) frame->sp--;");
        w.println("        TMP_RESTORE; return NULL;");
        w.println("    }");
        w.println();

        // Get target MethodHandle from CallSite, cache it, invoke
        w.println("    if (!callSite) {");
        w.println("        VM_LOG(\"INVOKEDYNAMIC: CallSite is NULL\\n\");");
        w.println("        if ((*env)->ExceptionCheck(env)) { (*env)->ExceptionDescribe(env); (*env)->ExceptionClear(env); }");
        w.println("        for (int i = 0; i < capturedCount; i++) frame->sp--;");
        w.println("        TMP_RESTORE; return NULL;");
        w.println("    }");
        w.println();
        w.println("    jobject targetHandle = (*env)->CallObjectMethod(env, callSite, id_getTargetMid);");
        w.println("    if (!targetHandle) {");
        w.println("        VM_LOG(\"INVOKEDYNAMIC: getTarget returned NULL\\n\");");
        w.println("        (*env)->ExceptionClear(env);");
        w.println("        for (int i = 0; i < capturedCount; i++) frame->sp--;");
        w.println("        TMP_RESTORE; return NULL;");
        w.println("    }");
        w.println();
        // Cache the MethodHandle as global ref (thread-safe: if another thread raced, just discard ours)
        w.println("    jobject globalTarget = (*env)->NewGlobalRef(env, targetHandle);");
        w.println("    if (__sync_bool_compare_and_swap((void**)&meta->cachedIndyResult, NULL, globalTarget) == 0) {");
        w.println("        (*env)->DeleteGlobalRef(env, globalTarget);  // another thread won the race");
        w.println("    }");
        w.println();
        // Pop captured args and invoke
        w.println("    TMP_RESTORE;  // Restore before recursive call");
        w.println("    jobjectArray invokeArgs = vm_indy_pop_args(env, frame, methodDesc, capturedCount);");
        w.println("    jobject result = (*env)->CallObjectMethod(env, targetHandle, id_invokeWithArgsMid, invokeArgs);");
        w.println("    if ((*env)->ExceptionCheck(env)) return NULL;");
        w.println("    VM_LOG(\"INVOKEDYNAMIC: Success, result=%p\\n\", result);");
        w.println("    return result;");
        w.println("}");
        w.println();
    }

    private void emitStringConcatPath(PrintWriter w) {
        w.println("    if (id_scfClass && strcmp(bsmOwner, \"java/lang/invoke/StringConcatFactory\") == 0) {");
        w.println("        // StringConcatFactory path");
        w.println("        VM_LOG(\"INVOKEDYNAMIC: StringConcatFactory path, method=%s\\n\", bsmName);");
        w.println();
        // Create lookup
        w.println("        jclass callerClass = frame->callerClass;");
        w.println("        if (!callerClass) callerClass = (*env)->FindClass(env, \"java/lang/Object\");");
        w.println("        jobject publicLookup = (*env)->CallStaticObjectMethod(env, id_mhClass, id_lookupMid);");
        w.println("        jobject lookup = (*env)->CallStaticObjectMethod(env, id_mhClass, id_privateLookupInMid, callerClass, publicLookup);");
        w.println("        if (!lookup) { if ((*env)->ExceptionCheck(env)) (*env)->ExceptionClear(env); lookup = publicLookup; }");
        w.println();
        // Create invokedType (MethodType)
        w.println("        jobject classLoader = (*env)->CallObjectMethod(env, callerClass, id_getClassLoaderMid);");
        w.println("        jstring descStr = (*env)->NewStringUTF(env, methodDesc);");
        w.println("        jobject invokedType = (*env)->CallStaticObjectMethod(env, id_mtClass, id_fromDescMid, descStr, classLoader);");
        w.println("        if (!invokedType) { (*env)->ExceptionClear(env); TMP_RESTORE; return NULL; }");
        w.println("        jstring nameStr = (*env)->NewStringUTF(env, methodName);");
        w.println();
        w.println("        if (strcmp(bsmName, \"makeConcatWithConstants\") == 0 && id_makeConcatConstantsMid) {");
        w.println("            // makeConcatWithConstants(Lookup, String, MethodType, String recipe, Object... constants)");
        w.println("            if (bsm->argCount < 1) { VM_LOG(\"INVOKEDYNAMIC: SCF needs recipe arg\\n\"); TMP_RESTORE; return NULL; }");
        w.println("            const char* recipe = vm_get_string(bsm->args[0].strIdx);");
        w.println("            jstring recipeStr = (*env)->NewStringUTF(env, recipe);");
        w.println("            // Remaining BSM args are constants");
        w.println("            int constCount = bsm->argCount - 1;");
        w.println("            jobjectArray constants = (*env)->NewObjectArray(env, constCount, id_objectClass, NULL);");
        w.println("            for (int i = 0; i < constCount; i++) {");
        w.println("                BsmArg* a = &bsm->args[i + 1];");
        w.println("                jobject val = NULL;");
        w.println("                switch (a->type) {");
        w.println("                    case BSM_ARG_STRING: val = (*env)->NewStringUTF(env, vm_get_string(a->strIdx)); break;");
        w.println("                    case BSM_ARG_INTEGER: { VMValue v; v.i = a->intVal; val = vm_indy_box(env, 'I', v); break; }");
        w.println("                    case BSM_ARG_LONG: { VMValue v; v.j = a->longVal; val = vm_indy_box(env, 'J', v); break; }");
        w.println("                    case BSM_ARG_FLOAT: { VMValue v; v.f = a->floatVal; val = vm_indy_box(env, 'F', v); break; }");
        w.println("                    case BSM_ARG_DOUBLE: { VMValue v; v.d = a->doubleVal; val = vm_indy_box(env, 'D', v); break; }");
        w.println("                    default: break;");
        w.println("                }");
        w.println("                if (val) (*env)->SetObjectArrayElement(env, constants, i, val);");
        w.println("            }");
        w.println("            callSite = (*env)->CallStaticObjectMethod(env, id_scfClass, id_makeConcatConstantsMid,");
        w.println("                lookup, nameStr, invokedType, recipeStr, constants);");
        w.println("        } else if (strcmp(bsmName, \"makeConcat\") == 0 && id_makeConcatMid) {");
        w.println("            // makeConcat(Lookup, String, MethodType)");
        w.println("            callSite = (*env)->CallStaticObjectMethod(env, id_scfClass, id_makeConcatMid,");
        w.println("                lookup, nameStr, invokedType);");
        w.println("        } else {");
        w.println("            VM_LOG(\"INVOKEDYNAMIC: Unknown SCF method: %s\\n\", bsmName);");
        w.println("            TMP_RESTORE; return NULL;");
        w.println("        }");
        w.println("    }");
    }

    private void emitLambdaMetafactoryPath(PrintWriter w) {
        w.println("    else if (strcmp(bsmOwner, \"java/lang/invoke/LambdaMetafactory\") == 0) {");
        w.println("        // LambdaMetafactory path");
        w.println("        if (bsm->argCount < 3) {");
        w.println("            VM_LOG(\"INVOKEDYNAMIC: LMF needs >= 3 BSM args, got %d\\n\", bsm->argCount);");
        w.println("            TMP_RESTORE; return NULL;");
        w.println("        }");
        w.println();
        // Parse BSM args
        w.println("        const char* samMethodTypeStr = vm_get_string(bsm->args[0].strIdx);");
        w.println("        const char* implMethodStr = vm_get_string(bsm->args[1].strIdx);");
        w.println("        const char* instantiatedMethodTypeStr = vm_get_string(bsm->args[2].strIdx);");
        w.println("        int handleTag = bsm->args[1].handleTag;");
        w.println("        VM_LOG(\"INVOKEDYNAMIC: samType=%s, impl=%s, instType=%s, tag=%d\\n\",");
        w.println("            samMethodTypeStr, implMethodStr, instantiatedMethodTypeStr, handleTag);");
        w.println();
        // Parse implMethod: "owner.name(desc)" - dynamic allocation
        w.println("        int implMethodStrLen = strlen(implMethodStr);");
        w.println("        char* implOwner = tmp_buf_alloc(implMethodStrLen + 1);");
        w.println("        char* implName = tmp_buf_alloc(implMethodStrLen + 1);");
        w.println("        char* implDesc = tmp_buf_alloc(implMethodStrLen + 1);");
        w.println("        memset(implOwner, 0, implMethodStrLen + 1);");
        w.println("        memset(implName, 0, implMethodStrLen + 1);");
        w.println("        memset(implDesc, 0, implMethodStrLen + 1);");
        w.println("        const char* implParen = strchr(implMethodStr, '(');");
        w.println("        if (!implParen) { VM_LOG(\"INVOKEDYNAMIC: No paren in %s\\n\", implMethodStr); TMP_RESTORE; return NULL; }");
        w.println("        TMP_STRCPY(implDesc, implMethodStrLen + 1, implParen);");
        w.println("        const char* lastDot = NULL;");
        w.println("        for (const char* p = implMethodStr; p < implParen; p++) { if (*p == '.') lastDot = p; }");
        w.println("        if (!lastDot) { VM_LOG(\"INVOKEDYNAMIC: No dot in %s\\n\", implMethodStr); TMP_RESTORE; return NULL; }");
        w.println("        TMP_STRNCPY(implOwner, implMethodStr, lastDot - implMethodStr, implMethodStrLen + 1);");
        w.println("        TMP_STRNCPY(implName, lastDot + 1, implParen - lastDot - 1, implMethodStrLen + 1);");
        w.println();
        // Create Lookup
        w.println("        jclass ownerClass = (*env)->FindClass(env, implOwner);");
        w.println("        if (!ownerClass) { (*env)->ExceptionClear(env); VM_LOG(\"INVOKEDYNAMIC: Class not found: %s\\n\", implOwner); TMP_RESTORE; return NULL; }");
        w.println("        jobject publicLookup = (*env)->CallStaticObjectMethod(env, id_mhClass, id_lookupMid);");
        w.println("        jobject lookup = (*env)->CallStaticObjectMethod(env, id_mhClass, id_privateLookupInMid, ownerClass, publicLookup);");
        w.println("        if (!lookup) { if ((*env)->ExceptionCheck(env)) (*env)->ExceptionClear(env); lookup = publicLookup; }");
        w.println();
        // Create MethodType objects
        w.println("        jobject classLoader = (*env)->CallObjectMethod(env, ownerClass, id_getClassLoaderMid);");
        w.println("        jstring samDescStr = (*env)->NewStringUTF(env, samMethodTypeStr);");
        w.println("        jobject samMethodType = (*env)->CallStaticObjectMethod(env, id_mtClass, id_fromDescMid, samDescStr, classLoader);");
        w.println("        if (!samMethodType) { (*env)->ExceptionClear(env); TMP_RESTORE; return NULL; }");
        w.println("        jstring implDescStr = (*env)->NewStringUTF(env, implDesc);");
        w.println("        jobject implMethodType = (*env)->CallStaticObjectMethod(env, id_mtClass, id_fromDescMid, implDescStr, classLoader);");
        w.println("        if (!implMethodType) { (*env)->ExceptionClear(env); TMP_RESTORE; return NULL; }");
        w.println("        jstring instDescStr = (*env)->NewStringUTF(env, instantiatedMethodTypeStr);");
        w.println("        jobject instantiatedMethodType = (*env)->CallStaticObjectMethod(env, id_mtClass, id_fromDescMid, instDescStr, classLoader);");
        w.println("        if (!instantiatedMethodType) { (*env)->ExceptionClear(env); TMP_RESTORE; return NULL; }");
        w.println();
        // Find impl MethodHandle based on handleTag
        w.println("        jobject implMethodHandle = NULL;");
        w.println("        jstring implNameStr = (*env)->NewStringUTF(env, implName);");
        w.println("        if (handleTag == 6) { // REF_invokeStatic");
        w.println("            implMethodHandle = (*env)->CallObjectMethod(env, lookup, id_findStaticMid, ownerClass, implNameStr, implMethodType);");
        w.println("        } else if (handleTag == 5 || handleTag == 9) { // REF_invokeVirtual || REF_invokeInterface");
        w.println("            implMethodHandle = (*env)->CallObjectMethod(env, lookup, id_findVirtualMid, ownerClass, implNameStr, implMethodType);");
        w.println("        } else if (handleTag == 7) { // REF_invokeSpecial");
        w.println("            implMethodHandle = (*env)->CallObjectMethod(env, lookup, id_findSpecialMid, ownerClass, implNameStr, implMethodType, ownerClass);");
        w.println("        } else if (handleTag == 8) { // REF_newInvokeSpecial (constructor reference)");
        w.println("            implMethodHandle = (*env)->CallObjectMethod(env, lookup, id_findConstructorMid, ownerClass, implMethodType);");
        w.println("        } else {");
        w.println("            VM_LOG(\"INVOKEDYNAMIC: Unsupported handleTag=%d\\n\", handleTag);");
        w.println("            TMP_RESTORE; return NULL;");
        w.println("        }");
        w.println("        if (!implMethodHandle) {");
        w.println("            VM_LOG(\"INVOKEDYNAMIC: MethodHandle not found for %s.%s tag=%d\\n\", implOwner, implName, handleTag);");
        w.println("            if ((*env)->ExceptionCheck(env)) { (*env)->ExceptionDescribe(env); (*env)->ExceptionClear(env); }");
        w.println("            TMP_RESTORE; return NULL;");
        w.println("        }");
        w.println();
        // Create invokedType
        w.println("        jstring methodDescStr = (*env)->NewStringUTF(env, methodDesc);");
        w.println("        jobject invokedType = (*env)->CallStaticObjectMethod(env, id_mtClass, id_fromDescMid, methodDescStr, classLoader);");
        w.println("        if (!invokedType) { (*env)->ExceptionClear(env); TMP_RESTORE; return NULL; }");
        w.println("        jstring methodNameStr = (*env)->NewStringUTF(env, methodName);");
        w.println();
        // Call metafactory or altMetafactory
        w.println("        if (strcmp(bsmName, \"metafactory\") == 0) {");
        w.println("            callSite = (*env)->CallStaticObjectMethod(env, id_lmfClass, id_metafactoryMid,");
        w.println("                lookup, methodNameStr, invokedType, samMethodType, implMethodHandle, instantiatedMethodType);");
        w.println("        } else if (strcmp(bsmName, \"altMetafactory\") == 0 && id_altMetafactoryMid) {");
        w.println("            // altMetafactory(Lookup, String, MethodType, Object...)");
        w.println("            // Pack all BSM args: samMethodType, implMethodHandle, instantiatedMethodType, flags, ...extra");
        w.println("            jobjectArray altArgs = (*env)->NewObjectArray(env, bsm->argCount, id_objectClass, NULL);");
        w.println("            (*env)->SetObjectArrayElement(env, altArgs, 0, samMethodType);");
        w.println("            (*env)->SetObjectArrayElement(env, altArgs, 1, implMethodHandle);");
        w.println("            (*env)->SetObjectArrayElement(env, altArgs, 2, instantiatedMethodType);");
        w.println("            // Remaining args (flags, marker interfaces, bridges)");
        w.println("            for (int i = 3; i < bsm->argCount; i++) {");
        w.println("                BsmArg* a = &bsm->args[i];");
        w.println("                jobject val = NULL;");
        w.println("                switch (a->type) {");
        w.println("                    case BSM_ARG_INTEGER: { VMValue v; v.i = a->intVal; val = vm_indy_box(env, 'I', v); break; }");
        w.println("                    case BSM_ARG_STRING: val = (*env)->NewStringUTF(env, vm_get_string(a->strIdx)); break;");
        w.println("                    case BSM_ARG_METHOD_TYPE: {");
        w.println("                        jstring mtStr = (*env)->NewStringUTF(env, vm_get_string(a->strIdx));");
        w.println("                        val = (*env)->CallStaticObjectMethod(env, id_mtClass, id_fromDescMid, mtStr, classLoader);");
        w.println("                        break;");
        w.println("                    }");
        w.println("                    case BSM_ARG_CLASS: {");
        w.println("                        // Class reference - convert internal name to Class object");
        w.println("                        // Internal name uses '/', but Class.forName needs '.'");
        w.println("                        const char* internalName = vm_get_string(a->strIdx);");
        w.println("                        int internalNameLen = strlen(internalName);");
        w.println("                        char* javaName = tmp_buf_alloc(internalNameLen + 1);");
        w.println("                        int j = 0;");
        w.println("                        for (const char* p = internalName; *p && j < internalNameLen; p++, j++) {");
        w.println("                            javaName[j] = (*p == '/') ? '.' : *p;");
        w.println("                        }");
        w.println("                        javaName[j] = '\\0';");
        w.println("                        jstring clsNameStr = (*env)->NewStringUTF(env, javaName);");
        w.println("                        // No tmp_buf_free needed - TMP_RESTORE will clean up");
        w.println("                        val = (*env)->CallStaticObjectMethod(env, id_classClass, id_forNameMid, clsNameStr, JNI_TRUE, classLoader);");
        w.println("                        break;");
        w.println("                    }");
        w.println("                    default: break;");
        w.println("                }");
        w.println("                if (val) (*env)->SetObjectArrayElement(env, altArgs, i, val);");
        w.println("            }");
        w.println("            callSite = (*env)->CallStaticObjectMethod(env, id_lmfClass, id_altMetafactoryMid,");
        w.println("                lookup, methodNameStr, invokedType, altArgs);");
        w.println("        } else {");
        w.println("            VM_LOG(\"INVOKEDYNAMIC: Unknown LMF method: %s\\n\", bsmName);");
        w.println("            TMP_RESTORE; return NULL;");
        w.println("        }");
        w.println("    }");
    }
}
