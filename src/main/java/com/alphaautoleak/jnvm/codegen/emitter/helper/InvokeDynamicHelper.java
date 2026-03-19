package com.alphaautoleak.jnvm.codegen.emitter.helper;

import java.io.PrintWriter;

/**
 * InvokeDynamic helper functions
 * Generic bootstrap invocation with JDK 8~17+ compatibility.
 */
public class InvokeDynamicHelper extends VMHelper {
    private static final String[] INDY_BOX_CASE_LINES = {
            "        case 'I': cls = vm_find_class(env, \"java/lang/Integer\"); if (cls) mid = vm_get_static_method_id(env, cls, \"java/lang/Integer\", \"valueOf\", \"(I)Ljava/lang/Integer;\"); if (mid) return (*env)->CallStaticObjectMethod(env, cls, mid, val.i); break;",
            "        case 'J': cls = vm_find_class(env, \"java/lang/Long\"); if (cls) mid = vm_get_static_method_id(env, cls, \"java/lang/Long\", \"valueOf\", \"(J)Ljava/lang/Long;\"); if (mid) return (*env)->CallStaticObjectMethod(env, cls, mid, val.j); break;",
            "        case 'F': cls = vm_find_class(env, \"java/lang/Float\"); if (cls) mid = vm_get_static_method_id(env, cls, \"java/lang/Float\", \"valueOf\", \"(F)Ljava/lang/Float;\"); if (mid) return (*env)->CallStaticObjectMethod(env, cls, mid, val.f); break;",
            "        case 'D': cls = vm_find_class(env, \"java/lang/Double\"); if (cls) mid = vm_get_static_method_id(env, cls, \"java/lang/Double\", \"valueOf\", \"(D)Ljava/lang/Double;\"); if (mid) return (*env)->CallStaticObjectMethod(env, cls, mid, val.d); break;",
            "        case 'Z': cls = vm_find_class(env, \"java/lang/Boolean\"); if (cls) mid = vm_get_static_method_id(env, cls, \"java/lang/Boolean\", \"valueOf\", \"(Z)Ljava/lang/Boolean;\"); if (mid) return (*env)->CallStaticObjectMethod(env, cls, mid, val.i); break;",
            "        case 'B': cls = vm_find_class(env, \"java/lang/Byte\"); if (cls) mid = vm_get_static_method_id(env, cls, \"java/lang/Byte\", \"valueOf\", \"(B)Ljava/lang/Byte;\"); if (mid) return (*env)->CallStaticObjectMethod(env, cls, mid, (jbyte)val.i); break;",
            "        case 'S': cls = vm_find_class(env, \"java/lang/Short\"); if (cls) mid = vm_get_static_method_id(env, cls, \"java/lang/Short\", \"valueOf\", \"(S)Ljava/lang/Short;\"); if (mid) return (*env)->CallStaticObjectMethod(env, cls, mid, (jshort)val.i); break;",
            "        case 'C': cls = vm_find_class(env, \"java/lang/Character\"); if (cls) mid = vm_get_static_method_id(env, cls, \"java/lang/Character\", \"valueOf\", \"(C)Ljava/lang/Character;\"); if (mid) return (*env)->CallStaticObjectMethod(env, cls, mid, (jchar)val.i); break;"
    };

    private static final String[] INDY_UNBOX_CASE_LINES = {
            "        case 'I': cls = vm_find_class(env, \"java/lang/Integer\"); if (cls) mid = vm_get_method_id(env, cls, \"java/lang/Integer\", \"intValue\", \"()I\"); if (mid) frame->stack[frame->sp++].i = (*env)->CallIntMethod(env, result, mid); break;",
            "        case 'Z': cls = vm_find_class(env, \"java/lang/Boolean\"); if (cls) mid = vm_get_method_id(env, cls, \"java/lang/Boolean\", \"booleanValue\", \"()Z\"); if (mid) frame->stack[frame->sp++].i = (*env)->CallBooleanMethod(env, result, mid); break;",
            "        case 'B': cls = vm_find_class(env, \"java/lang/Byte\"); if (cls) mid = vm_get_method_id(env, cls, \"java/lang/Byte\", \"byteValue\", \"()B\"); if (mid) frame->stack[frame->sp++].i = (*env)->CallByteMethod(env, result, mid); break;",
            "        case 'S': cls = vm_find_class(env, \"java/lang/Short\"); if (cls) mid = vm_get_method_id(env, cls, \"java/lang/Short\", \"shortValue\", \"()S\"); if (mid) frame->stack[frame->sp++].i = (*env)->CallShortMethod(env, result, mid); break;",
            "        case 'C': cls = vm_find_class(env, \"java/lang/Character\"); if (cls) mid = vm_get_method_id(env, cls, \"java/lang/Character\", \"charValue\", \"()C\"); if (mid) frame->stack[frame->sp++].i = (*env)->CallCharMethod(env, result, mid); break;",
            "        case 'J': cls = vm_find_class(env, \"java/lang/Long\"); if (cls) mid = vm_get_method_id(env, cls, \"java/lang/Long\", \"longValue\", \"()J\"); if (mid) frame->stack[frame->sp++].j = (*env)->CallLongMethod(env, result, mid); break;",
            "        case 'F': cls = vm_find_class(env, \"java/lang/Float\"); if (cls) mid = vm_get_method_id(env, cls, \"java/lang/Float\", \"floatValue\", \"()F\"); if (mid) frame->stack[frame->sp++].f = (*env)->CallFloatMethod(env, result, mid); break;",
            "        case 'D': cls = vm_find_class(env, \"java/lang/Double\"); if (cls) mid = vm_get_method_id(env, cls, \"java/lang/Double\", \"doubleValue\", \"()D\"); if (mid) frame->stack[frame->sp++].d = (*env)->CallDoubleMethod(env, result, mid); break;"
    };

