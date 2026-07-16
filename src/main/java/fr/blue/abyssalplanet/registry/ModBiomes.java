package fr.blue.abyssalplanet.registry;

import fr.blue.abyssalplanet.AbyssalPlanet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.biome.Biome;

public final class ModBiomes {
    public static final int DEEP_ABYSSAL_RADIUS = 320;
    public static final int ABYSSAL_VALLEY_OUTER_RADIUS = 1024;

    public static final ResourceKey<Biome> DEEP_ABYSSAL = ResourceKey.create(
            Registries.BIOME,
            AbyssalPlanet.id("deep_abyssal")
    );
    public static final ResourceKey<Biome> ABYSSAL_VALLEY = ResourceKey.create(
            Registries.BIOME,
            AbyssalPlanet.id("abyssal_valley")
    );
    public static final ResourceKey<Biome> DECAY_ROAD = ResourceKey.create(
            Registries.BIOME,
            AbyssalPlanet.id("decay_road")
    );

    private ModBiomes() {
    }

    public static boolean isDeepAbyssal(BlockPos pos) {
        return distanceFromCenterSqr(pos) <= (long) DEEP_ABYSSAL_RADIUS * DEEP_ABYSSAL_RADIUS;
    }

    public static boolean isAbyssalValley(BlockPos pos) {
        long distanceSqr = distanceFromCenterSqr(pos);
        return distanceSqr > (long) DEEP_ABYSSAL_RADIUS * DEEP_ABYSSAL_RADIUS
                && distanceSqr <= (long) ABYSSAL_VALLEY_OUTER_RADIUS * ABYSSAL_VALLEY_OUTER_RADIUS;
    }

    public static boolean isDecayRoad(BlockPos pos) {
        return distanceFromCenterSqr(pos)
                > (long) ABYSSAL_VALLEY_OUTER_RADIUS * ABYSSAL_VALLEY_OUTER_RADIUS;
    }

    private static long distanceFromCenterSqr(BlockPos pos) {
        long x = pos.getX();
        long z = pos.getZ();
        return x * x + z * z;
    }
}
