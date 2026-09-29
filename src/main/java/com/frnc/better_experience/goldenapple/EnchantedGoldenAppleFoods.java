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
     * 强化版:与原版逐项相同,<strong>仅生命恢复一项不同</strong>——
     * 由原版的 II 级 20 秒(400 tick)改为 V 级 60 秒(1200 tick)。
     *
     * <p>其余四项(抗性提升 I / 防火 I 各 300 秒、伤害吸收 IV 120 秒)与营养值 4、
     * 饱和度 1.2、可随时食用均与原版一致,照抄是为了与上游模组保持逐字可对照。
     */
    public static FoodProperties buffed() {
        return new FoodProperties.Builder()
                .nutrition(4)
                .saturationMod(1.2F)
                .effect(new MobEffectInstance(MobEffects.REGENERATION, 1200, 4), 1.0F)
                .effect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 6000, 0), 1.0F)
                .effect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 6000, 0), 1.0F)
                .effect(new MobEffectInstance(MobEffects.ABSORPTION, 2400, 3), 1.0F)
                .alwaysEat()
                .build();
    }

    private EnchantedGoldenAppleFoods() {
    }
}
