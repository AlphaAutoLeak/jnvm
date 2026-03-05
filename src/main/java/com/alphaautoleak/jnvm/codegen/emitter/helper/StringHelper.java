package com.alphaautoleak.jnvm.codegen.emitter.helper;

import java.io.PrintWriter;

/**
 * 字符串处理辅助函数
 */
public class StringHelper extends VMHelper {
    
    @Override
    public String[] getIncludes() {
        return new String[] { "vm_types.h" };
    }
    
    @Override
    public void generateHeader(PrintWriter w) {
        w.println("const char* vm_get_string(VMString* pool, int idx);");
    }
    
    @Override
    public void generateSource(PrintWriter w) {
        w.println("const char* vm_get_string(VMString* pool, int idx) {");
        w.println("    return pool[idx].data;");
        w.println("}");
        w.println();
    }
}
