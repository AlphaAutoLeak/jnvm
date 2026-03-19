package com.alphaautoleak.jnvm.codegen.emitter;

import com.alphaautoleak.jnvm.codegen.emitter.helper.VMHelper;
import com.alphaautoleak.jnvm.codegen.emitter.helper.VMHelpers;
import com.alphaautoleak.jnvm.crypto.OpcodeObfuscator;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * Generates vm_interpreter.h and vm_interpreter.c - VM interpreter core
 * Generates separate functions for each return type to avoid boxing/unboxing
 */
public class VmInterpreterGenerator {
    private static final WrapperSpec[] WRAPPER_SPECS = {
            new WrapperSpec("void", "vm_execute_method_void", "(void)r;  // ignore return value"),
            new WrapperSpec("jint", "vm_execute_method_int", "return r.value.i;"),
            new WrapperSpec("jlong", "vm_execute_method_long", "return r.value.j;"),
            new WrapperSpec("jfloat", "vm_execute_method_float", "return r.value.f;"),
            new WrapperSpec("jdouble", "vm_execute_method_double", "return r.value.d;"),
            new WrapperSpec("jobject", "vm_execute_method_object", "return r.value.l;")
    };

    private static final class WrapperSpec {
        final String returnType;
        final String functionName;
        final String returnStatement;

        WrapperSpec(String returnType, String functionName, String returnStatement) {
            this.returnType = returnType;
            this.functionName = functionName;
            this.returnStatement = returnStatement;
        }
    }
    
    private final File dir;
    private final boolean debug;
    private final boolean encryptStrings;
    private final Instructions instructions;
    private final List<Instruction> allInstructions;
    private final Instruction[] instructionByOpcode = new Instruction[256];
    private final VmCachingSectionEmitter cachingSectionEmitter;
    private final VMHelpers helpers;
    private final int methodIdXorKey;
    private final OpcodeObfuscator opcodeObfuscator;
    
    public VmInterpreterGenerator(File dir, boolean debug, boolean encryptStrings, 
                                  int methodIdXorKey, OpcodeObfuscator opcodeObfuscator) {
        this.dir = dir;
        this.debug = debug;
        this.encryptStrings = encryptStrings;
        this.instructions = new Instructions();
        this.allInstructions = instructions.getAllInstructions();
        buildInstructionIndex();
        this.cachingSectionEmitter = new VmCachingSectionEmitter();
        this.helpers = new VMHelpers(encryptStrings);
        this.methodIdXorKey = methodIdXorKey;
        this.opcodeObfuscator = opcodeObfuscator;
    }
    
    public void generate() throws IOException {
        generateHeader();
        generateSource();
    }

    private void buildInstructionIndex() {
        for (Instruction inst : allInstructions) {
            int opcode = inst.getOpcode();
            if (opcode >= 0 && opcode < instructionByOpcode.length) {
                instructionByOpcode[opcode] = inst;
            }
        }
    }

    private Instruction getInstructionByOriginalOpcode(int opcode) {
        if (opcode < 0 || opcode >= instructionByOpcode.length) {
            return null;
        }
        return instructionByOpcode[opcode];
    }
    
    private void generateHeader() throws IOException {
        try (PrintWriter w = new PrintWriter(new java.io.FileWriter(new File(dir, "vm_interpreter.h")))) {
            w.println("#ifndef VM_INTERPRETER_H");
            w.println("#define VM_INTERPRETER_H");
            w.println("#include \"vm_types.h\"");
            w.println();
            
            // Frame memory pool initialization
            w.println("// Frame memory pool initialization (called in JNI_OnLoad)");
            w.println("void frame_pool_init(void);");
            w.println();

            // VM method lookup initialization
            w.println("// VM method lookup initialization (called in JNI_OnLoad)");
            w.println("void vm_init_method_lookup(void);");
            w.println();

            // Helper function declarations
            for (VMHelper helper : helpers.getAllHelpers()) {
                helper.generateHeader(w);
            }

            w.println();
            // Execution result struct (for return value and type)
            w.println("// Execution result struct (for return value and type)");
            w.println("typedef struct {");
            w.println("    VMValue value;");
            w.println("    char returnType;  // 'V', 'I', 'J', 'F', 'D', 'L'");
            w.println("} ExecuteResult;");
            w.println();

            // Execution function declarations for each return type
            // New format: args[0]=instance, args[1..n]=params, args[n+1]=callerClass
            emitWrapperDeclarations(w);
            w.println();
            // Internal function for VM-to-VM direct calls
            w.println("// Internal: direct VM-to-VM call with pre-built locals");
            w.println("ExecuteResult vm_execute_common(JNIEnv* env, int methodId, jobjectArray args, VMValue* directLocals, int directLocalSlots, jclass callerClass);");
            w.println("#endif");
        }
    }

