package com.frnc.better_experience.spyglass.client;

import com.frnc.better_experience.BetterExperience;
import com.frnc.better_experience.ClientConfig;
import com.frnc.better_experience.spyglass.SpyglassState;
import com.frnc.better_experience.spyglass.integration.SpyglassFinder;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * 每 tick 维护「是否处于按键开镜状态」, 并在松开按键后把缩放值写回配置。
 *
 * <p>开镜的判定是<strong>按住期间持续为真</strong>: 只要功能开启、没有开着界面、按键按着、
 * 并且玩家身上 (手持 / 饰品栏 / 背包) 有望远镜。判定结果交给
 * {@link SpyglassState}, 由 mixin 去改写 {@code isScoping()} 与 FOV。
 *
 * <p>写回配置刻意放在<strong>松开按键的那一刻</strong>而不是滚轮每次变化时: 滚轮一次开镜可能调十几下,
 * 每下都写盘没有必要。
 */
@Mod.EventBusSubscriber(modid = BetterExperience.MOD_ID, value = Dist.CLIENT)
public class KeyInputHandler
{
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;

        if (player == null)
        {
            SpyglassState.setKeyHeld(false);
            return;
        }

        // 配置可能被改过 (例如把最大倍数调小了), 每次 tick 顺手钳制一下
        SpyglassZoom.reclamp();

        boolean wasScoping = SpyglassState.isForcedScoping();

        boolean scoping = ClientConfig.spyglassEnabled
                && mc.screen == null
                && ClientHandler.USE_KEY.isDown()
                && SpyglassFinder.hasSpyglass(player);

        SpyglassState.setKeyHeld(scoping);

        if (wasScoping && !scoping)
        {
            SpyglassZoom.persistIfDirty();
        }
    }
}
