package com.alphaautoleak.jnvm.asm;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.util.*;

/**
 * 将 ASM 的 InsnList 序列化为自定义格式的字节码。
 *
 * 新格式设计：
 *   - 字节码：每条指令只有 opcode（1 字节）
 *   - 元数据：指令操作数存储在独立的 MetaEntry 数组中
 *   - pcToMetaIdx：PC -> 元数据索引的映射数组
 *
 * 这样 C 解释器可以通过 pcToMetaIdx[pc] 获取当前指令的元数据。
 */
public class BytecodeExtractor {

    private final ClassNode classNode;
    private final MethodNode methodNode;

    /** 字节码缓冲区 */
    private final List<Integer> bytecodes = new ArrayList<>();
    
    /** 元数据列表 */
    private final List<MetaEntry> metadataList = new ArrayList<>();
    
    /** PC -> 元数据索引映射 */
    private final Map<Integer, Integer> pcToMetaIdx = new HashMap<>();
    
    /** 字符串池 */
    private final List<String> stringPool = new ArrayList<>();
    private final Map<String, Integer> stringPoolIdx = new HashMap<>();

    /** 异常表 */
    private final List<ExceptionEntry> exceptionTable = new ArrayList<>();

    /** Bootstrap 方法表 */
    private final List<BootstrapEntry> bootstrapMethods = new ArrayList<>();

    /** Label -> PC 映射 */
    private final Map<LabelNode, Integer> labelToPc = new HashMap<>();
    
    /** 需要回填的跳转：(元数据索引, 目标 Label) */
    private final List<JumpBackpatch> jumpBackpatches = new ArrayList<>();
    
    /** Switch 回填 */
    private final List<SwitchBackpatch> switchBackpatches = new ArrayList<>();
    
    /** Switch 回填信息 */
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
    
    /** 跳转回填信息 */
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


    public BytecodeExtractor(ClassNode cn, MethodNode mn) {
        this.classNode = cn;
        this.methodNode = mn;
    }

    /**
     * 执行提取
     */
    public void extract() {
        // 第一遍：生成字节码和元数据
        firstPass();
        
        // 回填跳转目标
        backpatchJumps();
        
        // 提取异常表
        extractExceptionTable();
    }

    /**
     * 第一遍：遍历指令，生成字节码和元数据
     * 对于 Label，使用 ASM 提供的原始字节码偏移量
     */
    private void firstPass() {
        InsnList insns = methodNode.instructions;
        
        // 遍历所有指令，生成字节码和元数据
        // 同时收集 Label 的偏移量
        for (int i = 0; i < insns.size(); i++) {
            AbstractInsnNode node = insns.get(i);
            
            // 处理 Label
            if (node instanceof LabelNode) {
                LabelNode labelNode = (LabelNode) node;
                int offset;
                try {
                    // ASM 的 Label.getOffset() 返回原始字节码偏移量
                    offset = labelNode.getLabel().getOffset();
                    // 调试：打印 Label 偏移量
                    // System.out.println("Label offset: " + offset);
                } catch (IllegalStateException e) {
                    // 如果 Label 还没有被解析，使用当前字节码位置作为备选
                    offset = bytecodes.size();
                    System.out.println("Warning: Label not resolved, using fallback offset: " + offset);
                }
                labelToPc.put(labelNode, offset);
                continue;
            }
            
            // 跳过 LineNumber 和 Frame
            if (node instanceof LineNumberNode || node instanceof FrameNode) {
                continue;
            }
            
            // 发射指令
            emitInstruction(node);
        }
    }

