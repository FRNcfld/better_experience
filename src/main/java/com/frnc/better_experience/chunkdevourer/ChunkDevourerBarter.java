package com.frnc.better_experience.chunkdevourer;

import com.frnc.better_experience.BetterExperience;
import com.frnc.better_experience.BetterExperienceServerConfig;

import net.minecraft.world.item.Items;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.entries.LootPoolEntryContainer;
import net.minecraft.world.level.storage.loot.entries.LootPoolSingletonContainer;
import net.minecraft.world.level.storage.loot.functions.SetEnchantmentsFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraftforge.event.LootTableLoadEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 区块吞噬者的唯一产出点: 给原版猪灵交易表追加一个附魔书条目。
 *
 * <p><strong>为什么挂事件而不是覆盖数据包 JSON</strong>: 猪灵交易的产出是原版战利品表
 * {@code minecraft:gameplay/piglin_bartering} (见 {@code PiglinAi#getBarterResponseItems})。
 * 自己写一份同名 JSON 放进 {@code data/minecraft/loot_tables/} 会把整张表<strong>顶掉</strong> ——
 * 别的模组往猪灵交易里加的东西会一起消失。走 {@link LootTableLoadEvent} 则只是"再挂一个池子", 互不干扰。
 *
 * <p>Forge 对这张表确实会抛事件: {@code ForgeHooks#loadLootTable} 里是
 * {@code if (!custom) ret = ForgeEventFactory.loadLootTable(...)}, 而 {@code custom} 表示"来自存档数据包"。
 * 原版这张表属于内置数据包, 因此 {@code custom == false}, 事件会抛; 而且 {@code ret.freeze()} 在事件
 * <em>之后</em> 才调用, 所以这里调 {@code addPool} 是合法的 (freeze 之后再改才会抛异常)。
 *
 * <p><strong>为什么要额外加一个"随机概率"条件</strong>: 池子的权重只在池子<em>内部</em>参与抽取,
 * 池子与池子之间是各自独立滚动的。原版表只有一个池 (18 个条目, 权重合计 459), 我们再挂一个
 * 只有 1 个条目的池, 那个池每次交易都会稳稳命中 —— 那就成了每交易必出。
 * 加上 {@link LootItemRandomChanceCondition} 之后, 命中率取
 * {@code 权重 / (459 + 权重)}, 与"直接往原版池里塞一个同权重条目"完全等价。
 *
 * <p><strong>只出 1 级书</strong>: 这里固定 {@code ConstantValue.exactly(1.0F)} 而不是给个等级区间 ——
 * 2 级 / 3 级必须是<em>合出来的</em>。因为附魔本身设了 {@code isDiscoverable = false} 与
 * {@code isTradeable = false}, 铁砧已经是唯一的升级途径 (两本 1 级合 2 级, 两本 2 级合 3 级),
 * 让交易直接吐高级书会把那条路整个跳过。代价是一把 3 级镐要 4 本书, 而镐子每触发一次就损毁,
 * 所以权重默认给得比一般稀有附魔宽松 (见 {@code chunkDevourerBarterWeight})。
 */
@Mod.EventBusSubscriber(modid = BetterExperience.MOD_ID)
public class ChunkDevourerBarter
{
    /** 原版 {@code piglin_bartering} 表里 18 个条目的权重合计, 用来把条目权重换算成命中率 */
    private static final float VANILLA_ENTRY_WEIGHT = 459.0F;

    /** 池子名。Forge 的 {@code LootTable#addPool} 会拒绝重名, 所以带上命名空间保证唯一 */
    private static final String POOL_NAME = BetterExperience.MOD_ID + ":chunk_devourer";

    private ChunkDevourerBarter()
    {
    }

    @SubscribeEvent
    public static void onLootTableLoad(LootTableLoadEvent event)
    {
        if (!BuiltInLootTables.PIGLIN_BARTERING.equals(event.getName())) return;
        if (!ChunkDevourerEnchantments.CHUNK_DEVOURER.isPresent()) return;

        float weight = Math.max(1, BetterExperienceServerConfig.chunkDevourerBarterWeight);
        float chance = weight / (VANILLA_ENTRY_WEIGHT + weight);

        // 原版猪灵交易里的附魔书就是 "book + 附魔函数": SetEnchantmentsFunction 看到一个普通书物品时,
        // 会把它换成附魔书再写入附魔, 与 minecraft:enchant_randomly 同款用法。
        // 等级写死 1 级 —— 2 / 3 级只能靠铁砧合出来, 原因见类注释。
        LootPoolSingletonContainer.Builder<?> book = LootItem.lootTableItem(Items.BOOK);
        LootPoolEntryContainer.Builder<?> entry = book.apply(new SetEnchantmentsFunction.Builder()
                .withEnchantment(ChunkDevourerEnchantments.CHUNK_DEVOURER.get(),
                        ConstantValue.exactly(1.0F)));

        event.getTable().addPool(LootPool.lootPool()
                .name(POOL_NAME)
                .setRolls(ConstantValue.exactly(1.0F))
                .when(LootItemRandomChanceCondition.randomChance(chance))
                .add(entry)
                .build());
    }
}
