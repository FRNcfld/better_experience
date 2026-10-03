package com.frnc.better_experience.oceanblessing;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

/**
 * 海洋之祝: 三叉戟专属的辅助型附魔。
 *
 * <p>自身不改任何数值, 作用只有一条: 持有时解开三叉戟上另外三个附魔的限制 —— 激流不必在水中 / 雨中,
 * 引雷不必在雷雨天, 穿刺的加伤对任何目标都生效。判定落在 {@code TridentItemMixin}(激流两处)、
 * {@code ThrownTridentMixin} 与 {@code LightningRodBlockMixin}(引雷的两条落雷路径, 缺一不可)、
 * 以及 {@code EnchantmentHelperMixin}(穿刺), 由 {@link OceanBlessingRules} 统一读总开关。
 *
 * <p><strong>三叉戟专属是白送的</strong>: 类别用原版的 {@link EnchantmentCategory#TRIDENT},
 * 它的 {@code canEnchant} 就是 {@code instanceof TridentItem}, 不必自己写判定。
 *
 * <p><strong>获取方式按宝藏附魔处理</strong> ({@link #isTreasureOnly()} 返回 true): 附魔台不会刷出它的附魔书,
 * 只能靠战利品箱 / 钓鱼得到的附魔书, 再在铁砧上打到三叉戟上 —— 与原版 Mending 同一路子。
 *
 * <p><strong>创造模式不需要自己做任何事</strong>: Forge 会依据
 * {@code IForgeEnchantment#allowedInCreativeTab} 自动把附魔书加进原版的<strong>原材料</strong>标签页
 * ({@code CreativeModeTabs#INGREDIENTS}, 允许的类别是全部 {@code EnchantmentCategory}),
 * 本类默认的 {@code isAllowedOnBooks()} 为 true、类别 TRIDENT 也在其中, 因此是自动出现的。
 * 不要再手动往别的标签页里加一份。
 *
 * <p><strong>兼容性</strong>: 刻意不覆写 {@code checkCompatibility}。默认实现是 {@code this != pOther},
 * 激流那边的覆写只额外排斥 LOYALTY 与 CHANNELING, 所以本附魔与激流、引雷都能共存。
 * 但请注意<strong>激流与引雷在原版就是互斥的</strong> ({@code isCompatibleWith} 双向判定, 铁砧也合不上),
 * 因此三附魔同持不可能 —— 同一把三叉戟上, 激流与引雷的效果只能生效一个。
 */
public class OceanBlessingEnchantment extends Enchantment
{
    public OceanBlessingEnchantment()
    {
        super(Rarity.RARE, EnchantmentCategory.TRIDENT, new EquipmentSlot[] { EquipmentSlot.MAINHAND });
    }

    /** 效果是"开 / 关", 没有分级的余地, 所以只有 1 级 */
    @Override
    public int getMaxLevel()
    {
        return 1;
    }

    /** 与原版引雷同档的花费区间; 本附魔虽是宝藏附魔不进附魔台抽取, 这两个值仍会用于铁砧与物品提示 */
    @Override
    public int getMinCost(int level)
    {
        return 25;
    }

    @Override
    public int getMaxCost(int level)
    {
        return 50;
    }

    @Override
    public boolean isTreasureOnly()
    {
        return true;
    }
}
