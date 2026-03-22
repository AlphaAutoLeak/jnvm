package com.alphaautoleak.jnvm.codegen.emitter;

import com.alphaautoleak.jnvm.codegen.emitter.helper.VMHelpers;
import com.alphaautoleak.jnvm.crypto.OpcodeObfuscator;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;

/**
 * Generates vm_interpreter.h and vm_interpreter.c - VM interpreter core
 * Generates separate functions for each return type to avoid boxing/unboxing
 */
public class VmInterpreterGenerator {
    private static final List<VmInterpreterWrapperSpec> WRAPPER_SPECS = Arrays.asList(
            new VmInterpreterWrapperSpec("void", "vm_execute_method_void", "(void)r;  // ignore return value"),
            new VmInterpreterWrapperSpec("jint", "vm_execute_method_int", "return r.value.i;"),
            new VmInterpreterWrapperSpec("jlong", "vm_execute_method_long", "return r.value.j;"),
            new VmInterpreterWrapperSpec("jfloat", "vm_execute_method_float", "return r.value.f;"),
            new VmInterpreterWrapperSpec("jdouble", "vm_execute_method_double", "return r.value.d;"),
            new VmInterpreterWrapperSpec("jobject", "vm_execute_method_object", "return r.value.l;")
    );
    
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
    private final VmInterpreterCoreEmitter coreEmitter;
    
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
        this.coreEmitter = new VmInterpreterCoreEmitter(instructionByOpcode, opcodeObfuscator);
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
        VmInterpreterHeaderEmitter.emit(dir, helpers.getAllHelpers(), WRAPPER_SPECS);
    }
    
    private void generateSource() throws IOException {
        try (PrintWriter w = new PrintWriter(new java.io.FileWriter(new File(dir, "vm_interpreter.c")))) {
            VmInterpreterSourcePreambleEmitter.emit(
                    w,
                    debug,
                    encryptStrings,
                    methodIdXorKey,
                    cachingSectionEmitter,
                    helpers.getAllHelpers()
            );
            coreEmitter.emitExecuteCommon(w);
            VmInterpreterWrapperEmitter.emit(w, WRAPPER_SPECS);
        }
    }
}
