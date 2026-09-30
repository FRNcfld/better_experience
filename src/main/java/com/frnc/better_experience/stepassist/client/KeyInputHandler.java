package com.frnc.better_experience.stepassist.client;

import com.frnc.better_experience.BetterExperience;
import com.frnc.better_experience.Config;
import com.frnc.better_experience.stepassist.StepAssistHandler;
import com.frnc.better_experience.stepassist.StepAssistMode;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** 按键循环上坡辅助模式, 并在动作栏回显当前模式 (默认不绑定按键, 需在「控制」中自行设置) */
@Mod.EventBusSubscriber(modid = BetterExperience.MOD_ID, value = Dist.CLIENT)
public class KeyInputHandler
{
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        // 功能被配置整体关闭时按键不做任何事——此时回显模式名只会误导玩家
        if (!Config.stepAssistEnabled) return;

        if (!ClientHandler.CYCLE_KEY.consumeClick()) return;

        StepAssistHandler.cycleMode();

        StepAssistMode mode = StepAssistHandler.getMode();
        mc.player.displayClientMessage(
                Component.translatable(
                                "message.better_experience.step_assist.mode",
                                Component.translatable(mode.messageKey()))
                        .withStyle(mode == StepAssistMode.OFF ? ChatFormatting.RED : ChatFormatting.GREEN),
                true);
    }
}
