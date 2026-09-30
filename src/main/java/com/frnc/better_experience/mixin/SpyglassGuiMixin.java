package com.frnc.better_experience.mixin;

import com.frnc.better_experience.ClientConfig;
import com.frnc.better_experience.spyglass.SpyglassOverlayStyle;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 望远镜界面的两处开关 (合并了来源模组的 {@code InGameHudMixin} 的对应部分)。
 *
 * <ul>
 *   <li><b>覆盖层样式</b> —— 配置为 {@code NONE} 时不画原版的望远镜遮罩 (来源模组的 {@code noRender});</li>
 *   <li><b>准星</b> —— 配置关闭时才隐藏准星。<strong>注意默认值是 true (= 保留原版行为)</strong>,
 *       与来源模组默认隐藏准星相反, 因为用户要的就是"保留准星"。</li>
 * </ul>
 *
 * <p>来源模组原本还通过 {@code @ModifyArg} 把覆盖层贴图换成它自带的两张 PNG —— 那两张是 GPL 素材,
 * <strong>未复制</strong>, 因此本模组只支持「用原版贴图」与「完全不画」两种。
 *
 * <p>目标是原版类, 方法名要经 refmap 重映射, 不可加 {@code remap = false}。
 */
@Mixin(Gui.class)
public abstract class SpyglassGuiMixin
{
    @Inject(method = "renderSpyglassOverlay", at = @At("HEAD"), cancellable = true)
    private void betterExperience$hideOverlay(GuiGraphics graphics, float scopeScale, CallbackInfo ci)
    {
        if (ClientConfig.spyglassEnabled && ClientConfig.spyglassOverlay == SpyglassOverlayStyle.NONE)
        {
            ci.cancel();
        }
    }

    @Inject(method = "renderCrosshair", at = @At("HEAD"), cancellable = true)
    private void betterExperience$hideCrosshair(GuiGraphics graphics, CallbackInfo ci)
    {
        if (!ClientConfig.spyglassEnabled || ClientConfig.spyglassShowCrosshair) return;

        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && player.isScoping())
        {
            ci.cancel();
        }
    }
}
