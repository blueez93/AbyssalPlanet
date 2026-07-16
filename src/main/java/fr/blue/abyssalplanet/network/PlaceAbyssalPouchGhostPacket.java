package fr.blue.abyssalplanet.network;

import fr.blue.abyssalplanet.item.AbyssalPouchItem;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public class PlaceAbyssalPouchGhostPacket {
    public static void encode(PlaceAbyssalPouchGhostPacket packet, FriendlyByteBuf buffer) {
    }

    public static PlaceAbyssalPouchGhostPacket decode(FriendlyByteBuf buffer) {
        return new PlaceAbyssalPouchGhostPacket();
    }

    public static void handle(
            PlaceAbyssalPouchGhostPacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                AbyssalPouchItem.tryPlaceGhost(player);
            }
        });
        context.setPacketHandled(true);
    }
}
