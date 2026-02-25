package com.alphaautoleak.jnvm.asm;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.*;

/**
 * 将 ASM 的 InsnList 序列化为自定义格式的字节码。
 *
 * 自定义字节码格式：
 *   - opcode 保持原始 JVM opcode（1 字节）
 *   - 操作数统一使用 2 字节无符号 index（指向自定义常量池）
 *   - 跳转指令使用 4 字节有符号偏移（相对当前指令起始位置）
 *   - tableswitch / lookupswitch 使用自定义紧凑格式
 *
 * 这样 C 解释器可以用简单的 switch(opcode) + 读取固定长度操作数来解释执行。
 */
public class BytecodeExtractor {

    private final ClassNode classNode;
    private final MethodNode methodNode;

    /** 自定义字节码输出 */
    private final ByteArrayOutputStream bytecodeOut = new ByteArrayOutputStream(256);
    private final DataOutputStream dos = new DataOutputStream(bytecodeOut);

    /** 自定义常量池 */
    private final List<CPEntry> constantPool = new ArrayList<>();
    private int nextCpIndex = 0;

    /** 去重：key → cp index */
    private final Map<String, Integer> cpDedup = new HashMap<>();

    /** 异常表 */
    private final List<ExceptionEntry> exceptionTable = new ArrayList<>();

    /** Bootstrap 方法表 */
    private final List<BootstrapEntry> bootstrapMethods = new ArrayList<>();

    /** Label → 字节码偏移量映射（两遍扫描） */
    private final Map<LabelNode, Integer> labelOffsets = new HashMap<>();

    /** 需要回填的跳转指令：(bytecodePosition, targetLabel) */
    private final List<JumpPatch> jumpPatches = new ArrayList<>();

    /** switch 回填 */
    private final List<SwitchPatch> switchPatches = new ArrayList<>();

    public BytecodeExtractor(ClassNode cn, MethodNode mn) {
        this.classNode = cn;
        this.methodNode = mn;
    }

    /**
     * 执行提取：两遍扫描
     * 第一遍：生成字节码 + 记录 label 位置 + 记录回填点
     * 第二遍：回填跳转偏移
     */
    public void extract() {
        try {
            // 收集 bootstrap methods（类级别）
            extractBootstrapMethods();

            // 第一遍：生成字节码
            firstPass();

            // 回填跳转
            backpatchJumps();

            // 转换异常表
            extractExceptionTable();

        } catch (IOException e) {
            throw new RuntimeException("Bytecode extraction failed", e);
        }
    }

    /**
     * 第一遍扫描：遍历 InsnList，生成自定义字节码
     */
    private void firstPass() throws IOException {
        InsnList insns = methodNode.instructions;

        for (int i = 0; i < insns.size(); i++) {
            AbstractInsnNode node = insns.get(i);

            // 记录 Label 位置
            if (node instanceof LabelNode) {
                labelOffsets.put((LabelNode) node, bytecodeOut.size());
                continue; // Label 不生成字节码
            }

            // 跳过 LineNumber 和 Frame 节点
            if (node instanceof LineNumberNode || node instanceof FrameNode) {
                continue;
            }

            emitInstruction(node);
        }
    }