    @Override
    public String[] getIncludes() {
        return new String[] { "vm_types.h", "vm_data.h", "<jni.h>", "<string.h>", "<stdio.h>", "<stdlib.h>" };
    }

    @Override
    public void generateHeader(PrintWriter w) {
        w.println("jobject vm_invoke_dynamic(JNIEnv* env, VMFrame* frame, MetaEntry* meta);");
        w.println("int vm_indy_push_return(JNIEnv* env, VMFrame* frame, MetaEntry* meta, jobject result);");
    }

    @Override
    public void generateSource(PrintWriter w) {
        emitStaticCache(w);
        emitHelperFunctions(w);
        emitMainFunction(w);
    }

    private void emitLines(PrintWriter w, String[] lines) {
        for (String line : lines) {
            w.println(line);
        }
    }

    private void emitStaticCache(PrintWriter w) {
        w.println("// === InvokeDynamic static cache ===");
        w.println("static jclass id_mhClass = NULL;");
        w.println("static jclass id_mhHandleClass = NULL;");
        w.println("static jclass id_lookupClass = NULL;");
        w.println("static jclass id_mtClass = NULL;");
        w.println("static jclass id_classClass = NULL;");
        w.println("static jclass id_callSiteClass = NULL;");
        w.println("static jclass id_objectClass = NULL;");
        w.println();
        w.println("static jmethodID id_lookupMid = NULL;");
        w.println("static jmethodID id_privateLookupInMid = NULL;");
        w.println("static jmethodID id_lookupInMid = NULL;");
        w.println("static jmethodID id_invokeWithArgsMid = NULL;");
        w.println("static jmethodID id_fromDescMid = NULL;");
        w.println("static jmethodID id_mtReturnTypeMid = NULL;");
        w.println("static jmethodID id_getClassLoaderMid = NULL;");
        w.println("static jmethodID id_getTargetMid = NULL;");
        w.println("static jmethodID id_findStaticMid = NULL;");
        w.println("static jmethodID id_findVirtualMid = NULL;");
        w.println("static jmethodID id_findSpecialMid = NULL;");
        w.println("static jmethodID id_findConstructorMid = NULL;");
        w.println("static jmethodID id_findGetterMid = NULL;");
        w.println("static jmethodID id_findStaticGetterMid = NULL;");
        w.println("static jmethodID id_findSetterMid = NULL;");
        w.println("static jmethodID id_findStaticSetterMid = NULL;");
        w.println();
        w.println("static int id_indy_initialized = 0;");
        w.println();
    }

    private void emitHelperFunctions(PrintWriter w) {
        emitHelperPart1(w);
        emitHelperPart2(w);
        emitHelperPart3(w);
        emitHelperPart4(w);
    }

