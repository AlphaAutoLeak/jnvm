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
import java.security.SecureRandom;
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

    private static final class MethodRoute {
        final int segmentIndex;
        final int segmentOffset;
        final int encodedRoute;

        MethodRoute(int segmentIndex, int segmentOffset, int encodedRoute) {
            this.segmentIndex = segmentIndex;
            this.segmentOffset = segmentOffset;
            this.encodedRoute = encodedRoute;
        }
    }

    private static final int META_TYPE_COUNT = 16;
    private static final int FIELD_META_TYPE = 0;
    private static final int FIELD_INT_VAL = 1;
    private static final int FIELD_LONG_VAL = 2;
    private static final int FIELD_FLOAT_BITS = 3;
    private static final int FIELD_DOUBLE_BITS = 4;
    private static final int FIELD_STR_IDX = 10;
    private static final int FIELD_STR_LEN = 11;
    private static final int FIELD_CLASS_IDX = 12;
    private static final int FIELD_CLASS_LEN = 13;
    private static final int FIELD_OWNER_IDX = 20;
    private static final int FIELD_OWNER_LEN = 21;
    private static final int FIELD_NAME_IDX = 22;
    private static final int FIELD_NAME_LEN = 23;
    private static final int FIELD_DESC_IDX = 24;
    private static final int FIELD_DESC_LEN = 25;
    private static final int FIELD_HANDLE_TAG = 26;
    private static final int FIELD_BSM_IDX = 30;
    private static final int FIELD_JUMP_OFFSET = 40;
    private static final int FIELD_IINC_INDEX = 50;
    private static final int FIELD_IINC_CONST = 51;
    private static final int FIELD_SWITCH_LOW = 60;
    private static final int FIELD_SWITCH_HIGH = 61;
    private static final int FIELD_SWITCH_KEY_BASE = 100;
    private static final int FIELD_SWITCH_OFFSET_BASE = 500;
    private static final int FIELD_DIMS = 70;
    private static final int FIELD_ARG_COUNT = 80;
    private static final int FIELD_RETURN_TYPE = 81;
    private static final int FIELD_ARG_TYPES_IDX = 82;
    private static final int FIELD_ARG_LOCAL_SLOTS = 83;
    private static final int FIELD_ARG_WIDE_MASK = 84;
    private static final int FIELD_METHOD_DESC_IDX = 900;
    private static final int FIELD_METHOD_DESC_LEN = 901;
    private static final int FIELD_METHOD_ARG_COUNT = 902;
    private static final int FIELD_METHOD_ARG_TYPES_IDX = 903;
    private static final int FIELD_METHOD_RETURN_TYPE = 904;
    private static final int FIELD_METHOD_OWNER_IDX = 905;
    private static final int FIELD_METHOD_NAME_IDX = 906;
    private static final int FIELD_PC2META_BASE = 1200;
    
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
    private final int[] metaTypeEncode = new int[META_TYPE_COUNT];
    private final int[] metaTypeDecode = new int[256];
    private final int metaSalt;
    private final int fieldSalt;
    private final Map<Integer, Integer> methodMetaKeys = new HashMap<>();
    private final int methodRouteKey;
    private final List<List<EncryptedMethodData>> methodSegments = new ArrayList<>();
    private final Map<Integer, MethodRoute> methodRoutesById = new HashMap<>();
    
    public VmDataGenerator(File dir, List<EncryptedMethodData> methods, byte[] stringKey, boolean encryptStrings) {
        this.dir = dir;
        this.methods = methods;
        this.stringKey = stringKey;           // method bytecode key (8 bytes)
        this.encryptStrings = encryptStrings;
        this.bootstrapMethodsEmitter = new VmBootstrapMethodsEmitter(globalBootstrapMethods, this::getOrAddStringIndex);
        SecureRandom random = new SecureRandom();
        int salt;
        do {
            salt = random.nextInt();
        } while (salt == 0);
        this.metaSalt = salt;
        int fs;
        do {
            fs = random.nextInt();
        } while (fs == 0);
        this.fieldSalt = fs;
        int routeKey;
        do {
            routeKey = random.nextInt();
        } while (routeKey == 0);
        this.methodRouteKey = routeKey;
        initMetaTypeCodec(random);
        initMethodMetaKeys(random);
        initMethodRouting(random);
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
            w.println();
            emitMetaDecodeSupport(w);
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
        for (int seg = 0; seg < methodSegments.size(); seg++) {
            List<EncryptedMethodData> segMethods = methodSegments.get(seg);
            w.printf("static VMMethod vm_method_seg_%d[] = {", seg);
            for (EncryptedMethodData method : segMethods) {
                emitSingleMethodLiteral(w, method);
            }
            w.println("\n};");
            w.println();
        }
        w.printf("static VMMethod vm_method_runtime[%d];%n", methods.size());
        w.println("VMMethod* vm_methods = vm_method_runtime;");
        w.println();

        w.println("static VMMethod* vm_method_segs[] = {");
        for (int seg = 0; seg < methodSegments.size(); seg++) {
            String suffix = (seg + 1 < methodSegments.size()) ? "," : "";
            w.printf("    vm_method_seg_%d%s%n", seg, suffix);
        }
        w.println("};");
        w.println("static const int vm_method_seg_lens[] = {");
        for (int seg = 0; seg < methodSegments.size(); seg++) {
            int len = methodSegments.get(seg).size();
            String suffix = (seg + 1 < methodSegments.size()) ? "," : "";
            w.printf("    %d%s%n", len, suffix);
        }
        w.println("};");
        w.printf("static const uint32_t VM_METHOD_ROUTE_KEY = 0x%08xu;%n", Integer.toUnsignedLong(methodRouteKey));
        w.println();

        int[] encodedRoutes = new int[methods.size()];
        for (int i = 0; i < encodedRoutes.length; i++) {
            encodedRoutes[i] = encodeMethodRoute(0, i);
        }
        for (EncryptedMethodData method : methods) {
            int methodId = method.getMethodId();
            if (methodId < 0 || methodId >= encodedRoutes.length) {
                continue;
            }
            MethodRoute route = methodRoutesById.get(methodId);
            if (route == null) {
                continue;
            }
            encodedRoutes[methodId] = route.encodedRoute;
        }

        w.println("static const uint32_t vm_method_route_obf[] = {");
        for (int i = 0; i < encodedRoutes.length; i++) {
            if (i % 8 == 0) {
                w.print("    ");
            }
            w.printf("0x%08xu%s", Integer.toUnsignedLong(encodedRoutes[i]), (i + 1 < encodedRoutes.length ? ", " : ""));
            if ((i + 1) % 8 == 0 || i + 1 == encodedRoutes.length) {
                w.println();
            }
        }
        w.println("};");
        w.println();

        w.println("static inline uint32_t vm_method_route_mix(uint32_t methodId) {");
        w.println("    uint32_t x = VM_METHOD_ROUTE_KEY ^ (methodId * 0x045d9f3bu);");
        w.println("    x ^= (x << 11);");
        w.println("    x ^= (x >> 7);");
        w.println("    x ^= 0x7f4a7c15u;");
        w.println("    return x;");
        w.println("}");
        w.println();

        w.println("static inline uint32_t vm_decode_method_route(uint32_t encoded, uint32_t methodId) {");
        w.println("    return encoded ^ vm_method_route_mix(methodId);");
        w.println("}");
        w.println();

        w.println("void vm_init_method_table(void) {");
        w.println("    for (int methodId = 0; methodId < vm_method_count; methodId++) {");
        w.println("        uint32_t packed = vm_decode_method_route(vm_method_route_obf[methodId], (uint32_t)methodId);");
        w.println("        int seg = (int)((packed >> 16) & 0xFFFFu);");
        w.println("        int off = (int)(packed & 0xFFFFu);");
        w.println("        if (seg < 0 || seg >= (int)(sizeof(vm_method_seg_lens) / sizeof(vm_method_seg_lens[0]))) {");
        w.println("            continue;");
        w.println("        }");
        w.println("        if (off < 0 || off >= vm_method_seg_lens[seg]) {");
        w.println("            continue;");
        w.println("        }");
        w.println("        vm_methods[methodId] = vm_method_segs[seg][off];");
        w.println("    }");
        w.println("}");
    }

    private void emitSingleMethodLiteral(PrintWriter w, EncryptedMethodData method) {
        int metaKey = methodMetaKeys.getOrDefault(method.getMethodId(), 0);
        String desc = method.getDescriptor();
        Integer descIdx = globalStringIndexMap.get(desc);
        int plainDescIdx = descIdx != null ? descIdx : -1;
        int plainDescLen = desc != null ? desc.length() : 0;
        MethodDescriptorParser.DescriptorInfo descriptorInfo = MethodDescriptorParser.parse(desc);
        String argTypes = descriptorInfo.getArgTypes();
        int plainArgCount = descriptorInfo.getArgCount();
        int plainArgTypesIdx = plainArgCount > 0 ? getOrAddStringIndex(argTypes) : -1;
        int plainReturnType = descriptorInfo.getReturnTypeChar();
        int plainMethodOwnerIdx = method.getOwner() != null ? getOrAddStringIndex(method.getOwner()) : -1;
        int plainMethodNameIdx = method.getName() != null ? getOrAddStringIndex(method.getName()) : -1;

        w.printf("%n    { .methodId=%d, .metaKey=0x%08xu, .metaDecoded=0, .maxStack=%d, .maxLocals=%d, ",
                method.getMethodId(), Integer.toUnsignedLong(metaKey), method.getMaxStack(), method.getMaxLocals());
        w.printf(".bytecode=(uint8_t*)m%d_bc, .bytecodeLen=%d, ",
                method.getMethodId(), method.getEncryptedBytecode().length);
        w.printf(".metadata=m%d_meta, .metadataCount=%d, ",
                method.getMethodId(), method.getMetadata().size());
        w.printf(".pcToMetaIdx=m%d_pc2meta, ", method.getMethodId());
        w.printf(".descIdx=%d, .descLen=%d, ",
                encodeInt(plainDescIdx, metaKey, FIELD_METHOD_DESC_IDX),
                encodeInt(plainDescLen, metaKey, FIELD_METHOD_DESC_LEN));

        List<ExceptionEntry> excTable = method.getExceptionTable();
        if (excTable != null && !excTable.isEmpty()) {
            w.printf(".exceptionTable=m%d_exc, .exceptionTableLength=%d, ",
                    method.getMethodId(), excTable.size());
        } else {
            w.printf(".exceptionTable=NULL, .exceptionTableLength=0, ");
        }
        w.printf(".isStatic=%d, .argCount=%d, .argTypesIdx=%d, .returnTypeChar=(char)0x%02x, .ownerIdx=%d, .nameIdx=%d },",
                method.isStatic() ? 1 : 0,
                encodeInt(plainArgCount, metaKey, FIELD_METHOD_ARG_COUNT),
                encodeInt(plainArgTypesIdx, metaKey, FIELD_METHOD_ARG_TYPES_IDX),
                encodeByte(plainReturnType, metaKey, FIELD_METHOD_RETURN_TYPE),
                encodeInt(plainMethodOwnerIdx, metaKey, FIELD_METHOD_OWNER_IDX),
                encodeInt(plainMethodNameIdx, metaKey, FIELD_METHOD_NAME_IDX));
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

    private void emitInvokeMetaSuffix(PrintWriter w, int methodId, int metaIdx, List<String> localPool, MetaEntry m, int metaKey) {
        MethodDescriptorParser.DescriptorInfo info = resolveInvokeMetaInfo(methodId, metaIdx, localPool, m);
        if (info == null) {
            return;
        }
        String argTypes = info.getArgTypes();
        int argLocalSlots = 0;
        long argWideMask = 0L;
        if (argTypes != null && !argTypes.isEmpty()) {
            for (int i = 0; i < argTypes.length(); i++) {
                char t = argTypes.charAt(i);
                argLocalSlots++;
                if (t == 'J' || t == 'D') {
                    argLocalSlots++;
                    if (i < 64) {
                        argWideMask |= (1L << i);
                    }
                }
            }
        }

        w.printf(", .argCount=%d, .returnTypeChar=(char)0x%02x, .argLocalSlots=%d, .argWideMask=0x%xULL",
                encodeInt(info.getArgCount(), metaKey, FIELD_ARG_COUNT),
                encodeByte(info.getReturnTypeChar(), metaKey, FIELD_RETURN_TYPE),
                encodeInt(argLocalSlots, metaKey, FIELD_ARG_LOCAL_SLOTS),
                encodeLong(argWideMask, metaKey, FIELD_ARG_WIDE_MASK));
        if (argTypes != null && !argTypes.isEmpty()) {
            w.printf(", .argTypesIdx=%d", encodeInt(getOrAddStringIndex(argTypes), metaKey, FIELD_ARG_TYPES_IDX));
        } else {
            w.printf(", .argTypesIdx=%d", encodeInt(-1, metaKey, FIELD_ARG_TYPES_IDX));
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

    private void initMetaTypeCodec(Random random) {
        Arrays.fill(metaTypeDecode, -1);
        List<Integer> tags = new ArrayList<>(256);
        for (int i = 0; i < 256; i++) {
            tags.add(i);
        }
        Collections.shuffle(tags, random);
        for (int i = 0; i < META_TYPE_COUNT; i++) {
            int encoded = tags.get(i);
            metaTypeEncode[i] = encoded;
            metaTypeDecode[encoded] = i;
        }
        for (int i = 0; i < metaTypeDecode.length; i++) {
            if (metaTypeDecode[i] < 0) {
                metaTypeDecode[i] = i;
            }
        }
    }

    private void initMethodMetaKeys(Random random) {
        for (EncryptedMethodData method : methods) {
            int key;
            do {
                key = random.nextInt();
            } while (key == 0);
            methodMetaKeys.put(method.getMethodId(), key);
        }
    }

    private void initMethodRouting(Random random) {
        methodSegments.clear();
        methodRoutesById.clear();

        List<EncryptedMethodData> shuffled = new ArrayList<>(methods);
        Collections.shuffle(shuffled, random);

        int segCount = chooseSegmentCount(shuffled.size());
        for (int i = 0; i < segCount; i++) {
            methodSegments.add(new ArrayList<EncryptedMethodData>());
        }

        for (int i = 0; i < shuffled.size(); i++) {
            methodSegments.get(i % segCount).add(shuffled.get(i));
        }

        for (int seg = 0; seg < methodSegments.size(); seg++) {
            List<EncryptedMethodData> list = methodSegments.get(seg);
            for (int off = 0; off < list.size(); off++) {
                EncryptedMethodData method = list.get(off);
                int packed = ((seg & 0xFFFF) << 16) | (off & 0xFFFF);
                int encoded = encodeMethodRoute(packed, method.getMethodId());
                methodRoutesById.put(method.getMethodId(), new MethodRoute(seg, off, encoded));
            }
        }
    }

    private int chooseSegmentCount(int methodCount) {
        if (methodCount <= 8) {
            return 2;
        }
        if (methodCount <= 32) {
            return 3;
        }
        if (methodCount <= 96) {
            return 4;
        }
        return 5;
    }

    private int encodeMetaType(MetaType type) {
        int idx = type != null ? type.value : 0;
        if (idx < 0 || idx >= META_TYPE_COUNT) {
            return idx & 0xFF;
        }
        return metaTypeEncode[idx] & 0xFF;
    }

    private int encodeInt(int value, int metaKey, int fieldId) {
        return value ^ metaMix(metaKey, fieldId);
    }

    private int encodeByte(int value, int metaKey, int fieldId) {
        return (value ^ (metaMix(metaKey, fieldId) & 0xFF)) & 0xFF;
    }

    private long encodeLong(long value, int metaKey, int fieldId) {
        return value ^ metaMix64(metaKey, fieldId);
    }

    private int encodeMethodRoute(int packedRoute, int methodId) {
        return packedRoute ^ methodRouteMix(methodId);
    }

    private int methodRouteMix(int methodId) {
        int x = methodRouteKey ^ (methodId * 0x45d9f3b);
        x ^= (x << 11);
        x ^= (x >>> 7);
        x ^= 0x7f4a7c15;
        return x;
    }

    private int metaMix(int metaKey, int fieldId) {
        int fid = fieldId ^ fieldSalt;
        int x = metaKey ^ (metaSalt + fid * 0x9e3779b9);
        x ^= (x << 13);
        x ^= (x >>> 17);
        x ^= (x << 5);
        return x;
    }

    private long metaMix64(int metaKey, int fieldId) {
        long hi = Integer.toUnsignedLong(metaMix(metaKey, fieldId));
        long lo = Integer.toUnsignedLong(metaMix(metaKey, fieldId ^ 0x7f4a7c15));
        return (hi << 32) | lo;
    }
    
    private void emitMethodData(PrintWriter w, EncryptedMethodData method) {
        int id = method.getMethodId();
        int metaKey = methodMetaKeys.getOrDefault(id, 0);
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
                emitMetaEntry(w, id, i, metaList.get(i), metaKey);
            }
            
            w.printf("static MetaEntry m%d_meta[] = {", id);
            for (int i = 0; i < metaList.size(); i++) {
                MetaEntry m = metaList.get(i);
                int encodedType = encodeByte(encodeMetaType(m.type), metaKey, FIELD_META_TYPE);
                w.printf("\n    { .type=(MetaType)%d, ", encodedType);
                
                switch (m.type) {
                    case META_INT:
                    case META_LOCAL:
                        w.printf(".intVal=%d", encodeInt(m.intVal, metaKey, FIELD_INT_VAL));
                        break;
                    case META_LONG:
                        w.printf(".longVal=%dL", encodeLong(m.longVal, metaKey, FIELD_LONG_VAL));
                        break;
                    case META_FLOAT:
                        w.printf(".intVal=%d", encodeInt(Float.floatToRawIntBits(m.floatVal), metaKey, FIELD_FLOAT_BITS));
                        break;
                    case META_DOUBLE:
                        w.printf(".longVal=%dL", encodeLong(Double.doubleToRawLongBits(m.doubleVal), metaKey, FIELD_DOUBLE_BITS));
                        break;
                    case META_STRING:
                        w.printf(".strIdx=%d, .strLen=%d", 
                                encodeInt(mapStringIndex(localPool, m.strIdx), metaKey, FIELD_STR_IDX),
                                encodeInt(m.strLen, metaKey, FIELD_STR_LEN));
                        break;
                    case META_CLASS:
                        w.printf(".classIdx=%d, .classLen=%d", 
                                encodeInt(mapStringIndex(localPool, m.classIdx), metaKey, FIELD_CLASS_IDX),
                                encodeInt(m.classLen, metaKey, FIELD_CLASS_LEN));
                        break;
                    case META_FIELD:
                        w.printf(".ownerIdx=%d, .ownerLen=%d, ",
                                encodeInt(mapStringIndex(localPool, m.ownerIdx), metaKey, FIELD_OWNER_IDX),
                                encodeInt(m.ownerLen, metaKey, FIELD_OWNER_LEN));
                        w.printf(".nameIdx=%d, .nameLen=%d, ",
                                encodeInt(mapStringIndex(localPool, m.nameIdx), metaKey, FIELD_NAME_IDX),
                                encodeInt(m.nameLen, metaKey, FIELD_NAME_LEN));
                        w.printf(".descIdx=%d, .descLen=%d",
                                encodeInt(mapStringIndex(localPool, m.descIdx), metaKey, FIELD_DESC_IDX),
                                encodeInt(m.descLen, metaKey, FIELD_DESC_LEN));
                        break;
                    case META_METHOD:
                        w.printf(".ownerIdx=%d, .ownerLen=%d, ",
                                encodeInt(mapStringIndex(localPool, m.ownerIdx), metaKey, FIELD_OWNER_IDX),
                                encodeInt(m.ownerLen, metaKey, FIELD_OWNER_LEN));
                        w.printf(".nameIdx=%d, .nameLen=%d, ",
                                encodeInt(mapStringIndex(localPool, m.nameIdx), metaKey, FIELD_NAME_IDX),
                                encodeInt(m.nameLen, metaKey, FIELD_NAME_LEN));
                        w.printf(".descIdx=%d, .descLen=%d",
                                encodeInt(mapStringIndex(localPool, m.descIdx), metaKey, FIELD_DESC_IDX),
                                encodeInt(m.descLen, metaKey, FIELD_DESC_LEN));
                        if (m.handleTag > 0) {
                            w.printf(", .handleTag=%d", encodeInt(m.handleTag, metaKey, FIELD_HANDLE_TAG));
                        }
                        // Add pre-computed invocation metadata
                        emitInvokeMetaSuffix(w, id, i, localPool, m, metaKey);
                        break;
                    case META_INVOKE_DYNAMIC:
                        // Map local bsmIdx to global index
                        int globalBsmIdx = mapBsmIndex(method.getBootstrapMethods(), m.bsmIdx);
                        w.printf(".nameIdx=%d, .nameLen=%d, ",
                                encodeInt(mapStringIndex(localPool, m.nameIdx), metaKey, FIELD_NAME_IDX),
                                encodeInt(m.nameLen, metaKey, FIELD_NAME_LEN));
                        w.printf(".descIdx=%d, .descLen=%d",
                                encodeInt(mapStringIndex(localPool, m.descIdx), metaKey, FIELD_DESC_IDX),
                                encodeInt(m.descLen, metaKey, FIELD_DESC_LEN));
                        w.printf(", .bsmIdx=%d",
                                encodeInt(globalBsmIdx, metaKey, FIELD_BSM_IDX));
                        emitInvokeMetaSuffix(w, id, i, localPool, m, metaKey);
                        break;
                    case META_JUMP:
                        w.printf(".jumpOffset=%d", encodeInt(m.jumpOffset, metaKey, FIELD_JUMP_OFFSET));
                        break;
                    case META_IINC:
                        w.printf(".iincIndex=%d, .iincConst=%d",
                                encodeInt(m.iincIndex, metaKey, FIELD_IINC_INDEX),
                                encodeInt(m.iincConst, metaKey, FIELD_IINC_CONST));
                        break;
                    case META_SWITCH:
                        w.printf(".switchLow=%d, .switchHigh=%d, ",
                                encodeInt(m.switchLow, metaKey, FIELD_SWITCH_LOW),
                                encodeInt(m.switchHigh, metaKey, FIELD_SWITCH_HIGH));
                        w.printf(".switchOffsets=m%d_meta%d_offs", id, i);
                        // Add switchKeys for LOOKUPSWITCH (when keys are present)
                        if (m.switchKeys != null && m.switchKeys.length > 0) {
                            w.printf(", .switchKeys=m%d_meta%d_keys", id, i);
                        }
                        break;
                    case META_TYPE:
                        w.printf(".classIdx=%d, .classLen=%d, .dims=%d",
                                encodeInt(mapStringIndex(localPool, m.classIdx), metaKey, FIELD_CLASS_IDX),
                                encodeInt(m.classLen, metaKey, FIELD_CLASS_LEN),
                                encodeInt(m.dims, metaKey, FIELD_DIMS));
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
            int encodedPcMeta = encodeInt(pc2meta[i], metaKey, FIELD_PC2META_BASE + i);
            w.printf("%d%s", encodedPcMeta, (i < pc2meta.length - 1 ? ", " : ""));
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
    
    private void emitMetaEntry(PrintWriter w, int methodId, int idx, MetaEntry m, int metaKey) {
        if (m.type == MetaType.META_SWITCH && m.switchOffsets != null) {
            // Emit switchOffsets array
            w.printf("static int m%d_meta%d_offs[] = {", methodId, idx);
            for (int i = 0; i < m.switchOffsets.length; i++) {
                int enc = encodeInt(m.switchOffsets[i], metaKey, FIELD_SWITCH_OFFSET_BASE + i);
                w.printf("%d%s", enc, (i < m.switchOffsets.length - 1 ? ", " : ""));
            }
            w.println("};");
            
            // Emit switchKeys array for LOOKUPSWITCH (non-null keys means LOOKUPSWITCH)
            if (m.switchKeys != null && m.switchKeys.length > 0) {
                w.printf("static int m%d_meta%d_keys[] = {", methodId, idx);
                for (int i = 0; i < m.switchKeys.length; i++) {
                    int enc = encodeInt(m.switchKeys[i], metaKey, FIELD_SWITCH_KEY_BASE + i);
                    w.printf("%d%s", enc, (i < m.switchKeys.length - 1 ? ", " : ""));
                }
                w.println("};");
            }
        }
    }

    private void emitMetaDecodeSupport(PrintWriter w) {
        w.println("static const uint8_t vm_meta_type_decode[256] = {");
        for (int i = 0; i < 256; i++) {
            if (i % 16 == 0) {
                w.print("    ");
            }
            w.printf("%d%s", metaTypeDecode[i] & 0xFF, (i < 255 ? ", " : ""));
            if ((i + 1) % 16 == 0) {
                w.println();
            }
        }
        w.println("};");
        w.println();
        w.printf("static const uint32_t VM_META_SALT = 0x%08xu;%n", Integer.toUnsignedLong(metaSalt));
        w.printf("static const uint32_t VM_FIELD_SALT = 0x%08xu;%n", Integer.toUnsignedLong(fieldSalt));
        w.println();

        w.println("static inline uint32_t vm_meta_mix(uint32_t key, uint32_t fieldId) {");
        w.println("    uint32_t fid = fieldId ^ VM_FIELD_SALT;");
        w.println("    uint32_t x = key ^ (VM_META_SALT + fid * 0x9e3779b9u);");
        w.println("    x ^= (x << 13);");
        w.println("    x ^= (x >> 17);");
        w.println("    x ^= (x << 5);");
        w.println("    return x;");
        w.println("}");
        w.println();

        w.println("static inline int vm_meta_dec_i32(int value, uint32_t key, uint32_t fieldId) {");
        w.println("    return value ^ (int)vm_meta_mix(key, fieldId);");
        w.println("}");
        w.println();

        w.println("static inline uint8_t vm_meta_dec_u8(uint8_t value, uint32_t key, uint32_t fieldId) {");
        w.println("    return (uint8_t)(value ^ (uint8_t)(vm_meta_mix(key, fieldId) & 0xFFu));");
        w.println("}");
        w.println();

        w.println("static inline uint64_t vm_meta_mix64(uint32_t key, uint32_t fieldId) {");
        w.println("    uint64_t hi = (uint64_t)vm_meta_mix(key, fieldId);");
        w.println("    uint64_t lo = (uint64_t)vm_meta_mix(key, fieldId ^ 0x7f4a7c15u);");
        w.println("    return (hi << 32) | lo;");
        w.println("}");
        w.println();

        w.println("static inline uint64_t vm_meta_dec_u64(uint64_t value, uint32_t key, uint32_t fieldId) {");
        w.println("    return value ^ vm_meta_mix64(key, fieldId);");
        w.println("}");
        w.println();

        w.println("static void vm_decode_method_meta(VMMethod* m) {");
        w.println("    if (m == NULL || m->metaDecoded) {");
        w.println("        return;");
        w.println("    }");
        w.println("    uint32_t key = m->metaKey;");
        w.println("    m->descIdx = vm_meta_dec_i32(m->descIdx, key, " + FIELD_METHOD_DESC_IDX + "u);");
        w.println("    m->descLen = vm_meta_dec_i32(m->descLen, key, " + FIELD_METHOD_DESC_LEN + "u);");
        w.println("    m->argCount = vm_meta_dec_i32(m->argCount, key, " + FIELD_METHOD_ARG_COUNT + "u);");
        w.println("    m->argTypesIdx = vm_meta_dec_i32(m->argTypesIdx, key, " + FIELD_METHOD_ARG_TYPES_IDX + "u);");
        w.println("    m->returnTypeChar = (char)vm_meta_dec_u8((uint8_t)m->returnTypeChar, key, " + FIELD_METHOD_RETURN_TYPE + "u);");
        w.println("    m->ownerIdx = vm_meta_dec_i32(m->ownerIdx, key, " + FIELD_METHOD_OWNER_IDX + "u);");
        w.println("    m->nameIdx = vm_meta_dec_i32(m->nameIdx, key, " + FIELD_METHOD_NAME_IDX + "u);");
        w.println("    if (m->bytecodeLen > 0 && m->pcToMetaIdx != NULL) {");
        w.println("        for (int pc = 0; pc < m->bytecodeLen; pc++) {");
        w.println("            m->pcToMetaIdx[pc] = vm_meta_dec_i32(m->pcToMetaIdx[pc], key, " + FIELD_PC2META_BASE + "u + (uint32_t)pc);");
        w.println("        }");
        w.println("    }");
        w.println("    if (m->metadata != NULL && m->metadataCount > 0) {");
        w.println("    for (int i = 0; i < m->metadataCount; i++) {");
        w.println("        MetaEntry* me = &m->metadata[i];");
        w.println("        uint8_t rawType = vm_meta_dec_u8((uint8_t)me->type, key, " + FIELD_META_TYPE + "u);");
        w.println("        me->type = (MetaType)vm_meta_type_decode[rawType];");
        w.println("        switch (me->type) {");
        w.println("            case META_INT:");
        w.println("            case META_LOCAL:");
        w.println("            case META_NEWARRAY:");
        w.println("                me->intVal = vm_meta_dec_i32(me->intVal, key, " + FIELD_INT_VAL + "u);");
        w.println("                break;");
        w.println("            case META_LONG: {");
        w.println("                uint64_t v = vm_meta_dec_u64((uint64_t)me->longVal, key, " + FIELD_LONG_VAL + "u);");
        w.println("                me->longVal = (jlong)v;");
        w.println("                break;");
        w.println("            }");
        w.println("            case META_FLOAT: {");
        w.println("                uint32_t bits = (uint32_t)vm_meta_dec_i32(me->intVal, key, " + FIELD_FLOAT_BITS + "u);");
        w.println("                memcpy(&me->floatVal, &bits, sizeof(bits));");
        w.println("                break;");
        w.println("            }");
        w.println("            case META_DOUBLE: {");
        w.println("                uint64_t bits = vm_meta_dec_u64((uint64_t)me->longVal, key, " + FIELD_DOUBLE_BITS + "u);");
        w.println("                memcpy(&me->doubleVal, &bits, sizeof(bits));");
        w.println("                break;");
        w.println("            }");
        w.println("            case META_STRING:");
        w.println("                me->strIdx = vm_meta_dec_i32(me->strIdx, key, " + FIELD_STR_IDX + "u);");
        w.println("                me->strLen = vm_meta_dec_i32(me->strLen, key, " + FIELD_STR_LEN + "u);");
        w.println("                break;");
        w.println("            case META_CLASS:");
        w.println("                me->classIdx = vm_meta_dec_i32(me->classIdx, key, " + FIELD_CLASS_IDX + "u);");
        w.println("                me->classLen = vm_meta_dec_i32(me->classLen, key, " + FIELD_CLASS_LEN + "u);");
        w.println("                break;");
        w.println("            case META_FIELD:");
        w.println("            case META_METHOD:");
        w.println("                me->ownerIdx = vm_meta_dec_i32(me->ownerIdx, key, " + FIELD_OWNER_IDX + "u);");
        w.println("                me->ownerLen = vm_meta_dec_i32(me->ownerLen, key, " + FIELD_OWNER_LEN + "u);");
        w.println("                me->nameIdx = vm_meta_dec_i32(me->nameIdx, key, " + FIELD_NAME_IDX + "u);");
        w.println("                me->nameLen = vm_meta_dec_i32(me->nameLen, key, " + FIELD_NAME_LEN + "u);");
        w.println("                me->descIdx = vm_meta_dec_i32(me->descIdx, key, " + FIELD_DESC_IDX + "u);");
        w.println("                me->descLen = vm_meta_dec_i32(me->descLen, key, " + FIELD_DESC_LEN + "u);");
        w.println("                if (me->handleTag != 0) {");
        w.println("                    me->handleTag = vm_meta_dec_i32(me->handleTag, key, " + FIELD_HANDLE_TAG + "u);");
        w.println("                }");
        w.println("                me->argCount = vm_meta_dec_i32(me->argCount, key, " + FIELD_ARG_COUNT + "u);");
        w.println("                me->returnTypeChar = (char)vm_meta_dec_u8((uint8_t)me->returnTypeChar, key, " + FIELD_RETURN_TYPE + "u);");
        w.println("                me->argTypesIdx = vm_meta_dec_i32(me->argTypesIdx, key, " + FIELD_ARG_TYPES_IDX + "u);");
        w.println("                me->argLocalSlots = vm_meta_dec_i32(me->argLocalSlots, key, " + FIELD_ARG_LOCAL_SLOTS + "u);");
        w.println("                me->argWideMask = vm_meta_dec_u64(me->argWideMask, key, " + FIELD_ARG_WIDE_MASK + "u);");
        w.println("                break;");
        w.println("            case META_INVOKE_DYNAMIC:");
        w.println("                me->bsmIdx = vm_meta_dec_i32(me->bsmIdx, key, " + FIELD_BSM_IDX + "u);");
        w.println("                me->nameIdx = vm_meta_dec_i32(me->nameIdx, key, " + FIELD_NAME_IDX + "u);");
        w.println("                me->nameLen = vm_meta_dec_i32(me->nameLen, key, " + FIELD_NAME_LEN + "u);");
        w.println("                me->descIdx = vm_meta_dec_i32(me->descIdx, key, " + FIELD_DESC_IDX + "u);");
        w.println("                me->descLen = vm_meta_dec_i32(me->descLen, key, " + FIELD_DESC_LEN + "u);");
        w.println("                me->argCount = vm_meta_dec_i32(me->argCount, key, " + FIELD_ARG_COUNT + "u);");
        w.println("                me->returnTypeChar = (char)vm_meta_dec_u8((uint8_t)me->returnTypeChar, key, " + FIELD_RETURN_TYPE + "u);");
        w.println("                me->argTypesIdx = vm_meta_dec_i32(me->argTypesIdx, key, " + FIELD_ARG_TYPES_IDX + "u);");
        w.println("                me->argLocalSlots = vm_meta_dec_i32(me->argLocalSlots, key, " + FIELD_ARG_LOCAL_SLOTS + "u);");
        w.println("                me->argWideMask = vm_meta_dec_u64(me->argWideMask, key, " + FIELD_ARG_WIDE_MASK + "u);");
        w.println("                break;");
        w.println("            case META_JUMP:");
        w.println("                me->jumpOffset = vm_meta_dec_i32(me->jumpOffset, key, " + FIELD_JUMP_OFFSET + "u);");
        w.println("                break;");
        w.println("            case META_IINC:");
        w.println("                me->iincIndex = vm_meta_dec_i32(me->iincIndex, key, " + FIELD_IINC_INDEX + "u);");
        w.println("                me->iincConst = vm_meta_dec_i32(me->iincConst, key, " + FIELD_IINC_CONST + "u);");
        w.println("                break;");
        w.println("            case META_SWITCH: {");
        w.println("                me->switchLow = vm_meta_dec_i32(me->switchLow, key, " + FIELD_SWITCH_LOW + "u);");
        w.println("                me->switchHigh = vm_meta_dec_i32(me->switchHigh, key, " + FIELD_SWITCH_HIGH + "u);");
        w.println("                int offsetCount = 0;");
        w.println("                if (me->switchKeys != NULL) {");
        w.println("                    int npairs = me->switchLow;");
        w.println("                    if (npairs < 0) npairs = 0;");
        w.println("                    offsetCount = npairs + 1;");
        w.println("                    for (int k = 0; k < npairs; k++) {");
        w.println("                        me->switchKeys[k] = vm_meta_dec_i32(me->switchKeys[k], key, " + FIELD_SWITCH_KEY_BASE + "u + (uint32_t)k);");
        w.println("                    }");
        w.println("                } else {");
        w.println("                    int span = me->switchHigh - me->switchLow + 1;");
        w.println("                    if (span < 0) span = 0;");
        w.println("                    offsetCount = span + 1;");
        w.println("                }");
        w.println("                if (me->switchOffsets != NULL) {");
        w.println("                    for (int k = 0; k < offsetCount; k++) {");
        w.println("                        me->switchOffsets[k] = vm_meta_dec_i32(me->switchOffsets[k], key, " + FIELD_SWITCH_OFFSET_BASE + "u + (uint32_t)k);");
        w.println("                    }");
        w.println("                }");
        w.println("                break;");
        w.println("            }");
        w.println("            case META_TYPE:");
        w.println("                me->classIdx = vm_meta_dec_i32(me->classIdx, key, " + FIELD_CLASS_IDX + "u);");
        w.println("                me->classLen = vm_meta_dec_i32(me->classLen, key, " + FIELD_CLASS_LEN + "u);");
        w.println("                me->dims = vm_meta_dec_i32(me->dims, key, " + FIELD_DIMS + "u);");
        w.println("                break;");
        w.println("            default:");
        w.println("                break;");
        w.println("        }");
        w.println("    }");
        w.println("    }");
        w.println("    m->metaDecoded = 1;");
        w.println("    m->metaKey = 0;");
        w.println("}");
        w.println();

        w.println("void vm_init_meta_all(void) {");
        w.println("    for (int i = 0; i < vm_method_count; i++) {");
        w.println("        vm_decode_method_meta(&vm_methods[i]);");
        w.println("    }");
        w.println("}");
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
    
}
