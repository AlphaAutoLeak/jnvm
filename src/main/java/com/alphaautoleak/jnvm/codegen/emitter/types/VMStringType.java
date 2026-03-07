package com.alphaautoleak.jnvm.codegen.emitter.types;

import java.io.PrintWriter;

/**
 * Generates VMString type definition
 * Supports ChaCha20 encrypted strings
 */
public class VMStringType {
    
    public static void generate(PrintWriter w, boolean encryptStrings) {
        w.println("/* String pool */");
        w.println("typedef struct {");
        if (encryptStrings) {
            w.println("    const unsigned char* encData;  // encrypted data");
            w.println("    char* decData;                 // decryption cache (allocated at runtime)");
            w.println("    int len;                       // original length");
            w.println("    int encrypted;                 // is encrypted (1=yes, 0=no)");
        } else {
            // Non-encryption mode: simplified structure
            w.println("    const unsigned char* encData;  // string data");
            w.println("    char* decData;                 // unused");
            w.println("    int len;                       // string length");
            w.println("    int encrypted;                 // always 0");
        }
        w.println("} VMString;");
        w.println();
    }
}
