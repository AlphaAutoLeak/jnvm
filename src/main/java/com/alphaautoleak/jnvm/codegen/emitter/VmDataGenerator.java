package com.alphaautoleak.jnvm.codegen.emitter;

import com.alphaautoleak.jnvm.asm.*;
import com.alphaautoleak.jnvm.asm.BytecodeExtractor.MetaEntry;
import com.alphaautoleak.jnvm.asm.BytecodeExtractor.MetaType;
import com.alphaautoleak.jnvm.crypto.CryptoUtils;
import com.alphaautoleak.jnvm.crypto.EncryptedMethodData;
import com.alphaautoleak.jnvm.crypto.StringEncryptor;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * 生成 vm_data.h 和 vm_data.c - VM 数据（方法元数据、字符串池等）
 * 
 * 新格式：
 * - 字符串池：所有字符串使用 ChaCha20 加密存储
 * - 元数据数组：每条指令的操作数
 * - pcToMetaIdx：PC -> 元数据索引映射
 * - Bootstrap 方法表：全局共享
 */
public class VmDataGenerator {
    
    private final List<EncryptedMethodData> methods;
    private final byte[] stringKey;       // 方法字节码解密密钥 (8 bytes)
    private final byte[] vmStringKey;     // 字符串 ChaCha20 密钥 (32 bytes), 仅当 encryptStrings=true 时使用
    private final byte[] stringNonce;     // ChaCha20 nonce for strings (12 bytes), 仅当 encryptStrings=true 时使用
    private final boolean encryptStrings; // 是否加密字符串
    private final File dir;
    
    /** 全局字符串池：字符串 -> 全局索引 */
    private Map<String, Integer> globalStringIndexMap;
    
    /** 全局 Bootstrap 方法表 */
    private List<BootstrapEntry> globalBootstrapMethods = new ArrayList<>();
    private Map<String, Integer> bootstrapIndexMap = new HashMap<>();
    
    public VmDataGenerator(File dir, List<EncryptedMethodData> methods, byte[] stringKey, boolean encryptStrings) {
        this.dir = dir;
        this.methods = methods;
        this.stringKey = stringKey;           // 方法字节码密钥 (8 bytes)
        this.encryptStrings = encryptStrings;
        if (encryptStrings) {
            this.vmStringKey = CryptoUtils.generateKey();  // 字符串 ChaCha20 密钥 (32 bytes)
            this.stringNonce = CryptoUtils.generateNonce(); // 字符串 ChaCha20 nonce (12 bytes)
        } else {
            this.vmStringKey = null;
            this.stringNonce = null;
        }
    }
    
    public void generate() throws IOException {
        // 第一遍：收集所有 bootstrap 方法
        collectBootstrapMethods();
        
        generateHeader();
        generateSource();
    }
    
    /**
     * 收集所有方法的 bootstrap 方法到全局表
     */
    private void collectBootstrapMethods() {
        for (EncryptedMethodData method : methods) {
            List<BootstrapEntry> bsmList = method.getBootstrapMethods();
            if (bsmList == null) continue;
            
            for (BootstrapEntry bsm : bsmList) {
                // 包含 args 信息在 key 中，确保不同的 BSM 不会被合并
                StringBuilder keyBuilder = new StringBuilder();
                keyBuilder.append(bsm.getHandleOwner()).append(".");
                keyBuilder.append(bsm.getHandleName()).append(bsm.getHandleDescriptor());
                // 添加 args 信息
                if (bsm.getArguments() != null) {
                    for (Object arg : bsm.getArguments()) {
                        keyBuilder.append("|").append(arg != null ? arg.toString() : "null");
                    }
                }
                String key = keyBuilder.toString();
                
                if (!bootstrapIndexMap.containsKey(key)) {
                    bootstrapIndexMap.put(key, globalBootstrapMethods.size());
                    globalBootstrapMethods.add(bsm);
                }
            }
        }
        
        // 更新每个方法的 bsmIdx 到全局索引
        // 这需要在生成元数据时处理
    }
    