    private void emitWrapperDeclarations(PrintWriter w) {
        for (WrapperSpec spec : WRAPPER_SPECS) {
            w.println(spec.returnType + " " + spec.functionName + "(JNIEnv* env, int methodId, jobjectArray args);");
        }
    }
    
    private void generateSource() throws IOException {
        try (PrintWriter w = new PrintWriter(new java.io.FileWriter(new File(dir, "vm_interpreter.c")))) {
            w.println("#define _POSIX_C_SOURCE 200809L");
            w.println("#include \"vm_interpreter.h\"");
            w.println("#include \"vm_data.h\"");
            w.println("#include \"chacha20.h\"");
            w.println("#include <stdio.h>");
            w.println("#include <stdlib.h>");
            w.println("#include <string.h>");
            w.println("#include <stdatomic.h>  // for atomic cache operations");
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
            
            // === Branch prediction hint macros ===
            w.println("// === Branch prediction hints ===");
            w.println("#define LIKELY(x)   __builtin_expect(!!(x), 1)");
            w.println("#define UNLIKELY(x) __builtin_expect(!!(x), 0)");
            w.println();

            // === Class and method cache system ===
            emitCachingSystem(w);

            // Helper function implementations
            for (VMHelper helper : helpers.getAllHelpers()) {
                helper.generateSource(w);
            }

            // String decryption function
            w.println("static void vm_decrypt_string(const unsigned char* enc, int len, char* out, const unsigned char* key) {");
            w.println("    for (int i = 0; i < len; i++) {");
            w.println("        out[i] = (char)((enc[i] - (i & 0xFF)) ^ key[i % 8]);");
            w.println("    }");
            w.println("    out[len] = '\\0';");
            w.println("}");
            w.println();
            
            // Main interpreter function (with return type parameter)
            emitExecuteCommon(w);

            // Wrapper functions for each return type
            emitExecuteWrappers(w);
        }
    }
    
    private void emitCachingSystem(PrintWriter w) {
        cachingSectionEmitter.emit(w);
    }
    
    private void emitExecuteCommon(PrintWriter w) {
        emitExecuteCommonPrelude(w);
        emitNeedsMetaTable(w);
        emitDispatchTable(w);
        emitDispatchPrologue(w);
        emitInstructionHandlers(w);
        emitExecuteCommonEpilogue(w);
    }

    private void emitExecuteCommonPrelude(PrintWriter w) {
        // ExecuteResult is now defined in vm_interpreter.h
        w.println("__attribute__((hot))");
        w.println("ExecuteResult vm_execute_common(JNIEnv* env, int methodId, jobjectArray args, VMValue* directLocals, int directLocalSlots, jclass callerClass) {");
        w.println("    frame_pool_ensure_init();");
        w.println("    ExecuteResult execResult = { .returnType = 'V' };");
        w.println("    methodId ^= METHOD_ID_XOR_KEY;");
        w.println("    if (methodId < 0 || methodId >= vm_method_count) {");
        w.println("        execResult.returnType = 'E';  // Error");
        w.println("        return execResult;");
        w.println("    }");
        w.println("    VMMethod* m = &vm_methods[methodId];");
        w.println();

        w.println("    uint8_t* bytecode = m->bytecode;");
        w.println();

        w.println("    jobject instance = NULL;");
        w.println("    if (!directLocals && args) {");
        w.println("        jsize argsLen = (*env)->GetArrayLength(env, args);");
        w.println("        instance = (argsLen > 0) ? (*env)->GetObjectArrayElement(env, args, 0) : NULL;");
        w.println("        callerClass = (argsLen > 1 && m->argCount + 1 < argsLen) ?");
        w.println("            (jclass)(*env)->GetObjectArrayElement(env, args, argsLen - 1) : callerClass;");
        w.println("    }");
        w.println();

        w.println("    VMFrame frame = { .pc = 0, .sp = 0, .callerClass = callerClass };");
        w.println("    frame.stack = frame_pool_push(m->maxStack);");
        w.println();

        w.println("    const char* methodDesc = (m->descIdx >= 0) ? vm_get_string(m->descIdx) : NULL;");
        w.println("    if (directLocals) {");
        w.println("        frame.locals = directLocals;  // reuse caller's buffer directly (zero copy)");
        w.println("    } else {");
        w.println("        frame.locals = frame_pool_push(m->maxLocals);");
        w.println("        memset(frame.locals, 0, m->maxLocals * sizeof(VMValue));");
        w.println("        frame.locals[0].l = instance;");
        w.println("        const char* argTypes = (m->argTypesIdx >= 0) ? vm_get_string(m->argTypesIdx) : NULL;");
        w.println("        // Unbox args[1..n] (skip args[0]=instance, skip last element=callerClass)");
        w.println("        jsize argsLen = args ? (*env)->GetArrayLength(env, args) : 0;");
        w.println("        vm_unbox_args_fast(env, &frame, args, argTypes, m->argCount, instance ? 1 : 0, argsLen > 1 ? argsLen - 1 : 1);");
        w.println("    }");
        w.println();
    }

