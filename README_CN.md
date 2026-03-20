# JNVM - Java Native VM 保护器

一款强大的 Java 字节码保护工具，将 Java 方法转换为原生代码执行，提供强有力的反编译和逆向工程保护。

## 特性

- **原生 VM 执行**：将 Java 字节码转换为自定义 VM 解释器执行的原生代码
- **操作码混淆**：每条字节码指令映射到随机操作码值，防止轻易重建
- **ChaCha20 加密**：所有字节码和字符串使用 ChaCha20 流密码加密
- **跨平台**：通过 Zig 编译器支持多平台（Windows、Linux、macOS、Android）
- **INVOKEDYNAMIC 支持**：支持 lambda 表达式和动态方法调用（实验性）

> **警告**：INVOKEDYNAMIC 实现为实验性功能，可能存在 bug。生产环境请谨慎使用。

## 环境要求

- Java 8+
- [Zig](https://ziglang.org/) 0.11+（用于原生编译）
- Gradle 8+

## 使用方法

### 配置文件

创建 `config.yml` 文件：

```yaml
# 输入 JAR 文件
jar: test/app.jar

# 输出 JAR 文件
out: test/app-protected.jar

# 保护规则
protect:
  - "**"  # 保护所有方法

# 目标平台
targets:
  - x86_64-windows-gnu

# 选项
debug: false
native-dir: native
protect-bootstrap-payload: false
direct-native-rewrite: false
```

### 运行

```bash
java -jar jnvm.jar config.yml
```

### 保护规则

| 规则 | 描述 |
|------|------|
| `package.**` | 保护包及子包中的所有方法 |
| `ClassName` | 保护类中的所有方法 |
| `ClassName#methodName` | 保护特定方法 |
| `@annotation` | 保护带有注解的方法 |

`direct-native-rewrite` 选项：
- `false`（默认）：通过重写方法体调用桥接方法进行保护
- `true`：将被保护的非 `<clinit>` 方法重写为真正的 `native` 方法，并在该类的 `<clinit>` 中注册每个类的原生方法

## 保护前后示例

### 原始代码
```java
package pack.tests.reflects.annot;
import java.lang.reflect.Field;
import pack.tests.reflects.annot.anno;
public class annoe {
    @anno(val="PASS")
    private static final String fail = "WHAT";
    @anno
    public void dox() throws Exception {
        String toGet = "FAIL";
        for (Field f : annoe.class.getDeclaredFields()) {
            f.setAccessible(true);
            anno obj = f.getAnnotation(anno.class);
            if (obj == null) continue;
            toGet = obj.val();
        }
        System.out.println(toGet);
    }
    @anno(val="no")
    public void dov() {
        System.out.println("FAIL");
    }
}
```

### 保护后代码
```java
package pack.tests.reflects.annot;
import lib.xml.abc.Dispatcher;
import pack.tests.reflects.annot.anno;
public class annoe {
   @anno(val="PASS")
   private static final String fail = "WHAT";
   @anno
   public void dox() throws Exception {
      Dispatcher.executeVoid(1282844577, new Object[]{this, annoe.class});
   }
   @anno(val="no")
   public void dov() {
      Dispatcher.executeVoid(1282844576, new Object[]{this, annoe.class});
   }
}
```

原始方法体被替换为原生 VM 调用，使逆向工程变得极其困难。

## 工作原理

1. **扫描**：分析输入 JAR，查找匹配保护规则的方法
2. **加密**：使用 ChaCha20 加密字节码，每个方法使用唯一密钥
3. **代码生成**：生成 C 源文件：
   - `vm_types.h` - VM 类型定义
   - `vm_data.c` - 加密的方法数据和字符串池
   - `vm_interpreter.c` - 自定义字节码解释器
   - `vm_bridge.c` - 带 RegisterNatives 的 JNI 桥接
4. **编译**：使用 Zig 为指定目标编译原生代码
5. **修补**：重写被保护的方法以调用原生 VM
6. **打包**：将原生库嵌入输出 JAR

## 兼容性

JNVM 已通过以下混淆器处理的 JAR 测试：
- **Zelix KlassMaster (ZKM)** - 支持包括加密字符串解密、invokedynamic
- **ProGuard** - 标准混淆
- **Allatori** - 字符串加密和流程混淆
- **原生 Java** - 无混淆

## 支持的字节码指令（190/202 操作码）

### 常量指令（19 个操作码）
`NOP`, `ACONST_NULL`, `ICONST_M1` 到 `ICONST_5`, `LCONST_0`, `LCONST_1`, `FCONST_0` 到 `FCONST_2`, `DCONST_0`, `DCONST_1`, `BIPUSH`, `SIPUSH`, `LDC`, `LDC_W`, `LDC2_W`

### 加载/存储（50 个操作码）
`ILOAD`, `LLOAD`, `FLOAD`, `DLOAD`, `ALOAD`, `ILOAD_0` 到 `ILOAD_3`, `LLOAD_0` 到 `LLOAD_3`, `FLOAD_0` 到 `FLOAD_3`, `DLOAD_0` 到 `DLOAD_3`, `ALOAD_0` 到 `ALOAD_3`, `ISTORE`, `LSTORE`, `FSTORE`, `DSTORE`, `ASTORE`, `ISTORE_0` 到 `ISTORE_3`, `LSTORE_0` 到 `LSTORE_3`, `FSTORE_0` 到 `FSTORE_3`, `DSTORE_0` 到 `DSTORE_3`, `ASTORE_0` 到 `ASTORE_3`

### 栈操作（9 个操作码）
`POP`, `POP2`, `DUP`, `DUP_X1`, `DUP_X2`, `DUP2`, `DUP2_X1`, `DUP2_X2`, `SWAP`

### 算术运算（52 个操作码）
- **加/减/乘/除/取余**：`IADD`, `LADD`, `FADD`, `DADD`, `ISUB`, `LSUB`, `FSUB`, `DSUB`, `IMUL`, `LMUL`, `FMUL`, `DMUL`, `IDIV`, `LDIV`, `FDIV`, `DDIV`, `IREM`, `LREM`, `FREM`, `DREM`
- **取负**：`INEG`, `LNEG`, `FNEG`, `DNEG`
- **位运算/移位**：`ISHL`, `LSHL`, `ISHR`, `LSHR`, `IUSHR`, `LUSHR`, `IAND`, `LAND`, `IOR`, `LOR`, `IXOR`, `LXOR`
- **类型转换**：`I2L`, `I2F`, `I2D`, `L2I`, `L2F`, `L2D`, `F2I`, `F2L`, `F2D`, `D2I`, `D2L`, `D2F`, `I2B`, `I2C`, `I2S`
- **比较**：`LCMP`, `FCMPL`, `FCMPG`, `DCMPL`, `DCMPG`

### 控制流（26 个操作码）
- **条件跳转**：`IFEQ`, `IFNE`, `IFLT`, `IFGE`, `IFGT`, `IFLE`, `IF_ICMPEQ`, `IF_ICMPNE`, `IF_ICMPLT`, `IF_ICMPGE`, `IF_ICMPGT`, `IF_ICMPLE`, `IF_ACMPEQ`, `IF_ACMPNE`, `IFNULL`, `IFNONNULL`
- **无条件跳转**：`GOTO`, `GOTO_W`
- **开关**：`TABLESWITCH`, `LOOKUPSWITCH`
- **返回**：`RETURN`, `IRETURN`, `LRETURN`, `FRETURN`, `DRETURN`, `ARETURN`
- **其他**：`IINC`, `ATHROW`

### 对象操作（12 个操作码）
`NEW`, `CHECKCAST`, `INSTANCEOF`, `GETFIELD`, `PUTFIELD`, `GETSTATIC`, `PUTSTATIC`, `INVOKEVIRTUAL`, `INVOKESPECIAL`, `INVOKESTATIC`, `INVOKEINTERFACE`, `INVOKEDYNAMIC`

### 数组操作（20 个操作码）
`NEWARRAY`, `ANEWARRAY`, `MULTIANEWARRAY`, `ARRAYLENGTH`, `IALOAD`, `LALOAD`, `FALOAD`, `DALOAD`, `AALOAD`, `BALOAD`, `CALOAD`, `SALOAD`, `IASTORE`, `LASTORE`, `FASTORE`, `DASTORE`, `AASTORE`, `BASTORE`, `CASTORE`, `SASTORE`

### 监视器（2 个操作码）
`MONITORENTER`, `MONITOREXIT`

### 未实现（12 个操作码）

| 操作码 | 原因 |
|--------|------|
| `JSR`, `RET`, `JSR_W` | 自 Java 6 起已弃用 |
| `WIDE` | 扩展局部变量索引（很少需要） |
| `RET_*` 变体 | WIDE 扩展的一部分 |

## 从源码构建

```bash
git clone https://github.com/AlphaAutoLeak/jnvm.git
cd JNVM
./gradlew build
```

编译后的 JAR 位于 `build/libs/` 目录。

## 许可证

本项目仅供教育和合法软件保护目的使用。

## 免责声明

请负责任地使用本工具。作者不对任何滥用或本软件造成的损害负责。