    private void generateHeader() throws IOException {
        try (PrintWriter w = new PrintWriter(new java.io.FileWriter(new File(dir, "vm_data.h")))) {
            w.println("#ifndef VM_DATA_H");
            w.println("#define VM_DATA_H");
            w.println("#include \"vm_types.h\"");
            w.println();
            w.println("extern const uint8_t vm_key[];");
            w.println("extern const int vm_method_count;");
            w.println("extern VMMethod vm_methods[];");
            w.println("extern VMString vm_strings[];");
            w.println("extern const int vm_string_count;");
            if (encryptStrings) {
                w.println("extern const uint8_t vm_string_key[];");
                w.println("extern const uint8_t vm_string_nonce[];");
            }
            w.println("extern VMBootstrapMethod vm_bootstrap_methods[];");
            w.println("extern const int vm_bootstrap_count;");
            w.println();
            w.println("#endif");
        }
    }
    
    private void generateSource() throws IOException {
        try (PrintWriter w = new PrintWriter(new java.io.FileWriter(new File(dir, "vm_data.c")))) {
            w.println("#include \"vm_data.h\"");
            w.println("#include \"chacha20.h\"");
            w.println();
            
            // 加密密钥 (8 bytes)
            w.println("const uint8_t vm_key[] = {");
            for (int i = 0; i < stringKey.length; i++) {
                w.printf("0x%02x%s", stringKey[i] & 0xFF, (i < stringKey.length - 1 ? ", " : ""));
            }
            w.println("\n};");
            w.println();
            
            // 全局字符串池
            Set<String> allStrings = new LinkedHashSet<>();
            for (EncryptedMethodData method : methods) {
                List<String> pool = method.getStringPool();
                if (pool != null) {
                    allStrings.addAll(pool);
                }
                // 添加方法描述符到字符串池
                if (method.getDescriptor() != null) {
                    allStrings.add(method.getDescriptor());
                }
                // 添加异常表中的 catch 类型到字符串池
                List<ExceptionEntry> excTable = method.getExceptionTable();
                if (excTable != null) {
                    for (ExceptionEntry e : excTable) {
                        if (e.getCatchType() != null) {
                            allStrings.add(e.getCatchType());
                        }
                    }
                }
            }
            
            // 添加 BSM 相关的字符串到全局池
            for (BootstrapEntry bsm : globalBootstrapMethods) {
                // 添加 bootstrap 方法自身的信息
                allStrings.add(bsm.getHandleOwner());
                allStrings.add(bsm.getHandleName());
                allStrings.add(bsm.getHandleDescriptor());
                
                // 添加 BSM 参数中的字符串
                List<Object> args = bsm.getArguments();
                List<BootstrapEntry.ArgType> argTypes = bsm.getArgumentTypes();
                if (args != null && argTypes != null) {
                    for (int j = 0; j < args.size(); j++) {
                        Object arg = args.get(j);
                        BootstrapEntry.ArgType argType = argTypes.get(j);
                        switch (argType) {
                            case STRING:
                            case METHOD_TYPE:
                                allStrings.add(arg.toString());
                                break;
                            case METHOD_HANDLE:
                                // 格式: "tag:owner:name:desc"
                                String[] parts = arg.toString().split(":");
                                if (parts.length >= 4) {
                                    // 存储完整的方法引用字符串 (owner.name + desc)
                                    allStrings.add(parts[1] + "." + parts[2] + parts[3]);
                                }
                                break;
                        }
                    }
                }
            }
            
            // 建立全局字符串索引映射
            globalStringIndexMap = new HashMap<>();
            int globalIdx = 0;
            for (String s : allStrings) {
                globalStringIndexMap.put(s, globalIdx++);
            }
            
            emitStringPool(w, allStrings);
            
            w.println("const int vm_method_count = " + methods.size() + ";");
            w.println("const int vm_string_count = " + allStrings.size() + ";");
            w.println("const int vm_bootstrap_count = " + globalBootstrapMethods.size() + ";");
            w.println();
            
            // 生成 Bootstrap 方法表
            emitBootstrapMethods(w);
            
            // 为每个方法生成数据
            for (EncryptedMethodData method : methods) {
                emitMethodData(w, method);
            }
            
            // 方法数组
            w.println("VMMethod vm_methods[] = {");
            for (EncryptedMethodData method : methods) {
                w.printf("    { .methodId=%d, .maxStack=%d, .maxLocals=%d, ",
                    method.getMethodId(), method.getMaxStack(), method.getMaxLocals());
                w.printf(".bytecode=(uint8_t*)m%d_bc, .bytecodeLen=%d, ",
                    method.getMethodId(), method.getEncryptedBytecode().length);
                w.printf(".metadata=m%d_meta, .metadataCount=%d, ",
                    method.getMethodId(), method.getMetadata().size());
                w.printf(".pcToMetaIdx=m%d_pc2meta, ", method.getMethodId());
                // 添加 descIdx 和 descLen
                String desc = method.getDescriptor();
                Integer descIdx = globalStringIndexMap.get(desc);
                if (descIdx != null) {
                    w.printf(".descIdx=%d, .descLen=%d, ", descIdx, desc.length());
                } else {
                    w.printf(".descIdx=-1, .descLen=0, ");
                }
                // 添加异常表
                List<ExceptionEntry> excTable = method.getExceptionTable();
                if (excTable != null && !excTable.isEmpty()) {
                    w.printf(".exceptionTable=m%d_exc, .exceptionTableLength=%d, ",
                        method.getMethodId(), excTable.size());
                } else {
                    w.printf(".exceptionTable=NULL, .exceptionTableLength=0, ");
                }
                w.printf(".isStatic=%d },\n", method.isStatic() ? 1 : 0);
            }
            w.println("};");
        }
    }
    
