package com.alphaautoleak.jnvm.codegen.emitter;

import com.alphaautoleak.jnvm.codegen.emitter.helper.VMHelper;
import com.alphaautoleak.jnvm.codegen.emitter.helper.VMHelpers;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * 生成 vm_interpreter.h 和 vm_interpreter.c - VM 解释器核心
 */
public class VmInterpreterGenerator {
    
    private final File dir;
    private final boolean debug;
    private final Instructions instructions;
    private final VMHelpers helpers;
    private final int methodIdXorKey;
    
    public VmInterpreterGenerator(File dir, boolean debug, int methodIdXorKey) {
        this.dir = dir;
        this.debug = debug;
        this.instructions = new Instructions();
        this.helpers = new VMHelpers();
        this.methodIdXorKey = methodIdXorKey;
    }
    
    public void generate() throws IOException {
        generateHeader();
        generateSource();
    }
    
    private void generateHeader() throws IOException {
        try (PrintWriter w = new PrintWriter(new java.io.FileWriter(new File(dir, "vm_interpreter.h")))) {
            w.println("#ifndef VM_INTERPRETER_H");
            w.println("#define VM_INTERPRETER_H");
            w.println("#include \"vm_types.h\"");
            w.println();
            
            // 辅助函数声明
            for (VMHelper helper : helpers.getAllHelpers()) {
                helper.generateHeader(w);
            }
            
            w.println();
            w.println("jobject vm_execute_method(JNIEnv* env, int methodId, jobject instance, jobjectArray args);");
            w.println();
            w.println("#endif");
        }
    }
    
    private void generateSource() throws IOException {
        try (PrintWriter w = new PrintWriter(new java.io.FileWriter(new File(dir, "vm_interpreter.c")))) {
            w.println("#include \"vm_interpreter.h\"");
            w.println("#include \"vm_data.h\"");
            w.println("#include \"chacha20.h\"");
            w.println("#include <stdio.h>");
            w.println("#include <stdlib.h>");
            w.println("#include <string.h>");
            w.println();
            
            // XOR key
            w.println("#define METHOD_ID_XOR_KEY 0x" + Integer.toHexString(methodIdXorKey));
            w.println();
            
            // Debug macros
            if (debug) {
                w.println("#define VM_LOG(fmt, ...) printf(\"[VM] \" fmt, ##__VA_ARGS__)");
                w.println("#define VM_DEBUG_LOG(fmt, ...) printf(\"[VM-DEBUG] \" fmt, ##__VA_ARGS__)");
            } else {
                w.println("#define VM_LOG(fmt, ...)");
                w.println("#define VM_DEBUG_LOG(fmt, ...)");
            }
            w.println();
            
            // 辅助函数实现
            for (VMHelper helper : helpers.getAllHelpers()) {
                helper.generateSource(w);
            }
            
            // 主解释器函数
            emitExecuteMethod(w);
        }
    }
    
    private void emitExecuteMethod(PrintWriter w) {
        w.println("jobject vm_execute_method(JNIEnv* env, int methodId, jobject instance, jobjectArray args) {");
        w.println("    methodId ^= METHOD_ID_XOR_KEY;");
        w.println("    if (methodId < 0 || methodId >= vm_method_count) return NULL;");
        w.println("    VMMethod* m = &vm_methods[methodId];");
        w.println();
        
        // 解密字节码
        w.println("    uint8_t* bytecode = (uint8_t*)malloc(m->bytecodeLen);");
        w.println("    chacha20_encrypt(m->key, m->nonce, m->bytecode, bytecode, m->bytecodeLen);");
        w.println();
        
        // 初始化帧
        w.println("    VMValue result = {0};");
        w.println("    char resultType = 'V';  // 'V'=void, 'I'=int, 'J'=long, 'F'=float, 'D'=double, 'L'=object");
        w.println("    VMFrame frame = { .pc = 0, .sp = 0 };");
        w.println("    frame.stack = (VMValue*)calloc(m->maxStack, sizeof(VMValue));");
        w.println("    frame.locals = (VMValue*)calloc(m->maxLocals, sizeof(VMValue));");
        w.println();
        
        // 设置 this 和参数
        w.println("    frame.locals[0].l = instance;");
        w.println("    const char* methodDesc = (m->descIdx >= 0) ? vm_strings[m->descIdx].data : NULL;");
        w.println("    vm_unbox_args(env, &frame, args, methodDesc, instance ? 1 : 0);");
        w.println();
        
        // 主循环
        w.println("    VM_LOG(\"Executing method %d, bytecodeLen=%d\\n\", methodId, m->bytecodeLen);");
        w.println("    while (frame.pc < m->bytecodeLen) {");
        w.println("        uint8_t opcode = bytecode[frame.pc];");
        w.println("        MetaEntry* meta = vm_get_meta(m, frame.pc);");
        w.println("        VM_LOG(\"m%d: pc=%d op=0x%02x sp=%d\\n\", methodId, frame.pc, opcode, frame.sp);");
        w.println();
        w.println("        switch (opcode) {");
        
        for (Instruction inst : instructions.getAllInstructions()) {
            inst.generate(w);
        }
        
        w.println("            default:");
        w.println("                VM_LOG(\"Unknown opcode: 0x%02x at pc=%d\\n\", opcode, frame.pc);");
        w.println("                frame.pc++; break;");
        w.println("        }");
        w.println();
        
        // 异常处理
        w.println("        if ((*env)->ExceptionCheck(env)) {");
        w.println("            VM_LOG(\"Exception thrown at pc=%d\\n\", frame.pc);");
        w.println("            jthrowable exception = (*env)->ExceptionOccurred(env);");
        w.println("            (*env)->ExceptionClear(env);");
        w.println("            int handlerPc = vm_find_exception_handler(env, m, frame.pc, exception);");
        w.println("            if (handlerPc >= 0) {");
        w.println("                frame.sp = 0;");
        w.println("                frame.stack[frame.sp++].l = exception;");
        w.println("                frame.pc = handlerPc;");
        w.println("                continue;");
        w.println("            }");
        w.println("            VM_LOG(\"No handler found, rethrowing\\n\");");
        w.println("            (*env)->Throw(env, exception);");
        w.println("            break;");
        w.println("        }");
        w.println("    }");
        w.println();
        
        // 返回结果
        w.println("method_exit:");
        w.println("    ;");
        w.println("    char methodReturnType = 'V';");
        w.println("    if (methodDesc) {");
        w.println("        const char* p = methodDesc;");
        w.println("        while (*p && *p != ')') p++;");
        w.println("        if (*p == ')') methodReturnType = *(p + 1);");
        w.println("    }");
        w.println("    VM_LOG(\"Method %d finished, returnType=%c\\n\", methodId, methodReturnType);");
        w.println("    jobject resultObj = vm_box_result(env, result, methodReturnType);");
        w.println("    free(frame.locals);");
        w.println("    free(frame.stack);");
        w.println("    free(bytecode);");
        w.println("    return resultObj;");
        w.println("}");
    }
}
