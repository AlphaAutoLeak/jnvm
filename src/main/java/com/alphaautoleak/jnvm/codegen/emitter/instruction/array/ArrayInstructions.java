package com.alphaautoleak.jnvm.codegen.emitter.instruction.array;

/**
 * 数组操作指令注册
 */
public class ArrayInstructions {
    
    public static void registerAll(InstructionRegistry registry) {
        registry.register(new NewArrayInstruction());
        registry.register(new ANewArrayInstruction());
        registry.register(new ArrayLengthInstruction());
        
        // 基本类型数组加载
        registry.register(new IALoadInstruction());
        registry.register(new AALoadInstruction());
        
        // 其他类型数组加载
        registry.register(new BaseInstructions.SimpleInstruction(0x2f, "LALOAD", 
            "jint idx = frame.stack[--frame.sp].i; jlongArray arr = (jlongArray)frame.stack[--frame.sp].l; " +
            "jlong* elems = (*env)->GetLongArrayElements(env, arr, NULL); frame.stack[frame.sp++].j = elems[idx]; " +
            "(*env)->ReleaseLongArrayElements(env, arr, elems, 0);"));
        registry.register(new BaseInstructions.SimpleInstruction(0x30, "FALOAD",
            "jint idx = frame.stack[--frame.sp].i; jfloatArray arr = (jfloatArray)frame.stack[--frame.sp].l; " +
            "jfloat* elems = (*env)->GetFloatArrayElements(env, arr, NULL); frame.stack[frame.sp++].f = elems[idx]; " +
            "(*env)->ReleaseFloatArrayElements(env, arr, elems, 0);"));
        registry.register(new BaseInstructions.SimpleInstruction(0x31, "DALOAD",
            "jint idx = frame.stack[--frame.sp].i; jdoubleArray arr = (jdoubleArray)frame.stack[--frame.sp].l; " +
            "jdouble* elems = (*env)->GetDoubleArrayElements(env, arr, NULL); frame.stack[frame.sp++].d = elems[idx]; " +
            "(*env)->ReleaseDoubleArrayElements(env, arr, elems, 0);"));
        registry.register(new BaseInstructions.SimpleInstruction(0x33, "BALOAD",
            "jint idx = frame.stack[--frame.sp].i; jbyteArray arr = (jbyteArray)frame.stack[--frame.sp].l; " +
            "jbyte* elems = (*env)->GetByteArrayElements(env, arr, NULL); frame.stack[frame.sp++].i = elems[idx]; " +
            "(*env)->ReleaseByteArrayElements(env, arr, elems, 0);"));
        registry.register(new BaseInstructions.SimpleInstruction(0x34, "CALOAD",
            "jint idx = frame.stack[--frame.sp].i; jcharArray arr = (jcharArray)frame.stack[--frame.sp].l; " +
            "jchar* elems = (*env)->GetCharArrayElements(env, arr, NULL); frame.stack[frame.sp++].i = elems[idx]; " +
            "(*env)->ReleaseCharArrayElements(env, arr, elems, 0);"));
        registry.register(new BaseInstructions.SimpleInstruction(0x35, "SALOAD",
            "jint idx = frame.stack[--frame.sp].i; jshortArray arr = (jshortArray)frame.stack[--frame.sp].l; " +
            "jshort* elems = (*env)->GetShortArrayElements(env, arr, NULL); frame.stack[frame.sp++].i = elems[idx]; " +
            "(*env)->ReleaseShortArrayElements(env, arr, elems, 0);"));
        
        // 基本类型数组存储
        registry.register(new IAStoreInstruction());
        registry.register(new AAStoreInstruction());
        
        // 其他类型数组存储
        registry.register(new BaseInstructions.SimpleInstruction(0x50, "LASTORE",
            "jlong val = frame.stack[--frame.sp].j; jint idx = frame.stack[--frame.sp].i; " +
            "jlongArray arr = (jlongArray)frame.stack[--frame.sp].l; jlong* elems = (*env)->GetLongArrayElements(env, arr, NULL); " +
            "elems[idx] = val; (*env)->ReleaseLongArrayElements(env, arr, elems, 0);"));
        registry.register(new BaseInstructions.SimpleInstruction(0x51, "FASTORE",
            "jfloat val = frame.stack[--frame.sp].f; jint idx = frame.stack[--frame.sp].i; " +
            "jfloatArray arr = (jfloatArray)frame.stack[--frame.sp].l; jfloat* elems = (*env)->GetFloatArrayElements(env, arr, NULL); " +
            "elems[idx] = val; (*env)->ReleaseFloatArrayElements(env, arr, elems, 0);"));
        registry.register(new BaseInstructions.SimpleInstruction(0x52, "DASTORE",
            "jdouble val = frame.stack[--frame.sp].d; jint idx = frame.stack[--frame.sp].i; " +
            "jdoubleArray arr = (jdoubleArray)frame.stack[--frame.sp].l; jdouble* elems = (*env)->GetDoubleArrayElements(env, arr, NULL); " +
            "elems[idx] = val; (*env)->ReleaseDoubleArrayElements(env, arr, elems, 0);"));
        registry.register(new BaseInstructions.SimpleInstruction(0x54, "BASTORE",
            "jbyte val = (jbyte)frame.stack[--frame.sp].i; jint idx = frame.stack[--frame.sp].i; " +
            "jbyteArray arr = (jbyteArray)frame.stack[--frame.sp].l; jbyte* elems = (*env)->GetByteArrayElements(env, arr, NULL); " +
            "elems[idx] = val; (*env)->ReleaseByteArrayElements(env, arr, elems, 0);"));
        registry.register(new BaseInstructions.SimpleInstruction(0x55, "CASTORE",
            "jchar val = (jchar)frame.stack[--frame.sp].i; jint idx = frame.stack[--frame.sp].i; " +
            "jcharArray arr = (jcharArray)frame.stack[--frame.sp].l; jchar* elems = (*env)->GetCharArrayElements(env, arr, NULL); " +
            "elems[idx] = val; (*env)->ReleaseCharArrayElements(env, arr, elems, 0);"));
        registry.register(new BaseInstructions.SimpleInstruction(0x56, "SASTORE",
            "jshort val = (jshort)frame.stack[--frame.sp].i; jint idx = frame.stack[--frame.sp].i; " +
            "jshortArray arr = (jshortArray)frame.stack[--frame.sp].l; jshort* elems = (*env)->GetShortArrayElements(env, arr, NULL); " +
            "elems[idx] = val; (*env)->ReleaseShortArrayElements(env, arr, elems, 0);"));
    }
}