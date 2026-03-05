package com.alphaautoleak.jnvm.codegen.emitter.helper;

import java.io.PrintWriter;

/**
 * VM 辅助函数生成器基类
 */
public abstract class VMHelper {
    
    /**
     * 生成函数声明到头文件
     */
    public abstract void generateHeader(PrintWriter w);
    
    /**
     * 生成函数实现到源文件
     */
    public abstract void generateSource(PrintWriter w);
    
    /**
     * 获取需要的头文件
     */
    public abstract String[] getIncludes();
    
    /**
     * 辅助方法：打印多行代码
     */
    protected void emit(PrintWriter w, String... lines) {
        for (String line : lines) {
            w.println(line);
        }
    }
}