    private void emitStringPool(PrintWriter w, Set<String> strings) {
        if (encryptStrings) {
            // 加密模式：生成 ChaCha20 密钥和 nonce
            w.println("const uint8_t vm_string_key[] = {");
            for (int i = 0; i < vmStringKey.length; i++) {
                if (i % 16 == 0) w.print("    ");
                w.printf("0x%02x%s", vmStringKey[i] & 0xFF, (i < vmStringKey.length - 1 ? ", " : ""));
            }
            w.println("\n};");
            
            w.println("const uint8_t vm_string_nonce[] = {");
            for (int i = 0; i < stringNonce.length; i++) {
                if (i % 16 == 0) w.print("    ");
                w.printf("0x%02x%s", stringNonce[i] & 0xFF, (i < stringNonce.length - 1 ? ", " : ""));
            }
            w.println("\n};");
            w.println();
            
            // 加密并存储每个字符串
            int idx = 0;
            for (String s : strings) {
                byte[] plaintext = s.getBytes(StandardCharsets.UTF_8);
                byte[] encrypted = CryptoUtils.chacha20(vmStringKey, stringNonce, 0, plaintext);
                
                w.printf("static const unsigned char vm_str_%d[] = {", idx);
                for (int i = 0; i < encrypted.length; i++) {
                    if (i % 16 == 0) w.printf("\n    ");
                    w.printf("0x%02x%s", encrypted[i] & 0xFF, (i < encrypted.length - 1 ? ", " : ""));
                }
                w.println("\n};");
                idx++;
            }
            w.println();
            
            w.println("VMString vm_strings[] = {");
            idx = 0;
            for (String s : strings) {
                w.printf("    { .encData=vm_str_%d, .decData=NULL, .len=%d, .encrypted=1 },\n", idx, s.length());
                idx++;
            }
            w.println("};");
            w.println();
        } else {
            // 非加密模式：直接存储明文字符串（添加 null 终止符）
            int idx = 0;
            for (String s : strings) {
                byte[] bytes = s.getBytes(StandardCharsets.UTF_8);
                w.printf("static const char vm_str_%d[] = {", idx);
                for (int i = 0; i < bytes.length; i++) {
                    if (i % 16 == 0) w.printf("\n    ");
                    w.printf("0x%02x, ", bytes[i] & 0xFF);
                }
                w.println("\n    0x00");  // null 终止符
                w.println("};");
                idx++;
            }
            w.println();
            
            w.println("VMString vm_strings[] = {");
            idx = 0;
            for (String s : strings) {
                w.printf("    { .encData=(const unsigned char*)vm_str_%d, .decData=NULL, .len=%d, .encrypted=0 },\n", idx, s.length());
                idx++;
            }
            w.println("};");
            w.println();
        }
    }
    
