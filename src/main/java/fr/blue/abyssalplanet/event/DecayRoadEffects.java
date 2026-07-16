package fr.blue.abyssalplanet.event;

import fr.blue.abyssalplanet.AbyssalPlanet;
import fr.blue.abyssalplanet.registry.ModBiomes;
import fr.blue.abyssalplanet.registry.ModDimensions;
import fr.blue.abyssalplanet.registry.ModEffects;
import fr.blue.abyssalplanet.registry.ModFluids;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AbyssalPlanet.MOD_ID)
public final class DecayRoadEffects {
    private static final String EXPOSURE_TICKS_TAG = "AbyssalPlanetDecayExposureTicks";
    private static final int DAMAGE_INTERVAL_TICKS = 20 * 30;

    private DecayRoadEffects() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        if (player.isCreative() || player.isSpectator() || !isExposed(player)) {
            clearExposure(player);
            return;
        }

        int exposureTicks = player.getPersistentData().getInt(EXPOSURE_TICKS_TAG) + 1;
        player.getPersistentData().putInt(EXPOSURE_TICKS_TAG, exposureTicks);

        if (player.tickCount % 20 == 0) {
            player.addEffect(new MobEffectInstance(
                    ModEffects.TOXIC_BURN.get(),
                    45,
                    0,
                    false,
                    true,
                    true
            ));
        }

        if (exposureTicks % DAMAGE_INTERVAL_TICKS == 0) {
            player.hurt(player.damageSources().magic(), 1.0F);
        }
    }

    private static boolean isExposed(ServerPlayer player) {
        BlockPos feet = player.blockPosition();
        BlockPos eyes = BlockPos.containing(player.getEyePosition());
        boolean inDecayRoad = player.level().dimension().equals(ModDimensions.ABYSSAL_PLANET_LEVEL)
                && player.serverLevel().getBiome(feet).is(ModBiomes.DECAY_ROAD);
        boolean touchingAbyssalWater = ModFluids.isAbyssalWater(player.level().getFluidState(feet))
                || ModFluids.isAbyssalWater(player.level().getFluidState(eyes));
        return inDecayRoad || touchingAbyssalWater;
    }

    private static void clearExposure(ServerPlayer player) {
        player.getPersistentData().remove(EXPOSURE_TICKS_TAG);
        if (player.hasEffect(ModEffects.TOXIC_BURN.get())
                && !AbyssalHunterCrossbowEvents.hasActiveWeaponBurn(player)) {
            player.removeEffect(ModEffects.TOXIC_BURN.get());
        }
    }
}