    private void emitHelperPart1(PrintWriter w) {
        w.println("static void vm_indy_clear_if_exception(JNIEnv* env) {");
        w.println("    if ((*env)->ExceptionCheck(env)) (*env)->ExceptionClear(env);");
        w.println("}");
        w.println();

        w.println("static void vm_indy_throw_bsm_error(JNIEnv* env, const char* msg) {");
        w.println("    jclass cls = vm_find_class(env, \"java/lang/BootstrapMethodError\");");
        w.println("    if (!cls) cls = vm_find_class(env, \"java/lang/LinkageError\");");
        w.println("    if (cls) (*env)->ThrowNew(env, cls, msg ? msg : \"Bootstrap method linkage failed\");");
        w.println("}");
        w.println();

        w.println("static void vm_indy_init_cache(JNIEnv* env) {");
        w.println("    if (id_indy_initialized) return;");
        w.println("    id_indy_initialized = 1;");
        w.println();
        w.println("    id_mhClass = vm_find_class(env, \"java/lang/invoke/MethodHandles\");");
        w.println("    id_mhHandleClass = vm_find_class(env, \"java/lang/invoke/MethodHandle\");");
        w.println("    id_lookupClass = vm_find_class(env, \"java/lang/invoke/MethodHandles$Lookup\");");
        w.println("    id_mtClass = vm_find_class(env, \"java/lang/invoke/MethodType\");");
        w.println("    id_classClass = vm_find_class(env, \"java/lang/Class\");");
        w.println("    id_callSiteClass = vm_find_class(env, \"java/lang/invoke/CallSite\");");
        w.println("    id_objectClass = vm_find_class(env, \"java/lang/Object\");");
        w.println();
        w.println("    if (id_mhClass) {");
        w.println("        id_lookupMid = (*env)->GetStaticMethodID(env, id_mhClass, \"lookup\", \"()Ljava/lang/invoke/MethodHandles$Lookup;\");");
        w.println("        vm_indy_clear_if_exception(env);");
        w.println("        id_privateLookupInMid = (*env)->GetStaticMethodID(env, id_mhClass, \"privateLookupIn\",");
        w.println("            \"(Ljava/lang/Class;Ljava/lang/invoke/MethodHandles$Lookup;)Ljava/lang/invoke/MethodHandles$Lookup;\");");
        w.println("        vm_indy_clear_if_exception(env);");
        w.println("    }");
        w.println("    if (id_mhHandleClass) {");
        w.println("        id_invokeWithArgsMid = (*env)->GetMethodID(env, id_mhHandleClass, \"invokeWithArguments\", \"([Ljava/lang/Object;)Ljava/lang/Object;\");");
        w.println("        vm_indy_clear_if_exception(env);");
        w.println("    }");
        w.println("    if (id_lookupClass) {");
        w.println("        id_lookupInMid = (*env)->GetMethodID(env, id_lookupClass, \"in\", \"(Ljava/lang/Class;)Ljava/lang/invoke/MethodHandles$Lookup;\");");
        w.println("        vm_indy_clear_if_exception(env);");
        w.println("        id_findStaticMid = (*env)->GetMethodID(env, id_lookupClass, \"findStatic\",");
        w.println("            \"(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;\");");
        w.println("        vm_indy_clear_if_exception(env);");
        w.println("        id_findVirtualMid = (*env)->GetMethodID(env, id_lookupClass, \"findVirtual\",");
        w.println("            \"(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;\");");
        w.println("        vm_indy_clear_if_exception(env);");
        w.println("        id_findSpecialMid = (*env)->GetMethodID(env, id_lookupClass, \"findSpecial\",");
        w.println("            \"(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/invoke/MethodType;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;\");");
        w.println("        vm_indy_clear_if_exception(env);");
        w.println("        id_findConstructorMid = (*env)->GetMethodID(env, id_lookupClass, \"findConstructor\",");
        w.println("            \"(Ljava/lang/Class;Ljava/lang/invoke/MethodType;)Ljava/lang/invoke/MethodHandle;\");");
        w.println("        vm_indy_clear_if_exception(env);");
        w.println("        id_findGetterMid = (*env)->GetMethodID(env, id_lookupClass, \"findGetter\",");
        w.println("            \"(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;\");");
        w.println("        vm_indy_clear_if_exception(env);");
        w.println("        id_findStaticGetterMid = (*env)->GetMethodID(env, id_lookupClass, \"findStaticGetter\",");
        w.println("            \"(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;\");");
        w.println("        vm_indy_clear_if_exception(env);");
        w.println("        id_findSetterMid = (*env)->GetMethodID(env, id_lookupClass, \"findSetter\",");
        w.println("            \"(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;\");");
        w.println("        vm_indy_clear_if_exception(env);");
        w.println("        id_findStaticSetterMid = (*env)->GetMethodID(env, id_lookupClass, \"findStaticSetter\",");
        w.println("            \"(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/Class;)Ljava/lang/invoke/MethodHandle;\");");
        w.println("        vm_indy_clear_if_exception(env);");
        w.println("    }");
        w.println("    if (id_mtClass) {");
        w.println("        id_fromDescMid = (*env)->GetStaticMethodID(env, id_mtClass, \"fromMethodDescriptorString\",");
        w.println("            \"(Ljava/lang/String;Ljava/lang/ClassLoader;)Ljava/lang/invoke/MethodType;\");");
        w.println("        vm_indy_clear_if_exception(env);");
        w.println("        id_mtReturnTypeMid = (*env)->GetMethodID(env, id_mtClass, \"returnType\", \"()Ljava/lang/Class;\");");
        w.println("        vm_indy_clear_if_exception(env);");
        w.println("    }");
        w.println("    if (id_classClass) {");
        w.println("        id_getClassLoaderMid = (*env)->GetMethodID(env, id_classClass, \"getClassLoader\", \"()Ljava/lang/ClassLoader;\");");
        w.println("        vm_indy_clear_if_exception(env);");
        w.println("    }");
        w.println("    if (id_callSiteClass) {");
        w.println("        id_getTargetMid = (*env)->GetMethodID(env, id_callSiteClass, \"getTarget\", \"()Ljava/lang/invoke/MethodHandle;\");");
        w.println("        vm_indy_clear_if_exception(env);");
        w.println("    }");
        w.println("}");
        w.println();
    }

