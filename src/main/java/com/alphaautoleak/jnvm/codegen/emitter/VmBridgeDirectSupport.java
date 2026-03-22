package com.alphaautoleak.jnvm.codegen.emitter;

import com.alphaautoleak.jnvm.utils.BridgeFastPathUtil;

final class VmBridgeDirectSupport {

    private VmBridgeDirectSupport() {
    }

    static String assignmentCode(int slot, String descriptor, String valueName) {
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

    static String jniTypeForParamDescriptor(String descriptor) {
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

    static String jniTypeForReturnDescriptor(String descriptor) {
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

    static String returnValueExpr(String descriptor) {
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

    static String defaultReturnLiteral(String descriptor) {
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
}
