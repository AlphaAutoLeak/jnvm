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
        w.println("void vm_init_unbox_cache(JNIEnv* env);");
        w.println("void vm_unbox_args_fast(JNIEnv* env, VMFrame* frame, jobjectArray args, const char* argTypes, int argCount, int hasThis, int endIndex, int argsLen);");
    }
    
    @Override
    public void generateSource(PrintWriter w) {
        w.println("static jclass id_integerClass;");
        w.println("static jclass id_longClass;");
        w.println("static jclass id_floatClass;");
        w.println("static jclass id_doubleClass;");
        w.println("static jclass id_booleanClass;");
        w.println("static jclass id_byteClass;");
        w.println("static jclass id_shortClass;");
        w.println("static jclass id_charClass;");
        w.println("static jmethodID id_intValueMid;");
        w.println("static jmethodID id_longValueMid;");
        w.println("static jmethodID id_floatValueMid;");
        w.println("static jmethodID id_doubleValueMid;");
        w.println("static jmethodID id_booleanValueMid;");
        w.println("static jmethodID id_byteValueMid;");
        w.println("static jmethodID id_shortValueMid;");
        w.println("static jmethodID id_charValueMid;");
        w.println();
        w.println("void vm_init_unbox_cache(JNIEnv* env) {");
        w.println("    if (id_integerClass != NULL) return;");
        w.println("    id_integerClass = vm_find_class(env, \"java/lang/Integer\");");
        w.println("    id_longClass = vm_find_class(env, \"java/lang/Long\");");
        w.println("    id_floatClass = vm_find_class(env, \"java/lang/Float\");");
        w.println("    id_doubleClass = vm_find_class(env, \"java/lang/Double\");");
        w.println("    id_booleanClass = vm_find_class(env, \"java/lang/Boolean\");");
        w.println("    id_byteClass = vm_find_class(env, \"java/lang/Byte\");");
        w.println("    id_shortClass = vm_find_class(env, \"java/lang/Short\");");
        w.println("    id_charClass = vm_find_class(env, \"java/lang/Character\");");
        w.println("    if (id_integerClass) id_intValueMid = (*env)->GetMethodID(env, id_integerClass, \"intValue\", \"()I\");");
        w.println("    if (id_longClass) id_longValueMid = (*env)->GetMethodID(env, id_longClass, \"longValue\", \"()J\");");
        w.println("    if (id_floatClass) id_floatValueMid = (*env)->GetMethodID(env, id_floatClass, \"floatValue\", \"()F\");");
        w.println("    if (id_doubleClass) id_doubleValueMid = (*env)->GetMethodID(env, id_doubleClass, \"doubleValue\", \"()D\");");
        w.println("    if (id_booleanClass) id_booleanValueMid = (*env)->GetMethodID(env, id_booleanClass, \"booleanValue\", \"()Z\");");
        w.println("    if (id_byteClass) id_byteValueMid = (*env)->GetMethodID(env, id_byteClass, \"byteValue\", \"()B\");");
        w.println("    if (id_shortClass) id_shortValueMid = (*env)->GetMethodID(env, id_shortClass, \"shortValue\", \"()S\");");
        w.println("    if (id_charClass) id_charValueMid = (*env)->GetMethodID(env, id_charClass, \"charValue\", \"()C\");");
        w.println("}");
        w.println();
        // Fast version: directly use pre-stored argument type string
        // New format: args[0]=instance, args[1..n]=params, args[n+1]=callerClass
        // endIndex is exclusive: process args[1..endIndex-1]
        w.println("void vm_unbox_args_fast(JNIEnv* env, VMFrame* frame, jobjectArray args, const char* argTypes, int argCount, int hasThis, int endIndex, int argsLen) {");
        w.println("    if (!args || argCount <= 0) return;");
        w.println("    if (UNLIKELY(id_integerClass == NULL)) vm_init_unbox_cache(env);");
        w.println("    jsize len = (jsize)argsLen;");
        w.println("    if (len <= 0) len = (*env)->GetArrayLength(env, args);");
        w.println("    if (len <= 1) return;");
        w.println("    if (argCount > 24) (*env)->EnsureLocalCapacity(env, argCount + 8);");
        w.println();
        
        w.println("    int localIdx = hasThis ? 1 : 0;");
        w.println("    // Process args[1..endIndex-1], skip args[0]=instance and args[endIndex]=callerClass");
        w.println("    int stopIdx = endIndex > 0 ? endIndex : len;");
        w.println("    if (stopIdx > len) stopIdx = len;");
        w.println("    int paramIdx = 0;");
        w.println("    for (jsize i = 1; i < stopIdx && paramIdx < argCount; i++, paramIdx++) {");
        w.println("        jobject arg = (*env)->GetObjectArrayElement(env, args, i);");
        w.println("        char expectedType = argTypes ? argTypes[paramIdx] : 0;  // argTypes is 0-indexed for params only");
        w.println("        int isWide = (expectedType == 'J' || expectedType == 'D');");
        w.println();
        w.println("        if (expectedType == 'L' || expectedType == '[' || expectedType == 0) {");
        w.println("            frame->locals[localIdx].l = arg;");
        w.println("        } else if (arg == NULL) {");
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
        w.println("                    if (id_integerClass && id_intValueMid && (*env)->IsInstanceOf(env, arg, id_integerClass)) {");
        w.println("                        frame->locals[localIdx].i = (*env)->CallIntMethod(env, arg, id_intValueMid); unboxed = 1;");
        w.println("                    }");
        w.println("                    break;");
        w.println("                case 'Z':");
        w.println("                    if (id_booleanClass && id_booleanValueMid && (*env)->IsInstanceOf(env, arg, id_booleanClass)) {");
        w.println("                        frame->locals[localIdx].i = (*env)->CallBooleanMethod(env, arg, id_booleanValueMid); unboxed = 1;");
        w.println("                    }");
        w.println("                    break;");
        w.println("                case 'B':");
        w.println("                    if (id_byteClass && id_byteValueMid && (*env)->IsInstanceOf(env, arg, id_byteClass)) {");
        w.println("                        frame->locals[localIdx].i = (*env)->CallByteMethod(env, arg, id_byteValueMid); unboxed = 1;");
        w.println("                    }");
        w.println("                    break;");
        w.println("                case 'S':");
        w.println("                    if (id_shortClass && id_shortValueMid && (*env)->IsInstanceOf(env, arg, id_shortClass)) {");
        w.println("                        frame->locals[localIdx].i = (*env)->CallShortMethod(env, arg, id_shortValueMid); unboxed = 1;");
        w.println("                    }");
        w.println("                    break;");
        w.println("                case 'C':");
        w.println("                    if (id_charClass && id_charValueMid && (*env)->IsInstanceOf(env, arg, id_charClass)) {");
        w.println("                        frame->locals[localIdx].i = (*env)->CallCharMethod(env, arg, id_charValueMid); unboxed = 1;");
        w.println("                    }");
        w.println("                    break;");
        w.println("                case 'J':");
        w.println("                    if (id_longClass && id_longValueMid && (*env)->IsInstanceOf(env, arg, id_longClass)) {");
        w.println("                        frame->locals[localIdx].j = (*env)->CallLongMethod(env, arg, id_longValueMid); unboxed = 1;");
        w.println("                    }");
        w.println("                    break;");
        w.println("                case 'F':");
        w.println("                    if (id_floatClass && id_floatValueMid && (*env)->IsInstanceOf(env, arg, id_floatClass)) {");
        w.println("                        frame->locals[localIdx].f = (*env)->CallFloatMethod(env, arg, id_floatValueMid); unboxed = 1;");
        w.println("                    }");
        w.println("                    break;");
        w.println("                case 'D':");
        w.println("                    if (id_doubleClass && id_doubleValueMid && (*env)->IsInstanceOf(env, arg, id_doubleClass)) {");
        w.println("                        frame->locals[localIdx].d = (*env)->CallDoubleMethod(env, arg, id_doubleValueMid); unboxed = 1;");
        w.println("                    }");
        w.println("                    break;");
        w.println("                default:");
        w.println("                    break;");
        w.println("            }");
        w.println("            if (unboxed) {");
        w.println("                (*env)->DeleteLocalRef(env, arg);");
        w.println("            } else {");
        w.println("                frame->locals[localIdx].l = arg;");
        w.println("            }");
        w.println("        }");
        w.println("        if (isWide) localIdx++;");
        w.println("        localIdx++;");
        w.println("    }");
        w.println("}");
        w.println();
    }
}
