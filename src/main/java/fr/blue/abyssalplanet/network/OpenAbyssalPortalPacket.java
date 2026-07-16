package fr.blue.abyssalplanet.network;

import fr.blue.abyssalplanet.item.AbyssalStaffItem;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

public class OpenAbyssalPortalPacket {
    public static void encode(OpenAbyssalPortalPacket packet, FriendlyByteBuf buffer) {
    }

    public static OpenAbyssalPortalPacket decode(FriendlyByteBuf buffer) {
        return new OpenAbyssalPortalPacket();
    }

    public static void handle(OpenAbyssalPortalPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                AbyssalStaffItem.tryOpenPortal(player);
            }
        });
        context.setPacketHandled(true);
    }
}
