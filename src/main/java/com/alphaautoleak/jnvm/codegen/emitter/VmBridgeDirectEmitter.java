package com.alphaautoleak.jnvm.codegen.emitter;

import com.alphaautoleak.jnvm.utils.BridgeFastPathUtil;

import java.io.PrintWriter;
import java.util.List;

final class VmBridgeDirectEmitter {

    private VmBridgeDirectEmitter() {
    }

    static void emitDirectNativeFunctions(PrintWriter w,
                                          boolean directNativeRewrite,
                                          List<VmBridgeGenerator.DirectNativeEntry> directEntries,
                                          int methodIdXorKey) {
        if (!directNativeRewrite || directEntries.isEmpty()) {
            w.println("/* no direct-native method stubs */");
            return;
        }
        for (VmBridgeGenerator.DirectNativeEntry entry : directEntries) {
            emitDirectNativeFunction(w, entry, methodIdXorKey);
            w.println();
        }
    }

    static void emitDirectRegistrationHelpers(PrintWriter w,
                                              boolean directNativeRewrite,
                                              List<VmBridgeGenerator.DirectClassRegistration> directClassRegistrations) {
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
                VmBridgeGenerator.DirectClassRegistration reg = directClassRegistrations.get(i);
                w.println("static JNINativeMethod class_natives_" + i + "[] = {");
                for (int j = 0; j < reg.entries.size(); j++) {
                    VmBridgeGenerator.DirectNativeEntry entry = reg.entries.get(j);
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
                VmBridgeGenerator.DirectClassRegistration reg = directClassRegistrations.get(i);
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

    private static void emitDirectNativeFunction(PrintWriter w,
                                                 VmBridgeGenerator.DirectNativeEntry entry,
                                                 int methodIdXorKey) {
        StringBuilder sig = new StringBuilder();
        sig.append("static ")
                .append(VmBridgeDirectSupport.jniTypeForReturnDescriptor(entry.returnDescriptor))
                .append(" JNICALL ")
                .append(entry.nativeFunctionName)
                .append("(JNIEnv* env, ")
                .append(entry.isStatic ? "jclass cls" : "jobject receiver");

        for (int i = 0; i < entry.paramDescriptors.size(); i++) {
            sig.append(", ")
                    .append(VmBridgeDirectSupport.jniTypeForParamDescriptor(entry.paramDescriptors.get(i)))
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
            w.println("    " + VmBridgeDirectSupport.assignmentCode(slot, desc, valueName));
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
            w.println("    if (r.returnType == 'X') return " + VmBridgeDirectSupport.defaultReturnLiteral(entry.returnDescriptor) + ";");
            w.println("    return " + VmBridgeDirectSupport.returnValueExpr(entry.returnDescriptor) + ";");
        } else {
            w.println("    (void)r;");
            w.println("    return;");
        }
        w.println("}");
    }
}
