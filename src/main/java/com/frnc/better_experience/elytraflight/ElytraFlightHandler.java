package com.frnc.better_experience.elytraflight;

import com.frnc.better_experience.BetterExperience;
import com.frnc.better_experience.Config;
import com.frnc.better_experience.elytraflight.network.ElytraFlightNetwork;
import com.frnc.better_experience.elytraflight.network.ElytraFlightStatePacket;
import com.mojang.logging.LogUtils;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.network.PacketDistributor;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 鞘翅飞行开关 (按键切换, 热键默认不绑定, 需在「控制」中自行设置):
 *   - 开启时鞘翅飞行可正常触发; 关闭时无法起飞。
 *   - 开关状态每名玩家独立, 服务端权威; 热键切换只改内存态, 不写配置文件。
 *   - 拦截点为原版 {@code Player#tryToStartFallFlying} (见 mixin PlayerElytraFlightMixin),
 *     客户端与服务端共用同一入口, 故关闭时客户端不会发出 START_FALL_FLYING 包,
 *     服务端也不会进入滑翔 —— 换客户端无法绕过。
 *
 * 状态存放说明: 客户端镜像刻意放在这个通用类而不是 client/KeyInputHandler。
 * 拦截用的 mixin 注册在 better_experience.mixins.json 的 "mixins" (两侧加载), 其方法体
 * 不能引用客户端专属类, 否则服务端加载时会崩; 放在此处即可用
 * player.level().isClientSide 分支判断, 无需 DistExecutor。
 */
@Mod.EventBusSubscriber(modid = BetterExperience.MOD_ID)
public class ElytraFlightHandler
{
    private static final Logger LOGGER = LogUtils.getLogger();

    /** 服务端权威的每玩家开关; 未记录的玩家按默认 (开启) 处理 */
    private static final Map<UUID, Boolean> enabledByPlayer = new HashMap<>();

    /** 客户端本地的开关镜像 (由服务端经 ElytraFlightStatePacket 同步) */
    private static boolean clientEnabled = true;

    /**
     * 供 mixin 调用的统一查询: 按当前逻辑侧返回"是否允许起飞"。
     * 客户端与服务端各自持有状态 (客户端那份由 S→C 包同步), 两侧都会拦截。
     */
    public static boolean isFlightAllowed(Player player)
    {
        // 功能级总开关关闭时回到原版行为: 一律允许起飞
        if (!Config.elytraFlightEnabled) return true;

        if (player.level().isClientSide)
        {
            return clientEnabled;
        }
        return isEnabled(player);
    }

    /**
     * 服务端: 该玩家的鞘翅飞行开关 (未知玩家按默认开启)。
     *
     * <p>配置 {@code elytraFlightEnabled} 是<strong>功能级总开关</strong>: 关掉时这里恒为 true
     * (即回到"原版允许起飞"), 玩家在游戏内怎么按都关不掉。
     */
    public static boolean isEnabled(Player player)
    {
        if (!Config.elytraFlightEnabled) return true;
        return enabledByPlayer.getOrDefault(player.getUUID(), true);
    }

    /** 服务端: 切换该玩家的鞘翅飞行开关, 返回切换后的新状态 (功能被总开关关闭时恒为 true) */
    public static boolean toggleFlight(Player player)
    {
        if (!Config.elytraFlightEnabled)
        {
            LOGGER.info("[better_experience] 鞘翅飞行开关功能已被配置关闭, 忽略玩家 {} 的切换请求", player.getName().getString());
            return true;
        }

        boolean newState = !isEnabled(player);
        enabledByPlayer.put(player.getUUID(), newState);
        LOGGER.info("[better_experience] 玩家 {} 的鞘翅飞行开关: {}", player.getName().getString(), newState);
        return newState;
    }

    /** 客户端: 写入由服务端同步来的开关状态 */
    public static void setClientEnabled(boolean enabled)
    {
        clientEnabled = enabled;
    }

    /** 玩家登录时同步当前开关状态给客户端, 避免断线重连后客户端停留在旧状态 */
    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event)
    {
        if (event.getEntity() instanceof ServerPlayer serverPlayer)
        {
            ElytraFlightNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> serverPlayer),
                    new ElytraFlightStatePacket(isEnabled(serverPlayer)));
        }
    }
}
