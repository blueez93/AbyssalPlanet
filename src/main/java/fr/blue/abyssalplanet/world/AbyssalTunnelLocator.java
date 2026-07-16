package fr.blue.abyssalplanet.world;

import fr.blue.abyssalplanet.registry.ModBiomes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;

public final class AbyssalTunnelLocator {
    public static final int SHAFT_RADIUS = 34;
    public static final int RIM_RADIUS = 43;
    public static final int CAVERN_RADIUS = 52;
    public static final int BOTTOM_FLOOR_Y = 12;
    public static final int CAVERN_CEILING_Y = 35;
    public static final int BOSS_SPAWN_Y = 19;
    public static final int MINIMUM_SEPARATION_BLOCKS = 2500;

    private static final int MIN_SOUTH_OFFSET = 220;
    private static final int MAX_SOUTH_OFFSET = 500;
    private static final int FIRST_SOUTH_Z = roundUpToChunk(
            ModBiomes.ABYSSAL_VALLEY_OUTER_RADIUS + MIN_SOUTH_OFFSET
    );
    private static final int LAST_SOUTH_Z = roundDownToChunk(
            ModBiomes.ABYSSAL_VALLEY_OUTER_RADIUS + MAX_SOUTH_OFFSET
    );
    private static final int SOUTH_SLOT_COUNT = (LAST_SOUTH_Z - FIRST_SOUTH_Z) / 16 + 1;
    private static final int LEGACY_DISTANCE_FROM_WORLD_CENTER =
            ModBiomes.ABYSSAL_VALLEY_OUTER_RADIUS + 480;
    private static final long TUNNEL_SALT = 0x6A17C4D93BE2058FL;

    private AbyssalTunnelLocator() {
    }

    public static BlockPos getCenter(long seed) {
        int slot = (int) Long.remainderUnsigned(mix64(seed ^ TUNNEL_SALT), SOUTH_SLOT_COUNT);
        int z = FIRST_SOUTH_Z + slot * 16;
        return new BlockPos(0, BOSS_SPAWN_Y, z);
    }

    public static BlockPos getLegacyCenter(long seed) {
        double angle = unit(mix64(seed ^ TUNNEL_SALT)) * Math.PI * 2.0D;
        int x = (int) Math.round(Math.cos(angle) * LEGACY_DISTANCE_FROM_WORLD_CENTER);
        int z = (int) Math.round(Math.sin(angle) * LEGACY_DISTANCE_FROM_WORLD_CENTER);
        return new BlockPos(x, BOSS_SPAWN_Y, z);
    }

    public static boolean isCenterChunk(long seed, ChunkPos chunkPos) {
        BlockPos center = getCenter(seed);
        return chunkPos.x == center.getX() >> 4 && chunkPos.z == center.getZ() >> 4;
    }

    public static double horizontalDistance(BlockPos center, int x, int z) {
        double dx = x - center.getX();
        double dz = z - center.getZ();
        return Math.sqrt(dx * dx + dz * dz);
    }

    private static long mix64(long value) {
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }

    private static double unit(long value) {
        return (value >>> 11) * 0x1.0p-53;
    }

    private static int roundUpToChunk(int blockCoordinate) {
        return Math.floorDiv(blockCoordinate + 15, 16) * 16;
    }

    private static int roundDownToChunk(int blockCoordinate) {
        return Math.floorDiv(blockCoordinate, 16) * 16;
    }
}
