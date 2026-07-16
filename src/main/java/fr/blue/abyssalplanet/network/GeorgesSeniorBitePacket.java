package fr.blue.abyssalplanet.network;

import fr.blue.abyssalplanet.entity.GeorgesSeniorEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public final class GeorgesSeniorBitePacket {
    public static void encode(GeorgesSeniorBitePacket packet, FriendlyByteBuf buffer) {
    }

    public static GeorgesSeniorBitePacket decode(FriendlyByteBuf buffer) {
        return new GeorgesSeniorBitePacket();
    }

    public static void handle(
            GeorgesSeniorBitePacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.getVehicle() instanceof GeorgesSeniorEntity senior) {
                senior.performRiderBite(player);
            }
        });
        context.setPacketHandled(true);
    }
}
