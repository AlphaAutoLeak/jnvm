package com.alphaautoleak.jnvm.codegen.emitter;

import com.alphaautoleak.jnvm.asm.MethodInfo;
import com.alphaautoleak.jnvm.utils.BridgeFastPathUtil;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Generates vm_bridge.c - JNI bridge layer
 * Supports legacy generic bridge and direct-native-rewrite mode.
 */
public class VmBridgeGenerator {

    static final class DirectNativeEntry {
        final int methodId;
        final String owner;
        final String methodName;
        final String methodDesc;
        final boolean isStatic;
        final String nativeFunctionName;
        final List<String> paramDescriptors;
        final String returnDescriptor;

        DirectNativeEntry(int methodId,
                          String owner,
                          String methodName,
                          String methodDesc,
                          boolean isStatic,
                          String nativeFunctionName,
                          List<String> paramDescriptors,
                          String returnDescriptor) {
            this.methodId = methodId;
            this.owner = owner;
            this.methodName = methodName;
            this.methodDesc = methodDesc;
            this.isStatic = isStatic;
            this.nativeFunctionName = nativeFunctionName;
            this.paramDescriptors = paramDescriptors;
            this.returnDescriptor = returnDescriptor;
        }
    }

    static final class DirectClassRegistration {
        final String owner;
        final List<DirectNativeEntry> entries = new ArrayList<>();

        DirectClassRegistration(String owner) {
            this.owner = owner;
        }
    }

    private final File dir;
    private final String bridgeClass;
    private final boolean encryptStrings;
    private final int methodIdXorKey;
    private final boolean directNativeRewrite;
    private final List<DirectNativeEntry> directEntries;
    private final List<DirectClassRegistration> directClassRegistrations;

    public VmBridgeGenerator(File dir,
                             String bridgeClass,
                             boolean encryptStrings,
                             List<MethodInfo> protectedMethods,
                             int methodIdXorKey,
                             boolean directNativeRewrite) {
        this.dir = dir;
        this.bridgeClass = bridgeClass;
        this.encryptStrings = encryptStrings;
        this.methodIdXorKey = methodIdXorKey;
        this.directNativeRewrite = directNativeRewrite;
        this.directEntries = directNativeRewrite ? buildDirectEntries(protectedMethods) : new ArrayList<DirectNativeEntry>();
        this.directClassRegistrations = directNativeRewrite ? buildDirectClassRegistrations(directEntries) : new ArrayList<DirectClassRegistration>();
    }

    public void generate() throws IOException {
        try (PrintWriter w = new PrintWriter(new java.io.FileWriter(new File(dir, "vm_bridge.c")))) {
            w.println("#include \"vm_types.h\"");
            w.println("#include \"vm_data.h\"");
            w.println("#include \"vm_interpreter.h\"");
            w.println();

            // Legacy generic bridge
            VmBridgeLegacyEmitter.emitExecuteFunctions(w);
            w.println();

            // Direct native method stubs
            VmBridgeDirectEmitter.emitDirectNativeFunctions(w, directNativeRewrite, directEntries, methodIdXorKey);
            w.println();

            // Class-level RegisterNatives helpers for direct mode
            VmBridgeDirectEmitter.emitDirectRegistrationHelpers(w, directNativeRewrite, directClassRegistrations);
            w.println();

            // Generate RegisterNatives
            VmBridgeRegistrationEmitter.emitRegisterNatives(w, bridgeClass);
            w.println();

            // Generate JNI_OnLoad
            VmBridgeOnLoadEmitter.emitJNIOnLoad(w, encryptStrings);
        }
    }

    private List<DirectNativeEntry> buildDirectEntries(List<MethodInfo> protectedMethods) {
        List<DirectNativeEntry> entries = new ArrayList<>();
        for (MethodInfo method : protectedMethods) {
            if (method.isConstructor() || method.isClassInit()) {
                continue;
            }
            entries.add(new DirectNativeEntry(
                    method.getMethodId(),
                    method.getOwner(),
                    method.getName(),
                    method.getDescriptor(),
                    method.isStatic(),
                    BridgeFastPathUtil.directNativeFunctionName(method.getMethodId(), methodIdXorKey),
                    BridgeFastPathUtil.parameterDescriptors(method.getDescriptor()),
                    BridgeFastPathUtil.returnDescriptor(method.getDescriptor())
            ));
        }
        return entries;
    }

    private List<DirectClassRegistration> buildDirectClassRegistrations(List<DirectNativeEntry> entries) {
        Map<String, DirectClassRegistration> byOwner = new LinkedHashMap<>();
        for (DirectNativeEntry entry : entries) {
            DirectClassRegistration reg = byOwner.get(entry.owner);
            if (reg == null) {
                reg = new DirectClassRegistration(entry.owner);
                byOwner.put(entry.owner, reg);
            }
            reg.entries.add(entry);
        }
        return new ArrayList<>(byOwner.values());
    }

}
