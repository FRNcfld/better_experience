package com.frnc.better_experience.mixin;

import com.frnc.better_experience.BetterExperienceClientConfig;
import com.frnc.better_experience.spyglass.client.SpyglassZoom;

import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 滚轮调整缩放 (来源: Spyglass Improvements 的 {@code MouseMixin} + {@code MouseEvents.onScroll})。
 *
 * <p>开镜时滚轮不再做别的事, 而是按当前倍数的固定比例调整放大倍数。
 *
 * <p>注入后 {@code cancel} 掉原版处理: 一是避免滚轮累积量影响之后的普通滚动, 二是确保开镜期间滚轮
 * 不会去动快捷栏 (原版虽然也判断了 {@code isScoping()}, 但直接接管更明确)。
 *
 * <p>目标是原版类, 方法名要经 refmap 重映射, 不可加 {@code remap = false}。
 */
@Mixin(MouseHandler.class)
public abstract class SpyglassScrollMixin
{
    @Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
    private void betterExperience$zoomOnScroll(long window, double xOffset, double yOffset, CallbackInfo ci)
    {
        if (!BetterExperienceClientConfig.spyglassEnabled) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || !player.isScoping()) return;
        if (!mc.options.getCameraType().isFirstPerson()) return;

        SpyglassZoom.applyScroll(yOffset,
                mc.options.discreteMouseScroll().get(),
                mc.options.mouseWheelSensitivity().get());

        ci.cancel();
    }
}
