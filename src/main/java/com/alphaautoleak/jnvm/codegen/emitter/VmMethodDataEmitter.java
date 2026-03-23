package com.alphaautoleak.jnvm.codegen.emitter;

import com.alphaautoleak.jnvm.cli.CliReporter;

import com.alphaautoleak.jnvm.asm.BootstrapEntry;
import com.alphaautoleak.jnvm.asm.ExceptionEntry;
import com.alphaautoleak.jnvm.asm.MetaEntry;
import com.alphaautoleak.jnvm.asm.MetaType;
import com.alphaautoleak.jnvm.crypto.EncryptedMethodData;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

final class VmMethodDataEmitter {

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

    private final Map<String, Integer> globalStringIndexMap;
    private final Map<String, MethodDescriptorParser.DescriptorInfo> invokeMetaCache;
    private final Map<String, Integer> bootstrapIndexMap;
    private final Map<Integer, Integer> methodMetaKeys;
    private final int[] metaTypeEncode;
    private final int metaSalt;
    private final int fieldSalt;

    VmMethodDataEmitter(Map<String, Integer> globalStringIndexMap,
                        Map<String, MethodDescriptorParser.DescriptorInfo> invokeMetaCache,
                        Map<String, Integer> bootstrapIndexMap,
                        Map<Integer, Integer> methodMetaKeys,
                        int[] metaTypeEncode,
                        int metaSalt,
                        int fieldSalt) {
        this.globalStringIndexMap = globalStringIndexMap;
        this.invokeMetaCache = invokeMetaCache;
        this.bootstrapIndexMap = bootstrapIndexMap;
        this.methodMetaKeys = methodMetaKeys;
        this.metaTypeEncode = metaTypeEncode;
        this.metaSalt = metaSalt;
        this.fieldSalt = fieldSalt;
    }

