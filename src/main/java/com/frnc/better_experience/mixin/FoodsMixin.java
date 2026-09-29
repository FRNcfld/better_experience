package com.frnc.better_experience.mixin;

import com.frnc.better_experience.goldenapple.EnchantedGoldenAppleFoods;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.food.Foods;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 强化附魔金苹果的生命恢复效果 (来源: 【附魔金苹果重生】enchanted_golden_apple_reborn 1.4 的 FoodsMixin)。
 *
 * <p>在 {@code Foods.<clinit>} 的 TAIL 注入, 把 {@code Foods.ENCHANTED_GOLDEN_APPLE} 换成强化版。
 * 与原版 1.20.1 逐项对照如下 (只改了生命恢复一项):
 *
 * <table>
 *   <tr><th>效果</th><th>强化版</th><th>原版</th></tr>
 *   <tr><td>生命恢复</td><td>1200 tick (60s), 等级 V</td><td>400 tick (20s), 等级 II</td></tr>
 *   <tr><td>抗性提升</td><td>6000 tick, 等级 I</td><td>同</td></tr>
 *   <tr><td>防火</td><td>6000 tick, 等级 I</td><td>同</td></tr>
 *   <tr><td>伤害吸收</td><td>2400 tick, 等级 IV</td><td>同</td></tr>
 *   <tr><td>营养 / 饱和度</td><td>4 / 1.2</td><td>同</td></tr>
 *   <tr><td>可随时食用</td><td>是</td><td>同</td></tr>
 * </table>
 *
 * <p><strong>是否生效由配置项 {@code better_experience-common.toml} 的 {@code enchantedGoldenAppleBuffEnabled}
 * 决定, 默认开启。</strong> 但本类<strong>不读配置</strong>——{@code Foods.<clinit>} 与配置加载的
 * 先后顺序取决于 Forge 内部调度, 不可依赖。这里只做两件事: 把原版值快照下来, 然后无条件装上
 * 强化版; 真正的取舍推迟到 {@link ItemMixin} 在"取用食物属性"那一刻进行。
 *
 * <p>注意: 这里<strong>必须重写整个 FoodProperties</strong>——原版把四项效果都写在
 * {@code Foods.<clinit>} 里, 而普通金苹果与附魔金苹果用的是同一个
 * {@code new MobEffectInstance(MobEffects.REGENERATION, ...)} 调用点,
 * 所以无法用 {@code @ModifyArg} 精确只改附魔金苹果那一处而不误伤普通金苹果。
 *
 * <p>{@code @Mutable} 是必需的: {@code ENCHANTED_GOLDEN_APPLE} 在 {@code Foods} 里是
 * {@code public static final}, 只有去掉 final 才能在注入点重新赋值。
 *
 * <p>本模组<strong>不含</strong>来源模组的旧配方 (8 金块 + 苹果)。
 */
@Mixin(Foods.class)
public abstract class FoodsMixin
{
    @Shadow
    @Final
    @Mutable
    public static FoodProperties ENCHANTED_GOLDEN_APPLE;

    @Inject(method = "<clinit>", at = @At("TAIL"))
    private static void betterExperience$installBuffedEnchantedGoldenApple(CallbackInfo ci)
    {
        // 先抓下原版值, 配置关闭时由 ItemMixin 交还
        EnchantedGoldenAppleFoods.vanilla = ENCHANTED_GOLDEN_APPLE;
        ENCHANTED_GOLDEN_APPLE = EnchantedGoldenAppleFoods.buffed();
    }
}
