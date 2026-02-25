package com.alphaautoleak.jnvm.asm;

import java.util.ArrayList;
import java.util.List;

public class BootstrapEntry {

    private int handleTag;
    private String handleOwner;
    private String handleName;
    private String handleDescriptor;

    /**
     * Bootstrap 参数 — 保持原始类型
     * 每个元素是以下之一：
     * - String (字符串常量或 recipe)
     * - Integer
     * - Long
     * - Float
     * - Double
     * - org.objectweb.asm.Type (MethodType 描述符)
     * - org.objectweb.asm.Handle (MethodHandle)
     */
    private List<Object> arguments = new ArrayList<>();

    /** 参数的类型标记 */
    public enum ArgType {
        STRING,      // 普通字符串
        INTEGER,     // int
        LONG,        // long
        FLOAT,       // float
        DOUBLE,      // double
        METHOD_TYPE, // MethodType 描述符字符串
        METHOD_HANDLE // MethodHandle 引用
    }

    private List<ArgType> argumentTypes = new ArrayList<>();

    public BootstrapEntry() {}

    public int getHandleTag() { return handleTag; }
    public void setHandleTag(int handleTag) { this.handleTag = handleTag; }

    public String getHandleOwner() { return handleOwner; }
    public void setHandleOwner(String handleOwner) { this.handleOwner = handleOwner; }

    public String getHandleName() { return handleName; }
    public void setHandleName(String handleName) { this.handleName = handleName; }

    public String getHandleDescriptor() { return handleDescriptor; }
    public void setHandleDescriptor(String handleDescriptor) { this.handleDescriptor = handleDescriptor; }

    public List<Object> getArguments() { return arguments; }
    public void setArguments(List<Object> arguments) { this.arguments = arguments; }

    public List<ArgType> getArgumentTypes() { return argumentTypes; }
    public void setArgumentTypes(List<ArgType> argumentTypes) { this.argumentTypes = argumentTypes; }

    @Override
    public String toString() {
        return String.format("Bootstrap{tag=%d, %s.%s%s, args=%d}",
                handleTag, handleOwner, handleName, handleDescriptor, arguments.size());
    }
}