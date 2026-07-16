package fr.blue.abyssalplanet.network;

import fr.blue.abyssalplanet.AbyssalPlanet;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class ModNetwork {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(AbyssalPlanet.MOD_ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private ModNetwork() {
    }

    public static void register() {
        int id = 0;
        CHANNEL.registerMessage(
                id++,
                OpenAbyssalPortalPacket.class,
                OpenAbyssalPortalPacket::encode,
                OpenAbyssalPortalPacket::decode,
                OpenAbyssalPortalPacket::handle
        );
        CHANNEL.registerMessage(
                id++,
                SecondaryDaggerStrikePacket.class,
                SecondaryDaggerStrikePacket::encode,
                SecondaryDaggerStrikePacket::decode,
                SecondaryDaggerStrikePacket::handle
        );
        CHANNEL.registerMessage(
                id++,
                PlaceAbyssalPouchGhostPacket.class,
                PlaceAbyssalPouchGhostPacket::encode,
                PlaceAbyssalPouchGhostPacket::decode,
                PlaceAbyssalPouchGhostPacket::handle
        );
        CHANNEL.registerMessage(
                id++,
                GeorgesSeniorBitePacket.class,
                GeorgesSeniorBitePacket::encode,
                GeorgesSeniorBitePacket::decode,
                GeorgesSeniorBitePacket::handle
        );
    }
}
