package com.alphaautoleak.jnvm.codegen.emitter.helper;

import java.util.ArrayList;
import java.util.List;

/**
 * VM helper function registry
 */
public class VMHelpers {
    
    private final List<VMHelper> helpers = new ArrayList<>();
    
    public VMHelpers(boolean encryptStrings) {
        // Register all helper functions
        helpers.add(new StringHelper(encryptStrings));
        helpers.add(new MetaHelper());
        // MethodDescHelper removed - using pre-parsed argTypes instead of runtime parsing
        helpers.add(new UnboxHelper());
        helpers.add(new InvokeDynamicHelper());
        helpers.add(new ExceptionHelper());
        // BoxHelper removed - using type-specialized execution functions instead of boxing
    }
    
    public List<VMHelper> getAllHelpers() {
        return helpers;
    }
}
