package com.alphaautoleak.jnvm.codegen.emitter;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

/**
 * 生成 build.zig - Zig 构建脚本
 */
public class BuildZigGenerator {
    
    private final File dir;
    private final List<String> targets;
    private final String javaHome;
    
    public BuildZigGenerator(File dir, List<String> targets, String javaHome) {
        this.dir = dir;
        this.targets = targets;
        this.javaHome = javaHome;
    }
    
    public void generate() throws IOException {
        try (PrintWriter w = new PrintWriter(new java.io.FileWriter(new File(dir, "build.zig")))) {
            w.println("const std = @import(\"std\");");
            w.println();
            w.println("pub fn build(b: *std.Build) void {");
            w.println("    const target = b.standardTargetOptions(.{});");
            w.println("    const optimize = b.standardOptimizeOption(.{});");
            w.println();
            w.println("    const java_home = \"" + javaHome + "\";");
            w.println();
            w.println("    const lib = b.addSharedLibrary(.{");
            w.println("        .name = \"customvm\",");
            w.println("        .root_module = b.createModule(.{");
            w.println("            .target = target,");
            w.println("            .optimize = optimize,");
            w.println("            .link_libc = true,");
            w.println("        }),");
            w.println("    });");
            w.println();
            w.println("    lib.addIncludePath(.{ .cwd_relative = java_home ++ \"/include\" });");
            w.println("    lib.addIncludePath(.{ .cwd_relative = java_home ++ \"/include/linux\" });");
            w.println("    lib.addIncludePath(.{ .cwd_relative = java_home ++ \"/include/win32\" });");
            w.println();
            w.println("    lib.addCSourceFiles(.{");
            w.println("        .files = &.{ \"vm_data.c\", \"vm_interpreter.c\", \"vm_bridge.c\", \"chacha20.c\" },");
            w.println("        .flags = &.{ \"-O2\", \"-std=c11\" },");
            w.println("    });");
            w.println();
            w.println("    b.installArtifact(lib);");
            w.println("}");
        }
    }
}
