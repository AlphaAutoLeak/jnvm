package com.alphaautoleak.jnvm.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Shared helper utilities for bridge/native method id and descriptor handling.
 */
public final class BridgeFastPathUtil {

    private BridgeFastPathUtil() {
    }

    public static int obfuscateMethodId(int methodId, int methodIdXorKey) {
        return methodId ^ methodIdXorKey;
    }

    public static String directNativeFunctionName(int methodId, int methodIdXorKey) {
        int obfId = obfuscateMethodId(methodId, methodIdXorKey);
        return "native_direct_" + Integer.toUnsignedString(obfId, 16);
    }

    public static String returnDescriptor(String methodDesc) {
        int end = methodDesc.indexOf(')');
        return methodDesc.substring(end + 1);
    }

    /**
     * Parse parameter descriptors from a method descriptor.
     * Example: "(IJLjava/lang/String;[I)V" -> ["I","J","Ljava/lang/String;","[I"]
     */
    public static List<String> parameterDescriptors(String methodDesc) {
        List<String> result = new ArrayList<>();
        int i = 1; // skip '('
        int n = methodDesc.length();
        while (i < n && methodDesc.charAt(i) != ')') {
            int start = i;
            char c = methodDesc.charAt(i);
            if (c == 'L') {
                int semi = methodDesc.indexOf(';', i);
                if (semi < 0) {
                    break;
                }
                result.add(methodDesc.substring(start, semi + 1));
                i = semi + 1;
                continue;
            }
            if (c == '[') {
                i++;
                while (i < n && methodDesc.charAt(i) == '[') {
                    i++;
                }
                if (i < n && methodDesc.charAt(i) == 'L') {
                    int semi = methodDesc.indexOf(';', i);
                    if (semi < 0) {
                        break;
                    }
                    i = semi + 1;
                } else {
                    i++;
                }
                result.add(methodDesc.substring(start, i));
                continue;
            }
            result.add(methodDesc.substring(start, i + 1));
            i++;
        }
        return result;
    }

    public static boolean isWideDescriptor(String descriptor) {
        return "J".equals(descriptor) || "D".equals(descriptor);
    }

    public static boolean isObjectLikeDescriptor(String descriptor) {
        return descriptor.startsWith("L") || descriptor.startsWith("[");
    }
}