    private void emitNeedsMetaTable(PrintWriter w) {
        w.println("    // Metadata requirement table (indexed by obfuscated opcode)");
        w.println("    static const uint8_t needs_meta[256] = {");
        StringBuilder metaTable = new StringBuilder();
        for (int i = 0; i < 256; i++) {
            int originalOp = opcodeObfuscator.decode(i);
            Instruction inst = getInstructionByOriginalOpcode(originalOp);
            boolean needsMeta = inst != null && inst.needsMeta();
            if (i % 32 == 0) metaTable.append("        ");
            metaTable.append(needsMeta ? "1" : "0");
            metaTable.append(i < 255 ? "," : "");
            if ((i + 1) % 32 == 0) {
                w.println(metaTable.toString());
                metaTable = new StringBuilder();
            }
        }
        if (metaTable.length() > 0) {
            w.println(metaTable.toString());
        }
        w.println("    };");
        w.println();
    }

    private void emitDispatchTable(PrintWriter w) {
        w.println("    // Dispatch table (indexed by obfuscated opcode)");
        w.println("    static const void* dispatch_table[256] = {");
        for (int i = 0; i < 256; i++) {
            int originalOp = opcodeObfuscator.decode(i);
            Instruction inst = getInstructionByOriginalOpcode(originalOp);
            if (inst != null) {
                w.printf("        &&OP_%02x,%n", originalOp);
            } else {
                w.printf("        &&OP_DEFAULT,%n");
            }
        }
        w.println("    };");
        w.println();
    }

    private void emitDispatchPrologue(PrintWriter w) {
        w.println("    #define DISPATCH_NEXT \\");
        w.println("        do { \\");
        w.println("            uint8_t _op = bytecode[frame.pc]; \\");
        w.println("            int _metaIdx = m->pcToMetaIdx[frame.pc]; \\");
        w.println("            meta = (_metaIdx >= 0) ? &m->metadata[_metaIdx] : NULL; \\");
        w.println("            goto *dispatch_table[_op]; \\");
        w.println("        } while(0)");
        w.println();

        w.println("    int _hasException = 0;  // set to 1 when unhandled exception causes exit");
        w.println("    MetaEntry* meta = NULL;");
        w.println("    DISPATCH_NEXT;");
        w.println();
    }

    private void emitInstructionHandlers(PrintWriter w) {
        for (int i = 0; i < 256; i++) {
            int originalOp = opcodeObfuscator.decode(i);
            Instruction inst = getInstructionByOriginalOpcode(originalOp);
            if (inst != null) {
                inst.generateComputedGoto(w);
                w.println();
            }
        }
    }

    private void emitExecuteCommonEpilogue(PrintWriter w) {
        w.println("        OP_DEFAULT:");
        w.println("            VM_LOG(\"Unknown opcode: 0x%02x at pc=%d\\n\", bytecode[frame.pc], frame.pc);");
        w.println("            frame.pc++;");
        w.println("            DISPATCH_NEXT;");
        w.println();

        w.println("    method_exit:");
        w.println("    ;");
        w.println("    if (UNLIKELY(_hasException)) {");
        w.println("        execResult.returnType = 'X';  // signal unhandled exception");
        w.println("    } else {");
        w.println("        // Get return type from method descriptor");
        w.println("        if (methodDesc) {");
        w.println("            const char* p = methodDesc;");
        w.println("            while (*p && *p != ')') p++;");
        w.println("            if (*p == ')') execResult.returnType = *(p + 1);");
        w.println("        }");
        w.println("        // Get return value from top of stack");
        w.println("        if (frame.sp > 0) {");
        w.println("            execResult.value = frame.stack[frame.sp - 1];");
        w.println("        }");
        w.println("    }");
        w.println("    if (!directLocals) frame_pool_pop(m->maxLocals);");
        w.println("    frame_pool_pop(m->maxStack);");
        w.println("    return execResult;");
        w.println("}");
        w.println();
    }

    private void emitExecuteWrappers(PrintWriter w) {
        for (WrapperSpec spec : WRAPPER_SPECS) {
            emitWrapperFunction(w, spec.returnType, spec.functionName, spec.returnStatement);
        }
    }

    private void emitWrapperFunction(PrintWriter w, String returnType, String functionName, String returnStatement) {
        w.println("__attribute__((hot))");
        w.println(returnType + " " + functionName + "(JNIEnv* env, int methodId, jobjectArray args) {");
        w.println("    ExecuteResult r = vm_execute_common(env, methodId, args, NULL, 0, NULL);");
        w.println("    " + returnStatement);
        w.println("}");
        w.println();
    }
}
