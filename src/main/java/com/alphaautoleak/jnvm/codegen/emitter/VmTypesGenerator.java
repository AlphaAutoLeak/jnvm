package com.alphaautoleak.jnvm.codegen.emitter;

import com.alphaautoleak.jnvm.codegen.emitter.types.BootstrapType;
import com.alphaautoleak.jnvm.codegen.emitter.types.MetaType;
import com.alphaautoleak.jnvm.codegen.emitter.types.VMMethodType;
import com.alphaautoleak.jnvm.codegen.emitter.types.VMStringType;
import com.alphaautoleak.jnvm.codegen.emitter.types.VMValueType;
import com.alphaautoleak.jnvm.crypto.OpcodeObfuscator;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Generates vm_types.h - VM type definitions
 */
public class VmTypesGenerator {
    
    private final File dir;
    private final boolean encryptStrings;
    private final OpcodeObfuscator opcodeObfuscator;
    
    public VmTypesGenerator(File dir, boolean encryptStrings, OpcodeObfuscator opcodeObfuscator) {
        this.dir = dir;
        this.encryptStrings = encryptStrings;
        this.opcodeObfuscator = opcodeObfuscator;
    }
    
    public OpcodeObfuscator getOpcodeObfuscator() {
        return opcodeObfuscator;
    }
    
    public void generate() throws IOException {
        try (PrintWriter w = new PrintWriter(new java.io.FileWriter(new File(dir, "vm_types.h")))) {
            emitHeader(w);
            VMValueType.generate(w);
            MetaType.generate(w);
            VMMethodType.generate(w);
            VMStringType.generate(w, encryptStrings);
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
        w.println("#include <math.h>");
        w.println();
    }
    
    private void emitFooter(PrintWriter w) {
        w.println("#endif");
    }
}
