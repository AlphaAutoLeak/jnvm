package com.alphaautoleak.jnvm.converter;

import com.alphaautoleak.jnvm.asm.JarScanner;
import com.alphaautoleak.jnvm.asm.MethodInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class ConversionScanResult {

    private final List<MethodInfo> protectedMethods;
    private final Set<String> affectedClasses;
    private final Set<String> bootstrapMethodKeys;

    private ConversionScanResult(List<MethodInfo> protectedMethods,
                                 Set<String> affectedClasses,
                                 Set<String> bootstrapMethodKeys) {
        this.protectedMethods = Collections.unmodifiableList(new ArrayList<>(protectedMethods));
        this.affectedClasses = Collections.unmodifiableSet(new HashSet<>(affectedClasses));
        this.bootstrapMethodKeys = Collections.unmodifiableSet(new HashSet<>(bootstrapMethodKeys));
    }

    static ConversionScanResult from(JarScanner scanner, List<MethodInfo> protectedMethods) {
        return new ConversionScanResult(
                protectedMethods,
                scanner.getAffectedClasses(),
                scanner.getBootstrapMethodKeys()
        );
    }

    boolean isEmpty() {
        return protectedMethods.isEmpty();
    }

    int getAffectedClassCount() {
        return affectedClasses.size();
    }

    List<MethodInfo> getProtectedMethods() {
        return protectedMethods;
    }

    Set<String> getAffectedClasses() {
        return affectedClasses;
    }

    Set<String> getBootstrapMethodKeys() {
        return bootstrapMethodKeys;
    }
}
