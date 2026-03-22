package com.alphaautoleak.jnvm.codegen.emitter;

import com.alphaautoleak.jnvm.asm.BootstrapEntry;
import com.alphaautoleak.jnvm.asm.ExceptionEntry;
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

    static final class MethodRoute {
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
    private static final int FIELD_ARG_POP_MAP_BASE = 5000;
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

    private String buildBootstrapKey(BootstrapEntry bsm) {
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(bsm.getHandleOwner()).append('.');
        keyBuilder.append(bsm.getHandleName()).append(bsm.getHandleDescriptor());
        if (bsm.getArguments() != null) {
            for (Object arg : bsm.getArguments()) {
                keyBuilder.append('|').append(arg != null ? arg.toString() : "null");
            }
        }
        return keyBuilder.toString();
    }
    
    private void generateHeader() throws IOException {
        VmDataHeaderEmitter.emit(dir, encryptStrings);
    }
    
    private void generateSource() throws IOException {
        try (PrintWriter w = new PrintWriter(new java.io.FileWriter(new File(dir, "vm_data.c")))) {
            w.println("#include \"vm_data.h\"");
            w.println("#include \"chacha20.h\"");
            w.println();

            emitVmKey(w);
            VmDataStringCollector.CollectedStrings collectedStrings =
                    VmDataStringCollector.collect(methods, globalBootstrapMethods);
            Set<String> allStrings = collectedStrings.getAllStrings();
            globalStringIndexMap = collectedStrings.getGlobalStringIndexMap();
            invokeMetaCache = collectedStrings.getInvokeMetaCache();
            emitStringPool(w, allStrings);

            emitGlobalCounts(w, allStrings.size());
            bootstrapMethodsEmitter.emit(w);

            VmMethodDataEmitter methodDataEmitter = new VmMethodDataEmitter(
                    globalStringIndexMap,
                    invokeMetaCache,
                    bootstrapIndexMap,
                    methodMetaKeys,
                    metaTypeEncode,
                    metaSalt,
                    fieldSalt
            );
            VmMethodTableEmitter methodTableEmitter = new VmMethodTableEmitter(
                    methods,
                    methodSegments,
                    methodMetaKeys,
                    methodRoutesById,
                    globalStringIndexMap,
                    methodRouteKey,
                    metaSalt,
                    fieldSalt
            );

            for (EncryptedMethodData method : methods) {
                methodDataEmitter.emitMethodData(w, method);
            }

            methodTableEmitter.emit(w);
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

    private void emitGlobalCounts(PrintWriter w, int stringCount) {
        w.println("const int vm_method_count = " + methods.size() + ";");
        w.println("const int vm_string_count = " + stringCount + ";");
        w.println("const int vm_bootstrap_count = " + globalBootstrapMethods.size() + ";");
        w.println();
    }

    private void emitStringPool(PrintWriter w, Set<String> strings) {
        VmStringPoolEmitter.emit(w, strings, encryptStrings, vmStringKey, stringNonce);
    }

    private int getOrAddStringIndex(String s) {
        Integer idx = globalStringIndexMap.get(s);
        if (idx != null) {
            return idx;
        }
        System.err.println("[WARN] String not found in global pool: " + s);
        return 0;
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

    private void emitMetaDecodeSupport(PrintWriter w) {
        VmMetaDecodeEmitter.emit(
                w,
                metaTypeDecode,
                metaSalt,
                fieldSalt,
                FIELD_METHOD_DESC_IDX,
                FIELD_METHOD_DESC_LEN,
                FIELD_METHOD_ARG_COUNT,
                FIELD_METHOD_ARG_TYPES_IDX,
                FIELD_METHOD_RETURN_TYPE,
                FIELD_METHOD_OWNER_IDX,
                FIELD_METHOD_NAME_IDX,
                FIELD_PC2META_BASE,
                FIELD_META_TYPE,
                FIELD_INT_VAL,
                FIELD_LONG_VAL,
                FIELD_FLOAT_BITS,
                FIELD_DOUBLE_BITS,
                FIELD_STR_IDX,
                FIELD_STR_LEN,
                FIELD_CLASS_IDX,
                FIELD_CLASS_LEN,
                FIELD_OWNER_IDX,
                FIELD_OWNER_LEN,
                FIELD_NAME_IDX,
                FIELD_NAME_LEN,
                FIELD_DESC_IDX,
                FIELD_DESC_LEN,
                FIELD_HANDLE_TAG,
                FIELD_ARG_COUNT,
                FIELD_RETURN_TYPE,
                FIELD_ARG_TYPES_IDX,
                FIELD_ARG_LOCAL_SLOTS,
                FIELD_ARG_WIDE_MASK,
                FIELD_ARG_POP_MAP_BASE,
                FIELD_BSM_IDX,
                FIELD_JUMP_OFFSET,
                FIELD_IINC_INDEX,
                FIELD_IINC_CONST,
                FIELD_SWITCH_LOW,
                FIELD_SWITCH_HIGH,
                FIELD_SWITCH_KEY_BASE,
                FIELD_SWITCH_OFFSET_BASE,
                FIELD_DIMS
        );
    }
    
}
