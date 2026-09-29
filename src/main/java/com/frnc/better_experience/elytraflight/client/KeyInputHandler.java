package com.frnc.better_experience.elytraflight.client;

import com.frnc.better_experience.BetterExperience;
import com.frnc.better_experience.elytraflight.network.ElytraFlightNetwork;
import com.frnc.better_experience.elytraflight.network.ToggleElytraFlightPacket;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = BetterExperience.MOD_ID, value = Dist.CLIENT)
public class KeyInputHandler
{
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event)
    {
        if (event.phase != TickEvent.Phase.END) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (mc.screen != null) return;

        // 切换鞘翅飞行开关 (默认 H 键, 尊重玩家改绑), 服务端会回发聊天消息确认状态
        if (ClientHandler.TOGGLE_KEY.consumeClick())
        {
            ElytraFlightNetwork.CHANNEL.sendToServer(new ToggleElytraFlightPacket());
        }
    }
}
