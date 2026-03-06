package com.alphaautoleak.jnvm.codegen.emitter.instruction.control;

import com.alphaautoleak.jnvm.codegen.emitter.Instruction;

import java.io.PrintWriter;

/**
 * Return instruction (RETURN, IRETURN, LRETURN, FRETURN, DRETURN, ARETURN)
 * 
 * 重要：返回指令使用 goto exit_label 跳转到统一退出点，
 * 在那里根据方法描述符的返回类型进行正确的装箱。
 * 这样 IRETURN 可以根据方法返回 boolean/int/byte/short/char 
 * 选择正确的装箱类型 (Boolean/Integer/Byte/Short/Character)。
 */
public class ReturnInstruction extends Instruction {
    private final String returnType; // "void", "int", "long", "float", "double", "object"

    public ReturnInstruction(int opcode, String name, String returnType) {
        super(opcode, name);
        this.returnType = returnType;
    }

    @Override
    protected void generateBody(PrintWriter w) {
        switch (returnType) {
            case "void":
                // void 方法直接跳转到退出点
                w.println("                goto method_exit;");
                break;
            case "int":
                // IRETURN: 弹出 int 值并存储到 result 中，设置 resultType 标记
                // 注意：不在这里装箱，在 method_exit 处根据方法描述符装箱
                w.println("                result.i = frame.stack[--frame.sp].i;");
                w.println("                resultType = 'I';");
                w.println("                goto method_exit;");
                break;
            case "long":
                // LRETURN: 弹出 long 值
                w.println("                result.j = frame.stack[--frame.sp].j;");
                w.println("                resultType = 'J';");
                w.println("                goto method_exit;");
                break;
            case "float":
                // FRETURN: 弹出 float 值
                w.println("                result.f = frame.stack[--frame.sp].f;");
                w.println("                resultType = 'F';");
                w.println("                goto method_exit;");
                break;
            case "double":
                // DRETURN: 弹出 double 值
                w.println("                result.d = frame.stack[--frame.sp].d;");
                w.println("                resultType = 'D';");
                w.println("                goto method_exit;");
                break;
            case "object":
            default:
                // ARETURN: 弹出对象引用
                w.println("                result.l = frame.stack[--frame.sp].l;");
                w.println("                resultType = 'L';");
                w.println("                goto method_exit;");
                break;
        }
    }

    @Override
    protected boolean needsPcIncrement() {
        return false;  // 返回指令不增加 pc，直接跳转到 method_exit
    }

    @Override
    public void generate(PrintWriter w) {
        w.printf("            case 0x%02x: /* %s */\n", opcode, comment);
        generateBody(w);
    }

    @Override
    public void generateComputedGoto(PrintWriter w) {
        w.printf("        OP_%02x:  /* %s */\n", opcode, comment);
        // 在 computed goto 模式下，返回指令直接跳转到 method_exit
        generateBody(w);  // 复用相同的逻辑，都用 goto method_exit
    }
}