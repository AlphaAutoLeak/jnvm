package com.alphaautoleak.jnvm.asm;

import com.alphaautoleak.jnvm.crypto.OpcodeObfuscator;
import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

import java.util.*;

/**
 * Serializes ASM InsnList to custom bytecode format.
 *
 * New format design:
 *   - Bytecode: each instruction has only opcode (1 byte), obfuscated
 *   - Metadata: instruction operands stored in separate MetaEntry array
 *   - pcToMetaIdx: PC to metadata index mapping array
 *
 * This allows C interpreter to get current instruction metadata via pcToMetaIdx[pc].
 */
public class BytecodeExtractor {

    private final ClassNode classNode;
    private final MethodNode methodNode;

    /** Opcode obfuscator (shared globally) */
    private final OpcodeObfuscator opcodeObfuscator;

    /** Bytecode buffer */
    private final List<Integer> bytecodes = new ArrayList<>();
    
    /** Metadata list */
    private final List<MetaEntry> metadataList = new ArrayList<>();
    
    /** PC to metadata index mapping */
    private final Map<Integer, Integer> pcToMetaIdx = new HashMap<>();
    
    /** String pool */
    private final List<String> stringPool = new ArrayList<>();
    private final Map<String, Integer> stringPoolIdx = new HashMap<>();

    /** Exception table */
    private final List<ExceptionEntry> exceptionTable = new ArrayList<>();

    /** Bootstrap method table */
    private final BootstrapMethodRegistry bootstrapRegistry = new BootstrapMethodRegistry();

    /** Stack type frames from ASM analysis (for 64-bit stack op transformation) */
    private Frame<BasicValue>[] frames;

    /** Label to PC mapping */
    private final Map<LabelNode, Integer> labelToPc = new HashMap<>();
    
    /** Jumps to backfill: (metadata index, target Label) */
    private final List<JumpBackpatch> jumpBackpatches = new ArrayList<>();
    
    /** Switch backfill */
    private final List<SwitchBackpatch> switchBackpatches = new ArrayList<>();
    
    /** Switch backfill info */
    private static class SwitchBackpatch {
        final int metaIdx;
        final int srcPc;
        final LabelNode defaultLabel;
        final List<LabelNode> caseLabels;
        
        SwitchBackpatch(int metaIdx, int srcPc, LabelNode defaultLabel, List<LabelNode> caseLabels) {
            this.metaIdx = metaIdx;
            this.srcPc = srcPc;
            this.defaultLabel = defaultLabel;
            this.caseLabels = caseLabels;
        }
    }
    
    /** Jump backfill info */
    private static class JumpBackpatch {
        final int metaIdx;
        final int srcPc;
        final LabelNode targetLabel;
        
        JumpBackpatch(int metaIdx, int srcPc, LabelNode targetLabel) {
            this.metaIdx = metaIdx;
            this.srcPc = srcPc;
            this.targetLabel = targetLabel;
        }
    }


    public BytecodeExtractor(ClassNode cn, MethodNode mn, OpcodeObfuscator opcodeObfuscator) {
        this.classNode = cn;
        this.methodNode = mn;
        this.opcodeObfuscator = opcodeObfuscator;
    }
    
    /**
     * Legacy constructor (no obfuscation) - uses identity mapping
     */
    public BytecodeExtractor(ClassNode cn, MethodNode mn) {
        this(cn, mn, new OpcodeObfuscator() {
            @Override public int encode(int opcode) { return opcode; }
            @Override public int decode(int obfuscated) { return obfuscated; }
        });
    }

    /**
     * Performs extraction
     */
    @SuppressWarnings("unchecked")
    public void extract() {
        // Run stack type analysis for 64-bit stack operation transformation
        try {
            Analyzer<BasicValue> analyzer = new Analyzer<>(new BasicInterpreter());
            frames = analyzer.analyze(classNode.name, methodNode);
        } catch (AnalyzerException e) {
            frames = null; // Fall back to no transformation
        }

        // First pass: generate bytecode and metadata
        firstPass();

        // Backfill jump targets
        backpatchJumps();

        // Extract exception table
        extractExceptionTable();
    }
    
