package com.alphaautoleak.jnvm.codegen.emitter.instruction.arithmetic;

import com.alphaautoleak.jnvm.codegen.emitter.instruction.InstructionRegistry;

/**
 * Arithmetic instructions registration
 */
public class ArithmeticInstructions {
    
    public static void registerAll(InstructionRegistry registry) {
        // IADD, LADD, FADD, DADD
        registry.register(new BinaryOpInstruction(0x60, "IADD", "i", "+"));
        registry.register(new BinaryOpInstruction(0x61, "LADD", "j", "+"));
        registry.register(new BinaryOpInstruction(0x62, "FADD", "f", "+"));
        registry.register(new BinaryOpInstruction(0x63, "DADD", "d", "+"));
        
        // ISUB, LSUB, FSUB, DSUB
        registry.register(new BinaryOpInstruction(0x64, "ISUB", "i", "-"));
        registry.register(new BinaryOpInstruction(0x65, "LSUB", "j", "-"));
        registry.register(new BinaryOpInstruction(0x66, "FSUB", "f", "-"));
        registry.register(new BinaryOpInstruction(0x67, "DSUB", "d", "-"));
        
        // IMUL, LMUL, FMUL, DMUL
        registry.register(new BinaryOpInstruction(0x68, "IMUL", "i", "*"));
        registry.register(new BinaryOpInstruction(0x69, "LMUL", "j", "*"));
        registry.register(new BinaryOpInstruction(0x6a, "FMUL", "f", "*"));
        registry.register(new BinaryOpInstruction(0x6b, "DMUL", "d", "*"));
        
        // IDIV, LDIV, FDIV, DDIV
        registry.register(new DivInstruction(0x6c, "IDIV", "i"));
        registry.register(new DivInstruction(0x6d, "LDIV", "j"));
        registry.register(new BinaryOpInstruction(0x6e, "FDIV", "f", "/"));
        registry.register(new BinaryOpInstruction(0x6f, "DDIV", "d", "/"));
        
        // IREM, LREM, FREM, DREM
        registry.register(new DivInstruction(0x70, "IREM", "i", true));
        registry.register(new DivInstruction(0x71, "LREM", "j", true));
        registry.register(new DivInstruction(0x72, "FREM", "f", true));
        registry.register(new DivInstruction(0x73, "DREM", "d", true));

        // INEG, LNEG, FNEG, DNEG
        registry.register(new NegInstruction(0x74, "INEG", "i"));
        registry.register(new NegInstruction(0x75, "LNEG", "j"));
        registry.register(new NegInstruction(0x76, "FNEG", "f"));
        registry.register(new NegInstruction(0x77, "DNEG", "d"));
        
        // ISHL, LSHL, ISHR, LSHR, IUSHR, LUSHR
        registry.register(new ShiftInstruction(0x78, "ISHL", "i", "<<", false));
        registry.register(new ShiftInstruction(0x79, "LSHL", "j", "<<", false));
        registry.register(new ShiftInstruction(0x7a, "ISHR", "i", ">>", false));
        registry.register(new ShiftInstruction(0x7b, "LSHR", "j", ">>", false));
        registry.register(new ShiftInstruction(0x7c, "IUSHR", "i", ">>", true));
        registry.register(new ShiftInstruction(0x7d, "LUSHR", "j", ">>", true));
        
        // IAND, LAND, IOR, LOR, IXOR, LXOR
        registry.register(new BinaryOpInstruction(0x7e, "IAND", "i", "&"));
        registry.register(new BinaryOpInstruction(0x7f, "LAND", "j", "&"));
        registry.register(new BinaryOpInstruction(0x80, "IOR", "i", "|"));
        registry.register(new BinaryOpInstruction(0x81, "LOR", "j", "|"));
        registry.register(new BinaryOpInstruction(0x82, "IXOR", "i", "^"));
        registry.register(new BinaryOpInstruction(0x83, "LXOR", "j", "^"));
        
        // LCMP, FCMPL, FCMPG, DCMPL, DCMPG
        registry.register(new CmpInstruction(0x94, "LCMP", "j"));
        registry.register(new CmpInstruction(0x95, "FCMPL", "f"));
        registry.register(new CmpInstruction(0x96, "FCMPG", "f"));
        registry.register(new CmpInstruction(0x97, "DCMPL", "d"));
        registry.register(new CmpInstruction(0x98, "DCMPG", "d"));
        
        // Type conversion instructions
        registry.register(new CastInstruction(0x85, "I2L", "i", "j", "jlong"));
        registry.register(new CastInstruction(0x86, "I2F", "i", "f", "jfloat"));
        registry.register(new CastInstruction(0x87, "I2D", "i", "d", "jdouble"));
        registry.register(new CastInstruction(0x88, "L2I", "j", "i", "jint"));
        registry.register(new CastInstruction(0x89, "L2F", "j", "f", "jfloat"));
        registry.register(new CastInstruction(0x8a, "L2D", "j", "d", "jdouble"));
        registry.register(new CastInstruction(0x8b, "F2I", "f", "i", "jint"));
        registry.register(new CastInstruction(0x8c, "F2L", "f", "j", "jlong"));
        registry.register(new CastInstruction(0x8d, "F2D", "f", "d", "jdouble"));
        registry.register(new CastInstruction(0x8e, "D2I", "d", "i", "jint"));
        registry.register(new CastInstruction(0x8f, "D2L", "d", "j", "jlong"));
        registry.register(new CastInstruction(0x90, "D2F", "d", "f", "jfloat"));
        registry.register(new CastInstruction(0x91, "I2B", "i", "i", "jbyte"));
        registry.register(new CastInstruction(0x92, "I2C", "i", "i", "jchar"));
        registry.register(new CastInstruction(0x93, "I2S", "i", "i", "jshort"));
    }
}