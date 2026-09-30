package com.frnc.better_experience.spyglass.integration;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.fml.ModList;

/**
 * 查找玩家身上可用的望远镜。
 *
 * <p>两级策略:
 * <ol>
 *   <li>装了 <strong>Curios</strong> → 先查饰品栏 (belt 槽, 靠数据包标签把 {@code minecraft:spyglass} 标进去);</li>
 *   <li>否则 / 没查到 → 退化为主手、副手与背包查找。</li>
 * </ol>
 *
 * <p>本类<strong>不引用任何 Curios 类型</strong>, 引用只发生在 {@link CuriosLookup} 里,
 * 且必须先通过 {@link #isCuriosLoaded()} 判断 (见那个类的注释)。
 */
public final class SpyglassFinder
{
    private static final String CURIOS_MOD_ID = "curios";

    private SpyglassFinder()
    {
    }

    private static boolean isCuriosLoaded()
    {
        return ModList.get().isLoaded(CURIOS_MOD_ID);
    }

    /** 主手、副手或背包里是否有一副望远镜 */
    public static boolean hasSpyglassInInventory(Player player)
    {
        return findHotbarSlot(player) >= 0 || player.getOffhandItem().is(Items.SPYGLASS);
    }

    /**
     * 查找快捷栏 (槽位 0-8) 里望远镜所在的位置。
     *
     * @return 快捷栏下标, 没找到返回 -1
     */
    public static int findHotbarSlot(Player player)
    {
        for (int slot = 0; slot < 9; slot++)
        {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(Items.SPYGLASS)) return slot;
        }

        return -1;
    }

    /**
     * 玩家是否"有得用": 手持望远镜、装在饰品栏、或背包里有。
     *
     * <p>这是「按使用键能不能开镜」的判断依据。
     */
    public static boolean hasSpyglass(Player player)
    {
        if (player.isUsingItem() && player.getUseItem().is(Items.SPYGLASS)) return true;
        if (isCuriosLoaded() && CuriosLookup.hasSpyglass(player)) return true;

        return hasSpyglassInInventory(player);
    }
}
