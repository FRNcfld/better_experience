package com.frnc.better_experience.goldenapple;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.food.FoodProperties;

/**
 * 附魔金苹果的两套食物属性:原版快照与强化版。
 *
 * <p>由 {@link com.frnc.better_experience.mixin.FoodsMixin} 在 {@code Foods.<clinit>} 末尾填充并安装,
 * 由 {@link com.frnc.better_experience.mixin.ItemMixin} 在取用食物属性时按配置二选一。
 */
public final class EnchantedGoldenAppleFoods {

    /**
     * 原版(未强化)的附魔金苹果属性。
     * 在 {@code FoodsMixin} 覆盖 {@code Foods.ENCHANTED_GOLDEN_APPLE} <strong>之前</strong>抓取,
     * 因此不会随原版数值变动而失真。{@code Foods} 类初始化完成前为 {@code null}。
     */
    public static FoodProperties vanilla;

    /**
     * 强化版:比原版强化了<strong>生命恢复 / 抗性提升 / 伤害吸收 / 营养 / 饱和度</strong>五项,
     * 防火与"可随时食用"与原版一致。
     *
     * <p>逐项对照见 {@link com.frnc.better_experience.mixin.FoodsMixin} 的表格。等级换算:
     * {@code MobEffectInstance} 的 amplifier 从 0 起算, 所以等级 III = amplifier 2、
     * 等级 V = amplifier 4。
     *
     * <p>饱和度按原版公式 {@code 营养 × 饱和度系数 × 2} 结算, 且<strong>封顶在饥饿值</strong>
     * (见 {@code FoodData#eat(int, float)}): 这里 {@code 10 × 3.0 × 2 = 60}, 远超上限,
     * 实际效果就是把饱和度条直接拉满; 营养 10 同理会把饥饿值顶到上限 20。
     */
    public static FoodProperties buffed() {
        return new FoodProperties.Builder()
                .nutrition(10)
                .saturationMod(3.0F)
                .effect(new MobEffectInstance(MobEffects.REGENERATION, 1200, 4), 1.0F)
                .effect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 6000, 2), 1.0F)
                .effect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 6000, 0), 1.0F)
                .effect(new MobEffectInstance(MobEffects.ABSORPTION, 2400, 4), 1.0F)
                .alwaysEat()
                .build();
    }

    private EnchantedGoldenAppleFoods() {
    }
}
