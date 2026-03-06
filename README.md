# JNVM - Java Native VM Protector

A powerful Java bytecode protection tool that converts Java methods into native code execution, providing strong protection against decompilation and reverse engineering.

## Features

- **Native VM Execution**: Converts Java bytecode to native C code executed by a custom VM interpreter
  - **ChaCha20 Encryption**: All bytecode and string is encrypted using ChaCha20 stream cipher
- **Cross-Platform**: Supports multiple targets via Zig compiler (Windows, Linux, macOS, Android)
- **Anti-Debug**: Built-in anti-debugging protections

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
| `--encrypt-strings <BOOL>` | Enable ChaCha20 string encryption | true |
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
   - `vm_bridge.c` - JNI bridge with RegisterNatives
4. **Compilation**: Compiles native code using Zig for specified targets
5. **Patching**: Rewrites protected methods to call the native VM
6. **Packaging**: Embeds native libraries into the output JAR

## Compatibility

JNVM has been tested with JARs obfuscated by:
- **Zelix KlassMaster (ZKM)** - Full support including encrypted string decryption
- **ProGuard** - Standard obfuscation
- **Allatori** - String encryption and flow obfuscation
- **Vanilla Java** - No obfuscation

## Supported Bytecode Instructions (198/202 opcodes)

- **Constants**: `NOP`, `ACONST_NULL`, `ICONST_*`, `LCONST_*`, `FCONST_*`, `DCONST_*`, `BIPUSH`, `SIPUSH`, `LDC`, `LDC_W`, `LDC2_W`
- **Load/Store**: `ILOAD`, `LLOAD`, `FLOAD`, `DLOAD`, `ALOAD` (+ `_*` variants), `ISTORE`, `LSTORE`, `FSTORE`, `DSTORE`, `ASTORE` (+ `_*` variants)
- **Arithmetic**: `IADD`, `LADD`, `FADD`, `DADD`, `ISUB`, `LSUB`, `FSUB`, `DSUB`, `IMUL`, `LMUL`, `FMUL`, `DMUL`, `IDIV`, `LDIV`, `FDIV`, `DDIV`, `IREM`, `LREM`, `FREM`, `DREM`, `INEG`, `LNEG`, `FNEG`, `DNEG`
- **Bitwise/Shift**: `ISHL`, `LSHL`, `ISHR`, `LSHR`, `IUSHR`, `LUSHR`, `IAND`, `LAND`, `IOR`, `LOR`, `IXOR`, `LXOR`
- **Type Conversion**: `I2L`, `I2F`, `I2D`, `L2I`, `L2F`, `L2D`, `F2I`, `F2L`, `F2D`, `D2I`, `D2L`, `D2F`, `I2B`, `I2C`, `I2S`
- **Comparisons**: `LCMP`, `FCMPL`, `FCMPG`, `DCMPL`, `DCMPG`
- **Control Flow**: `IFEQ`, `IFNE`, `IFLT`, `IFGE`, `IFGT`, `IFLE`, `IF_ICMPEQ`, `IF_ICMPNE`, `IF_ICMPLT`, `IF_ICMPGE`, `IF_ICMPGT`, `IF_ICMPLE`, `IF_ACMPEQ`, `IF_ACMPNE`, `GOTO`, `GOTO_W`, `TABLESWITCH`, `LOOKUPSWITCH`
- **References**: `IFNULL`, `IFNONNULL`
- **Object Operations**: `NEW`, `CHECKCAST`, `INSTANCEOF`, `GETFIELD`, `PUTFIELD`, `GETSTATIC`, `PUTSTATIC`
- **Method Invocation**: `INVOKEVIRTUAL`, `INVOKESPECIAL`, `INVOKESTATIC`, `INVOKEINTERFACE`, `INVOKEDYNAMIC`
- **Array Operations**: `NEWARRAY`, `ANEWARRAY`, `MULTIANEWARRAY`, `ARRAYLENGTH`, `IALOAD`, `LALOAD`, `FALOAD`, `DALOAD`, `AALOAD`, `BALOAD`, `CALOAD`, `SALOAD`, `IASTORE`, `LASTORE`, `FASTORE`, `DASTORE`, `AASTORE`, `BASTORE`, `CASTORE`, `SASTORE`
- **Stack Operations**: `POP`, `POP2`, `DUP`, `DUP_X1`, `DUP_X2`, `DUP2`, `DUP2_X1`, `DUP2_X2`, `SWAP`
- **Exceptions**: `ATHROW`
- **Monitor**: `MONITORENTER`, `MONITOREXIT`
- **Returns**: `RETURN`, `IRETURN`, `LRETURN`, `FRETURN`, `DRETURN`, `ARETURN`
- **Local Variable**: `IINC`

### Not Implemented (Legacy/Rarely Used)
- `JSR`, `RET`, `JSR_W` - Deprecated since Java 6
- `WIDE` - Extended local variable indexing (rarely needed)

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
