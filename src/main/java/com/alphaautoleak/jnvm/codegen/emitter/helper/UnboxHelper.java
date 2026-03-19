package com.alphaautoleak.jnvm.codegen.emitter.helper;

import java.io.PrintWriter;

/**
 * Parameter unboxing helper - uses globally cached wrapper classes with atomic cache
 */
public class UnboxHelper extends VMHelper {
    
    @Override
    public String[] getIncludes() {
        return new String[] { "vm_types.h", "<jni.h>", "<stdatomic.h>" };
    }
    
    @Override
    public void generateHeader(PrintWriter w) {
        // endIndex: exclusive end index, args[endIndex-1] is last param, args[endIndex] is callerClass
        w.println("void vm_unbox_args_fast(JNIEnv* env, VMFrame* frame, jobjectArray args, const char* argTypes, int argCount, int hasThis, int endIndex);");
    }
    
    @Override
    public void generateSource(PrintWriter w) {
        // Fast version: directly use pre-stored argument type string with atomic caching
        // New format: args[0]=instance, args[1..n]=params, args[n+1]=callerClass
        // endIndex is exclusive: process args[1..endIndex-1]
        w.println("void vm_unbox_args_fast(JNIEnv* env, VMFrame* frame, jobjectArray args, const char* argTypes, int argCount, int hasThis, int endIndex) {");
        w.println("    if (!args) return;");
        w.println("    jsize len = (*env)->GetArrayLength(env, args);");
        w.println("    (*env)->EnsureLocalCapacity(env, len + 32);");
        w.println();
        
        // Helper macro for atomic method ID caching
        w.println("    #define GET_CACHED_METHOD_ID(var, clazz, name, desc) \\");
        w.println("        do { \\");
        w.println("            cached = atomic_load_explicit(&var, memory_order_relaxed); \\");
        w.println("            if (__builtin_expect(cached == 0, 0) && clazz) { \\");
        w.println("                cached = (*env)->GetMethodID(env, clazz, name, desc); \\");
        w.println("                if (cached) atomic_store_explicit(&var, cached, memory_order_relaxed); \\");
        w.println("            } \\");
        w.println("        } while(0)");
        w.println();
        
        w.println("    jmethodID cached;");
        w.println();
        
        w.println("    jclass integerClass = vm_find_class(env, \"java/lang/Integer\");");
        w.println("    static _Atomic jmethodID intValueMid;");
        w.println("    GET_CACHED_METHOD_ID(intValueMid, integerClass, \"intValue\", \"()I\");");
        w.println("    jmethodID intValueMid_cached = atomic_load_explicit(&intValueMid, memory_order_relaxed);");
        w.println();
        
        w.println("    jclass longClass = vm_find_class(env, \"java/lang/Long\");");
        w.println("    static _Atomic jmethodID longValueMid;");
        w.println("    GET_CACHED_METHOD_ID(longValueMid, longClass, \"longValue\", \"()J\");");
        w.println("    jmethodID longValueMid_cached = atomic_load_explicit(&longValueMid, memory_order_relaxed);");
        w.println();
        
        w.println("    jclass floatClass = vm_find_class(env, \"java/lang/Float\");");
        w.println("    static _Atomic jmethodID floatValueMid;");
        w.println("    GET_CACHED_METHOD_ID(floatValueMid, floatClass, \"floatValue\", \"()F\");");
        w.println("    jmethodID floatValueMid_cached = atomic_load_explicit(&floatValueMid, memory_order_relaxed);");
        w.println();
        
        w.println("    jclass doubleClass = vm_find_class(env, \"java/lang/Double\");");
        w.println("    static _Atomic jmethodID doubleValueMid;");
        w.println("    GET_CACHED_METHOD_ID(doubleValueMid, doubleClass, \"doubleValue\", \"()D\");");
        w.println("    jmethodID doubleValueMid_cached = atomic_load_explicit(&doubleValueMid, memory_order_relaxed);");
        w.println();
        
        w.println("    jclass booleanClass = vm_find_class(env, \"java/lang/Boolean\");");
        w.println("    static _Atomic jmethodID booleanValueMid;");
        w.println("    GET_CACHED_METHOD_ID(booleanValueMid, booleanClass, \"booleanValue\", \"()Z\");");
        w.println("    jmethodID booleanValueMid_cached = atomic_load_explicit(&booleanValueMid, memory_order_relaxed);");
        w.println();
        
        w.println("    jclass byteClass = vm_find_class(env, \"java/lang/Byte\");");
        w.println("    static _Atomic jmethodID byteValueMid;");
        w.println("    GET_CACHED_METHOD_ID(byteValueMid, byteClass, \"byteValue\", \"()B\");");
        w.println("    jmethodID byteValueMid_cached = atomic_load_explicit(&byteValueMid, memory_order_relaxed);");
        w.println();
        
        w.println("    jclass shortClass = vm_find_class(env, \"java/lang/Short\");");
        w.println("    static _Atomic jmethodID shortValueMid;");
        w.println("    GET_CACHED_METHOD_ID(shortValueMid, shortClass, \"shortValue\", \"()S\");");
        w.println("    jmethodID shortValueMid_cached = atomic_load_explicit(&shortValueMid, memory_order_relaxed);");
        w.println();
        
        w.println("    jclass charClass = vm_find_class(env, \"java/lang/Character\");");
        w.println("    static _Atomic jmethodID charValueMid;");
        w.println("    GET_CACHED_METHOD_ID(charValueMid, charClass, \"charValue\", \"()C\");");
        w.println("    jmethodID charValueMid_cached = atomic_load_explicit(&charValueMid, memory_order_relaxed);");
        w.println();
        
        w.println("    int localIdx = hasThis ? 1 : 0;");
        w.println("    // Process args[1..endIndex-1], skip args[0]=instance and args[endIndex]=callerClass");
        w.println("    int startIdx = 1;");
        w.println("    int stopIdx = endIndex > 0 ? endIndex : len;");
        w.println("    for (jsize i = startIdx; i < stopIdx && i < len; i++) {");
        w.println("        jobject arg = (*env)->GetObjectArrayElement(env, args, i);");
        w.println("        char expectedType = argTypes ? argTypes[i - 1] : 0;  // argTypes is 0-indexed for params only");
        w.println("        int isWide = (expectedType == 'J' || expectedType == 'D');");
        w.println();
        w.println("        if (arg == NULL) {");
        w.println("            // Primitive null should not happen with compiler-emitted bridge args; keep deterministic defaults");
        w.println("            switch (expectedType) {");
        w.println("                case 'I': case 'B': case 'C': case 'S': case 'Z': frame->locals[localIdx].i = 0; break;");
        w.println("                case 'J': frame->locals[localIdx].j = 0; break;");
        w.println("                case 'F': frame->locals[localIdx].f = 0.0f; break;");
        w.println("                case 'D': frame->locals[localIdx].d = 0.0; break;");
        w.println("                default: frame->locals[localIdx].l = NULL; break;");
        w.println("            }");
        w.println("        } else {");
        w.println("            int unboxed = 0;");
        w.println("            switch (expectedType) {");
        w.println("                case 'I':");
        w.println("                    if (integerClass && intValueMid_cached && (*env)->IsInstanceOf(env, arg, integerClass)) {");
        w.println("                        frame->locals[localIdx].i = (*env)->CallIntMethod(env, arg, intValueMid_cached); unboxed = 1;");
        w.println("                    }");
        w.println("                    break;");
        w.println("                case 'Z':");
        w.println("                    if (booleanClass && booleanValueMid_cached && (*env)->IsInstanceOf(env, arg, booleanClass)) {");
        w.println("                        frame->locals[localIdx].i = (*env)->CallBooleanMethod(env, arg, booleanValueMid_cached); unboxed = 1;");
        w.println("                    }");
        w.println("                    break;");
        w.println("                case 'B':");
        w.println("                    if (byteClass && byteValueMid_cached && (*env)->IsInstanceOf(env, arg, byteClass)) {");
        w.println("                        frame->locals[localIdx].i = (*env)->CallByteMethod(env, arg, byteValueMid_cached); unboxed = 1;");
        w.println("                    }");
        w.println("                    break;");
        w.println("                case 'S':");
        w.println("                    if (shortClass && shortValueMid_cached && (*env)->IsInstanceOf(env, arg, shortClass)) {");
        w.println("                        frame->locals[localIdx].i = (*env)->CallShortMethod(env, arg, shortValueMid_cached); unboxed = 1;");
        w.println("                    }");
        w.println("                    break;");
        w.println("                case 'C':");
        w.println("                    if (charClass && charValueMid_cached && (*env)->IsInstanceOf(env, arg, charClass)) {");
        w.println("                        frame->locals[localIdx].i = (*env)->CallCharMethod(env, arg, charValueMid_cached); unboxed = 1;");
        w.println("                    }");
        w.println("                    break;");
        w.println("                case 'J':");
        w.println("                    if (longClass && longValueMid_cached && (*env)->IsInstanceOf(env, arg, longClass)) {");
        w.println("                        frame->locals[localIdx].j = (*env)->CallLongMethod(env, arg, longValueMid_cached); unboxed = 1;");
        w.println("                    }");
        w.println("                    break;");
        w.println("                case 'F':");
        w.println("                    if (floatClass && floatValueMid_cached && (*env)->IsInstanceOf(env, arg, floatClass)) {");
        w.println("                        frame->locals[localIdx].f = (*env)->CallFloatMethod(env, arg, floatValueMid_cached); unboxed = 1;");
        w.println("                    }");
        w.println("                    break;");
        w.println("                case 'D':");
        w.println("                    if (doubleClass && doubleValueMid_cached && (*env)->IsInstanceOf(env, arg, doubleClass)) {");
        w.println("                        frame->locals[localIdx].d = (*env)->CallDoubleMethod(env, arg, doubleValueMid_cached); unboxed = 1;");
        w.println("                    }");
        w.println("                    break;");
        w.println("                default:");
        w.println("                    break;");
        w.println("            }");
        w.println("            if (!unboxed) frame->locals[localIdx].l = arg;");
        w.println("        }");
        w.println("        if (isWide) localIdx++;");
        w.println("        localIdx++;");
        w.println("    }");
        w.println("}");
        w.println();
    }
}
