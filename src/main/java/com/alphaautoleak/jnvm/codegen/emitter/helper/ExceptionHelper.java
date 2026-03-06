package com.alphaautoleak.jnvm.codegen.emitter.helper;

import java.io.PrintWriter;

/**
 * 异常处理辅助函数
 */
public class ExceptionHelper extends VMHelper {
    
    @Override
    public String[] getIncludes() {
        return new String[] { "vm_types.h", "vm_data.h", "<jni.h>" };
    }
    
    @Override
    public void generateHeader(PrintWriter w) {
        w.println("int vm_find_exception_handler(JNIEnv* env, VMMethod* m, int pc, jthrowable exception);");
    }
    
    @Override
    public void generateSource(PrintWriter w) {
        w.println("int vm_find_exception_handler(JNIEnv* env, VMMethod* m, int pc, jthrowable exception) {");
        w.println("    if (m->exceptionTable == NULL || m->exceptionTableLength <= 0) return -1;");
        w.println("    for (int i = 0; i < m->exceptionTableLength; i++) {");
        w.println("        VMExceptionEntry* entry = &m->exceptionTable[i];");
        w.println("        if (pc >= entry->startPc && pc < entry->endPc) {");
        w.println("            if (entry->catchTypeIdx < 0) return entry->handlerPc; // catch-all");
        w.println("            const char* catchType = vm_get_string(entry->catchTypeIdx);");
        w.println("            jclass catchClass = vm_find_class(env, catchType);");
        w.println("            if (catchClass && (*env)->IsInstanceOf(env, exception, catchClass)) {");
        w.println("                return entry->handlerPc;");
        w.println("            }");
        w.println("        }");
        w.println("    }");
        w.println("    return -1;");
        w.println("}");
        w.println();
    }
}