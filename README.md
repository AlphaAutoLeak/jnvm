# JNVM - Java Native VM Protector

A powerful Java bytecode protection tool that converts Java methods into native code execution, providing strong protection against decompilation and reverse engineering.

## Features

- **Native VM Execution**: Converts Java bytecode to native C code executed by a custom VM interpreter
- **ChaCha20 Encryption**: All bytecode is encrypted using ChaCha20 stream cipher
- **Random Bridge Class**: Generates random, natural-looking package names for the bridge class on each build
- **Cross-Platform**: Supports multiple targets via Zig compiler (Windows, Linux, macOS, Android)
- **Anti-Debug**: Built-in anti-debugging protections
- **Exception Handling**: Full support for Java exception handling in native code
- **Synchronized Blocks**: Support for `MONITORENTER`/`MONITOREXIT` instructions

## Requirements

- Java 17+
- [Zig](https://ziglang.org/) 0.11+ (for native compilation)
- Gradle 8+

## Usage

### Basic Usage

```bash
java -jar jnvm.jar --jar input.jar --out output-obf.jar
```

### Command Line Options

| Option | Description | Default |
|--------|-------------|---------|
| `--jar <FILE>` | Input JAR file path | (required) |
| `--out <FILE>` | Output JAR file path | `<input>-protected.jar` |
| `--protect <RULE>` | Protection rule (repeatable) | Protect all methods |
| `--target <TARGETS>` | Comma-separated Zig targets | Current platform |
| `--anti-debug <BOOL>` | Enable anti-debug protections | true |
| `--debug <BOOL>` | Enable debug logging | false |
| `--native-dir <DIR>` | Output directory for native sources | `./native` |

### Protection Rules

- `package.**` - Protect all methods in package and subpackages
- `ClassName` - Protect all methods in class
- `ClassName#methodName` - Protect specific method
- `@annotation` - Protect methods with annotation

### Examples

```bash
# Protect with specific target
java -jar jnvm.jar --jar app.jar --out app-obf.jar --target x86_64-linux-gnu

# Protect specific packages
java -jar jnvm.jar --jar app.jar --protect "com.example.core.**" --protect "com.example.security.**"

# Multi-platform build
java -jar jnvm.jar --jar app.jar --target x86_64-windows-gnu,x86_64-linux-gnu,aarch64-linux-android29

# Enable debug mode
java -jar jnvm.jar --jar app.jar --debug true
```

## How It Works

1. **Scanning**: Analyzes the input JAR to find methods matching protection rules
2. **Encryption**: Encrypts bytecode using ChaCha20 with unique keys per method
3. **Code Generation**: Generates C source files including:
   - `vm_types.h` - VM type definitions
   - `vm_data.c` - Encrypted method data and string pool
   - `vm_interpreter.c` - Custom bytecode interpreter
   - `vm_bridge.c` - JNI bridge with random class name
4. **Compilation**: Compiles native code using Zig for specified targets
5. **Patching**: Rewrites protected methods to call the native VM
6. **Packaging**: Embeds native libraries into the output JAR

## Random Bridge Class

Each build generates a unique, natural-looking bridge class name:

```
# Build 1
[PATCH] Bridge class: org.access.json.json.Manager

# Build 2  
[PATCH] Bridge class: internal.proxy.beans.sql.Driver

# Build 3
[PATCH] Bridge class: com.util.concurrent.spi.Factory
```

This makes reverse engineering significantly harder as the entry point varies between builds.

## Supported Bytecode Instructions

- Constants: `ICONST`, `LCONST`, `FCONST`, `DCONST`, `BIPUSH`, `SIPUSH`, `LDC`
- Load/Store: `ILOAD`, `LLOAD`, `FLOAD`, `DLOAD`, `ALOAD`, `ISTORE`, etc.
- Arithmetic: `IADD`, `ISUB`, `IMUL`, `IDIV`, `IREM`, `INEG`, etc.
- Type Conversion: `I2L`, `I2F`, `I2D`, `L2I`, etc.
- Comparisons: `LCMP`, `FCMPL`, `FCMPG`, `DCMPL`, `DCMPG`
- Control Flow: `IFEQ`, `IFNE`, `IFLT`, `IFGE`, `IFGT`, `IFLE`, `GOTO`, `TABLESWITCH`, `LOOKUPSWITCH`
- References: `IFNULL`, `IFNONNULL`, `IF_ACMPEQ`, `IF_ACMPNE`
- Object Operations: `NEW`, `GETFIELD`, `PUTFIELD`, `GETSTATIC`, `PUTSTATIC`
- Method Invocation: `INVOKEVIRTUAL`, `INVOKESPECIAL`, `INVOKESTATIC`, `INVOKEINTERFACE`, `INVOKEDYNAMIC`
- Array Operations: `NEWARRAY`, `ANEWARRAY`, `ARRAYLENGTH`, `IALOAD`, `IASTORE`, etc.
- Stack Operations: `POP`, `POP2`, `DUP`, `DUP2`, `SWAP`
- Exceptions: `ATHROW`
- Monitor: `MONITORENTER`, `MONITOREXIT`
- Returns: `RETURN`, `IRETURN`, `LRETURN`, `FRETURN`, `DRETURN`, `ARETURN`

## Building from Source

```bash
git clone https://github.com/snowf14k3/JNVM.git
cd JNVM
./gradlew build
```

## License

This project is provided for educational and legitimate software protection purposes only.

## Disclaimer

Use this tool responsibly. The authors are not responsible for any misuse or damage caused by this software.
