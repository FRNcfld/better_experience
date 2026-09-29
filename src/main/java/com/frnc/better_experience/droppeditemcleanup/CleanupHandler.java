package com.frnc.better_experience.droppeditemcleanup;

import com.frnc.better_experience.Config;
import com.frnc.better_experience.BetterExperience;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * 掉落物自动清理 (参考 CleanMaid 的 CleanupModule):
 *   - 按配置间隔 (cleanupIntervalSeconds) 定时清理全部已加载维度的掉落物 (ItemEntity)
 *   - 分批处理 (cleanupMaxItemsPerBatch 每 tick), 避免主线程卡顿
 *   - 维度/物品黑白名单 (CleanupLists, 数据包 data/better_experience/dropped_item_cleanup/*.json)
 *   - 保护规则: 命名物品 / 新鲜掉落 / 死亡掉落 (DeathDropProtection)
 *   - 清理前 cleanupWarningSeconds 秒广播一次预警 (消息走语言文件 message.better_experience.cleanup_warning)
 *   - 清理完成广播结果
 * 仅服务端执行 (ServerTickEvent 只在与 MinecraftServer 关联的物理服务端触发)。
 */
@Mod.EventBusSubscriber(modid = BetterExperience.MOD_ID)
public class CleanupHandler
{
    private static final int TICKS_PER_SECOND = 20;

    /** 距上次清理已流逝的 tick 数 (每次清理后归零) */
    private static int elapsedTicks = 0;
    /** 是否正在执行清理 (分批进行中) */
    private static boolean cleaning = false;
    /** 待清理的掉落物队列 */
    private static final Deque<ItemEntity> pendingItems = new ArrayDeque<>();
    private static int totalRemoved = 0;
    private static long cleanupStartTimeMillis = 0;

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END) return;
        MinecraftServer server = event.getServer();
        if (server == null) return;
        if (!Config.cleanupEnabled) return;

        // 定期清理已过保护期的死亡掉落记录
        DeathDropProtection.cleanupExpired(server.overworld().getGameTime());

        if (cleaning)
        {
            processBatch();
            if (pendingItems.isEmpty())
            {
                finishCleanup(server);
            }
        }
        else
        {
            elapsedTicks++;
            int intervalTicks = Math.max(1, Config.cleanupIntervalSeconds) * TICKS_PER_SECOND;

            // 清理前预警: 剩余时间恰等于 cleanupWarningSeconds 时广播一次
            int warningSeconds = Config.cleanupWarningSeconds;
            if (warningSeconds > 0
                    && warningSeconds < Config.cleanupIntervalSeconds
                    && elapsedTicks == intervalTicks - warningSeconds * TICKS_PER_SECOND)
            {
                server.getPlayerList().broadcastSystemMessage(
                        Component.translatable("message.better_experience.cleanup_warning", warningSeconds), false);
            }

            if (elapsedTicks >= intervalTicks)
            {
                elapsedTicks = 0;
                startCleanup(server);
            }
        }
    }

    /** 启动一次清理: 收集受维度过滤的掉落物 */
    private static void startCleanup(MinecraftServer server)
    {
        cleaning = true;
        totalRemoved = 0;
        cleanupStartTimeMillis = System.currentTimeMillis();
        pendingItems.clear();
        for (ServerLevel level : server.getAllLevels())
        {
            if (!shouldCleanDimension(level.dimension().location())) continue;
            pendingItems.addAll(level.getEntities(EntityType.ITEM, e -> true));
        }
    }

    /** 每 tick 移除一批掉落物 (上限 cleanupMaxItemsPerBatch) */
    private static void processBatch()
    {
        int batch = 0;
        while (batch < Config.cleanupMaxItemsPerBatch && !pendingItems.isEmpty())
        {
            ItemEntity item = pendingItems.poll();
            if (item != null && item.isAlive() && shouldRemove(item))
            {
                item.discard();
                totalRemoved++;
            }
            batch++;
        }
    }

    /** 维度过滤: 黑名单命中→清理; 白名单命中→跳过; 默认→清理 */
    private static boolean shouldCleanDimension(ResourceLocation dimensionId)
    {
        if (Config.cleanupDimensionBlacklistEnable && CleanupLists.isDimensionBlacklisted(dimensionId))
        {
            return true;
        }
        if (Config.cleanupDimensionWhitelistEnable && CleanupLists.isDimensionWhitelisted(dimensionId))
        {
            return false;
        }
        return true;
    }

    /** 判定掉落物是否应被清理 (物品黑白名单 + 保护规则) */
    private static boolean shouldRemove(ItemEntity item)
    {
        ResourceLocation itemId = ForgeRegistries.ITEMS.getKey(item.getItem().getItem());

        // 物品白名单 → 受保护 (命中即免清理)
        if (Config.cleanupItemWhitelistEnable && itemId != null && CleanupLists.isItemWhitelisted(itemId))
        {
            return false;
        }
        // 物品黑名单 → 必须清理 (命中即清, 无视后续保护)
        if (Config.cleanupItemBlacklistEnable && itemId != null && CleanupLists.isItemBlacklisted(itemId))
        {
            return true;
        }
        // 命名物品保护
        if (Config.cleanupProtectNamedItems && item.hasCustomName())
        {
            return false;
        }
        // 新鲜掉落保护
        int minAgeTicks = Math.max(0, Config.cleanupMinimumItemAgeSeconds) * TICKS_PER_SECOND;
        if (item.tickCount < minAgeTicks)
        {
            return false;
        }
        // 死亡掉落保护
        if (Config.cleanupProtectPlayerDeathDrops
                && DeathDropProtection.isProtected(item.getUUID(), item.level().getGameTime()))
        {
            return false;
        }
        return true;
    }

    /** 清理完成, 广播结果 */
    private static void finishCleanup(MinecraftServer server)
    {
        cleaning = false;
        long durationMs = System.currentTimeMillis() - cleanupStartTimeMillis;
        if (Config.cleanupBroadcastResult && totalRemoved > 0)
        {
            server.getPlayerList().broadcastSystemMessage(
                    Component.translatable("message.better_experience.cleanup_result",
                            totalRemoved, String.format("%.2f", durationMs / 1000.0)),
                    false);
        }
    }
}
