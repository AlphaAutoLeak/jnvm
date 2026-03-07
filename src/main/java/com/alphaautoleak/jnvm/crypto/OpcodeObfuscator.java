package com.alphaautoleak.jnvm.crypto;

import java.security.SecureRandom;
import java.util.Arrays;

/**
 * Opcode obfuscator - maps JVM opcodes to random values.
 * Each compilation generates a different mapping, making bytecode analysis harder.
 */
public class OpcodeObfuscator {

    /** Mapping table: originalOpcode -> obfuscatedOpcode */
    private final int[] encodeTable = new int[256];

    /** Reverse mapping: obfuscatedOpcode -> originalOpcode */
    private final int[] decodeTable = new int[256];

    public OpcodeObfuscator() {
        generateMapping();
    }

    /**
     * Generates random opcode mapping.
     * Creates a bijection between original opcodes and obfuscated values.
     */
    private void generateMapping() {
        SecureRandom random = new SecureRandom();

        // Initialize with identity mapping
        for (int i = 0; i < 256; i++) {
            encodeTable[i] = i;
        }

        // Fisher-Yates shuffle
        for (int i = 255; i > 0; i--) {
            int j = random.nextInt(i + 1);
            int temp = encodeTable[i];
            encodeTable[i] = encodeTable[j];
            encodeTable[j] = temp;
        }

        // Build decode table (reverse mapping)
        Arrays.fill(decodeTable, -1);
        for (int i = 0; i < 256; i++) {
            decodeTable[encodeTable[i]] = i;
        }
    }

    /**
     * Encodes an opcode to its obfuscated value.
     */
    public int encode(int opcode) {
        return encodeTable[opcode & 0xFF];
    }

    /**
     * Decodes an obfuscated opcode back to original.
     */
    public int decode(int obfuscated) {
        return decodeTable[obfuscated & 0xFF];
    }

    /**
     * Returns the encode table for C code generation.
     */
    public int[] getEncodeTable() {
        return encodeTable.clone();
    }

    /**
     * Returns the decode table for C code generation.
     */
    public int[] getDecodeTable() {
        return decodeTable.clone();
    }

    /**
     * Generates C code for the decode table.
     * This table is used by the native interpreter to decode opcodes.
     */
    public String generateDecodeTableCCode() {
        StringBuilder sb = new StringBuilder();
        sb.append("// Opcode decode table: obfuscatedOpcode -> originalOpcode\n");
        sb.append("static const uint8_t OPCODE_DECODE[256] = {\n");
        for (int i = 0; i < 256; i++) {
            if (i % 16 == 0) sb.append("    ");
            sb.append(String.format("0x%02x", decodeTable[i] & 0xFF));
            if (i < 255) sb.append(", ");
            if ((i + 1) % 16 == 0) sb.append("\n");
        }
        sb.append("};\n");
        return sb.toString();
    }
}
