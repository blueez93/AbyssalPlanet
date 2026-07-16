package fr.blue.abyssalplanet.client;

import fr.blue.abyssalplanet.AbyssalPlanet;
import fr.blue.abyssalplanet.entity.AbyssalSerpentEntity;
import fr.blue.abyssalplanet.entity.KrakenBossEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ViewportEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AbyssalPlanet.MOD_ID, value = Dist.CLIENT)
public class ClientForgeEvents {
    @SubscribeEvent
    public static void onCameraAngles(ViewportEvent.ComputeCameraAngles event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;

        if (player == null || player.level() == null) {
            return;
        }

        float shake = 0.0F;
        AABB searchBox = player.getBoundingBox().inflate(96.0D);

        for (KrakenBossEntity kraken : player.level().getEntitiesOfClass(KrakenBossEntity.class, searchBox)) {
            shake = Math.max(shake, kraken.getClientChargeShakeStrength((float) event.getPartialTick()));
        }
        for (AbyssalSerpentEntity serpent : player.level().getEntitiesOfClass(AbyssalSerpentEntity.class, searchBox.inflate(80.0D))) {
            shake = Math.max(shake, serpent.getClientShakeStrength((float) event.getPartialTick()) * 0.65F);
        }

        if (shake <= 0.0F) {
            return;
        }

        float time = player.tickCount + (float) event.getPartialTick();
        event.setPitch(event.getPitch() + Mth.sin(time * 3.3F) * 0.18F * shake);
        event.setYaw(event.getYaw() + Mth.cos(time * 2.7F) * 0.14F * shake);
        event.setRoll(event.getRoll() + Mth.sin(time * 4.1F) * 0.10F * shake);
    }
}
