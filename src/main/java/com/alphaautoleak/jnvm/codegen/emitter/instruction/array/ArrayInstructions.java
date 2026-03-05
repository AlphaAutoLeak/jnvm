package com.alphaautoleak.jnvm.codegen.emitter.instruction.array;

import com.alphaautoleak.jnvm.codegen.emitter.instruction.InstructionRegistry;

/**
 * Array operation instructions registration
 */
public class ArrayInstructions {
    
    public static void registerAll(InstructionRegistry registry) {
        registry.register(new NewArrayInstruction());
        registry.register(new ANewArrayInstruction());
        registry.register(new ArrayLengthInstruction());
        
        // Array load instructions
        registry.register(new IALoadInstruction());
        registry.register(new LALoadInstruction());
        registry.register(new FALoadInstruction());
        registry.register(new DALoadInstruction());
        registry.register(new AALoadInstruction());
        registry.register(new BALoadInstruction());
        registry.register(new CALoadInstruction());
        registry.register(new SALoadInstruction());
        
        // Array store instructions
        registry.register(new IAStoreInstruction());
        registry.register(new LAStoreInstruction());
        registry.register(new FAStoreInstruction());
        registry.register(new DAStoreInstruction());
        registry.register(new AAStoreInstruction());
        registry.register(new BAStoreInstruction());
        registry.register(new CAStoreInstruction());
        registry.register(new SAStoreInstruction());
    }
}