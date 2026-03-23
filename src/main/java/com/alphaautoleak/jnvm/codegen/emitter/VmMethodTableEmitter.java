package com.alphaautoleak.jnvm.codegen.emitter;

import com.alphaautoleak.jnvm.cli.CliReporter;

import com.alphaautoleak.jnvm.asm.ExceptionEntry;
import com.alphaautoleak.jnvm.crypto.EncryptedMethodData;

import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

final class VmMethodTableEmitter {

    private static final int FIELD_METHOD_DESC_IDX = 900;
    private static final int FIELD_METHOD_DESC_LEN = 901;
    private static final int FIELD_METHOD_ARG_COUNT = 902;
    private static final int FIELD_METHOD_ARG_TYPES_IDX = 903;
    private static final int FIELD_METHOD_RETURN_TYPE = 904;
    private static final int FIELD_METHOD_OWNER_IDX = 905;
    private static final int FIELD_METHOD_NAME_IDX = 906;

    private final List<EncryptedMethodData> methods;
    private final List<List<EncryptedMethodData>> methodSegments;
    private final Map<Integer, Integer> methodMetaKeys;
    private final Map<Integer, VmDataGenerator.MethodRoute> methodRoutesById;
    private final Map<String, Integer> globalStringIndexMap;
    private final int methodRouteKey;
    private final int metaSalt;
    private final int fieldSalt;

    VmMethodTableEmitter(List<EncryptedMethodData> methods,
                         List<List<EncryptedMethodData>> methodSegments,
                         Map<Integer, Integer> methodMetaKeys,
                         Map<Integer, VmDataGenerator.MethodRoute> methodRoutesById,
                         Map<String, Integer> globalStringIndexMap,
                         int methodRouteKey,
                         int metaSalt,
                         int fieldSalt) {
        this.methods = methods;
        this.methodSegments = methodSegments;
        this.methodMetaKeys = methodMetaKeys;
        this.methodRoutesById = methodRoutesById;
        this.globalStringIndexMap = globalStringIndexMap;
        this.methodRouteKey = methodRouteKey;
        this.metaSalt = metaSalt;
        this.fieldSalt = fieldSalt;
    }

    void emit(PrintWriter w) {
        emitSegmentArrays(w);
        emitMethodStorage(w);
        int[] encodedRoutes = buildEncodedRoutes();
        emitRouteTable(w, encodedRoutes);
        emitRouteHelpers(w);
        emitInitMethodTable(w);
    }

    private void emitSegmentArrays(PrintWriter w) {
        for (int seg = 0; seg < methodSegments.size(); seg++) {
            List<EncryptedMethodData> segMethods = methodSegments.get(seg);
            w.printf("static VMMethod vm_method_seg_%d[] = {", seg);
            for (EncryptedMethodData method : segMethods) {
                emitSingleMethodLiteral(w, method);
            }
            w.println("\n};");
            w.println();
        }
    }

    private void emitMethodStorage(PrintWriter w) {
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
    }

    private int[] buildEncodedRoutes() {
        int[] encodedRoutes = new int[methods.size()];
        for (int i = 0; i < encodedRoutes.length; i++) {
            encodedRoutes[i] = encodeMethodRoute(0, i);
        }
        for (EncryptedMethodData method : methods) {
            int methodId = method.getMethodId();
            if (methodId < 0 || methodId >= encodedRoutes.length) {
                continue;
            }
            VmDataGenerator.MethodRoute route = methodRoutesById.get(methodId);
            if (route == null) {
                continue;
            }
            encodedRoutes[methodId] = route.encodedRoute;
        }
        return encodedRoutes;
    }

    private void emitRouteTable(PrintWriter w, int[] encodedRoutes) {
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
    }

    private void emitRouteHelpers(PrintWriter w) {
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
    }

    private void emitInitMethodTable(PrintWriter w) {
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
            w.printf(".exceptionTable=m%d_exc, .exceptionTableLength=%d, ", method.getMethodId(), excTable.size());
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

    private int getOrAddStringIndex(String s) {
        Integer idx = globalStringIndexMap.get(s);
        if (idx != null) {
            return idx;
        }
        CliReporter.warn("String not found in global pool: " + s);
        return 0;
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

    private int encodeInt(int value, int metaKey, int fieldId) {
        return value ^ metaMix(metaKey, fieldId);
    }

    private int encodeByte(int value, int metaKey, int fieldId) {
        return (value ^ (metaMix(metaKey, fieldId) & 0xFF)) & 0xFF;
    }

    private int metaMix(int metaKey, int fieldId) {
        int fid = fieldId ^ fieldSalt;
        int x = metaKey ^ (metaSalt + fid * 0x9e3779b9);
        x ^= (x << 13);
        x ^= (x >>> 17);
        x ^= (x << 5);
        return x;
    }
}
