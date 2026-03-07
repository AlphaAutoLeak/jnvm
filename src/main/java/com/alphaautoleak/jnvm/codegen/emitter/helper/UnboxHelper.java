package com.alphaautoleak.jnvm.codegen.emitter.helper;

import java.io.PrintWriter;

/**
 * 参数拆箱辅助函数 - 使用全局缓存的包装类
 */
public class UnboxHelper extends VMHelper {
    
    @Override
    public String[] getIncludes() {
        return new String[] { "vm_types.h", "<jni.h>" };
    }
    
    @Override
    public void generateHeader(PrintWriter w) {
        w.println("void vm_unbox_args(JNIEnv* env, VMFrame* frame, jobjectArray args, const char* methodDesc, int hasThis);");
        w.println("void vm_unbox_args_fast(JNIEnv* env, VMFrame* frame, jobjectArray args, const char* argTypes, int argCount, int hasThis);");
    }
    
    @Override
    public void generateSource(PrintWriter w) {
        // 快速版本：直接使用预存的参数类型字符串
        w.println("void vm_unbox_args_fast(JNIEnv* env, VMFrame* frame, jobjectArray args, const char* argTypes, int argCount, int hasThis) {");
        w.println("    if (!args) return;");
        w.println("    jsize len = (*env)->GetArrayLength(env, args);");
        w.println("    (*env)->EnsureLocalCapacity(env, len + 32);");
        w.println();
        w.println("    // 使用 vm_find_class 缓存包装类（复用全局缓存）");
        w.println("    jclass integerClass = vm_find_class(env, \"java/lang/Integer\");");
        w.println("    static jmethodID intValueMid = NULL;");
        w.println("    if (integerClass && !intValueMid) intValueMid = (*env)->GetMethodID(env, integerClass, \"intValue\", \"()I\");");
        w.println();
        w.println("    jclass longClass = vm_find_class(env, \"java/lang/Long\");");
        w.println("    static jmethodID longValueMid = NULL;");
        w.println("    if (longClass && !longValueMid) longValueMid = (*env)->GetMethodID(env, longClass, \"longValue\", \"()J\");");
        w.println();
        w.println("    jclass floatClass = vm_find_class(env, \"java/lang/Float\");");
        w.println("    static jmethodID floatValueMid = NULL;");
        w.println("    if (floatClass && !floatValueMid) floatValueMid = (*env)->GetMethodID(env, floatClass, \"floatValue\", \"()F\");");
        w.println();
        w.println("    jclass doubleClass = vm_find_class(env, \"java/lang/Double\");");
        w.println("    static jmethodID doubleValueMid = NULL;");
        w.println("    if (doubleClass && !doubleValueMid) doubleValueMid = (*env)->GetMethodID(env, doubleClass, \"doubleValue\", \"()D\");");
        w.println();
        w.println("    jclass booleanClass = vm_find_class(env, \"java/lang/Boolean\");");
        w.println("    static jmethodID booleanValueMid = NULL;");
        w.println("    if (booleanClass && !booleanValueMid) booleanValueMid = (*env)->GetMethodID(env, booleanClass, \"booleanValue\", \"()Z\");");
        w.println();
        w.println("    jclass byteClass = vm_find_class(env, \"java/lang/Byte\");");
        w.println("    static jmethodID byteValueMid = NULL;");
        w.println("    if (byteClass && !byteValueMid) byteValueMid = (*env)->GetMethodID(env, byteClass, \"byteValue\", \"()B\");");
        w.println();
        w.println("    jclass shortClass = vm_find_class(env, \"java/lang/Short\");");
        w.println("    static jmethodID shortValueMid = NULL;");
        w.println("    if (shortClass && !shortValueMid) shortValueMid = (*env)->GetMethodID(env, shortClass, \"shortValue\", \"()S\");");
        w.println();
        w.println("    jclass charClass = vm_find_class(env, \"java/lang/Character\");");
        w.println("    static jmethodID charValueMid = NULL;");
        w.println("    if (charClass && !charValueMid) charValueMid = (*env)->GetMethodID(env, charClass, \"charValue\", \"()C\");");
        w.println();
        w.println("    int localIdx = hasThis ? 1 : 0;");
        w.println("    for (jsize i = 0; i < len; i++) {");
        w.println("        jobject arg = (*env)->GetObjectArrayElement(env, args, i);");
        w.println("        char expectedType = argTypes ? argTypes[i] : 0;  // 直接索引，O(1)");
        w.println();
        w.println("        if (arg == NULL) {");
        w.println("            frame->locals[localIdx].l = NULL;");
        w.println("        } else {");
        w.println("            int unboxed = 0;");
        w.println("            if (expectedType == 'I' || expectedType == 'B' || expectedType == 'C' || expectedType == 'S' || expectedType == 'Z') {");
        w.println("                if (integerClass && (*env)->IsInstanceOf(env, arg, integerClass) && intValueMid) {");
        w.println("                    frame->locals[localIdx].i = (*env)->CallIntMethod(env, arg, intValueMid); unboxed = 1;");
        w.println("                } else if (booleanClass && (*env)->IsInstanceOf(env, arg, booleanClass) && booleanValueMid) {");
        w.println("                    frame->locals[localIdx].i = (*env)->CallBooleanMethod(env, arg, booleanValueMid); unboxed = 1;");
        w.println("                } else if (byteClass && (*env)->IsInstanceOf(env, arg, byteClass) && byteValueMid) {");
        w.println("                    frame->locals[localIdx].i = (*env)->CallByteMethod(env, arg, byteValueMid); unboxed = 1;");
        w.println("                } else if (shortClass && (*env)->IsInstanceOf(env, arg, shortClass) && shortValueMid) {");
        w.println("                    frame->locals[localIdx].i = (*env)->CallShortMethod(env, arg, shortValueMid); unboxed = 1;");
        w.println("                } else if (charClass && (*env)->IsInstanceOf(env, arg, charClass) && charValueMid) {");
        w.println("                    frame->locals[localIdx].i = (*env)->CallCharMethod(env, arg, charValueMid); unboxed = 1;");
        w.println("                }");
        w.println("            } else if (expectedType == 'J') {");
        w.println("                if (longClass && (*env)->IsInstanceOf(env, arg, longClass) && longValueMid) {");
        w.println("                    frame->locals[localIdx].j = (*env)->CallLongMethod(env, arg, longValueMid); unboxed = 1;");
        w.println("                }");
        w.println("                localIdx++;");
        w.println("            } else if (expectedType == 'F') {");
        w.println("                if (floatClass && (*env)->IsInstanceOf(env, arg, floatClass) && floatValueMid) {");
        w.println("                    frame->locals[localIdx].f = (*env)->CallFloatMethod(env, arg, floatValueMid); unboxed = 1;");
        w.println("                }");
        w.println("            } else if (expectedType == 'D') {");
        w.println("                if (doubleClass && (*env)->IsInstanceOf(env, arg, doubleClass) && doubleValueMid) {");
        w.println("                    frame->locals[localIdx].d = (*env)->CallDoubleMethod(env, arg, doubleValueMid); unboxed = 1;");
        w.println("                }");
        w.println("                localIdx++;");
        w.println("            }");
        w.println("            if (!unboxed) frame->locals[localIdx].l = arg;");
        w.println("        }");
        w.println("        localIdx++;");
        w.println("    }");
        w.println("}");
        w.println();
        
        // 保留旧版本以兼容其他调用
        w.println("void vm_unbox_args(JNIEnv* env, VMFrame* frame, jobjectArray args, const char* methodDesc, int hasThis) {");
        w.println("    if (!args) return;");
        w.println("    jsize len = (*env)->GetArrayLength(env, args);");
        w.println("    (*env)->EnsureLocalCapacity(env, len + 32);");
        w.println();
        w.println("    // 使用 vm_find_class 缓存包装类（复用全局缓存）");
        w.println("    jclass integerClass = vm_find_class(env, \"java/lang/Integer\");");
        w.println("    static jmethodID intValueMid = NULL;");
        w.println("    if (integerClass && !intValueMid) intValueMid = (*env)->GetMethodID(env, integerClass, \"intValue\", \"()I\");");
        w.println();
        w.println("    jclass longClass = vm_find_class(env, \"java/lang/Long\");");
        w.println("    static jmethodID longValueMid = NULL;");
        w.println("    if (longClass && !longValueMid) longValueMid = (*env)->GetMethodID(env, longClass, \"longValue\", \"()J\");");
        w.println();
        w.println("    jclass floatClass = vm_find_class(env, \"java/lang/Float\");");
        w.println("    static jmethodID floatValueMid = NULL;");
        w.println("    if (floatClass && !floatValueMid) floatValueMid = (*env)->GetMethodID(env, floatClass, \"floatValue\", \"()F\");");
        w.println();
        w.println("    jclass doubleClass = vm_find_class(env, \"java/lang/Double\");");
        w.println("    static jmethodID doubleValueMid = NULL;");
        w.println("    if (doubleClass && !doubleValueMid) doubleValueMid = (*env)->GetMethodID(env, doubleClass, \"doubleValue\", \"()D\");");
        w.println();
        w.println("    jclass booleanClass = vm_find_class(env, \"java/lang/Boolean\");");
        w.println("    static jmethodID booleanValueMid = NULL;");
        w.println("    if (booleanClass && !booleanValueMid) booleanValueMid = (*env)->GetMethodID(env, booleanClass, \"booleanValue\", \"()Z\");");
        w.println();
        w.println("    jclass byteClass = vm_find_class(env, \"java/lang/Byte\");");
        w.println("    static jmethodID byteValueMid = NULL;");
        w.println("    if (byteClass && !byteValueMid) byteValueMid = (*env)->GetMethodID(env, byteClass, \"byteValue\", \"()B\");");
        w.println();
        w.println("    jclass shortClass = vm_find_class(env, \"java/lang/Short\");");
        w.println("    static jmethodID shortValueMid = NULL;");
        w.println("    if (shortClass && !shortValueMid) shortValueMid = (*env)->GetMethodID(env, shortClass, \"shortValue\", \"()S\");");
        w.println();
        w.println("    jclass charClass = vm_find_class(env, \"java/lang/Character\");");
        w.println("    static jmethodID charValueMid = NULL;");
        w.println("    if (charClass && !charValueMid) charValueMid = (*env)->GetMethodID(env, charClass, \"charValue\", \"()C\");");
        w.println();
        w.println("    int localIdx = hasThis ? 1 : 0;");
        w.println("    for (jsize i = 0; i < len; i++) {");
        w.println("        jobject arg = (*env)->GetObjectArrayElement(env, args, i);");
        w.println("        char expectedType = methodDesc ? vm_get_arg_type(methodDesc, i) : 0;");
        w.println();
        w.println("        if (arg == NULL) {");
        w.println("            frame->locals[localIdx].l = NULL;");
        w.println("        } else {");
        w.println("            int unboxed = 0;");
        w.println("            if (expectedType == 'I' || expectedType == 'B' || expectedType == 'C' || expectedType == 'S' || expectedType == 'Z') {");
        w.println("                if (integerClass && (*env)->IsInstanceOf(env, arg, integerClass) && intValueMid) {");
        w.println("                    frame->locals[localIdx].i = (*env)->CallIntMethod(env, arg, intValueMid); unboxed = 1;");
        w.println("                } else if (booleanClass && (*env)->IsInstanceOf(env, arg, booleanClass) && booleanValueMid) {");
        w.println("                    frame->locals[localIdx].i = (*env)->CallBooleanMethod(env, arg, booleanValueMid); unboxed = 1;");
        w.println("                } else if (byteClass && (*env)->IsInstanceOf(env, arg, byteClass) && byteValueMid) {");
        w.println("                    frame->locals[localIdx].i = (*env)->CallByteMethod(env, arg, byteValueMid); unboxed = 1;");
        w.println("                } else if (shortClass && (*env)->IsInstanceOf(env, arg, shortClass) && shortValueMid) {");
        w.println("                    frame->locals[localIdx].i = (*env)->CallShortMethod(env, arg, shortValueMid); unboxed = 1;");
        w.println("                } else if (charClass && (*env)->IsInstanceOf(env, arg, charClass) && charValueMid) {");
        w.println("                    frame->locals[localIdx].i = (*env)->CallCharMethod(env, arg, charValueMid); unboxed = 1;");
        w.println("                }");
        w.println("            } else if (expectedType == 'J') {");
        w.println("                if (longClass && (*env)->IsInstanceOf(env, arg, longClass) && longValueMid) {");
        w.println("                    frame->locals[localIdx].j = (*env)->CallLongMethod(env, arg, longValueMid); unboxed = 1;");
        w.println("                }");
        w.println("                localIdx++;");
        w.println("            } else if (expectedType == 'F') {");
        w.println("                if (floatClass && (*env)->IsInstanceOf(env, arg, floatClass) && floatValueMid) {");
        w.println("                    frame->locals[localIdx].f = (*env)->CallFloatMethod(env, arg, floatValueMid); unboxed = 1;");
        w.println("                }");
        w.println("            } else if (expectedType == 'D') {");
        w.println("                if (doubleClass && (*env)->IsInstanceOf(env, arg, doubleClass) && doubleValueMid) {");
        w.println("                    frame->locals[localIdx].d = (*env)->CallDoubleMethod(env, arg, doubleValueMid); unboxed = 1;");
        w.println("                }");
        w.println("                localIdx++;");
        w.println("            }");
        w.println("            if (!unboxed) frame->locals[localIdx].l = arg;");
        w.println("        }");
        w.println("        localIdx++;");
        w.println("    }");
        w.println("}");
        w.println();
    }
}