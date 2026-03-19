package com.alphaautoleak.jnvm.utils;

/**
 * Utility for building stable method keys in format: owner.name.descriptor
 */
public final class MethodKeyUtil {

    private MethodKeyUtil() {
    }

    public static String of(String owner, String name, String descriptor) {
        return owner + "." + name + "." + descriptor;
    }
}
