package com.alphaautoleak.jnvm.codegen.emitter;

import com.alphaautoleak.jnvm.asm.ArgType;
import com.alphaautoleak.jnvm.asm.BootstrapEntry;
import com.alphaautoleak.jnvm.asm.ExceptionEntry;
import com.alphaautoleak.jnvm.asm.MetaEntry;
import com.alphaautoleak.jnvm.asm.MetaType;
import com.alphaautoleak.jnvm.crypto.CryptoUtils;
import com.alphaautoleak.jnvm.crypto.EncryptedMethodData;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Generates vm_data.h and vm_data.c - VM data (method metadata, string pool, etc.)
 * 
 * New format:
 * - String pool: all strings stored encrypted with ChaCha20
 * - Metadata array: operands for each instruction
 * - pcToMetaIdx: PC to metadata index mapping
 * - Bootstrap method table: globally shared
 */
public class VmDataGenerator {
    
    private final List<EncryptedMethodData> methods;
    private final byte[] stringKey;       // method bytecode decryption key (8 bytes)
    private final byte[] vmStringKey;     // string ChaCha20 key (32 bytes), only used when encryptStrings=true
    private final byte[] stringNonce;     // ChaCha20 nonce for strings (12 bytes), only used when encryptStrings=true
    private final boolean encryptStrings; // whether to encrypt strings
    private final File dir;
    
    /** Global string pool: string -> global index */
    private Map<String, Integer> globalStringIndexMap;
    
    /** Global Bootstrap method table */
    private List<BootstrapEntry> globalBootstrapMethods = new ArrayList<>();
    private Map<String, Integer> bootstrapIndexMap = new HashMap<>();
    private final VmBootstrapMethodsEmitter bootstrapMethodsEmitter;
    
    /** Method invocation metadata pre-computation cache: "methodId_metaIdx" -> descriptor info */
    private Map<String, MethodDescriptorParser.DescriptorInfo> invokeMetaCache = new HashMap<>();
    
    public VmDataGenerator(File dir, List<EncryptedMethodData> methods, byte[] stringKey, boolean encryptStrings) {
        this.dir = dir;
        this.methods = methods;
        this.stringKey = stringKey;           // method bytecode key (8 bytes)
        this.encryptStrings = encryptStrings;
        this.bootstrapMethodsEmitter = new VmBootstrapMethodsEmitter(globalBootstrapMethods, this::getOrAddStringIndex);
        if (encryptStrings) {
            this.vmStringKey = CryptoUtils.generateKey();  // string ChaCha20 key (32 bytes)
            this.stringNonce = CryptoUtils.generateNonce(); // string ChaCha20 nonce (12 bytes)
        } else {
            this.vmStringKey = null;
            this.stringNonce = null;
        }
    }
    
    public void generate() throws IOException {
        // First pass: collect all bootstrap methods
        collectBootstrapMethods();
        
        generateHeader();
        generateSource();
    }
    
