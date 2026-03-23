package com.alphaautoleak.jnvm.utils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Utility for building stable method keys in format: owner.name.descriptor
 */
public final class MethodKeyUtil {

    private MethodKeyUtil() {
    }

    public static String of(String owner, String name, String descriptor) {
        return owner + "." + name + "." + descriptor;
    }

    public static String normalize(String methodKey) {
        if (methodKey == null || methodKey.isEmpty()) {
            return methodKey;
        }

        int descriptorStart = methodKey.indexOf('(');
        if (descriptorStart < 0) {
            return methodKey;
        }

        String descriptor = methodKey.substring(descriptorStart);
        String prefix = methodKey.substring(0, descriptorStart);

        int hashSeparator = prefix.lastIndexOf('#');
        if (hashSeparator >= 0) {
            return of(prefix.substring(0, hashSeparator), prefix.substring(hashSeparator + 1), descriptor);
        }

        if (prefix.endsWith(".")) {
            prefix = prefix.substring(0, prefix.length() - 1);
        }

        int dotSeparator = prefix.lastIndexOf('.');
        if (dotSeparator < 0) {
            return methodKey;
        }

        return of(prefix.substring(0, dotSeparator), prefix.substring(dotSeparator + 1), descriptor);
    }

    public static Set<String> normalizeAll(Set<String> methodKeys) {
        if (methodKeys == null || methodKeys.isEmpty()) {
            return Collections.emptySet();
        }

        Set<String> normalized = new HashSet<>();
        for (String methodKey : methodKeys) {
            normalized.add(normalize(methodKey));
        }
        return normalized;
    }
}
