package com.alphaautoleak.jnvm.codegen.emitter.instruction.control;

import com.alphaautoleak.jnvm.codegen.emitter.instruction.InstructionRegistry;

/**
 * Control flow instructions registration
 */
public class ControlInstructions {
    
    public static void registerAll(InstructionRegistry registry) {
        // RETURN
        registry.register(new ReturnInstruction(0xb1, "RETURN", "void"));
        
        // IRETURN, LRETURN, FRETURN, DRETURN, ARETURN
        registry.register(new ReturnInstruction(0xac, "IRETURN", "int"));
        registry.register(new ReturnInstruction(0xad, "LRETURN", "long"));
        registry.register(new ReturnInstruction(0xae, "FRETURN", "float"));
        registry.register(new ReturnInstruction(0xaf, "DRETURN", "double"));
        registry.register(new ReturnInstruction(0xb0, "ARETURN", "object"));
        
        // GOTO
        registry.register(new GotoInstruction());
        
        // IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE
        registry.register(new IfZeroInstruction(0x99, "IFEQ", "== 0"));
        registry.register(new IfZeroInstruction(0x9a, "IFNE", "!= 0"));
        registry.register(new IfZeroInstruction(0x9b, "IFLT", "< 0"));
        registry.register(new IfZeroInstruction(0x9c, "IFGE", ">= 0"));
        registry.register(new IfZeroInstruction(0x9d, "IFGT", "> 0"));
        registry.register(new IfZeroInstruction(0x9e, "IFLE", "<= 0"));
        
        // IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE
        registry.register(new IfCmpInstruction(0x9f, "IF_ICMPEQ", "=="));
        registry.register(new IfCmpInstruction(0xa0, "IF_ICMPNE", "!="));
        registry.register(new IfCmpInstruction(0xa1, "IF_ICMPLT", "<"));
        registry.register(new IfCmpInstruction(0xa2, "IF_ICMPGE", ">="));
        registry.register(new IfCmpInstruction(0xa3, "IF_ICMPGT", ">"));
        registry.register(new IfCmpInstruction(0xa4, "IF_ICMPLE", "<="));
        
        // IF_ACMPEQ, IF_ACMPNE
        registry.register(new IfAcmpInstruction(0xa5, "IF_ACMPEQ", "=="));
        registry.register(new IfAcmpInstruction(0xa6, "IF_ACMPNE", "!="));
        
        // IFNULL, IFNONNULL
        registry.register(new IfNullInstruction(0xc6, "IFNULL", "== NULL"));
        registry.register(new IfNullInstruction(0xc7, "IFNONNULL", "!= NULL"));
        
        // IINC
        registry.register(new IincInstruction());
    }
}
