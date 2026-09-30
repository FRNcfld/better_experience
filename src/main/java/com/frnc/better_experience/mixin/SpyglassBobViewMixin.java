package com.frnc.better_experience.mixin;

import com.frnc.better_experience.spyglass.SpyglassState;

import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 按键开镜期间取消视角摇晃 (view bobbing)。
 *
 * <p><strong>为什么需要</strong>: 原版 {@code GameRenderer.bobView} 的三处调用点<strong>只判断
 * {@code options.bobView()}, 没有任何 {@code isScoping()} 检查</strong>——也就是说原版开镜时照样会晃。
 * 而 {@code bobView} 里除了相机旋转还做了<strong>相机位移</strong>:
 * <pre>
 * posestack.translate(sin(f * PI) * f1 * 0.5F, -abs(cos(f * PI) * f1), 0.0F);
 * </pre>
 * 位移量约 0.1 格, 是个固定值; 但视野越小, 同样一段位移在画面上占的比例就越大, 于是一边走一边用高倍率
 * 望远镜时, 平常几乎看不出来的走路摆动会被放大成明显晃动。放大 10 倍 = 位移看上去也放大 10 倍。
 *
 * <p>这里在 {@code bobView} 的 HEAD 直接取消, 一次注入同时覆盖三个调用点 (renderLevel 与世界渲染各一处、
 * 手部渲染两处)。
 *
 * <p><strong>只在「按键强行开镜」时取消</strong> ({@link SpyglassState#isForcedScoping()}), 不动原版
 * 「手持望远镜右键」的既有表现。若希望后者也一并取消, 把判断换成 {@code player.isScoping()} 即可。
 *
 * <p>目标是原版类, 方法名要经 refmap 重映射, 不可加 {@code remap = false}。
 */
@Mixin(GameRenderer.class)
public abstract class SpyglassBobViewMixin
{
    @Inject(method = "bobView", at = @At("HEAD"), cancellable = true)
    private void betterExperience$noBobbingWhileScoping(CallbackInfo ci)
    {
        if (SpyglassState.isForcedScoping())
        {
            ci.cancel();
        }
    }
}
