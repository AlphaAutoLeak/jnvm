package com.alphaautoleak.jnvm.codegen.emitter;

import com.alphaautoleak.jnvm.asm.ArgType;
import com.alphaautoleak.jnvm.asm.BootstrapEntry;

import java.io.PrintWriter;
import java.util.List;
import java.util.function.ToIntFunction;

/**
 * Emits global bootstrap method tables for vm_data.c.
 */
class VmBootstrapMethodsEmitter {
    private final List<BootstrapEntry> bootstrapMethods;
    private final ToIntFunction<String> stringIndexResolver;

    VmBootstrapMethodsEmitter(List<BootstrapEntry> bootstrapMethods, ToIntFunction<String> stringIndexResolver) {
        this.bootstrapMethods = bootstrapMethods;
        this.stringIndexResolver = stringIndexResolver;
    }

    void emit(PrintWriter w) {
        if (bootstrapMethods.isEmpty()) {
            w.println("VMBootstrapMethod vm_bootstrap_methods[] = {};");
            w.println();
            return;
        }

        emitBootstrapArgsArrays(w);
        emitBootstrapMethodArray(w);
    }

    private void emitBootstrapArgsArrays(PrintWriter w) {
        for (int i = 0; i < bootstrapMethods.size(); i++) {
            BootstrapEntry bsm = bootstrapMethods.get(i);
            List<Object> args = bsm.getArguments();
            List<ArgType> argTypes = bsm.getArgumentTypes();

            if (args == null || args.isEmpty()) {
                continue;
            }

            w.printf("static BsmArg bsm%d_args[] = {", i);
            for (int j = 0; j < args.size(); j++) {
                Object arg = args.get(j);
                ArgType argType = argTypes.get(j);

                w.printf("\n    { .type=%s, ", bsmArgTypeToString(argType));
                emitSingleArgInitializer(w, arg, argType);
                w.printf(" },");
            }
            w.println("\n};");
        }
    }

    private void emitSingleArgInitializer(PrintWriter w, Object arg, ArgType argType) {
        switch (argType) {
            case STRING:
            case METHOD_TYPE:
            case CLASS:
                w.printf(".strIdx=%d", stringIndexResolver.applyAsInt(arg.toString()));
                break;
            case INTEGER:
                w.printf(".intVal=%d", (Integer) arg);
                break;
            case LONG:
                w.printf(".longVal=%dL", (Long) arg);
                break;
            case FLOAT:
                w.printf(".floatVal=%af", (Float) arg);
                break;
            case DOUBLE:
                w.printf(".doubleVal=%a", (Double) arg);
                break;
            case METHOD_HANDLE:
                String[] parts = arg.toString().split(":", 4);
                if (parts.length >= 4) {
                    w.printf(".handleTag=%s, .ownerIdx=%d, .nameIdx=%d, .descIdx=%d",
                            parts[0],
                            stringIndexResolver.applyAsInt(parts[1]),
                            stringIndexResolver.applyAsInt(parts[2]),
                            stringIndexResolver.applyAsInt(parts[3]));
                } else {
                    w.printf(".handleTag=0, .ownerIdx=-1, .nameIdx=-1, .descIdx=-1");
                }
                break;
        }
    }

    private void emitBootstrapMethodArray(PrintWriter w) {
        w.println("VMBootstrapMethod vm_bootstrap_methods[] = {");
        for (int i = 0; i < bootstrapMethods.size(); i++) {
            BootstrapEntry bsm = bootstrapMethods.get(i);
            w.printf("    { .handleTag=%d, ", bsm.getHandleTag());
            w.printf(".ownerIdx=%d, ", stringIndexResolver.applyAsInt(bsm.getHandleOwner()));
            w.printf(".nameIdx=%d, ", stringIndexResolver.applyAsInt(bsm.getHandleName()));
            w.printf(".descIdx=%d, ", stringIndexResolver.applyAsInt(bsm.getHandleDescriptor()));

            List<Object> args = bsm.getArguments();
            if (args != null && !args.isEmpty()) {
                w.printf(".args=bsm%d_args, .argCount=%d", i, args.size());
            } else {
                w.printf(".args=NULL, .argCount=0");
            }
            w.printf(" },\n");
        }
        w.println("};");
        w.println();
    }

    private String bsmArgTypeToString(ArgType type) {
        switch (type) {
            case STRING:
                return "BSM_ARG_STRING";
            case INTEGER:
                return "BSM_ARG_INTEGER";
            case LONG:
                return "BSM_ARG_LONG";
            case FLOAT:
                return "BSM_ARG_FLOAT";
            case DOUBLE:
                return "BSM_ARG_DOUBLE";
            case METHOD_TYPE:
                return "BSM_ARG_METHOD_TYPE";
            case METHOD_HANDLE:
                return "BSM_ARG_METHOD_HANDLE";
            case CLASS:
                return "BSM_ARG_CLASS";
            default:
                return "BSM_ARG_STRING";
        }
    }
}