    private void emitHelperPart2(PrintWriter w) {
        w.println("static jobject vm_indy_box(JNIEnv* env, char type, VMValue val) {");
        w.println("    jclass cls = NULL; jmethodID mid = NULL;");
        w.println("    switch (type) {");
        emitLines(w, INDY_BOX_CASE_LINES);
        w.println("        default: return val.l;");
        w.println("    }");
        w.println("    return NULL;");
        w.println("}");
        w.println();

        w.println("static jobjectArray vm_indy_pop_args(JNIEnv* env, VMFrame* frame, const char* argTypes, int capturedCount) {");
        w.println("    if (capturedCount <= 0) return (*env)->NewObjectArray(env, 0, id_objectClass, NULL);");
        w.println("    jobjectArray arr = (*env)->NewObjectArray(env, capturedCount, id_objectClass, NULL);");
        w.println("    if (!arr) { return NULL; }");
        w.println("    for (int i = capturedCount - 1; i >= 0; i--) {");
        w.println("        VMValue val = frame->stack[--frame->sp];");
        w.println("        char t = argTypes ? argTypes[i] : 'L';");
        w.println("        jobject boxed = vm_indy_box(env, t, val);");
        w.println("        if ((*env)->ExceptionCheck(env)) { return NULL; }");
        w.println("        (*env)->SetObjectArrayElement(env, arr, i, boxed);");
        w.println("        if ((*env)->ExceptionCheck(env)) { return NULL; }");
        w.println("    }");
        w.println("    return arr;");
        w.println("}");
        w.println();

        w.println("static jobject vm_indy_get_lookup(JNIEnv* env, jclass callerClass) {");
        w.println("    if (!id_mhClass || !id_lookupMid) return NULL;");
        w.println("    jobject publicLookup = (*env)->CallStaticObjectMethod(env, id_mhClass, id_lookupMid);");
        w.println("    if ((*env)->ExceptionCheck(env)) return NULL;");
        w.println("    if (!callerClass) return publicLookup;");
        w.println("    if (id_privateLookupInMid) {");
        w.println("        jobject privateLookup = (*env)->CallStaticObjectMethod(env, id_mhClass, id_privateLookupInMid, callerClass, publicLookup);");
        w.println("        if (!(*env)->ExceptionCheck(env) && privateLookup) return privateLookup;");
        w.println("        vm_indy_clear_if_exception(env);");
        w.println("    }");
        w.println("    if (id_lookupInMid) {");
        w.println("        jobject inLookup = (*env)->CallObjectMethod(env, publicLookup, id_lookupInMid, callerClass);");
        w.println("        if (!(*env)->ExceptionCheck(env) && inLookup) return inLookup;");
        w.println("        vm_indy_clear_if_exception(env);");
        w.println("    }");
        w.println("    return publicLookup;");
        w.println("}");
        w.println();

        w.println("static jobject vm_indy_method_type(JNIEnv* env, const char* methodDesc, jobject classLoader) {");
        w.println("    if (!id_mtClass || !id_fromDescMid || !methodDesc) return NULL;");
        w.println("    jstring desc = (*env)->NewStringUTF(env, methodDesc);");
        w.println("    if (!desc) return NULL;");
        w.println("    jobject mt = (*env)->CallStaticObjectMethod(env, id_mtClass, id_fromDescMid, desc, classLoader);");
        w.println("    if ((*env)->ExceptionCheck(env)) return NULL;");
        w.println("    return mt;");
        w.println("}");
        w.println();

        w.println("static jobject vm_indy_class_from_type_desc(JNIEnv* env, const char* typeDesc, jobject classLoader) {");
        w.println("    if (!typeDesc || !id_mtReturnTypeMid) return NULL;");
        w.println("    TMP_SAVE;");
        w.println("    int len = (int)strlen(typeDesc);");
        w.println("    char* methodDesc = tmp_buf_alloc(len + 4);");
        w.println("    methodDesc[0] = '(';");
        w.println("    methodDesc[1] = ')';");
        w.println("    memcpy(methodDesc + 2, typeDesc, len);");
        w.println("    methodDesc[len + 2] = '\\0';");
        w.println("    jobject mt = vm_indy_method_type(env, methodDesc, classLoader);");
        w.println("    if (!mt) { TMP_RESTORE; return NULL; }");
        w.println("    jobject cls = (*env)->CallObjectMethod(env, mt, id_mtReturnTypeMid);");
        w.println("    if ((*env)->ExceptionCheck(env)) { TMP_RESTORE; return NULL; }");
        w.println("    TMP_RESTORE;");
        w.println("    return cls;");
        w.println("}");
        w.println();

        w.println("static jobject vm_indy_owner_class(JNIEnv* env, const char* ownerInternal, jobject classLoader) {");
        w.println("    if (!ownerInternal) return NULL;");
        w.println("    TMP_SAVE;");
        w.println("    int len = (int)strlen(ownerInternal);");
        w.println("    char* typeDesc = tmp_buf_alloc(len + 3);");
        w.println("    typeDesc[0] = 'L';");
        w.println("    memcpy(typeDesc + 1, ownerInternal, len);");
        w.println("    typeDesc[len + 1] = ';';");
        w.println("    typeDesc[len + 2] = '\\0';");
        w.println("    jobject cls = vm_indy_class_from_type_desc(env, typeDesc, classLoader);");
        w.println("    if (!cls) {");
        w.println("        vm_indy_clear_if_exception(env);");
        w.println("        cls = vm_find_class(env, ownerInternal);");
        w.println("    }");
        w.println("    TMP_RESTORE;");
        w.println("    return cls;");
        w.println("}");
        w.println();
    }

