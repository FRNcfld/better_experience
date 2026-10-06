package com.frnc.better_experience.saturation;

import com.frnc.better_experience.BetterExperience;
import com.frnc.better_experience.BetterExperienceServerConfig;
import com.frnc.better_experience.saturation.network.SaturationNetwork;
import com.frnc.better_experience.saturation.network.SaturationSyncPacket;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.food.FoodData;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 饱和度机制的公共常量与查询, 外加服务端→客户端的数值同步。
 *
 * <p><strong>为什么必须自己同步</strong>: 原版 {@code ServerPlayer} 只在
 * {@code health 变化 || foodLevel 变化 || 饱和度是否恰为 0 的标记翻转} 时才发
 * {@code ClientboundSetHealthPacket}。也就是说饱和度从 20 涨到 50 这种变化<strong>根本不会</strong>告诉客户端,
 * 客户端的 HUD 会一直显示旧值。
 *
 * <p>原模组为此每 tick 给每名玩家发 3 个包 (saturation / exhaustion / 消耗上限); 这里改成
 * <strong>一个合并包、并且只在数值真的变化时才发</strong>, 用 {@code Map<UUID, ...>} 记录上次发送值
 * (写法参照 {@code doublejump/JumpHandler.java})。消耗上限不是玩家的实际数值, 只是给 HUD 算百分比用,
 * 客户端自己也有配置能算——但它随服务端配置走才准确, 所以一并同步。
 *
 * <p>消耗度还要再退一步: 它在疾跑时<strong>每 tick 都在涨</strong>, "变了就发"实际等于每 tick 一个包,
 * 所以它按游戏刻抽样 (见 {@link #EXHAUSTION_SYNC_INTERVAL_TICKS})。
 */
@Mod.EventBusSubscriber(modid = BetterExperience.MOD_ID)
public final class SaturationHandler
{
    /** 原版饥饿值上限, 与 {@code FoodData.eat} 里写死的那个 20 对应 */
    public static final int MAX_FOOD_LEVEL = 20;

    /** 饱和度「软上限」: 超过它时饱和药水改走衰减公式 (原模组用的就是 20) */
    public static final float SATURATION_SOFT_CAP = 20.0F;

    /**
     * 消耗度最多每多少游戏刻同步一次。
     *
     * <p>疾跑时原版每 tick 给消耗度加 0.1 (见 {@code FoodData#addExhaustion}), 所以对消耗度用
     * "变了就发"等于每 tick 一个包 —— 那是原模组的老问题换了个样子。
     */
    private static final int EXHAUSTION_SYNC_INTERVAL_TICKS = 5;

    /** 上次同步给各玩家的数值 */
    private static final Map<UUID, SyncedValues> lastSynced = new HashMap<>();

    private SaturationHandler()
    {
    }

    /**
     * 消耗度上限。配置 ≤ 0 时返回 {@link Float#MAX_VALUE}, 使
     * {@code if (exhaustionLevel > cap)} 恒假, 等于彻底禁用消耗 (与原模组语义一致)。
     */
    public static float maxExhaustion()
    {
        int configured = BetterExperienceServerConfig.saturationMaxExhaustion;
        return configured > 0 ? (float) configured : Float.MAX_VALUE;
    }

    /** 服务端: 数值变化时把服务端权威值推给该玩家 */
    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (!BetterExperienceServerConfig.saturationEnabled) return;

        FoodData food = player.getFoodData();
        float saturation = food.getSaturationLevel();
        float exhaustion = food.getExhaustionLevel();
        float maxExhaustion = maxExhaustion();

        SyncedValues cached = lastSynced.get(player.getUUID());
        if (cached != null && !shouldResend(cached, saturation, exhaustion, maxExhaustion, player)) return;

        lastSynced.put(player.getUUID(), new SyncedValues(saturation, exhaustion, maxExhaustion));
        SaturationNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                new SaturationSyncPacket(saturation, exhaustion, maxExhaustion));
    }

    /**
     * 是否该重发一次。
     *
     * <p>饱和度与消耗上限一变就发: 它们是玩家能直接感知的跳变。
     *
     * <p>消耗度则按游戏刻抽样。包是三个值一起发的, 所以抽样期间只要饱和度变了, 消耗度也会顺带刷新 ——
     * 抽样只会让刷新变稀, 不会漏掉任何一次"玩家真正看得出来的变化"。
     */
    private static boolean shouldResend(SyncedValues cached, float saturation, float exhaustion,
            float maxExhaustion, ServerPlayer player)
    {
        if (saturation != cached.saturation() || maxExhaustion != cached.maxExhaustion()) return true;
        if (exhaustion == cached.exhaustion()) return false;
        return player.level().getGameTime() % EXHAUSTION_SYNC_INTERVAL_TICKS == 0L;
    }

    /**
     * 玩家退出时丢掉他那一行的缓存。
     *
     * <p>这纯粹是"上次发了什么"的记录, 人走了就没有意义, 不清就会随历史玩家数一直长。
     * 顺带还堵掉一个隐患: 客户端那份 {@code ClientSaturation} 是静态的、换服务器不会自己归零,
     * 若玩家重进后数值恰好与上次相同, 留着缓存就会判定为"没变"而不发包, HUD 于是一直显示上一次连接的读数。
     */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event)
    {
        lastSynced.remove(event.getEntity().getUUID());
    }

    /** 上次同步的三个值; record 自带 equals, 直接拿来做变化检测 */
    private record SyncedValues(float saturation, float exhaustion, float maxExhaustion)
    {
    }
}