    /**
     * 生成全局 Bootstrap 方法表
     */
    private void emitBootstrapMethods(PrintWriter w) {
        if (globalBootstrapMethods.isEmpty()) {
            w.println("VMBootstrapMethod vm_bootstrap_methods[] = {};");
            w.println();
            return;
        }
        
        // 为每个 bootstrap 方法的参数生成数组
        for (int i = 0; i < globalBootstrapMethods.size(); i++) {
            BootstrapEntry bsm = globalBootstrapMethods.get(i);
            List<Object> args = bsm.getArguments();
            List<BootstrapEntry.ArgType> argTypes = bsm.getArgumentTypes();
            
            if (args != null && !args.isEmpty()) {
                w.printf("static BsmArg bsm%d_args[] = {", i);
                for (int j = 0; j < args.size(); j++) {
                    Object arg = args.get(j);
                    BootstrapEntry.ArgType argType = argTypes.get(j);
                    
                    w.printf("\n    { .type=%s, ", bsmArgTypeToString(argType));
                    
                    switch (argType) {
                        case STRING:
                            w.printf(".strIdx=%d", getOrAddStringIndex(arg.toString()));
                            break;
                        case INTEGER:
                            w.printf(".intVal=%d", (Integer) arg);
                            break;
                        case LONG:
                            w.printf(".longVal=%dL", (Long) arg);
                            break;
                        case FLOAT:
                            w.printf(".floatVal=%af", (Float) arg);
                            break;
                        case DOUBLE:
                            w.printf(".doubleVal=%a", (Double) arg);
                            break;
                        case METHOD_TYPE:
                            w.printf(".strIdx=%d", getOrAddStringIndex(arg.toString()));
                            break;
                        case METHOD_HANDLE:
                            // 格式: "tag:owner:name:desc"
                            String[] parts = arg.toString().split(":");
                            if (parts.length >= 4) {
                                w.printf(".handleTag=%s, .strIdx=%d", parts[0], 
                                    getOrAddStringIndex(parts[1] + "." + parts[2] + parts[3]));
                            }
                            break;
                    }
                    w.printf(" },");
                }
                w.println("\n};");
            }
        }
        
        // 生成 bootstrap 方法数组
        w.println("VMBootstrapMethod vm_bootstrap_methods[] = {");
        for (int i = 0; i < globalBootstrapMethods.size(); i++) {
            BootstrapEntry bsm = globalBootstrapMethods.get(i);
            w.printf("    { .handleTag=%d, ", bsm.getHandleTag());
            w.printf(".ownerIdx=%d, ", getOrAddStringIndex(bsm.getHandleOwner()));
            w.printf(".nameIdx=%d, ", getOrAddStringIndex(bsm.getHandleName()));
            w.printf(".descIdx=%d, ", getOrAddStringIndex(bsm.getHandleDescriptor()));
            
            List<Object> args = bsm.getArguments();
            if (args != null && !args.isEmpty()) {
                w.printf(".args=bsm%d_args, .argCount=%d", i, args.size());
            } else {
                w.printf(".args=NULL, .argCount=0");
            }
            w.printf(" },\n");
        }
        w.println("};");
        w.println();
    }
    
