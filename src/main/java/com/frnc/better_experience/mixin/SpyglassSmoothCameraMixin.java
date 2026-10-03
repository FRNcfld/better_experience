package com.frnc.better_experience.mixin;

import com.frnc.better_experience.BetterExperienceClientConfig;
import com.frnc.better_experience.spyglass.client.SpyglassZoom;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 平滑镜头: 按缩放倍数成比例放慢视角转动, 放大得越多瞄得越稳
 * (来源: Spyglass Improvements 的 {@code MouseMixin} + {@code MouseEvents.onDisplacementX/Y})。
 *
 * <p><strong>做法与原模组不同, 但数学上等价, 而且稳健得多。</strong> 原版 {@code MouseHandler.turnPlayer()}
 * 里有个分支:
 * <pre>
 * else if (第一人称 &amp;&amp; isScoping())  { d2 = accumulatedDX * d5; }   // 开镜: d5
 * else                            { d2 = accumulatedDX * d6; }   // 平时: d6 = d5 * 8
 * </pre>
 * 原模组是把开镜分支里的 {@code d5} 换成 {@code d6 * MULTIPLIER} (即再乘 {@code 8 * fovModifier})。
 * 这里改成在方法开头<strong>直接把 {@code accumulatedDX/DY} 乘掉</strong>同样的系数 —— 因为这两个字段
 * 随后只被该分支使用, 且用后立刻清零, 所以效果完全一致。
 *
 * <p>好处是<strong>不需要按局部变量序号做 {@code @ModifyVariable}</strong>, 也不用去碰原模组那套
 * {@code SmoothDouble} 的重置逻辑 (那部分要影子化四个私有字段, 是最容易随版本失效的地方)。
 *
 * <p>字段名用 Parchment 里的 {@code accumulatedDX} / {@code accumulatedDY}, 由 reobf 负责重映射到 SRG
 * (已验证 reobf 会处理 mixin 的 {@code @Shadow} 成员, 参见 {@code FlatBedrockMixin} 的 {@code randomName})。
 */
@Mixin(MouseHandler.class)
public abstract class SpyglassSmoothCameraMixin
{
    @Shadow
    private double accumulatedDX;

    @Shadow
    private double accumulatedDY;

    @Inject(method = "turnPlayer", at = @At("HEAD"))
    private void betterExperience$scaleAimWhileZoomed(CallbackInfo ci)
    {
        if (!BetterExperienceClientConfig.spyglassEnabled || !BetterExperienceClientConfig.spyglassSmoothCamera) return;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null || !player.isScoping()) return;

        // 8 * fovModifier: 开镜时原版系数是 d5, 乘上它等价于原模组的 d6 * MULTIPLIER
        double factor = 8.0D * SpyglassZoom.fovModifier();

        this.accumulatedDX *= factor;
        this.accumulatedDY *= factor;
    }
}
