# JNVM - Java Native VM Protector

A powerful Java bytecode protection tool that converts Java methods into native code execution, providing strong protection against decompilation and reverse engineering.

## Features

- **Native VM Execution**: Converts Java bytecode to native code executed by a custom VM interpreter
- **Opcode Obfuscation**: Each bytecode instruction is mapped to a random opcode value, preventing easy reconstruction
- **ChaCha20 Encryption**: All bytecode and strings are encrypted using ChaCha20 stream cipher
- **Cross-Platform**: Supports multiple targets via Zig compiler (Windows, Linux, macOS, Android)
- **INVOKEDYNAMIC Support**: Support for lambda expressions and dynamic method invocation (experimental)

> **Warning**: INVOKEDYNAMIC implementation is experimental and may contain bugs. Use with caution in production environments.

## Requirements

- Java 8+
- [Zig](https://ziglang.org/) 0.11+ (for native compilation)
- Gradle 8+

## Usage

### Configuration File

Create a `config.yml` file:

```yaml
# Input JAR file
jar: test/app.jar

# Output JAR file
out: test/app-protected.jar

# Protection rules
protect:
  - "**"  # Protect all methods

# Target platforms
targets:
  - x86_64-windows-gnu

# Options
debug: false
native-dir: native
```

### Run

```bash
java -jar jnvm.jar config.yml
```

### Protection Rules

| Rule | Description |
|------|-------------|
| `package.**` | Protect all methods in package and subpackages |
| `ClassName` | Protect all methods in class |
| `ClassName#methodName` | Protect specific method |
| `@annotation` | Protect methods with annotation |

## How It Works

1. **Scanning**: Analyzes the input JAR to find methods matching protection rules
2. **Encryption**: Encrypts bytecode using ChaCha20 with unique keys per method
3. **Code Generation**: Generates C source files:
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

## Supported Bytecode Instructions (190/202 opcodes)

### Constants (19 opcodes)
`NOP`, `ACONST_NULL`, `ICONST_M1` to `ICONST_5`, `LCONST_0`, `LCONST_1`, `FCONST_0` to `FCONST_2`, `DCONST_0`, `DCONST_1`, `BIPUSH`, `SIPUSH`, `LDC`, `LDC_W`, `LDC2_W`

### Load/Store (50 opcodes)
`ILOAD`, `LLOAD`, `FLOAD`, `DLOAD`, `ALOAD`, `ILOAD_0` to `ILOAD_3`, `LLOAD_0` to `LLOAD_3`, `FLOAD_0` to `FLOAD_3`, `DLOAD_0` to `DLOAD_3`, `ALOAD_0` to `ALOAD_3`, `ISTORE`, `LSTORE`, `FSTORE`, `DSTORE`, `ASTORE`, `ISTORE_0` to `ISTORE_3`, `LSTORE_0` to `LSTORE_3`, `FSTORE_0` to `FSTORE_3`, `DSTORE_0` to `DSTORE_3`, `ASTORE_0` to `ASTORE_3`

### Stack Operations (9 opcodes)
`POP`, `POP2`, `DUP`, `DUP_X1`, `DUP_X2`, `DUP2`, `DUP2_X1`, `DUP2_X2`, `SWAP`

### Arithmetic (52 opcodes)
- **Add/Sub/Mul/Div/Rem**: `IADD`, `LADD`, `FADD`, `DADD`, `ISUB`, `LSUB`, `FSUB`, `DSUB`, `IMUL`, `LMUL`, `FMUL`, `DMUL`, `IDIV`, `LDIV`, `FDIV`, `DDIV`, `IREM`, `LREM`, `FREM`, `DREM`
- **Negation**: `INEG`, `LNEG`, `FNEG`, `DNEG`
- **Bitwise/Shift**: `ISHL`, `LSHL`, `ISHR`, `LSHR`, `IUSHR`, `LUSHR`, `IAND`, `LAND`, `IOR`, `LOR`, `IXOR`, `LXOR`
- **Conversion**: `I2L`, `I2F`, `I2D`, `L2I`, `L2F`, `L2D`, `F2I`, `F2L`, `F2D`, `D2I`, `D2L`, `D2F`, `I2B`, `I2C`, `I2S`
- **Comparison**: `LCMP`, `FCMPL`, `FCMPG`, `DCMPL`, `DCMPG`

### Control Flow (26 opcodes)
- **Conditional**: `IFEQ`, `IFNE`, `IFLT`, `IFGE`, `IFGT`, `IFLE`, `IF_ICMPEQ`, `IF_ICMPNE`, `IF_ICMPLT`, `IF_ICMPGE`, `IF_ICMPGT`, `IF_ICMPLE`, `IF_ACMPEQ`, `IF_ACMPNE`, `IFNULL`, `IFNONNULL`
- **Unconditional**: `GOTO`, `GOTO_W`
- **Switch**: `TABLESWITCH`, `LOOKUPSWITCH`
- **Return**: `RETURN`, `IRETURN`, `LRETURN`, `FRETURN`, `DRETURN`, `ARETURN`
- **Other**: `IINC`, `ATHROW`

### Object Operations (12 opcodes)
`NEW`, `CHECKCAST`, `INSTANCEOF`, `GETFIELD`, `PUTFIELD`, `GETSTATIC`, `PUTSTATIC`, `INVOKEVIRTUAL`, `INVOKESPECIAL`, `INVOKESTATIC`, `INVOKEINTERFACE`, `INVOKEDYNAMIC`

### Array Operations (20 opcodes)
`NEWARRAY`, `ANEWARRAY`, `MULTIANEWARRAY`, `ARRAYLENGTH`, `IALOAD`, `LALOAD`, `FALOAD`, `DALOAD`, `AALOAD`, `BALOAD`, `CALOAD`, `SALOAD`, `IASTORE`, `LASTORE`, `FASTORE`, `DASTORE`, `AASTORE`, `BASTORE`, `CASTORE`, `SASTORE`

### Monitor (2 opcodes)
`MONITORENTER`, `MONITOREXIT`

### Not Implemented (12 opcodes)
| Opcode | Reason |
|--------|--------|
| `JSR`, `RET`, `JSR_W` | Deprecated since Java 6 |
| `WIDE` | Extended local variable indexing (rarely needed) |
| `RET_*` variants | Part of WIDE extension |

## Building from Source

```bash
git clone https://github.com/AlphaAutoLeak/jnvm.git
cd JNVM
./gradlew build
```

The compiled JAR will be in `build/libs/`.

## License

This project is provided for educational and legitimate software protection purposes only.

## Disclaimer

Use this tool responsibly. The authors are not responsible for any misuse or damage caused by this software.
