package com.alphaautoleak.jnvm.codegen.emitter;

import com.alphaautoleak.jnvm.codegen.emitter.types.BootstrapType;
import com.alphaautoleak.jnvm.codegen.emitter.types.MetaType;
import com.alphaautoleak.jnvm.codegen.emitter.types.VMMethodType;
import com.alphaautoleak.jnvm.codegen.emitter.types.VMStringType;
import com.alphaautoleak.jnvm.codegen.emitter.types.VMValueType;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * 生成 vm_types.h - VM 类型定义
 */
public class VmTypesGenerator {
    
    private final File dir;
    
    public VmTypesGenerator(File dir) {
        this.dir = dir;
    }
    
    public void generate() throws IOException {
        try (PrintWriter w = new PrintWriter(new java.io.FileWriter(new File(dir, "vm_types.h")))) {
            emitHeader(w);
            VMValueType.generate(w);
            MetaType.generate(w);
            VMMethodType.generate(w);
            VMStringType.generate(w);
            BootstrapType.generate(w);
            emitFooter(w);
        }
    }
    
    private void emitHeader(PrintWriter w) {
        w.println("#ifndef VM_TYPES_H");
        w.println("#define VM_TYPES_H");
        w.println();
        w.println("#include <jni.h>");
        w.println("#include <stdint.h>");
        w.println("#include <string.h>");
        w.println("#include <stdlib.h>");
        w.println();
    }
    
    private void emitFooter(PrintWriter w) {
        w.println("#endif");
    }
}
