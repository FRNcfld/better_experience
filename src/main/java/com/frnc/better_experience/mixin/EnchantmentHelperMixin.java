package com.frnc.better_experience.mixin;

import com.frnc.better_experience.oceanblessing.OceanBlessingRules;

import net.minecraft.world.entity.MobType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 海洋之祝 + 穿刺: 取消穿刺的加伤"只对水生生物生效"的限制。
 *
 * <p><strong>为什么注入在这里, 而不是直接改 {@code TridentImpalerEnchantment}:</strong>
 * 穿刺的判定是 {@code TridentImpalerEnchantment#getDamageBonus(int, MobType)} 里的
 * {@code pCreatureType == MobType.WATER}, 但那个重载<strong>没有 ItemStack 参数</strong>, 查不到三叉戟带没带海洋之祝;
 * 而 {@code EnchantmentHelper} 实际调的是带 ItemStack 的 3 参重载 —— 它是
 * {@code IForgeEnchantment} 的<em>接口默认方法</em> (仅 {@code self().m_7335_(level, mobType)}), 并不在
 * {@code Enchantment} 的字节码里, 因此也无法在穿刺类上注入它。
 * 唯一同时拿得到物品栈与 {@code MobType} 的地方就是这个静态入口, 所以把目标的 {@code MobType}
 * 换成 {@code WATER} 交给原版去算, 也就<strong>不必抄 2.5F 这个系数</strong>。
 *
 * <p><strong>换成 WATER 会不会误伤别的附魔:</strong> 不会。整个原版里对 {@code MobType} 敏感的加伤只有
 * {@code DamageEnchantment} 的亡灵杀手 / 节肢杀手 (类别 WEAPON), 而
 * {@code EnchantmentCategory.WEAPON.canEnchant} 是 {@code instanceof SwordItem} —— 三叉戟不在其中,
 * 所以这两个上不了三叉戟; 锋利虽然也是 WEAPON, 但它压根不看 {@code MobType}。
 * 因此对一把三叉戟而言, 受这次替换影响的只有穿刺自己。
 *
 * <p>顺带一个可见变化: 原版物品提示用 {@code MobType.UNDEFINED} 计算"主手伤害", 本来就不含穿刺加成;
 * 替换后提示里会把这部分算进去。由于加成此时确实对任何目标都生效, 提示反而更准确了。
 *
 * <p>目标为原版类, 方法名需经 better_experience.refmap.json 重映射到 SRG, 因此不可加 remap = false。
 */
@Mixin(EnchantmentHelper.class)
public abstract class EnchantmentHelperMixin
{
    @Inject(method = "getDamageBonus", at = @At("HEAD"), cancellable = true)
    private static void betterExperience$impalingAgainstAnyTarget(ItemStack stack, MobType mobType,
            CallbackInfoReturnable<Float> cir)
    {
        // 已经是 WATER 就直接放行: 下面那行会再进本方法一次, 靠这一条收住 (不会无限递归)
        if (mobType == MobType.WATER) return;
        if (!OceanBlessingRules.liftsImpalingMobTypeRestriction(stack)) return;

        cir.setReturnValue(EnchantmentHelper.getDamageBonus(stack, MobType.WATER));
    }
}