    /**
     * Collects all bootstrap methods to global table
     */
    private void collectBootstrapMethods() {
        for (EncryptedMethodData method : methods) {
            List<BootstrapEntry> bsmList = method.getBootstrapMethods();
            if (bsmList == null) continue;
            
            for (BootstrapEntry bsm : bsmList) {
                String key = buildBootstrapKey(bsm);
                if (!bootstrapIndexMap.containsKey(key)) {
                    bootstrapIndexMap.put(key, globalBootstrapMethods.size());
                    globalBootstrapMethods.add(bsm);
                }
            }
        }
        
        // Update each method bsmIdx to global index
        // This needs to be handled during metadata generation
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
            if (encryptStrings) {
                w.println("extern const uint8_t vm_string_key[];");
                w.println("extern const uint8_t vm_string_nonce[];");
            }
            w.println("extern VMBootstrapMethod vm_bootstrap_methods[];");
            w.println("extern const int vm_bootstrap_count;");
            w.println();
            w.println("#endif");
        }
    }
    
    private void generateSource() throws IOException {
        try (PrintWriter w = new PrintWriter(new java.io.FileWriter(new File(dir, "vm_data.c")))) {
            w.println("#include \"vm_data.h\"");
            w.println("#include \"chacha20.h\"");
            w.println();

            emitVmKey(w);
            Set<String> allStrings = collectAllStringsAndInvokeMeta();
            buildGlobalStringIndex(allStrings);
            emitStringPool(w, allStrings);

            emitGlobalCounts(w, allStrings.size());
            bootstrapMethodsEmitter.emit(w);

            for (EncryptedMethodData method : methods) {
                emitMethodData(w, method);
            }

            emitMethodArray(w);
        }
    }

    private void emitVmKey(PrintWriter w) {
        w.println("const uint8_t vm_key[] = {");
        for (int i = 0; i < stringKey.length; i++) {
            w.printf("0x%02x%s", stringKey[i] & 0xFF, (i < stringKey.length - 1 ? ", " : ""));
        }
        w.println("\n};");
        w.println();
    }

    private Set<String> collectAllStringsAndInvokeMeta() {
        Set<String> allStrings = new LinkedHashSet<>();
        collectMethodLevelStrings(allStrings);
        collectMethodIdentityStrings(allStrings);
        precomputeInvokeMetaAndCollectArgTypes(allStrings);
        collectBootstrapStrings(allStrings);
        collectVmMethodArgTypeStrings(allStrings);
        return allStrings;
    }

    private void collectMethodLevelStrings(Set<String> allStrings) {
        for (EncryptedMethodData method : methods) {
            List<String> pool = method.getStringPool();
            if (pool != null) {
                allStrings.addAll(pool);
            }
            if (method.getDescriptor() != null) {
                allStrings.add(method.getDescriptor());
            }
            List<ExceptionEntry> excTable = method.getExceptionTable();
            if (excTable == null) {
                continue;
            }
            for (ExceptionEntry e : excTable) {
                if (e.getCatchType() != null) {
                    allStrings.add(e.getCatchType());
                }
            }
        }
    }

    private void collectMethodIdentityStrings(Set<String> allStrings) {
        for (EncryptedMethodData method : methods) {
            if (method.getOwner() != null) {
                allStrings.add(method.getOwner());
            }
            if (method.getName() != null) {
                allStrings.add(method.getName());
            }
        }
    }

    private void precomputeInvokeMetaAndCollectArgTypes(Set<String> allStrings) {
        invokeMetaCache.clear();
        for (EncryptedMethodData method : methods) {
            List<String> localPool = method.getStringPool();
            List<MetaEntry> metaList = method.getMetadata();
            if (localPool == null || metaList == null) {
                continue;
            }
            for (int i = 0; i < metaList.size(); i++) {
                MetaEntry m = metaList.get(i);
                if (m.type != MetaType.META_METHOD && m.type != MetaType.META_INVOKE_DYNAMIC) {
                    continue;
                }
                if (m.descIdx < 0 || m.descIdx >= localPool.size()) {
                    continue;
                }
                String desc = localPool.get(m.descIdx);
                MethodDescriptorParser.DescriptorInfo info = MethodDescriptorParser.parse(desc);
                invokeMetaCache.put(method.getMethodId() + "_" + i, info);
                if (info.getArgTypes() != null && !info.getArgTypes().isEmpty()) {
                    allStrings.add(info.getArgTypes());
                }
            }
        }
    }

    private void collectBootstrapStrings(Set<String> allStrings) {
        for (BootstrapEntry bsm : globalBootstrapMethods) {
            allStrings.add(bsm.getHandleOwner());
            allStrings.add(bsm.getHandleName());
            allStrings.add(bsm.getHandleDescriptor());

            List<Object> args = bsm.getArguments();
            List<ArgType> argTypes = bsm.getArgumentTypes();
            if (args == null || argTypes == null) {
                continue;
            }
            for (int j = 0; j < args.size(); j++) {
                Object arg = args.get(j);
                ArgType argType = argTypes.get(j);
                switch (argType) {
                    case STRING:
                    case METHOD_TYPE:
                    case CLASS:
                        allStrings.add(arg.toString());
                        break;
                    case METHOD_HANDLE:
                        String[] parts = arg.toString().split(":", 4);
                        if (parts.length >= 4) {
                            allStrings.add(parts[1]);
                            allStrings.add(parts[2]);
                            allStrings.add(parts[3]);
                        }
                        break;
                }
            }
        }
    }

    private void collectVmMethodArgTypeStrings(Set<String> allStrings) {
        for (EncryptedMethodData method : methods) {
            String desc = method.getDescriptor();
            if (desc == null) {
                continue;
            }
            String argTypes = MethodDescriptorParser.parseArgTypes(desc);
            if (argTypes != null && !argTypes.isEmpty()) {
                allStrings.add(argTypes);
            }
        }
    }

    private void buildGlobalStringIndex(Set<String> allStrings) {
        globalStringIndexMap = new HashMap<>();
        int globalIdx = 0;
        for (String s : allStrings) {
            globalStringIndexMap.put(s, globalIdx++);
        }
    }

    private void emitGlobalCounts(PrintWriter w, int stringCount) {
        w.println("const int vm_method_count = " + methods.size() + ";");
        w.println("const int vm_string_count = " + stringCount + ";");
        w.println("const int vm_bootstrap_count = " + globalBootstrapMethods.size() + ";");
        w.println();
    }

    private void emitMethodArray(PrintWriter w) {
        w.println("VMMethod vm_methods[] = {");
        for (EncryptedMethodData method : methods) {
            w.printf("    { .methodId=%d, .maxStack=%d, .maxLocals=%d, ",
                    method.getMethodId(), method.getMaxStack(), method.getMaxLocals());
            w.printf(".bytecode=(uint8_t*)m%d_bc, .bytecodeLen=%d, ",
                    method.getMethodId(), method.getEncryptedBytecode().length);
            w.printf(".metadata=m%d_meta, .metadataCount=%d, ",
                    method.getMethodId(), method.getMetadata().size());
            w.printf(".pcToMetaIdx=m%d_pc2meta, ", method.getMethodId());

            String desc = method.getDescriptor();
            Integer descIdx = globalStringIndexMap.get(desc);
            if (descIdx != null) {
                w.printf(".descIdx=%d, .descLen=%d, ", descIdx, desc.length());
            } else {
                w.printf(".descIdx=-1, .descLen=0, ");
            }

            List<ExceptionEntry> excTable = method.getExceptionTable();
            if (excTable != null && !excTable.isEmpty()) {
                w.printf(".exceptionTable=m%d_exc, .exceptionTableLength=%d, ",
                        method.getMethodId(), excTable.size());
            } else {
                w.printf(".exceptionTable=NULL, .exceptionTableLength=0, ");
            }

            String argTypes = MethodDescriptorParser.parseArgTypes(desc);
            int argCount = argTypes.length();
            int argTypesIdx = argCount > 0 ? getOrAddStringIndex(argTypes) : -1;
            int methodOwnerIdx = method.getOwner() != null ? getOrAddStringIndex(method.getOwner()) : -1;
            int methodNameIdx = method.getName() != null ? getOrAddStringIndex(method.getName()) : -1;
            w.printf(".isStatic=%d, .argCount=%d, .argTypesIdx=%d, .ownerIdx=%d, .nameIdx=%d },\n",
                    method.isStatic() ? 1 : 0, argCount, argTypesIdx, methodOwnerIdx, methodNameIdx);
        }
        w.println("};");
    }
    
    private void emitStringPool(PrintWriter w, Set<String> strings) {
        if (encryptStrings) {
            // Encryption mode: generate ChaCha20 key and nonce
            w.println("const uint8_t vm_string_key[] = {");
            for (int i = 0; i < vmStringKey.length; i++) {
                if (i % 16 == 0) w.print("    ");
                w.printf("0x%02x%s", vmStringKey[i] & 0xFF, (i < vmStringKey.length - 1 ? ", " : ""));
            }
            w.println("\n};");
            
            w.println("const uint8_t vm_string_nonce[] = {");
            for (int i = 0; i < stringNonce.length; i++) {
                if (i % 16 == 0) w.print("    ");
                w.printf("0x%02x%s", stringNonce[i] & 0xFF, (i < stringNonce.length - 1 ? ", " : ""));
            }
            w.println("\n};");
            w.println();
            
            // Encrypt and store each string
            int idx = 0;
            for (String s : strings) {
                byte[] plaintext = toModifiedUtf8(s);
                byte[] encrypted = CryptoUtils.chacha20(vmStringKey, stringNonce, 0, plaintext);
                
                w.printf("static const unsigned char vm_str_%d[] = {", idx);
                for (int i = 0; i < encrypted.length; i++) {
                    if (i % 16 == 0) w.printf("\n    ");
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
        } else {
            // Non-encryption mode: store plaintext strings directly (add null terminator)
            int idx = 0;
            for (String s : strings) {
                byte[] bytes = toModifiedUtf8(s);
                w.printf("static const char vm_str_%d[] = {", idx);
                for (int i = 0; i < bytes.length; i++) {
                    if (i % 16 == 0) w.printf("\n    ");
                    w.printf("0x%02x, ", bytes[i] & 0xFF);
                }
                w.println("\n    0x00");  // null terminator
                w.println("};");
                idx++;
            }
            w.println();
            
            w.println("VMString vm_strings[] = {");
            idx = 0;
            for (String s : strings) {
                w.printf("    { .encData=(const unsigned char*)vm_str_%d, .decData=NULL, .len=%d, .encrypted=0 },\n", idx, toModifiedUtf8(s).length);
                idx++;
            }
            w.println("};");
            w.println();
        }
    }
    
    /**
     * Gets string index
     */
    private int getOrAddStringIndex(String s) {
        Integer idx = globalStringIndexMap.get(s);
        if (idx != null) return idx;
        // String should already be in global pool
        System.err.println("[WARN] String not found in global pool: " + s);
        return 0;
    }
    
    /**
     * Maps method-local string index to global string index
     */
    private int mapStringIndex(List<String> localPool, int localIdx) {
        if (localPool == null || localIdx < 0 || localIdx >= localPool.size()) {
            return localIdx; // Keep original value (may be incorrect)
        }
        String str = localPool.get(localIdx);
        Integer globalIdx = globalStringIndexMap.get(str);
        if (globalIdx == null) {
            return localIdx; // Should not happen
        }
        return globalIdx;
    }
    
    /**
     * Maps method-local bootstrap method index to global index
     */
    private int mapBsmIndex(List<BootstrapEntry> localBsmList, int localIdx) {
        if (localBsmList == null || localIdx < 0 || localIdx >= localBsmList.size()) {
            return localIdx;
        }
        BootstrapEntry bsm = localBsmList.get(localIdx);
        String key = buildBootstrapKey(bsm);
        Integer globalIdx = bootstrapIndexMap.get(key);
        return globalIdx != null ? globalIdx : localIdx;
    }

    private String buildBootstrapKey(BootstrapEntry bsm) {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(bsm.getHandleOwner()).append(".");
        keyBuilder.append(bsm.getHandleName()).append(bsm.getHandleDescriptor());
        if (bsm.getArguments() != null) {
            for (Object arg : bsm.getArguments()) {
                keyBuilder.append("|").append(arg != null ? arg.toString() : "null");
            }
        }
        return keyBuilder.toString();
    }

    private void emitInvokeMetaSuffix(PrintWriter w, int methodId, int metaIdx, List<String> localPool, MetaEntry m) {
        MethodDescriptorParser.DescriptorInfo info = resolveInvokeMetaInfo(methodId, metaIdx, localPool, m);
        if (info == null) {
            return;
        }
        w.printf(", .argCount=%d, .returnTypeChar='%c'", info.getArgCount(), info.getReturnTypeChar());
        if (info.getArgTypes() != null && !info.getArgTypes().isEmpty()) {
            w.printf(", .argTypesIdx=%d", getOrAddStringIndex(info.getArgTypes()));
        } else {
            w.printf(", .argTypesIdx=-1");
        }
    }

    private MethodDescriptorParser.DescriptorInfo resolveInvokeMetaInfo(int methodId, int metaIdx, List<String> localPool, MetaEntry m) {
        MethodDescriptorParser.DescriptorInfo info = invokeMetaCache.get(methodId + "_" + metaIdx);
        if (info != null) {
            return info;
        }
        if (localPool == null || m.descIdx < 0 || m.descIdx >= localPool.size()) {
            return null;
        }
        return MethodDescriptorParser.parse(localPool.get(m.descIdx));
    }
    
    private void emitMethodData(PrintWriter w, EncryptedMethodData method) {
        int id = method.getMethodId();
        List<String> localPool = method.getStringPool();
        
        // Bytecode
        w.printf("static const uint8_t m%d_bc[] = {", id);
        byte[] bc = method.getEncryptedBytecode();
        for (int i = 0; i < bc.length; i++) {
            if (i % 16 == 0) w.printf("\n    ");
            w.printf("0x%02x%s", bc[i] & 0xFF, (i < bc.length - 1 ? ", " : ""));
        }
        w.println("\n};");
        w.println();
        
        // Metadata array
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
                        w.printf(".floatVal=%af", m.floatVal);
                        break;
                    case META_DOUBLE:
                        w.printf(".doubleVal=%a", m.doubleVal);
                        break;
                    case META_STRING:
                        w.printf(".strIdx=%d, .strLen=%d", 
                            mapStringIndex(localPool, m.strIdx), m.strLen);
                        break;
                    case META_CLASS:
                        w.printf(".classIdx=%d, .classLen=%d", 
                            mapStringIndex(localPool, m.classIdx), m.classLen);
                        break;
                    case META_FIELD:
                    case META_METHOD:
                        w.printf(".ownerIdx=%d, .ownerLen=%d, ",
                            mapStringIndex(localPool, m.ownerIdx), m.ownerLen);
                        w.printf(".nameIdx=%d, .nameLen=%d, ",
                            mapStringIndex(localPool, m.nameIdx), m.nameLen);
                        w.printf(".descIdx=%d, .descLen=%d",
                            mapStringIndex(localPool, m.descIdx), m.descLen);
                        // Add pre-computed invocation metadata
                        if (m.type == MetaType.META_METHOD) {
                            emitInvokeMetaSuffix(w, id, i, localPool, m);
                        }
                        break;
                    case META_INVOKE_DYNAMIC:
                        // Map local bsmIdx to global index
                        int globalBsmIdx = mapBsmIndex(method.getBootstrapMethods(), m.bsmIdx);
                        w.printf(".bsmIdx=%d, ", globalBsmIdx);
                        w.printf(".nameIdx=%d, .nameLen=%d, ",
                            mapStringIndex(localPool, m.nameIdx), m.nameLen);
                        w.printf(".descIdx=%d, .descLen=%d",
                            mapStringIndex(localPool, m.descIdx), m.descLen);
                        emitInvokeMetaSuffix(w, id, i, localPool, m);
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
                        // Add switchKeys for LOOKUPSWITCH (when keys are present)
                        if (m.switchKeys != null && m.switchKeys.length > 0) {
                            w.printf(", .switchKeys=m%d_meta%d_keys", id, i);
                        }
                        break;
                    case META_TYPE:
                        w.printf(".classIdx=%d, .classLen=%d, .dims=%d",
                            mapStringIndex(localPool, m.classIdx), m.classLen, m.dims);
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
        
        // pcToMetaIdx array
        int[] pc2meta = method.getPcToMetaIdx();
        w.printf("static int m%d_pc2meta[] = {", id);
        for (int i = 0; i < pc2meta.length; i++) {
            if (i % 32 == 0) w.printf("\n    ");
            w.printf("%d%s", pc2meta[i], (i < pc2meta.length - 1 ? ", " : ""));
        }
        w.println("\n};");
        w.println();
        
        // Exception table (preserve original order)
        List<ExceptionEntry> excTable = method.getExceptionTable();
        if (excTable != null && !excTable.isEmpty()) {
            w.printf("static VMExceptionEntry m%d_exc[] = {", id);
            for (int i = 0; i < excTable.size(); i++) {
                ExceptionEntry e = excTable.get(i);
                int catchTypeIdx = -1;
                if (e.getCatchType() != null) {
                    catchTypeIdx = getOrAddStringIndex(e.getCatchType());
                }
                w.printf("\n    { .startPc=%d, .endPc=%d, .handlerPc=%d, .catchTypeIdx=%d },",
                    e.getStartPc(), e.getEndPc(), e.getHandlerPc(), catchTypeIdx);
            }
            w.println("\n};");
            w.println();
        }
    }
    
    private void emitMetaEntry(PrintWriter w, int methodId, int idx, MetaEntry m) {
        if (m.type == MetaType.META_SWITCH && m.switchOffsets != null) {
            // Emit switchOffsets array
            w.printf("static int m%d_meta%d_offs[] = {", methodId, idx);
            for (int i = 0; i < m.switchOffsets.length; i++) {
                w.printf("%d%s", m.switchOffsets[i], (i < m.switchOffsets.length - 1 ? ", " : ""));
            }
            w.println("};");
            
            // Emit switchKeys array for LOOKUPSWITCH (non-null keys means LOOKUPSWITCH)
            if (m.switchKeys != null && m.switchKeys.length > 0) {
                w.printf("static int m%d_meta%d_keys[] = {", methodId, idx);
                for (int i = 0; i < m.switchKeys.length; i++) {
                    w.printf("%d%s", m.switchKeys[i], (i < m.switchKeys.length - 1 ? ", " : ""));
                }
                w.println("};");
            }
        }
    }
    
    private void emitBytes(PrintWriter w, byte[] data) {
        for (int i = 0; i < data.length; i++) {
            if (i % 16 == 0) w.printf("\n    ");
            w.printf("0x%02x%s", data[i] & 0xFF, (i < data.length - 1 ? ", " : ""));
        }
    }

    /**
     * Encode Java String to Modified UTF-8 (same format as JNI NewStringUTF).
     * This avoids embedded 0x00 bytes and encodes surrogate pairs as two 3-byte sequences.
     */
    private byte[] toModifiedUtf8(String s) {
        if (s == null || s.isEmpty()) return new byte[0];
        int len = s.length();
        // Worst case 3 bytes per char
        byte[] out = new byte[len * 3];
        int idx = 0;
        for (int i = 0; i < len; i++) {
            int c = s.charAt(i);
            if (c == 0x0000) {
                out[idx++] = (byte) 0xC0;
                out[idx++] = (byte) 0x80;
            } else if (c <= 0x007F) {
                out[idx++] = (byte) c;
            } else if (c <= 0x07FF) {
                out[idx++] = (byte) (0xC0 | (c >> 6));
                out[idx++] = (byte) (0x80 | (c & 0x3F));
            } else {
                out[idx++] = (byte) (0xE0 | (c >> 12));
                out[idx++] = (byte) (0x80 | ((c >> 6) & 0x3F));
                out[idx++] = (byte) (0x80 | (c & 0x3F));
            }
        }
        if (idx == out.length) return out;
        byte[] trimmed = new byte[idx];
        System.arraycopy(out, 0, trimmed, 0, idx);
        return trimmed;
    }
    
    private String metaTypeToString(MetaType type) {
        return type.name();
    }
}
