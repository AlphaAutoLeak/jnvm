package com.alphaautoleak.jnvm.codegen.emitter.helper;

import java.io.PrintWriter;

/**
 * 字符串处理辅助函数
 * 支持 ChaCha20 加密字符串的按需解密
 */
public class StringHelper extends VMHelper {
    
    private final boolean encryptStrings;
    
    public StringHelper(boolean encryptStrings) {
        this.encryptStrings = encryptStrings;
    }
    
    @Override
    public String[] getIncludes() {
        if (encryptStrings) {
            return new String[] { "vm_types.h", "vm_data.h", "chacha20.h", "<stdlib.h>", "<string.h>" };
        } else {
            return new String[] { "vm_types.h", "vm_data.h" };
        }
    }
    
    @Override
    public void generateHeader(PrintWriter w) {
        w.println("const char* vm_get_string(int idx);");
        if (encryptStrings) {
            w.println("void vm_init_strings();");
        }
    }
    
    @Override
    public void generateSource(PrintWriter w) {
        if (encryptStrings) {
            // 字符串初始化函数 - 预解密所有字符串
            w.println("void vm_init_strings() {");
            w.println("    for (int i = 0; i < vm_string_count; i++) {");
            w.println("        VMString* vs = &vm_strings[i];");
            w.println("        if (vs->encrypted && vs->decData == NULL) {");
            w.println("            vs->decData = (char*)malloc(vs->len + 1);");
            w.println("            chacha20_encrypt(vm_string_key, vm_string_nonce,");
            w.println("                vs->encData, (uint8_t*)vs->decData, vs->len);");
            w.println("            vs->decData[vs->len] = '\\0';");
            w.println("        }");
            w.println("    }");
            w.println("}");
            w.println();
            
            // 获取解密后的字符串
            w.println("const char* vm_get_string(int idx) {");
            w.println("    if (idx < 0 || idx >= vm_string_count) return \"\";");
            w.println("    VMString* vs = &vm_strings[idx];");
            w.println("    if (vs->encrypted) {");
            w.println("        return vs->decData ? vs->decData : \"\";");
            w.println("    }");
            w.println("    return (const char*)vs->encData;");
            w.println("}");
            w.println();
        } else {
            // 非加密模式：直接返回字符串
            w.println("const char* vm_get_string(int idx) {");
            w.println("    if (idx < 0 || idx >= vm_string_count) return \"\";");
            w.println("    return (const char*)vm_strings[idx].encData;");
            w.println("}");
            w.println();
        }
    }
}
