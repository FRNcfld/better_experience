package com.frnc.better_experience.stepassist.client;

import com.frnc.better_experience.BetterExperience;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 客户端按键注册: 循环上坡辅助模式。默认不绑定按键, 需在「控制」中自行设置 */
@Mod.EventBusSubscriber(modid = BetterExperience.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientHandler
{
    public static final String KEY_CATEGORY = "key.categories.better_experience";
    public static final String KEY_CYCLE_STEP_ASSIST = "key.better_experience.step_assist_mode";

    public static final KeyMapping CYCLE_KEY = new KeyMapping(
            KEY_CYCLE_STEP_ASSIST,
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            KEY_CATEGORY
    );

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event)
    {
        event.register(CYCLE_KEY);
    }
}
