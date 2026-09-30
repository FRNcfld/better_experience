package com.frnc.better_experience.mixin.sodium;

import com.mojang.logging.LogUtils;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.IntInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.slf4j.Logger;

/**
 * 用 ASM 把 Sodium 亮度滑块的上限从 100 改成 1000 (来源: GJEB 的
 * {@code SodiumGameOptionPagesAsm})。
 *
 * <p>Sodium 的「亮度」不是直接用 {@code Options.gamma}, 而是自己建了一个整数百分比滑块:
 * 对外表现是 {@code gamma * 100}, 滑块范围 0–100。伽马扩到 0.0–10.0 之后, 不一起改这里,
 * 在 Sodium 的视频设置界面里亮度仍然只能拉到 100%。
 *
 * <p>目标字节码形如
 * {@code new SliderControl(option, 0, 100, 1, ControlValueFormatter.brightness())},
 * 亮度上限就是 {@code brightness()} 调用往前数<strong>第二条真实指令</strong>的整数常量
 * (中间隔着步长 {@code iconst_1})。把它改成 {@code sipush 1000} 即可。
 *
 * <p>比来源模组多做了两处加固:
 * <ul>
 *   <li>往前找指令时跳过 label / 行号等伪指令 (它们的 opcode 为负), 避免它们插在中间时找错位置;</li>
 *   <li>只在该常量确实是原版的 100 时才改写, 否则打警告——万一某个版本换了常量或调换了参数顺序,
 *       宁可不动也不要改坏别的数值。</li>
 * </ul>
 */
public final class SodiumGameOptionPagesAsm
{
    private static final Logger LOGGER = LogUtils.getLogger();

    /** Sodium 亮度滑块的原版上限 (百分比) */
    private static final int VANILLA_BRIGHTNESS_MAX = 100;

    /** 扩到 1000, 与伽马 0.0–10.0 对应 (Sodium 内部按 gamma * 100 换算) */
    private static final int EXTENDED_BRIGHTNESS_MAX = 1000;

    private static final String CONTROL_VALUE_FORMATTER_SUFFIX = "/ControlValueFormatter";
    private static final String BRIGHTNESS_METHOD = "brightness";
    private static final String BRIGHTNESS_DESC_PREFIX = "()L";
    private static final String BRIGHTNESS_DESC_SUFFIX = "ControlValueFormatter;";

    private SodiumGameOptionPagesAsm()
    {
    }

    public static void raiseBrightnessSliderMax(ClassNode classNode)
    {
        for (MethodNode method : classNode.methods)
        {
            for (AbstractInsnNode instruction : method.instructions)
            {
                if (!(instruction instanceof MethodInsnNode call)) continue;
                if (call.getOpcode() != Opcodes.INVOKESTATIC) continue;
                if (!call.owner.endsWith(CONTROL_VALUE_FORMATTER_SUFFIX)) continue;
                if (!BRIGHTNESS_METHOD.equals(call.name)) continue;
                if (!isBrightnessDescriptor(call.desc)) continue;

                rewriteBrightnessMax(call);
            }
        }
    }

    /** {@code ()L<某包>/ControlValueFormatter;} —— 不写死包名, 以容忍 Sodium 系改名 */
    private static boolean isBrightnessDescriptor(String descriptor)
    {
        return descriptor.startsWith(BRIGHTNESS_DESC_PREFIX)
                && descriptor.endsWith(BRIGHTNESS_DESC_SUFFIX);
    }

    private static void rewriteBrightnessMax(MethodInsnNode brightnessCall)
    {
        AbstractInsnNode maxNode = previousRealInstruction(previousRealInstruction(brightnessCall));

        if (!(maxNode instanceof IntInsnNode max))
        {
            LOGGER.warn("[better_experience] 未能在 Sodium 的亮度滑块处找到上限常量, 该界面亮度仍将封顶 {}%", VANILLA_BRIGHTNESS_MAX);
            return;
        }

        if (max.operand != VANILLA_BRIGHTNESS_MAX)
        {
            LOGGER.warn("[better_experience] Sodium 亮度滑块的上限常量不是预期的 {}, 而是 {}, 已跳过改写",
                    VANILLA_BRIGHTNESS_MAX, max.operand);
            return;
        }

        max.setOpcode(Opcodes.SIPUSH);
        max.operand = EXTENDED_BRIGHTNESS_MAX;
        LOGGER.debug("[better_experience] 已把 Sodium 亮度滑块上限改为 {}", EXTENDED_BRIGHTNESS_MAX);
    }

    /** 上一条真实指令: opcode 为负的是 label / 行号 / 栈帧等伪指令, 一律跳过 */
    private static AbstractInsnNode previousRealInstruction(AbstractInsnNode node)
    {
        AbstractInsnNode current = node == null ? null : node.getPrevious();

        while (current != null && current.getOpcode() < 0)
        {
            current = current.getPrevious();
        }

        return current;
    }
}
