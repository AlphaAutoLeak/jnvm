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

    private static final class DirectNativeEntry {
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

    private static final class DirectClassRegistration {
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
            emitExecuteFunctions(w);
            w.println();

            // Direct native method stubs
            emitDirectNativeFunctions(w);
            w.println();

            // Class-level RegisterNatives helpers for direct mode
            emitDirectRegistrationHelpers(w);
            w.println();

            // Generate RegisterNatives
            emitRegisterNatives(w);
            w.println();

            // Generate JNI_OnLoad
            emitJNIOnLoad(w);
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

    /**
     * Generate legacy execute functions for each return type
     * Format: methodId + Object[]
     */
    private void emitExecuteFunctions(PrintWriter w) {
        w.println("/* void return type */");
        w.println("static void JNICALL native_execute_void(JNIEnv* env, jclass cls,");
        w.println("                                        jint methodId, jobjectArray args) {");
        w.println("    (void)cls;");
        w.println("    vm_execute_method_void(env, methodId, args);");
        w.println("}");
        w.println();

        w.println("/* int return type (boolean/byte/char/short/int) */");
        w.println("static jint JNICALL native_execute_int(JNIEnv* env, jclass cls,");
        w.println("                                         jint methodId, jobjectArray args) {");
        w.println("    (void)cls;");
        w.println("    return vm_execute_method_int(env, methodId, args);");
        w.println("}");
        w.println();

        w.println("/* long return type */");
        w.println("static jlong JNICALL native_execute_long(JNIEnv* env, jclass cls,");
        w.println("                                           jint methodId, jobjectArray args) {");
        w.println("    (void)cls;");
        w.println("    return vm_execute_method_long(env, methodId, args);");
        w.println("}");
        w.println();

        w.println("/* float return type */");
        w.println("static jfloat JNICALL native_execute_float(JNIEnv* env, jclass cls,");
        w.println("                                             jint methodId, jobjectArray args) {");
        w.println("    (void)cls;");
        w.println("    return vm_execute_method_float(env, methodId, args);");
        w.println("}");
        w.println();

        w.println("/* double return type */");
        w.println("static jdouble JNICALL native_execute_double(JNIEnv* env, jclass cls,");
        w.println("                                              jint methodId, jobjectArray args) {");
        w.println("    (void)cls;");
        w.println("    return vm_execute_method_double(env, methodId, args);");
        w.println("}");
        w.println();

        w.println("/* object return type */");
        w.println("static jobject JNICALL native_execute_object(JNIEnv* env, jclass cls,");
        w.println("                                               jint methodId, jobjectArray args) {");
        w.println("    (void)cls;");
        w.println("    return vm_execute_method_object(env, methodId, args);");
        w.println("}");
    }

    private void emitDirectNativeFunctions(PrintWriter w) {
        if (!directNativeRewrite || directEntries.isEmpty()) {
            w.println("/* no direct-native method stubs */");
            return;
        }
        for (DirectNativeEntry entry : directEntries) {
            emitDirectNativeFunction(w, entry);
            w.println();
        }
    }

    private void emitDirectNativeFunction(PrintWriter w, DirectNativeEntry entry) {
        StringBuilder sig = new StringBuilder();
        sig.append("static ")
                .append(jniTypeForReturnDescriptor(entry.returnDescriptor))
                .append(" JNICALL ")
                .append(entry.nativeFunctionName)
                .append("(JNIEnv* env, ")
                .append(entry.isStatic ? "jclass cls" : "jobject receiver");

        for (int i = 0; i < entry.paramDescriptors.size(); i++) {
            sig.append(", ")
                    .append(jniTypeForParamDescriptor(entry.paramDescriptors.get(i)))
                    .append(" p")
                    .append(i);
        }
        sig.append(") {");

        w.println("/* direct-native stub for " + entry.owner + "." + entry.methodName + entry.methodDesc + " */");
        w.println(sig.toString());
        w.println("    int maxLocals = vm_methods[" + entry.methodId + "].maxLocals;");
        w.println("    int localCap = maxLocals > 0 ? maxLocals : 1;");
        w.println("    VMValue tempLocals[localCap];");
        w.println("    memset(tempLocals, 0, (size_t)localCap * sizeof(VMValue));");
        if (!entry.isStatic) {
            w.println("    tempLocals[0].l = receiver;");
        }

        int slot = entry.isStatic ? 0 : 1;
        for (int i = 0; i < entry.paramDescriptors.size(); i++) {
            String desc = entry.paramDescriptors.get(i);
            String valueName = "p" + i;
            w.println("    " + assignmentCode(slot, desc, valueName));
            slot += BridgeFastPathUtil.isWideDescriptor(desc) ? 2 : 1;
        }

        int obfMethodId = BridgeFastPathUtil.obfuscateMethodId(entry.methodId, methodIdXorKey);
        String obfMethodLiteral = "((jint)0x" + Integer.toUnsignedString(obfMethodId, 16) + "u)";
        if (entry.isStatic) {
            w.println("    jclass callerClass = cls;");
        } else {
            w.println("    jclass callerClass = receiver ? (*env)->GetObjectClass(env, receiver) : NULL;");
        }
        w.println("    ExecuteResult r = vm_execute_common(env, " + obfMethodLiteral + ", NULL, tempLocals, maxLocals, callerClass);");
        if (!entry.isStatic) {
            w.println("    if (callerClass) (*env)->DeleteLocalRef(env, callerClass);");
        }
        if (!"V".equals(entry.returnDescriptor)) {
            w.println("    if (r.returnType == 'X') return " + defaultReturnLiteral(entry.returnDescriptor) + ";");
            w.println("    return " + returnValueExpr(entry.returnDescriptor) + ";");
        } else {
            w.println("    (void)r;");
            w.println("    return;");
        }
        w.println("}");
    }

    private void emitDirectRegistrationHelpers(PrintWriter w) {
        w.println("/* Bridge native: register direct native methods for a specific class */");
        w.println("static void JNICALL native_register_class_natives(JNIEnv* env, jclass cls, jclass targetClass);");
        w.println();
        if (!directNativeRewrite || directClassRegistrations.isEmpty()) {
            w.println("static int register_target_class_natives(JNIEnv* env, jclass targetClass) {");
            w.println("    (void)env;");
            w.println("    (void)targetClass;");
            w.println("    return JNI_OK;");
            w.println("}");
        } else {
            for (int i = 0; i < directClassRegistrations.size(); i++) {
                DirectClassRegistration reg = directClassRegistrations.get(i);
                w.println("static JNINativeMethod class_natives_" + i + "[] = {");
                for (int j = 0; j < reg.entries.size(); j++) {
                    DirectNativeEntry entry = reg.entries.get(j);
                    String line = "{ \"" + entry.methodName + "\", \"" + entry.methodDesc + "\", (void*)" + entry.nativeFunctionName + " }";
                    if (j + 1 < reg.entries.size()) {
                        w.println("    " + line + ",");
                    } else {
                        w.println("    " + line);
                    }
                }
                w.println("};");
                w.println();
            }

            w.println("static int register_target_class_natives(JNIEnv* env, jclass targetClass) {");
            w.println("    jclass classClass = (*env)->FindClass(env, \"java/lang/Class\");");
            w.println("    if (classClass == NULL) return JNI_ERR;");
            w.println("    jmethodID getNameMid = (*env)->GetMethodID(env, classClass, \"getName\", \"()Ljava/lang/String;\");");
            w.println("    if (getNameMid == NULL) return JNI_ERR;");
            w.println("    jstring nameObj = (jstring)(*env)->CallObjectMethod(env, targetClass, getNameMid);");
            w.println("    if ((*env)->ExceptionCheck(env) || nameObj == NULL) return JNI_ERR;");
            w.println("    const char* targetName = (*env)->GetStringUTFChars(env, nameObj, NULL);");
            w.println("    if (targetName == NULL) {");
            w.println("        (*env)->DeleteLocalRef(env, nameObj);");
            w.println("        return JNI_ERR;");
            w.println("    }");
            w.println("    int rc = JNI_ERR;");
            for (int i = 0; i < directClassRegistrations.size(); i++) {
                DirectClassRegistration reg = directClassRegistrations.get(i);
                String dottedName = reg.owner.replace('/', '.');
                String prefix = (i == 0) ? "if" : "else if";
                w.println("    " + prefix + " (strcmp(targetName, \"" + dottedName + "\") == 0) {");
                w.println("        rc = ((*env)->RegisterNatives(env, targetClass, class_natives_" + i + ",");
                w.println("              sizeof(class_natives_" + i + ") / sizeof(class_natives_" + i + "[0])) < 0) ? JNI_ERR : JNI_OK;");
                w.println("    }");
            }
            w.println("    (*env)->ReleaseStringUTFChars(env, nameObj, targetName);");
            w.println("    (*env)->DeleteLocalRef(env, nameObj);");
            w.println("    return rc;");
            w.println("}");
        }
        w.println();
        w.println("static void JNICALL native_register_class_natives(JNIEnv* env, jclass cls, jclass targetClass) {");
        w.println("    (void)cls;");
        w.println("    if (targetClass == NULL) return;");
        w.println("    if (register_target_class_natives(env, targetClass) != JNI_OK) {");
        w.println("        jclass errCls = (*env)->FindClass(env, \"java/lang/UnsatisfiedLinkError\");");
        w.println("        if (errCls) {");
        w.println("            (*env)->ThrowNew(env, errCls, \"JNVM failed to register class native methods\");");
        w.println("        }");
        w.println("    }");
        w.println("}");
    }

    private String assignmentCode(int slot, String descriptor, String valueName) {
        if ("J".equals(descriptor)) {
            return "tempLocals[" + slot + "].j = " + valueName + ";";
        }
        if ("F".equals(descriptor)) {
            return "tempLocals[" + slot + "].f = " + valueName + ";";
        }
        if ("D".equals(descriptor)) {
            return "tempLocals[" + slot + "].d = " + valueName + ";";
        }
        if (BridgeFastPathUtil.isObjectLikeDescriptor(descriptor)) {
            return "tempLocals[" + slot + "].l = " + valueName + ";";
        }
        return "tempLocals[" + slot + "].i = (jint)" + valueName + ";";
    }

    private String jniTypeForParamDescriptor(String descriptor) {
        switch (descriptor.charAt(0)) {
            case 'Z':
                return "jboolean";
            case 'B':
                return "jbyte";
            case 'C':
                return "jchar";
            case 'S':
                return "jshort";
            case 'I':
                return "jint";
            case 'J':
                return "jlong";
            case 'F':
                return "jfloat";
            case 'D':
                return "jdouble";
            case 'L':
                if ("Ljava/lang/Class;".equals(descriptor)) {
                    return "jclass";
                }
                return "jobject";
            case '[':
                return "jobject";
            default:
                return "jobject";
        }
    }

    private String jniTypeForReturnDescriptor(String descriptor) {
        switch (descriptor.charAt(0)) {
            case 'V':
                return "void";
            case 'Z':
                return "jboolean";
            case 'B':
                return "jbyte";
            case 'C':
                return "jchar";
            case 'S':
                return "jshort";
            case 'I':
                return "jint";
            case 'J':
                return "jlong";
            case 'F':
                return "jfloat";
            case 'D':
                return "jdouble";
            default:
                return "jobject";
        }
    }

    private String returnValueExpr(String descriptor) {
        switch (descriptor.charAt(0)) {
            case 'Z':
                return "(jboolean)r.value.i";
            case 'B':
                return "(jbyte)r.value.i";
            case 'C':
                return "(jchar)r.value.i";
            case 'S':
                return "(jshort)r.value.i";
            case 'I':
                return "r.value.i";
            case 'J':
                return "r.value.j";
            case 'F':
                return "r.value.f";
            case 'D':
                return "r.value.d";
            default:
                return "(jobject)r.value.l";
        }
    }

    private String defaultReturnLiteral(String descriptor) {
        switch (descriptor.charAt(0)) {
            case 'Z':
                return "JNI_FALSE";
            case 'B':
                return "(jbyte)0";
            case 'C':
                return "(jchar)0";
            case 'S':
                return "(jshort)0";
            case 'I':
                return "0";
            case 'J':
                return "0";
            case 'F':
                return "0.0f";
            case 'D':
                return "0.0";
            default:
                return "NULL";
        }
    }

    /**
     * Generates RegisterNatives table
     */
    private void emitRegisterNatives(PrintWriter w) {
        List<String> nativeRegistrations = new ArrayList<>();
        nativeRegistrations.add("{ \"__jnvm$registerClassNatives\", \"(Ljava/lang/Class;)V\",       (void*)native_register_class_natives }");
        nativeRegistrations.add("{ \"executeVoid\",   \"(I[Ljava/lang/Object;)V\",                  (void*)native_execute_void }");
        nativeRegistrations.add("{ \"executeInt\",    \"(I[Ljava/lang/Object;)I\",                  (void*)native_execute_int }");
        nativeRegistrations.add("{ \"executeLong\",   \"(I[Ljava/lang/Object;)J\",                  (void*)native_execute_long }");
        nativeRegistrations.add("{ \"executeFloat\",  \"(I[Ljava/lang/Object;)F\",                  (void*)native_execute_float }");
        nativeRegistrations.add("{ \"executeDouble\", \"(I[Ljava/lang/Object;)D\",                  (void*)native_execute_double }");
        nativeRegistrations.add("{ \"executeObject\", \"(I[Ljava/lang/Object;)Ljava/lang/Object;\", (void*)native_execute_object }");

        w.println("/* JNI method registration table */");
        w.println("static JNINativeMethod native_methods[] = {");
        for (int i = 0; i < nativeRegistrations.size(); i++) {
            String line = nativeRegistrations.get(i);
            if (i + 1 < nativeRegistrations.size()) {
                w.println("    " + line + ",");
            } else {
                w.println("    " + line);
            }
        }
        w.println("};");
        w.println();

        w.println("/* Register native methods */");
        w.println("static int register_native_methods(JNIEnv* env) {");
        w.println("    jclass cls = (*env)->FindClass(env, \"" + bridgeClass + "\");");
        w.println("    if (cls == NULL) {");
        w.println("        return JNI_ERR;");
        w.println("    }");
        w.println();
        w.println("    if ((*env)->RegisterNatives(env, cls, native_methods,");
        w.println("                                 sizeof(native_methods) / sizeof(native_methods[0])) < 0) {");
        w.println("        return JNI_ERR;");
        w.println("    }");
        w.println();
        w.println("    return JNI_OK;");
        w.println("}");
    }

    /**
     * Generates JNI_OnLoad
     */
    private void emitJNIOnLoad(PrintWriter w) {
        w.println("/* JNI_OnLoad - initialize on library load */");
        w.println("JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved) {");
        w.println("    JNIEnv* env = NULL;");
        w.println("    if ((*vm)->GetEnv(vm, (void**)&env, JNI_VERSION_1_8) != JNI_OK) {");
        w.println("        return JNI_ERR;");
        w.println("    }");
        w.println();
        w.println("    // Initialize frame memory pool");
        w.println("    frame_pool_init();");
        w.println();
        if (encryptStrings) {
            w.println("    // Initialize string pool (decrypt all strings)");
            w.println("    vm_init_strings();");
            w.println();
        }
        w.println("    // Decode obfuscated metadata once at startup");
        w.println("    vm_init_meta_all();");
        w.println();
        w.println("    // Initialize VM method lookup table (for direct VM-to-VM calls)");
        w.println("    vm_init_method_lookup();");
        w.println();
        w.println("    if (register_native_methods(env) != JNI_OK) {");
        w.println("        return JNI_ERR;");
        w.println("    }");
        w.println();
        w.println("    return JNI_VERSION_1_8;");
        w.println("}");
    }
}
