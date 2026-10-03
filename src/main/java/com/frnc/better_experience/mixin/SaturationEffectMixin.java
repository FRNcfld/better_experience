package com.frnc.better_experience.mixin;

import com.frnc.better_experience.BetterExperienceServerConfig;
import com.frnc.better_experience.saturation.SaturationHandler;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 饱和状态效果的衰减式给药 (来源: Saturation Plus 的 {@code SaturationStatusEffectMixin})。
 *
 * <p><strong>为什么这条必须移植</strong>: 解除饱和度封顶之后, 原版饱和效果的「每 tick 加饱和」会变成
 * 真正的无限增长 (药水持续期间一路涨到 {@code Float.MAX_VALUE})。这里改成: 饱和度越高, 每 tick 的
 * 增量越小, 越接近软上限涨得越慢。
 *
 * <p>只在服务端生效, 且只处理 {@link MobEffects#SATURATION} 这一个效果, 其余效果一律交回原版。
 *
 * <p>公式 (与原模组逐字一致):
 * <pre>
 * 饱和度 &lt;= 20 : 直接走 FoodData.eat(amplifier + 1, 1.0F)        // 低饱和度时与体验一致
 * 饱和度 &gt;  20 : saturation += (amplifier + 1) * 2 / base^rate
 *                 其中 base = 饱和度 &gt;= 100 ? 饱和度 : 饱和度 - 19
 * </pre>
 * 默认 {@code rate = 2}, 于是增量按平方衰减; {@code rate = 0} 时 {@code base^0 = 1}, 等于关闭衰减。
 * 注意进入 {@code > 20} 分支后 {@code base} 恒大于 1, 不存在除零。
 *
 * <p>目标是原版类, 方法名要经 refmap 重映射, 不可加 {@code remap = false}。
 */
@Mixin(MobEffect.class)
public abstract class SaturationEffectMixin
{
    @Inject(method = "applyEffectTick(Lnet/minecraft/world/entity/LivingEntity;I)V",
            at = @At("HEAD"), cancellable = true)
    private void betterExperience$saturationEffectDecay(LivingEntity entity, int amplifier, CallbackInfo ci)
    {
        if (!BetterExperienceServerConfig.saturationEnabled) return;
        if ((Object) this != MobEffects.SATURATION) return;   // 只接管饱和效果
        if (entity.level().isClientSide()) return;            // 效果由服务端施加
        if (!(entity instanceof Player player)) return;

        FoodData foodData = player.getFoodData();
        float saturation = foodData.getSaturationLevel();
        int amount = amplifier + 1;

        if (saturation > SaturationHandler.SATURATION_SOFT_CAP)
        {
            float base = saturation >= 100.0F ? saturation : saturation - 19.0F;
            int rate = Math.abs(BetterExperienceServerConfig.saturationEffectDecayRate);
            float gain = (amount * 2.0F) / (float) Math.pow(base, rate);
            foodData.setSaturation(saturation + gain);
        }
        else
        {
            // 走的是本模组改写过的 eat, 因此同样享受溢出不封顶
            foodData.eat(amount, 1.0F);
        }

        ci.cancel();
    }
}
