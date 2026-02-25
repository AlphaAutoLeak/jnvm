package com.alphaautoleak.jnvm.asm;

/**
 * 自定义常量池条目。
 * 每个被保护方法拥有独立的常量池。
 * 运行时在 C 端预解析为 jclass / jmethodID / jfieldID。
 */
public class CPEntry {

    public enum Type {
        /** 整数常量 (ldc int) */
        INTEGER,
        /** 长整数常量 (ldc2_w long) */
        LONG,
        /** 浮点常量 (ldc float) */
        FLOAT,
        /** 双精度常量 (ldc2_w double) */
        DOUBLE,
        /** 字符串常量 (ldc String) */
        STRING,
        /** 类引用 (new, checkcast, instanceof, anewarray) */
        CLASS,
        /** 方法引用 (invokevirtual, invokespecial, invokestatic) */
        METHOD_REF,
        /** 接口方法引用 (invokeinterface) */
        INTERFACE_METHOD_REF,
        /** 字段引用 (getfield, putfield, getstatic, putstatic) */
        FIELD_REF,
        /** invokedynamic 引用 */
        INVOKE_DYNAMIC,
        /** MethodType (for invokedynamic) */
        METHOD_TYPE,
        /** MethodHandle (for bootstrap methods) */
        METHOD_HANDLE
    }

    /** 常量池内索引 */
    private int index;

    /** 条目类型 */
    private Type type;

    // ===== 根据 type 使用不同字段 =====

    /** INTEGER */
    private int intValue;

    /** LONG */
    private long longValue;

    /** FLOAT */
    private float floatValue;

    /** DOUBLE */
    private double doubleValue;

    /** STRING */
    private String stringValue;

    /** CLASS: 类内部名 */
    private String className;

    /** METHOD_REF / INTERFACE_METHOD_REF: 所属类 */
    private String refOwner;

    /** METHOD_REF / INTERFACE_METHOD_REF / FIELD_REF: 名称 */
    private String refName;

    /** METHOD_REF / INTERFACE_METHOD_REF / FIELD_REF: 描述符 */
    private String refDescriptor;

    /** INVOKE_DYNAMIC: bootstrap method 索引 */
    private int bootstrapMethodIndex;

    /** INVOKE_DYNAMIC: 调用名称 */
    private String dynamicName;

    /** INVOKE_DYNAMIC: 调用描述符 */
    private String dynamicDescriptor;

    /** METHOD_HANDLE: handle tag (1~9) */
    private int handleTag;

    /** METHOD_HANDLE: handle owner */
    private String handleOwner;

    /** METHOD_HANDLE: handle name */
    private String handleName;

    /** METHOD_HANDLE: handle descriptor */
    private String handleDescriptor;

    public CPEntry() {}

    // ===== 工厂方法 =====

    public static CPEntry ofInt(int index, int value) {
        CPEntry e = new CPEntry();
        e.index = index;
        e.type = Type.INTEGER;
        e.intValue = value;
        return e;
    }

    public static CPEntry ofLong(int index, long value) {
        CPEntry e = new CPEntry();
        e.index = index;
        e.type = Type.LONG;
        e.longValue = value;
        return e;
    }

    public static CPEntry ofFloat(int index, float value) {
        CPEntry e = new CPEntry();
        e.index = index;
        e.type = Type.FLOAT;
        e.floatValue = value;
        return e;
    }

    public static CPEntry ofDouble(int index, double value) {
        CPEntry e = new CPEntry();
        e.index = index;
        e.type = Type.DOUBLE;
        e.doubleValue = value;
        return e;
    }

    public static CPEntry ofString(int index, String value) {
        CPEntry e = new CPEntry();
        e.index = index;
        e.type = Type.STRING;
        e.stringValue = value;
        return e;
    }

    public static CPEntry ofClass(int index, String className) {
        CPEntry e = new CPEntry();
        e.index = index;
        e.type = Type.CLASS;
        e.className = className;
        return e;
    }

