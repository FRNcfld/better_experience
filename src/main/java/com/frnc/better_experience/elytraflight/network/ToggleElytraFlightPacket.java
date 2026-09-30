package com.frnc.better_experience.elytraflight.network;

import com.frnc.better_experience.elytraflight.ElytraFlightHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

/** 客户端 -> 服务端: 切换鞘翅飞行开关, 服务端回发动作栏提示并把新状态同步给客户端 */
public class ToggleElytraFlightPacket
{
    public ToggleElytraFlightPacket() {}

    public static void encode(ToggleElytraFlightPacket msg, FriendlyByteBuf buf) {}

    public static ToggleElytraFlightPacket decode(FriendlyByteBuf buf)
    {
        return new ToggleElytraFlightPacket();
    }

    public static void handle(ToggleElytraFlightPacket msg, Supplier<NetworkEvent.Context> ctx)
    {
        ctx.get().enqueueWork(() ->
        {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            boolean newState = ElytraFlightHandler.toggleFlight(player);

            Component message = Component.translatable(newState
                            ? "message.better_experience.elytra_flight.enabled"
                            : "message.better_experience.elytra_flight.disabled")
                    .withStyle(newState ? ChatFormatting.GREEN : ChatFormatting.RED);
            // 走动作栏而不是聊天栏, 与亮度 / 上坡辅助的按键提示保持一致 (第二个参数 true = 动作栏)
            player.displayClientMessage(message, true);

            // 同步状态给客户端, 使其据此决定是否触发鞘翅飞行
            ElytraFlightNetwork.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player),
                    new ElytraFlightStatePacket(newState));
        });
        ctx.get().setPacketHandled(true);
    }
}
