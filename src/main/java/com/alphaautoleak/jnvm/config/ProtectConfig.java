package com.alphaautoleak.jnvm.config;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class ProtectConfig {

    private File inputJar;
    private File outputJar;
    private List<String> protectRules = new ArrayList<>();
    private List<String> excludeRules = new ArrayList<>();
    private File configFile; // protect.conf or config.yml
    private List<String> targets = new ArrayList<>();
    private File nativeDir;
    private boolean encryptStrings = true;
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
     * Validates the current config state.
     */
    public void validate() {
        new ProtectConfigValidator().validate(this);
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

    public boolean isDirectNativeRewrite() {
        return directNativeRewrite;
    }

    public void setDirectNativeRewrite(boolean directNativeRewrite) {
        this.directNativeRewrite = directNativeRewrite;
    }
}
