package com.frnc.better_experience.chunkdevourer;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

/**
 * 区块吞噬者: 钻石级及以上镐子专属的范围删除附魔。
 *
 * <p>挖掉一个方块时, 把<strong>以该方块所在区块为中心</strong>的一整片区块从世界底部删到建筑上限。
 * 1 级 1 个区块、2 级 3×3 共 9 个、3 级 5×5 共 25 个。删除本身由 {@link ChunkDevourerHandler}
 * 与 {@link ChunkDevourerQueue} 负责, 本类只管附魔自己的规则。
 *
 * <p><strong>适用物品</strong>: 类别只能用原版的 {@link EnchantmentCategory#DIGGER}, 它太宽,
 * 所以 {@link #canEnchant} 在上面又叠了两道收窄 (镐子 + 钻石级及以上), 理由见那个方法。
 *
 * <p><strong>只能由猪灵交易获得</strong>: 原版没有"只准某个来源产出"的单一开关, 要靠四个方法联合作战,
 * 缺一个就会从别的口子漏出来 —— {@link #isTreasureOnly()} 挡附魔台,
 * {@link #isDiscoverable()} 挡战利品箱与钓鱼 ({@code EnchantRandomlyFunction} 按它过滤),
 * {@link #isTradeable()} 挡村民交易, {@link #canApplyAtEnchantingTable} 兜底。
 * 唯一产出点是 {@link ChunkDevourerBarter} 追加的猪灵交易池, 而且<strong>只出 1 级书</strong>。
 *
 * <p><strong>铁砧仍是必经的升级途径</strong>: {@code isDiscoverable} 为 false 不影响铁砧 ——
 * 合并附魔书走的是 {@code AnvilMenu}, 不经过被过滤的 {@code selectEnchantment}, 所以两本 1 级书
 * 照样能合到 2 级、3 级。{@link #isAllowedOnBooks()} 保持默认 true, 附魔书才承受得住这个附魔。
 *
 * <p>创造模式下 Forge 会依据 {@code allowedInCreativeTab} 自动把附魔书放进"原材料"标签页, 不必手动加。
 * 刻意不覆写 {@code checkCompatibility}, 默认只排斥自己, 所以与时运 / 精准采集都能共存。
 */
public class ChunkDevourerEnchantment extends Enchantment
{
    public ChunkDevourerEnchantment()
    {
        super(Rarity.VERY_RARE, EnchantmentCategory.DIGGER, new EquipmentSlot[] { EquipmentSlot.MAINHAND });
    }

    /** 1 级 1 个区块, 2 级 3×3, 3 级 5×5 */
    @Override
    public int getMaxLevel()
    {
        return 3;
    }

    /** 本附魔不进附魔台, 这两个值只用在铁砧与物品提示上; 等级间拉开差距, 让合到 3 级的代价随等级上升 */
    @Override
    public int getMinCost(int level)
    {
        return 30 + (level - 1) * 25;
    }

    @Override
    public int getMaxCost(int level)
    {
        return getMinCost(level) + 50;
    }

    /**
     * 只认<strong>钻石级及以上的镐子</strong>: 先过 {@code DIGGER} 类别再收窄到 {@code PickaxeItem}
     * (斧 / 锹 / 锄与镐一样都继承 {@code DiggerItem}, 光看类别会把它们放进来), 最后比等级。
     *
     * <p>等级用 {@code Tier#getLevel()} (原版 WOOD 0 / STONE 1 / IRON 2 / DIAMOND 3 / NETHERITE 4),
     * 所以 {@code >= Tiers.DIAMOND.getLevel()} 正好是"钻石及以上"; 注意 <strong>GOLD 是 0</strong>,
     * 金镐会被挡在外面。模组镐子如实实现 {@code getLevel()} 就能吃到, 返回 0 的会被挡下。
     *
     * <p>该方法被 Forge 标了 {@code @Deprecated} 并指向 {@code TierSortingRegistry}, 这里
     * <strong>刻意不用</strong>后者: 它的排序表是"注册 + 拓扑排序"两步算出来的, 在"已注册但排序表
     * 还没算出来"的时机 (首次资源重载前) 调 {@code getTiersLowerThan} 会拿到空表, 而"不在低于钻石的
     * 档里"是个<em>反向</em>判定 —— 空表会让它对所有等级都成立, 门槛朝松动的一侧失效。
     * {@code getLevel()} 是确定值, 失效方向是"太严", 可以接受。
     *
     * <p>铁砧那条路也堵住了: {@code AnvilMenu} 在"物品 + 附魔书"这一支会调本方法, 所以铁镐配这本
     * 附魔书合不出结果 ("书 + 书"那一支会被原版强制放行, 升级照常; 创造模式同样例外, 那是原版行为)。
     */
    @Override
    public boolean canEnchant(ItemStack stack)
    {
        Item item = stack.getItem();
        if (!EnchantmentCategory.DIGGER.canEnchant(item)) return false;
        if (!(item instanceof PickaxeItem pickaxe)) return false;
        return pickaxe.getTier().getLevel() >= Tiers.DIAMOND.getLevel();
    }

    /** 附魔台不给 (附魔台本来也刷不出本附魔, 这里是兜底) */
    @Override
    public boolean canApplyAtEnchantingTable(ItemStack stack)
    {
        return false;
    }

    /** 宝藏附魔: 附魔台不会刷出它的附魔书 */
    @Override
    public boolean isTreasureOnly()
    {
        return true;
    }

    /** 不可被随机附魔发现: 战利品箱 / 钓鱼都不会产出 */
    @Override
    public boolean isDiscoverable()
    {
        return false;
    }

    /** 村民不交易: 唯一的产出点是猪灵交易 */
    @Override
    public boolean isTradeable()
    {
        return false;
    }
}
