package com.alphaautoleak.jnvm.config;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ProtectConfig {

    private File inputJar;
    private File outputJar;
    private List<String> protectRules = new ArrayList<>();
    private File configFile;       // protect.conf
    private List<String> targets = new ArrayList<>();
    private boolean antiDebug = true;
    private File nativeDir;

    private boolean debug = false;

    public boolean isDebug() { return debug; }
    public void setDebug(boolean debug) { this.debug = debug; }


    /**
     * 校验 + 合并 configFile 中的规则
     */
    public void validate() throws IOException {
        if (inputJar == null || !inputJar.exists()) {
            throw new IllegalArgumentException("Input JAR not found: " + inputJar);
        }

        // 如果指定了 protect.conf，读取并合并规则
        if (configFile != null && configFile.exists()) {
            System.out.println("[INFO] Loading protect rules from: " + configFile);
            try (BufferedReader br = new BufferedReader(new FileReader(configFile))) {
                String line;
                while ((line = br.readLine()) != null) {
                    line = line.trim();
                    // 跳过空行和注释
                    if (line.isEmpty() || line.startsWith("#") || line.startsWith("//")) {
                        continue;
                    }
                    protectRules.add(line);
                }
            }
        }

        // 如果没有任何规则，默认保护全部
        if (protectRules.isEmpty()) {
            System.out.println("[WARN] No protect rules specified, protecting ALL methods.");
            protectRules.add("**");
        }

        // 确保 native 目录存在
        if (nativeDir != null && !nativeDir.exists()) {
            nativeDir.mkdirs();
        }
    }

    /**
     * 判断一个类/方法是否应该被保护
     * @param className  内部格式 e.g. "com/example/service/UserService"
     * @param methodName 方法名 e.g. "getUser"，null 表示检查整个类
     */
    public boolean shouldProtect(String className, String methodName) {
        // 转为点分格式
        String dotClass = className.replace('/', '.');

        for (String rule : protectRules) {
            // 规则1: 全部保护
            if (rule.equals("**")) {
                return true;
            }

            // 规则2: com.example.** (包通配)
            if (rule.endsWith(".**")) {
                String pkg = rule.substring(0, rule.length() - 3);
                if (dotClass.startsWith(pkg)) {
                    return true;
                }
            }

            // 规则3: com.example.MyClass (整个类)
            else if (!rule.contains("#") && !rule.startsWith("@")) {
                if (dotClass.equals(rule)) {
                    return true;
                }
            }

            // 规则4: com.example.MyClass#methodName (特定方法)
            else if (rule.contains("#")) {
                String[] parts = rule.split("#", 2);
                if (dotClass.equals(parts[0]) && methodName != null && methodName.equals(parts[1])) {
                    return true;
                }
            }

            // 规则5: @VMProtect (注解，后续步骤处理)
            else if (rule.startsWith("@")) {
                // 注解匹配在 ASM visitor 中处理
            }
        }

        return false;
    }

    /**
     * 检查规则中是否包含注解规则
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
}