package com.alphaautoleak.jnvm.codegen.emitter;

import java.io.PrintWriter;

final class VmMetaDecodeEmitter {

    private VmMetaDecodeEmitter() {
    }

    static void emit(PrintWriter w,
                     int[] metaTypeDecode,
                     int metaSalt,
                     int fieldSalt,
                     int fieldMethodDescIdx,
                     int fieldMethodDescLen,
                     int fieldMethodArgCount,
                     int fieldMethodArgTypesIdx,
                     int fieldMethodReturnType,
                     int fieldMethodOwnerIdx,
                     int fieldMethodNameIdx,
                     int fieldPc2MetaBase,
                     int fieldMetaType,
                     int fieldIntVal,
                     int fieldLongVal,
                     int fieldFloatBits,
                     int fieldDoubleBits,
                     int fieldStrIdx,
                     int fieldStrLen,
                     int fieldClassIdx,
                     int fieldClassLen,
                     int fieldOwnerIdx,
                     int fieldOwnerLen,
                     int fieldNameIdx,
                     int fieldNameLen,
                     int fieldDescIdx,
                     int fieldDescLen,
                     int fieldHandleTag,
                     int fieldArgCount,
                     int fieldReturnType,
                     int fieldArgTypesIdx,
                     int fieldArgLocalSlots,
                     int fieldArgWideMask,
                     int fieldArgPopMapBase,
                     int fieldBsmIdx,
                     int fieldJumpOffset,
                     int fieldIincIndex,
                     int fieldIincConst,
                     int fieldSwitchLow,
                     int fieldSwitchHigh,
                     int fieldSwitchKeyBase,
                     int fieldSwitchOffsetBase,
                     int fieldDims) {
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
        w.println("    m->descIdx = vm_meta_dec_i32(m->descIdx, key, " + fieldMethodDescIdx + "u);");
        w.println("    m->descLen = vm_meta_dec_i32(m->descLen, key, " + fieldMethodDescLen + "u);");
        w.println("    m->argCount = vm_meta_dec_i32(m->argCount, key, " + fieldMethodArgCount + "u);");
        w.println("    m->argTypesIdx = vm_meta_dec_i32(m->argTypesIdx, key, " + fieldMethodArgTypesIdx + "u);");
        w.println("    m->returnTypeChar = (char)vm_meta_dec_u8((uint8_t)m->returnTypeChar, key, " + fieldMethodReturnType + "u);");
        w.println("    m->ownerIdx = vm_meta_dec_i32(m->ownerIdx, key, " + fieldMethodOwnerIdx + "u);");
        w.println("    m->nameIdx = vm_meta_dec_i32(m->nameIdx, key, " + fieldMethodNameIdx + "u);");
        w.println("    if (m->bytecodeLen > 0 && m->pcToMetaIdx != NULL) {");
        w.println("        for (int pc = 0; pc < m->bytecodeLen; pc++) {");
        w.println("            m->pcToMetaIdx[pc] = vm_meta_dec_i32(m->pcToMetaIdx[pc], key, " + fieldPc2MetaBase + "u + (uint32_t)pc);");
        w.println("        }");
        w.println("    }");
        w.println("    if (m->metadata != NULL && m->metadataCount > 0) {");
        w.println("    for (int i = 0; i < m->metadataCount; i++) {");
        w.println("        MetaEntry* me = &m->metadata[i];");
        w.println("        uint8_t rawType = vm_meta_dec_u8((uint8_t)me->type, key, " + fieldMetaType + "u);");
        w.println("        me->type = (MetaType)vm_meta_type_decode[rawType];");
        w.println("        switch (me->type) {");
        w.println("            case META_INT:");
        w.println("            case META_LOCAL:");
        w.println("            case META_NEWARRAY:");
        w.println("                me->intVal = vm_meta_dec_i32(me->intVal, key, " + fieldIntVal + "u);");
        w.println("                break;");
        w.println("            case META_LONG: {");
        w.println("                uint64_t v = vm_meta_dec_u64((uint64_t)me->longVal, key, " + fieldLongVal + "u);");
        w.println("                me->longVal = (jlong)v;");
        w.println("                break;");
        w.println("            }");
        w.println("            case META_FLOAT: {");
        w.println("                uint32_t bits = (uint32_t)vm_meta_dec_i32(me->intVal, key, " + fieldFloatBits + "u);");
        w.println("                memcpy(&me->floatVal, &bits, sizeof(bits));");
        w.println("                break;");
        w.println("            }");
        w.println("            case META_DOUBLE: {");
        w.println("                uint64_t bits = vm_meta_dec_u64((uint64_t)me->longVal, key, " + fieldDoubleBits + "u);");
        w.println("                memcpy(&me->doubleVal, &bits, sizeof(bits));");
        w.println("                break;");
        w.println("            }");
        w.println("            case META_STRING:");
        w.println("                me->strIdx = vm_meta_dec_i32(me->strIdx, key, " + fieldStrIdx + "u);");
        w.println("                me->strLen = vm_meta_dec_i32(me->strLen, key, " + fieldStrLen + "u);");
        w.println("                break;");
        w.println("            case META_CLASS:");
        w.println("                me->classIdx = vm_meta_dec_i32(me->classIdx, key, " + fieldClassIdx + "u);");
        w.println("                me->classLen = vm_meta_dec_i32(me->classLen, key, " + fieldClassLen + "u);");
        w.println("                break;");
        w.println("            case META_FIELD:");
        w.println("            case META_METHOD:");
        w.println("                me->ownerIdx = vm_meta_dec_i32(me->ownerIdx, key, " + fieldOwnerIdx + "u);");
        w.println("                me->ownerLen = vm_meta_dec_i32(me->ownerLen, key, " + fieldOwnerLen + "u);");
        w.println("                me->nameIdx = vm_meta_dec_i32(me->nameIdx, key, " + fieldNameIdx + "u);");
        w.println("                me->nameLen = vm_meta_dec_i32(me->nameLen, key, " + fieldNameLen + "u);");
        w.println("                me->descIdx = vm_meta_dec_i32(me->descIdx, key, " + fieldDescIdx + "u);");
        w.println("                me->descLen = vm_meta_dec_i32(me->descLen, key, " + fieldDescLen + "u);");
        w.println("                if (me->handleTag != 0) {");
        w.println("                    me->handleTag = vm_meta_dec_i32(me->handleTag, key, " + fieldHandleTag + "u);");
        w.println("                }");
        w.println("                me->argCount = vm_meta_dec_i32(me->argCount, key, " + fieldArgCount + "u);");
        w.println("                me->returnTypeChar = (char)vm_meta_dec_u8((uint8_t)me->returnTypeChar, key, " + fieldReturnType + "u);");
        w.println("                me->argTypesIdx = vm_meta_dec_i32(me->argTypesIdx, key, " + fieldArgTypesIdx + "u);");
        w.println("                me->argLocalSlots = vm_meta_dec_i32(me->argLocalSlots, key, " + fieldArgLocalSlots + "u);");
        w.println("                me->argWideMask = vm_meta_dec_u64(me->argWideMask, key, " + fieldArgWideMask + "u);");
        w.println("                if (me->argPopMap != NULL && me->argCount > 0) {");
        w.println("                    for (int k = 0; k < me->argCount; k++) {");
        w.println("                        me->argPopMap[k] = vm_meta_dec_i32(me->argPopMap[k], key, " + fieldArgPopMapBase + "u + (uint32_t)k);");
        w.println("                    }");
        w.println("                }");
        w.println("                break;");
        w.println("            case META_INVOKE_DYNAMIC:");
        w.println("                me->bsmIdx = vm_meta_dec_i32(me->bsmIdx, key, " + fieldBsmIdx + "u);");
        w.println("                me->nameIdx = vm_meta_dec_i32(me->nameIdx, key, " + fieldNameIdx + "u);");
        w.println("                me->nameLen = vm_meta_dec_i32(me->nameLen, key, " + fieldNameLen + "u);");
        w.println("                me->descIdx = vm_meta_dec_i32(me->descIdx, key, " + fieldDescIdx + "u);");
        w.println("                me->descLen = vm_meta_dec_i32(me->descLen, key, " + fieldDescLen + "u);");
        w.println("                me->argCount = vm_meta_dec_i32(me->argCount, key, " + fieldArgCount + "u);");
        w.println("                me->returnTypeChar = (char)vm_meta_dec_u8((uint8_t)me->returnTypeChar, key, " + fieldReturnType + "u);");
        w.println("                me->argTypesIdx = vm_meta_dec_i32(me->argTypesIdx, key, " + fieldArgTypesIdx + "u);");
        w.println("                me->argLocalSlots = vm_meta_dec_i32(me->argLocalSlots, key, " + fieldArgLocalSlots + "u);");
        w.println("                me->argWideMask = vm_meta_dec_u64(me->argWideMask, key, " + fieldArgWideMask + "u);");
        w.println("                if (me->argPopMap != NULL && me->argCount > 0) {");
        w.println("                    for (int k = 0; k < me->argCount; k++) {");
        w.println("                        me->argPopMap[k] = vm_meta_dec_i32(me->argPopMap[k], key, " + fieldArgPopMapBase + "u + (uint32_t)k);");
        w.println("                    }");
        w.println("                }");
        w.println("                break;");
        w.println("            case META_JUMP:");
        w.println("                me->jumpOffset = vm_meta_dec_i32(me->jumpOffset, key, " + fieldJumpOffset + "u);");
        w.println("                break;");
        w.println("            case META_IINC:");
        w.println("                me->iincIndex = vm_meta_dec_i32(me->iincIndex, key, " + fieldIincIndex + "u);");
        w.println("                me->iincConst = vm_meta_dec_i32(me->iincConst, key, " + fieldIincConst + "u);");
        w.println("                break;");
        w.println("            case META_SWITCH: {");
        w.println("                me->switchLow = vm_meta_dec_i32(me->switchLow, key, " + fieldSwitchLow + "u);");
        w.println("                me->switchHigh = vm_meta_dec_i32(me->switchHigh, key, " + fieldSwitchHigh + "u);");
        w.println("                int offsetCount = 0;");
        w.println("                if (me->switchKeys != NULL) {");
        w.println("                    int npairs = me->switchLow;");
        w.println("                    if (npairs < 0) npairs = 0;");
        w.println("                    offsetCount = npairs + 1;");
        w.println("                    for (int k = 0; k < npairs; k++) {");
        w.println("                        me->switchKeys[k] = vm_meta_dec_i32(me->switchKeys[k], key, " + fieldSwitchKeyBase + "u + (uint32_t)k);");
        w.println("                    }");
        w.println("                } else {");
        w.println("                    int span = me->switchHigh - me->switchLow + 1;");
        w.println("                    if (span < 0) span = 0;");
        w.println("                    offsetCount = span + 1;");
        w.println("                }");
        w.println("                if (me->switchOffsets != NULL) {");
        w.println("                    for (int k = 0; k < offsetCount; k++) {");
        w.println("                        me->switchOffsets[k] = vm_meta_dec_i32(me->switchOffsets[k], key, " + fieldSwitchOffsetBase + "u + (uint32_t)k);");
        w.println("                    }");
        w.println("                }");
        w.println("                break;");
        w.println("            }");
        w.println("            case META_TYPE:");
        w.println("                me->classIdx = vm_meta_dec_i32(me->classIdx, key, " + fieldClassIdx + "u);");
        w.println("                me->classLen = vm_meta_dec_i32(me->classLen, key, " + fieldClassLen + "u);");
        w.println("                me->dims = vm_meta_dec_i32(me->dims, key, " + fieldDims + "u);");
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
}
