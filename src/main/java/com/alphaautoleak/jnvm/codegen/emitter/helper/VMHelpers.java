package com.alphaautoleak.jnvm.codegen.emitter.helper;

import java.util.ArrayList;
import java.util.List;

/**
 * VM 辅助函数注册表
 */
public class VMHelpers {
    
    private final List<VMHelper> helpers = new ArrayList<>();
    
    public VMHelpers(boolean encryptStrings) {
        // 注册所有辅助函数
        helpers.add(new StringHelper(encryptStrings));
        helpers.add(new MetaHelper());
        helpers.add(new MethodDescHelper());
        helpers.add(new UnboxHelper());
        helpers.add(new InvokeDynamicHelper());
        helpers.add(new ExceptionHelper());
        // BoxHelper 已移除 - 使用类型特化的执行函数替代装箱
    }
    
    public List<VMHelper> getAllHelpers() {
        return helpers;
    }
}