    private void emitHelperPart3(PrintWriter w) {
        w.println("static jobject vm_indy_resolve_handle(JNIEnv* env, jobject lookup, int tag, const char* owner, const char* name, const char* desc, jobject classLoader) {");
        w.println("    if (!lookup || !owner || !name || !desc) return NULL;");
        w.println("    jobject ownerClass = vm_indy_owner_class(env, owner, classLoader);");
        w.println("    if (!ownerClass) return NULL;");
        w.println("    jstring nameStr = (*env)->NewStringUTF(env, name);");
        w.println("    if (!nameStr) return NULL;");
        w.println("    if (desc[0] == '(') {");
        w.println("        jobject methodType = vm_indy_method_type(env, desc, classLoader);");
        w.println("        if (!methodType) return NULL;");
        w.println("        jobject mh = NULL;");
        w.println("        switch (tag) {");
        w.println("            case 6: if (id_findStaticMid) mh = (*env)->CallObjectMethod(env, lookup, id_findStaticMid, ownerClass, nameStr, methodType); break;");
        w.println("            case 5: case 9: if (id_findVirtualMid) mh = (*env)->CallObjectMethod(env, lookup, id_findVirtualMid, ownerClass, nameStr, methodType); break;");
        w.println("            case 7: if (id_findSpecialMid) mh = (*env)->CallObjectMethod(env, lookup, id_findSpecialMid, ownerClass, nameStr, methodType, ownerClass); break;");
        w.println("            case 8: if (id_findConstructorMid) mh = (*env)->CallObjectMethod(env, lookup, id_findConstructorMid, ownerClass, methodType); break;");
        w.println("            default: break;");
        w.println("        }");
        w.println("        if ((*env)->ExceptionCheck(env)) return NULL;");
        w.println("        return mh;");
        w.println("    }");
        w.println("    jobject fieldType = vm_indy_class_from_type_desc(env, desc, classLoader);");
        w.println("    if (!fieldType) return NULL;");
        w.println("    jobject mh = NULL;");
        w.println("    switch (tag) {");
        w.println("        case 1: if (id_findGetterMid) mh = (*env)->CallObjectMethod(env, lookup, id_findGetterMid, ownerClass, nameStr, fieldType); break;");
        w.println("        case 2: if (id_findStaticGetterMid) mh = (*env)->CallObjectMethod(env, lookup, id_findStaticGetterMid, ownerClass, nameStr, fieldType); break;");
        w.println("        case 3: if (id_findSetterMid) mh = (*env)->CallObjectMethod(env, lookup, id_findSetterMid, ownerClass, nameStr, fieldType); break;");
        w.println("        case 4: if (id_findStaticSetterMid) mh = (*env)->CallObjectMethod(env, lookup, id_findStaticSetterMid, ownerClass, nameStr, fieldType); break;");
        w.println("        default: break;");
        w.println("    }");
        w.println("    if ((*env)->ExceptionCheck(env)) return NULL;");
        w.println("    return mh;");
        w.println("}");
        w.println();

        w.println("static jobject vm_indy_convert_bsm_arg(JNIEnv* env, jobject lookup, BsmArg* a, jobject classLoader) {");
        w.println("    if (!a) return NULL;");
        w.println("    switch (a->type) {");
        w.println("        case BSM_ARG_STRING: return (*env)->NewStringUTF(env, vm_get_string(a->strIdx));");
        w.println("        case BSM_ARG_INTEGER: { VMValue v; v.i = a->intVal; return vm_indy_box(env, 'I', v); }");
        w.println("        case BSM_ARG_LONG: { VMValue v; v.j = a->longVal; return vm_indy_box(env, 'J', v); }");
        w.println("        case BSM_ARG_FLOAT: { VMValue v; v.f = a->floatVal; return vm_indy_box(env, 'F', v); }");
        w.println("        case BSM_ARG_DOUBLE: { VMValue v; v.d = a->doubleVal; return vm_indy_box(env, 'D', v); }");
        w.println("        case BSM_ARG_METHOD_TYPE: return vm_indy_method_type(env, vm_get_string(a->strIdx), classLoader);");
        w.println("        case BSM_ARG_CLASS: {");
        w.println("            const char* className = vm_get_string(a->strIdx);");
        w.println("            if (!className) return NULL;");
        w.println("            TMP_SAVE;");
        w.println("            int len = (int)strlen(className);");
        w.println("            const char* descToUse = NULL;");
        w.println("            if (len == 1 || className[0] == '[' || className[0] == 'L') {");
        w.println("                descToUse = className;");
        w.println("            } else {");
        w.println("                char* clsDesc = tmp_buf_alloc(len + 3);");
        w.println("                clsDesc[0] = 'L';");
        w.println("                memcpy(clsDesc + 1, className, len);");
        w.println("                clsDesc[len + 1] = ';';");
        w.println("                clsDesc[len + 2] = '\\0';");
        w.println("                descToUse = clsDesc;");
        w.println("            }");
        w.println("            jobject cls = vm_indy_class_from_type_desc(env, descToUse, classLoader);");
        w.println("            TMP_RESTORE;");
        w.println("            return cls;");
        w.println("        }");
        w.println("        case BSM_ARG_METHOD_HANDLE: {");
        w.println("            const char* owner = vm_get_string(a->ownerIdx);");
        w.println("            const char* name = vm_get_string(a->nameIdx);");
        w.println("            const char* desc = vm_get_string(a->descIdx);");
        w.println("            return vm_indy_resolve_handle(env, lookup, a->handleTag, owner, name, desc, classLoader);");
        w.println("        }");
        w.println("        default: return NULL;");
        w.println("    }");
        w.println("}");
        w.println();
    }

