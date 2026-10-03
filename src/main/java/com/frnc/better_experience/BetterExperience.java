package com.frnc.better_experience;

import com.frnc.better_experience.doublejump.JumpHandler;
import com.frnc.better_experience.doublejump.network.DoubleJumpNetwork;
import com.frnc.better_experience.elytraflight.network.ElytraFlightNetwork;
import com.frnc.better_experience.oceanblessing.OceanBlessingEnchantments;
import com.frnc.better_experience.saturation.network.SaturationNetwork;
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
import net.minecraftforge.fml.loading.FMLEnvironment;
import org.slf4j.Logger;

/**
 * Better Experience 主入口。
 *
 * <p>本 mod 是 1.20.1 / Forge 下的原版体验优化模组，不依赖 Create 等第三方前置。当前内容：
 * <ul>
 *   <li><b>二段跳</b>：空中再按跳跃键触发一次，可按键开关，服务端权威。</li>
 *   <li><b>鞘翅飞行开关</b>：可按键开关，关闭时无法起飞（拦截原版 tryToStartFallFlying）。</li>
 *   <li><b>掉落物定时清理</b>：维度/物品黑白名单 + 命名/新鲜/死亡掉落保护，数据包驱动。</li>
 *   <li><b>附魔金苹果强化</b>：生命恢复 V 级 60 秒、抗性提升 III 级、伤害吸收 V 级，营养 10 / 饱和度满，可配置开关。</li>
 *   <li><b>平坦基岩</b>：主世界底部 / 下界顶底只生成 1 层基岩，可配置开关；仅影响新生成的区块。</li>
 *   <li><b>亮度上限扩展</b>：伽马上限由原版 100% 放宽到 1000%，可按键开关，按住热键滚轮可调值；
 *       Sodium / Rubidium / Embeddium 的界面里滑块仍是 0–100%（本模组不修改任何界面类），那些环境用热键调值。</li>
 *   <li><b>上坡辅助</b>：可按键循环「关闭 / 上坡 / 自动跳跃」三模式，行走·潜行·疾跑三档高度可配置。</li>
 *   <li><b>饱和度机制</b>：饱和度不被饥饿值封顶，饥饿满时进食可溢出转饱和度，HUD 显示读数。</li>
 *   <li><b>望远镜改进</b>：不手持也能开镜，滚轮缩放并记住设置，可显示准星与倍数。</li>
 *   <li><b>海洋之祝</b>：新增的三叉戟专属附魔，持有时解除激流的水 / 雨限制、引雷的雷雨天限制，并让穿刺的加伤对所有目标生效。</li>
 * </ul>
 *
 * <p>每一项都能在 {@code config/better_experience-common.toml} 里整体关闭（功能级总开关，玩家在
 * 游戏内无法重新打开）。所有热键<strong>默认均不绑定</strong>，需玩家自行在「控制」中设置。
 */
@Mod(BetterExperience.MOD_ID)
public class BetterExperience {

    /** 本 mod 的命名空间，需与 META-INF/mods.toml 中的 modId 一致。 */
    public static final String MOD_ID = "better_experience";

    private static final Logger LOGGER = LogUtils.getLogger();

    /** 快捷创建本 mod 的 ResourceLocation。 */
    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }

    public BetterExperience(FMLJavaModLoadingContext context) {
        IEventBus modEventBus = context.getModEventBus();

        modEventBus.addListener(this::commonSetup);

        // 服务端/世界机制集中在 COMMON 配置: config/better_experience-common.toml
        context.registerConfig(ModConfig.Type.COMMON, BetterExperienceServerConfig.SPEC);

        // 纯客户端偏好单独一份: config/better_experience-client.toml (望远镜缩放等)。
        // CLIENT 类型在专用服务器上不存在, 所以只在客户端注册, 避免服务端刷无意义的警告。
        if (FMLEnvironment.dist.isClient())
        {
            context.registerConfig(ModConfig.Type.CLIENT, BetterExperienceClientConfig.SPEC);
        }

        // 二段跳 / 鞘翅飞行开关 / 饱和度同步的网络通道（各一条）
        DoubleJumpNetwork.register();
        ElytraFlightNetwork.register();
        SaturationNetwork.register();

        // 海洋之祝附魔（三叉戟专属，本 mod 唯一的注册表内容）
        OceanBlessingEnchantments.register(modEventBus);

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
