package com.alphaautoleak.jnvm.codegen.emitter;

import com.alphaautoleak.jnvm.codegen.emitter.helper.VMHelper;

import java.io.PrintWriter;

final class VmInterpreterSourcePreambleEmitter {

    private VmInterpreterSourcePreambleEmitter() {
    }

    static void emit(PrintWriter w,
                     boolean debug,
                     boolean encryptStrings,
                     int methodIdXorKey,
                     VmCachingSectionEmitter cachingSectionEmitter,
                     Iterable<VMHelper> helpers) {
        w.println("#define _POSIX_C_SOURCE 200809L");
        w.println("#include \"vm_interpreter.h\"");
        w.println("#include \"vm_data.h\"");
        w.println("#include \"chacha20.h\"");
        w.println("#include <stdio.h>");
        w.println("#include <stdlib.h>");
        w.println("#include <string.h>");
        w.println("#include <stdatomic.h>  // for atomic cache operations");
        w.println();

        w.println("#define METHOD_ID_XOR_KEY 0x" + Integer.toHexString(methodIdXorKey));
        w.println();

        emitDebugMacros(w, debug);
        emitPerfMacros(w, debug);

        w.println("// === Branch prediction hints ===");
        w.println("#define LIKELY(x)   __builtin_expect(!!(x), 1)");
        w.println("#define UNLIKELY(x) __builtin_expect(!!(x), 0)");
        w.println("#define META_FLAG_MH_POLY_INVOKE 0x01u");
        w.println();

        cachingSectionEmitter.emit(w);

        for (VMHelper helper : helpers) {
            helper.generateSource(w);
        }

        w.println("static void vm_decrypt_string(const unsigned char* enc, int len, char* out, const unsigned char* key) {");
        w.println("    for (int i = 0; i < len; i++) {");
        w.println("        out[i] = (char)((enc[i] - (i & 0xFF)) ^ key[i % 8]);");
        w.println("    }");
        w.println("    out[len] = '\\0';");
        w.println("}");
        w.println();

        if (encryptStrings) {
            w.println("/* string encryption enabled */");
            w.println();
        }
    }

    private static void emitDebugMacros(PrintWriter w, boolean debug) {
        if (debug) {
            w.println("#define VM_LOG(fmt, ...) printf(\"[VM] \" fmt, ##__VA_ARGS__)");
            w.println("#define VM_DEBUG_LOG(fmt, ...) printf(\"[VM-DEBUG] \" fmt, ##__VA_ARGS__)");
            w.println("#define VM_DEBUG_ENABLED 1");
            w.println("#define VM_LOG_FLUSH() fflush(stdout)");
        } else {
            w.println("#define VM_LOG(fmt, ...)");
            w.println("#define VM_DEBUG_LOG(fmt, ...)");
            w.println("#define VM_DEBUG_ENABLED 0");
            w.println("#define VM_LOG_FLUSH() ((void)0)");
        }
        w.println();
    }

    private static void emitPerfMacros(PrintWriter w, boolean debug) {
        if (debug) {
            w.println("// === Invoke path perf counters (debug only, thread-local) ===");
            w.println("static __thread unsigned long long vm_perf_invoke_total = 0ULL;");
            w.println("static __thread unsigned long long vm_perf_direct_candidate = 0ULL;");
            w.println("static __thread unsigned long long vm_perf_direct_hit = 0ULL;");
            w.println("static __thread unsigned long long vm_perf_jni_path = 0ULL;");
            w.println("static __thread unsigned long long vm_perf_direct_reject = 0ULL;");
            w.println("static inline void vm_perf_invoke_maybe_dump(void) {");
            w.println("    if ((vm_perf_invoke_total & 0x3FFFULL) == 0ULL) {");
            w.println("        VM_LOG(\"PERF invoke: total=%llu candidate=%llu direct=%llu jni=%llu reject=%llu\\n\",");
            w.println("               vm_perf_invoke_total, vm_perf_direct_candidate, vm_perf_direct_hit,");
            w.println("               vm_perf_jni_path, vm_perf_direct_reject);");
            w.println("        VM_LOG_FLUSH();");
            w.println("    }");
            w.println("}");
            w.println("#define VM_PERF_INVOKE_BEGIN() do { ++vm_perf_invoke_total; vm_perf_invoke_maybe_dump(); } while(0)");
            w.println("#define VM_PERF_DIRECT_CANDIDATE() do { ++vm_perf_direct_candidate; } while(0)");
            w.println("#define VM_PERF_DIRECT_HIT() do { ++vm_perf_direct_hit; } while(0)");
            w.println("#define VM_PERF_JNI_PATH() do { ++vm_perf_jni_path; } while(0)");
            w.println("#define VM_PERF_DIRECT_REJECT() do { ++vm_perf_direct_reject; } while(0)");
        } else {
            w.println("#define VM_PERF_INVOKE_BEGIN() ((void)0)");
            w.println("#define VM_PERF_DIRECT_CANDIDATE() ((void)0)");
            w.println("#define VM_PERF_DIRECT_HIT() ((void)0)");
            w.println("#define VM_PERF_JNI_PATH() ((void)0)");
            w.println("#define VM_PERF_DIRECT_REJECT() ((void)0)");
        }
        w.println();
    }
}
