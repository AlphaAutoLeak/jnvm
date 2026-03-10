package com.alphaautoleak.jnvm.codegen.emitter.helper;

import java.io.PrintWriter;

/**
 * Return value boxing helper - uses cached wrapper classes with atomic methodIDs
 */
public class BoxHelper extends VMHelper {
    
    @Override
    public String[] getIncludes() {
        return new String[] { "vm_types.h", "<jni.h>", "<stdatomic.h>" };
    }
    
    @Override
    public void generateHeader(PrintWriter w) {
        w.println("jobject vm_box_result(JNIEnv* env, VMValue result, char returnType);");
    }
    
    @Override
    public void generateSource(PrintWriter w) {
        w.println("jobject vm_box_result(JNIEnv* env, VMValue result, char returnType) {");
        w.println("    // Atomic cached methodIDs for boxing");
        w.println("    static _Atomic jmethodID boolInitMid;");
        w.println("    static _Atomic jmethodID byteInitMid;");
        w.println("    static _Atomic jmethodID charInitMid;");
        w.println("    static _Atomic jmethodID shortInitMid;");
        w.println("    static _Atomic jmethodID intInitMid;");
        w.println("    static _Atomic jmethodID longInitMid;");
        w.println("    static _Atomic jmethodID floatInitMid;");
        w.println("    static _Atomic jmethodID doubleInitMid;");
        w.println();
        
        // Helper macro
        w.println("    #define GET_CACHED_INIT(var, clazz, desc) \\");
        w.println("        do { \\");
        w.println("            mid = atomic_load_explicit(&var, memory_order_relaxed); \\");
        w.println("            if (__builtin_expect(mid == 0, 0) && cls) { \\");
        w.println("                mid = (*env)->GetMethodID(env, cls, \"<init>\", desc); \\");
        w.println("                if (mid) atomic_store_explicit(&var, mid, memory_order_relaxed); \\");
        w.println("            } \\");
        w.println("        } while(0)");
        w.println();
        
        w.println("    jmethodID mid;");
        w.println();
        w.println("    switch (returnType) {");
        w.println("        case 'V': return NULL;");
        w.println("        case 'Z': {");
        w.println("            jclass cls = vm_find_class(env, \"java/lang/Boolean\");");
        w.println("            GET_CACHED_INIT(boolInitMid, cls, \"(Z)V\");");
        w.println("            return cls && mid ? (*env)->NewObject(env, cls, mid, result.i ? JNI_TRUE : JNI_FALSE) : NULL;");
        w.println("        }");
        w.println("        case 'B': {");
        w.println("            jclass cls = vm_find_class(env, \"java/lang/Byte\");");
        w.println("            GET_CACHED_INIT(byteInitMid, cls, \"(B)V\");");
        w.println("            return cls && mid ? (*env)->NewObject(env, cls, mid, (jbyte)result.i) : NULL;");
        w.println("        }");
        w.println("        case 'C': {");
        w.println("            jclass cls = vm_find_class(env, \"java/lang/Character\");");
        w.println("            GET_CACHED_INIT(charInitMid, cls, \"(C)V\");");
        w.println("            return cls && mid ? (*env)->NewObject(env, cls, mid, (jchar)result.i) : NULL;");
        w.println("        }");
        w.println("        case 'S': {");
        w.println("            jclass cls = vm_find_class(env, \"java/lang/Short\");");
        w.println("            GET_CACHED_INIT(shortInitMid, cls, \"(S)V\");");
        w.println("            return cls && mid ? (*env)->NewObject(env, cls, mid, (jshort)result.i) : NULL;");
        w.println("        }");
        w.println("        case 'I': {");
        w.println("            jclass cls = vm_find_class(env, \"java/lang/Integer\");");
        w.println("            GET_CACHED_INIT(intInitMid, cls, \"(I)V\");");
        w.println("            return cls && mid ? (*env)->NewObject(env, cls, mid, result.i) : NULL;");
        w.println("        }");
        w.println("        case 'J': {");
        w.println("            jclass cls = vm_find_class(env, \"java/lang/Long\");");
        w.println("            GET_CACHED_INIT(longInitMid, cls, \"(J)V\");");
        w.println("            return cls && mid ? (*env)->NewObject(env, cls, mid, result.j) : NULL;");
        w.println("        }");
        w.println("        case 'F': {");
        w.println("            jclass cls = vm_find_class(env, \"java/lang/Float\");");
        w.println("            GET_CACHED_INIT(floatInitMid, cls, \"(F)V\");");
        w.println("            return cls && mid ? (*env)->NewObject(env, cls, mid, result.f) : NULL;");
        w.println("        }");
        w.println("        case 'D': {");
        w.println("            jclass cls = vm_find_class(env, \"java/lang/Double\");");
        w.println("            GET_CACHED_INIT(doubleInitMid, cls, \"(D)V\");");
        w.println("            return cls && mid ? (*env)->NewObject(env, cls, mid, result.d) : NULL;");
        w.println("        }");
        w.println("        default: return result.l;");
        w.println("    }");
        w.println("}");
        w.println();
    }
}
