package com.alphaautoleak.jnvm.patcher;

import com.alphaautoleak.jnvm.asm.MethodInfo;
import com.alphaautoleak.jnvm.utils.MethodKeyUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

final class MethodPatchRegistry {

    private final Map<String, Integer> methodIdMap = new HashMap<>();
    private final Set<String> classesWithDirectNativeMethods = new HashSet<>();
    private final Set<String> classesWithProtectedClinit = new HashSet<>();
    private final int methodIdXorKey;

    MethodPatchRegistry(List<MethodInfo> protectedMethods, boolean directNativeRewrite) {
        this.methodIdXorKey = createMethodIdXorKey();

        for (MethodInfo method : protectedMethods) {
            String key = MethodKeyUtil.of(method.getOwner(), method.getName(), method.getDescriptor());
            methodIdMap.put(key, method.getMethodId());

            if (!directNativeRewrite) {
                continue;
            }
            if (method.isClassInit()) {
                classesWithProtectedClinit.add(method.getOwner());
            } else if (!method.isConstructor()) {
                classesWithDirectNativeMethods.add(method.getOwner());
            }
        }
    }

    int getMethodIdXorKey() {
        return methodIdXorKey;
    }

    Integer findMethodId(String owner, String methodName, String descriptor) {
        return methodIdMap.get(MethodKeyUtil.of(owner, methodName, descriptor));
    }

    boolean hasDirectNativeMethods(String className) {
        return classesWithDirectNativeMethods.contains(className);
    }

    boolean hasProtectedClinit(String className) {
        return classesWithProtectedClinit.contains(className);
    }

    Set<String> emptyBootstrapKeysIfNull(Set<String> bootstrapMethodKeys) {
        return bootstrapMethodKeys != null ? bootstrapMethodKeys : Collections.emptySet();
    }

    private int createMethodIdXorKey() {
        Random rand = new Random();
        int key;
        do {
            key = rand.nextInt();
        } while (key == 0);
        return key;
    }
}
