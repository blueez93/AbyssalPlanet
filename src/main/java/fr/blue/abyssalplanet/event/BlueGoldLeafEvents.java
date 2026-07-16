package fr.blue.abyssalplanet.event;

import fr.blue.abyssalplanet.AbyssalPlanet;
import fr.blue.abyssalplanet.item.BlueGoldLeafItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AbyssalPlanet.MOD_ID)
public final class BlueGoldLeafEvents {
    private BlueGoldLeafEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase == TickEvent.Phase.END
                && event.player instanceof ServerPlayer player
                && player.tickCount % 2 == 0) {
            BlueGoldLeafItem.reconcileBonusHearts(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (event.isWasDeath() && event.getEntity() instanceof ServerPlayer player) {
            BlueGoldLeafItem.clearBonusHearts(player);
        }
    }
}
