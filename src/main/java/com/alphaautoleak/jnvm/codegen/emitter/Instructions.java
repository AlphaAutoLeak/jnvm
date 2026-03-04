package com.alphaautoleak.jnvm.codegen.emitter;

import com.alphaautoleak.jnvm.codegen.emitter.instruction.*;

import java.util.List;

/**
 * JVM 指令注册表 - 聚合所有指令分类
 */
public class Instructions {
    
    private final InstructionRegistry registry;
    
    public Instructions() {
        registry = new InstructionRegistry();
        registerAll();
    }
    
    private void registerAll() {
        // 常量加载指令
        ConstantsInstructions.registerAll(registry);
        
        // 加载/存储指令
        LoadStoreInstructions.registerAll(registry);
        
        // 栈操作指令
        StackInstructions.registerAll(registry);
        
        // 算术指令
        ArithmeticInstructions.registerAll(registry);
        
        // 控制流指令
        ControlInstructions.registerAll(registry);
        
        // 对象和字段操作指令
        ObjectInstructions.registerAll(registry);
        
        // 数组操作指令
        ArrayInstructions.registerAll(registry);
        
        // NOP
        registry.register(new BaseInstructions.SimpleInstruction(0x00, "NOP", ""));
    }
    
    public List<Instruction> getAllInstructions() {
        return registry.getAllInstructions();
    }
    
    public Instruction getInstruction(int opcode) {
        return registry.getInstruction(opcode);
    }
}