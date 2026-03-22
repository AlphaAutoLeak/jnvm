package com.alphaautoleak.jnvm.codegen.emitter;

import com.alphaautoleak.jnvm.crypto.CryptoUtils;

import java.io.PrintWriter;
import java.util.Set;

final class VmStringPoolEmitter {

    private VmStringPoolEmitter() {
    }

    static void emit(PrintWriter w,
                     Set<String> strings,
                     boolean encryptStrings,
                     byte[] vmStringKey,
                     byte[] stringNonce) {
        if (encryptStrings) {
            emitEncrypted(w, strings, vmStringKey, stringNonce);
            return;
        }
        emitPlaintext(w, strings);
    }

    private static void emitEncrypted(PrintWriter w,
                                      Set<String> strings,
                                      byte[] vmStringKey,
                                      byte[] stringNonce) {
        w.println("const uint8_t vm_string_key[] = {");
        for (int i = 0; i < vmStringKey.length; i++) {
            if (i % 16 == 0) {
                w.print("    ");
            }
            w.printf("0x%02x%s", vmStringKey[i] & 0xFF, (i < vmStringKey.length - 1 ? ", " : ""));
        }
        w.println("\n};");

        w.println("const uint8_t vm_string_nonce[] = {");
        for (int i = 0; i < stringNonce.length; i++) {
            if (i % 16 == 0) {
                w.print("    ");
            }
            w.printf("0x%02x%s", stringNonce[i] & 0xFF, (i < stringNonce.length - 1 ? ", " : ""));
        }
        w.println("\n};");
        w.println();

        int idx = 0;
        for (String s : strings) {
            byte[] plaintext = ModifiedUtf8Encoder.encode(s);
            byte[] encrypted = CryptoUtils.chacha20(vmStringKey, stringNonce, 0, plaintext);

            w.printf("static const unsigned char vm_str_%d[] = {", idx);
            for (int i = 0; i < encrypted.length; i++) {
                if (i % 16 == 0) {
                    w.printf("\n    ");
                }
                w.printf("0x%02x%s", encrypted[i] & 0xFF, (i < encrypted.length - 1 ? ", " : ""));
            }
            w.println("\n};");
            idx++;
        }
        w.println();

        w.println("VMString vm_strings[] = {");
        idx = 0;
        for (String s : strings) {
            w.printf("    { .encData=vm_str_%d, .decData=NULL, .len=%d, .encrypted=1 },\n", idx, s.length());
            idx++;
        }
        w.println("};");
        w.println();
    }

    private static void emitPlaintext(PrintWriter w, Set<String> strings) {
        int idx = 0;
        for (String s : strings) {
            byte[] bytes = ModifiedUtf8Encoder.encode(s);
            w.printf("static const char vm_str_%d[] = {", idx);
            for (int i = 0; i < bytes.length; i++) {
                if (i % 16 == 0) {
                    w.printf("\n    ");
                }
                w.printf("0x%02x, ", bytes[i] & 0xFF);
            }
            w.println("\n    0x00");
            w.println("};");
            idx++;
        }
        w.println();

        w.println("VMString vm_strings[] = {");
        idx = 0;
        for (String s : strings) {
            w.printf(
                    "    { .encData=(const unsigned char*)vm_str_%d, .decData=NULL, .len=%d, .encrypted=0 },\n",
                    idx,
                    ModifiedUtf8Encoder.encode(s).length);
            idx++;
        }
        w.println("};");
        w.println();
    }
}
