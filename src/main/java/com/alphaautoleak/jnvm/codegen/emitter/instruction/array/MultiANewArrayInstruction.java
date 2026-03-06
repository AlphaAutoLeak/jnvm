package com.alphaautoleak.jnvm.codegen.emitter.instruction.array;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * MULTIANEWARRAY instruction (0xc5)
 * Creates a multi-dimensional array
 * Stack: ..., count1, [count2, ...] -> ..., arrayref
 *
 * meta->classIdx: array class descriptor (e.g., "[[[I")
 * meta->dims: number of dimensions
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
        w.println("                    VM_LOG(\"MULTIANEWARRAY: class=%s, dims=%d\\n\", className, dims);");
        w.println();
        w.println("                    // 弹出各维度的大小");
        w.println("                    jint* sizes = (jint*)malloc(dims * sizeof(jint));");
        w.println("                    for (int i = dims - 1; i >= 0; i--) {");
        w.println("                        sizes[i] = frame.stack[--frame.sp].i;");
        w.println("                        VM_LOG(\"  dim[%d] = %d\\n\", i, sizes[i]);");
        w.println("                    }");
        w.println();
        w.println("                    // 使用 JNI 创建多维数组");
        w.println("                    jclass arrayClass = vm_find_class(env, className);");  // 使用缓存版本
        w.println("                    if (arrayClass == NULL) {");
        w.println("                        VM_LOG(\"MULTIANEWARRAY: Failed to find class %s\\n\", className);");
        w.println("                        frame.stack[frame.sp].l = NULL;");
        w.println("                    } else {");
        w.println("                        // 创建数组对象");
        w.println("                        jarray result = NULL;");
        w.println();
        w.println("                        // 根据维度创建数组");
        w.println("                        // 先创建最外层数组");
        w.println("                        if (dims >= 1) {");
        w.println("                            // 根据元素类型创建数组");
        w.println("                            // 解析类名确定元素类型");
        w.println("                            int depth = 0;");
        w.println("                            while (className[depth] == '[') depth++;");
        w.println("                            char elemType = className[depth];");
        w.println();
        w.println("                            // 使用 NewObjectArray 创建引用类型数组");
        w.println("                            // 或 New<X>Array 创建基本类型数组");
        w.println("                            switch (elemType) {");
        w.println("                                case 'I':");
        w.println("                                    result = (*env)->NewIntArray(env, sizes[0]);");
        w.println("                                    break;");
        w.println("                                case 'J':");
        w.println("                                    result = (*env)->NewLongArray(env, sizes[0]);");
        w.println("                                    break;");
        w.println("                                case 'F':");
        w.println("                                    result = (*env)->NewFloatArray(env, sizes[0]);");
        w.println("                                    break;");
        w.println("                                case 'D':");
        w.println("                                    result = (*env)->NewDoubleArray(env, sizes[0]);");
        w.println("                                    break;");
        w.println("                                case 'Z':");
        w.println("                                    result = (*env)->NewBooleanArray(env, sizes[0]);");
        w.println("                                    break;");
        w.println("                                case 'B':");
        w.println("                                    result = (*env)->NewByteArray(env, sizes[0]);");
        w.println("                                    break;");
        w.println("                                case 'C':");
        w.println("                                    result = (*env)->NewCharArray(env, sizes[0]);");
        w.println("                                    break;");
        w.println("                                case 'S':");
        w.println("                                    result = (*env)->NewShortArray(env, sizes[0]);");
        w.println("                                    break;");
        w.println("                                default:");
        w.println("                                    // 引用类型 - 需要元素类");
        w.println("                                    if (elemType == 'L' || depth < dims) {");
        w.println("                                        // 获取元素类型");
        w.println("                                        char elemClassName[256];");
        w.println("                                        if (depth < dims) {");
        w.println("                                            // 嵌套数组，元素类型是数组");
        w.println("                                            strcpy(elemClassName, className + 1);");
        w.println("                                        } else {");
        w.println("                                            // 对象类型");
        w.println("                                            int len = strlen(className + depth + 1);");
        w.println("                                            strncpy(elemClassName, className + depth + 1, len - 1);");
        w.println("                                            elemClassName[len - 1] = '\\0';");
        w.println("                                        }");
        w.println("                                        jclass elemClass = vm_find_class(env, elemClassName);");  // 使用缓存版本
        w.println("                                        result = (*env)->NewObjectArray(env, sizes[0], elemClass, NULL);");
        w.println("                                    }");
        w.println("                                    break;");
        w.println("                            }");
        w.println();
        w.println("                            // 对于多维数组，递归填充子数组");
        w.println("                            if (dims > 1 && result != NULL) {");
        w.println("                                // 递归创建子数组");
        w.println("                                for (int i = 0; i < sizes[0]; i++) {");
        w.println("                                    // 这里需要递归创建子数组，简化起见暂时跳过");
        w.println("                                    // 实际实现需要更复杂的逻辑");
        w.println("                                }");
        w.println("                            }");
        w.println("                        }");
        w.println();
        w.println("                        frame.stack[frame.sp].l = result;");
        w.println("                        frame.stackTypes[frame.sp++] = TYPE_REF;");
        w.println("                    }");
        w.println("                    free(sizes);");
        w.println("                }");
        pcIncBreak(w);
    }

    @Override
    public void generate(PrintWriter w) {
        w.printf("            case 0x%02x: /* %s */\n", opcode, comment);
        generateBody(w);
    }
}
