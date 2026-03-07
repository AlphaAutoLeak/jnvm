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
 * Generates vm_types.h - VM type definitions and opcode decode table
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
    
    public void generate() throws IOException {
        try (PrintWriter w = new PrintWriter(new java.io.FileWriter(new File(dir, "vm_types.h")))) {
            emitHeader(w);
            emitOpcodeDecodeTable(w);
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
    
    /**
     * Emits opcode decode table for runtime decoding
     */
    private void emitOpcodeDecodeTable(PrintWriter w) {
        w.println("// Opcode decode table: obfuscatedOpcode -> originalOpcode");
        w.println("// Generated at build time, each compilation has different mapping");
        w.println("static const uint8_t OPCODE_DECODE[256] = {");
        int[] decodeTable = opcodeObfuscator.getDecodeTable();
        for (int i = 0; i < 256; i++) {
            if (i % 16 == 0) w.print("    ");
            w.printf("0x%02x", decodeTable[i] & 0xFF);
            if (i < 255) w.print(", ");
            if ((i + 1) % 16 == 0) w.println();
        }
        w.println("};");
        w.println();
        
        // Helper macro for decoding
        w.println("// Decode obfuscated opcode at runtime");
        w.println("#define DECODE_OPCODE(op) OPCODE_DECODE[(op) & 0xFF]");
        w.println();
    }
    
    private void emitFooter(PrintWriter w) {
        w.println("#endif");
    }
}