    /**
     * 发射单条指令的自定义字节码
     */
    private void emitInstruction(AbstractInsnNode node) throws IOException {
        int opcode = node.getOpcode();

        switch (node.getType()) {
            case AbstractInsnNode.INSN:
                // 无操作数指令：nop, aconst_null, iconst_m1~5, return, iadd, ...
                emitOpcode(opcode);
                break;

            case AbstractInsnNode.INT_INSN:
                emitIntInsn((IntInsnNode) node);
                break;

            case AbstractInsnNode.VAR_INSN:
                emitVarInsn((VarInsnNode) node);
                break;

            case AbstractInsnNode.TYPE_INSN:
                emitTypeInsn((TypeInsnNode) node);
                break;

            case AbstractInsnNode.FIELD_INSN:
                emitFieldInsn((FieldInsnNode) node);
                break;

            case AbstractInsnNode.METHOD_INSN:
                emitMethodInsn((MethodInsnNode) node);
                break;

            case AbstractInsnNode.INVOKE_DYNAMIC_INSN:
                emitInvokeDynamicInsn((InvokeDynamicInsnNode) node);
                break;

            case AbstractInsnNode.JUMP_INSN:
                emitJumpInsn((JumpInsnNode) node);
                break;

            case AbstractInsnNode.LDC_INSN:
                emitLdcInsn((LdcInsnNode) node);
                break;

            case AbstractInsnNode.IINC_INSN:
                emitIincInsn((IincInsnNode) node);
                break;

            case AbstractInsnNode.TABLESWITCH_INSN:
                emitTableSwitchInsn((TableSwitchInsnNode) node);
                break;

            case AbstractInsnNode.LOOKUPSWITCH_INSN:
                emitLookupSwitchInsn((LookupSwitchInsnNode) node);
                break;

            case AbstractInsnNode.MULTIANEWARRAY_INSN:
                emitMultiANewArrayInsn((MultiANewArrayInsnNode) node);
                break;

            default:
                // 未知节点类型，跳过
                break;
        }
    }

    // ===== 各类型指令的发射 =====

    private void emitOpcode(int opcode) throws IOException {
        dos.writeByte(opcode);
    }

    /**
     * bipush / sipush / newarray
     */
    private void emitIntInsn(IntInsnNode node) throws IOException {
        dos.writeByte(node.getOpcode());
        if (node.getOpcode() == Opcodes.BIPUSH) {
            dos.writeByte(node.operand); // 1 byte
        } else if (node.getOpcode() == Opcodes.SIPUSH) {
            dos.writeShort(node.operand); // 2 bytes
        } else {
            // NEWARRAY: operand is array type
            dos.writeByte(node.operand);
        }
    }

    /**
     * xLOAD / xSTORE / RET — 操作数是 local 变量索引
     * 统一用 2 字节（支持 wide）
     */
    private void emitVarInsn(VarInsnNode node) throws IOException {
        dos.writeByte(node.getOpcode());
        dos.writeShort(node.var); // 2 bytes, 统一格式
    }

    /**
     * NEW / ANEWARRAY / CHECKCAST / INSTANCEOF
     * 操作数：自定义 CP 中的 CLASS 索引
     */
    private void emitTypeInsn(TypeInsnNode node) throws IOException {
        int cpIdx = getOrCreateClassEntry(node.desc);
        dos.writeByte(node.getOpcode());
        dos.writeShort(cpIdx);
    }

    /**
     * GETFIELD / PUTFIELD / GETSTATIC / PUTSTATIC
     * 操作数：自定义 CP 中的 FIELD_REF 索引
     */
    private void emitFieldInsn(FieldInsnNode node) throws IOException {
        int cpIdx = getOrCreateFieldRefEntry(node.owner, node.name, node.desc);
        dos.writeByte(node.getOpcode());
        dos.writeShort(cpIdx);
    }

    /**
     * INVOKEVIRTUAL / INVOKESPECIAL / INVOKESTATIC / INVOKEINTERFACE
     * 操作数：自定义 CP 中的 METHOD_REF 或 INTERFACE_METHOD_REF 索引
     */
    private void emitMethodInsn(MethodInsnNode node) throws IOException {
        int cpIdx;
        if (node.itf) {
            cpIdx = getOrCreateInterfaceMethodRefEntry(node.owner, node.name, node.desc);
        } else {
            cpIdx = getOrCreateMethodRefEntry(node.owner, node.name, node.desc);
        }
        dos.writeByte(node.getOpcode());
        dos.writeShort(cpIdx);
        if (node.getOpcode() == Opcodes.INVOKEINTERFACE) {
            // 额外写入参数 count + 0 padding（与标准 JVM 格式兼容）
            int argCount = Type.getArgumentTypes(node.desc).length + 1;
            dos.writeByte(argCount);
            dos.writeByte(0);
        }
    }

