package com.frnc.better_experience.doublejump.network;

import com.frnc.better_experience.doublejump.client.KeyInputHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 服务端 -> 客户端: 同步二段跳开关状态, 客户端据此决定是否触发二段跳 */
public class DoubleJumpStatePacket
{
    private final boolean enabled;

    public DoubleJumpStatePacket(boolean enabled)
    {
        this.enabled = enabled;
    }

    public static void encode(DoubleJumpStatePacket msg, FriendlyByteBuf buf)
    {
        buf.writeBoolean(msg.enabled);
    }

    public static DoubleJumpStatePacket decode(FriendlyByteBuf buf)
    {
        return new DoubleJumpStatePacket(buf.readBoolean());
    }

    public static void handle(DoubleJumpStatePacket msg, Supplier<NetworkEvent.Context> ctx)
    {
        ctx.get().enqueueWork(() ->
        {
            // 该包仅发往客户端, 用接收端逻辑侧判断, 避免在服务端加载客户端类
            if (ctx.get().getDirection().getReceptionSide().isClient())
            {
                KeyInputHandler.setDoubleJumpEnabled(msg.enabled);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
