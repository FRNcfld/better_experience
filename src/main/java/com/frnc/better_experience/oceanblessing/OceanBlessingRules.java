package com.frnc.better_experience.oceanblessing;

import com.frnc.better_experience.BetterExperienceServerConfig;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;

/**
 * 海洋之祝"解开环境限制"的规则入口。两个 mixin 只调这里, 总开关也就只需在一处读取。
 *
 * <p>总开关读的是<strong>本侧</strong>的 COMMON 配置 —— 本模组的配置不跨端同步, 与仓库既有约定一致。
 * 它同时管住激流与引雷两处限制。
 */
public final class OceanBlessingRules
{
    private OceanBlessingRules()
    {
    }

    /**
     * 玩家是否正握着一把带海洋之祝的三叉戟, 因而可以无视激流"必须在水中或雨中"的要求。
     *
     * <p>要同时看"正在使用的物品"和两只手, 原因是原版两个判定点的时机不同:
     * <ul>
     *   <li>{@code TridentItem#use} 的判定发生在 {@code startUsingItem} <strong>之前</strong>,
     *       那一刻 {@code getUseItem()} 还没指向三叉戟, 只有手上查得到;</li>
     *   <li>{@code TridentItem#releaseUsing} 期间 {@code getUseItem()} 才是指向它的可靠来源。</li>
     * </ul>
     *
     * <p><strong>已知的一点宽松</strong>: {@code use} 那一处是 {@code @Redirect} 注入,
     * 处理器能拿到的只有 {@code Player}, 拿不到"用的是哪只手"。因此这里是按双手扫描判定的 ——
     * 若一只手握着无附魔的激流三叉戟、另一只手握着带海洋之祝的三叉戟, 也会放行。
     * 要消掉这点宽松就得改成 {@code @Inject} 重写 {@code use} 的守卫 (即把原版的耐久判定与
     * {@code getRiptide} 判定抄一份), 权衡后认为不值得 —— 这个组合本身已相当于"手上有海洋之祝"。
     */
    public static boolean liftsRiptideWaterRestriction(Player player)
    {
        if (!BetterExperienceServerConfig.oceanBlessingEnabled) return false;
        return OceanBlessingEnchantments.hasOceanBlessing(player.getUseItem())
                || OceanBlessingEnchantments.hasOceanBlessing(player.getMainHandItem())
                || OceanBlessingEnchantments.hasOceanBlessing(player.getOffhandItem());
    }

    /**
     * 这枚被投出的三叉戟是否带海洋之祝, 因而引雷可以无视"雷雨天"的要求。
     *
     * <p>引雷有<strong>两条</strong>落雷路径, 两条都用这个入口判定, 少一条就会表现为"部分场景不生效":
     * 命中生物时走 {@code ThrownTrident#onHitEntity}, 命中避雷针方块时走
     * {@code LightningRodBlock#onProjectileHit}。
     */
    public static boolean liftsChannelingWeatherRestriction(ItemStack thrownTrident)
    {
        return BetterExperienceServerConfig.oceanBlessingEnabled
                && OceanBlessingEnchantments.hasOceanBlessing(thrownTrident);
    }

    /**
     * 这把三叉戟是否该把<strong>任意</strong>目标都当作水生生物来算穿刺加伤,
     * 即同时带着穿刺与海洋之祝。
     *
     * <p>额外要求带穿刺: 没带穿刺时换不换 {@code MobType} 都没有意义, 先挡掉可以少走一次重算。
     */
    public static boolean liftsImpalingMobTypeRestriction(ItemStack stack)
    {
        if (!BetterExperienceServerConfig.oceanBlessingEnabled) return false;
        if (EnchantmentHelper.getItemEnchantmentLevel(Enchantments.IMPALING, stack) <= 0) return false;
        return OceanBlessingEnchantments.hasOceanBlessing(stack);
    }
}
