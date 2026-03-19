package com.alphaautoleak.jnvm.patcher;

import com.alphaautoleak.jnvm.utils.InstructionsUtil;
import com.alphaautoleak.jnvm.utils.BridgeFastPathUtil;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

/**
 * Rewrites protected method bodies to native bridge calls.
 * Supports legacy bridge mode and direct-native-rewrite mode.
 */
public class MethodBodyRewriter {

    static final String REGISTER_NATIVE_METHOD_NAME = "__jnvm$registerClassNatives";
    static final String REGISTER_NATIVE_METHOD_DESC = "(Ljava/lang/Class;)V";

    private final String bridgeClass;
    private final int methodIdXorKey;
    private final boolean directNativeRewrite;
    private int bootstrapImplSeq = 0;

    MethodBodyRewriter(String bridgeClass, int methodIdXorKey, boolean directNativeRewrite) {
        this.bridgeClass = bridgeClass;
        this.methodIdXorKey = methodIdXorKey;
        this.directNativeRewrite = directNativeRewrite;
    }

    void rewrite(ClassNode cn, MethodNode mn, int methodId, boolean classHasDirectNativeMethods) {
        if (directNativeRewrite) {
            if ("<clinit>".equals(mn.name)) {
                rewriteClinitForDirectNative(cn, mn, methodId, classHasDirectNativeMethods);
            } else {
                rewriteAsDirectNative(mn);
            }
            return;
        }
        rewriteLegacyObjectArray(cn, mn, methodId);
    }

    MethodNode rewriteBootstrapEntryAsTrampoline(ClassNode cn, MethodNode bootstrapEntry, int methodId) {
        String implName = "__jnvm$bootstrap$impl$" + (bootstrapImplSeq++);
        MethodNode impl = cloneAsBootstrapImpl(bootstrapEntry, implName);
        // Bootstrap entry compatibility: always protect impl with legacy bridge rewrite.
        rewriteLegacyObjectArray(cn, impl, methodId);
        rewriteBootstrapTrampolineBody(cn.name, bootstrapEntry, implName);
        return impl;
    }

    private MethodNode cloneAsBootstrapImpl(MethodNode source, String implName) {
        int implAccess = (source.access & ~(Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED))
                | Opcodes.ACC_PRIVATE
                | Opcodes.ACC_SYNTHETIC;
        MethodNode impl = new MethodNode(
                Opcodes.ASM9,
                implAccess,
                implName,
                source.desc,
                source.signature,
                source.exceptions == null ? null : source.exceptions.toArray(new String[0])
        );
        source.accept(impl);
        return impl;
    }

    private void rewriteBootstrapTrampolineBody(String owner, MethodNode entry, String implName) {
        boolean isStatic = (entry.access & Opcodes.ACC_STATIC) != 0;
        Type[] argTypes = Type.getArgumentTypes(entry.desc);
        Type retType = Type.getReturnType(entry.desc);

        InsnList insns = new InsnList();
        int localIdx = 0;
        int invokeOpcode;
        if (isStatic) {
            invokeOpcode = Opcodes.INVOKESTATIC;
        } else {
            invokeOpcode = Opcodes.INVOKESPECIAL;
            insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
            localIdx = 1;
        }

        for (Type argType : argTypes) {
            InstructionsUtil.loadRaw(insns, argType, localIdx);
            localIdx += argType.getSize();
        }

        insns.add(new MethodInsnNode(
                invokeOpcode,
                owner,
                implName,
                entry.desc,
                false
        ));
        appendReturn(insns, retType);

        entry.instructions.clear();
        entry.instructions.add(insns);
        entry.tryCatchBlocks.clear();
        if (entry.localVariables != null) entry.localVariables.clear();
        entry.maxStack = 0;
        entry.maxLocals = 0;
    }