    /**
     * INVOKEDYNAMIC
     * 操作数：自定义 CP 中的 INVOKE_DYNAMIC 索引
     */
    private void emitInvokeDynamicInsn(InvokeDynamicInsnNode node) throws IOException {
        int bsmIdx = findBootstrapMethodIndex(node.bsm, node.bsmArgs);
        int cpIdx = getOrCreateInvokeDynamicEntry(bsmIdx, node.name, node.desc);
        dos.writeByte(Opcodes.INVOKEDYNAMIC);
        dos.writeShort(cpIdx);
        dos.writeByte(0); // padding
        dos.writeByte(0); // padding
    }

    /**
     * GOTO / IF_xxx / JSR 等跳转
     * 统一用 4 字节有符号偏移（指令起始位置到目标 label 的偏移）
     */
    private void emitJumpInsn(JumpInsnNode node) throws IOException {
        int instrStart = bytecodeOut.size();
        dos.writeByte(node.getOpcode());

        // 先写占位 4 字节，后续回填
        jumpPatches.add(new JumpPatch(bytecodeOut.size(), instrStart, node.label));
        dos.writeInt(0); // placeholder
    }

    /**
     * LDC / LDC_W / LDC2_W
     * 操作数：自定义 CP 索引
     */
    private void emitLdcInsn(LdcInsnNode node) throws IOException {
        int cpIdx;
        Object cst = node.cst;

        if (cst instanceof Integer) {
            cpIdx = getOrCreateIntEntry((Integer) cst);
            dos.writeByte(Opcodes.LDC);
        } else if (cst instanceof Long) {
            cpIdx = getOrCreateLongEntry((Long) cst);
            dos.writeByte(Opcodes.LDC); // 统一用 LDC
        } else if (cst instanceof Float) {
            cpIdx = getOrCreateFloatEntry((Float) cst);
            dos.writeByte(Opcodes.LDC);
        } else if (cst instanceof Double) {
            cpIdx = getOrCreateDoubleEntry((Double) cst);
            dos.writeByte(Opcodes.LDC);
        } else if (cst instanceof String) {
            cpIdx = getOrCreateStringEntry((String) cst);
            dos.writeByte(Opcodes.LDC);
        } else if (cst instanceof Type) {
            Type t = (Type) cst;
            if (t.getSort() == Type.OBJECT || t.getSort() == Type.ARRAY) {
                cpIdx = getOrCreateClassEntry(t.getInternalName());
            } else if (t.getSort() == Type.METHOD) {
                cpIdx = getOrCreateStringEntry(t.getDescriptor());
            } else {
                cpIdx = getOrCreateStringEntry(t.getDescriptor());
            }
            dos.writeByte(Opcodes.LDC);
        } else if (cst instanceof Handle) {
            Handle h = (Handle) cst;
            cpIdx = getOrCreateMethodHandleEntry(h.getTag(), h.getOwner(), h.getName(), h.getDesc());
            dos.writeByte(Opcodes.LDC);
        } else {
            throw new RuntimeException("Unsupported LDC constant type: " + cst.getClass());
        }

        dos.writeShort(cpIdx);
    }

    /**
     * IINC: local变量索引 + 增量
     */
    private void emitIincInsn(IincInsnNode node) throws IOException {
        dos.writeByte(Opcodes.IINC);
        dos.writeShort(node.var);
        dos.writeShort(node.incr);
    }

