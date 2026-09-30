package com.frnc.better_experience.elytraflight.client;

import com.frnc.better_experience.BetterExperience;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 客户端按键注册: 切换鞘翅飞行开关。默认不绑定按键, 需在「控制」中自行设置 */
@Mod.EventBusSubscriber(modid = BetterExperience.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public class ClientHandler
{
    public static final String KEY_CATEGORY = "key.categories.better_experience";
    public static final String KEY_TOGGLE_ELYTRA_FLIGHT = "key.better_experience.elytra_flight_toggle";

    public static final KeyMapping TOGGLE_KEY = new KeyMapping(
            KEY_TOGGLE_ELYTRA_FLIGHT,
            InputConstants.Type.KEYSYM,
            InputConstants.UNKNOWN.getValue(),
            KEY_CATEGORY
    );

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event)
    {
        event.register(TOGGLE_KEY);
    }
}
