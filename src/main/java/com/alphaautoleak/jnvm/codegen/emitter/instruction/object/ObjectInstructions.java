package com.alphaautoleak.jnvm.codegen.emitter.instruction.object;

import com.alphaautoleak.jnvm.codegen.emitter.instruction.InstructionRegistry;

/**
 * Object and field operation instructions registration
 */
public class ObjectInstructions {
    
    public static void registerAll(InstructionRegistry registry) {
        registry.register(new NewInstruction());
        registry.register(new CheckCastInstruction());
        registry.register(new InstanceOfInstruction());
        
        registry.register(new GetStaticInstruction());
        registry.register(new PutStaticInstruction());
        registry.register(new GetFieldInstruction());
        registry.register(new PutFieldInstruction());
        
        registry.register(new InvokeVirtualInstruction());
        registry.register(new InvokeSpecialInstruction());
        registry.register(new InvokeStaticInstruction());
        registry.register(new InvokeInterfaceInstruction());
        registry.register(new InvokeDynamicInstruction());
    }
}