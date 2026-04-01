package com.alphaautoleak.jnvm.config;

import com.alphaautoleak.jnvm.cli.CliReporter;
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;

final class ProtectConfigLoader {

    void load(ProtectConfig config) throws IOException {
        File configFile = config.getConfigFile();
        if (configFile == null || !configFile.exists()) {
            return;
        }

        String fileName = configFile.getName().toLowerCase();
        if (fileName.endsWith(".yml") || fileName.endsWith(".yaml")) {
            loadFromYaml(config, configFile);
            return;
        }
        loadRulesFromTextConfig(config, configFile);
    }

    private void loadFromYaml(ProtectConfig config, File yamlFile) throws IOException {
        CliReporter.info("Loading config from YAML: " + yamlFile);
        Yaml yaml = new Yaml();
        try (FileInputStream fis = new FileInputStream(yamlFile)) {
            Map<String, Object> values = yaml.load(fis);
            if (values == null) {
                return;
            }

            loadJarPath(config, values);
            loadOutputPath(config, values);
            mergeStringList(values, "protect", config.getProtectRules(), false);
            mergeStringList(values, "exclude", config.getExcludeRules(), false);
            mergeStringList(values, "targets", config.getTargets(), true);

            config.setEncryptStrings(readBoolean(values, "encrypt-strings", config.isEncryptStrings()));
            config.setDebug(readBoolean(values, "debug", config.isDebug()));
            config.setDirectNativeRewrite(readBoolean(
                    values,
                    "direct-native-rewrite",
                    config.isDirectNativeRewrite()));

            if (values.containsKey("native-dir") && config.getNativeDir() == null) {
                config.setNativeDir(resolveConfiguredFile(config, (String) values.get("native-dir")));
            }
            if (values.containsKey("zig-exe") && config.getZigExecutable() == null) {
                config.setZigExecutable(resolveConfiguredFile(config, (String) values.get("zig-exe")));
            }
        }
    }

    private void loadJarPath(ProtectConfig config, Map<String, Object> values) {
        if (!values.containsKey("jar") || config.getInputJar() != null) {
            return;
        }
        config.setInputJar(resolveConfiguredFile(config, (String) values.get("jar")));
    }

    private void loadOutputPath(ProtectConfig config, Map<String, Object> values) {
        if (!values.containsKey("out") || config.getOutputJar() != null) {
            return;
        }
        config.setOutputJar(resolveConfiguredFile(config, (String) values.get("out")));
    }

    private File resolveConfiguredFile(ProtectConfig config, String path) {
        File configured = new File(path);
        if (configured.isAbsolute()) {
            return configured;
        }

        File configFile = config.getConfigFile();
        if (configFile == null) {
            return configured;
        }

        File configDir = configFile.getAbsoluteFile().getParentFile();
        if (configDir == null) {
            return configured;
        }
        return new File(configDir, path);
    }

    @SuppressWarnings("unchecked")
    private void mergeStringList(Map<String, Object> values,
                                 String key,
                                 List<String> target,
                                 boolean onlyWhenTargetEmpty) {
        if (!values.containsKey(key)) {
            return;
        }
        if (onlyWhenTargetEmpty && !target.isEmpty()) {
            return;
        }

        Object value = values.get(key);
        if (!(value instanceof List)) {
            return;
        }

        List<String> incoming = (List<String>) value;
        for (String item : incoming) {
            if (!target.contains(item)) {
                target.add(item);
            }
        }
    }

    private boolean readBoolean(Map<String, Object> values, String key, boolean defaultValue) {
        if (!values.containsKey(key)) {
            return defaultValue;
        }
        return Boolean.TRUE.equals(values.get(key));
    }

    private void loadRulesFromTextConfig(ProtectConfig config, File plainConfig) throws IOException {
        CliReporter.info("Loading protect rules from: " + plainConfig);
        try (BufferedReader br = new BufferedReader(new FileReader(plainConfig))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) {
                    continue;
                }
                config.getProtectRules().add(line);
            }
        }
    }
}