    public static CPEntry ofMethodRef(int index, String owner, String name, String desc) {
        CPEntry e = new CPEntry();
        e.index = index;
        e.type = Type.METHOD_REF;
        e.refOwner = owner;
        e.refName = name;
        e.refDescriptor = desc;
        return e;
    }

    public static CPEntry ofInterfaceMethodRef(int index, String owner, String name, String desc) {
        CPEntry e = new CPEntry();
        e.index = index;
        e.type = Type.INTERFACE_METHOD_REF;
        e.refOwner = owner;
        e.refName = name;
        e.refDescriptor = desc;
        return e;
    }

    public static CPEntry ofFieldRef(int index, String owner, String name, String desc) {
        CPEntry e = new CPEntry();
        e.index = index;
        e.type = Type.FIELD_REF;
        e.refOwner = owner;
        e.refName = name;
        e.refDescriptor = desc;
        return e;
    }

    public static CPEntry ofInvokeDynamic(int index, int bsmIndex, String name, String desc) {
        CPEntry e = new CPEntry();
        e.index = index;
        e.type = Type.INVOKE_DYNAMIC;
        e.bootstrapMethodIndex = bsmIndex;
        e.dynamicName = name;
        e.dynamicDescriptor = desc;
        return e;
    }

    public static CPEntry ofMethodHandle(int index, int tag, String owner, String name, String desc) {
        CPEntry e = new CPEntry();
        e.index = index;
        e.type = Type.METHOD_HANDLE;
        e.handleTag = tag;
        e.handleOwner = owner;
        e.handleName = name;
        e.handleDescriptor = desc;
        return e;
    }

    // ===== Getters =====

    public int getIndex() { return index; }
    public Type getType() { return type; }
    public int getIntValue() { return intValue; }
    public long getLongValue() { return longValue; }
    public float getFloatValue() { return floatValue; }
    public double getDoubleValue() { return doubleValue; }
    public String getStringValue() { return stringValue; }
    public String getClassName() { return className; }
    public String getRefOwner() { return refOwner; }
    public String getRefName() { return refName; }
    public String getRefDescriptor() { return refDescriptor; }
    public int getBootstrapMethodIndex() { return bootstrapMethodIndex; }
    public String getDynamicName() { return dynamicName; }
    public String getDynamicDescriptor() { return dynamicDescriptor; }
    public int getHandleTag() { return handleTag; }
    public String getHandleOwner() { return handleOwner; }
    public String getHandleName() { return handleName; }
    public String getHandleDescriptor() { return handleDescriptor; }

    public void setIndex(int index) { this.index = index; }

    @Override
    public String toString() {
        switch (type) {
            case INTEGER: return "CP[" + index + "] INT=" + intValue;
            case LONG: return "CP[" + index + "] LONG=" + longValue;
            case FLOAT: return "CP[" + index + "] FLOAT=" + floatValue;
            case DOUBLE: return "CP[" + index + "] DOUBLE=" + doubleValue;
            case STRING: return "CP[" + index + "] STRING=\"" + stringValue + "\"";
            case CLASS: return "CP[" + index + "] CLASS=" + className;
            case METHOD_REF: return "CP[" + index + "] METHOD=" + refOwner + "." + refName + refDescriptor;
            case INTERFACE_METHOD_REF: return "CP[" + index + "] IMETHOD=" + refOwner + "." + refName + refDescriptor;
            case FIELD_REF: return "CP[" + index + "] FIELD=" + refOwner + "." + refName + ":" + refDescriptor;
            case INVOKE_DYNAMIC: return "CP[" + index + "] INDY bsm=" + bootstrapMethodIndex + " " + dynamicName + dynamicDescriptor;
            case METHOD_HANDLE: return "CP[" + index + "] HANDLE tag=" + handleTag + " " + handleOwner + "." + handleName;
            default: return "CP[" + index + "] UNKNOWN";
        }
    }
}