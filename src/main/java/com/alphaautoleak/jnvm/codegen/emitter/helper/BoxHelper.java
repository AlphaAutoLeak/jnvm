package com.alphaautoleak.jnvm.codegen.emitter.helper;

import java.io.PrintWriter;

/**
 * 返回值装箱辅助函数 - 使用缓存的包装类和 methodID
 */
public class BoxHelper extends VMHelper {
    
    @Override
    public String[] getIncludes() {
        return new String[] { "vm_types.h", "<jni.h>" };
    }
    
    @Override
    public void generateHeader(PrintWriter w) {
        w.println("jobject vm_box_result(JNIEnv* env, VMValue result, char returnType);");
    }
    
    @Override
    public void generateSource(PrintWriter w) {
        w.println("jobject vm_box_result(JNIEnv* env, VMValue result, char returnType) {");
        w.println("    // 使用 vm_find_class 和静态 methodID 缓存");
        w.println("    static jmethodID boolInitMid = NULL;");
        w.println("    static jmethodID byteInitMid = NULL;");
        w.println("    static jmethodID charInitMid = NULL;");
        w.println("    static jmethodID shortInitMid = NULL;");
        w.println("    static jmethodID intInitMid = NULL;");
        w.println("    static jmethodID longInitMid = NULL;");
        w.println("    static jmethodID floatInitMid = NULL;");
        w.println("    static jmethodID doubleInitMid = NULL;");
        w.println();
        w.println("    switch (returnType) {");
        w.println("        case 'V': return NULL;");
        w.println("        case 'Z': {");
        w.println("            jclass cls = vm_find_class(env, \"java/lang/Boolean\");");
        w.println("            if (!boolInitMid && cls) boolInitMid = (*env)->GetMethodID(env, cls, \"<init>\", \"(Z)V\");");
        w.println("            return cls && boolInitMid ? (*env)->NewObject(env, cls, boolInitMid, result.i ? JNI_TRUE : JNI_FALSE) : NULL;");
        w.println("        }");
        w.println("        case 'B': {");
        w.println("            jclass cls = vm_find_class(env, \"java/lang/Byte\");");
        w.println("            if (!byteInitMid && cls) byteInitMid = (*env)->GetMethodID(env, cls, \"<init>\", \"(B)V\");");
        w.println("            return cls && byteInitMid ? (*env)->NewObject(env, cls, byteInitMid, (jbyte)result.i) : NULL;");
        w.println("        }");
        w.println("        case 'C': {");
        w.println("            jclass cls = vm_find_class(env, \"java/lang/Character\");");
        w.println("            if (!charInitMid && cls) charInitMid = (*env)->GetMethodID(env, cls, \"<init>\", \"(C)V\");");
        w.println("            return cls && charInitMid ? (*env)->NewObject(env, cls, charInitMid, (jchar)result.i) : NULL;");
        w.println("        }");
        w.println("        case 'S': {");
        w.println("            jclass cls = vm_find_class(env, \"java/lang/Short\");");
        w.println("            if (!shortInitMid && cls) shortInitMid = (*env)->GetMethodID(env, cls, \"<init>\", \"(S)V\");");
        w.println("            return cls && shortInitMid ? (*env)->NewObject(env, cls, shortInitMid, (jshort)result.i) : NULL;");
        w.println("        }");
        w.println("        case 'I': {");
        w.println("            jclass cls = vm_find_class(env, \"java/lang/Integer\");");
        w.println("            if (!intInitMid && cls) intInitMid = (*env)->GetMethodID(env, cls, \"<init>\", \"(I)V\");");
        w.println("            return cls && intInitMid ? (*env)->NewObject(env, cls, intInitMid, result.i) : NULL;");
        w.println("        }");
        w.println("        case 'J': {");
        w.println("            jclass cls = vm_find_class(env, \"java/lang/Long\");");
        w.println("            if (!longInitMid && cls) longInitMid = (*env)->GetMethodID(env, cls, \"<init>\", \"(J)V\");");
        w.println("            return cls && longInitMid ? (*env)->NewObject(env, cls, longInitMid, result.j) : NULL;");
        w.println("        }");
        w.println("        case 'F': {");
        w.println("            jclass cls = vm_find_class(env, \"java/lang/Float\");");
        w.println("            if (!floatInitMid && cls) floatInitMid = (*env)->GetMethodID(env, cls, \"<init>\", \"(F)V\");");
        w.println("            return cls && floatInitMid ? (*env)->NewObject(env, cls, floatInitMid, result.f) : NULL;");
        w.println("        }");
        w.println("        case 'D': {");
        w.println("            jclass cls = vm_find_class(env, \"java/lang/Double\");");
        w.println("            if (!doubleInitMid && cls) doubleInitMid = (*env)->GetMethodID(env, cls, \"<init>\", \"(D)V\");");
        w.println("            return cls && doubleInitMid ? (*env)->NewObject(env, cls, doubleInitMid, result.d) : NULL;");
        w.println("        }");
        w.println("        default: return result.l;");
        w.println("    }");
        w.println("}");
        w.println();
    }
}