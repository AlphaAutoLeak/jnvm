package com.alphaautoleak.jnvm.asm;

import java.util.ArrayList;
import java.util.List;

/**
 * BootstrapMethods 属性中的一条记录。
 * invokedynamic 指令引用此表。
 */
public class BootstrapEntry {

    /** bootstrap method handle 的 tag (6 = invokestatic, etc.) */
    private int handleTag;

    /** bootstrap method 所属类 */
    private String handleOwner;

    /** bootstrap method 名称 */
    private String handleName;

    /** bootstrap method 描述符 */
    private String handleDescriptor;

    /** bootstrap 参数（静态参数） */
    private List<Object> arguments = new ArrayList<>();

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

    @Override
    public String toString() {
        return String.format("Bootstrap{tag=%d, %s.%s%s, args=%d}",
                handleTag, handleOwner, handleName, handleDescriptor, arguments.size());
    }
}