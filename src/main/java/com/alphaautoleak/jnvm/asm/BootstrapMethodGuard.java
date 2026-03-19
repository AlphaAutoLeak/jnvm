package com.alphaautoleak.jnvm.asm;

import com.alphaautoleak.jnvm.utils.MethodKeyUtil;
import org.objectweb.asm.Handle;
import org.objectweb.asm.tree.*;

import java.util.*;

/**
 * Tracks invokedynamic bootstrap method usage and filters methods that are
 * unsafe to virtualize in JVM linkage path.
 */
class BootstrapMethodGuard {

    /** Methods used as invokedynamic bootstrap targets: owner.name.desc */
    private final Set<String> bootstrapMethodTargets = new HashSet<>();

    /** Classes that own invokedynamic bootstrap methods */
    private final Set<String> bootstrapOwnerClasses = new HashSet<>();

    /** Method dependency graph: method -> directly referenced methods/handles */
    private final Map<String, Set<String>> methodDependencies = new HashMap<>();

    void scanClass(ClassNode cn) {
        collectBootstrapMethodTargets(cn);
        collectMethodDependencies(cn);
    }

    FilterOutcome filter(List<MethodInfo> protectedMethods, boolean protectBootstrapPayload) {
        if (protectedMethods.isEmpty()) {
            return new FilterOutcome(protectedMethods, false, protectBootstrapPayload, 0, 0);
        }

        collectFallbackBootstrapTargetsFromMetadata(protectedMethods);
        if (bootstrapMethodTargets.isEmpty()) {
            return new FilterOutcome(protectedMethods, false, protectBootstrapPayload, 0, 0);
        }

        Set<String> bootstrapSensitiveMethods = computeBootstrapSensitiveMethods();

        List<MethodInfo> filtered = new ArrayList<>(protectedMethods.size());
        int skipByClassCount = 0;
        int skipByMethodCount = 0;

        for (MethodInfo info : protectedMethods) {
            String key = MethodKeyUtil.of(info.getOwner(), info.getName(), info.getDescriptor());
            boolean skipByClass = !protectBootstrapPayload && bootstrapOwnerClasses.contains(info.getOwner());
            boolean skipByMethod = bootstrapSensitiveMethods.contains(key);
            if (skipByClass || skipByMethod) {
                if (skipByClass) {
                    skipByClassCount++;
                    System.out.println("  [SKIP] Bootstrap-owner class method: " + key);
                } else {
                    skipByMethodCount++;
                    System.out.println("  [SKIP] Bootstrap-sensitive method: " + key);
                }
                continue;
            }
            filtered.add(info);
        }

        boolean changed = filtered.size() != protectedMethods.size();
        return new FilterOutcome(filtered, changed, protectBootstrapPayload, skipByClassCount, skipByMethodCount);
    }

