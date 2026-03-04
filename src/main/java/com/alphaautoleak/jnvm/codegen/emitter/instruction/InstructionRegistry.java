package com.alphaautoleak.jnvm.codegen.emitter.instruction;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Instruction registry interface
 */
public class InstructionRegistry {
    
    private final Map<Integer, Instruction> instructions = new HashMap<>();
    private final List<Instruction> instructionList = new ArrayList<>();
    
    public void register(Instruction inst) {
        instructions.put(inst.getOpcode(), inst);
        instructionList.add(inst);
    }
    
    public List<Instruction> getAllInstructions() {
        return instructionList;
    }
    
    public Instruction getInstruction(int opcode) {
        return instructions.get(opcode);
    }
}