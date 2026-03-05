package com.alphaautoleak.jnvm.codegen.emitter.helper;

import java.io.PrintWriter;

/**
 * 返回值装箱辅助函数
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
        w.println("    switch (returnType) {");
        w.println("        case 'V': return NULL;");
        w.println("        case 'Z': {");
        w.println("            jclass cls = (*env)->FindClass(env, \"java/lang/Boolean\");");
        w.println("            jmethodID mid = (*env)->GetMethodID(env, cls, \"<init>\", \"(Z)V\");");
        w.println("            return (*env)->NewObject(env, cls, mid, result.i ? JNI_TRUE : JNI_FALSE);");
        w.println("        }");
        w.println("        case 'B': {");
        w.println("            jclass cls = (*env)->FindClass(env, \"java/lang/Byte\");");
        w.println("            jmethodID mid = (*env)->GetMethodID(env, cls, \"<init>\", \"(B)V\");");
        w.println("            return (*env)->NewObject(env, cls, mid, (jbyte)result.i);");
        w.println("        }");
        w.println("        case 'C': {");
        w.println("            jclass cls = (*env)->FindClass(env, \"java/lang/Character\");");
        w.println("            jmethodID mid = (*env)->GetMethodID(env, cls, \"<init>\", \"(C)V\");");
        w.println("            return (*env)->NewObject(env, cls, mid, (jchar)result.i);");
        w.println("        }");
        w.println("        case 'S': {");
        w.println("            jclass cls = (*env)->FindClass(env, \"java/lang/Short\");");
        w.println("            jmethodID mid = (*env)->GetMethodID(env, cls, \"<init>\", \"(S)V\");");
        w.println("            return (*env)->NewObject(env, cls, mid, (jshort)result.i);");
        w.println("        }");
        w.println("        case 'I': {");
        w.println("            jclass cls = (*env)->FindClass(env, \"java/lang/Integer\");");
        w.println("            jmethodID mid = (*env)->GetMethodID(env, cls, \"<init>\", \"(I)V\");");
        w.println("            return (*env)->NewObject(env, cls, mid, result.i);");
        w.println("        }");
        w.println("        case 'J': {");
        w.println("            jclass cls = (*env)->FindClass(env, \"java/lang/Long\");");
        w.println("            jmethodID mid = (*env)->GetMethodID(env, cls, \"<init>\", \"(J)V\");");
        w.println("            return (*env)->NewObject(env, cls, mid, result.j);");
        w.println("        }");
        w.println("        case 'F': {");
        w.println("            jclass cls = (*env)->FindClass(env, \"java/lang/Float\");");
        w.println("            jmethodID mid = (*env)->GetMethodID(env, cls, \"<init>\", \"(F)V\");");
        w.println("            return (*env)->NewObject(env, cls, mid, result.f);");
        w.println("        }");
        w.println("        case 'D': {");
        w.println("            jclass cls = (*env)->FindClass(env, \"java/lang/Double\");");
        w.println("            jmethodID mid = (*env)->GetMethodID(env, cls, \"<init>\", \"(D)V\");");
        w.println("            return (*env)->NewObject(env, cls, mid, result.d);");
        w.println("        }");
        w.println("        default: return result.l;");
        w.println("    }");
        w.println("}");
        w.println();
    }
}