    /**
     * TABLESWITCH
     * 自定义格式：opcode(1) + default_offset(4) + low(4) + high(4) + offsets(4 * n)
     */
    private void emitTableSwitchInsn(TableSwitchInsnNode node) throws IOException {
        int instrStart = bytecodeOut.size();
        dos.writeByte(Opcodes.TABLESWITCH);

        // default offset - placeholder
        int defaultPos = bytecodeOut.size();
        dos.writeInt(0);

        dos.writeInt(node.min);
        dos.writeInt(node.max);

        // jump offsets - placeholders
        List<int[]> patches = new ArrayList<>();
        for (int i = 0; i < node.labels.size(); i++) {
            int pos = bytecodeOut.size();
            dos.writeInt(0);
            patches.add(new int[]{pos, i});
        }

        switchPatches.add(new SwitchPatch(instrStart, defaultPos, node.dflt, patches, node.labels));
    }

    /**
     * LOOKUPSWITCH
     * 自定义格式：opcode(1) + default_offset(4) + npairs(4) + [key(4) + offset(4)] * npairs
     */
    private void emitLookupSwitchInsn(LookupSwitchInsnNode node) throws IOException {
        int instrStart = bytecodeOut.size();
        dos.writeByte(Opcodes.LOOKUPSWITCH);

        // default offset - placeholder
        int defaultPos = bytecodeOut.size();
        dos.writeInt(0);

        dos.writeInt(node.keys.size());

        List<int[]> patches = new ArrayList<>();
        for (int i = 0; i < node.keys.size(); i++) {
            dos.writeInt(node.keys.get(i));
            int pos = bytecodeOut.size();
            dos.writeInt(0);
            patches.add(new int[]{pos, i});
        }

        switchPatches.add(new SwitchPatch(instrStart, defaultPos, node.dflt, patches, node.labels));
    }

    /**
     * MULTIANEWARRAY
     */
    private void emitMultiANewArrayInsn(MultiANewArrayInsnNode node) throws IOException {
        int cpIdx = getOrCreateClassEntry(node.desc);
        dos.writeByte(Opcodes.MULTIANEWARRAY);
        dos.writeShort(cpIdx);
        dos.writeByte(node.dims);
    }

    // ===== 回填跳转偏移 =====

