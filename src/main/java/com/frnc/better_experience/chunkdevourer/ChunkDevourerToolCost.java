package com.frnc.better_experience.chunkdevourer;

/**
 * 区块吞噬者触发一次所付出的工具代价 (配置项 {@code chunkDevourerToolCost})。
 *
 * <p>{@link #DESTROY_TOOL} (默认) 直接损毁镐子, 一把镐换一次, 且无视耐久附魔;
 * {@link #SINGLE_DURABILITY} 不论删了多少格整次只扣 1 点耐久, 可被耐久附魔豁免。
 * 两种都不按格扣 (5×5 一次要多扣约 86 万点, 任何镐子都是秒碎), 创造模式两种都不生效。
 * 实现与这样定的理由见 {@code ChunkDevourerQueue#settleToolCost}。
 */
public enum ChunkDevourerToolCost
{
    DESTROY_TOOL,
    SINGLE_DURABILITY
}
