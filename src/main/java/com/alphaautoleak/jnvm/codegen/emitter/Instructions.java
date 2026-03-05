package com.alphaautoleak.jnvm.codegen.emitter;

import com.alphaautoleak.jnvm.codegen.emitter.instruction.*;
import com.alphaautoleak.jnvm.codegen.emitter.instruction.stack.StackInstructions;
import com.alphaautoleak.jnvm.codegen.emitter.instruction.arithmetic.ArithmeticInstructions;
import com.alphaautoleak.jnvm.codegen.emitter.instruction.control.ControlInstructions;
import com.alphaautoleak.jnvm.codegen.emitter.instruction.object.ObjectInstructions;
import com.alphaautoleak.jnvm.codegen.emitter.instruction.array.ArrayInstructions;

import java.io.PrintWriter;
import java.util.List;

/**
 * JVM instruction registry - aggregates all instruction categories
 */
public class Instructions {
    
    private final InstructionRegistry registry;
    
    public Instructions() {
        registry = new InstructionRegistry();
        registerAll();
    }
    
    private void registerAll() {
        // Constant loading instructions
        ConstantsInstructions.registerAll(registry);
        
        // Load/Store instructions
        LoadStoreInstructions.registerAll(registry);
        
        // Stack operation instructions
        StackInstructions.registerAll(registry);
        
        // Arithmetic instructions
        ArithmeticInstructions.registerAll(registry);
        
        // Control flow instructions
        ControlInstructions.registerAll(registry);
        
        // Object and field operation instructions
        ObjectInstructions.registerAll(registry);
        
        // Array operation instructions
        ArrayInstructions.registerAll(registry);
        
        // NOP
        registry.register(new BaseInstructions.SimpleInstruction(0x00, "NOP", ""));
        
        // Monitor instructions (MONITORENTER=0xc2, MONITOREXIT=0xc3)
        registry.register(new Instruction(0xc2, "MONITORENTER") {
            @Override
            protected void generateBody(PrintWriter w) {
                w.println("                {");
                w.println("                    jobject obj = frame.stack[--frame.sp].l;");
                w.println("                    if (obj) {");
                w.println("                        (*env)->MonitorEnter(env, obj);");
                w.println("                    }");
                w.println("                }");
                pcIncBreak(w);
            }
        });
        
        registry.register(new Instruction(0xc3, "MONITOREXIT") {
            @Override
            protected void generateBody(PrintWriter w) {
                w.println("                {");
                w.println("                    jobject obj = frame.stack[--frame.sp].l;");
                w.println("                    if (obj) {");
                w.println("                        (*env)->MonitorExit(env, obj);");
                w.println("                    }");
                w.println("                }");
                pcIncBreak(w);
            }
        });
    }
    
    public List<Instruction> getAllInstructions() {
        return registry.getAllInstructions();
    }
    
    public Instruction getInstruction(int opcode) {
        return registry.getInstruction(opcode);
    }
}