    /**
     * First pass: traverse instructions, generate bytecode and metadata
     * Label maps to current PC (bytecodes.size())
     */
    private void firstPass() {
        InsnList insns = methodNode.instructions;

        // Traverse all instructions, generate bytecode and metadata
        // Also collect Label PCs
        for (int i = 0; i < insns.size(); i++) {
            AbstractInsnNode node = insns.get(i);

            // Handle Label - map to current PC
            if (node instanceof LabelNode) {
                LabelNode labelNode = (LabelNode) node;
                // Label PC is current bytecodes position
                int pc = bytecodes.size();
                labelToPc.put(labelNode, pc);
                continue;
            }

            // Skip LineNumber and Frame
            if (node instanceof LineNumberNode || node instanceof FrameNode) {
                continue;
            }

            // Emit instruction (with instruction index for stack analysis)
            emitInstruction(node, i);
        }
    }

    private void emitInstruction(AbstractInsnNode node, int insnIndex) {
        int opcode = node.getOpcode();
        int pc = bytecodes.size();

        // Transform stack operations for 64-bit VM (1 slot per long/double)
        opcode = transformStackOpFor64Bit(opcode, insnIndex);

        // Write obfuscated opcode
        bytecodes.add(opcodeObfuscator.encode(opcode));
        
        switch (node.getType()) {
            case AbstractInsnNode.INSN:
                // No operand instruction
                pcToMetaIdx.put(pc, -1);
                break;
                
            case AbstractInsnNode.INT_INSN:
                emitIntInsn((IntInsnNode) node, pc);
                break;
                
            case AbstractInsnNode.VAR_INSN:
                emitVarInsn((VarInsnNode) node, pc);
                break;
                
            case AbstractInsnNode.TYPE_INSN:
                emitTypeInsn((TypeInsnNode) node, pc);
                break;
                
            case AbstractInsnNode.FIELD_INSN:
                emitFieldInsn((FieldInsnNode) node, pc);
                break;
                
            case AbstractInsnNode.METHOD_INSN:
                emitMethodInsn((MethodInsnNode) node, pc);
                break;
                
            case AbstractInsnNode.INVOKE_DYNAMIC_INSN:
                emitInvokeDynamicInsn((InvokeDynamicInsnNode) node, pc);
                break;
                
            case AbstractInsnNode.JUMP_INSN:
                emitJumpInsn((JumpInsnNode) node, pc);
                break;
                
            case AbstractInsnNode.LDC_INSN:
                emitLdcInsn((LdcInsnNode) node, pc);
                break;
                
            case AbstractInsnNode.IINC_INSN:
                emitIincInsn((IincInsnNode) node, pc);
                break;
                
            case AbstractInsnNode.TABLESWITCH_INSN:
                emitTableSwitchInsn((TableSwitchInsnNode) node, pc);
                break;
                
            case AbstractInsnNode.LOOKUPSWITCH_INSN:
                emitLookupSwitchInsn((LookupSwitchInsnNode) node, pc);
                break;
                
            case AbstractInsnNode.MULTIANEWARRAY_INSN:
                emitMultiANewArrayInsn((MultiANewArrayInsnNode) node, pc);
                break;
                
            default:
                pcToMetaIdx.put(pc, -1);
                break;
        }
    }

    // ===== Metadata generation for each instruction type =====

    private void emitIntInsn(IntInsnNode node, int pc) {
        MetaEntry meta = new MetaEntry();
        meta.type = MetaType.META_INT;
        meta.intVal = node.operand;
        addMeta(pc, meta);
    }

    private void emitVarInsn(VarInsnNode node, int pc) {
        MetaEntry meta = new MetaEntry();
        meta.type = MetaType.META_LOCAL;
        meta.intVal = node.var;
        addMeta(pc, meta);
    }

