package com.frnc.better_experience.mixin;

import com.frnc.better_experience.BetterExperienceServerConfig;

import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 「可无限进食」(来源: Saturation Plus 的 {@code PlayerHungerMixin})。
 *
 * <p>原版 {@code Player#canEat(boolean)} 只在 {@code ignoreHunger || 无敌 || needsFood()} 时返回 true,
 * 也就是饥饿条满 20 时普通食物吃不下。要让「满饥饿进食 → 溢出营养转饱和度」成立, 就必须放开这个限制。
 *
 * <p>这里在 HEAD 注入并直接改写返回值。与原模组的<b>两处不同</b>:
 * <ul>
 *   <li>原模组只对 {@code ServerPlayer} 生效并读 gamerule; 这里<strong>双端生效</strong>——
 *       客户端也要放行, 否则吃东西的抬手动画和服务端不一致。配置是 COMMON, 双端各读各的;</li>
 *   <li>原模组只 {@code setReturnValue} 不 {@code cancel}; 这里同样只改返回值, 语义等价。</li>
 * </ul>
 *
 * <p>目标是原版类, 方法名要经 refmap 重映射, 不可加 {@code remap = false}。
 */
@Mixin(Player.class)
public abstract class SaturationCanEatMixin
{
    @Inject(method = "canEat(Z)Z", at = @At("HEAD"), cancellable = true)
    private void betterExperience$alwaysHungry(boolean ignoreHunger, CallbackInfoReturnable<Boolean> cir)
    {
        if (!BetterExperienceServerConfig.saturationEnabled || !BetterExperienceServerConfig.saturationAlwaysHungry) return;

        cir.setReturnValue(true);
    }
}
