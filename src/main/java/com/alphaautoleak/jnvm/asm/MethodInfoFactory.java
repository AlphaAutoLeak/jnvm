package com.alphaautoleak.jnvm.asm;

import com.alphaautoleak.jnvm.crypto.OpcodeObfuscator;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

final class MethodInfoFactory {

    private final OpcodeObfuscator opcodeObfuscator;

    MethodInfoFactory(OpcodeObfuscator opcodeObfuscator) {
        this.opcodeObfuscator = opcodeObfuscator;
    }

    MethodInfo create(int methodId, ClassNode classNode, MethodNode methodNode) {
        MethodInfo info = new MethodInfo();
        info.setMethodId(methodId);
        info.setOwner(classNode.name);
        info.setName(methodNode.name);
        info.setDescriptor(methodNode.desc);
        info.setAccess(methodNode.access);
        info.setMaxStack(methodNode.maxStack);
        info.setMaxLocals(methodNode.maxLocals);
        info.setSignature(methodNode.signature);

        BytecodeExtractor extractor = new BytecodeExtractor(classNode, methodNode, opcodeObfuscator);
        extractor.extract();

        info.setBytecode(extractor.getBytecode());
        info.setMetadata(extractor.getMetadata());
        info.setPcToMetaIdx(extractor.getPcToMetaIdx());
        info.setStringPool(extractor.getStringPool());
        info.setExceptionTable(extractor.getExceptionTable());
        info.setBootstrapMethods(extractor.getBootstrapMethods());
        return info;
    }
}
