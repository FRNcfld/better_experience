package com.frnc.better_experience.mixin;

import com.frnc.better_experience.spyglass.SpyglassState;

import net.minecraft.client.CameraType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 开镜期间强制第一人称 (来源: Spyglass Improvements 的 {@code CameraTypeMixin})。
 *
 * <p>原版的缩放与望远镜覆盖层都只在第一人称生效。玩家若停在第三人称, 按住使用键会「什么都没发生」,
 * 所以这里在按键开镜期间把 {@code isFirstPerson()} 改写成 true, 行为才一致。
 *
 * <p>只在 {@link SpyglassState#isForcedScoping()} 为真时生效 (即按键按住、且功能开启),
 * 平时完全不影响玩家的视角设置; 松开按键立刻恢复, 不会改动玩家的实际设置项。
 *
 * <p>目标是原版类, 方法名要经 refmap 重映射, 不可加 {@code remap = false}。
 */
@Mixin(CameraType.class)
public abstract class SpyglassCameraMixin
{
    @Inject(method = "isFirstPerson", at = @At("RETURN"), cancellable = true)
    private void betterExperience$forceFirstPerson(CallbackInfoReturnable<Boolean> cir)
    {
        // 本来就已经是 true 就不必再设一次
        if (!cir.getReturnValue() && SpyglassState.isForcedScoping())
        {
            cir.setReturnValue(true);
        }
    }
}
