package com.alphaautoleak.jnvm.codegen.emitter;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

final class VmDataHeaderEmitter {

    private VmDataHeaderEmitter() {
    }

    static void emit(File dir, boolean encryptStrings) throws IOException {
        try (PrintWriter w = new PrintWriter(new java.io.FileWriter(new File(dir, "vm_data.h")))) {
            w.println("#ifndef VM_DATA_H");
            w.println("#define VM_DATA_H");
            w.println("#include \"vm_types.h\"");
            w.println();
            w.println("extern const uint8_t vm_key[];");
            w.println("extern const int vm_method_count;");
            w.println("extern VMMethod* vm_methods;");
            w.println("extern VMString vm_strings[];");
            w.println("extern const int vm_string_count;");
            if (encryptStrings) {
                w.println("extern const uint8_t vm_string_key[];");
                w.println("extern const uint8_t vm_string_nonce[];");
            }
            w.println("extern VMBootstrapMethod vm_bootstrap_methods[];");
            w.println("extern const int vm_bootstrap_count;");
            w.println("void vm_init_method_table(void);");
            w.println("void vm_init_meta_all(void);");
            w.println();
            w.println("#endif");
        }
    }
}
