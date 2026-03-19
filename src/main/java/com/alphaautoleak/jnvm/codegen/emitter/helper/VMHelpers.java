package com.alphaautoleak.jnvm.codegen.emitter.helper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * VM helper function registry
 */
public class VMHelpers {
    
    private final List<VMHelper> helpers = new ArrayList<>();
    
    public VMHelpers(boolean encryptStrings) {
        registerCoreHelpers(encryptStrings);
        registerInvokeHelpers();
    }
    
    public List<VMHelper> getAllHelpers() {
        return Collections.unmodifiableList(helpers);
    }

    private void registerCoreHelpers(boolean encryptStrings) {
        helpers.add(new StringHelper(encryptStrings));
        helpers.add(new MetaHelper());
        helpers.add(new UnboxHelper());
    }

    private void registerInvokeHelpers() {
        helpers.add(new InvokeDynamicHelper());
        helpers.add(new ExceptionHelper());
    }
}
