package com.frnc.better_experience.doublejump.network;

import com.frnc.better_experience.doublejump.JumpHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 客户端 -> 服务端: 玩家在空中再次按下跳跃键, 触发二段跳 */
public class DoubleJumpPacket
{
    public DoubleJumpPacket() {}

    public static void encode(DoubleJumpPacket msg, FriendlyByteBuf buf) {}

    public static DoubleJumpPacket decode(FriendlyByteBuf buf)
    {
        return new DoubleJumpPacket();
    }

    public static void handle(DoubleJumpPacket msg, Supplier<NetworkEvent.Context> ctx)
    {
        ctx.get().enqueueWork(() ->
        {
            ServerPlayer player = ctx.get().getSender();
            if (player != null)
            {
                JumpHandler.handleDoubleJump(player);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
