package com.alphaautoleak.jnvm.codegen.emitter;

/**
 * Parses JVM method descriptors and extracts argument/return metadata used by VM codegen.
 */
final class MethodDescriptorParser {

    private MethodDescriptorParser() {
    }

    static final class DescriptorInfo {
        private final int argCount;
        private final char returnTypeChar;
        private final String argTypes;

        DescriptorInfo(int argCount, char returnTypeChar, String argTypes) {
            this.argCount = argCount;
            this.returnTypeChar = returnTypeChar;
            this.argTypes = argTypes;
        }

        int getArgCount() {
            return argCount;
        }

        char getReturnTypeChar() {
            return returnTypeChar;
        }

        String getArgTypes() {
            return argTypes;
        }
    }

    static DescriptorInfo parse(String desc) {
        if (desc == null || desc.isEmpty()) {
            return new DescriptorInfo(0, 'V', "");
        }

        int argCount = 0;
        StringBuilder argTypes = new StringBuilder();

        int i = 1; // skip '('
        while (i < desc.length() && desc.charAt(i) != ')') {
            char c = desc.charAt(i);
            if (c == 'L') {
                argTypes.append('L');
                while (i < desc.length() && desc.charAt(i) != ';') {
                    i++;
                }
                i++; // skip ';'
            } else if (c == '[') {
                argTypes.append('L');
                while (i < desc.length() && desc.charAt(i) == '[') {
                    i++;
                }
                if (i < desc.length() && desc.charAt(i) == 'L') {
                    while (i < desc.length() && desc.charAt(i) != ';') {
                        i++;
                    }
                    i++; // skip ';'
                } else {
                    i++; // skip primitive type char
                }
            } else {
                argTypes.append(c);
                i++;
            }
            argCount++;
        }

        char returnType = 'V';
        if (i < desc.length() && desc.charAt(i) == ')') {
            i++;
            if (i < desc.length()) {
                returnType = desc.charAt(i);
            }
        }

        return new DescriptorInfo(argCount, returnType, argTypes.toString());
    }

    static String parseArgTypes(String desc) {
        return parse(desc).getArgTypes();
    }
}
