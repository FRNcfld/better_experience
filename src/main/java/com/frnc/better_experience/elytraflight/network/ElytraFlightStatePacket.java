package com.frnc.better_experience.elytraflight.network;

import com.frnc.better_experience.elytraflight.ElytraFlightHandler;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/** 服务端 -> 客户端: 同步鞘翅飞行开关状态, 客户端据此决定是否触发鞘翅飞行 */
public class ElytraFlightStatePacket
{
    private final boolean enabled;

    public ElytraFlightStatePacket(boolean enabled)
    {
        this.enabled = enabled;
    }

    public static void encode(ElytraFlightStatePacket msg, FriendlyByteBuf buf)
    {
        buf.writeBoolean(msg.enabled);
    }

    public static ElytraFlightStatePacket decode(FriendlyByteBuf buf)
    {
        return new ElytraFlightStatePacket(buf.readBoolean());
    }

    public static void handle(ElytraFlightStatePacket msg, Supplier<NetworkEvent.Context> ctx)
    {
        ctx.get().enqueueWork(() ->
        {
            // 该包仅发往客户端, 用接收端逻辑侧判断, 避免在服务端加载客户端类
            if (ctx.get().getDirection().getReceptionSide().isClient())
            {
                ElytraFlightHandler.setClientEnabled(msg.enabled);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
