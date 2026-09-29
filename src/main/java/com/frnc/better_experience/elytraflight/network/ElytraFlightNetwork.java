package com.frnc.better_experience.elytraflight.network;

import com.frnc.better_experience.BetterExperience;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ElytraFlightNetwork
{
    private static final String PROTOCOL_VERSION = "1";

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(BetterExperience.MOD_ID, "elytra_flight"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public static void register()
    {
        int id = 0;
        CHANNEL.registerMessage(id++, ToggleElytraFlightPacket.class,
                ToggleElytraFlightPacket::encode,
                ToggleElytraFlightPacket::decode,
                ToggleElytraFlightPacket::handle);
        CHANNEL.registerMessage(id++, ElytraFlightStatePacket.class,
                ElytraFlightStatePacket::encode,
                ElytraFlightStatePacket::decode,
                ElytraFlightStatePacket::handle);
    }
}
