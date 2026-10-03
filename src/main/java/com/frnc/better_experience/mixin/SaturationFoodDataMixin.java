package com.frnc.better_experience.mixin;

import com.frnc.better_experience.BetterExperienceServerConfig;
import com.frnc.better_experience.saturation.SaturationHandler;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 饱和度机制的核心改动 (来源: Saturation Plus 的 {@code HungerMixin} + {@code ExhaustionMixin}),
 * 三处都注入在 {@link FoodData} 上。
 *
 * <p><strong>1. {@code eat(int, float)} —— 解除封顶 + 溢出饥饿转饱和度</strong>
 *
 * <p>原版实现 (1.20.1) 是:
 * <pre>
 * foodLevel       = min(foodInc + foodLevel, 20)
 * saturationLevel = min(saturationLevel + foodInc * satMod * 2.0, foodLevel)   // ← 封顶在饥饿值
 * </pre>
 * 这里 HEAD 注入后 {@code cancel} 掉整段, 换成:
 * <pre>
 * saturationGain = foodInc * satMod * 2.0
 * if (foodLevel + foodInc &gt; 20)
 *     saturationGain += (foodLevel + foodInc - 20) * 溢出比例     // ← 原模组这里是写死的 1:1
 * foodLevel       = min(foodInc + foodLevel, 20)
 * saturationLevel = saturationLevel + saturationGain              // ← 不封顶
 * </pre>
 * 也就是满饥饿时进食, 多出来的营养值会按配置的比例 (默认 1:1) 转成饱和度, 且饱和度可以超过饥饿值。
 *
 * <p><strong>2. {@code tick(Player)} —— 可选的两道上限</strong>
 *
 * <p>HEAD 注入但<strong>不 cancel</strong> (原版逻辑继续跑), 只做两道钳制:
 * 「饱和度是否受饥饿值限制」(默认关) 与「饱和度硬上限」(默认 -1 = 不限)。
 * 原模组还在这里有一段 {@code saturation >= 2^24} 的浮点溢出保护, 正常游玩永远触发不到, <strong>未移植</strong>。
 *
 * <p><strong>3. {@code @ModifyConstant} —— 消耗度上限</strong>
 *
 * <p>原版 {@code tick} 里 {@code 4.0F} 这个常量出现两次: 偏移 20 的是 {@code if (exhaustionLevel > 4.0F)}
 * 的比较, 偏移 31 的是 {@code exhaustionLevel -= 4.0F} 的扣减 (已用 javap 核对 ordinal 为 0 与 1)。
 * 两处都替换成配置的消耗上限; 配置 ≤ 0 时返回 {@link Float#MAX_VALUE} 等于彻底禁用消耗。
 *
 * <p>{@code require = 0}: 万一将来原版改掉这两个常量, 只是本项失效, 不该让整个 mixin 硬崩。
 *
 * <p>目标是原版类, 方法名与常量都要经 better_experience.refmap.json 重映射到 SRG,
 * 因此<strong>不可加 {@code remap = false}</strong>。本 mixin 放在配置的 {@code mixins} (双端) 列表里:
 * 客户端本地预测也会走 {@code eat}, 双端一致才不会出现饥饿条跳变, 权威值仍以服务端为准。
 */
@Mixin(FoodData.class)
public abstract class SaturationFoodDataMixin
{
    /** {@code eat} 的 HEAD 注入: 整段替换原版逻辑 */
    @Inject(method = "eat(IF)V", at = @At("HEAD"), cancellable = true)
    private void betterExperience$eatWithOverflow(int foodLevelModifier, float saturationLevelModifier, CallbackInfo ci)
    {
        if (!BetterExperienceServerConfig.saturationEnabled) return;

        FoodData self = (FoodData) (Object) this;

        int foodLevel = self.getFoodLevel();

        // 常规增量, 与 vanilla 完全一致
        float saturationGain = (float) foodLevelModifier * saturationLevelModifier * 2.0F;

        // 溢出饥饿值转饱和度 (原模组写死 1:1, 这里按配置的比例换算)
        int overflow = foodLevel + foodLevelModifier - SaturationHandler.MAX_FOOD_LEVEL;
        if (overflow > 0)
        {
            saturationGain += (float) overflow * (float) BetterExperienceServerConfig.saturationOverflowRatio;
        }

        self.setFoodLevel(Math.min(foodLevel + foodLevelModifier, SaturationHandler.MAX_FOOD_LEVEL));
        // 刻意不封顶: 这是"溢出饱和度存储"的关键 (封顶由 tick 里的两道可选限制负责)
        self.setSaturation(self.getSaturationLevel() + saturationGain);

        ci.cancel();
    }

    /** {@code tick} 的 HEAD 注入: 只做可选的两道上限, 不 cancel */
    @Inject(method = "tick(Lnet/minecraft/world/entity/player/Player;)V", at = @At("HEAD"))
    private void betterExperience$clampSaturation(Player player, CallbackInfo ci)
    {
        if (!BetterExperienceServerConfig.saturationEnabled) return;

        FoodData self = (FoodData) (Object) this;
        float saturation = self.getSaturationLevel();

        if (BetterExperienceServerConfig.saturationHungerLimitsSaturation)
        {
            saturation = Math.min(saturation, (float) self.getFoodLevel());
        }

        int cap = BetterExperienceServerConfig.saturationMaxSaturation;
        saturation = Math.min(saturation, cap < 0 ? Float.MAX_VALUE : (float) cap);

        self.setSaturation(saturation);
    }

    /** 消耗度比较处的 4.0F (ordinal 0) */
    @ModifyConstant(method = "tick(Lnet/minecraft/world/entity/player/Player;)V",
            constant = @Constant(floatValue = 4.0F, ordinal = 0), require = 0)
    private float betterExperience$maxExhaustionCompare(float original)
    {
        return BetterExperienceServerConfig.saturationEnabled ? SaturationHandler.maxExhaustion() : original;
    }

    /** 消耗度扣减处的 4.0F (ordinal 1) */
    @ModifyConstant(method = "tick(Lnet/minecraft/world/entity/player/Player;)V",
            constant = @Constant(floatValue = 4.0F, ordinal = 1), require = 0)
    private float betterExperience$maxExhaustionDecrement(float original)
    {
        return BetterExperienceServerConfig.saturationEnabled ? SaturationHandler.maxExhaustion() : original;
    }
}
