package com.alphaautoleak.jnvm.codegen.emitter.instruction.control;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;
import com.alphaautoleak.jnvm.codegen.emitter.instruction.InstructionRegistry;

import java.io.PrintWriter;

/**
 * Control flow instructions registration
 */
public class ControlInstructions {
    
    public static void registerAll(InstructionRegistry registry) {
        // RETURN
        registry.register(new ReturnInstruction(0xb1, "RETURN", "void"));
        
        // IRETURN, LRETURN, FRETURN, DRETURN, ARETURN
        registry.register(new ReturnInstruction(0xac, "IRETURN", "int"));
        registry.register(new ReturnInstruction(0xad, "LRETURN", "long"));
        registry.register(new ReturnInstruction(0xae, "FRETURN", "float"));
        registry.register(new ReturnInstruction(0xaf, "DRETURN", "double"));
        registry.register(new ReturnInstruction(0xb0, "ARETURN", "object"));
        
        // GOTO
        registry.register(new GotoInstruction());
        
        // IFEQ, IFNE, IFLT, IFGE, IFGT, IFLE
        registry.register(new IfZeroInstruction(0x99, "IFEQ", "== 0"));
        registry.register(new IfZeroInstruction(0x9a, "IFNE", "!= 0"));
        registry.register(new IfZeroInstruction(0x9b, "IFLT", "< 0"));
        registry.register(new IfZeroInstruction(0x9c, "IFGE", ">= 0"));
        registry.register(new IfZeroInstruction(0x9d, "IFGT", "> 0"));
        registry.register(new IfZeroInstruction(0x9e, "IFLE", "<= 0"));
        
        // IF_ICMPEQ, IF_ICMPNE, IF_ICMPLT, IF_ICMPGE, IF_ICMPGT, IF_ICMPLE
        registry.register(new IfCmpInstruction(0x9f, "IF_ICMPEQ", "=="));
        registry.register(new IfCmpInstruction(0xa0, "IF_ICMPNE", "!="));
        registry.register(new IfCmpInstruction(0xa1, "IF_ICMPLT", "<"));
        registry.register(new IfCmpInstruction(0xa2, "IF_ICMPGE", ">="));
        registry.register(new IfCmpInstruction(0xa3, "IF_ICMPGT", ">"));
        registry.register(new IfCmpInstruction(0xa4, "IF_ICMPLE", "<="));
        
        // IF_ACMPEQ, IF_ACMPNE
        registry.register(new IfAcmpInstruction(0xa5, "IF_ACMPEQ", "=="));
        registry.register(new IfAcmpInstruction(0xa6, "IF_ACMPNE", "!="));
        
        // IFNULL, IFNONNULL
        registry.register(new IfNullInstruction(0xc6, "IFNULL", "== NULL"));
        registry.register(new IfNullInstruction(0xc7, "IFNONNULL", "!= NULL"));
        
        // TABLESWITCH (0xaa) and LOOKUPSWITCH (0xab)
        registry.register(new TableSwitchInstruction());
        registry.register(new LookupSwitchInstruction());
        
        // IINC
        registry.register(new IincInstruction());
        
        // ATHROW
        registry.register(new AThrowInstruction());
    }
    
    /**
     * TABLESWITCH instruction (0xaa)
     * Used for switch statements with contiguous keys
     * switchOffsets layout: [default, caseLow, caseLow+1, ..., caseHigh]
     * Values are absolute PC addresses
     */
    static class TableSwitchInstruction extends Instruction {
        public TableSwitchInstruction() {
            super(0xaa, "TABLESWITCH");
        }
        
        @Override
        protected void generateBody(PrintWriter w) {
            w.println("                {");
            w.println("                    jint key = frame.stack[--frame.sp].i;");
            w.println("                    MetaEntry* sw = meta;");
            w.println("                    if (sw) {");
            w.println("                        jint low = sw->switchLow;");
            w.println("                        jint high = sw->switchHigh;");
            w.println("                        // switchOffsets are absolute PC values");
            w.println("                        if (key >= low && key <= high) {");
            w.println("                            frame.pc = sw->switchOffsets[key - low + 1];");
            w.println("                        } else {");
            w.println("                            frame.pc = sw->switchOffsets[0];");
            w.println("                        }");
            w.println("                    } else {");
            w.println("                        frame.pc++;");
            w.println("                    }");
            w.println("                }");
        }
    }
    
    /**
     * LOOKUPSWITCH instruction (0xab)
     * Used for switch statements with sparse keys
     * For LOOKUPSWITCH:
     *   - switchLow = npairs (number of key-offset pairs)
     *   - switchHigh = unused
     *   - switchKeys array stores the actual key values
     *   - switchOffsets array: [default, case0, case1, ...] (absolute PC values!)
     */
    static class LookupSwitchInstruction extends Instruction {
        public LookupSwitchInstruction() {
            super(0xab, "LOOKUPSWITCH");
        }
        
        @Override
        protected void generateBody(PrintWriter w) {
            w.println("                {");
            w.println("                    jint key = frame.stack[--frame.sp].i;");
            w.println("                    MetaEntry* sw = meta;");
            w.println("                    if (sw) {");
            w.println("                        int npairs = sw->switchLow;");
            w.println("                        VM_LOG(\"LOOKUPSWITCH: key=%d, npairs=%d, switchKeys=%p\\n\", key, npairs, sw->switchKeys);");
            w.println("                        int found = 0;");
            w.println("                        if (sw->switchKeys) {");
            w.println("                            for (int i = 0; i < npairs; i++) {");
            w.println("                                VM_LOG(\"  checking key[%d]=%d\\n\", i, sw->switchKeys[i]);");
            w.println("                                if (sw->switchKeys[i] == key) {");
            w.println("                                    VM_LOG(\"  MATCH! jumping to pc=%d\\n\", sw->switchOffsets[i+1]);");
            w.println("                                    frame.pc = sw->switchOffsets[i+1];");
            w.println("                                    found = 1;");
            w.println("                                    break;");
            w.println("                                }");
            w.println("                            }");
            w.println("                        }");
            w.println("                        if (!found) {");
            w.println("                            VM_LOG(\"  No match, jumping to default pc=%d\\n\", sw->switchOffsets[0]);");
            w.println("                            frame.pc = sw->switchOffsets[0];");
            w.println("                        }");
            w.println("                    } else {");
            w.println("                        VM_LOG(\"LOOKUPSWITCH: meta is NULL!\\n\");");
            w.println("                        frame.pc++;");
            w.println("                    }");
            w.println("                }");
        }
    }
}
