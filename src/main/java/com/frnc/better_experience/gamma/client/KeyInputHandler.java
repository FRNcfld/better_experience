package com.frnc.better_experience.gamma.client;

import com.frnc.better_experience.BetterExperience;
import com.frnc.better_experience.gamma.ExtendedGamma;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 按键切换亮度扩展开关, 并在动作栏回显当前状态 (默认不绑定按键, 需在「控制」中自行设置) */
@Mod.EventBusSubscriber(modid = BetterExperience.MOD_ID, value = Dist.CLIENT)
public class KeyInputHandler
{
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        if (!ClientHandler.TOGGLE_KEY.consumeClick()) return;

        boolean enabled = ExtendedGamma.toggle();
        mc.player.displayClientMessage(
                Component.translatable(enabled
                                ? "message.better_experience.extended_gamma.enabled"
                                : "message.better_experience.extended_gamma.disabled")
                        .withStyle(enabled ? ChatFormatting.GREEN : ChatFormatting.RED),
                true);
    }
}
