package com.alphaautoleak.jnvm.codegen;

import java.io.IOException;

interface NativeGenerationStep {

    String getOutputLabel();

    void generate() throws IOException;
}