    private void emitHelperPart4(PrintWriter w) {
        w.println("int vm_indy_push_return(JNIEnv* env, VMFrame* frame, MetaEntry* meta, jobject result) {");
        w.println("    if (!frame || !meta) return 0;");
        w.println("    if (meta->returnTypeChar == 'V') return 1;");
        w.println("    if (meta->returnTypeChar == 'L' || meta->returnTypeChar == '[') {");
        w.println("        frame->stack[frame->sp++].l = result;");
        w.println("        return 1;");
        w.println("    }");
        w.println("    if (!result) {");
        w.println("        jclass npe = vm_find_class(env, \"java/lang/NullPointerException\");");
        w.println("        if (npe) (*env)->ThrowNew(env, npe, \"invokedynamic primitive return is null\");");
        w.println("        return 0;");
        w.println("    }");
        w.println("    jclass cls = NULL; jmethodID mid = NULL;");
        w.println("    switch (meta->returnTypeChar) {");
        emitLines(w, INDY_UNBOX_CASE_LINES);
        w.println("        default: frame->stack[frame->sp++].l = result; return 1;");
        w.println("    }");
        w.println("    return !(*env)->ExceptionCheck(env);");
        w.println("}");
        w.println();
    }

    private void emitMainFunction(PrintWriter w) {
        w.println("jobject vm_invoke_dynamic(JNIEnv* env, VMFrame* frame, MetaEntry* meta) {");
        emitMainFunctionPrelude(w);
        emitMainFunctionBootstrapResolution(w);
        emitMainFunctionInvokeTarget(w);
        w.println("}");
    }

