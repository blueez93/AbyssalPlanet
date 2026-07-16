package fr.blue.abyssalplanet.client;

import fr.blue.abyssalplanet.AbyssalPlanet;
import fr.blue.abyssalplanet.registry.ModDimensions;
import net.minecraft.client.Minecraft;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AbyssalPlanet.MOD_ID, value = Dist.CLIENT)
public final class AbyssalMusicClientEvents {
    private static AbyssalAmbientSound activeSound;

    private AbyssalMusicClientEvents() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        boolean inAbyss = minecraft.player != null
                && minecraft.player.level().dimension().equals(ModDimensions.ABYSSAL_PLANET_LEVEL);

        if (!inAbyss) {
            stopAmbientSound(minecraft);
            return;
        }

        minecraft.getMusicManager().stopPlaying();

        if (activeSound == null || activeSound.isStopped()) {
            activeSound = new AbyssalAmbientSound();
            minecraft.getSoundManager().play(activeSound);
        }
    }

    private static void stopAmbientSound(Minecraft minecraft) {
        if (activeSound == null) {
            return;
        }

        activeSound.stopImmediately();
        minecraft.getSoundManager().stop(activeSound);
        activeSound = null;
    }
}
