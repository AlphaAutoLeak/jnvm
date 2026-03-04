package com.alphaautoleak.jnvm.codegen.emitter;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * 生成 chacha20.h 和 chacha20.c - 加密算法
 */
public class ChaCha20Generator extends CodeEmitter {
    
    private final File dir;
    
    public ChaCha20Generator(File dir) throws IOException {
        super(dir, "chacha20.h");
        this.dir = dir;
    }
    
    @Override
    public void generate() throws IOException {
        generateHeader();
        generateSource();
    }
    
    private void generateHeader() {
        beginGuard("CHACHA20_H");
        emit("#include <stdint.h>");
        emit("#include <stddef.h>");
        emit();
        emit("void chacha20_encrypt(const uint8_t* key, const uint8_t* nonce,");
        emit("                      const uint8_t* input, uint8_t* output, size_t len);");
        endGuard();
        close();
    }
    
    private void generateSource() throws IOException {
        try (PrintWriter src = new PrintWriter(new java.io.FileWriter(new File(dir, "chacha20.c")))) {
            src.println("#include \"chacha20.h\"");
            src.println("#include <string.h>");
            src.println();
            src.println("#define ROTL32(v, n) (((v) << (n)) | ((v) >> (32 - (n))))");
            src.println();
            src.println("static void quarterround(uint32_t* a, uint32_t* b, uint32_t* c, uint32_t* d) {");
            src.println("    *a += *b; *d ^= *a; *d = ROTL32(*d, 16);");
            src.println("    *c += *d; *b ^= *c; *b = ROTL32(*b, 12);");
            src.println("    *a += *b; *d ^= *a; *d = ROTL32(*d, 8);");
            src.println("    *c += *d; *b ^= *c; *b = ROTL32(*b, 7);");
            src.println("}");
            src.println();
            src.println("static const char* SIGMA = \"expand 32-byte k\";");
            src.println();
            src.println("void chacha20_encrypt(const uint8_t* key, const uint8_t* nonce,");
            src.println("                      const uint8_t* input, uint8_t* output, size_t len) {");
            src.println("    uint32_t state[16], working[16];");
            src.println("    uint8_t block[64];");
            src.println();
            src.println("    for (int i = 0; i < 4; i++)");
            src.println("        state[i] = ((const uint32_t*)(const void*)SIGMA)[i];");
            src.println("    for (int i = 0; i < 8; i++)");
            src.println("        state[4+i] = ((const uint32_t*)(const void*)key)[i];");
            src.println("    state[12] = 0;");
            src.println("    for (int i = 0; i < 3; i++)");
            src.println("        state[13+i] = ((const uint32_t*)(const void*)nonce)[i];");
            src.println();
            src.println("    while (len > 0) {");
            src.println("        size_t block_len = len < 64 ? len : 64;");
            src.println("        memcpy(working, state, sizeof(working));");
            src.println("        for (int i = 0; i < 10; i++) {");
            src.println("            quarterround(&working[0], &working[4], &working[8],  &working[12]);");
            src.println("            quarterround(&working[1], &working[5], &working[9],  &working[13]);");
            src.println("            quarterround(&working[2], &working[6], &working[10], &working[14]);");
            src.println("            quarterround(&working[3], &working[7], &working[11], &working[15]);");
            src.println("            quarterround(&working[0], &working[5], &working[10], &working[15]);");
            src.println("            quarterround(&working[1], &working[6], &working[11], &working[12]);");
            src.println("            quarterround(&working[2], &working[7], &working[8],  &working[13]);");
            src.println("            quarterround(&working[3], &working[4], &working[9],  &working[14]);");
            src.println("        }");
            src.println("        for (int i = 0; i < 16; i++)");
            src.println("            working[i] += state[i];");
            src.println("        memcpy(block, working, 64);");
            src.println("        for (size_t i = 0; i < block_len; i++)");
            src.println("            output[i] = input[i] ^ block[i];");
            src.println("        state[12]++;");
            src.println("        input += block_len; output += block_len; len -= block_len;");
            src.println("    }");
            src.println("}");
        }
    }
}
