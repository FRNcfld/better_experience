package com.frnc.better_experience.spyglass.client;

import com.frnc.better_experience.BetterExperience;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterGuiOverlaysEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 客户端注册: 「使用望远镜」按键 + 缩放倍数读数 */
@Mod.EventBusSubscriber(modid = BetterExperience.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientHandler
{
    public static final String KEY_CATEGORY = "key.categories.better_experience";
    public static final String KEY_USE_SPYGLASS = "key.better_experience.spyglass_use";

    /** 按住即开镜。默认不绑定按键, 需在「控制」中自行设置 */
    public static final KeyMapping USE_KEY = new KeyMapping(
            KEY_USE_SPYGLASS,
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            KEY_CATEGORY
    );

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event)
    {
        event.register(USE_KEY);
    }

    @SubscribeEvent
    public static void registerOverlays(RegisterGuiOverlaysEvent event)
    {
        event.registerAboveAll("spyglass_zoom", SpyglassScopeOverlay.INSTANCE);
    }
}