    private void rewriteLegacyObjectArray(ClassNode cn, MethodNode mn, int methodId) {
        boolean isStatic = (mn.access & Opcodes.ACC_STATIC) != 0;
        Type[] argTypes = Type.getArgumentTypes(mn.desc);
        Type retType = Type.getReturnType(mn.desc);

        InsnList insns = new InsnList();
        int obfMethodId = BridgeFastPathUtil.obfuscateMethodId(methodId, methodIdXorKey);

        // 1) methodId
        InstructionsUtil.pushInt(insns, obfMethodId);

        // 2) Object[] args: [0]=instance/null, [1..n]=params boxed, [n+1]=callerClass
        int argsArrayLen = argTypes.length + 2;
        InstructionsUtil.pushInt(insns, argsArrayLen);
        insns.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/Object"));

        // args[0] = receiver (or null for static)
        insns.add(new InsnNode(Opcodes.DUP));
        InstructionsUtil.pushInt(insns, 0);
        if (isStatic) {
            insns.add(new InsnNode(Opcodes.ACONST_NULL));
        } else {
            insns.add(new VarInsnNode(Opcodes.ALOAD, 0));
        }
        insns.add(new InsnNode(Opcodes.AASTORE));

        // args[1..n] = boxed parameters
        int localIdx = isStatic ? 0 : 1;
        for (int i = 0; i < argTypes.length; i++) {
            insns.add(new InsnNode(Opcodes.DUP));
            InstructionsUtil.pushInt(insns, i + 1);
            InstructionsUtil.loadAndBox(insns, argTypes[i], localIdx);
            insns.add(new InsnNode(Opcodes.AASTORE));
            localIdx += argTypes[i].getSize();
        }

        // args[n+1] = callerClass
        insns.add(new InsnNode(Opcodes.DUP));
        InstructionsUtil.pushInt(insns, argTypes.length + 1);
        insns.add(new LdcInsnNode(org.objectweb.asm.Type.getType("L" + cn.name + ";")));
        insns.add(new InsnNode(Opcodes.AASTORE));

        // 3) call legacy bridge entry
        insns.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                bridgeClass,
                getLegacyExecuteMethodName(retType),
                getLegacyExecuteDesc(retType),
                false
        ));

        // 4) return
        generateDirectReturn(insns, retType);

        mn.instructions.clear();
        mn.instructions.add(insns);
        mn.tryCatchBlocks.clear();
        if (mn.localVariables != null) mn.localVariables.clear();
        mn.maxStack = 0;
        mn.maxLocals = 0;
    }

    void prependRegisterCallToClinit(ClassNode cn, MethodNode clinit) {
        InsnList prelude = buildRegisterClassNativesPrelude(cn.name);
        clinit.instructions.insert(prelude);
    }

    MethodNode createSyntheticRegisterOnlyClinit(String ownerClassInternalName) {
        MethodNode mn = new MethodNode(
                Opcodes.ACC_STATIC,
                "<clinit>",
                "()V",
                null,
                null
        );
        mn.instructions.add(buildRegisterClassNativesPrelude(ownerClassInternalName));
        mn.instructions.add(new InsnNode(Opcodes.RETURN));
        mn.maxStack = 0;
        mn.maxLocals = 0;
        return mn;
    }

    private InsnList buildRegisterClassNativesPrelude(String ownerClassInternalName) {
        InsnList insns = new InsnList();
        insns.add(new LdcInsnNode(Type.getType("L" + ownerClassInternalName + ";")));
        insns.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                bridgeClass,
                REGISTER_NATIVE_METHOD_NAME,
                REGISTER_NATIVE_METHOD_DESC,
                false
        ));
        return insns;
    }

    private void rewriteAsDirectNative(MethodNode mn) {
        mn.access |= Opcodes.ACC_NATIVE;
        mn.access &= ~Opcodes.ACC_ABSTRACT;
        if (mn.instructions != null) {
            mn.instructions.clear();
        }
        mn.tryCatchBlocks.clear();
        if (mn.localVariables != null) mn.localVariables.clear();
        mn.maxStack = 0;
        mn.maxLocals = 0;
    }

    private void rewriteClinitForDirectNative(ClassNode cn, MethodNode mn, int methodId, boolean classHasDirectNativeMethods) {
        InsnList insns = new InsnList();
        int obfMethodId = BridgeFastPathUtil.obfuscateMethodId(methodId, methodIdXorKey);

        // 1) Ensure this class direct-native methods are registered before VM execute
        if (classHasDirectNativeMethods) {
            insns.add(buildRegisterClassNativesPrelude(cn.name));
        }

        // 2) executeVoid(methodId, new Object[]{ null, callerClass })
        InstructionsUtil.pushInt(insns, obfMethodId);
        InstructionsUtil.pushInt(insns, 2);
        insns.add(new TypeInsnNode(Opcodes.ANEWARRAY, "java/lang/Object"));

        insns.add(new InsnNode(Opcodes.DUP));
        InstructionsUtil.pushInt(insns, 0);
        insns.add(new InsnNode(Opcodes.ACONST_NULL));
        insns.add(new InsnNode(Opcodes.AASTORE));

        insns.add(new InsnNode(Opcodes.DUP));
        InstructionsUtil.pushInt(insns, 1);
        insns.add(new LdcInsnNode(Type.getType("L" + cn.name + ";")));
        insns.add(new InsnNode(Opcodes.AASTORE));

        insns.add(new MethodInsnNode(
                Opcodes.INVOKESTATIC,
                bridgeClass,
                "executeVoid",
                "(I[Ljava/lang/Object;)V",
                false
        ));
        insns.add(new InsnNode(Opcodes.RETURN));

        mn.instructions.clear();
        mn.instructions.add(insns);
        mn.tryCatchBlocks.clear();
        if (mn.localVariables != null) mn.localVariables.clear();
        mn.maxStack = 0;
        mn.maxLocals = 0;
    }

    private String getLegacyExecuteMethodName(Type retType) {
        switch (retType.getSort()) {
            case Type.VOID:
                return "executeVoid";
            case Type.LONG:
                return "executeLong";
            case Type.FLOAT:
                return "executeFloat";
            case Type.DOUBLE:
                return "executeDouble";
            case Type.OBJECT:
            case Type.ARRAY:
                return "executeObject";
            default:
                return "executeInt";
        }
    }

    private String getLegacyExecuteDesc(Type retType) {
        switch (retType.getSort()) {
            case Type.VOID:
                return "(I[Ljava/lang/Object;)V";
            case Type.LONG:
                return "(I[Ljava/lang/Object;)J";
            case Type.FLOAT:
                return "(I[Ljava/lang/Object;)F";
            case Type.DOUBLE:
                return "(I[Ljava/lang/Object;)D";
            case Type.OBJECT:
            case Type.ARRAY:
                return "(I[Ljava/lang/Object;)Ljava/lang/Object;";
            default:
                return "(I[Ljava/lang/Object;)I";
        }
    }

    /**
     * Generates direct return instruction (no unboxing needed)
     */
    private void generateDirectReturn(InsnList insns, Type retType) {
        switch (retType.getSort()) {
            case Type.VOID:
                insns.add(new InsnNode(Opcodes.RETURN));
                break;
            case Type.BOOLEAN:
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT:
                insns.add(new InsnNode(Opcodes.IRETURN));
                break;
            case Type.LONG:
                insns.add(new InsnNode(Opcodes.LRETURN));
                break;
            case Type.FLOAT:
                insns.add(new InsnNode(Opcodes.FRETURN));
                break;
            case Type.DOUBLE:
                insns.add(new InsnNode(Opcodes.DRETURN));
                break;
            case Type.OBJECT:
            case Type.ARRAY:
                insns.add(new TypeInsnNode(Opcodes.CHECKCAST, retType.getInternalName()));
                insns.add(new InsnNode(Opcodes.ARETURN));
                break;
            default:
                insns.add(new InsnNode(Opcodes.ARETURN));
                break;
        }
    }

    private void appendReturn(InsnList insns, Type retType) {
        switch (retType.getSort()) {
            case Type.VOID:
                insns.add(new InsnNode(Opcodes.RETURN));
                return;
            case Type.BOOLEAN:
            case Type.BYTE:
            case Type.CHAR:
            case Type.SHORT:
            case Type.INT:
                insns.add(new InsnNode(Opcodes.IRETURN));
                return;
            case Type.LONG:
                insns.add(new InsnNode(Opcodes.LRETURN));
                return;
            case Type.FLOAT:
                insns.add(new InsnNode(Opcodes.FRETURN));
                return;
            case Type.DOUBLE:
                insns.add(new InsnNode(Opcodes.DRETURN));
                return;
            default:
                insns.add(new InsnNode(Opcodes.ARETURN));
        }
    }
}