    void emitMethodData(PrintWriter w, EncryptedMethodData method) {
        int id = method.getMethodId();
        int metaKey = methodMetaKeys.getOrDefault(id, 0);
        List<String> localPool = method.getStringPool();

        w.printf("static const uint8_t m%d_bc[] = {", id);
        byte[] bc = method.getEncryptedBytecode();
        for (int i = 0; i < bc.length; i++) {
            if (i % 16 == 0) {
                w.printf("\n    ");
            }
            w.printf("0x%02x%s", bc[i] & 0xFF, (i < bc.length - 1 ? ", " : ""));
        }
        w.println("\n};");
        w.println();

        List<MetaEntry> metaList = method.getMetadata();
        if (!metaList.isEmpty()) {
            for (int i = 0; i < metaList.size(); i++) {
                emitMetaEntry(w, id, i, metaList.get(i), metaKey, localPool);
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
                        emitOwnerNameDesc(w, m, metaKey, localPool);
                        break;
                    case META_METHOD:
                        emitOwnerNameDesc(w, m, metaKey, localPool);
                        if (m.handleTag > 0) {
                            w.printf(", .handleTag=%d", encodeInt(m.handleTag, metaKey, FIELD_HANDLE_TAG));
                        }
                        emitInvokeMetaSuffix(w, id, i, localPool, m, metaKey);
                        MethodDescriptorParser.DescriptorInfo methodInfo = resolveInvokeMetaInfo(id, i, localPool, m);
                        if (methodInfo != null && methodInfo.getArgCount() > 0) {
                            w.printf(", .argPopMap=m%d_meta%d_argpop", id, i);
                        }
                        break;
                    case META_INVOKE_DYNAMIC:
                        int globalBsmIdx = mapBsmIndex(method.getBootstrapMethods(), m.bsmIdx);
                        w.printf(".nameIdx=%d, .nameLen=%d, ",
                                encodeInt(mapStringIndex(localPool, m.nameIdx), metaKey, FIELD_NAME_IDX),
                                encodeInt(m.nameLen, metaKey, FIELD_NAME_LEN));
                        w.printf(".descIdx=%d, .descLen=%d",
                                encodeInt(mapStringIndex(localPool, m.descIdx), metaKey, FIELD_DESC_IDX),
                                encodeInt(m.descLen, metaKey, FIELD_DESC_LEN));
                        w.printf(", .bsmIdx=%d", encodeInt(globalBsmIdx, metaKey, FIELD_BSM_IDX));
                        emitInvokeMetaSuffix(w, id, i, localPool, m, metaKey);
                        MethodDescriptorParser.DescriptorInfo indyInfo = resolveInvokeMetaInfo(id, i, localPool, m);
                        if (indyInfo != null && indyInfo.getArgCount() > 0) {
                            w.printf(", .argPopMap=m%d_meta%d_argpop", id, i);
                        }
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

        int[] pc2meta = method.getPcToMetaIdx();
        w.printf("static int m%d_pc2meta[] = {", id);
        for (int i = 0; i < pc2meta.length; i++) {
            if (i % 32 == 0) {
                w.printf("\n    ");
            }
            int encodedPcMeta = encodeInt(pc2meta[i], metaKey, FIELD_PC2META_BASE + i);
            w.printf("%d%s", encodedPcMeta, (i < pc2meta.length - 1 ? ", " : ""));
        }
        w.println("\n};");
        w.println();

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

    private void emitOwnerNameDesc(PrintWriter w, MetaEntry m, int metaKey, List<String> localPool) {
        w.printf(".ownerIdx=%d, .ownerLen=%d, ",
                encodeInt(mapStringIndex(localPool, m.ownerIdx), metaKey, FIELD_OWNER_IDX),
                encodeInt(m.ownerLen, metaKey, FIELD_OWNER_LEN));
        w.printf(".nameIdx=%d, .nameLen=%d, ",
                encodeInt(mapStringIndex(localPool, m.nameIdx), metaKey, FIELD_NAME_IDX),
                encodeInt(m.nameLen, metaKey, FIELD_NAME_LEN));
        w.printf(".descIdx=%d, .descLen=%d",
                encodeInt(mapStringIndex(localPool, m.descIdx), metaKey, FIELD_DESC_IDX),
                encodeInt(m.descLen, metaKey, FIELD_DESC_LEN));
    }

    private void emitMetaEntry(PrintWriter w, int methodId, int idx, MetaEntry m, int metaKey, List<String> localPool) {
        if (m.type == MetaType.META_SWITCH && m.switchOffsets != null) {
            w.printf("static int m%d_meta%d_offs[] = {", methodId, idx);
            for (int i = 0; i < m.switchOffsets.length; i++) {
                int enc = encodeInt(m.switchOffsets[i], metaKey, FIELD_SWITCH_OFFSET_BASE + i);
                w.printf("%d%s", enc, (i < m.switchOffsets.length - 1 ? ", " : ""));
            }
            w.println("};");

            if (m.switchKeys != null && m.switchKeys.length > 0) {
                w.printf("static int m%d_meta%d_keys[] = {", methodId, idx);
                for (int i = 0; i < m.switchKeys.length; i++) {
                    int enc = encodeInt(m.switchKeys[i], metaKey, FIELD_SWITCH_KEY_BASE + i);
                    w.printf("%d%s", enc, (i < m.switchKeys.length - 1 ? ", " : ""));
                }
                w.println("};");
            }
        }
        if ((m.type == MetaType.META_METHOD || m.type == MetaType.META_INVOKE_DYNAMIC) && localPool != null) {
            MethodDescriptorParser.DescriptorInfo info = resolveInvokeMetaInfo(methodId, idx, localPool, m);
            if (info != null && info.getArgCount() > 0) {
                String argTypes = info.getArgTypes();
                if (argTypes != null && !argTypes.isEmpty()) {
                    int argLocalSlots = 0;
                    for (int i = 0; i < argTypes.length(); i++) {
                        char t = argTypes.charAt(i);
                        argLocalSlots += (t == 'J' || t == 'D') ? 2 : 1;
                    }
                    w.printf("static int m%d_meta%d_argpop[] = {", methodId, idx);
                    int cursor = argLocalSlots;
                    int popIdx = 0;
                    for (int i = argTypes.length() - 1; i >= 0; i--) {
                        char t = argTypes.charAt(i);
                        cursor -= (t == 'J' || t == 'D') ? 2 : 1;
                        int encoded = encodeInt(cursor, metaKey, FIELD_ARG_POP_MAP_BASE + popIdx);
                        w.printf("%d%s", encoded, (i > 0 ? ", " : ""));
                        popIdx++;
                    }
                    w.println("};");
                }
            }
        }
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

    private int getOrAddStringIndex(String s) {
        Integer idx = globalStringIndexMap.get(s);
        if (idx != null) {
            return idx;
        }
        CliReporter.warn("String not found in global pool: " + s);
        return 0;
    }

    private int mapStringIndex(List<String> localPool, int localIdx) {
        if (localPool == null || localIdx < 0 || localIdx >= localPool.size()) {
            return localIdx;
        }
        String str = localPool.get(localIdx);
        Integer globalIdx = globalStringIndexMap.get(str);
        if (globalIdx == null) {
            return localIdx;
        }
        return globalIdx;
    }

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
        keyBuilder.append(bsm.getHandleOwner()).append('.');
        keyBuilder.append(bsm.getHandleName()).append(bsm.getHandleDescriptor());
        if (bsm.getArguments() != null) {
            for (Object arg : bsm.getArguments()) {
                keyBuilder.append('|').append(arg != null ? arg.toString() : "null");
            }
        }
        return keyBuilder.toString();
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
}
