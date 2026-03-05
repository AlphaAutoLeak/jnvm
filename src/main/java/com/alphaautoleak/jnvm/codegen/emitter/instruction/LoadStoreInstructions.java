package com.alphaautoleak.jnvm.codegen.emitter.instruction;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * Load/Store instructions
 */
public class LoadStoreInstructions {
    
    /**
     * Register all load/store instructions
     */
    public static void registerAll(InstructionRegistry registry) {
        // ILOAD - simple load (unboxing already done during parameter setup)
        registry.register(new BaseInstructions.MetaInstruction(0x15, "ILOAD", 
            "{ int _idx = meta->intVal; VM_LOG(\"ILOAD: local[%d]=%d\\n\", _idx, frame.locals[_idx].i); frame.stack[frame.sp] = frame.locals[_idx]; frame.stackTypes[frame.sp++] = TYPE_INT; }"));
        
        // LLOAD - simple load
        registry.register(new BaseInstructions.MetaInstruction(0x16, "LLOAD", 
            "frame.stack[frame.sp] = frame.locals[meta->intVal]; frame.stackTypes[frame.sp++] = TYPE_LONG;"));
        
        // FLOAD - simple load
        registry.register(new BaseInstructions.MetaInstruction(0x17, "FLOAD", 
            "frame.stack[frame.sp] = frame.locals[meta->intVal]; frame.stackTypes[frame.sp++] = TYPE_FLOAT;"));
        
        // DLOAD - simple load
        registry.register(new BaseInstructions.MetaInstruction(0x18, "DLOAD", 
            "frame.stack[frame.sp] = frame.locals[meta->intVal]; frame.stackTypes[frame.sp++] = TYPE_DOUBLE;"));
        
        // ALOAD - no unboxing needed
        registry.register(new BaseInstructions.MetaInstruction(0x19, "ALOAD", 
            "frame.stack[frame.sp] = frame.locals[meta->intVal]; frame.stackTypes[frame.sp++] = TYPE_REF;"));
        
        // ILOAD_0 to ILOAD_3
        for (int i = 0; i < 4; i++) {
            registry.register(new BaseInstructions.SimpleInstruction(0x1a + i, "ILOAD_" + i,
                "frame.stack[frame.sp] = frame.locals[" + i + "]; frame.stackTypes[frame.sp++] = TYPE_INT;"));
        }
        
        // LLOAD_0 to LLOAD_3
        for (int i = 0; i < 4; i++) {
            registry.register(new BaseInstructions.SimpleInstruction(0x1e + i, "LLOAD_" + i,
                "frame.stack[frame.sp] = frame.locals[" + i + "]; frame.stackTypes[frame.sp++] = TYPE_LONG;"));
        }
        
        // FLOAD_0 to FLOAD_3
        for (int i = 0; i < 4; i++) {
            registry.register(new BaseInstructions.SimpleInstruction(0x22 + i, "FLOAD_" + i,
                "frame.stack[frame.sp] = frame.locals[" + i + "]; frame.stackTypes[frame.sp++] = TYPE_FLOAT;"));
        }
        
        // DLOAD_0 to DLOAD_3
        for (int i = 0; i < 4; i++) {
            registry.register(new BaseInstructions.SimpleInstruction(0x26 + i, "DLOAD_" + i,
                "frame.stack[frame.sp] = frame.locals[" + i + "]; frame.stackTypes[frame.sp++] = TYPE_DOUBLE;"));
        }
        
        // ALOAD_0 to ALOAD_3
        for (int i = 0; i < 4; i++) {
            registry.register(new BaseInstructions.SimpleInstruction(0x2a + i, "ALOAD_" + i,
                "frame.stack[frame.sp] = frame.locals[" + i + "]; frame.stackTypes[frame.sp++] = TYPE_REF;"));
        }
        
        // ISTORE, LSTORE, FSTORE, DSTORE, ASTORE (store doesn't need to set type, just pop)
        registry.register(new BaseInstructions.MetaInstruction(0x36, "ISTORE", "frame.locals[meta->intVal] = frame.stack[--frame.sp];"));
        registry.register(new BaseInstructions.MetaInstruction(0x37, "LSTORE", "frame.locals[meta->intVal] = frame.stack[--frame.sp];"));
        registry.register(new BaseInstructions.MetaInstruction(0x38, "FSTORE", "frame.locals[meta->intVal] = frame.stack[--frame.sp];"));
        registry.register(new BaseInstructions.MetaInstruction(0x39, "DSTORE", "frame.locals[meta->intVal] = frame.stack[--frame.sp];"));
        registry.register(new BaseInstructions.MetaInstruction(0x3a, "ASTORE", "frame.locals[meta->intVal] = frame.stack[--frame.sp];"));
        
        // ISTORE_0 to ISTORE_3
        for (int i = 0; i < 4; i++) {
            registry.register(new BaseInstructions.SimpleInstruction(0x3b + i, "ISTORE_" + i,
                "frame.locals[" + i + "] = frame.stack[--frame.sp];"));
        }
        // LSTORE_0 to LSTORE_3
        for (int i = 0; i < 4; i++) {
            registry.register(new BaseInstructions.SimpleInstruction(0x3f + i, "LSTORE_" + i,
                "frame.locals[" + i + "] = frame.stack[--frame.sp];"));
        }
        // FSTORE_0 to FSTORE_3
        for (int i = 0; i < 4; i++) {
            registry.register(new BaseInstructions.SimpleInstruction(0x43 + i, "FSTORE_" + i,
                "frame.locals[" + i + "] = frame.stack[--frame.sp];"));
        }
        // DSTORE_0 to DSTORE_3
        for (int i = 0; i < 4; i++) {
            registry.register(new BaseInstructions.SimpleInstruction(0x47 + i, "DSTORE_" + i,
                "frame.locals[" + i + "] = frame.stack[--frame.sp];"));
        }
        // ASTORE_0 to ASTORE_3
        for (int i = 0; i < 4; i++) {
            registry.register(new BaseInstructions.SimpleInstruction(0x4b + i, "ASTORE_" + i,
                "frame.locals[" + i + "] = frame.stack[--frame.sp];"));
        }
    }
}
