package com.alphaautoleak.jnvm.compiler;

import java.io.File;

final class ZigInstallation {

    private final File homeDirectory;
    private final File executable;
    private final String version;

    ZigInstallation(File homeDirectory, File executable, String version) {
        this.homeDirectory = homeDirectory;
        this.executable = executable;
        this.version = version;
    }

    File getHomeDirectory() {
        return homeDirectory;
    }

    File getExecutable() {
        return executable;
    }

    String getVersion() {
        return version;
    }
}