    /**
     * 获取字符串索引
     */
    private int getOrAddStringIndex(String s) {
        Integer idx = globalStringIndexMap.get(s);
        if (idx != null) return idx;
        // 字符串应该已经在全局池中
        System.err.println("[WARN] String not found in global pool: " + s);
        return 0;
    }
    
    private String bsmArgTypeToString(BootstrapEntry.ArgType type) {
        switch (type) {
            case STRING: return "BSM_ARG_STRING";
            case INTEGER: return "BSM_ARG_INTEGER";
            case LONG: return "BSM_ARG_LONG";
            case FLOAT: return "BSM_ARG_FLOAT";
            case DOUBLE: return "BSM_ARG_DOUBLE";
            case METHOD_TYPE: return "BSM_ARG_METHOD_TYPE";
            case METHOD_HANDLE: return "BSM_ARG_METHOD_HANDLE";
            default: return "BSM_ARG_STRING";
        }
    }
    
    private String escapeCString(String s) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '\\': sb.append("\\\\"); break;
                case '"': sb.append("\\\""); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                default:
                    if (c >= 32 && c < 127) {
                        sb.append(c);
                    } else {
                        // 使用 \xhh 格式，但如果下一个字符是十六进制字符，需要用字符串结束
                        // 更安全的做法：使用 "\" "\xhh" 拼接，这样 \xhh 后面是引号结束
                        sb.append("\" \"\\x");
                        sb.append(String.format("%02x", (int) c));
                        sb.append("\" \"");
                    }
            }
        }
        return sb.toString();
    }
    
    /**
     * 将方法局部字符串索引映射到全局字符串索引
     */
    private int mapStringIndex(List<String> localPool, int localIdx) {
        if (localPool == null || localIdx < 0 || localIdx >= localPool.size()) {
            return localIdx; // 保持原值（可能是错误的）
        }
        String str = localPool.get(localIdx);
        Integer globalIdx = globalStringIndexMap.get(str);
        if (globalIdx == null) {
            return localIdx; // 不应该发生
        }
        return globalIdx;
    }
    
    /**
     * 将方法局部的 bootstrap 方法索引映射到全局索引
     */
    private int mapBsmIndex(List<BootstrapEntry> localBsmList, int localIdx) {
        if (localBsmList == null || localIdx < 0 || localIdx >= localBsmList.size()) {
            System.out.println("[DEBUG] mapBsmIndex: invalid input, localBsmList=" + localBsmList + ", localIdx=" + localIdx);
            return localIdx;
        }
        BootstrapEntry bsm = localBsmList.get(localIdx);
        // 使用与 collectBootstrapMethods 相同的 key 生成逻辑
        StringBuilder keyBuilder = new StringBuilder();
        keyBuilder.append(bsm.getHandleOwner()).append(".");
        keyBuilder.append(bsm.getHandleName()).append(bsm.getHandleDescriptor());
        if (bsm.getArguments() != null) {
            for (Object arg : bsm.getArguments()) {
                keyBuilder.append("|").append(arg != null ? arg.toString() : "null");
            }
        }
        String key = keyBuilder.toString();
        
        Integer globalIdx = bootstrapIndexMap.get(key);
        if (globalIdx == null) {
            System.out.println("[DEBUG] mapBsmIndex: key not found: " + key);
            System.out.println("[DEBUG] Available keys: " + bootstrapIndexMap.keySet());
            return localIdx;
        }
        return globalIdx;
    }
    
    private void emitMethodData(PrintWriter w, EncryptedMethodData method) {
        int id = method.getMethodId();
        List<String> localPool = method.getStringPool();
        
        // 调试：打印 BSM 信息
        if (id == 111) {
            System.out.println("[DEBUG] Method 111 (Calculations.run) BSM info:");
            List<BootstrapEntry> bsmList = method.getBootstrapMethods();
            if (bsmList != null) {
                System.out.println("  BSM count: " + bsmList.size());
                for (int i = 0; i < bsmList.size(); i++) {
                    BootstrapEntry bsm = bsmList.get(i);
                    System.out.println("  Local BSM[" + i + "]: " + bsm.getHandleOwner() + "." + bsm.getHandleName());
                    List<Object> args = bsm.getArguments();
                    List<BootstrapEntry.ArgType> argTypes = bsm.getArgumentTypes();
                    if (args != null) {
                        System.out.println("    args count: " + args.size());
                        for (int j = 0; j < args.size(); j++) {
                            Object arg = args.get(j);
                            BootstrapEntry.ArgType argType = argTypes != null && j < argTypes.size() ? argTypes.get(j) : null;
                            System.out.println("    arg[" + j + "] type=" + argType + " value=" + arg);
                        }
                    }
                }
            } else {
                System.out.println("  BSM list is null!");
            }
        }
        
        // 字节码
        w.printf("static const uint8_t m%d_bc[] = {", id);
        byte[] bc = method.getEncryptedBytecode();
        for (int i = 0; i < bc.length; i++) {
            if (i % 16 == 0) w.printf("\n    ");
            w.printf("0x%02x%s", bc[i] & 0xFF, (i < bc.length - 1 ? ", " : ""));
        }
        w.println("\n};");
        w.println();
        
        // 元数据数组
        List<MetaEntry> metaList = method.getMetadata();
        if (!metaList.isEmpty()) {
            for (int i = 0; i < metaList.size(); i++) {
                emitMetaEntry(w, id, i, metaList.get(i));
            }
            
            w.printf("static MetaEntry m%d_meta[] = {", id);
            for (int i = 0; i < metaList.size(); i++) {
                MetaEntry m = metaList.get(i);
                w.printf("\n    { .type=%s, ", metaTypeToString(m.type));
                
                switch (m.type) {
                    case META_INT:
                    case META_LOCAL:
                        w.printf(".intVal=%d", m.intVal);
                        break;
                    case META_LONG:
                        w.printf(".longVal=%dL", m.longVal);
                        break;
                    case META_FLOAT:
                        w.printf(".floatVal=%af", m.floatVal);
                        break;
                    case META_DOUBLE:
                        w.printf(".doubleVal=%a", m.doubleVal);
                        break;
                    case META_STRING:
                        w.printf(".strIdx=%d, .strLen=%d", 
                            mapStringIndex(localPool, m.strIdx), m.strLen);
                        break;
                    case META_CLASS:
                        w.printf(".classIdx=%d, .classLen=%d", 
                            mapStringIndex(localPool, m.classIdx), m.classLen);
                        break;
                    case META_FIELD:
                    case META_METHOD:
                        w.printf(".ownerIdx=%d, .ownerLen=%d, ",
                            mapStringIndex(localPool, m.ownerIdx), m.ownerLen);
                        w.printf(".nameIdx=%d, .nameLen=%d, ",
                            mapStringIndex(localPool, m.nameIdx), m.nameLen);
                        w.printf(".descIdx=%d, .descLen=%d",
                            mapStringIndex(localPool, m.descIdx), m.descLen);
                        break;
                    case META_INVOKE_DYNAMIC:
                        // 映射局部 bsmIdx 到全局索引
                        int globalBsmIdx = mapBsmIndex(method.getBootstrapMethods(), m.bsmIdx);
                        if (id == 111) {
                            System.out.println("[DEBUG] Method 111 INVOKEDYNAMIC: localIdx=" + m.bsmIdx + " -> globalIdx=" + globalBsmIdx);
                        }
                        w.printf(".bsmIdx=%d, ", globalBsmIdx);
                        w.printf(".nameIdx=%d, .nameLen=%d, ",
                            mapStringIndex(localPool, m.nameIdx), m.nameLen);
                        w.printf(".descIdx=%d, .descLen=%d",
                            mapStringIndex(localPool, m.descIdx), m.descLen);
                        break;
                    case META_JUMP:
                        w.printf(".jumpOffset=%d", m.jumpOffset);
                        break;
                    case META_IINC:
                        w.printf(".iincIndex=%d, .iincConst=%d", m.iincIndex, m.iincConst);
                        break;
                    case META_SWITCH:
                        w.printf(".switchLow=%d, .switchHigh=%d, ",
                            m.switchLow, m.switchHigh);
                        w.printf(".switchOffsets=m%d_meta%d_offs", id, i);
                        // Add switchKeys for LOOKUPSWITCH (when keys are present)
                        if (m.switchKeys != null && m.switchKeys.length > 0) {
                            w.printf(", .switchKeys=m%d_meta%d_keys", id, i);
                        }
                        break;
                    case META_TYPE:
                        w.printf(".classIdx=%d, .classLen=%d, .dims=%d",
                            mapStringIndex(localPool, m.classIdx), m.classLen, m.dims);
                        break;
                    default:
                        break;
                }
                w.printf(" },");
            }
            w.println("\n};");
            w.println();
        } else {
            w.printf("static MetaEntry m%d_meta[] = {};\n", id);
            w.println();
        }
        
        // pcToMetaIdx 数组
        int[] pc2meta = method.getPcToMetaIdx();
        w.printf("static int m%d_pc2meta[] = {", id);
        for (int i = 0; i < pc2meta.length; i++) {
            if (i % 32 == 0) w.printf("\n    ");
            w.printf("%d%s", pc2meta[i], (i < pc2meta.length - 1 ? ", " : ""));
        }
        w.println("\n};");
        w.println();
        
        // 异常表
        List<ExceptionEntry> excTable = method.getExceptionTable();
        if (excTable != null && !excTable.isEmpty()) {
            w.printf("static VMExceptionEntry m%d_exc[] = {", id);
            for (int i = 0; i < excTable.size(); i++) {
                ExceptionEntry e = excTable.get(i);
                int catchTypeIdx = -1;
                if (e.getCatchType() != null) {
                    catchTypeIdx = getOrAddStringIndex(e.getCatchType());
                }
                w.printf("\n    { .startPc=%d, .endPc=%d, .handlerPc=%d, .catchTypeIdx=%d },",
                    e.getStartPc(), e.getEndPc(), e.getHandlerPc(), catchTypeIdx);
            }
            w.println("\n};");
            w.println();
        }
    }
    
    private void emitMetaEntry(PrintWriter w, int methodId, int idx, MetaEntry m) {
        if (m.type == MetaType.META_SWITCH && m.switchOffsets != null) {
            // Emit switchOffsets array
            w.printf("static int m%d_meta%d_offs[] = {", methodId, idx);
            for (int i = 0; i < m.switchOffsets.length; i++) {
                w.printf("%d%s", m.switchOffsets[i], (i < m.switchOffsets.length - 1 ? ", " : ""));
            }
            w.println("};");
            
            // Emit switchKeys array for LOOKUPSWITCH (non-null keys means LOOKUPSWITCH)
            if (m.switchKeys != null && m.switchKeys.length > 0) {
                w.printf("static int m%d_meta%d_keys[] = {", methodId, idx);
                for (int i = 0; i < m.switchKeys.length; i++) {
                    w.printf("%d%s", m.switchKeys[i], (i < m.switchKeys.length - 1 ? ", " : ""));
                }
                w.println("};");
            }
        }
    }
    
    private void emitBytes(PrintWriter w, byte[] data) {
        for (int i = 0; i < data.length; i++) {
            if (i % 16 == 0) w.printf("\n    ");
            w.printf("0x%02x%s", data[i] & 0xFF, (i < data.length - 1 ? ", " : ""));
        }
    }
    
    private String metaTypeToString(MetaType type) {
        return type.name();
    }
}
