package com.alphaautoleak.jnvm.config;

import org.yaml.snakeyaml.Yaml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ProtectConfig {

    private File inputJar;
    private File outputJar;
    private List<String> protectRules = new ArrayList<>();
    private List<String> excludeRules = new ArrayList<>();
    private File configFile;       // protect.conf or config.yml
    private List<String> targets = new ArrayList<>();
    private boolean antiDebug = true;
    private File nativeDir;
    private boolean encryptStrings = true;

    private boolean debug = false;

    public boolean isDebug() { return debug; }
    public void setDebug(boolean debug) { this.debug = debug; }

    /**
     * Loads config from YAML file
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

            // jar
            if (config.containsKey("jar")) {
                String jarPath = (String) config.get("jar");
                if (inputJar == null) {
                    inputJar = new File(jarPath);
                }
            }

            // out
            if (config.containsKey("out")) {
                String outPath = (String) config.get("out");
                if (outputJar == null) {
                    outputJar = new File(outPath);
                }
            }

            // protect (List<String>)
            if (config.containsKey("protect")) {
                Object protectObj = config.get("protect");
                if (protectObj instanceof List) {
                    List<String> yamlRules = (List<String>) protectObj;
                    for (String rule : yamlRules) {
                        if (!protectRules.contains(rule)) {
                            protectRules.add(rule);
                        }
                    }
                }
            }

            // exclude (List<String>)
            if (config.containsKey("exclude")) {
                Object excludeObj = config.get("exclude");
                if (excludeObj instanceof List) {
                    List<String> yamlExcludes = (List<String>) excludeObj;
                    for (String rule : yamlExcludes) {
                        if (!excludeRules.contains(rule)) {
                            excludeRules.add(rule);
                        }
                    }
                }
            }

            // targets (List<String>)
            if (config.containsKey("targets")) {
                Object targetsObj = config.get("targets");
                if (targetsObj instanceof List) {
                    List<String> yamlTargets = (List<String>) targetsObj;
                    if (targets.isEmpty()) {
                        targets.addAll(yamlTargets);
                    }
                }
            }

            // anti-debug
            if (config.containsKey("anti-debug")) {
                antiDebug = Boolean.TRUE.equals(config.get("anti-debug"));
            }

            // encrypt-strings
            if (config.containsKey("encrypt-strings")) {
                encryptStrings = Boolean.TRUE.equals(config.get("encrypt-strings"));
            }

            // debug
            if (config.containsKey("debug")) {
                debug = Boolean.TRUE.equals(config.get("debug"));
            }

            // native-dir
            if (config.containsKey("native-dir")) {
                String nativeDirPath = (String) config.get("native-dir");
                if (nativeDir == null) {
                    nativeDir = new File(nativeDirPath);
                }
            }
        }
    }

    /**
     * Validates and merges rules from configFile
     */
    public void validate() throws IOException {
        // If config file specified, load it first
        if (configFile != null && configFile.exists()) {
            String fileName = configFile.getName().toLowerCase();
            if (fileName.endsWith(".yml") || fileName.endsWith(".yaml")) {
                loadFromYaml(configFile);
            } else {
                // Old format: read rules line by line
                System.out.println("[INFO] Loading protect rules from: " + configFile);
                try (BufferedReader br = new BufferedReader(new FileReader(configFile))) {
                    String line;
                    while ((line = br.readLine()) != null) {
                        line = line.trim();
                        // Skip empty lines and comments
                        if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) {
                            continue;
                        }
                        protectRules.add(line);
                    }
                }
            }
        }

        if (inputJar == null || !inputJar.exists()) {
            throw new IllegalArgumentException("Input JAR not found: " + inputJar);
        }

        if (protectRules.isEmpty()) {
            System.out.println("[WARN] No protect rules specified, protecting ALL methods.");
            protectRules.add("**");
        }

        // Ensure native directory exists
        if (nativeDir != null && !nativeDir.exists()) {
            nativeDir.mkdirs();
        }
    }

    /**
     * Determines if a class/method should be protected
     * @param className  internal format e.g. "com/example/service/UserService"
     * @param methodName method name e.g. "getUser", null means check entire class
     */
    public boolean shouldProtect(String className, String methodName) {
        String dotClass = className.replace('/', '.');

        if (matchesAnyRule(excludeRules, dotClass, methodName)) {
            return false;
        }
        return matchesAnyRule(protectRules, dotClass, methodName);
    }

    private boolean matchesAnyRule(List<String> rules, String dotClass, String methodName) {
        for (String rule : rules) {
            if (rule.equals("**")) {
                return true;
            }
            if (rule.endsWith(".**")) {
                String pkg = rule.substring(0, rule.length() - 3);
                if (dotClass.startsWith(pkg)) {
                    return true;
                }
            } else if (rule.contains("#")) {
                String[] parts = rule.split("#", 2);
                if (dotClass.equals(parts[0]) && methodName != null && methodName.equals(parts[1])) {
                    return true;
                }
            } else if (!rule.startsWith("@")) {
                if (dotClass.equals(rule)) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Checks if rules contain annotation rules
     */
    public List<String> getAnnotationRules() {
        List<String> result = new ArrayList<>();
        for (String rule : protectRules) {
            if (rule.startsWith("@")) {
                // @com.example.VMProtect → Lcom/example/VMProtect;
                String desc = "L" + rule.substring(1).replace('.', '/') + ";";
                result.add(desc);
            }
        }
        return result;
    }

    // ===== Getters / Setters =====

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
}