    private void emitMainFunctionPrelude(PrintWriter w) {
        w.println("    TMP_SAVE;");
        w.println("    if (!meta) { VM_LOG(\"INVOKEDYNAMIC: meta is NULL\\n\"); TMP_RESTORE; return NULL; }");
        w.println("    vm_indy_init_cache(env);");
        w.println("    if (!id_lookupMid || !id_invokeWithArgsMid || !id_getTargetMid || !id_fromDescMid || !id_mtReturnTypeMid) {");
        w.println("        vm_indy_throw_bsm_error(env, \"InvokeDynamic runtime cache initialization failed\");");
        w.println("        TMP_RESTORE;");
        w.println("        return NULL;");
        w.println("    }");
        w.println();
        w.println("    const char* methodName = meta->nameStr ? meta->nameStr : vm_get_string(meta->nameIdx);");
        w.println("    const char* methodDesc = meta->descStr ? meta->descStr : vm_get_string(meta->descIdx);");
        w.println("    if (!methodName || !methodDesc) {");
        w.println("        vm_indy_throw_bsm_error(env, \"InvokeDynamic metadata missing name/descriptor\");");
        w.println("        TMP_RESTORE;");
        w.println("        return NULL;");
        w.println("    }");
        w.println();
        w.println("    int capturedCount = meta->argCount;");
        w.println("    const char* capturedTypes = meta->argTypesStr ? meta->argTypesStr : ((meta->argTypesIdx >= 0) ? vm_get_string(meta->argTypesIdx) : NULL);");
        w.println("    if (capturedCount < 0) capturedCount = 0;");
        w.println("    if (frame->sp < capturedCount) {");
        w.println("        vm_indy_throw_bsm_error(env, \"InvokeDynamic stack underflow before callsite invocation\");");
        w.println("        TMP_RESTORE;");
        w.println("        return NULL;");
        w.println("    }");
        w.println();
    }

