package com.alphaautoleak.jnvm.asm;

import org.objectweb.asm.Handle;
import org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.List;

/**
 * Maintains per-method bootstrap method table and normalized bootstrap arguments.
 */
class BootstrapMethodRegistry {

    private final List<BootstrapEntry> entries = new ArrayList<>();

    int findOrCreate(Handle bsm, Object[] bsmArgs) {
        for (int i = 0; i < entries.size(); i++) {
            BootstrapEntry e = entries.get(i);
            if (e.getHandleTag() != bsm.getTag()) {
                continue;
            }
            if (!e.getHandleOwner().equals(bsm.getOwner())
                    || !e.getHandleName().equals(bsm.getName())
                    || !e.getHandleDescriptor().equals(bsm.getDesc())) {
                continue;
            }
            if (argsEqual(e.getArguments(), bsmArgs)) {
                return i;
            }
        }

        BootstrapEntry entry = new BootstrapEntry();
        entry.setHandleTag(bsm.getTag());
        entry.setHandleOwner(bsm.getOwner());
        entry.setHandleName(bsm.getName());
        entry.setHandleDescriptor(bsm.getDesc());

        List<Object> args = new ArrayList<>();
        List<ArgType> argTypes = new ArrayList<>();
        if (bsmArgs != null) {
            for (Object arg : bsmArgs) {
                appendNormalizedArg(args, argTypes, arg);
            }
        }
        entry.setArguments(args);
        entry.setArgumentTypes(argTypes);

        int idx = entries.size();
        entries.add(entry);
        return idx;
    }

    List<BootstrapEntry> getEntries() {
        return entries;
    }

    private static void appendNormalizedArg(List<Object> args, List<ArgType> argTypes, Object arg) {
        if (arg instanceof Integer) {
            args.add(arg);
            argTypes.add(ArgType.INTEGER);
            return;
        }
        if (arg instanceof Long) {
            args.add(arg);
            argTypes.add(ArgType.LONG);
            return;
        }
        if (arg instanceof Float) {
            args.add(arg);
            argTypes.add(ArgType.FLOAT);
            return;
        }
        if (arg instanceof Double) {
            args.add(arg);
            argTypes.add(ArgType.DOUBLE);
            return;
        }
        if (arg instanceof String) {
            args.add(arg);
            argTypes.add(ArgType.STRING);
            return;
        }
        if (arg instanceof Type) {
            Type t = (Type) arg;
            String desc = t.getDescriptor();
            if (desc.contains("(")) {
                args.add(desc);
                argTypes.add(ArgType.METHOD_TYPE);
            } else {
                args.add(t.getInternalName());
                argTypes.add(ArgType.CLASS);
            }
            return;
        }
        if (arg instanceof Handle) {
            args.add(serializeHandle((Handle) arg));
            argTypes.add(ArgType.METHOD_HANDLE);
            return;
        }
        args.add(arg.toString());
        argTypes.add(ArgType.STRING);
    }

    private static boolean argsEqual(List<Object> normalizedArgs, Object[] runtimeArgs) {
        if (normalizedArgs == null && runtimeArgs == null) {
            return true;
        }
        if (normalizedArgs == null || runtimeArgs == null) {
            return false;
        }
        if (normalizedArgs.size() != runtimeArgs.length) {
            return false;
        }

        for (int i = 0; i < normalizedArgs.size(); i++) {
            Object normalized = normalizedArgs.get(i);
            Object runtime = runtimeArgs[i];
            if (!argEquals(normalized, runtime)) {
                return false;
            }
        }
        return true;
    }

    private static boolean argEquals(Object normalized, Object runtime) {
        if (normalized == null && runtime == null) {
            return true;
        }
        if (normalized == null || runtime == null) {
            return false;
        }

        if (runtime instanceof Handle) {
            return normalized.toString().equals(serializeHandle((Handle) runtime));
        }
        if (runtime instanceof Type) {
            Type t = (Type) runtime;
            String desc = t.getDescriptor();
            if (desc.contains("(")) {
                return normalized.toString().equals(desc);
            }
            return normalized.toString().equals(t.getInternalName());
        }
        return normalized.toString().equals(runtime.toString());
    }

    private static String serializeHandle(Handle handle) {
        return handle.getTag() + ":" + handle.getOwner() + ":" + handle.getName() + ":" + handle.getDesc();
    }
}
