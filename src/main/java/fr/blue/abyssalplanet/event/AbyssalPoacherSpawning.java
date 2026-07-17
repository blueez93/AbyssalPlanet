package fr.blue.abyssalplanet.event;

import fr.blue.abyssalplanet.AbyssalPlanet;
import fr.blue.abyssalplanet.entity.AbyssalPoacherEntity;
import fr.blue.abyssalplanet.entity.AbyssalWandererEntity;
import fr.blue.abyssalplanet.registry.ModBiomes;
import fr.blue.abyssalplanet.registry.ModDimensions;
import fr.blue.abyssalplanet.registry.ModEntities;
import fr.blue.abyssalplanet.world.AbyssalPoacherGroupData;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = AbyssalPlanet.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AbyssalPoacherSpawning {
    private static final String NEXT_GROUP_ATTEMPT_TAG = "AbyssalPoacherNextGroupAttempt";
    private static final int MIN_INITIAL_DELAY_TICKS = 20 * 40;
    private static final int INITIAL_DELAY_VARIANCE_TICKS = 20 * 40;
    private static final int MIN_RETRY_DELAY_TICKS = 20 * 70;
    private static final int RETRY_DELAY_VARIANCE_TICKS = 20 * 70;
    private static final int MIN_SUCCESS_DELAY_TICKS = 20 * 60 * 12;
    private static final int SUCCESS_DELAY_VARIANCE_TICKS = 20 * 60 * 8;
    private static final double GROUP_SPAWN_CHANCE = 0.38D;
    private static final double ACTIVE_GROUP_RADIUS = 180.0D;

    private AbyssalPoacherSpawning() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || event.player.level().isClientSide
                || !(event.player instanceof ServerPlayer player)
                || player.tickCount % 20 != 0) {
            return;
        }

        ServerLevel level = player.serverLevel();
        if (!level.dimension().equals(ModDimensions.ABYSSAL_PLANET_LEVEL)) {
            return;
        }

        long gameTime = level.getGameTime();
        long nextAttempt = player.getPersistentData().getLong(NEXT_GROUP_ATTEMPT_TAG);
        if (nextAttempt <= 0L) {
            schedule(
                    player,
                    gameTime + MIN_INITIAL_DELAY_TICKS + level.random.nextInt(INITIAL_DELAY_VARIANCE_TICKS + 1)
            );
            return;
        }
        if (gameTime < nextAttempt) {
            return;
        }

        if (!level.getBiome(player.blockPosition()).is(ModBiomes.DECAY_ROAD)
                || hasActiveGroupNearby(level, player.blockPosition())) {
            scheduleRetry(player, level, gameTime);
            return;
        }

        if (level.random.nextDouble() > GROUP_SPAWN_CHANCE || !spawnGroup(level, player)) {
            scheduleRetry(player, level, gameTime);
            return;
        }

        schedule(
                player,
                gameTime + MIN_SUCCESS_DELAY_TICKS + level.random.nextInt(SUCCESS_DELAY_VARIANCE_TICKS + 1)
        );
    }

    private static void scheduleRetry(ServerPlayer player, ServerLevel level, long gameTime) {
        schedule(
                player,
                gameTime + MIN_RETRY_DELAY_TICKS + level.random.nextInt(RETRY_DELAY_VARIANCE_TICKS + 1)
        );
    }

    private static void schedule(ServerPlayer player, long gameTime) {
        player.getPersistentData().putLong(NEXT_GROUP_ATTEMPT_TAG, gameTime);
    }

    private static boolean hasActiveGroupNearby(ServerLevel level, BlockPos center) {
        AABB searchArea = new AABB(center).inflate(ACTIVE_GROUP_RADIUS, 96.0D, ACTIVE_GROUP_RADIUS);
        return !level.getEntitiesOfClass(
                AbyssalPoacherEntity.class,
                searchArea,
                poacher -> poacher.isAlive()
        ).isEmpty() || !level.getEntitiesOfClass(
                AbyssalWandererEntity.class,
                searchArea,
                wanderer -> wanderer.isAlive()
        ).isEmpty();
    }

    private static boolean spawnGroup(ServerLevel level, ServerPlayer player) {
        BlockPos center = findGroupCenter(level, player.blockPosition());
        if (center == null) {
            return false;
        }

        List<BlockPos> positions = findMemberPositions(level, center);
        if (positions.size() < 5) {
            return false;
        }

        List<AbyssalPoacherEntity> members = new ArrayList<>(5);
        for (int index = 0; index < 5; index++) {
            AbyssalPoacherEntity poacher = ModEntities.ABYSSAL_POACHER.get().create(level);
            if (poacher == null) {
                return false;
            }
            BlockPos position = positions.get(index);
            poacher.moveTo(
                    position.getX() + 0.5D,
                    position.getY(),
                    position.getZ() + 0.5D,
                    level.random.nextFloat() * 360.0F,
                    0.0F
            );
            members.add(poacher);
        }

        UUID groupId = UUID.randomUUID();
        AbyssalPoacherEntity intactSurvivor = members.get(4);
        UUID leaderId = intactSurvivor.getUUID();
        int bannerIndex = level.random.nextInt(4);
        AbyssalPoacherGroupData.get(level).createGroup(groupId, leaderId, center);

        for (int index = 0; index < members.size(); index++) {
            AbyssalPoacherEntity poacher = members.get(index);
            boolean intact = index == 4;
            poacher.configureGroup(groupId, leaderId, intact, index == bannerIndex);
            poacher.finalizeSpawn(
                    level,
                    level.getCurrentDifficultyAt(poacher.blockPosition()),
                    MobSpawnType.EVENT,
                    null,
                    null
            );
            poacher.setPersistenceRequired();
            level.addFreshEntity(poacher);
        }
        return true;
    }

    @Nullable
    private static BlockPos findGroupCenter(ServerLevel level, BlockPos playerPos) {
        for (int attempt = 0; attempt < 36; attempt++) {
            double angle = level.random.nextDouble() * Mth.TWO_PI;
            double distance = 28.0D + level.random.nextDouble() * 22.0D;
            int x = Mth.floor(playerPos.getX() + Math.cos(angle) * distance);
            int z = Mth.floor(playerPos.getZ() + Math.sin(angle) * distance);
            int floorY = level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z);
            BlockPos candidate = new BlockPos(x, floorY, z);
            if (isValidMemberPosition(level, candidate)
                    && level.getBiome(candidate).is(ModBiomes.DECAY_ROAD)) {
                return candidate;
            }
        }
        return null;
    }

    private static List<BlockPos> findMemberPositions(ServerLevel level, BlockPos center) {
        List<BlockPos> offsets = new ArrayList<>();
        for (int x = -4; x <= 4; x++) {
            for (int z = -4; z <= 4; z++) {
                if (x * x + z * z <= 18) {
                    offsets.add(new BlockPos(x, 0, z));
                }
            }
        }
        Collections.shuffle(offsets, new java.util.Random(level.random.nextLong()));

        List<BlockPos> positions = new ArrayList<>(5);
        for (BlockPos offset : offsets) {
            int x = center.getX() + offset.getX();
            int z = center.getZ() + offset.getZ();
            int floorY = level.getHeight(Heightmap.Types.OCEAN_FLOOR, x, z);
            BlockPos candidate = new BlockPos(x, floorY, z);
            if (!isValidMemberPosition(level, candidate)
                    || !level.getBiome(candidate).is(ModBiomes.DECAY_ROAD)) {
                continue;
            }

            boolean separated = positions.stream().allMatch(existing ->
                    existing.distSqr(candidate) >= 2.0D
            );
            if (separated) {
                positions.add(candidate);
            }
            if (positions.size() == 5) {
                return positions;
            }
        }
        return positions;
    }

    private static boolean isValidMemberPosition(ServerLevel level, BlockPos feet) {
        if (!level.getFluidState(feet).is(FluidTags.WATER)
                || !level.getFluidState(feet.above()).is(FluidTags.WATER)
                || !level.getFluidState(feet.above(2)).is(FluidTags.WATER)) {
            return false;
        }

        return !level.getBlockState(feet.below()).getCollisionShape(level, feet.below()).isEmpty()
                && level.noCollision(
                ModEntities.ABYSSAL_POACHER.get().getDimensions().makeBoundingBox(Vec3.atBottomCenterOf(feet))
        );
    }
}
