package com.alphaautoleak.jnvm.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProtectConfigBootstrapTest {

    @TempDir
    Path tempDir;

    @Test
    void loadAppliesDefaultsAndDerivesOutputJar() throws IOException {
        Path inputJar = Files.createFile(tempDir.resolve("app.jar"));
        Path configFile = tempDir.resolve("config.yml");

        Files.write(
                configFile,
                Collections.singletonList(
                        "jar: " + toYamlPath(inputJar) + "\n" +
                                "protect:\n" +
                                "  - sample.**\n"
                ),
                StandardCharsets.UTF_8
        );

        ProtectConfig config = new ProtectConfigBootstrap().load(configFile.toString());

        assertEquals(inputJar.toFile().getAbsoluteFile(), config.getInputJar().getAbsoluteFile());
        assertEquals(tempDir.resolve("app-obf.jar").toFile().getAbsoluteFile(), config.getOutputJar().getAbsoluteFile());
        assertEquals("native", config.getNativeDir().getPath());
        assertEquals(Collections.singletonList("sample.**"), config.getProtectRules());
        assertFalse(config.getTargets().isEmpty());
    }

    @Test
    void loadAddsFallbackProtectRuleAndPreservesExplicitSettings() throws IOException {
        Path inputJar = Files.createFile(tempDir.resolve("demo.jar"));
        Path outputJar = tempDir.resolve("custom-output.jar");
        Path nativeDir = tempDir.resolve("generated-native");
        Path configFile = tempDir.resolve("config.yml");

        String yaml = "jar: " + toYamlPath(inputJar) + "\n"
                + "out: " + toYamlPath(outputJar) + "\n"
                + "native-dir: " + toYamlPath(nativeDir) + "\n"
                + "debug: true\n"
                + "direct-native-rewrite: true\n";
        Files.write(configFile, Collections.singletonList(yaml), StandardCharsets.UTF_8);

        ProtectConfig config = new ProtectConfigBootstrap().load(configFile.toString());

        assertEquals(outputJar.toFile().getAbsoluteFile(), config.getOutputJar().getAbsoluteFile());
        assertEquals(nativeDir.toFile().getAbsoluteFile(), config.getNativeDir().getAbsoluteFile());
        assertEquals(Collections.singletonList("**"), config.getProtectRules());
        assertTrue(config.isDebug());
        assertTrue(config.isDirectNativeRewrite());
        assertTrue(Files.isDirectory(nativeDir));
    }

    private String toYamlPath(Path path) {
        return path.toAbsolutePath().toString().replace('\\', '/');
    }
}
