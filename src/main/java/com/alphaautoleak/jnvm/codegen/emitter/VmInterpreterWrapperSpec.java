package com.alphaautoleak.jnvm.codegen.emitter;

final class VmInterpreterWrapperSpec {

    final String returnType;
    final String functionName;
    final String returnStatement;

    VmInterpreterWrapperSpec(String returnType, String functionName, String returnStatement) {
        this.returnType = returnType;
        this.functionName = functionName;
        this.returnStatement = returnStatement;
    }
}
