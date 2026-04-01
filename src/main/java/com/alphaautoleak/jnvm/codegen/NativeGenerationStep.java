package com.alphaautoleak.jnvm.codegen;

import java.io.IOException;
import java.util.List;

interface NativeGenerationStep {

    List<String> getOutputFiles();

    default String getOutputLabel() {
        return String.join(" / ", getOutputFiles());
    }

    default int getGeneratedFileCount() {
        return getOutputFiles().size();
    }

    void generate() throws IOException;
}
