package com.alphaautoleak.jnvm.codegen.emitter.instruction.array;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * MULTIANEWARRAY instruction (0xc5) - 64-bit only
 */
public class MultiANewArrayInstruction extends Instruction {
    public MultiANewArrayInstruction() {
        super(0xc5, "MULTIANEWARRAY");
    }

    @Override
    protected void generateBody(PrintWriter w) {
        w.println("                {");
        w.println("                    const char* className = vm_get_string(meta->classIdx);");
        w.println("                    int dims = meta->dims;");
        w.println();
        w.println("                    jint* sizes = (jint*)malloc(dims * sizeof(jint));");
        w.println("                    for (int i = dims - 1; i >= 0; i--) {");
        w.println("                        sizes[i] = frame.stack[--frame.sp].i;");
        w.println("                    }");
        w.println();
        w.println("                    jarray result = NULL;");
        w.println("                    if (dims >= 1) {");
        w.println("                        const char* p = className;");
        w.println("                        while (*p == '[') p++;");
        w.println("                        int depth = p - className;");
        w.println("                        char elemType = *p;");
        w.println();
        w.println("                        switch (elemType) {");
        w.println("                            case 'I': result = (*env)->NewIntArray(env, sizes[0]); break;");
        w.println("                            case 'J': result = (*env)->NewLongArray(env, sizes[0]); break;");
        w.println("                            case 'F': result = (*env)->NewFloatArray(env, sizes[0]); break;");
        w.println("                            case 'D': result = (*env)->NewDoubleArray(env, sizes[0]); break;");
        w.println("                            case 'Z': result = (*env)->NewBooleanArray(env, sizes[0]); break;");
        w.println("                            case 'B': result = (*env)->NewByteArray(env, sizes[0]); break;");
        w.println("                            case 'C': result = (*env)->NewCharArray(env, sizes[0]); break;");
        w.println("                            case 'S': result = (*env)->NewShortArray(env, sizes[0]); break;");
        w.println("                            default:");
        w.println("                                if (elemType == 'L' || depth < dims) {");
        w.println("                                    const char* elemClassName;");
        w.println("                                    char elemClassNameBuf[256];");
        w.println("                                    if (depth < dims) {");
        w.println("                                        elemClassName = className + 1;");
        w.println("                                    } else {");
        w.println("                                        const char* start = p + 1;");
        w.println("                                        const char* end = start;");
        w.println("                                        while (*end && *end != ';') end++;");
        w.println("                                        int len = end - start;");
        w.println("                                        if (len > 0 && len < 256) {");
        w.println("                                            memcpy(elemClassNameBuf, start, len);");
        w.println("                                            elemClassNameBuf[len] = '\\0';");
        w.println("                                            elemClassName = elemClassNameBuf;");
        w.println("                                        } else {");
        w.println("                                            elemClassName = start;");
        w.println("                                        }");
        w.println("                                    }");
        w.println("                                    jclass elemClass = vm_find_class(env, elemClassName);");
        w.println("                                    if (elemClass) {");
        w.println("                                        result = (*env)->NewObjectArray(env, sizes[0], elemClass, NULL);");
        w.println("                                    }");
        w.println("                                }");
        w.println("                                break;");
        w.println("                        }");
        w.println("                    }");
        w.println();
        w.println("                    frame.stack[frame.sp++].l = result;");
        w.println("                    free(sizes);");
        w.println("                }");
        pcIncBreak(w);
    }

    @Override
    public void generate(PrintWriter w) {
        w.printf("            case 0x%02x: /* %s */\n", opcode, comment);
        generateBody(w);
    }

    @Override
    public void generateComputedGoto(PrintWriter w) {
        w.printf("        OP_%02x:  /* %s */\n", opcode, comment);
        w.println("            {");
        w.println("                const char* className = vm_get_string(meta->classIdx);");
        w.println("                int dims = meta->dims;");
        w.println("                jint* sizes = (jint*)malloc(dims * sizeof(jint));");
        w.println("                for (int i = dims - 1; i >= 0; i--) {");
        w.println("                    sizes[i] = frame.stack[--frame.sp].i;");
        w.println("                }");
        w.println("                jarray result = NULL;");
        w.println("                if (dims >= 1) {");
        w.println("                    const char* p = className;");
        w.println("                    while (*p == '[') p++;");
        w.println("                    int depth = p - className;");
        w.println("                    char elemType = *p;");
        w.println("                    switch (elemType) {");
        w.println("                        case 'I': result = (*env)->NewIntArray(env, sizes[0]); break;");
        w.println("                        case 'J': result = (*env)->NewLongArray(env, sizes[0]); break;");
        w.println("                        case 'F': result = (*env)->NewFloatArray(env, sizes[0]); break;");
        w.println("                        case 'D': result = (*env)->NewDoubleArray(env, sizes[0]); break;");
        w.println("                        case 'Z': result = (*env)->NewBooleanArray(env, sizes[0]); break;");
        w.println("                        case 'B': result = (*env)->NewByteArray(env, sizes[0]); break;");
        w.println("                        case 'C': result = (*env)->NewCharArray(env, sizes[0]); break;");
        w.println("                        case 'S': result = (*env)->NewShortArray(env, sizes[0]); break;");
        w.println("                        default:");
        w.println("                            if (elemType == 'L' || depth < dims) {");
        w.println("                                const char* elemClassName;");
        w.println("                                char elemClassNameBuf[256];");
        w.println("                                if (depth < dims) {");
        w.println("                                    elemClassName = className + 1;");
        w.println("                                } else {");
        w.println("                                    const char* start = p + 1;");
        w.println("                                    const char* end = start;");
        w.println("                                    while (*end && *end != ';') end++;");
        w.println("                                    int len = end - start;");
        w.println("                                    if (len > 0 && len < 256) {");
        w.println("                                        memcpy(elemClassNameBuf, start, len);");
        w.println("                                        elemClassNameBuf[len] = '\\0';");
        w.println("                                        elemClassName = elemClassNameBuf;");
        w.println("                                    } else {");
        w.println("                                        elemClassName = start;");
        w.println("                                    }");
        w.println("                                }");
        w.println("                                jclass elemClass = vm_find_class(env, elemClassName);");
        w.println("                                if (elemClass) {");
        w.println("                                    result = (*env)->NewObjectArray(env, sizes[0], elemClass, NULL);");
        w.println("                                }");
        w.println("                            }");
        w.println("                            break;");
        w.println("                    }");
        w.println("                }");
        w.println("                frame.stack[frame.sp++].l = result;");
        w.println("                free(sizes);");
        w.println("            }");
        w.println("            frame.pc++;");
        w.println("            DISPATCH_NEXT;");
    }
}