    private void emitTypeInsn(TypeInsnNode node, int pc) {
        MetaEntry meta = new MetaEntry();
        meta.type = MetaType.META_CLASS;
        meta.classIdx = getStringIndex(node.desc);
        meta.classLen = node.desc.length();
        addMeta(pc, meta);
    }

    private void emitFieldInsn(FieldInsnNode node, int pc) {
        MetaEntry meta = new MetaEntry();
        meta.type = MetaType.META_FIELD;
        fillMemberMeta(meta, node.owner, node.name, node.desc);
        addMeta(pc, meta);
    }

    private void emitMethodInsn(MethodInsnNode node, int pc) {
        MetaEntry meta = new MetaEntry();
        meta.type = MetaType.META_METHOD;
        fillMemberMeta(meta, node.owner, node.name, node.desc);
        addMeta(pc, meta);
    }

    private void emitInvokeDynamicInsn(InvokeDynamicInsnNode node, int pc) {
        int bsmIdx = bootstrapRegistry.findOrCreate(node.bsm, node.bsmArgs);
        
        MetaEntry meta = new MetaEntry();
        meta.type = MetaType.META_INVOKE_DYNAMIC;
        meta.bsmIdx = bsmIdx;
        meta.nameIdx = getStringIndex(node.name);
        meta.nameLen = node.name.length();
        meta.descIdx = getStringIndex(node.desc);
        meta.descLen = node.desc.length();
        addMeta(pc, meta);
    }

    private void emitJumpInsn(JumpInsnNode node, int pc) {
        MetaEntry meta = new MetaEntry();
        meta.type = MetaType.META_JUMP;
        // Offset backfilled later
        meta.jumpOffset = 0;
        int idx = addMeta(pc, meta);
        jumpBackpatches.add(new JumpBackpatch(idx, pc, node.label));
    }

    private void emitLdcInsn(LdcInsnNode node, int pc) {
        MetaEntry meta = new MetaEntry();
        Object cst = node.cst;
        
        if (cst instanceof Integer) {
            meta.type = MetaType.META_INT;
            meta.intVal = (Integer) cst;
        } else if (cst instanceof Long) {
            meta.type = MetaType.META_LONG;
            meta.longVal = (Long) cst;
        } else if (cst instanceof Float) {
            meta.type = MetaType.META_FLOAT;
            meta.floatVal = (Float) cst;
        } else if (cst instanceof Double) {
            meta.type = MetaType.META_DOUBLE;
            meta.doubleVal = (Double) cst;
        } else if (cst instanceof String) {
            meta.type = MetaType.META_STRING;
            meta.strIdx = getStringIndex((String) cst);
            meta.strLen = ((String) cst).length();
        } else if (cst instanceof Type) {
            Type t = (Type) cst;
            meta.type = MetaType.META_CLASS;
            // Use internal name (e.g. "java/lang/String") instead of descriptor (e.g. "Ljava/lang/String;")
            String internalName = t.getInternalName();
            meta.classIdx = getStringIndex(internalName);
            meta.classLen = internalName.length();
        } else if (cst instanceof Handle) {
            // MethodHandle - temporarily stored as string
            Handle h = (Handle) cst;
            meta.type = MetaType.META_METHOD;
            fillMemberMeta(meta, h.getOwner(), h.getName(), h.getDesc());
        } else {
            throw new RuntimeException("Unsupported LDC constant: " + cst.getClass());
        }

        addMeta(pc, meta);
    }

    private void emitIincInsn(IincInsnNode node, int pc) {
        MetaEntry meta = new MetaEntry();
        meta.type = MetaType.META_IINC;
        meta.iincIndex = node.var;
        meta.iincConst = node.incr;
        addMeta(pc, meta);
    }

    private void emitTableSwitchInsn(TableSwitchInsnNode node, int pc) {
        MetaEntry meta = new MetaEntry();
        meta.type = MetaType.META_SWITCH;
        meta.switchLow = node.min;
        meta.switchHigh = node.max;
        meta.switchOffsets = new int[node.labels.size() + 1]; // default + cases

        int idx = addMeta(pc, meta);
        switchBackpatches.add(new SwitchBackpatch(idx, pc, node.dflt, node.labels));
    }

