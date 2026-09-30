package com.frnc.better_experience.doublejump;

import com.frnc.better_experience.Config;
import com.frnc.better_experience.BetterExperience;
import com.frnc.better_experience.doublejump.network.DoubleJumpNetwork;
import com.frnc.better_experience.doublejump.network.DoubleJumpStatePacket;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.TickEvent.PlayerTickEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 二段跳服务端逻辑。
 * 垂直速度在客户端 LocalPlayer 上施加 (玩家移动为客户端权威), 本类负责:
 *   - 每名玩家的开关状态 (热键切换, 内存态; 未记录的玩家按配置默认值处理)
 *   - 收到 DoubleJumpPacket 时重置服务端摔落距离并记录次数 (保证摔落伤害正确)
 *   - 摔落伤害削减 (LivingFallEvent)
 *   - 玩家登录时把当前开关状态同步给客户端
 */
@Mod.EventBusSubscriber(modid = BetterExperience.MOD_ID)
public class JumpHandler
{
    private static final Logger LOGGER = LogUtils.getLogger();

    /** 服务端权威的每玩家二段跳开关 (内存态, 热键切换不写配置); 未记录的玩家按 defaultEnabled 处理 */
    private static final Map<UUID, Boolean> enabledByPlayer = new HashMap<>();

    /** 未记录玩家的默认开关, 由配置 doubleJumpEnabled 初始化 */
    private static boolean defaultEnabled = true;

    /** 每个玩家本次腾空已使用的二段跳次数 (>=1 即已用过, 落地重置为 0) */
    private static final Map<UUID, Integer> jumpCounts = new HashMap<>();

    public static void initFromConfig()
    {
        defaultEnabled = Config.doubleJumpEnabled;
        LOGGER.info("[better_experience] 二段跳默认状态: {}", defaultEnabled);
    }

    /**
     * 服务端: 该玩家的二段跳开关。
     *
     * <p>配置 {@code doubleJumpFeatureEnabled} 是<strong>功能级总开关</strong>: 关掉时这里恒为 false,
     * 玩家在游戏内怎么按都开不回来 (凌驾于每名玩家的开关与 {@code doubleJumpEnabled} 默认值之上)。
     */
    public static boolean isDoubleJumpEnabled(Player player)
    {
        if (!Config.doubleJumpFeatureEnabled) return false;
        return enabledByPlayer.getOrDefault(player.getUUID(), defaultEnabled);
    }

    /** 服务端: 切换该玩家的二段跳开关, 返回切换后的新状态 (功能被总开关关闭时恒为 false) */
    public static boolean toggleDoubleJump(Player player)
    {
        if (!Config.doubleJumpFeatureEnabled)
        {
            LOGGER.info("[better_experience] 二段跳功能已被配置关闭, 忽略玩家 {} 的切换请求", player.getName().getString());
            return false;
        }

        boolean newState = !isDoubleJumpEnabled(player);
        enabledByPlayer.put(player.getUUID(), newState);
        LOGGER.info("[better_experience] 玩家 {} 的二段跳开关: {}", player.getName().getString(), newState);
        return newState;
    }

    /** 服务端收到二段跳触发包后调用: 重置摔落距离并记录次数 (垂直速度已由客户端施加) */
    public static void handleDoubleJump(ServerPlayer player)
    {
        if (!isDoubleJumpEnabled(player)) return;

        UUID playerId = player.getUUID();
        int jumpCount = jumpCounts.getOrDefault(playerId, 0);
        if (jumpCount >= 1) return;    // 本次腾空已用过二段跳
        if (player.onGround()) return; // 需在空中

        player.fallDistance = 0;       // 重置服务端摔落距离, 使摔落伤害只按二段跳后的下落计算
        jumpCounts.put(playerId, jumpCount + 1);
        LOGGER.debug("[better_experience] {} 二段跳 (本次第 {})", player.getName().getString(), jumpCount + 1);
    }

    /** 玩家登录时同步当前开关状态给客户端 */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer serverPlayer)
        {
            DoubleJumpNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                    new DoubleJumpStatePacket(isDoubleJumpEnabled(serverPlayer)));
        }
    }

    /** 落地时重置跳跃次数 (END 阶段, 避免在 LivingFallEvent 之前重置导致削减失效) */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        if (player.level().isClientSide) return;

        if (player.onGround())
        {
            jumpCounts.put(player.getUUID(), 0);
        }
    }

    /** 二段跳后削减摔落伤害, 并重置次数 */
    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event)
    {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide) return;
        if (!isDoubleJumpEnabled(player)) return;

        UUID playerId = player.getUUID();
        int jumpCount = jumpCounts.getOrDefault(playerId, 0);
        if (jumpCount > 0 && event.getDistance() > 3.0F)
        {
            event.setDistance(event.getDistance() * 0.7F);
            LOGGER.debug("[better_experience] {} 摔落伤害由 {} 削减至 {}", player.getName().getString(), event.getDistance() / 0.7F, event.getDistance());
        }
        jumpCounts.put(playerId, 0);
    }
}