    private void collectBootstrapMethodTargets(ClassNode cn) {
        for (MethodNode mn : cn.methods) {
            if (mn.instructions == null || mn.instructions.size() == 0) {
                continue;
            }
            for (AbstractInsnNode node = mn.instructions.getFirst(); node != null; node = node.getNext()) {
                if (!(node instanceof InvokeDynamicInsnNode)) {
                    continue;
                }
                InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) node;
                if (indy.bsm != null) {
                    addBootstrapTarget(indy.bsm.getOwner(), indy.bsm.getName(), indy.bsm.getDesc());
                }
                if (indy.bsmArgs == null) {
                    continue;
                }
                for (Object arg : indy.bsmArgs) {
                    if (!(arg instanceof Handle)) {
                        continue;
                    }
                    Handle h = (Handle) arg;
                    addBootstrapTarget(h.getOwner(), h.getName(), h.getDesc());
                }
            }
        }
    }

    private void collectMethodDependencies(ClassNode cn) {
        for (MethodNode mn : cn.methods) {
            if (mn.instructions == null || mn.instructions.size() == 0) {
                continue;
            }
            String srcKey = MethodKeyUtil.of(cn.name, mn.name, mn.desc);
            Set<String> deps = methodDependencies.computeIfAbsent(srcKey, k -> new HashSet<>());

            for (AbstractInsnNode node = mn.instructions.getFirst(); node != null; node = node.getNext()) {
                if (node instanceof MethodInsnNode) {
                    MethodInsnNode mi = (MethodInsnNode) node;
                    deps.add(MethodKeyUtil.of(mi.owner, mi.name, mi.desc));
                    continue;
                }
                if (node instanceof LdcInsnNode) {
                    Object cst = ((LdcInsnNode) node).cst;
                    if (cst instanceof Handle) {
                        Handle h = (Handle) cst;
                        deps.add(MethodKeyUtil.of(h.getOwner(), h.getName(), h.getDesc()));
                    }
                    continue;
                }
                if (node instanceof InvokeDynamicInsnNode) {
                    InvokeDynamicInsnNode indy = (InvokeDynamicInsnNode) node;
                    if (indy.bsm != null) {
                        Handle bsm = indy.bsm;
                        deps.add(MethodKeyUtil.of(bsm.getOwner(), bsm.getName(), bsm.getDesc()));
                    }
                    if (indy.bsmArgs == null) {
                        continue;
                    }
                    for (Object arg : indy.bsmArgs) {
                        if (!(arg instanceof Handle)) {
                            continue;
                        }
                        Handle h = (Handle) arg;
                        deps.add(MethodKeyUtil.of(h.getOwner(), h.getName(), h.getDesc()));
                    }
                }
            }
        }
    }

    private void collectFallbackBootstrapTargetsFromMetadata(List<MethodInfo> protectedMethods) {
        for (MethodInfo info : protectedMethods) {
            List<BootstrapEntry> bsms = info.getBootstrapMethods();
            if (bsms == null) {
                continue;
            }
            for (BootstrapEntry bsm : bsms) {
                if (bsm.getHandleOwner() != null && bsm.getHandleName() != null && bsm.getHandleDescriptor() != null) {
                    addBootstrapTarget(bsm.getHandleOwner(), bsm.getHandleName(), bsm.getHandleDescriptor());
                }

                List<Object> args = bsm.getArguments();
                List<ArgType> argTypes = bsm.getArgumentTypes();
                if (args == null || argTypes == null) {
                    continue;
                }
                int n = Math.min(args.size(), argTypes.size());
                for (int i = 0; i < n; i++) {
                    if (argTypes.get(i) != ArgType.METHOD_HANDLE) {
                        continue;
                    }
                    addMethodHandleArgTarget(args.get(i));
                }
            }
        }
    }

    private void addMethodHandleArgTarget(Object raw) {
        if (raw == null) {
            return;
        }
        String[] parts = raw.toString().split(":", 4);
        if (parts.length < 4) {
            return;
        }
        addBootstrapTarget(parts[1], parts[2], parts[3]);
    }

    private Set<String> computeBootstrapSensitiveMethods() {
        Set<String> bootstrapSensitiveMethods = new HashSet<>();
        Deque<String> queue = new ArrayDeque<>(bootstrapMethodTargets);

        while (!queue.isEmpty()) {
            String current = queue.pollFirst();
            if (!bootstrapSensitiveMethods.add(current)) {
                continue;
            }
            Set<String> deps = methodDependencies.get(current);
            if (deps == null || deps.isEmpty()) {
                continue;
            }
            for (String dep : deps) {
                if (!bootstrapSensitiveMethods.contains(dep)) {
                    queue.addLast(dep);
                }
            }
        }

        return bootstrapSensitiveMethods;
    }

    private void addBootstrapTarget(String owner, String name, String desc) {
        if (owner == null || name == null || desc == null) {
            return;
        }
        bootstrapOwnerClasses.add(owner);
        bootstrapMethodTargets.add(MethodKeyUtil.of(owner, name, desc));
    }

    static final class FilterOutcome {
        private final List<MethodInfo> filteredMethods;
        private final boolean changed;
        private final boolean payloadOnlyMode;
        private final int skipByClassCount;
        private final int skipByDependencyCount;

        FilterOutcome(List<MethodInfo> filteredMethods,
                      boolean changed,
                      boolean payloadOnlyMode,
                      int skipByClassCount,
                      int skipByDependencyCount) {
            this.filteredMethods = filteredMethods;
            this.changed = changed;
            this.payloadOnlyMode = payloadOnlyMode;
            this.skipByClassCount = skipByClassCount;
            this.skipByDependencyCount = skipByDependencyCount;
        }

        List<MethodInfo> getFilteredMethods() {
            return filteredMethods;
        }

        boolean isChanged() {
            return changed;
        }

        int getTotalSkippedCount() {
            return skipByClassCount + skipByDependencyCount;
        }

        int getSkipByClassCount() {
            return skipByClassCount;
        }

        int getSkipByDependencyCount() {
            return skipByDependencyCount;
        }

        String getModeName() {
            return payloadOnlyMode ? "payload-only" : "owner-class-safe";
        }
    }
}
