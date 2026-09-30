package com.frnc.better_experience.mixin;

import com.frnc.better_experience.gamma.ExtendedGammaValueSet;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.network.chat.Component;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 把亮度 (伽马) 滑块上限从原版的 100% 扩到 1000% (来源: GJEB 的 {@code OptionsMixin})。
 *
 * <p>原版 {@code Options.gamma} 是 {@code new OptionInstance<>("options.gamma", ..., OptionInstance.UnitDouble.INSTANCE, 0.5D, d -> {})},
 * 取值范围被 {@code UnitDouble} 锁在 0.0–1.0, 并且 {@code UnitDouble.codec()} 也只接受 0–1,
 * 所以光改滑块不够——必须整个换掉这个 {@code OptionInstance}。
 *
 * <p>做法是 {@code @Redirect} 掉 {@code Options} 构造器里对 {@code gamma} 字段的那次 PUTFIELD,
 * 换成用 {@link ExtendedGammaValueSet} (0.0–10.0) 构造的实例。
 *
 * <p>除取值集合外, 其余参数<strong>逐字复刻原版</strong>: 相同的标题键、相同的 {@code noTooltip()}、
 * 相同的角标 lambda、相同的初值 0.5、以及同样为空的消费者 lambda (原版这里本来就是
 * {@code d -> {}}; 亮度实时更新靠的是每帧读取 {@code gamma.get()}, 见 {@code LightTexture},
 * 不依赖这个回调)。
 *
 * <p><strong>为什么无条件替换、不看配置</strong>: {@code Options} 只构造一次, 而伽马扩展开关
 * 是可以在游戏内切换的。若在配置为 false 时装回原版实例, 运行时就再也换不回扩展实现了。
 * 因此这里始终装入扩展取值集合 (0.0–10.0 恒定); 开关只决定这个值<strong>生不生效</strong>,
 * 由 {@link com.frnc.better_experience.mixin.GammaLightTextureMixin} 在渲染读取处处理,
 * 而配置项 {@code extendedGammaEnabled} 只决定开关的初值。
 *
 * <p>目标是原版类, 方法名 {@code <init>} 与字段名 {@code gamma} 都要经
 * better_experience.refmap.json 重映射到 SRG, 因此<strong>不可加 {@code remap = false}</strong>。
 *
 * <p>{@code @Mutable} 是必需的: {@code gamma} 在原版里是 {@code private final},
 * 只有去掉 final 才能在注入点重新赋值 (写法同 {@link FoodsMixin})。
 *
 * <p>本 mixin 只对客户端有意义, 放在配置的 {@code client} 列表里。
 */
@Mixin(Options.class)
public abstract class GammaOptionsMixin
{
    @Shadow
    @Final
    @Mutable
    private OptionInstance<Double> gamma;

    /**
     * {@code vanillaGamma} 是原版刚构造好、正要写进字段的实例。这里刻意<strong>丢弃它</strong>并换成
     * 自己的实现, 但参数必须保留——{@code @Redirect} 的处理方法签名要与被重定向的
     * PUTFIELD 的栈形状一致 (接收者 + 待写入的值)。
     */
    @Redirect(method = "<init>", at = @At(
            value = "FIELD",
            target = "Lnet/minecraft/client/Options;gamma:Lnet/minecraft/client/OptionInstance;",
            opcode = Opcodes.PUTFIELD))
    private void betterExperience$installToggleableGamma(Options self, OptionInstance<?> vanillaGamma)
    {
        this.gamma = betterExperience$createGamma();
    }

    /** 与原版 gamma 逐字一致, 仅把取值集合换成 0.0–10.0 (是否生效由渲染读取处决定) */
    private static OptionInstance<Double> betterExperience$createGamma()
    {
        return new OptionInstance<>(
                "options.gamma",
                OptionInstance.noTooltip(),
                (caption, value) -> {
                    int percent = (int) (value * 100.0D);
                    if (percent == 0)
                    {
                        return Options.genericValueLabel(caption, Component.translatable("options.gamma.min"));
                    }
                    if (percent == 50)
                    {
                        return Options.genericValueLabel(caption, Component.translatable("options.gamma.default"));
                    }
                    return percent == 1000
                            ? Options.genericValueLabel(caption, Component.translatable("options.gamma.max"))
                            : Options.genericValueLabel(caption, percent);
                },
                ExtendedGammaValueSet.instance(),
                0.5D,
                value -> { });
    }
}
