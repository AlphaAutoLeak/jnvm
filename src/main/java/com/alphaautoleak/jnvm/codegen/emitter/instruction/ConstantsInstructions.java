package com.alphaautoleak.jnvm.codegen.emitter.instruction;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * Constant loading instructions
 */
public class ConstantsInstructions {
    
    /** LDC instruction */
    public static class LdcInstruction extends Instruction {
        public LdcInstruction() {
            super(0x12, "LDC");
        }
        
        @Override
        protected void generateBody(PrintWriter w) {
            w.println("            case 0x12: /* LDC */");
            w.println("            case 0x13: /* LDC_W */");
            w.println("            case 0x14: /* LDC2_W */ {");
            w.println("                switch (meta->type) {");
            w.println("                    case META_INT:");
            w.println("                        frame.stack[frame.sp++].i = meta->intVal; break;");
            w.println("                    case META_LONG:");
            w.println("                        frame.stack[frame.sp++].j = meta->longVal; break;");
            w.println("                    case META_FLOAT:");
            w.println("                        frame.stack[frame.sp++].f = meta->floatVal; break;");
            w.println("                    case META_DOUBLE:");
            w.println("                        frame.stack[frame.sp++].d = meta->doubleVal; break;");
            w.println("                    case META_STRING: {");
            w.println("                        const char* str = vm_strings[meta->strIdx].data;");
            w.println("                        frame.stack[frame.sp++].l = (*env)->NewStringUTF(env, str); break;");
            w.println("                    }");
            w.println("                    case META_CLASS: {");
            w.println("                        const char* cls = vm_strings[meta->classIdx].data;");
            w.println("                        frame.stack[frame.sp++].l = (*env)->FindClass(env, cls); break;");
            w.println("                    }");
            w.println("                    default: break;");
            w.println("                }");
            w.println("                frame.pc++;");
            w.println("            }");
        }
        
        @Override
        public void generate(PrintWriter w) {
            generateBody(w);
        }
    }
    
    /**
     * Register all constant loading instructions
     */
    public static void registerAll(InstructionRegistry registry) {
        // ICONST_M1 to ICONST_5
        registry.register(new BaseInstructions.SimpleInstruction(0x02, "ICONST_M1", "frame.stack[frame.sp++].i = -1;"));
        for (int i = 0; i <= 5; i++) {
            registry.register(new BaseInstructions.SimpleInstruction(0x03 + i, "ICONST_" + i, 
                "frame.stack[frame.sp++].i = " + i + ";"));
        }
        
        // LCONST_0, LCONST_1
        registry.register(new BaseInstructions.SimpleInstruction(0x09, "LCONST_0", "frame.stack[frame.sp++].j = 0L;"));
        registry.register(new BaseInstructions.SimpleInstruction(0x0a, "LCONST_1", "frame.stack[frame.sp++].j = 1L;"));
        
        // FCONST_0, FCONST_1, FCONST_2
        for (int i = 0; i <= 2; i++) {
            registry.register(new BaseInstructions.SimpleInstruction(0x0b + i, "FCONST_" + i,
                "frame.stack[frame.sp++].f = " + i + ".0f;"));
        }
        
        // DCONST_0, DCONST_1
        registry.register(new BaseInstructions.SimpleInstruction(0x0e, "DCONST_0", "frame.stack[frame.sp++].d = 0.0;"));
        registry.register(new BaseInstructions.SimpleInstruction(0x0f, "DCONST_1", "frame.stack[frame.sp++].d = 1.0;"));
        
        // ACONST_NULL
        registry.register(new BaseInstructions.SimpleInstruction(0x01, "ACONST_NULL", "frame.stack[frame.sp++].l = NULL;"));
        
        // BIPUSH, SIPUSH
        registry.register(new BaseInstructions.MetaInstruction(0x10, "BIPUSH", "frame.stack[frame.sp++].i = meta->intVal;"));
        registry.register(new BaseInstructions.MetaInstruction(0x11, "SIPUSH", "frame.stack[frame.sp++].i = meta->intVal;"));
        
        // LDC series
        registry.register(new LdcInstruction());
    }
}