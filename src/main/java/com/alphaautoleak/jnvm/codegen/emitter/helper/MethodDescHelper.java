package com.alphaautoleak.jnvm.codegen.emitter.helper;

import java.io.PrintWriter;

/**
 * Method descriptor parsing helper functions
 */
public class MethodDescHelper extends VMHelper {
    
    @Override
    public String[] getIncludes() {
        return new String[0];
    }
    
    @Override
    public void generateHeader(PrintWriter w) {
        w.println("void vm_parse_method_desc(const char* desc, int* argCount, char* returnType);");
        w.println("char vm_get_arg_type(const char* desc, int argIndex);");
    }
    
    @Override
    public void generateSource(PrintWriter w) {
        // parse_method_desc
        w.println("__attribute__((const))");
        w.println("void vm_parse_method_desc(const char* desc, int* argCount, char* returnType) {");
        w.println("    *argCount = 0;");
        w.println("    const char* p = desc + 1;");
        w.println("    while (*p && *p != ')') {");
        w.println("        if (*p == 'L') { while (*p && *p != ';') p++; if (*p) p++; }");
        w.println("        else if (*p == '[') { while (*p == '[') p++; if (*p == 'L') { while (*p && *p != ';') p++; } if (*p) p++; }");
        w.println("        else { p++; }");
        w.println("        (*argCount)++;");
        w.println("    }");
        w.println("    if (*p == ')') p++;");
        w.println("    *returnType = *p ? *p : 'V';");
        w.println("}");
        w.println();
        
        // get_arg_type
        w.println("__attribute__((const))");
        w.println("char vm_get_arg_type(const char* desc, int argIndex) {");
        w.println("    const char* p = desc + 1;");
        w.println("    int current = 0;");
        w.println("    while (*p && *p != ')') {");
        w.println("        if (current == argIndex) {");
        w.println("            if (*p == 'L' || *p == '[') return 'L';");
        w.println("            return *p;");
        w.println("        }");
        w.println("        if (*p == 'L') { while (*p && *p != ';') p++; if (*p) p++; }");
        w.println("        else if (*p == '[') { while (*p == '[') p++; if (*p == 'L') { while (*p && *p != ';') p++; } if (*p) p++; }");
        w.println("        else { p++; }");
        w.println("        current++;");
        w.println("    }");
        w.println("    return 'L';");
        w.println("}");
        w.println();
    }
}