    private void emitMainFunctionBootstrapResolution(PrintWriter w) {
        w.println("    jobject targetHandle = meta->cachedIndyResult;");
        w.println("    if (!targetHandle) {");
        w.println("        if (meta->bsmIdx < 0 || meta->bsmIdx >= vm_bootstrap_count) {");
        w.println("            vm_indy_throw_bsm_error(env, \"Invalid bootstrap method index\");");
        w.println("            TMP_RESTORE;");
        w.println("            return NULL;");
        w.println("        }");
        w.println();
        w.println("        VMBootstrapMethod* bsm = &vm_bootstrap_methods[meta->bsmIdx];");
        w.println("        const char* bsmOwner = vm_get_string(bsm->ownerIdx);");
        w.println("        const char* bsmName = vm_get_string(bsm->nameIdx);");
        w.println("        const char* bsmDesc = vm_get_string(bsm->descIdx);");
        w.println("        if (!bsmOwner || !bsmName || !bsmDesc) {");
        w.println("            vm_indy_throw_bsm_error(env, \"Bootstrap method metadata is incomplete\");");
        w.println("            TMP_RESTORE;");
        w.println("            return NULL;");
        w.println("        }");
        w.println();
        w.println("        jclass callerClass = frame->callerClass;");
        w.println("        jobject classLoader = NULL;");
        w.println("        if (callerClass && id_getClassLoaderMid) {");
        w.println("            classLoader = (*env)->CallObjectMethod(env, callerClass, id_getClassLoaderMid);");
        w.println("            if ((*env)->ExceptionCheck(env)) { vm_indy_clear_if_exception(env); classLoader = NULL; }");
        w.println("        }");
        w.println();
        w.println("        jobject lookup = vm_indy_get_lookup(env, callerClass);");
        w.println("        if (!lookup) {");
        w.println("            vm_indy_throw_bsm_error(env, \"Failed to create MethodHandles.Lookup for invokedynamic\");");
        w.println("            TMP_RESTORE;");
        w.println("            return NULL;");
        w.println("        }");
        w.println("        jobject invokedType = vm_indy_method_type(env, methodDesc, classLoader);");
        w.println("        if (!invokedType) {");
        w.println("            vm_indy_throw_bsm_error(env, \"Failed to create invokedynamic MethodType\");");
        w.println("            TMP_RESTORE;");
        w.println("            return NULL;");
        w.println("        }");
        w.println("        jstring nameStr = (*env)->NewStringUTF(env, methodName);");
        w.println("        if (!nameStr) {");
        w.println("            vm_indy_throw_bsm_error(env, \"Failed to allocate invokedynamic callsite name string\");");
        w.println("            TMP_RESTORE;");
        w.println("            return NULL;");
        w.println("        }");
        w.println();
        w.println("        jobject bsmHandle = vm_indy_resolve_handle(env, lookup, bsm->handleTag, bsmOwner, bsmName, bsmDesc, classLoader);");
        w.println("        if (!bsmHandle) {");
        w.println("            if (!(*env)->ExceptionCheck(env)) vm_indy_throw_bsm_error(env, \"Failed to resolve bootstrap MethodHandle\");");
        w.println("            TMP_RESTORE;");
        w.println("            return NULL;");
        w.println("        }");
        w.println();
        w.println("        int bsmArgTotal = 3 + bsm->argCount;");
        w.println("        jobjectArray bsmArgs = (*env)->NewObjectArray(env, bsmArgTotal, id_objectClass, NULL);");
        w.println("        if (!bsmArgs) {");
        w.println("            vm_indy_throw_bsm_error(env, \"Failed to allocate bootstrap argument array\");");
        w.println("            TMP_RESTORE;");
        w.println("            return NULL;");
        w.println("        }");
        w.println("        (*env)->SetObjectArrayElement(env, bsmArgs, 0, lookup);");
        w.println("        (*env)->SetObjectArrayElement(env, bsmArgs, 1, nameStr);");
        w.println("        (*env)->SetObjectArrayElement(env, bsmArgs, 2, invokedType);");
        w.println("        if ((*env)->ExceptionCheck(env)) { TMP_RESTORE; return NULL; }");
        w.println();
        w.println("        for (int i = 0; i < bsm->argCount; i++) {");
        w.println("            jobject v = vm_indy_convert_bsm_arg(env, lookup, &bsm->args[i], classLoader);");
        w.println("            if ((*env)->ExceptionCheck(env)) { TMP_RESTORE; return NULL; }");
        w.println("            if (!v) {");
        w.println("                vm_indy_throw_bsm_error(env, \"Failed to materialize bootstrap static argument\");");
        w.println("                TMP_RESTORE;");
        w.println("                return NULL;");
        w.println("            }");
        w.println("            (*env)->SetObjectArrayElement(env, bsmArgs, i + 3, v);");
        w.println("            if ((*env)->ExceptionCheck(env)) { TMP_RESTORE; return NULL; }");
        w.println("        }");
        w.println();
        w.println("        jobject callSite = (*env)->CallObjectMethod(env, bsmHandle, id_invokeWithArgsMid, bsmArgs);");
        w.println("        if ((*env)->ExceptionCheck(env)) { TMP_RESTORE; return NULL; }");
        w.println("        if (!callSite || (id_callSiteClass && !(*env)->IsInstanceOf(env, callSite, id_callSiteClass))) {");
        w.println("            vm_indy_throw_bsm_error(env, \"Bootstrap method did not return a valid CallSite\");");
        w.println("            TMP_RESTORE;");
        w.println("            return NULL;");
        w.println("        }");
        w.println("        targetHandle = (*env)->CallObjectMethod(env, callSite, id_getTargetMid);");
        w.println("        if ((*env)->ExceptionCheck(env)) { TMP_RESTORE; return NULL; }");
        w.println("        if (!targetHandle) {");
        w.println("            vm_indy_throw_bsm_error(env, \"CallSite.getTarget() returned null\");");
        w.println("            TMP_RESTORE;");
        w.println("            return NULL;");
        w.println("        }");
        w.println();
        w.println("        jobject globalTarget = (*env)->NewGlobalRef(env, targetHandle);");
        w.println("        if (globalTarget) {");
        w.println("            if (__sync_bool_compare_and_swap((void**)&meta->cachedIndyResult, NULL, globalTarget) == 0) {");
        w.println("                (*env)->DeleteGlobalRef(env, globalTarget);");
        w.println("                targetHandle = meta->cachedIndyResult;");
        w.println("            } else {");
        w.println("                targetHandle = globalTarget;");
        w.println("            }");
        w.println("        }");
        w.println("    }");
        w.println();
    }

    private void emitMainFunctionInvokeTarget(PrintWriter w) {
        w.println("    jobjectArray invokeArgs = vm_indy_pop_args(env, frame, capturedTypes, capturedCount);");
        w.println("    if ((*env)->ExceptionCheck(env)) { TMP_RESTORE; return NULL; }");
        w.println("    if (!invokeArgs && capturedCount > 0) {");
        w.println("        vm_indy_throw_bsm_error(env, \"Failed to box invokedynamic captured arguments\");");
        w.println("        TMP_RESTORE;");
        w.println("        return NULL;");
        w.println("    }");
        w.println("    jobject result = (*env)->CallObjectMethod(env, targetHandle, id_invokeWithArgsMid, invokeArgs);");
        w.println("    if ((*env)->ExceptionCheck(env)) { TMP_RESTORE; return NULL; }");
        w.println("    TMP_RESTORE;");
        w.println("    return result;");
    }
}
