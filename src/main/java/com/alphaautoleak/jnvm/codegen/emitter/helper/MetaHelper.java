package com.alphaautoleak.jnvm.codegen.emitter.helper;

import java.io.PrintWriter;

/**
 * Metadata helper functions
 */
public class MetaHelper extends VMHelper {
    
    @Override
    public String[] getIncludes() {
        return new String[] { "vm_types.h" };
    }
    
    @Override
    public void generateHeader(PrintWriter w) {
        w.println("MetaEntry* vm_get_meta(VMMethod* m, int pc);");
    }
    
    @Override
    public void generateSource(PrintWriter w) {
        w.println("MetaEntry* vm_get_meta(VMMethod* m, int pc) {");
        w.println("    int idx = m->pcToMetaIdx[pc];");
        w.println("    return idx >= 0 ? &m->metadata[idx] : NULL;");
        w.println("}");
        w.println();
    }
}
