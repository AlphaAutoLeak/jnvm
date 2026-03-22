package com.alphaautoleak.jnvm.converter;

import com.alphaautoleak.jnvm.asm.MethodInfo;

import java.util.List;

final class ProtectionSummary {

    private final int methodCount;
    private final int affectedClassCount;
    private final int totalBytecode;
    private final int totalMetadata;

    private ProtectionSummary(int methodCount, int affectedClassCount, int totalBytecode, int totalMetadata) {
        this.methodCount = methodCount;
        this.affectedClassCount = affectedClassCount;
        this.totalBytecode = totalBytecode;
        this.totalMetadata = totalMetadata;
    }

    static ProtectionSummary from(List<MethodInfo> protectedMethods, int affectedClassCount) {
        int totalBytecode = 0;
        int totalMetadata = 0;
        for (MethodInfo method : protectedMethods) {
            totalBytecode += method.getBytecode().length;
            totalMetadata += method.getMetadata().size();
        }
        return new ProtectionSummary(protectedMethods.size(), affectedClassCount, totalBytecode, totalMetadata);
    }

    int getMethodCount() {
        return methodCount;
    }

    int getAffectedClassCount() {
        return affectedClassCount;
    }

    int getTotalBytecode() {
        return totalBytecode;
    }

    int getTotalMetadata() {
        return totalMetadata;
    }
}
