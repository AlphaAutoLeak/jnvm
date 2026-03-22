package com.alphaautoleak.jnvm.codegen.emitter;

import com.alphaautoleak.jnvm.crypto.OpcodeObfuscator;

import java.io.PrintWriter;

final class VmInterpreterCoreEmitter {

    private final Instruction[] instructionByOpcode;
    private final OpcodeObfuscator opcodeObfuscator;

    VmInterpreterCoreEmitter(Instruction[] instructionByOpcode, OpcodeObfuscator opcodeObfuscator) {
        this.instructionByOpcode = instructionByOpcode;
        this.opcodeObfuscator = opcodeObfuscator;
    }

    void emitExecuteCommon(PrintWriter w) {
        emitExecuteCommonPrelude(w);
        emitNeedsMetaTable(w);
        emitDispatchTable(w);
        emitDispatchPrologue(w);
        emitInstructionHandlers(w);
        emitExecuteCommonEpilogue(w);
    }

    private void emitExecuteCommonPrelude(PrintWriter w) {
        w.println("__attribute__((hot))");
        w.println("ExecuteResult vm_execute_common(JNIEnv* env, int methodId, jobjectArray args, VMValue* directLocals, int directLocalSlots, jclass callerClass) {");
        w.println("    frame_pool_ensure_init();");
        w.println("    ExecuteResult execResult = { .returnType = 'V' };");
        w.println("    methodId ^= METHOD_ID_XOR_KEY;");
        w.println("    if ((unsigned int)methodId >= (unsigned int)vm_method_count) {");
        w.println("        execResult.returnType = 'E';  // Error");
        w.println("        return execResult;");
        w.println("    }");
        w.println("    VMMethod* m = &vm_methods[methodId];");
        w.println();
        w.println("    uint8_t* bytecode = m->bytecode;");
        w.println("    int* pcToMetaIdx = m->pcToMetaIdx;");
        w.println("    MetaEntry* metaTable = m->metadata;");
        w.println();
        w.println("    jobject instance = NULL;");
        w.println("    jsize argsLen = 0;");
        w.println("    if (!directLocals && args) {");
        w.println("        argsLen = (*env)->GetArrayLength(env, args);");
        w.println("        if (!m->isStatic && argsLen > 0) {");
        w.println("            instance = (*env)->GetObjectArrayElement(env, args, 0);");
        w.println("        }");
        w.println("        callerClass = (argsLen > 1 && m->argCount + 1 < argsLen) ?");
        w.println("            (jclass)(*env)->GetObjectArrayElement(env, args, argsLen - 1) : callerClass;");
        w.println("    }");
        w.println();
        w.println("    VMFrame frame = { .pc = 0, .sp = 0, .callerClass = callerClass };");
        w.println("    frame.stack = frame_pool_push(m->maxStack);");
        w.println();
        w.println("    if (directLocals) {");
        w.println("        frame.locals = directLocals;  // reuse caller's buffer directly (zero copy)");
        w.println("    } else {");
        w.println("        frame.locals = frame_pool_push(m->maxLocals);");
        w.println("        memset(frame.locals, 0, m->maxLocals * sizeof(VMValue));");
        w.println("        frame.locals[0].l = instance;");
        w.println("        const char* argTypes = m->argTypesStr ? m->argTypesStr : ((m->argTypesIdx >= 0) ? vm_get_string(m->argTypesIdx) : NULL);");
        w.println("        // Unbox args[1..n] (skip args[0]=instance, skip last element=callerClass)");
        w.println("        vm_unbox_args_fast(env, &frame, args, argTypes, m->argCount, m->isStatic ? 0 : 1, argsLen > 1 ? argsLen - 1 : 1, argsLen);");
        w.println("    }");
        w.println();
    }

