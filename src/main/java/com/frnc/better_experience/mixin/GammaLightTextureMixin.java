package com.frnc.better_experience.mixin;

import com.frnc.better_experience.gamma.ExtendedGamma;

import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.renderer.LightTexture;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 亮度扩展开关的<strong>生效点</strong>: 决定实际参与渲染的亮度值。
 *
 * <p>{@code LightTexture#updateLightTexture} 里的
 * {@code float f14 = this.minecraft.options.gamma().get().floatValue();}
 * 是全 Minecraft <strong>唯一</strong>读取 {@code options.gamma()} 的地方 (已对整个源码核实)。
 * 所以亮度扩展开关只需在这里把值压回去, 就能做到
 * "滑块随便拖到 1000%, 但功能关着时亮度按原版来", 同时完全不碰存储值
 * —— 存储值不动, 开关来回切和重启都不会丢玩家设置。
 *
 * <p><strong>为什么用 {@code ordinal = 1}</strong>: {@code updateLightTexture} 里有<strong>两处</strong>
 * {@code OptionInstance.get()} (偏移 76 和 612), 亮度那次是第二处, 所以只靠 target 无法唯一定位。
 * {@code Options.gamma()} 那次调用虽然唯一, 但 {@link OptionInstance} 是 final 类, 没法在它那里
 * 换成一个返回钳制值的包装实例, 因此只能定位到 {@code get()} 上。
 *
 * <p>因为 {@code ordinal} 是位置匹配, 处理器里额外做了一次<strong>身份校验</strong>: 只有当拿到的确实是
 * {@code options.gamma()} 那一个实例时才改写, 否则原样返回。这样即使将来 ordinal 指错, 也只会退化成
 * "不生效", 而不会把别的选项的值改坏。
 *
 * <p>目标是原版类, 方法名与成员都要经 better_experience.refmap.json 重映射到 SRG,
 * 因此<strong>不可加 {@code remap = false}</strong>。本 mixin 只对客户端有意义, 放在配置的
 * {@code client} 列表里。
 */
@Mixin(LightTexture.class)
public abstract class GammaLightTextureMixin
{
    @Redirect(method = "updateLightTexture", at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/client/OptionInstance;get()Ljava/lang/Object;",
            ordinal = 1))
    private Object betterExperience$applyGammaToggle(OptionInstance<?> option)
    {
        Object value = option.get();

        // 身份校验: 只处理亮度那一个选项, 其余原样返回 (见类注释)
        if (!(value instanceof Double stored) || option != Minecraft.getInstance().options.gamma())
        {
            return value;
        }

        return ExtendedGamma.effectiveGamma(stored);
    }
}
