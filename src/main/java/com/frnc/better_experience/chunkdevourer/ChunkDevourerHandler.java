package com.frnc.better_experience.chunkdevourer;

import com.frnc.better_experience.BetterExperience;
import com.frnc.better_experience.BetterExperienceServerConfig;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 区块吞噬者的两个入口: 玩家挖到方块时入队, 服务端每 tick 推进队列。
 *
 * <p>本模组的其余功能多用 mixin, 这里改用 Forge 事件, 原因是这两个切入点<strong>本来就有现成的事件</strong>:
 * {@code BlockEvent.BreakEvent} 覆盖了玩家破坏方块的全部路径 (含创造模式秒破与"挖穿"判定的那两条),
 * 而删除动作又必须能被别的模组拦下, 用事件比注入原版方法更不容易和别的模组打架。
 * 本功能因此<strong>零 mixin</strong>。
 *
 * <p>仅服务端生效: {@code BlockEvent.BreakEvent} 只由 {@code ServerPlayerGameMode} 抛出,
 * {@code ServerTickEvent} 也只在与 {@code MinecraftServer} 关联的物理服务端触发。
 */
@Mod.EventBusSubscriber(modid = BetterExperience.MOD_ID)
public class ChunkDevourerHandler
{
    private ChunkDevourerHandler()
    {
    }

    /**
     * 挖到方块 → 把"删掉这片区块"排进队列。
     *
     * <p>优先级取 {@link EventPriority#LOWEST} 是<strong>刻意</strong>的: 这样本处理器一定跑在
     * 领地 / 保护类模组之后, 才能读到它们取消事件的结果。若抢在前面入队, 后面的取消就拦不住我们 ——
     * 队列里的删除走的是 {@code Level#setBlock}, 会绕过一切保护判定。
     *
     * <p>触发与否只看主手物品带不带本附魔 (见 {@link ChunkDevourerRules#triggerLevel}),
     * 所以总开关关闭、或手上不是带附魔的镐子, 都在那一步就返回了。
     */
    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onBlockBreak(BlockEvent.BreakEvent event)
    {
        if (event.isCanceled()) return;
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;

        int enchantLevel = ChunkDevourerRules.triggerLevel(player);
        if (enchantLevel <= 0) return;

        // 同一个玩家不叠第二个任务。默认配置下镐子触发一次就碎了, 本来也触发不了第二次;
        // 但若把代价调成"只扣 1 点耐久", 连着挖就会一份份往队列里堆, 后面的人得排很久。
        if (ChunkDevourerQueue.isBusy(player)) return;

        ChunkDevourerQueue.enqueue(enchantLevel, player, event.getPos());
    }

    /**
     * 推进删除队列。
     *
     * <p>总开关关掉时<strong>直接清空队列</strong>, 而不是把跑到一半的任务冻在半路 ——
     * 关掉开关的场合多半是"这玩意儿太卡了", 那就该立刻收手, 而不是留一个再也不动的任务占着静态表。
     */
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END) return;

        if (!BetterExperienceServerConfig.chunkDevourerEnabled)
        {
            ChunkDevourerQueue.clearAll();
            return;
        }

        ChunkDevourerQueue.tick(event.getServer());
    }

    /**
     * 服务器停下时清空队列。
     *
     * <p>队列是静态的, 而维度键 {@code minecraft:overworld} 跨存档、跨重启都一样。若不清,
     * 关掉世界时那个"跑到一半"的任务会留在内存里, 下次进任意一个存档时被 {@code server.getLevel} 认领并接着删 ——
     * 坐标对不上就会误删新世界的地形。顺带也解决了"任务卡住导致该玩家再也触发不了"的问题。
     */
    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event)
    {
        ChunkDevourerQueue.clearAll();
    }
}
