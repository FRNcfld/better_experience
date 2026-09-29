package com.frnc.better_experience;

import com.frnc.better_experience.doublejump.JumpHandler;
import com.frnc.better_experience.doublejump.network.DoubleJumpNetwork;
import com.frnc.better_experience.elytraflight.network.ElytraFlightNetwork;
import com.mojang.logging.LogUtils;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

/**
 * Better Experience 主入口。
 *
 * <p>本 mod 是 1.20.1 / Forge 下的原版体验优化模组，不依赖 Create 等第三方前置。当前内容：
 * <ul>
 *   <li><b>二段跳</b>：空中再按跳跃键触发一次，J 键开关，服务端权威。</li>
 *   <li><b>鞘翅飞行开关</b>：H 键开关，关闭时无法起飞（拦截原版 tryToStartFallFlying）。</li>
 *   <li><b>掉落物定时清理</b>：维度/物品黑白名单 + 命名/新鲜/死亡掉落保护，数据包驱动。</li>
 *   <li><b>附魔金苹果强化</b>：生命恢复由 II 级 20 秒改为 V 级 60 秒，可配置开关。</li>
 * </ul>
 */
@Mod(BetterExperience.MOD_ID)
public class BetterExperience {

    /** 本 mod 的命名空间，需与 META-INF/mods.toml 中的 modId 一致。 */
    public static final String MOD_ID = "better_experience";

    private static final Logger LOGGER = LogUtils.getLogger();

    /** 快捷创建本 mod 的 ResourceLocation。 */
    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    public BetterExperience(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        modEventBus.addListener(this::commonSetup);

        // 所有开关集中在 COMMON 配置: config/better_experience-common.toml
        context.registerConfig(ModConfig.Type.COMMON, Config.SPEC);

        // 二段跳 / 鞘翅飞行开关的网络通道（各一条）
        DoubleJumpNetwork.register();
        ElytraFlightNetwork.register();

        MinecraftForge.EVENT_BUS.register(this);

        LOGGER.info("[BetterExperience] initialized");
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        // 二段跳的默认开关取自配置（此后每名玩家可用热键各自切换）
        JumpHandler.initFromConfig();

        LOGGER.info("[BetterExperience] common setup done");
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("[BetterExperience] server starting");
    }
}
