package com.frnc.better_experience.mixin;

import com.frnc.better_experience.BetterExperienceClientConfig;
import com.frnc.better_experience.spyglass.client.SpyglassZoom;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 缩放的生效点 (来源: Spyglass Improvements 的 {@code AbstractClientPlayerEntityMixin})。
 *
 * <p>原版 {@code AbstractClientPlayer#getFieldOfViewModifier()} 在「第一人称 + 开镜 + 正在使用物品」时
 * 直接返回写死的 {@code 0.1F} —— 那正好是 10 倍。这里在 RETURN 处改写为滚轮调出来的值,
 * 于是放大倍数由玩家控制 (默认仍是 10 倍, 与原版一致)。
 *
 * <p>之所以在 RETURN 处覆盖而不是只处理某个分支: 这样「手持望远镜真开镜」与
 * 「按住按键强行开镜」两条路径得到完全一致的处理, 不需要分别照顾。
 *
 * <p>只作用于第一人称: 第三人称由 {@code SpyglassCameraMixin} 在开镜时强制切回第一人称,
 * 所以这里不需要额外考虑。
 *
 * <p>目标是原版类, 方法名要经 refmap 重映射, 不可加 {@code remap = false}。
 */
@Mixin(AbstractClientPlayer.class)
public abstract class SpyglassFovMixin
{
    @Inject(method = "getFieldOfViewModifier", at = @At("RETURN"), cancellable = true)
    private void betterExperience$applyZoom(CallbackInfoReturnable<Float> cir)
    {
        if (!BetterExperienceClientConfig.spyglassEnabled) return;

        AbstractClientPlayer self = (AbstractClientPlayer) (Object) this;
        if (!Minecraft.getInstance().options.getCameraType().isFirstPerson()) return;
        if (!self.isScoping()) return;

        cir.setReturnValue(SpyglassZoom.fovModifier());
    }
}
