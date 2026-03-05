package com.alphaautoleak.jnvm.codegen.emitter.helper;

import java.io.PrintWriter;

/**
 * 参数拆箱辅助函数
 */
public class UnboxHelper extends VMHelper {
    
    @Override
    public String[] getIncludes() {
        return new String[] { "vm_types.h", "<jni.h>" };
    }
    
    @Override
    public void generateHeader(PrintWriter w) {
        w.println("void vm_unbox_args(JNIEnv* env, VMFrame* frame, jobjectArray args, const char* methodDesc, int hasThis);");
    }
    
    @Override
    public void generateSource(PrintWriter w) {
        w.println("void vm_unbox_args(JNIEnv* env, VMFrame* frame, jobjectArray args, const char* methodDesc, int hasThis) {");
        w.println("    if (!args) return;");
        w.println("    jsize len = (*env)->GetArrayLength(env, args);");
        w.println("    (*env)->EnsureLocalCapacity(env, len + 32);");
        w.println();
        w.println("    // Pre-cache boxed classes and method IDs");
        w.println("    jclass integerClass = (*env)->FindClass(env, \"java/lang/Integer\");");
        w.println("    jmethodID intValueMid = integerClass ? (*env)->GetMethodID(env, integerClass, \"intValue\", \"()I\") : NULL;");
        w.println("    jclass longClass = (*env)->FindClass(env, \"java/lang/Long\");");
        w.println("    jmethodID longValueMid = longClass ? (*env)->GetMethodID(env, longClass, \"longValue\", \"()J\") : NULL;");
        w.println("    jclass floatClass = (*env)->FindClass(env, \"java/lang/Float\");");
        w.println("    jmethodID floatValueMid = floatClass ? (*env)->GetMethodID(env, floatClass, \"floatValue\", \"()F\") : NULL;");
        w.println("    jclass doubleClass = (*env)->FindClass(env, \"java/lang/Double\");");
        w.println("    jmethodID doubleValueMid = doubleClass ? (*env)->GetMethodID(env, doubleClass, \"doubleValue\", \"()D\") : NULL;");
        w.println("    jclass booleanClass = (*env)->FindClass(env, \"java/lang/Boolean\");");
        w.println("    jmethodID booleanValueMid = booleanClass ? (*env)->GetMethodID(env, booleanClass, \"booleanValue\", \"()Z\") : NULL;");
        w.println("    jclass byteClass = (*env)->FindClass(env, \"java/lang/Byte\");");
        w.println("    jmethodID byteValueMid = byteClass ? (*env)->GetMethodID(env, byteClass, \"byteValue\", \"()B\") : NULL;");
        w.println("    jclass shortClass = (*env)->FindClass(env, \"java/lang/Short\");");
        w.println("    jmethodID shortValueMid = shortClass ? (*env)->GetMethodID(env, shortClass, \"shortValue\", \"()S\") : NULL;");
        w.println("    jclass charClass = (*env)->FindClass(env, \"java/lang/Character\");");
        w.println("    jmethodID charValueMid = charClass ? (*env)->GetMethodID(env, charClass, \"charValue\", \"()C\") : NULL;");
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