    private void backpatchJumps() {
        byte[] code = bytecodeOut.toByteArray();

        // 回填普通跳转
        for (JumpPatch patch : jumpPatches) {
            Integer targetOffset = labelOffsets.get(patch.targetLabel);
            if (targetOffset == null) {
                throw new RuntimeException("Unresolved label in jump instruction");
            }
            int relOffset = targetOffset - patch.instrStart;
            writeInt(code, patch.patchPosition, relOffset);
        }

        // 回填 switch
        for (SwitchPatch patch : switchPatches) {
            // default
            Integer defOffset = labelOffsets.get(patch.defaultLabel);
            if (defOffset == null) throw new RuntimeException("Unresolved default label");
            writeInt(code, patch.defaultPatchPos, defOffset - patch.instrStart);

            // cases
            for (int[] p : patch.casePatchPositions) {
                int pos = p[0];
                int idx = p[1];
                LabelNode label = patch.caseLabels.get(idx);
                Integer caseOffset = labelOffsets.get(label);
                if (caseOffset == null) throw new RuntimeException("Unresolved case label");
                writeInt(code, pos, caseOffset - patch.instrStart);
            }
        }

        // 回写
        bytecodeOut.reset();
        try {
            bytecodeOut.write(code);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeInt(byte[] buf, int pos, int value) {
        buf[pos] = (byte) (value >> 24);
        buf[pos + 1] = (byte) (value >> 16);
        buf[pos + 2] = (byte) (value >> 8);
        buf[pos + 3] = (byte) value;
    }

    // ===== 异常表提取 =====

    private void extractExceptionTable() {
        if (methodNode.tryCatchBlocks == null) return;

        for (TryCatchBlockNode tcb : methodNode.tryCatchBlocks) {
            Integer startPc = labelOffsets.get(tcb.start);
            Integer endPc = labelOffsets.get(tcb.end);
            Integer handlerPc = labelOffsets.get(tcb.handler);

            if (startPc == null || endPc == null || handlerPc == null) {
                System.err.println("[WARN] Unresolved exception table label, skipping");
                continue;
            }

            ExceptionEntry entry = new ExceptionEntry(startPc, endPc, handlerPc, tcb.type);

            // 如果有 catch type，加入常量池
            if (tcb.type != null) {
                int cpIdx = getOrCreateClassEntry(tcb.type);
                entry.setCatchTypeCpIndex(cpIdx);
            }

            exceptionTable.add(entry);
        }
    }

    // ===== Bootstrap Methods 提取 =====

    private void extractBootstrapMethods() {
        // Bootstrap methods 是类级别的，从 ClassNode 获取
        // ASM 的 InvokeDynamicInsnNode 已经内联了 bsm 信息
        // 这里不需要预提取，在 emitInvokeDynamicInsn 时按需创建
    }

    private int findBootstrapMethodIndex(Handle bsm, Object[] bsmArgs) {
        // 检查是否已存在
        for (int i = 0; i < bootstrapMethods.size(); i++) {
            BootstrapEntry e = bootstrapMethods.get(i);
            if (e.getHandleTag() == bsm.getTag() &&
                    e.getHandleOwner().equals(bsm.getOwner()) &&
                    e.getHandleName().equals(bsm.getName()) &&
                    e.getHandleDescriptor().equals(bsm.getDesc())) {
                return i;
            }
        }

        // 创建新的
        BootstrapEntry entry = new BootstrapEntry();
        entry.setHandleTag(bsm.getTag());
        entry.setHandleOwner(bsm.getOwner());
        entry.setHandleName(bsm.getName());
        entry.setHandleDescriptor(bsm.getDesc());

        // 收集 bootstrap 参数
        List<Object> args = new ArrayList<>();
        if (bsmArgs != null) {
            for (Object arg : bsmArgs) {
                if (arg instanceof Type) {
                    args.add(((Type) arg).getDescriptor());
                } else if (arg instanceof Handle) {
                    Handle h = (Handle) arg;
                    // 存为特殊结构
                    Map<String, Object> handleMap = new HashMap<>();
                    handleMap.put("tag", h.getTag());
                    handleMap.put("owner", h.getOwner());
                    handleMap.put("name", h.getName());
                    handleMap.put("desc", h.getDesc());
                    args.add(handleMap);
                } else {
                    // Integer, Long, Float, Double, String
                    args.add(arg);
                }
            }
        }
        entry.setArguments(args);

        int idx = bootstrapMethods.size();
        bootstrapMethods.add(entry);
        return idx;
    }

    // ===== 常量池管理（去重） =====

    private int getOrCreateIntEntry(int value) {
        String key = "INT:" + value;
        return cpDedup.computeIfAbsent(key, k -> {
            int idx = nextCpIndex++;
            constantPool.add(CPEntry.ofInt(idx, value));
            return idx;
        });
    }

    private int getOrCreateLongEntry(long value) {
        String key = "LONG:" + value;
        return cpDedup.computeIfAbsent(key, k -> {
            int idx = nextCpIndex++;
            constantPool.add(CPEntry.ofLong(idx, value));
            return idx;
        });
    }

    private int getOrCreateFloatEntry(float value) {
        String key = "FLOAT:" + Float.floatToIntBits(value);
        return cpDedup.computeIfAbsent(key, k -> {
            int idx = nextCpIndex++;
            constantPool.add(CPEntry.ofFloat(idx, value));
            return idx;
        });
    }

    private int getOrCreateDoubleEntry(double value) {
        String key = "DOUBLE:" + Double.doubleToLongBits(value);
        return cpDedup.computeIfAbsent(key, k -> {
            int idx = nextCpIndex++;
            constantPool.add(CPEntry.ofDouble(idx, value));
            return idx;
        });
    }

    private int getOrCreateStringEntry(String value) {
        String key = "STRING:" + value;
        return cpDedup.computeIfAbsent(key, k -> {
            int idx = nextCpIndex++;
            constantPool.add(CPEntry.ofString(idx, value));
            return idx;
        });
    }

    private int getOrCreateClassEntry(String className) {
        String key = "CLASS:" + className;
        return cpDedup.computeIfAbsent(key, k -> {
            int idx = nextCpIndex++;
            constantPool.add(CPEntry.ofClass(idx, className));
            return idx;
        });
    }

    private int getOrCreateMethodRefEntry(String owner, String name, String desc) {
        String key = "METHOD:" + owner + "." + name + desc;
        return cpDedup.computeIfAbsent(key, k -> {
            int idx = nextCpIndex++;
            constantPool.add(CPEntry.ofMethodRef(idx, owner, name, desc));
            return idx;
        });
    }

    private int getOrCreateInterfaceMethodRefEntry(String owner, String name, String desc) {
        String key = "IMETHOD:" + owner + "." + name + desc;
        return cpDedup.computeIfAbsent(key, k -> {
            int idx = nextCpIndex++;
            constantPool.add(CPEntry.ofInterfaceMethodRef(idx, owner, name, desc));
            return idx;
        });
    }

    private int getOrCreateFieldRefEntry(String owner, String name, String desc) {
        String key = "FIELD:" + owner + "." + name + ":" + desc;
        return cpDedup.computeIfAbsent(key, k -> {
            int idx = nextCpIndex++;
            constantPool.add(CPEntry.ofFieldRef(idx, owner, name, desc));
            return idx;
        });
    }

    private int getOrCreateInvokeDynamicEntry(int bsmIdx, String name, String desc) {
        String key = "INDY:" + bsmIdx + ":" + name + desc;
        return cpDedup.computeIfAbsent(key, k -> {
            int idx = nextCpIndex++;
            constantPool.add(CPEntry.ofInvokeDynamic(idx, bsmIdx, name, desc));
            return idx;
        });
    }

    private int getOrCreateMethodHandleEntry(int tag, String owner, String name, String desc) {
        String key = "HANDLE:" + tag + ":" + owner + "." + name + desc;
        return cpDedup.computeIfAbsent(key, k -> {
            int idx = nextCpIndex++;
            constantPool.add(CPEntry.ofMethodHandle(idx, tag, owner, name, desc));
            return idx;
        });
    }

    // ===== 结果获取 =====

    public byte[] getBytecodeBytes() {
        return bytecodeOut.toByteArray();
    }

    public List<CPEntry> getConstantPool() {
        return constantPool;
    }

    public List<ExceptionEntry> getExceptionTable() {
        return exceptionTable;
    }

    public List<BootstrapEntry> getBootstrapMethods() {
        return bootstrapMethods;
    }

    // ===== 回填辅助结构 =====

    private static class JumpPatch {
        final int patchPosition;  // bytecode 中要回填的位置
        final int instrStart;     // 跳转指令起始位置
        final LabelNode targetLabel;

        JumpPatch(int patchPosition, int instrStart, LabelNode targetLabel) {
            this.patchPosition = patchPosition;
            this.instrStart = instrStart;
            this.targetLabel = targetLabel;
        }
    }

    private static class SwitchPatch {
        final int instrStart;
        final int defaultPatchPos;
        final LabelNode defaultLabel;
        final List<int[]> casePatchPositions; // [bytePos, caseIndex]
        final List<LabelNode> caseLabels;

        SwitchPatch(int instrStart, int defaultPatchPos, LabelNode defaultLabel,
                    List<int[]> casePatchPositions, List<LabelNode> caseLabels) {
            this.instrStart = instrStart;
            this.defaultPatchPos = defaultPatchPos;
            this.defaultLabel = defaultLabel;
            this.casePatchPositions = casePatchPositions;
            this.caseLabels = caseLabels;
        }
    }
}