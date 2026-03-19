package com.alphaautoleak.jnvm.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Centralized rule matching logic for protection include/exclude rules.
 */
final class ProtectRuleMatcher {

    private ProtectRuleMatcher() {
    }

    static boolean matchesAnyRule(List<String> rules, String dotClass, String methodName) {
        for (String rule : rules) {
            if (rule.equals("**")) {
                return true;
            }
            if (rule.endsWith(".**")) {
                String pkg = rule.substring(0, rule.length() - 3);
                if (dotClass.startsWith(pkg)) {
                    return true;
                }
                continue;
            }
            if (rule.contains("#")) {
                String[] parts = rule.split("#", 2);
                if (dotClass.equals(parts[0]) && methodName != null && methodName.equals(parts[1])) {
                    return true;
                }
                continue;
            }
            if (!rule.startsWith("@") && dotClass.equals(rule)) {
                return true;
            }
        }
        return false;
    }

    static List<String> toAnnotationDescriptors(List<String> rules) {
        List<String> result = new ArrayList<>();
        for (String rule : rules) {
            if (!rule.startsWith("@")) {
                continue;
            }
            result.add("L" + rule.substring(1).replace('.', '/') + ";");
        }
        return result;
    }
}
