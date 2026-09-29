package com.frnc.better_experience.doublejump.network;

import com.frnc.better_experience.BetterExperience;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class DoubleJumpNetwork
{
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(BetterExperience.MOD_ID, "double_jump"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register()
    {
        int id = 0;
        CHANNEL.registerMessage(id++, ToggleDoubleJumpPacket.class,
                ToggleDoubleJumpPacket::encode,
                ToggleDoubleJumpPacket::decode,
                ToggleDoubleJumpPacket::handle);
        CHANNEL.registerMessage(id++, DoubleJumpPacket.class,
                DoubleJumpPacket::encode,
                DoubleJumpPacket::decode,
                DoubleJumpPacket::handle);
        CHANNEL.registerMessage(id++, DoubleJumpStatePacket.class,
                DoubleJumpStatePacket::encode,
                DoubleJumpStatePacket::decode,
                DoubleJumpStatePacket::handle);
    }
}
