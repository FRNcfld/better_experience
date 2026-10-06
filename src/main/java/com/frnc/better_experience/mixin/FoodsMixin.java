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
 * 强化附魔金苹果的食物属性 (来源: 【附魔金苹果重生】enchanted_golden_apple_reborn 1.4 的 FoodsMixin)。
 *
 * <p>在 {@code Foods.<clinit>} 的 TAIL 注入, 把 {@code Foods.ENCHANTED_GOLDEN_APPLE} 换成强化版。
 * 与原版 1.20.1 逐项对照如下 (改了五项, 防火与"可随时食用"与原版一致):
 *
 * <table>
 *   <tr><th>效果</th><th>强化版</th><th>原版</th></tr>
 *   <tr><td>生命恢复</td><td>1200 tick (60s), 等级 V</td><td>400 tick (20s), 等级 II</td></tr>
 *   <tr><td>抗性提升</td><td>6000 tick (300s), 等级 III</td><td>6000 tick (300s), 等级 I</td></tr>
 *   <tr><td>防火</td><td>6000 tick (300s), 等级 I</td><td>同</td></tr>
 *   <tr><td>伤害吸收</td><td>2400 tick (120s), 等级 V</td><td>2400 tick (120s), 等级 IV</td></tr>
 *   <tr><td>营养 / 饱和度系数</td><td>10 / 3.0</td><td>4 / 1.2</td></tr>
 *   <tr><td>可随时食用</td><td>是</td><td>同</td></tr>
 * </table>
 *
 * <p>等级换算: {@code MobEffectInstance} 的 amplifier 从 0 起算, 所以等级 III = amplifier 2、
 * 等级 V = amplifier 4。饱和度按 {@code 营养 × 饱和度系数 × 2} 结算并封顶在饥饿值,
 * 这里算出来是 60、远超上限, 实际就是把饱和度条拉满 (详见 {@link EnchantedGoldenAppleFoods#buffed()})。
 *
 * <p><strong>是否生效由配置项 {@code better_experience-common.toml} 的 {@code enchantedGoldenAppleBuffEnabled}
 * 决定, 默认开启。</strong> 但本类<strong>不读配置</strong>——{@code Foods.<clinit>} 与配置加载的
 * 先后顺序取决于 Forge 内部调度, 不可依赖。这里只做两件事: 把原版值快照下来, 然后无条件装上
 * 强化版; 真正的取舍推迟到 {@link ItemMixin} 在"取用食物属性"那一刻进行。
 *
 * <p><strong>关掉开关能回退到什么程度</strong>: 只回退"取用食物属性"这一条路径。
 * {@link ItemMixin} 拦的是 {@code Item#getFoodProperties()}, 而它是原版与 Forge 取食物属性的唯一入口
 * ({@code ItemStack#getFoodProperties} 最终也落到这里), 所以物品在游戏里的表现与判定 —— 能不能吃、
 * 吃下去给什么 —— 都回到原版。但<strong>静态字段本身仍是强化版</strong>, 而且 {@code Items} 初始化时
 * 已经把它烙进了 {@code Item} 自己的 {@code foodProperties} 字段: 任何<em>绕过</em>
 * {@code getFoodProperties()} 直接读这两处的第三方代码, 无论开关如何拿到的都是强化值。
 * 这是"改原版静态量"这一手法的固有代价, 换成不碰静态量的做法 (例如连 {@code Items} 的构造一起替换)
 * 与别的模组冲突只会更多, 因此维持现状。
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
