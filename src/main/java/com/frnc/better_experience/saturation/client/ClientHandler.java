package com.frnc.better_experience.saturation.client;

import com.frnc.better_experience.BetterExperience;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 客户端注册: 把饱和度读数挂到 HUD 上 */
@Mod.EventBusSubscriber(modid = BetterExperience.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientHandler
{
    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event)
    {
        // registerAboveAll: 画在原版所有 overlay 之上, 与原模组一致
        event.registerAboveAll("saturation", SaturationOverlay.INSTANCE);
    }
}
