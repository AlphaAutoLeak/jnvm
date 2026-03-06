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
            w.println("                        frame.stack[frame.sp].i = meta->intVal;");
            w.println("                        frame.stackTypes[frame.sp++] = TYPE_INT; break;");
            w.println("                    case META_LONG:");
            w.println("                        frame.stack[frame.sp].j = meta->longVal;");
            w.println("                        frame.stackTypes[frame.sp++] = TYPE_LONG; break;");
            w.println("                    case META_FLOAT:");
            w.println("                        frame.stack[frame.sp].f = meta->floatVal;");
            w.println("                        frame.stackTypes[frame.sp++] = TYPE_FLOAT; break;");
            w.println("                    case META_DOUBLE:");
            w.println("                        frame.stack[frame.sp].d = meta->doubleVal;");
            w.println("                        frame.stackTypes[frame.sp++] = TYPE_DOUBLE; break;");
            w.println("                    case META_STRING: {");
            w.println("                        // 使用 ISO-8859-1 编码方式创建字符串");
            w.println("                        // 这样每个字节都会被映射到一个 char，保持长度一致");
            w.println("                        const char* str = vm_get_string(meta->strIdx);");
            w.println("                        frame.stack[frame.sp].l = (*env)->NewStringUTF(env, str);");
            w.println("                        frame.stackTypes[frame.sp++] = TYPE_REF; break;");
            w.println("                    }");
            w.println("                    case META_CLASS: {");
            w.println("                        // 使用调用者类的类加载器来加载类，确保类加载器一致性");
            w.println("                        const char* cls = vm_get_string(meta->classIdx);");
            w.println("                        jclass resultClass = NULL;");
            w.println("                        if (frame.callerClass != NULL) {");
            w.println("                            // 获取调用者类的类加载器");
            w.println("                            jclass classClass = (*env)->FindClass(env, \"java/lang/Class\");");
            w.println("                            jmethodID getClassLoader = (*env)->GetMethodID(env, classClass, \"getClassLoader\", \"()Ljava/lang/ClassLoader;\");");
            w.println("                            jobject classLoader = (*env)->CallObjectMethod(env, frame.callerClass, getClassLoader);");
            w.println("                            if (classLoader != NULL) {");
            w.println("                                // 使用 Class.forName(name, false, classLoader) 加载类");
            w.println("                                jmethodID forName = (*env)->GetStaticMethodID(env, classClass, \"forName\", \"(Ljava/lang/String;ZLjava/lang/ClassLoader;)Ljava/lang/Class;\");");
            w.println("                                jstring className = (*env)->NewStringUTF(env, cls);");
            w.println("                                // 将内部名称转换为二进制名称（用 . 替换 /）");
            w.println("                                char* binName = strdup(cls);");
            w.println("                                for (char* p = binName; *p; p++) { if (*p == '/') *p = '.'; }");
            w.println("                                jstring binNameStr = (*env)->NewStringUTF(env, binName);");
            w.println("                                resultClass = (*env)->CallStaticObjectMethod(env, classClass, forName, binNameStr, JNI_FALSE, classLoader);");
            w.println("                                free(binName);");
            w.println("                            }");
            w.println("                        }");
            w.println("                        if (resultClass == NULL) {");
            w.println("                            // 回退到 FindClass");
            w.println("                            resultClass = (*env)->FindClass(env, cls);");
            w.println("                        }");
            w.println("                        frame.stack[frame.sp].l = resultClass;");
            w.println("                        frame.stackTypes[frame.sp++] = TYPE_REF; break;");
            w.println("                    }");
            w.println("                    default: break;");
            w.println("                }");
            w.println("                frame.pc++;");
            w.println("            }");
            w.println("            break;");
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
        registry.register(new BaseInstructions.SimpleInstruction(0x02, "ICONST_M1", 
            "frame.stack[frame.sp].i = -1; frame.stackTypes[frame.sp++] = TYPE_INT;"));
        for (int i = 0; i <= 5; i++) {
            registry.register(new BaseInstructions.SimpleInstruction(0x03 + i, "ICONST_" + i, 
                "frame.stack[frame.sp].i = " + i + "; frame.stackTypes[frame.sp++] = TYPE_INT;"));
        }
        
        // LCONST_0, LCONST_1
        registry.register(new BaseInstructions.SimpleInstruction(0x09, "LCONST_0", 
            "frame.stack[frame.sp].j = 0L; frame.stackTypes[frame.sp++] = TYPE_LONG;"));
        registry.register(new BaseInstructions.SimpleInstruction(0x0a, "LCONST_1", 
            "frame.stack[frame.sp].j = 1L; frame.stackTypes[frame.sp++] = TYPE_LONG;"));
        
        // FCONST_0, FCONST_1, FCONST_2
        for (int i = 0; i <= 2; i++) {
            registry.register(new BaseInstructions.SimpleInstruction(0x0b + i, "FCONST_" + i,
                "frame.stack[frame.sp].f = " + i + ".0f; frame.stackTypes[frame.sp++] = TYPE_FLOAT;"));
        }
        
        // DCONST_0, DCONST_1
        registry.register(new BaseInstructions.SimpleInstruction(0x0e, "DCONST_0", 
            "frame.stack[frame.sp].d = 0.0; frame.stackTypes[frame.sp++] = TYPE_DOUBLE;"));
        registry.register(new BaseInstructions.SimpleInstruction(0x0f, "DCONST_1", 
            "frame.stack[frame.sp].d = 1.0; frame.stackTypes[frame.sp++] = TYPE_DOUBLE;"));
        
        // ACONST_NULL
        registry.register(new BaseInstructions.SimpleInstruction(0x01, "ACONST_NULL", 
            "frame.stack[frame.sp].l = NULL; frame.stackTypes[frame.sp++] = TYPE_REF;"));
        
        // BIPUSH, SIPUSH
        registry.register(new BaseInstructions.MetaInstruction(0x10, "BIPUSH", 
            "frame.stack[frame.sp].i = meta->intVal; frame.stackTypes[frame.sp++] = TYPE_INT;"));
        registry.register(new BaseInstructions.MetaInstruction(0x11, "SIPUSH", 
            "frame.stack[frame.sp].i = meta->intVal; frame.stackTypes[frame.sp++] = TYPE_INT;"));
        
        // LDC series
        registry.register(new LdcInstruction());
    }
}