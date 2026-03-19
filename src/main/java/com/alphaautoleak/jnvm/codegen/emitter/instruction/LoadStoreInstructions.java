package com.alphaautoleak.jnvm.codegen.emitter.instruction;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * Load/Store instructions (64-bit only)
 */
public class LoadStoreInstructions {
    private static String[] loadLocalTemplates(int localIndex) {
        String i = Integer.toString(localIndex);
        return new String[] {
                "frame.stack[frame.sp++] = frame.locals[" + i + "];",
                "frame.stack[frame.sp] = frame.locals[" + i + "]; frame.sp++;",
                "frame.sp += 1; frame.stack[frame.sp - 1] = frame.locals[" + i + "];"
        };
    }

    private static String[] storeLocalTemplates(int localIndex) {
        String i = Integer.toString(localIndex);
        return new String[] {
                "frame.locals[" + i + "] = frame.stack[--frame.sp];",
                "frame.sp -= 1; frame.locals[" + i + "] = frame.stack[frame.sp];",
                "frame.locals[" + i + "] = frame.stack[frame.sp - 1]; frame.sp -= 1;"
        };
    }
    
    /**
     * Register all load/store instructions
     */
    public static void registerAll(InstructionRegistry registry) {
        // ILOAD - simple load
        registry.register(new BaseInstructions.MetaInstruction(0x15, "ILOAD", 
            "{ int _idx = meta->intVal; VM_LOG(\"ILOAD: local[%d]=%d\\n\", _idx, frame.locals[_idx].i); frame.stack[frame.sp++] = frame.locals[_idx]; }"));
        
        // LLOAD
        registry.register(new BaseInstructions.MetaInstruction(0x16, "LLOAD", 
            "frame.stack[frame.sp++] = frame.locals[meta->intVal];"));
        
        // FLOAD
        registry.register(new BaseInstructions.MetaInstruction(0x17, "FLOAD", 
            "frame.stack[frame.sp++] = frame.locals[meta->intVal];"));
        
        // DLOAD
        registry.register(new BaseInstructions.MetaInstruction(0x18, "DLOAD", 
            "frame.stack[frame.sp++] = frame.locals[meta->intVal];"));
        
        // ALOAD
        registry.register(new BaseInstructions.MetaInstruction(0x19, "ALOAD", 
            "frame.stack[frame.sp++] = frame.locals[meta->intVal];"));
        
        // ILOAD_0 to ILOAD_3
        for (int i = 0; i < 4; i++) {
            registry.register(new BaseInstructions.PolymorphicSimpleInstruction(0x1a + i, "ILOAD_" + i,
                loadLocalTemplates(i)));
        }
        
        // LLOAD_0 to LLOAD_3
        for (int i = 0; i < 4; i++) {
            registry.register(new BaseInstructions.PolymorphicSimpleInstruction(0x1e + i, "LLOAD_" + i,
                loadLocalTemplates(i)));
        }
        
        // FLOAD_0 to FLOAD_3
        for (int i = 0; i < 4; i++) {
            registry.register(new BaseInstructions.PolymorphicSimpleInstruction(0x22 + i, "FLOAD_" + i,
                loadLocalTemplates(i)));
        }
        
        // DLOAD_0 to DLOAD_3
        for (int i = 0; i < 4; i++) {
            registry.register(new BaseInstructions.PolymorphicSimpleInstruction(0x26 + i, "DLOAD_" + i,
                loadLocalTemplates(i)));
        }
        
        // ALOAD_0 to ALOAD_3
        for (int i = 0; i < 4; i++) {
            registry.register(new BaseInstructions.PolymorphicSimpleInstruction(0x2a + i, "ALOAD_" + i,
                loadLocalTemplates(i)));
        }
        
        // ISTORE, LSTORE, FSTORE, DSTORE, ASTORE
        registry.register(new BaseInstructions.MetaInstruction(0x36, "ISTORE", "frame.locals[meta->intVal] = frame.stack[--frame.sp];"));
        registry.register(new BaseInstructions.MetaInstruction(0x37, "LSTORE", "frame.locals[meta->intVal] = frame.stack[--frame.sp];"));
        registry.register(new BaseInstructions.MetaInstruction(0x38, "FSTORE", "frame.locals[meta->intVal] = frame.stack[--frame.sp];"));
        registry.register(new BaseInstructions.MetaInstruction(0x39, "DSTORE", "frame.locals[meta->intVal] = frame.stack[--frame.sp];"));
        registry.register(new BaseInstructions.MetaInstruction(0x3a, "ASTORE", "frame.locals[meta->intVal] = frame.stack[--frame.sp];"));
        
        // ISTORE_0 to ISTORE_3
        for (int i = 0; i < 4; i++) {
            registry.register(new BaseInstructions.PolymorphicSimpleInstruction(0x3b + i, "ISTORE_" + i,
                storeLocalTemplates(i)));
        }
        // LSTORE_0 to LSTORE_3
        for (int i = 0; i < 4; i++) {
            registry.register(new BaseInstructions.PolymorphicSimpleInstruction(0x3f + i, "LSTORE_" + i,
                storeLocalTemplates(i)));
        }
        // FSTORE_0 to FSTORE_3
        for (int i = 0; i < 4; i++) {
            registry.register(new BaseInstructions.PolymorphicSimpleInstruction(0x43 + i, "FSTORE_" + i,
                storeLocalTemplates(i)));
        }
        // DSTORE_0 to DSTORE_3
        for (int i = 0; i < 4; i++) {
            registry.register(new BaseInstructions.PolymorphicSimpleInstruction(0x47 + i, "DSTORE_" + i,
                storeLocalTemplates(i)));
        }
        // ASTORE_0 to ASTORE_3
        for (int i = 0; i < 4; i++) {
            registry.register(new BaseInstructions.PolymorphicSimpleInstruction(0x4b + i, "ASTORE_" + i,
                storeLocalTemplates(i)));
        }
    }
}