    private void emitLookupSwitchInsn(LookupSwitchInsnNode node, int pc) {
        MetaEntry meta = new MetaEntry();
        meta.type = MetaType.META_SWITCH;
        meta.switchLow = node.keys.size();  // npairs (number of key-offset pairs)
        meta.switchHigh = 0;                // unused for LOOKUPSWITCH
        meta.switchKeys = new int[node.keys.size()];
        meta.switchOffsets = new int[node.keys.size() + 1]; // cases + default
        
        for (int i = 0; i < node.keys.size(); i++) {
            meta.switchKeys[i] = node.keys.get(i);
        }

        int idx = addMeta(pc, meta);
        switchBackpatches.add(new SwitchBackpatch(idx, pc, node.dflt, node.labels));
    }

    private void emitMultiANewArrayInsn(MultiANewArrayInsnNode node, int pc) {
        MetaEntry meta = new MetaEntry();
        meta.type = MetaType.META_TYPE;
        meta.classIdx = getStringIndex(node.desc);
        meta.classLen = node.desc.length();
        meta.dims = node.dims;
        addMeta(pc, meta);
    }

    private int addMeta(int pc, MetaEntry meta) {
        int idx = metadataList.size();
        metadataList.add(meta);
        pcToMetaIdx.put(pc, idx);
        return idx;
    }

    private void fillMemberMeta(MetaEntry meta, String owner, String name, String desc) {
        meta.ownerIdx = getStringIndex(owner);
        meta.ownerLen = owner.length();
        meta.nameIdx = getStringIndex(name);
        meta.nameLen = name.length();
        meta.descIdx = getStringIndex(desc);
        meta.descLen = desc.length();
    }

    // ===== Backfill jumps =====

    private void backpatchJumps() {
        // Backfill normal jumps
        for (JumpBackpatch bp : jumpBackpatches) {
            Integer targetPc = labelToPc.get(bp.targetLabel);
            if (targetPc == null) {
                throw new RuntimeException("Unresolved label in jump");
            }
            MetaEntry meta = metadataList.get(bp.metaIdx);
            // Store absolute PC instead of offset
            meta.jumpOffset = targetPc;
        }
        
        // Backfill switch
        for (SwitchBackpatch bp : switchBackpatches) {
            Integer defaultPc = labelToPc.get(bp.defaultLabel);
            if (defaultPc == null) {
                throw new RuntimeException("Unresolved default label in switch");
            }
            
            MetaEntry meta = metadataList.get(bp.metaIdx);
            // Store absolute PC instead of offset
            meta.switchOffsets[0] = defaultPc;
            
            for (int i = 0; i < bp.caseLabels.size(); i++) {
                Integer casePc = labelToPc.get(bp.caseLabels.get(i));
                if (casePc == null) {
                    throw new RuntimeException("Unresolved case label in switch");
                }
                meta.switchOffsets[i + 1] = casePc;
            }
        }
    }

    // ===== Extract exception table =====

    private void extractExceptionTable() {
        if (methodNode.tryCatchBlocks == null) return;
        
        for (TryCatchBlockNode tcb : methodNode.tryCatchBlocks) {
            Integer startPc = labelToPc.get(tcb.start);
            Integer endPc = labelToPc.get(tcb.end);
            Integer handlerPc = labelToPc.get(tcb.handler);
            
            if (startPc == null || endPc == null || handlerPc == null) {
                continue;
            }
            
            ExceptionEntry entry = new ExceptionEntry(startPc, endPc, handlerPc, tcb.type);
            exceptionTable.add(entry);
        }
    }

    // ===== 64-bit stack operation transformation =====

