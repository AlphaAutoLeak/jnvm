package com.alphaautoleak.jnvm.converter;

import com.alphaautoleak.jnvm.utils.BridgePackageNameGenerator;

import java.util.Random;

public final class NativeBindingContext {

    private final String bridgeClass;
    private final int methodIdXorKey;
    private final boolean directNativeRewrite;

    private NativeBindingContext(String bridgeClass, int methodIdXorKey, boolean directNativeRewrite) {
        this.bridgeClass = bridgeClass;
        this.methodIdXorKey = methodIdXorKey;
        this.directNativeRewrite = directNativeRewrite;
    }

    public static NativeBindingContext create(boolean directNativeRewrite) {
        return new NativeBindingContext(
                BridgePackageNameGenerator.generate(),
                createMethodIdXorKey(),
                directNativeRewrite
        );
    }

    public String getBridgeClass() {
        return bridgeClass;
    }

    public int getMethodIdXorKey() {
        return methodIdXorKey;
    }

    public boolean isDirectNativeRewrite() {
        return directNativeRewrite;
    }

    private static int createMethodIdXorKey() {
        Random rand = new Random();
        int key;
        do {
            key = rand.nextInt();
        } while (key == 0);
        return key;
    }
}