    private void emitInstruction(AbstractInsnNode node) {
        int opcode = node.getOpcode();
        int pc = bytecodes.size();
        
        // 写入 opcode
        bytecodes.add(opcode);
        
        switch (node.getType()) {
            case AbstractInsnNode.INSN:
                // 无操作数指令
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

    // ===== 各类型指令的元数据生成 =====

    private void emitIntInsn(IntInsnNode node, int pc) {
        MetaEntry meta = new MetaEntry();
        meta.type = MetaType.META_INT;
        meta.intVal = node.operand;
        int idx = metadataList.size();
        metadataList.add(meta);
        pcToMetaIdx.put(pc, idx);
    }

    private void emitVarInsn(VarInsnNode node, int pc) {
        MetaEntry meta = new MetaEntry();
        meta.type = MetaType.META_LOCAL;
        meta.intVal = node.var;
        int idx = metadataList.size();
        metadataList.add(meta);
        pcToMetaIdx.put(pc, idx);
    }

    private void emitTypeInsn(TypeInsnNode node, int pc) {
        MetaEntry meta = new MetaEntry();
        meta.type = MetaType.META_CLASS;
        meta.classIdx = getStringIndex(node.desc);
        meta.classLen = node.desc.length();
        int idx = metadataList.size();
        metadataList.add(meta);
        pcToMetaIdx.put(pc, idx);
    }

    private void emitFieldInsn(FieldInsnNode node, int pc) {
        MetaEntry meta = new MetaEntry();
        meta.type = MetaType.META_FIELD;
        meta.ownerIdx = getStringIndex(node.owner);
        meta.ownerLen = node.owner.length();
        meta.nameIdx = getStringIndex(node.name);
        meta.nameLen = node.name.length();
        meta.descIdx = getStringIndex(node.desc);
        meta.descLen = node.desc.length();
        int idx = metadataList.size();
        metadataList.add(meta);
        pcToMetaIdx.put(pc, idx);
    }

    private void emitMethodInsn(MethodInsnNode node, int pc) {
        MetaEntry meta = new MetaEntry();
        meta.type = MetaType.META_METHOD;
        meta.ownerIdx = getStringIndex(node.owner);
        meta.ownerLen = node.owner.length();
        meta.nameIdx = getStringIndex(node.name);
        meta.nameLen = node.name.length();
        meta.descIdx = getStringIndex(node.desc);
        meta.descLen = node.desc.length();
        int idx = metadataList.size();
        metadataList.add(meta);
        pcToMetaIdx.put(pc, idx);
    }

    private void emitInvokeDynamicInsn(InvokeDynamicInsnNode node, int pc) {
        int bsmIdx = findOrCreateBootstrapMethod(node.bsm, node.bsmArgs);
        
        MetaEntry meta = new MetaEntry();
        meta.type = MetaType.META_INVOKE_DYNAMIC;
        meta.bsmIdx = bsmIdx;
        meta.nameIdx = getStringIndex(node.name);
        meta.nameLen = node.name.length();
        meta.descIdx = getStringIndex(node.desc);
        meta.descLen = node.desc.length();
        int idx = metadataList.size();
        metadataList.add(meta);
        pcToMetaIdx.put(pc, idx);
    }

    private void emitJumpInsn(JumpInsnNode node, int pc) {
        MetaEntry meta = new MetaEntry();
        meta.type = MetaType.META_JUMP;
        // 偏移量稍后回填
        meta.jumpOffset = 0;
        int idx = metadataList.size();
        metadataList.add(meta);
        pcToMetaIdx.put(pc, idx);
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
            String desc = t.getDescriptor();
            meta.classIdx = getStringIndex(desc);
            meta.classLen = desc.length();
        } else if (cst instanceof Handle) {
            // MethodHandle - 暂时作为字符串存储
            Handle h = (Handle) cst;
            meta.type = MetaType.META_METHOD;
            meta.ownerIdx = getStringIndex(h.getOwner());
            meta.ownerLen = h.getOwner().length();
            meta.nameIdx = getStringIndex(h.getName());
            meta.nameLen = h.getName().length();
            meta.descIdx = getStringIndex(h.getDesc());
            meta.descLen = h.getDesc().length();
        } else {
            throw new RuntimeException("Unsupported LDC constant: " + cst.getClass());
        }
        
        int idx = metadataList.size();
        metadataList.add(meta);
        pcToMetaIdx.put(pc, idx);
    }

    private void emitIincInsn(IincInsnNode node, int pc) {
        MetaEntry meta = new MetaEntry();
        meta.type = MetaType.META_IINC;
        meta.iincIndex = node.var;
        meta.iincConst = node.incr;
        int idx = metadataList.size();
        metadataList.add(meta);
        pcToMetaIdx.put(pc, idx);
    }

    private void emitTableSwitchInsn(TableSwitchInsnNode node, int pc) {
        MetaEntry meta = new MetaEntry();
        meta.type = MetaType.META_SWITCH;
        meta.switchLow = node.min;
        meta.switchHigh = node.max;
        meta.switchOffsets = new int[node.labels.size() + 1]; // default + cases
        
        int idx = metadataList.size();
        metadataList.add(meta);
        pcToMetaIdx.put(pc, idx);
        
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
        
        int idx = metadataList.size();
        metadataList.add(meta);
        pcToMetaIdx.put(pc, idx);
        
        switchBackpatches.add(new SwitchBackpatch(idx, pc, node.dflt, node.labels));
    }

    private void emitMultiANewArrayInsn(MultiANewArrayInsnNode node, int pc) {
        MetaEntry meta = new MetaEntry();
        meta.type = MetaType.META_TYPE;
        meta.classIdx = getStringIndex(node.desc);
        meta.classLen = node.desc.length();
        meta.dims = node.dims;
        int idx = metadataList.size();
        metadataList.add(meta);
        pcToMetaIdx.put(pc, idx);
    }

    // ===== 回填跳转 =====

    private void backpatchJumps() {
        // 回填普通跳转
        for (JumpBackpatch bp : jumpBackpatches) {
            Integer targetPc = labelToPc.get(bp.targetLabel);
            if (targetPc == null) {
                throw new RuntimeException("Unresolved label in jump");
            }
            MetaEntry meta = metadataList.get(bp.metaIdx);
            // 存储绝对 PC 而不是偏移量
            meta.jumpOffset = targetPc;
        }
        
        // 回填 switch
        for (SwitchBackpatch bp : switchBackpatches) {
            Integer defaultPc = labelToPc.get(bp.defaultLabel);
            if (defaultPc == null) {
                throw new RuntimeException("Unresolved default label in switch");
            }
            
            MetaEntry meta = metadataList.get(bp.metaIdx);
            // 存储绝对 PC 而不是偏移量
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

    // ===== 异常表提取 =====

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

    // ===== Bootstrap Methods =====

    private int findOrCreateBootstrapMethod(Handle bsm, Object[] bsmArgs) {
        // 查找时需要比较 args，不同的 args 应该创建不同的 BSM
        for (int i = 0; i < bootstrapMethods.size(); i++) {
            BootstrapEntry e = bootstrapMethods.get(i);
            if (e.getHandleTag() == bsm.getTag() &&
                e.getHandleOwner().equals(bsm.getOwner()) &&
                e.getHandleName().equals(bsm.getName()) &&
                e.getHandleDescriptor().equals(bsm.getDesc())) {
                // 还需要比较 args
                List<Object> existingArgs = e.getArguments();
                if (argsEqual(existingArgs, bsmArgs)) {
                    return i;
                }
            }
        }
        
        BootstrapEntry entry = new BootstrapEntry();
        entry.setHandleTag(bsm.getTag());
        entry.setHandleOwner(bsm.getOwner());
        entry.setHandleName(bsm.getName());
        entry.setHandleDescriptor(bsm.getDesc());
        
        List<Object> args = new ArrayList<>();
        List<BootstrapEntry.ArgType> argTypes = new ArrayList<>();
        
        if (bsmArgs != null) {
            for (Object arg : bsmArgs) {
                if (arg instanceof Integer) {
                    args.add(arg);
                    argTypes.add(BootstrapEntry.ArgType.INTEGER);
                } else if (arg instanceof Long) {
                    args.add(arg);
                    argTypes.add(BootstrapEntry.ArgType.LONG);
                } else if (arg instanceof Float) {
                    args.add(arg);
                    argTypes.add(BootstrapEntry.ArgType.FLOAT);
                } else if (arg instanceof Double) {
                    args.add(arg);
                    argTypes.add(BootstrapEntry.ArgType.DOUBLE);
                } else if (arg instanceof String) {
                    args.add(arg);
                    argTypes.add(BootstrapEntry.ArgType.STRING);
                } else if (arg instanceof Type) {
                    Type t = (Type) arg;
                    args.add(t.getDescriptor());
                    argTypes.add(BootstrapEntry.ArgType.METHOD_TYPE);
                } else if (arg instanceof Handle) {
                    Handle h = (Handle) arg;
                    String serialized = h.getTag() + ":" + h.getOwner() + ":" +
                            h.getName() + ":" + h.getDesc();
                    args.add(serialized);
                    argTypes.add(BootstrapEntry.ArgType.METHOD_HANDLE);
                } else {
                    args.add(arg.toString());
                    argTypes.add(BootstrapEntry.ArgType.STRING);
                }
            }
        }
        
        entry.setArguments(args);
        entry.setArgumentTypes(argTypes);
        
        int idx = bootstrapMethods.size();
        bootstrapMethods.add(entry);
        return idx;
    }
    
    private boolean argsEqual(List<Object> args1, Object[] args2) {
        if (args1 == null && args2 == null) return true;
        if (args1 == null || args2 == null) return false;
        if (args1.size() != args2.length) return false;
        
        for (int i = 0; i < args1.size(); i++) {
            Object a1 = args1.get(i);
            Object a2 = args2[i];
            if (a1 == null && a2 == null) continue;
            if (a1 == null || a2 == null) return false;
            
            // 处理 Handle 序列化的情况
            if (a2 instanceof Handle) {
                Handle h = (Handle) a2;
                String serialized = h.getTag() + ":" + h.getOwner() + ":" +
                        h.getName() + ":" + h.getDesc();
                if (!a1.toString().equals(serialized)) return false;
            } else if (a2 instanceof Type) {
                if (!a1.toString().equals(((Type) a2).getDescriptor())) return false;
            } else {
                if (!a1.toString().equals(a2.toString())) return false;
            }
        }
        return true;
    }

    // ===== 字符串池管理 =====

    private int getStringIndex(String s) {
        Integer idx = stringPoolIdx.get(s);
        if (idx != null) return idx;
        
        idx = stringPool.size();
        stringPool.add(s);
        stringPoolIdx.put(s, idx);
        return idx;
    }

    // ===== 结果获取 =====

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
        return bootstrapMethods;
    }

    // ===== 内部类型 =====

    public enum MetaType {
        META_NONE(0),
        META_INT(1),
        META_LONG(2),
        META_FLOAT(3),
        META_DOUBLE(4),
        META_STRING(5),
        META_CLASS(6),
        META_FIELD(7),
        META_METHOD(8),
        META_INVOKE_DYNAMIC(9),
        META_JUMP(10),
        META_SWITCH(11),
        META_LOCAL(12),
        META_IINC(13),
        META_NEWARRAY(14),
        META_TYPE(15);

        public final int value;
        MetaType(int v) { this.value = v; }
    }

    public static class MetaEntry {
        public MetaType type = MetaType.META_NONE;
        
        // META_INT, META_LOCAL, META_NEWARRAY
        public int intVal;
        
        // META_LONG
        public long longVal;
        
        // META_FLOAT
        public float floatVal;
        
        // META_DOUBLE
        public double doubleVal;
        
        // META_STRING
        public int strIdx;
        public int strLen;
        
        // META_CLASS
        public int classIdx;
        public int classLen;
        
        // META_FIELD, META_METHOD
        public int ownerIdx;
        public int ownerLen;
        public int nameIdx;
        public int nameLen;
        public int descIdx;
        public int descLen;
        
        // META_INVOKE_DYNAMIC
        public int bsmIdx;
        
        // META_JUMP
        public int jumpOffset;
        
        // META_IINC
        public int iincIndex;
        public int iincConst;
        
        // META_SWITCH
        public int switchLow;
        public int switchHigh;
        public int[] switchKeys;
        public int[] switchOffsets;
        
        // META_TYPE (multianewarray)
        public int dims;
    }
}
