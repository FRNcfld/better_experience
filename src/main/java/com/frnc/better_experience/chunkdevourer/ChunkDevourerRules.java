package com.frnc.better_experience.chunkdevourer;

import com.frnc.better_experience.BetterExperienceServerConfig;

import net.minecraft.world.entity.player.Player;

/**
 * 区块吞噬者的规则入口: "等级 ↔ 区块范围"的映射, 以及总开关的唯一读取点 (读本侧的 COMMON 配置,
 * 与其他功能一致)。处理器与队列都不直接碰配置里的开关, 一律走这里, 免得漏判。
 */
public final class ChunkDevourerRules
{
    private ChunkDevourerRules()
    {
    }

    /**
     * 附魔等级 → 以被挖方块所在区块为中心的半径 (单位: 区块): 1 级 0 (1 个区块), 2 级 1 (3×3 共 9 个),
     * 3 级 2 (5×5 共 25 个)。边长是 {@code 2 * radius + 1}, 所以"1X1 / 3X3 / 5X5"是这条规则的自然结果。
     *
     * <p>夹紧到 0 ~ 2 是因为等级来自物品 NBT, 指令可以塞进 4 级甚至负数 ——
     * 不夹的话 4 级会变成 7×7 共 49 个区块。
     */
    public static int chunkRadiusOf(int enchantLevel)
    {
        return Math.max(0, Math.min(2, enchantLevel - 1));
    }

    /** 这名玩家这一下该触发的附魔等级 (0 = 不触发); 只看主手, 附魔本身就只声明了 MAINHAND */
    public static int triggerLevel(Player player)
    {
        if (!BetterExperienceServerConfig.chunkDevourerEnabled) return 0;
        return ChunkDevourerEnchantments.levelOf(player.getMainHandItem());
    }
}
