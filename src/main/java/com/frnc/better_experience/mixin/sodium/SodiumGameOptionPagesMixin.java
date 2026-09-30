package com.frnc.better_experience.mixin.sodium;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;

/**
 * Sodium / Embeddium 兼容层的<strong>挂载点</strong> (来源: GJEB 的
 * {@code integration.sodium.SodiumGameOptionPagesMixin})。
 *
 * <p>本类不含任何注入, 存在的唯一目的是让 {@link SodiumMixinPlugin#preApply} 针对
 * {@code SodiumGameOptionPages} 收到回调——真正的改写在 {@link SodiumGameOptionPagesAsm} 里用
 * ASM 完成。占位方法只是为了让这个类不是一个空 mixin。
 *
 * <p>用 {@code targets} 字符串而不是类字面量: 该目标是 Sodium 的类, 本模组刻意不依赖 Sodium,
 * 所以它不在编译期 classpath 上。
 *
 * <p>{@link Pseudo} 是<strong>必需</strong>的, 不是装饰: 没有它, Mixin 注解处理器会因目标类
 * 在编译期不存在而报 {@code Mixin target ... could not be found} 并直接中断编译。加上它,
 * 该目标会被当作"软目标", 编译期与运行期都允许缺席。注意 {@code disableTargetValidator}
 * 那个 Gradle 选项<strong>不能</strong>替代它——那条报错来自 {@code AnnotatedMixin}, 不在
 * {@code TargetValidator} 的管辖范围内。
 *
 * <p>运行期还有两道保险: 本 mixin 所在的配置是 {@code "required": false}, 且它只在插件检测到
 * Sodium 系模组时才会被注册 (见 {@link SodiumMixinPlugin#getMixins()}), 所以未安装时完全无副作用。
 *
 * <p>包名 {@code me.jellysquid.mods.sodium.client.gui} 是 Sodium、Rubidium 与 1.20.1 版
 * Embeddium 共用的 (Embeddium 为兼容其它模组的 mixin 刻意保留了它)。
 */
@Pseudo
@Mixin(targets = "me.jellysquid.mods.sodium.client.gui.SodiumGameOptionPages")
public abstract class SodiumGameOptionPagesMixin
{
    private static void betterExperience$placeholder()
    {
    }
}
