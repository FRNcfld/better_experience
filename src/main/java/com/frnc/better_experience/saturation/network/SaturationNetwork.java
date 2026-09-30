package com.frnc.better_experience.saturation.network;

import com.frnc.better_experience.BetterExperience;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/** 饱和度同步通道 (服务端 -> 客户端)。写法与 doublejump / elytraflight 的通道一致 */
public class SaturationNetwork
{
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(BetterExperience.MOD_ID, "saturation_sync"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register()
    {
        int id = 0;
        CHANNEL.registerMessage(id++, SaturationSyncPacket.class,
                SaturationSyncPacket::encode,
                SaturationSyncPacket::decode,
                SaturationSyncPacket::handle);
    }
}
