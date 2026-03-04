package com.alphaautoleak.jnvm.codegen.emitter;

import com.alphaautoleak.jnvm.asm.*;
import com.alphaautoleak.jnvm.asm.BytecodeExtractor.MetaEntry;
import com.alphaautoleak.jnvm.asm.BytecodeExtractor.MetaType;
import com.alphaautoleak.jnvm.crypto.EncryptedMethodData;
import com.alphaautoleak.jnvm.crypto.StringEncryptor;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 生成 vm_data.h 和 vm_data.c - VM 数据（方法元数据、字符串池等）
 * 
 * 新格式：
 * - 字符串池：所有字符串统一存储
 * - 元数据数组：每条指令的操作数
 * - pcToMetaIdx：PC -> 元数据索引映射
 */
public class VmDataGenerator {
    
    private final List<EncryptedMethodData> methods;
    private final byte[] stringKey;
    private final File dir;
    
    public VmDataGenerator(File dir, List<EncryptedMethodData> methods, byte[] stringKey) {
        this.dir = dir;
        this.methods = methods;
        this.stringKey = stringKey;
    }
    
    public void generate() throws IOException {
        generateHeader();
        generateSource();
    }
    
    private void generateHeader() throws IOException {
        try (PrintWriter w = new PrintWriter(new java.io.FileWriter(new File(dir, "vm_data.h")))) {
            w.println("#ifndef VM_DATA_H");
            w.println("#define VM_DATA_H");
            w.println("#include \"vm_types.h\"");
            w.println();
            w.println("extern const uint8_t vm_key[];");
            w.println("extern const int vm_method_count;");
            w.println("extern VMMethod vm_methods[];");
            w.println("extern VMString vm_strings[];");
            w.println("extern const int vm_string_count;");
            w.println();
            w.println("#endif");
        }
    }
    
    private void generateSource() throws IOException {
        try (PrintWriter w = new PrintWriter(new java.io.FileWriter(new File(dir, "vm_data.c")))) {
            w.println("#include \"vm_data.h\"");
            w.println("#include \"chacha20.h\"");
            w.println();
            
            // 加密密钥 (8 bytes)
            w.println("const uint8_t vm_key[] = {");
            for (int i = 0; i < stringKey.length; i++) {
                w.printf("0x%02x%s", stringKey[i] & 0xFF, (i < stringKey.length - 1 ? ", " : ""));
            }
            w.println("\n};");
            w.println();
            
            // 全局字符串池
            Set<String> allStrings = new LinkedHashSet<>();
            for (EncryptedMethodData method : methods) {
                List<String> pool = method.getStringPool();
                if (pool != null) {
                    allStrings.addAll(pool);
                }
            }
            
            emitStringPool(w, allStrings);
            
            w.println("const int vm_method_count = " + methods.size() + ";");
            w.println("const int vm_string_count = " + allStrings.size() + ";");
            w.println();
            
            // 为每个方法生成数据
            for (EncryptedMethodData method : methods) {
                emitMethodData(w, method);
            }
            
            // 方法数组
            w.println("VMMethod vm_methods[] = {");
            for (EncryptedMethodData method : methods) {
                w.printf("    { .methodId=%d, .maxStack=%d, .maxLocals=%d, ",
                    method.getMethodId(), method.getMaxStack(), method.getMaxLocals());
                w.printf(".bytecode=(uint8_t*)m%d_bc, .bytecodeLen=%d, ",
                    method.getMethodId(), method.getEncryptedBytecode().length);
                // 添加 key
                w.printf(".key={");
                byte[] key = method.getKey();
                for (int i = 0; i < key.length; i++) {
                    w.printf("0x%02x%s", key[i] & 0xFF, (i < key.length - 1 ? ", " : ""));
                }
                w.printf("}, ");
                // 添加 nonce
                w.printf(".nonce={");
                byte[] nonce = method.getNonce();
                for (int i = 0; i < nonce.length; i++) {
                    w.printf("0x%02x%s", nonce[i] & 0xFF, (i < nonce.length - 1 ? ", " : ""));
                }
                w.printf("}, ");
                w.printf(".metadata=m%d_meta, .metadataCount=%d, ",
                    method.getMethodId(), method.getMetadata().size());
                w.printf(".pcToMetaIdx=m%d_pc2meta },\n", method.getMethodId());
            }
            w.println("};");
        }
    }
    
    private void emitStringPool(PrintWriter w, Set<String> strings) {
        int idx = 0;
        for (String s : strings) {
            byte[] enc = StringEncryptor.encrypt(s, stringKey);
            w.printf("static const char vm_str_%d[] = {", idx);
            emitBytes(w, enc);
            w.println("};");
            idx++;
        }
        w.println();
        
        w.println("VMString vm_strings[] = {");
        idx = 0;
        for (String s : strings) {
            w.printf("    { .data=vm_str_%d, .len=%d },\n", idx, s.length());
            idx++;
        }
        w.println("};");
        w.println();
    }
    