    private void emitNeedsMetaTable(PrintWriter w) {
        w.println("    // Metadata requirement table (indexed by obfuscated opcode)");
        w.println("    static const uint8_t needs_meta[256] = {");
        StringBuilder metaTable = new StringBuilder();
        for (int i = 0; i < 256; i++) {
            int originalOp = opcodeObfuscator.decode(i);
            Instruction inst = getInstructionByOriginalOpcode(originalOp);
            boolean needsMeta = inst != null && inst.needsMeta();
            if (i % 32 == 0) {
                metaTable.append("        ");
            }
            metaTable.append(needsMeta ? "1" : "0");
            metaTable.append(i < 255 ? "," : "");
            if ((i + 1) % 32 == 0) {
                w.println(metaTable.toString());
                metaTable = new StringBuilder();
            }
        }
        if (metaTable.length() > 0) {
            w.println(metaTable.toString());
        }
        w.println("    };");
        w.println();
    }

    private void emitDispatchTable(PrintWriter w) {
        w.println("    // Dispatch table (indexed by obfuscated opcode)");
        w.println("    static const void* dispatch_table[256] = {");
        for (int i = 0; i < 256; i++) {
            int originalOp = opcodeObfuscator.decode(i);
            Instruction inst = getInstructionByOriginalOpcode(originalOp);
            if (inst != null) {
                w.printf("        &&OP_%02x,%n", originalOp);
            } else {
                w.printf("        &&OP_DEFAULT,%n");
            }
        }
        w.println("    };");
        w.println();
    }

    private void emitDispatchPrologue(PrintWriter w) {
        w.println("    #define DISPATCH_NEXT \\");
        w.println("        do { \\");
        w.println("            uint8_t _op = bytecode[frame.pc]; \\");
        w.println("            if (LIKELY(!needs_meta[_op])) { \\");
        w.println("                meta = NULL; \\");
        w.println("            } else { \\");
        w.println("                int _metaIdx = pcToMetaIdx[frame.pc]; \\");
        w.println("                meta = (_metaIdx >= 0) ? &metaTable[_metaIdx] : NULL; \\");
        w.println("            } \\");
        w.println("            goto *dispatch_table[_op]; \\");
        w.println("        } while(0)");
        w.println();
        w.println("    int _hasException = 0;  // set to 1 when unhandled exception causes exit");
        w.println("    MetaEntry* meta = NULL;");
        w.println("    DISPATCH_NEXT;");
        w.println();
    }

    private void emitInstructionHandlers(PrintWriter w) {
        for (int i = 0; i < 256; i++) {
            int originalOp = opcodeObfuscator.decode(i);
            Instruction inst = getInstructionByOriginalOpcode(originalOp);
            if (inst != null) {
                inst.generateComputedGoto(w);
                w.println();
            }
        }
    }

    private void emitExecuteCommonEpilogue(PrintWriter w) {
        w.println("        OP_DEFAULT:");
        w.println("            VM_LOG(\"Unknown opcode: 0x%02x at pc=%d\\n\", bytecode[frame.pc], frame.pc);");
        w.println("            frame.pc++;");
        w.println("            DISPATCH_NEXT;");
        w.println();
        w.println("    method_exit:");
        w.println("    ;");
        w.println("    if (UNLIKELY(_hasException)) {");
        w.println("        execResult.returnType = 'X';  // signal unhandled exception");
        w.println("    } else {");
        w.println("        // Return type is precomputed in VMMethod metadata");
        w.println("        execResult.returnType = m->returnTypeChar;");
        w.println("        // Get return value from top of stack");
        w.println("        if (frame.sp > 0) {");
        w.println("            execResult.value = frame.stack[frame.sp - 1];");
        w.println("        }");
        w.println("    }");
        w.println("    if (!directLocals) frame_pool_pop(m->maxLocals);");
        w.println("    frame_pool_pop(m->maxStack);");
        w.println("    return execResult;");
        w.println("}");
        w.println();
    }

    private Instruction getInstructionByOriginalOpcode(int opcode) {
        if (opcode < 0 || opcode >= instructionByOpcode.length) {
            return null;
        }
        return instructionByOpcode[opcode];
    }
}