    /**
     * Transforms stack manipulation opcodes for the 64-bit VM where long/double
     * occupy 1 slot instead of 2. Without this, DUP2/POP2/DUP_X2 etc. corrupt
     * the stack when operating on category-2 values (long/double).
     */
    private int transformStackOpFor64Bit(int opcode, int insnIndex) {
        if (frames == null || insnIndex < 0 || insnIndex >= frames.length || frames[insnIndex] == null) {
            return opcode;
        }
        Frame<BasicValue> frame = frames[insnIndex];
        int stackSize = frame.getStackSize();

        switch (opcode) {
            case Opcodes.DUP2: // 0x5c
                // Form 1: 2 cat1 → dup both (keep DUP2)
                // Form 2: 1 cat2 → dup it (use DUP)
                if (stackSize >= 1 && frame.getStack(stackSize - 1).getSize() == 2) {
                    return Opcodes.DUP;
                }
                break;

            case Opcodes.POP2: // 0x58
                // Form 1: 2 cat1 → pop both (keep POP2)
                // Form 2: 1 cat2 → pop it (use POP)
                if (stackSize >= 1 && frame.getStack(stackSize - 1).getSize() == 2) {
                    return Opcodes.POP;
                }
                break;

            case Opcodes.DUP_X2: // 0x5b
                // Form 1: cat1, cat1, cat1 → insert top below 3 (keep DUP_X2)
                // Form 2: cat1 on top, cat2 below → insert top below 2 (use DUP_X1)
                if (stackSize >= 2 && frame.getStack(stackSize - 2).getSize() == 2) {
                    return Opcodes.DUP_X1;
                }
                break;

            case Opcodes.DUP2_X1: // 0x5d
                // Form 1: cat1, cat1 on top, cat1 below → (keep DUP2_X1)
                // Form 2: cat2 on top, cat1 below → insert below 2 (use DUP_X1)
                if (stackSize >= 1 && frame.getStack(stackSize - 1).getSize() == 2) {
                    return Opcodes.DUP_X1;
                }
                break;

            case Opcodes.DUP2_X2: // 0x5e
                if (stackSize >= 1) {
                    boolean topIsCat2 = frame.getStack(stackSize - 1).getSize() == 2;
                    if (topIsCat2) {
                        // Check what's below
                        boolean belowIsCat2 = stackSize >= 2 && frame.getStack(stackSize - 2).getSize() == 2;
                        if (belowIsCat2) {
                            // Form 4: cat2 on top, cat2 below → DUP_X1
                            return Opcodes.DUP_X1;
                        } else {
                            // Form 2: cat2 on top, 2×cat1 below → DUP_X2
                            return Opcodes.DUP_X2;
                        }
                    } else if (stackSize >= 3) {
                        boolean belowBelowIsCat2 = frame.getStack(stackSize - 3).getSize() == 2;
                        if (belowBelowIsCat2) {
                            // Form 3: 2×cat1 on top, cat2 below → DUP2_X1
                            return Opcodes.DUP2_X1;
                        }
                    }
                }
                break;
        }
        return opcode;
    }

    // ===== String pool management =====

    private int getStringIndex(String s) {
        Integer idx = stringPoolIdx.get(s);
        if (idx != null) return idx;
        
        idx = stringPool.size();
        stringPool.add(s);
        stringPoolIdx.put(s, idx);
        return idx;
    }

    // ===== Result getters =====

    public byte[] getBytecode() {
        byte[] result = new byte[bytecodes.size()];
        for (int i = 0; i < bytecodes.size(); i++) {
            result[i] = (byte) bytecodes.get(i).intValue();
        }
        return result;
    }

    public List<MetaEntry> getMetadata() {
        return metadataList;
    }

    public int[] getPcToMetaIdx() {
        int[] result = new int[bytecodes.size()];
        for (int i = 0; i < bytecodes.size(); i++) {
            Integer idx = pcToMetaIdx.get(i);
            result[i] = idx != null ? idx : -1;
        }
        return result;
    }

    public List<String> getStringPool() {
        return stringPool;
    }

    public List<ExceptionEntry> getExceptionTable() {
        return exceptionTable;
    }

    public List<BootstrapEntry> getBootstrapMethods() {
        return bootstrapRegistry.getEntries();
    }
}
