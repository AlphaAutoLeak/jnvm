package com.alphaautoleak.jnvm.codegen.emitter;

import com.alphaautoleak.jnvm.asm.ArgType;
import com.alphaautoleak.jnvm.asm.BootstrapEntry;
import com.alphaautoleak.jnvm.asm.ExceptionEntry;
import com.alphaautoleak.jnvm.asm.MetaEntry;
import com.alphaautoleak.jnvm.asm.MetaType;
import com.alphaautoleak.jnvm.crypto.EncryptedMethodData;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

final class VmDataStringCollector {

    private VmDataStringCollector() {
    }

    static CollectedStrings collect(List<EncryptedMethodData> methods, List<BootstrapEntry> globalBootstrapMethods) {
        Set<String> allStrings = new LinkedHashSet<>();
        Map<String, MethodDescriptorParser.DescriptorInfo> invokeMetaCache = new HashMap<>();

        collectMethodLevelStrings(methods, allStrings);
        collectMethodIdentityStrings(methods, allStrings);
        precomputeInvokeMetaAndCollectArgTypes(methods, allStrings, invokeMetaCache);
        collectBootstrapStrings(globalBootstrapMethods, allStrings);
        collectVmMethodArgTypeStrings(methods, allStrings);

        Map<String, Integer> globalStringIndexMap = buildGlobalStringIndex(allStrings);
        return new CollectedStrings(allStrings, invokeMetaCache, globalStringIndexMap);
    }

    private static void collectMethodLevelStrings(List<EncryptedMethodData> methods, Set<String> allStrings) {
        for (EncryptedMethodData method : methods) {
            List<String> pool = method.getStringPool();
            if (pool != null) {
                allStrings.addAll(pool);
            }
            if (method.getDescriptor() != null) {
                allStrings.add(method.getDescriptor());
            }
            List<ExceptionEntry> excTable = method.getExceptionTable();
            if (excTable == null) {
                continue;
            }
            for (ExceptionEntry e : excTable) {
                if (e.getCatchType() != null) {
                    allStrings.add(e.getCatchType());
                }
            }
        }
    }

    private static void collectMethodIdentityStrings(List<EncryptedMethodData> methods, Set<String> allStrings) {
        for (EncryptedMethodData method : methods) {
            if (method.getOwner() != null) {
                allStrings.add(method.getOwner());
            }
            if (method.getName() != null) {
                allStrings.add(method.getName());
            }
        }
    }

    private static void precomputeInvokeMetaAndCollectArgTypes(List<EncryptedMethodData> methods,
                                                               Set<String> allStrings,
                                                               Map<String, MethodDescriptorParser.DescriptorInfo> invokeMetaCache) {
        invokeMetaCache.clear();
        for (EncryptedMethodData method : methods) {
            List<String> localPool = method.getStringPool();
            List<MetaEntry> metaList = method.getMetadata();
            if (localPool == null || metaList == null) {
                continue;
            }
            for (int i = 0; i < metaList.size(); i++) {
                MetaEntry m = metaList.get(i);
                if (m.type != MetaType.META_METHOD && m.type != MetaType.META_INVOKE_DYNAMIC) {
                    continue;
                }
                if (m.descIdx < 0 || m.descIdx >= localPool.size()) {
                    continue;
                }
                String desc = localPool.get(m.descIdx);
                MethodDescriptorParser.DescriptorInfo info = MethodDescriptorParser.parse(desc);
                invokeMetaCache.put(method.getMethodId() + "_" + i, info);
                if (info.getArgTypes() != null && !info.getArgTypes().isEmpty()) {
                    allStrings.add(info.getArgTypes());
                }
            }
        }
    }

    private static void collectBootstrapStrings(List<BootstrapEntry> globalBootstrapMethods, Set<String> allStrings) {
        for (BootstrapEntry bsm : globalBootstrapMethods) {
            allStrings.add(bsm.getHandleOwner());
            allStrings.add(bsm.getHandleName());
            allStrings.add(bsm.getHandleDescriptor());

            List<Object> args = bsm.getArguments();
            List<ArgType> argTypes = bsm.getArgumentTypes();
            if (args == null || argTypes == null) {
                continue;
            }
            for (int j = 0; j < args.size(); j++) {
                Object arg = args.get(j);
                ArgType argType = argTypes.get(j);
                switch (argType) {
                    case STRING:
                    case METHOD_TYPE:
                    case CLASS:
                        allStrings.add(arg.toString());
                        break;
                    case METHOD_HANDLE:
                        String[] parts = arg.toString().split(":", 4);
                        if (parts.length >= 4) {
                            allStrings.add(parts[1]);
                            allStrings.add(parts[2]);
                            allStrings.add(parts[3]);
                        }
                        break;
                }
            }
        }
    }

    private static void collectVmMethodArgTypeStrings(List<EncryptedMethodData> methods, Set<String> allStrings) {
        for (EncryptedMethodData method : methods) {
            String desc = method.getDescriptor();
            if (desc == null) {
                continue;
            }
            String argTypes = MethodDescriptorParser.parseArgTypes(desc);
            if (argTypes != null && !argTypes.isEmpty()) {
                allStrings.add(argTypes);
            }
        }
    }

    private static Map<String, Integer> buildGlobalStringIndex(Set<String> allStrings) {
        Map<String, Integer> globalStringIndexMap = new HashMap<>();
        int globalIdx = 0;
        for (String s : allStrings) {
            globalStringIndexMap.put(s, globalIdx++);
        }
        return globalStringIndexMap;
    }

    static final class CollectedStrings {
        private final Set<String> allStrings;
        private final Map<String, MethodDescriptorParser.DescriptorInfo> invokeMetaCache;
        private final Map<String, Integer> globalStringIndexMap;

        CollectedStrings(Set<String> allStrings,
                         Map<String, MethodDescriptorParser.DescriptorInfo> invokeMetaCache,
                         Map<String, Integer> globalStringIndexMap) {
            this.allStrings = allStrings;
            this.invokeMetaCache = invokeMetaCache;
            this.globalStringIndexMap = globalStringIndexMap;
        }

        Set<String> getAllStrings() {
            return allStrings;
        }

        Map<String, MethodDescriptorParser.DescriptorInfo> getInvokeMetaCache() {
            return invokeMetaCache;
        }

        Map<String, Integer> getGlobalStringIndexMap() {
            return globalStringIndexMap;
        }
    }
}
