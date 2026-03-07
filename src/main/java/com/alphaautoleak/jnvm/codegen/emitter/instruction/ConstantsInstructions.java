package com.alphaautoleak.jnvm.codegen.emitter.instruction;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * Constant loading instructions (64-bit only)
 */
public class ConstantsInstructions {
    
    /** LDC instruction */
    public static class LdcInstruction extends Instruction {
        public LdcInstruction() {
            super(0x12, "LDC");
        }
        
        @Override
        public boolean needsMeta() {
            return true;
        }
        
        private void generateLdcBody(PrintWriter w, String indent) {
            w.println(indent + "switch (meta->type) {");
            w.println(indent + "    case META_INT:");
            w.println(indent + "        frame.stack[frame.sp++].i = meta->intVal; break;");
            w.println(indent + "    case META_LONG:");
            w.println(indent + "        frame.stack[frame.sp++].j = meta->longVal; break;");
            w.println(indent + "    case META_FLOAT:");
            w.println(indent + "        frame.stack[frame.sp++].f = meta->floatVal; break;");
            w.println(indent + "    case META_DOUBLE:");
            w.println(indent + "        frame.stack[frame.sp++].d = meta->doubleVal; break;");
            w.println(indent + "    case META_STRING: {");
            w.println(indent + "        const char* str = vm_get_string(meta->strIdx);");
            w.println(indent + "        frame.stack[frame.sp++].l = (*env)->NewStringUTF(env, str); break;"); 
            w.println(indent + "    }");
            w.println(indent + "    case META_CLASS: {");
            w.println(indent + "        const char* cls = vm_get_string(meta->classIdx);");
            w.println(indent + "        jclass resultClass = NULL;");
            w.println(indent + "        if (frame.callerClass != NULL) {");
            w.println(indent + "            static jclass classClass_cached = NULL;");
            w.println(indent + "            static jmethodID getClassLoader_mid = NULL;");
            w.println(indent + "            static jmethodID forName_mid = NULL;");
            w.println(indent + "            if (!classClass_cached) {");
            w.println(indent + "                jclass localClassClass = (*env)->FindClass(env, \"java/lang/Class\");");
            w.println(indent + "                if (localClassClass) classClass_cached = (*env)->NewGlobalRef(env, localClassClass);");
            w.println(indent + "            }");
            w.println(indent + "            if (classClass_cached && !getClassLoader_mid) {");
            w.println(indent + "                getClassLoader_mid = (*env)->GetMethodID(env, classClass_cached, \"getClassLoader\", \"()Ljava/lang/ClassLoader;\");");
            w.println(indent + "            }");
            w.println(indent + "            if (classClass_cached && !forName_mid) {");
            w.println(indent + "                forName_mid = (*env)->GetStaticMethodID(env, classClass_cached, \"forName\", \"(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;\");");
            w.println(indent + "            }");
            w.println(indent + "            jobject classLoader = (classClass_cached && getClassLoader_mid) ?");
            w.println(indent + "                (*env)->CallObjectMethod(env, frame.callerClass, getClassLoader_mid) : NULL;");
            w.println(indent + "            if (classLoader != NULL && forName_mid) {");
            w.println(indent + "                char* binName = strdup(cls);");
            w.println(indent + "                for (char* p = binName; *p; p++) { if (*p == '/') *p = '.'; }");
            w.println(indent + "                jstring binNameStr = (*env)->NewStringUTF(env, binName);");
            w.println(indent + "                resultClass = (*env)->CallStaticObjectMethod(env, classClass_cached, forName_mid, binNameStr, JNI_FALSE, classLoader);");
            w.println(indent + "                free(binName);");
            w.println(indent + "            }");
            w.println(indent + "        }");
            w.println(indent + "        if (resultClass == NULL) {");
            w.println(indent + "            resultClass = vm_find_class(env, cls);");
            w.println(indent + "        }");
            w.println(indent + "        frame.stack[frame.sp++].l = resultClass; break;");
            w.println(indent + "    }");
            w.println(indent + "    default: break;");
            w.println(indent + "}");
        }
        
        @Override
        protected void generateBody(PrintWriter w) {
            w.println("            case 0x12: /* LDC */");
            w.println("            case 0x13: /* LDC_W */");
            w.println("            case 0x14: /* LDC2_W */ {");
            generateLdcBody(w, "                ");
            w.println("                frame.pc++;");
            w.println("            }");
            w.println("            break;");
        }
        
        @Override
        public void generate(PrintWriter w) {
            generateBody(w);
        }
        
        @Override
        public void generateComputedGoto(PrintWriter w) {
            w.println("        OP_12:  /* LDC */");
            generateLdcBody(w, "            ");
            w.println("            frame.pc++;");
            w.println("            DISPATCH_NEXT;");
            w.println();
            
            w.println("        OP_13:  /* LDC_W */");
            generateLdcBody(w, "            ");
            w.println("            frame.pc++;");
            w.println("            DISPATCH_NEXT;");
            w.println();
            
            w.println("        OP_14:  /* LDC2_W */");
            generateLdcBody(w, "            ");
            w.println("            frame.pc++;");
            w.println("            DISPATCH_NEXT;");
        }
    }
    
    /**
     * Register all constant loading instructions
     */
    public static void registerAll(InstructionRegistry registry) {
        // ICONST_M1 to ICONST_5
        registry.register(new BaseInstructions.SimpleInstruction(0x02, "ICONST_M1", 
            "frame.stack[frame.sp++].i = -1;"));
        for (int i = 0; i <= 5; i++) {
            registry.register(new BaseInstructions.SimpleInstruction(0x03 + i, "ICONST_" + i, 
                "frame.stack[frame.sp++].i = " + i + ";"));
        }
        
        // LCONST_0, LCONST_1
        registry.register(new BaseInstructions.SimpleInstruction(0x09, "LCONST_0", 
            "frame.stack[frame.sp++].j = 0L;"));
        registry.register(new BaseInstructions.SimpleInstruction(0x0a, "LCONST_1", 
            "frame.stack[frame.sp++].j = 1L;"));
        
        // FCONST_0, FCONST_1, FCONST_2
        for (int i = 0; i <= 2; i++) {
            registry.register(new BaseInstructions.SimpleInstruction(0x0b + i, "FCONST_" + i,
                "frame.stack[frame.sp++].f = " + i + ".0f;"));
        }
        
        // DCONST_0, DCONST_1
        registry.register(new BaseInstructions.SimpleInstruction(0x0e, "DCONST_0", 
            "frame.stack[frame.sp++].d = 0.0;"));
        registry.register(new BaseInstructions.SimpleInstruction(0x0f, "DCONST_1", 
            "frame.stack[frame.sp++].d = 1.0;"));
        
        // ACONST_NULL
        registry.register(new BaseInstructions.SimpleInstruction(0x01, "ACONST_NULL", 
            "frame.stack[frame.sp++].l = NULL;"));
        
        // BIPUSH, SIPUSH
        registry.register(new BaseInstructions.MetaInstruction(0x10, "BIPUSH", 
            "frame.stack[frame.sp++].i = meta->intVal;"));
        registry.register(new BaseInstructions.MetaInstruction(0x11, "SIPUSH", 
            "frame.stack[frame.sp++].i = meta->intVal;"));
        
        // LDC series
        registry.register(new LdcInstruction());
    }
}
