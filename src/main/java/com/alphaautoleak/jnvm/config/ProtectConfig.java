package com.alphaautoleak.jnvm.config;

import org.yaml.snakeyaml.Yaml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProtectConfig {

    private File inputJar;
    private File outputJar;
    private List<String> protectRules = new ArrayList<>();
    private List<String> excludeRules = new ArrayList<>();
    private File configFile; // protect.conf or config.yml
    private List<String> targets = new ArrayList<>();
    private boolean antiDebug = true;
    private File nativeDir;
    private boolean encryptStrings = true;
    /**
     * When false (default), all methods in bootstrap owner classes are skipped for safety.
     * When true, only bootstrap-sensitive closure is skipped, and non-sensitive payload methods
     * in those classes can still be protected.
     */
    private boolean protectBootstrapPayload = false;
    /**
     * When true, protected non-<clinit> methods are rewritten to ACC_NATIVE and
     * registered per-class in that class's <clinit>.
     */
    private boolean directNativeRewrite = false;
    private boolean debug = false;

    public boolean isDebug() {
        return debug;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    /**
     * Loads config from YAML file.
     */
    @SuppressWarnings("unchecked")
    public void loadFromYaml(File yamlFile) throws IOException {
        if (yamlFile == null || !yamlFile.exists()) {
            return;
        }

        System.out.println("[INFO] Loading config from YAML: " + yamlFile);
        Yaml yaml = new Yaml();
        try (FileInputStream fis = new FileInputStream(yamlFile)) {
            Map<String, Object> config = yaml.load(fis);
            if (config == null) {
                return;
            }

            loadJarPath(config);
            loadOutputPath(config);
            mergeStringList(config, "protect", protectRules, false);
            mergeStringList(config, "exclude", excludeRules, false);
            mergeStringList(config, "targets", targets, true);

            encryptStrings = readBoolean(config, "encrypt-strings", encryptStrings);
            debug = readBoolean(config, "debug", debug);
            protectBootstrapPayload = readBoolean(config, "protect-bootstrap-payload", protectBootstrapPayload);
            directNativeRewrite = readBoolean(config, "direct-native-rewrite", directNativeRewrite);

            if (config.containsKey("native-dir") && nativeDir == null) {
                nativeDir = new File((String) config.get("native-dir"));
            }
        }
    }

    /**
     * Validates and merges rules from configFile.
     */
    public void validate() throws IOException {
        if (configFile != null && configFile.exists()) {
            String fileName = configFile.getName().toLowerCase();
            if (fileName.endsWith(".yml") || fileName.endsWith(".yaml")) {
                loadFromYaml(configFile);
            } else {
                loadRulesFromTextConfig(configFile);
            }
        }

        if (inputJar == null || !inputJar.exists()) {
            throw new IllegalArgumentException("Input JAR not found: " + inputJar);
        }

        if (protectRules.isEmpty()) {
            System.out.println("[WARN] No protect rules specified, protecting ALL methods.");
            protectRules.add("**");
        }

        if (nativeDir != null && !nativeDir.exists()) {
            nativeDir.mkdirs();
        }
    }

    /**
     * Determines if a class/method should be protected.
     *
     * @param className internal format e.g. "com/example/service/UserService"
     * @param methodName method name e.g. "getUser", null means check entire class
     */
    public boolean shouldProtect(String className, String methodName) {
        String dotClass = className.replace('/', '.');
        if (ProtectRuleMatcher.matchesAnyRule(excludeRules, dotClass, methodName)) {
            return false;
        }
        return ProtectRuleMatcher.matchesAnyRule(protectRules, dotClass, methodName);
    }

    /**
     * Checks if rules contain annotation rules.
     */
    public List<String> getAnnotationRules() {
        return ProtectRuleMatcher.toAnnotationDescriptors(protectRules);
    }

    @SuppressWarnings("unchecked")
    private static void mergeStringList(Map<String, Object> config,
                                        String key,
                                        List<String> target,
                                        boolean onlyWhenTargetEmpty) {
        if (!config.containsKey(key)) {
            return;
        }
        if (onlyWhenTargetEmpty && !target.isEmpty()) {
            return;
        }

        Object value = config.get(key);
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

    private static boolean readBoolean(Map<String, Object> config, String key, boolean defaultValue) {
        if (!config.containsKey(key)) {
            return defaultValue;
        }
        return Boolean.TRUE.equals(config.get(key));
    }

    private void loadJarPath(Map<String, Object> config) {
        if (!config.containsKey("jar") || inputJar != null) {
            return;
        }
        inputJar = new File((String) config.get("jar"));
    }

    private void loadOutputPath(Map<String, Object> config) {
        if (!config.containsKey("out") || outputJar != null) {
            return;
        }
        outputJar = new File((String) config.get("out"));
    }

    private void loadRulesFromTextConfig(File plainConfig) throws IOException {
        System.out.println("[INFO] Loading protect rules from: " + plainConfig);
        try (BufferedReader br = new BufferedReader(new FileReader(plainConfig))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) {
                    continue;
                }
                protectRules.add(line);
            }
        }
    }

    public File getInputJar() {
        return inputJar;
    }

    public void setInputJar(File inputJar) {
        this.inputJar = inputJar;
    }

    public File getOutputJar() {
        return outputJar;
    }

    public void setOutputJar(File outputJar) {
        this.outputJar = outputJar;
    }

    public List<String> getProtectRules() {
        return protectRules;
    }

    public void setProtectRules(List<String> protectRules) {
        this.protectRules = protectRules;
    }

    public List<String> getExcludeRules() {
        return excludeRules;
    }

    public void setExcludeRules(List<String> excludeRules) {
        this.excludeRules = excludeRules;
    }

    public File getConfigFile() {
        return configFile;
    }

    public void setConfigFile(File configFile) {
        this.configFile = configFile;
    }

    public List<String> getTargets() {
        return targets;
    }

    public void setTargets(List<String> targets) {
        this.targets = targets;
    }

    public boolean isAntiDebug() {
        return antiDebug;
    }

    public void setAntiDebug(boolean antiDebug) {
        this.antiDebug = antiDebug;
    }

    public File getNativeDir() {
        return nativeDir;
    }

    public void setNativeDir(File nativeDir) {
        this.nativeDir = nativeDir;
    }

    public boolean isEncryptStrings() {
        return encryptStrings;
    }

    public void setEncryptStrings(boolean encryptStrings) {
        this.encryptStrings = encryptStrings;
    }

    public boolean isProtectBootstrapPayload() {
        return protectBootstrapPayload;
    }

    public void setProtectBootstrapPayload(boolean protectBootstrapPayload) {
        this.protectBootstrapPayload = protectBootstrapPayload;
    }

    public boolean isDirectNativeRewrite() {
        return directNativeRewrite;
    }

    public void setDirectNativeRewrite(boolean directNativeRewrite) {
        this.directNativeRewrite = directNativeRewrite;
    }
}
