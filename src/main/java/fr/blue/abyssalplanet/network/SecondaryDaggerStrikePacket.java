package fr.blue.abyssalplanet.network;

import fr.blue.abyssalplanet.item.GoldenBlueDaggerItem;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SecondaryDaggerStrikePacket {
    public static void encode(SecondaryDaggerStrikePacket packet, FriendlyByteBuf buffer) {
    }

    public static SecondaryDaggerStrikePacket decode(FriendlyByteBuf buffer) {
        return new SecondaryDaggerStrikePacket();
    }

    public static void handle(
            SecondaryDaggerStrikePacket packet,
            Supplier<NetworkEvent.Context> contextSupplier
    ) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null) {
                GoldenBlueDaggerItem.trySecondaryStrike(player);
            }
        });
        context.setPacketHandled(true);
    }
}
