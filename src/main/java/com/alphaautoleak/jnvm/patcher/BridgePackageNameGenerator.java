package com.alphaautoleak.jnvm.patcher;

import java.util.Random;

/**
 * 生成随机的 Bridge 类包名
 */
class BridgePackageNameGenerator {

    private static final String[] PREFIXES = {
            "org", "com", "io", "net", "app", "core", "internal", "runtime",
            "system", "base", "native", "dev", "lib", "pkg", "module"
    };

    private static final String[] MIDDLES = {
            "reflect", "io", "nio", "util", "lang", "math", "net", "security",
            "crypto", "awt", "swing", "beans", "text", "time", "concurrent",
            "function", "stream", "atomic", "locks", "abstract", "annotation",
            "management", "rmi", "sql", "xml", "json", "logger", "handler",
            "proxy", "factory", "builder", "helper", "support", "wrapper",
            "access", "impl", "spi"
    };

    private static final String[] SUFFIXES = {
            "Bridge", "Helper", "Handler", "Proxy", "Factory", "Loader",
            "Manager", "Wrapper", "Support", "Accessor", "Invoker", "Executor",
            "Runner", "Worker", "Processor", "Provider", "Service", "Context",
            "Environment", "Config", "Registry", "Container", "Controller",
            "Dispatcher", "Resolver", "Mapper", "Adapter", "Connector", "Driver", "Agent"
    };

    static String generate() {
        Random rand = new Random();
        int depth = 3 + rand.nextInt(3);
        StringBuilder sb = new StringBuilder();

        sb.append(PREFIXES[rand.nextInt(PREFIXES.length)]);
        for (int i = 1; i < depth; i++) {
            sb.append("/").append(MIDDLES[rand.nextInt(MIDDLES.length)]);
        }
        sb.append("/").append(SUFFIXES[rand.nextInt(SUFFIXES.length)]);

        return sb.toString();
    }
}
