package com.frnc.better_experience.saturation.network;

import com.frnc.better_experience.saturation.client.ClientSaturation;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 服务端 -> 客户端: 同步饱和度 / 消耗度 / 消耗上限, 供 HUD 显示。
 *
 * <p>三个值合并成一个包、且只在变化时发送 (见 {@code SaturationHandler.onPlayerTick}),
 * 而不是像原模组那样每 tick 发三个包。
 *
 * <p>客户端侧刻意<strong>不</strong>写回 {@code FoodData}: 服务端才是权威, 覆盖客户端本地的
 * {@code FoodData} 只会让本地预测和其他模组读到被污染的值。HUD 读的是 {@link ClientSaturation} 这份缓存。
 */
public class SaturationSyncPacket
{
    private final float saturation;
    private final float exhaustion;
    private final float maxExhaustion;

    public SaturationSyncPacket(float saturation, float exhaustion, float maxExhaustion)
    {
        this.saturation = saturation;
        this.exhaustion = exhaustion;
        this.maxExhaustion = maxExhaustion;
    }

    public static void encode(SaturationSyncPacket msg, FriendlyByteBuf buf)
    {
        buf.writeFloat(msg.saturation);
        buf.writeFloat(msg.exhaustion);
        buf.writeFloat(msg.maxExhaustion);
    }

    public static SaturationSyncPacket decode(FriendlyByteBuf buf)
    {
        return new SaturationSyncPacket(buf.readFloat(), buf.readFloat(), buf.readFloat());
    }

    public static void handle(SaturationSyncPacket msg, Supplier<NetworkEvent.Context> ctx)
    {
        ctx.get().enqueueWork(() ->
        {
            // 只在客户端处理; 服务端收到就直接丢弃, 这样服务端永远不会加载客户端专属类
            if (ctx.get().getDirection().getReceptionSide().isClient())
            {
                ClientSaturation.set(msg.saturation, msg.exhaustion, msg.maxExhaustion);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}