    private void emitMethodData(PrintWriter w, EncryptedMethodData method) {
        int id = method.getMethodId();
        
        // 字节码
        w.printf("static const uint8_t m%d_bc[] = {", id);
        byte[] bc = method.getEncryptedBytecode();
        for (int i = 0; i < bc.length; i++) {
            if (i % 16 == 0) w.printf("\n    ");
            w.printf("0x%02x%s", bc[i] & 0xFF, (i < bc.length - 1 ? ", " : ""));
        }
        w.println("\n};");
        w.println();
        
        // 元数据数组
        List<MetaEntry> metaList = method.getMetadata();
        if (!metaList.isEmpty()) {
            for (int i = 0; i < metaList.size(); i++) {
                emitMetaEntry(w, id, i, metaList.get(i));
            }
            
            w.printf("static MetaEntry m%d_meta[] = {", id);
            for (int i = 0; i < metaList.size(); i++) {
                MetaEntry m = metaList.get(i);
                w.printf("\n    { .type=%s, ", metaTypeToString(m.type));
                
                switch (m.type) {
                    case META_INT:
                    case META_LOCAL:
                        w.printf(".intVal=%d", m.intVal);
                        break;
                    case META_LONG:
                        w.printf(".longVal=%dL", m.longVal);
                        break;
                    case META_FLOAT:
                        w.printf(".floatVal=%ff", m.floatVal);
                        break;
                    case META_DOUBLE:
                        w.printf(".doubleVal=%f", m.doubleVal);
                        break;
                    case META_STRING:
                        w.printf(".strIdx=%d, .strLen=%d", m.strIdx, m.strLen);
                        break;
                    case META_CLASS:
                        w.printf(".classIdx=%d, .classLen=%d", m.classIdx, m.classLen);
                        break;
                    case META_FIELD:
                    case META_METHOD:
                        w.printf(".ownerIdx=%d, .ownerLen=%d, ",
                            m.ownerIdx, m.ownerLen);
                        w.printf(".nameIdx=%d, .nameLen=%d, ",
                            m.nameIdx, m.nameLen);
                        w.printf(".descIdx=%d, .descLen=%d",
                            m.descIdx, m.descLen);
                        break;
                    case META_INVOKE_DYNAMIC:
                        w.printf(".bsmIdx=%d, ", m.bsmIdx);
                        w.printf(".nameIdx=%d, .nameLen=%d, ",
                            m.nameIdx, m.nameLen);
                        w.printf(".descIdx=%d, .descLen=%d",
                            m.descIdx, m.descLen);
                        break;
                    case META_JUMP:
                        w.printf(".jumpOffset=%d", m.jumpOffset);
                        break;
                    case META_IINC:
                        w.printf(".iincIndex=%d, .iincConst=%d", m.iincIndex, m.iincConst);
                        break;
                    case META_SWITCH:
                        w.printf(".switchLow=%d, .switchHigh=%d, ",
                            m.switchLow, m.switchHigh);
                        w.printf(".switchOffsets=m%d_meta%d_offs", id, i);
                        break;
                    case META_TYPE:
                        w.printf(".classIdx=%d, .classLen=%d, .dims=%d",
                            m.classIdx, m.classLen, m.dims);
                        break;
                    default:
                        break;
                }
                w.printf(" },");
            }
            w.println("\n};");
            w.println();
        } else {
            w.printf("static MetaEntry m%d_meta[] = {};\n", id);
            w.println();
        }
        
        // pcToMetaIdx 数组
        int[] pc2meta = method.getPcToMetaIdx();
        w.printf("static int m%d_pc2meta[] = {", id);
        for (int i = 0; i < pc2meta.length; i++) {
            if (i % 32 == 0) w.printf("\n    ");
            w.printf("%d%s", pc2meta[i], (i < pc2meta.length - 1 ? ", " : ""));
        }
        w.println("\n};");
        w.println();
    }
    
    private void emitMetaEntry(PrintWriter w, int methodId, int idx, MetaEntry m) {
        if (m.type == MetaType.META_SWITCH && m.switchOffsets != null) {
            w.printf("static int m%d_meta%d_offs[] = {", methodId, idx);
            for (int i = 0; i < m.switchOffsets.length; i++) {
                w.printf("%d%s", m.switchOffsets[i], (i < m.switchOffsets.length - 1 ? ", " : ""));
            }
            w.println("};");
        }
    }
    
    private void emitBytes(PrintWriter w, byte[] data) {
        for (int i = 0; i < data.length; i++) {
            if (i % 16 == 0) w.printf("\n    ");
            w.printf("0x%02x%s", data[i] & 0xFF, (i < data.length - 1 ? ", " : ""));
        }
    }
    
    private String metaTypeToString(MetaType type) {
        return type.name();
    }
}
