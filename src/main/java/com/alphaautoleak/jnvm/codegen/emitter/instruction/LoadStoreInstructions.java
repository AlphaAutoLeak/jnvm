package com.alphaautoleak.jnvm.codegen.emitter.instruction;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

/**
 * åŠ è½½/å­˜å‚¨æŒ‡ä»¤
 */
public class LoadStoreInstructions {
    
    /**
     * æ³¨å†Œæ‰€æœ‰åŠ è½?å­˜å‚¨æŒ‡ä»¤
     */
    public static void registerAll(InstructionRegistry registry) {
        // ILOAD, LLOAD, FLOAD, DLOAD, ALOAD
        registry.register(new BaseInstructions.MetaInstruction(0x15, "ILOAD", "frame.stack[frame.sp++] = frame.locals[meta->intVal];"));
        registry.register(new BaseInstructions.MetaInstruction(0x16, "LLOAD", "frame.stack[frame.sp++] = frame.locals[meta->intVal];"));
        registry.register(new BaseInstructions.MetaInstruction(0x17, "FLOAD", "frame.stack[frame.sp++] = frame.locals[meta->intVal];"));
        registry.register(new BaseInstructions.MetaInstruction(0x18, "DLOAD", "frame.stack[frame.sp++] = frame.locals[meta->intVal];"));
        registry.register(new BaseInstructions.MetaInstruction(0x19, "ALOAD", "frame.stack[frame.sp++] = frame.locals[meta->intVal];"));
        
        // ILOAD_0 åˆ?ILOAD_3
        for (int i = 0; i < 4; i++) {
            registry.register(new BaseInstructions.SimpleInstruction(0x1a + i, "ILOAD_" + i,
                "frame.stack[frame.sp++] = frame.locals[" + i + "];"));
        }
        // LLOAD_0 åˆ?LLOAD_3
        for (int i = 0; i < 4; i++) {
            registry.register(new BaseInstructions.SimpleInstruction(0x1e + i, "LLOAD_" + i,
                "frame.stack[frame.sp++] = frame.locals[" + i + "];"));
        }
        // FLOAD_0 åˆ?FLOAD_3
        for (int i = 0; i < 4; i++) {
            registry.register(new BaseInstructions.SimpleInstruction(0x22 + i, "FLOAD_" + i,
                "frame.stack[frame.sp++] = frame.locals[" + i + "];"));
        }
        // DLOAD_0 åˆ?DLOAD_3
        for (int i = 0; i < 4; i++) {
            registry.register(new BaseInstructions.SimpleInstruction(0x26 + i, "DLOAD_" + i,
                "frame.stack[frame.sp++] = frame.locals[" + i + "];"));
        }
        // ALOAD_0 åˆ?ALOAD_3
        for (int i = 0; i < 4; i++) {
            registry.register(new BaseInstructions.SimpleInstruction(0x2a + i, "ALOAD_" + i,
                "frame.stack[frame.sp++] = frame.locals[" + i + "];"));
        }
        
        // ISTORE, LSTORE, FSTORE, DSTORE, ASTORE
        registry.register(new BaseInstructions.MetaInstruction(0x36, "ISTORE", "frame.locals[meta->intVal] = frame.stack[--frame.sp];"));
        registry.register(new BaseInstructions.MetaInstruction(0x37, "LSTORE", "frame.locals[meta->intVal] = frame.stack[--frame.sp];"));
        registry.register(new BaseInstructions.MetaInstruction(0x38, "FSTORE", "frame.locals[meta->intVal] = frame.stack[--frame.sp];"));
        registry.register(new BaseInstructions.MetaInstruction(0x39, "DSTORE", "frame.locals[meta->intVal] = frame.stack[--frame.sp];"));
        registry.register(new BaseInstructions.MetaInstruction(0x3a, "ASTORE", "frame.locals[meta->intVal] = frame.stack[--frame.sp];"));
        
        // ISTORE_0 åˆ?ISTORE_3
        for (int i = 0; i < 4; i++) {
            registry.register(new BaseInstructions.SimpleInstruction(0x3b + i, "ISTORE_" + i,
                "frame.locals[" + i + "] = frame.stack[--frame.sp];"));
        }
        // LSTORE_0 åˆ?LSTORE_3
        for (int i = 0; i < 4; i++) {
            registry.register(new BaseInstructions.SimpleInstruction(0x3f + i, "LSTORE_" + i,
                "frame.locals[" + i + "] = frame.stack[--frame.sp];"));
        }
        // FSTORE_0 åˆ?FSTORE_3
        for (int i = 0; i < 4; i++) {
            registry.register(new BaseInstructions.SimpleInstruction(0x43 + i, "FSTORE_" + i,
                "frame.locals[" + i + "] = frame.stack[--frame.sp];"));
        }
        // DSTORE_0 åˆ?DSTORE_3
        for (int i = 0; i < 4; i++) {
            registry.register(new BaseInstructions.SimpleInstruction(0x47 + i, "DSTORE_" + i,
                "frame.locals[" + i + "] = frame.stack[--frame.sp];"));
        }
        // ASTORE_0 åˆ?ASTORE_3
        for (int i = 0; i < 4; i++) {
            registry.register(new BaseInstructions.SimpleInstruction(0x4b + i, "ASTORE_" + i,
                "frame.locals[" + i + "] = frame.stack[--frame.sp];"));
        }
    }
}
