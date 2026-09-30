package com.frnc.better_experience.mixin;

import com.frnc.better_experience.spyglass.SpyglassState;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 让「使用望远镜」按键也能进入开镜状态 (来源: Spyglass Improvements 的 {@code PlayerEntityMixin})。
 *
 * <p>原版 {@code Player#isScoping()} 要求真的在手持并使用望远镜。按住模组的按键时这里直接改写成 true,
 * 于是原版的缩放、覆盖层、准星逻辑全都会跟着生效——望远镜放在<strong>饰品栏</strong>或背包里也能用。
 *
 * <p>原模组是靠「让使用键等效按下 + 自动把望远镜换到副手/切快捷栏」达到同样效果的 (那套要做背包点击,
 * 又依赖 MixinExtras)。直接改写 {@code isScoping()} 简单得多, 而且不会擅自翻动玩家的物品栏。
 *
 * <p>目标是原版类, 方法名要经 refmap 重映射, 不可加 {@code remap = false}。
 */
@Mixin(Player.class)
public abstract class SpyglassScopingMixin
{
    @Inject(method = "isScoping", at = @At("RETURN"), cancellable = true)
    private void betterExperience$forceScoping(CallbackInfoReturnable<Boolean> cir)
    {
        if (SpyglassState.isForcedScoping())
        {
            cir.setReturnValue(true);
        }
    }
}
