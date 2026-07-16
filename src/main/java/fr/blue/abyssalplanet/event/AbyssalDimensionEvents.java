package fr.blue.abyssalplanet.event;

import fr.blue.abyssalplanet.AbyssalPlanet;
import fr.blue.abyssalplanet.entity.AbyssalBlueMoonfishEntity;
import fr.blue.abyssalplanet.entity.AbyssalOctopusEntity;
import fr.blue.abyssalplanet.entity.BabyKrakenEntity;
import fr.blue.abyssalplanet.entity.KrakenBossEntity;
import fr.blue.abyssalplanet.entity.LuminousAbyssalFishEntity;
import fr.blue.abyssalplanet.entity.MiniKrakenEntity;
import fr.blue.abyssalplanet.entity.ZwoingEntity;
import fr.blue.abyssalplanet.registry.ModBiomes;
import fr.blue.abyssalplanet.registry.ModDimensions;
import fr.blue.abyssalplanet.registry.ModEntities;
import fr.blue.abyssalplanet.registry.ModItems;
import fr.blue.abyssalplanet.world.KrakenSpawnData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.block.Blocks;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Mod.EventBusSubscriber(modid = AbyssalPlanet.MOD_ID)
public class AbyssalDimensionEvents {
    private static final int OCTOPUS_SPAWN_INTERVAL_TICKS = 20 * 20;
    private static final int MAX_OCTOPUSES_NEAR_PLAYER = 8;
    private static final double OCTOPUS_LOCAL_CAP_RADIUS = 64.0D;
    private static final double OCTOPUS_MIN_SPAWN_DISTANCE = 18.0D;
    private static final double OCTOPUS_MAX_SPAWN_DISTANCE = 34.0D;
    private static final int LUMINOUS_FISH_SPAWN_INTERVAL_TICKS = 20 * 25;
    private static final int MAX_LUMINOUS_FISH_NEAR_PLAYER = 6;
    private static final double LUMINOUS_FISH_LOCAL_CAP_RADIUS = 64.0D;
    private static final double LUMINOUS_FISH_MIN_SPAWN_DISTANCE = 16.0D;
    private static final double LUMINOUS_FISH_MAX_SPAWN_DISTANCE = 30.0D;
    private static final int MOONFISH_SPAWN_INTERVAL_TICKS = 20 * 22;
    private static final int MAX_MOONFISH_NEAR_PLAYER = 6;
    private static final double MOONFISH_LOCAL_CAP_RADIUS = 80.0D;
    private static final double MOONFISH_MIN_SPAWN_DISTANCE = 18.0D;
    private static final double MOONFISH_MAX_SPAWN_DISTANCE = 42.0D;
    private static final int MINI_KRAKEN_SPAWN_INTERVAL_TICKS = 20 * 45;
    private static final int MAX_MINI_KRAKENS_NEAR_PLAYER = 6;
    private static final double MINI_KRAKEN_LOCAL_CAP_RADIUS = 112.0D;
    private static final double MINI_KRAKEN_MIN_SPAWN_DISTANCE = 22.0D;
    private static final double MINI_KRAKEN_MAX_SPAWN_DISTANCE = 38.0D;
    private static final int BABY_KRAKEN_SPAWN_INTERVAL_TICKS = 20 * 20;
    private static final float BABY_KRAKEN_VALLEY_SPAWN_CHANCE = 0.003F;
    private static final float BABY_KRAKEN_DEEP_SPAWN_CHANCE = 0.01F;
    private static final double BABY_KRAKEN_LOCAL_CAP_RADIUS = 160.0D;
    private static final double BABY_KRAKEN_MIN_SPAWN_DISTANCE = 18.0D;
    private static final double BABY_KRAKEN_MAX_SPAWN_DISTANCE = 48.0D;
    private static final int ZWOING_SPAWN_INTERVAL_TICKS = 20 * 20;
    private static final int MAX_ZWOINGS_NEAR_PLAYER = 20;
    private static final double ZWOING_LOCAL_CAP_RADIUS = 96.0D;
    private static final double ZWOING_MIN_SPAWN_DISTANCE = 14.0D;
    private static final double ZWOING_MAX_SPAWN_DISTANCE = 64.0D;
    private static final float ZWOING_SPAWN_CHANCE = 0.70F;
    private static final int DENSE_POPULATION_INTERVAL_TICKS = 20 * 2;
    private static final int MAX_LOADED_ABYSSAL_CREATURES = 20;
    private static final int ABYSSAL_WATER_SURFACE_Y = 125;
    private static final BlockPos ARRIVAL_ZONE = new BlockPos(0, 90, 0);
    private static final int ABYSSAL_ENTRY_EFFECT_DURATION_TICKS = 20 * 60 * 4;
    private static final int ABYSSAL_ENTRY_DOLPHINS_GRACE_DURATION_TICKS = 20 * 60 * 4;
    private static final int ABYSSAL_ENTRY_NIGHT_VISION_DURATION_TICKS = 20 * 60 * 5;
    private static final float ABYSSAL_ARRIVAL_SOUTH_YAW = 0.0F;
    private static final int KRAKEN_BOSS_SPAWN_CHECK_INTERVAL_TICKS = 20;
    private static final double KRAKEN_BOSS_MIN_SPAWN_DISTANCE = 62.0D;
    private static final double KRAKEN_BOSS_MAX_SPAWN_DISTANCE = 92.0D;
    private static final double ABYSSAL_CURRENT_TARGET_SPEED = 0.22D;
    private static final double ABYSSAL_CURRENT_MAX_ACCELERATION = 0.055D;

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;

        if (!player.level().dimension().equals(ModDimensions.ABYSSAL_PLANET_LEVEL)) {
            return;
        }

        applyAbyssalValleyCurrent(player);

        if (player.tickCount % 40 == 0) {
            applyAbyssalPressure(player);
        }

        if (player.level().getGameTime() % DENSE_POPULATION_INTERVAL_TICKS == 0
                && isPopulationCoordinator(player)) {
            maintainLoadedPopulation(player.serverLevel());
        }

        if (player.tickCount % DENSE_POPULATION_INTERVAL_TICKS == 0) {
            ensureDensePopulationAround(player);
        }

        if (player.tickCount % OCTOPUS_SPAWN_INTERVAL_TICKS == 0) {
            trySpawnOctopusAround(player, false);
        }

        if (player.tickCount % LUMINOUS_FISH_SPAWN_INTERVAL_TICKS == 0) {
            trySpawnLuminousFishSchoolAround(player, false);
        }

        if (player.tickCount % MOONFISH_SPAWN_INTERVAL_TICKS == 0) {
            trySpawnMoonfishSchoolAround(player, false);
        }

        if (player.tickCount % MINI_KRAKEN_SPAWN_INTERVAL_TICKS == 0) {
            trySpawnMiniKrakenAround(player, false);
        }

        if (player.tickCount % BABY_KRAKEN_SPAWN_INTERVAL_TICKS == 0) {
            trySpawnBabyKrakenAround(player);
        }

        if (player.tickCount % ZWOING_SPAWN_INTERVAL_TICKS == 0) {
            trySpawnZwoingAround(player, false);
        }

        if (player.level().getGameTime() % KRAKEN_BOSS_SPAWN_CHECK_INTERVAL_TICKS == 0
                && isPopulationCoordinator(player)) {
            tickKrakenBossSpawnSchedule(player.serverLevel());
        }
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && event.getTo().equals(ModDimensions.ABYSSAL_PLANET_LEVEL)) {
            handleAbyssalArrival(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && player.level().dimension().equals(ModDimensions.ABYSSAL_PLANET_LEVEL)) {
            handleAbyssalArrival(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && player.level().dimension().equals(ModDimensions.ABYSSAL_PLANET_LEVEL)) {
            handleAbyssalArrival(player);
        }
    }

    @SubscribeEvent
    public static void onKrakenBossDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof KrakenBossEntity
                && event.getEntity().level() instanceof ServerLevel level
                && level.dimension().equals(ModDimensions.ABYSSAL_PLANET_LEVEL)) {
            KrakenSpawnData.get(level).scheduleRespawn(level.getGameTime(), level.random);
        }
    }

    public static void handleAbyssalArrival(ServerPlayer player) {
        if (!player.level().dimension().equals(ModDimensions.ABYSSAL_PLANET_LEVEL)) {
            return;
        }

        player.addEffect(new MobEffectInstance(
                MobEffects.WATER_BREATHING,
                ABYSSAL_ENTRY_EFFECT_DURATION_TICKS,
                0,
                false,
                true,
                true
        ));
        player.addEffect(new MobEffectInstance(
                MobEffects.DOLPHINS_GRACE,
                ABYSSAL_ENTRY_DOLPHINS_GRACE_DURATION_TICKS,
                0,
                false,
                true,
                true
        ));
        player.addEffect(new MobEffectInstance(
                MobEffects.NIGHT_VISION,
                ABYSSAL_ENTRY_NIGHT_VISION_DURATION_TICKS,
                0,
                false,
                true,
                true
        ));

        orientArrivalTowardSouth(player);

        ServerLevel level = player.serverLevel();
        KrakenSpawnData spawnData = KrakenSpawnData.get(level);
        if (hasActiveKrakenBoss(level)) {
            spawnData.markBossAlive();
        } else if (!spawnData.isBossAlive()) {
            spawnData.scheduleInitialSpawn(level.getGameTime(), level.random);
        }
    }

    private static void orientArrivalTowardSouth(ServerPlayer player) {
        if (player.blockPosition().distSqr(ARRIVAL_ZONE) > 12.0D * 12.0D) {
            return;
        }

        player.setYRot(ABYSSAL_ARRIVAL_SOUTH_YAW);
        player.setYHeadRot(ABYSSAL_ARRIVAL_SOUTH_YAW);
        player.setXRot(0.0F);
        player.connection.teleport(
                player.getX(),
                player.getY(),
                player.getZ(),
                ABYSSAL_ARRIVAL_SOUTH_YAW,
                0.0F
        );
    }

    @SubscribeEvent
    public static void onMobFinalizeSpawn(MobSpawnEvent.FinalizeSpawn event) {
        ServerLevel level = event.getLevel().getLevel();

        if (!level.dimension().equals(ModDimensions.ABYSSAL_PLANET_LEVEL)) {
            return;
        }

        if (!isAbyssalModMob(event.getEntity())) {
            event.setSpawnCancelled(true);
        }
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !level.dimension().equals(ModDimensions.ABYSSAL_PLANET_LEVEL)
                || !(event.getEntity() instanceof Mob mob)) {
            return;
        }

        if (!isAbyssalModMob(mob)) {
            event.setCanceled(true);
        }
    }

    private static boolean isAbyssalModMob(Mob mob) {
        var entityId = ForgeRegistries.ENTITY_TYPES.getKey(mob.getType());
        return entityId != null && AbyssalPlanet.MOD_ID.equals(entityId.getNamespace());
    }

    private static void applyAbyssalPressure(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(
                MobEffects.DARKNESS,
                20 * 7,
                0,
                false,
                false,
                true
        ));

        player.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN,
                20 * 5,
                0,
                false,
                false,
                true
        ));

    }

    private static void applyAbyssalValleyCurrent(ServerPlayer player) {
        if (!player.isInWater()
                || player.isSpectator()
                || player.getOffhandItem().is(ModItems.SOUL_OF_THE_SEAS.get())
                || !isAbyssalValley(player.serverLevel(), player.blockPosition())) {
            return;
        }

        double distanceFromCenter = Math.sqrt(
                player.getX() * player.getX() + player.getZ() * player.getZ()
        );
        if (distanceFromCenter < 0.001D) {
            return;
        }

        double directionX = -player.getX() / distanceFromCenter;
        double directionZ = -player.getZ() / distanceFromCenter;
        double inwardSpeed = player.getDeltaMovement().x * directionX
                + player.getDeltaMovement().z * directionZ;
        double acceleration = Math.min(
                ABYSSAL_CURRENT_MAX_ACCELERATION,
                Math.max(0.0D, ABYSSAL_CURRENT_TARGET_SPEED - inwardSpeed)
        );

        if (acceleration > 0.0D) {
            player.push(directionX * acceleration, 0.0D, directionZ * acceleration);
            player.hurtMarked = true;
        }
    }

    private static void ensureDensePopulationAround(ServerPlayer player) {
        ServerLevel level = player.serverLevel();

        if (isDeepAbyssal(level, player.blockPosition())) {
            for (int index = 0; index < 4; index++) {
                trySpawnOctopusAround(player, true);
            }

            for (int index = 0; index < 3; index++) {
                trySpawnMiniKrakenAround(player, true);
            }

            trySpawnLuminousFishSchoolAround(player, true);
        } else if (isAbyssalValley(level, player.blockPosition())) {
            for (int index = 0; index < 2; index++) {
                trySpawnMoonfishSchoolAround(player, true);
            }

            for (int index = 0; index < 8; index++) {
                trySpawnZwoingAround(player, true);
            }
        }
    }

    private static int countNearbyWildZwoings(ServerLevel level, ServerPlayer player) {
        return level.getEntitiesOfClass(
                ZwoingEntity.class,
                player.getBoundingBox().inflate(ZWOING_LOCAL_CAP_RADIUS),
                zwoing -> zwoing.isAlive() && zwoing.getOwnerUuid() == null
        ).size();
    }

    private static void trySpawnOctopusAround(ServerPlayer player, boolean guaranteed) {
        ServerLevel level = player.serverLevel();
        int nearbyOctopuses = level.getEntitiesOfClass(
                AbyssalOctopusEntity.class,
                player.getBoundingBox().inflate(OCTOPUS_LOCAL_CAP_RADIUS),
                Entity::isAlive
        ).size();

        if (getLoadedAbyssalCreatureCount(level) >= MAX_LOADED_ABYSSAL_CREATURES
                || nearbyOctopuses >= MAX_OCTOPUSES_NEAR_PLAYER
                || (!guaranteed && level.random.nextFloat() > 0.45F)) {
            return;
        }

        for (int attempt = 0; attempt < 24; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double distance = OCTOPUS_MIN_SPAWN_DISTANCE
                    + level.random.nextDouble() * (OCTOPUS_MAX_SPAWN_DISTANCE - OCTOPUS_MIN_SPAWN_DISTANCE);
            double x = player.getX() + Math.cos(angle) * distance;
            double y = player.getY() - 5.0D + level.random.nextDouble() * 10.0D;
            double z = player.getZ() + Math.sin(angle) * distance;
            BlockPos spawnPos = findWaterSpawnPosition(level, BlockPos.containing(x, y, z));

            if (spawnPos == null || !isDeepAbyssal(level, spawnPos)) {
                continue;
            }

            AbyssalOctopusEntity octopus = ModEntities.ABYSSAL_OCTOPUS.get().create(level);

            if (octopus == null) {
                return;
            }

            octopus.moveTo(
                    spawnPos.getX() + 0.5D,
                    spawnPos.getY() + 0.2D,
                    spawnPos.getZ() + 0.5D,
                    level.random.nextFloat() * 360.0F,
                    0.0F
            );
            if (!hasSpawnSpace(level, octopus)) {
                continue;
            }

            octopus.finalizeSpawn(
                    level,
                    level.getCurrentDifficultyAt(spawnPos),
                    MobSpawnType.NATURAL,
                    null,
                    null
            );
            level.addFreshEntity(octopus);
            return;
        }
    }

    private static boolean isPopulationCoordinator(ServerPlayer player) {
        for (ServerPlayer other : player.serverLevel().players()) {
            if (other.isAlive()
                    && !other.isSpectator()
                    && other.getUUID().compareTo(player.getUUID()) < 0) {
                return false;
            }
        }
        return true;
    }

    private static void maintainLoadedPopulation(ServerLevel level) {
        List<ServerPlayer> activePlayers = new ArrayList<>();
        List<AbyssalOctopusEntity> octopuses = new ArrayList<>();
        List<MiniKrakenEntity> miniKrakens = new ArrayList<>();
        List<LuminousAbyssalFishEntity> luminousFish = new ArrayList<>();
        List<AbyssalBlueMoonfishEntity> moonfish = new ArrayList<>();
        List<ZwoingEntity> wildZwoings = new ArrayList<>();
        List<KrakenBossEntity> krakenBosses = new ArrayList<>();
        List<Entity> entitiesOutsideTheirBiome = new ArrayList<>();

        for (ServerPlayer player : level.players()) {
            if (player.isAlive() && !player.isSpectator()) {
                activePlayers.add(player);
            }
        }

        if (activePlayers.isEmpty()) {
            return;
        }

        for (Entity entity : level.getAllEntities()) {
            if (!entity.isAlive()) {
                continue;
            }

            if (entity instanceof AbyssalOctopusEntity octopus) {
                if (isDeepAbyssal(level, octopus.blockPosition())) {
                    octopuses.add(octopus);
                } else {
                    entitiesOutsideTheirBiome.add(octopus);
                }
            } else if (entity instanceof MiniKrakenEntity miniKraken) {
                if (isDeepAbyssal(level, miniKraken.blockPosition())) {
                    miniKrakens.add(miniKraken);
                } else {
                    entitiesOutsideTheirBiome.add(miniKraken);
                }
            } else if (entity instanceof LuminousAbyssalFishEntity fish) {
                if (isDeepAbyssal(level, fish.blockPosition())) {
                    luminousFish.add(fish);
                } else {
                    entitiesOutsideTheirBiome.add(fish);
                }
            } else if (entity instanceof AbyssalBlueMoonfishEntity fish) {
                if (isAbyssalValley(level, fish.blockPosition())) {
                    moonfish.add(fish);
                } else {
                    entitiesOutsideTheirBiome.add(fish);
                }
            } else if (entity instanceof ZwoingEntity zwoing && zwoing.getOwnerUuid() == null) {
                if (isAbyssalValley(level, zwoing.blockPosition())) {
                    wildZwoings.add(zwoing);
                } else {
                    entitiesOutsideTheirBiome.add(zwoing);
                }
            } else if (entity instanceof KrakenBossEntity krakenBoss) {
                if (isDeepAbyssal(level, krakenBoss.blockPosition())) {
                    krakenBosses.add(krakenBoss);
                } else {
                    entitiesOutsideTheirBiome.add(krakenBoss);
                }
            }
        }

        entitiesOutsideTheirBiome.forEach(Entity::discard);

        trimLoadedPopulation(octopuses, MAX_OCTOPUSES_NEAR_PLAYER, activePlayers);
        trimLoadedPopulation(miniKrakens, MAX_MINI_KRAKENS_NEAR_PLAYER, activePlayers);
        trimLoadedPopulation(luminousFish, MAX_LUMINOUS_FISH_NEAR_PLAYER, activePlayers);
        trimLoadedPopulation(moonfish, MAX_MOONFISH_NEAR_PLAYER, activePlayers);
        trimLoadedPopulation(wildZwoings, MAX_ZWOINGS_NEAR_PLAYER, activePlayers);
        trimLoadedPopulation(krakenBosses, 1, activePlayers);

        boolean hasPlayerInDeepAbyssal = activePlayers.stream()
                .anyMatch(player -> isDeepAbyssal(level, player.blockPosition()));
        if (hasPlayerInDeepAbyssal) {
            int priorityPopulation = countActive(octopuses) + countActive(miniKrakens);
            int lowPriorityPopulation = countActive(luminousFish) + countActive(wildZwoings);
            int maximumLowPriorityPopulation = Math.max(
                    0,
                    MAX_LOADED_ABYSSAL_CREATURES
                            - MAX_OCTOPUSES_NEAR_PLAYER
                            - MAX_MINI_KRAKENS_NEAR_PLAYER
            );
            int lowPriorityToRemove = Math.max(
                    0,
                    lowPriorityPopulation - maximumLowPriorityPopulation
            );

            if (priorityPopulation < MAX_OCTOPUSES_NEAR_PLAYER + MAX_MINI_KRAKENS_NEAR_PLAYER
                    && lowPriorityToRemove > 0) {
                List<Entity> lowPriorityEntities = new ArrayList<>();
                luminousFish.stream().filter(entity -> !entity.isRemoved()).forEach(lowPriorityEntities::add);
                moonfish.stream().filter(entity -> !entity.isRemoved()).forEach(lowPriorityEntities::add);
                wildZwoings.stream().filter(entity -> !entity.isRemoved()).forEach(lowPriorityEntities::add);
                lowPriorityEntities.sort(
                        Comparator.comparingDouble(
                                (Entity entity) -> distanceToClosestPlayer(entity, activePlayers)
                        ).reversed()
                );
                for (int index = 0; index < Math.min(lowPriorityToRemove, lowPriorityEntities.size()); index++) {
                    lowPriorityEntities.get(index).discard();
                }
            }
        }

        List<Entity> ambientPopulation = new ArrayList<>();
        octopuses.stream().filter(entity -> !entity.isRemoved()).forEach(ambientPopulation::add);
        miniKrakens.stream().filter(entity -> !entity.isRemoved()).forEach(ambientPopulation::add);
        luminousFish.stream().filter(entity -> !entity.isRemoved()).forEach(ambientPopulation::add);
        moonfish.stream().filter(entity -> !entity.isRemoved()).forEach(ambientPopulation::add);
        wildZwoings.stream().filter(entity -> !entity.isRemoved()).forEach(ambientPopulation::add);

        if (ambientPopulation.size() > MAX_LOADED_ABYSSAL_CREATURES) {
            ambientPopulation.sort(
                    Comparator.comparingInt(AbyssalDimensionEvents::populationRemovalPriority)
                            .thenComparing(
                                    Comparator.comparingDouble(
                                            (Entity entity) -> distanceToClosestPlayer(entity, activePlayers)
                                    ).reversed()
                            )
            );
            int excess = ambientPopulation.size() - MAX_LOADED_ABYSSAL_CREATURES;
            for (int index = 0; index < excess; index++) {
                ambientPopulation.get(index).discard();
            }
        }
    }

    private static int populationRemovalPriority(Entity entity) {
        if (entity instanceof LuminousAbyssalFishEntity) {
            return 0;
        }
        if (entity instanceof AbyssalBlueMoonfishEntity) {
            return 1;
        }
        if (entity instanceof ZwoingEntity) {
            return 2;
        }
        if (entity instanceof AbyssalOctopusEntity) {
            return 3;
        }
        return 4;
    }

    private static int countActive(List<? extends Entity> entities) {
        return (int) entities.stream()
                .filter(entity -> entity.isAlive() && !entity.isRemoved())
                .count();
    }

    private static int getLoadedAbyssalCreatureCount(ServerLevel level) {
        int count = 0;
        for (Entity entity : level.getAllEntities()) {
            if (entity.isAlive()
                    && (entity instanceof AbyssalOctopusEntity
                    || entity instanceof MiniKrakenEntity
                    || entity instanceof LuminousAbyssalFishEntity
                    || entity instanceof AbyssalBlueMoonfishEntity
                    || (entity instanceof ZwoingEntity zwoing && zwoing.getOwnerUuid() == null))) {
                count++;
            }
        }
        return count;
    }

    private static <T extends Entity> void trimLoadedPopulation(
            List<T> entities,
            int maximum,
            List<ServerPlayer> players
    ) {
        if (entities.size() <= maximum) {
            return;
        }

        entities.sort(Comparator.comparingDouble(entity -> distanceToClosestPlayer(entity, players)));
        for (int index = maximum; index < entities.size(); index++) {
            entities.get(index).discard();
        }
    }

    private static double distanceToClosestPlayer(Entity entity, List<ServerPlayer> players) {
        double closestDistance = Double.MAX_VALUE;
        for (ServerPlayer player : players) {
            closestDistance = Math.min(closestDistance, entity.distanceToSqr(player));
        }
        return closestDistance;
    }

    private static void trySpawnMoonfishSchoolAround(ServerPlayer player, boolean guaranteed) {
        ServerLevel level = player.serverLevel();
        int nearbyMoonfish = level.getEntitiesOfClass(
                AbyssalBlueMoonfishEntity.class,
                player.getBoundingBox().inflate(MOONFISH_LOCAL_CAP_RADIUS),
                Entity::isAlive
        ).size();

        if (getLoadedAbyssalCreatureCount(level) >= MAX_LOADED_ABYSSAL_CREATURES
                || nearbyMoonfish >= MAX_MOONFISH_NEAR_PLAYER
                || (!guaranteed && level.random.nextFloat() > 0.65F)) {
            return;
        }

        for (int attempt = 0; attempt < 24; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double distance = MOONFISH_MIN_SPAWN_DISTANCE
                    + level.random.nextDouble() * (MOONFISH_MAX_SPAWN_DISTANCE - MOONFISH_MIN_SPAWN_DISTANCE);
            double x = player.getX() + Math.cos(angle) * distance;
            double y = player.getY() - 8.0D + level.random.nextDouble() * 16.0D;
            double z = player.getZ() + Math.sin(angle) * distance;
            BlockPos center = findWaterSpawnPosition(level, BlockPos.containing(x, y, z));

            if (center == null || !isAbyssalValley(level, center)) {
                continue;
            }

            int schoolSize = Math.min(
                    2 + level.random.nextInt(4),
                    Math.min(
                            MAX_MOONFISH_NEAR_PLAYER - nearbyMoonfish,
                            MAX_LOADED_ABYSSAL_CREATURES - getLoadedAbyssalCreatureCount(level)
                    )
            );
            spawnMoonfishSchool(level, center, schoolSize);
            return;
        }
    }

    private static void spawnMoonfishSchool(ServerLevel level, BlockPos center, int schoolSize) {
        SpawnGroupData schoolData = null;

        for (int index = 0; index < schoolSize; index++) {
            if (getLoadedAbyssalCreatureCount(level) >= MAX_LOADED_ABYSSAL_CREATURES) {
                return;
            }

            BlockPos spawnPos = findWaterSpawnPosition(level, center.offset(
                    level.random.nextInt(9) - 4,
                    level.random.nextInt(7) - 3,
                    level.random.nextInt(9) - 4
            ));

            if (spawnPos == null || !isAbyssalValley(level, spawnPos)) {
                continue;
            }

            AbyssalBlueMoonfishEntity fish = ModEntities.ABYSSAL_BLUE_MOONFISH.get().create(level);
            if (fish == null) {
                return;
            }

            fish.chooseRandomVariant();
            fish.moveTo(
                    spawnPos.getX() + 0.5D,
                    spawnPos.getY() + 0.2D,
                    spawnPos.getZ() + 0.5D,
                    level.random.nextFloat() * 360.0F,
                    0.0F
            );
            if (!hasSpawnSpace(level, fish)) {
                continue;
            }

            schoolData = fish.finalizeSpawn(
                    level,
                    level.getCurrentDifficultyAt(spawnPos),
                    MobSpawnType.NATURAL,
                    schoolData,
                    null
            );
            level.addFreshEntity(fish);
        }
    }

    private static void trySpawnLuminousFishSchoolAround(ServerPlayer player, boolean guaranteed) {
        ServerLevel level = player.serverLevel();
        int nearbyFish = level.getEntitiesOfClass(
                LuminousAbyssalFishEntity.class,
                player.getBoundingBox().inflate(LUMINOUS_FISH_LOCAL_CAP_RADIUS),
                Entity::isAlive
        ).size();

        if (getLoadedAbyssalCreatureCount(level) >= MAX_LOADED_ABYSSAL_CREATURES
                || nearbyFish >= MAX_LUMINOUS_FISH_NEAR_PLAYER
                || (!guaranteed && level.random.nextFloat() > 0.55F)) {
            return;
        }

        for (int attempt = 0; attempt < 20; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double distance = LUMINOUS_FISH_MIN_SPAWN_DISTANCE
                    + level.random.nextDouble() * (LUMINOUS_FISH_MAX_SPAWN_DISTANCE - LUMINOUS_FISH_MIN_SPAWN_DISTANCE);
            double x = player.getX() + Math.cos(angle) * distance;
            double y = player.getY() - 6.0D + level.random.nextDouble() * 12.0D;
            double z = player.getZ() + Math.sin(angle) * distance;
            BlockPos center = findWaterSpawnPosition(level, BlockPos.containing(x, y, z));

            if (center == null || !isDeepAbyssal(level, center)) {
                continue;
            }

            int schoolSize = Math.min(
                    4 + level.random.nextInt(5),
                    Math.min(
                            MAX_LUMINOUS_FISH_NEAR_PLAYER - nearbyFish,
                            MAX_LOADED_ABYSSAL_CREATURES - getLoadedAbyssalCreatureCount(level)
                    )
            );
            spawnLuminousFishSchool(level, center, schoolSize);
            return;
        }
    }

    private static void spawnLuminousFishSchool(ServerLevel level, BlockPos center, int schoolSize) {
        SpawnGroupData schoolData = null;

        for (int index = 0; index < schoolSize; index++) {
            if (getLoadedAbyssalCreatureCount(level) >= MAX_LOADED_ABYSSAL_CREATURES) {
                return;
            }

            BlockPos spawnPos = findWaterSpawnPosition(level, center.offset(
                    level.random.nextInt(7) - 3,
                    level.random.nextInt(5) - 2,
                    level.random.nextInt(7) - 3
            ));

            if (spawnPos == null || !isDeepAbyssal(level, spawnPos)) {
                continue;
            }

            LuminousAbyssalFishEntity fish = ModEntities.LUMINOUS_ABYSSAL_FISH.get().create(level);

            if (fish == null) {
                return;
            }

            fish.moveTo(
                    spawnPos.getX() + 0.5D,
                    spawnPos.getY() + 0.2D,
                    spawnPos.getZ() + 0.5D,
                    level.random.nextFloat() * 360.0F,
                    0.0F
            );
            if (!hasSpawnSpace(level, fish)) {
                continue;
            }

            schoolData = fish.finalizeSpawn(
                    level,
                    level.getCurrentDifficultyAt(spawnPos),
                    MobSpawnType.NATURAL,
                    schoolData,
                    null
            );
            level.addFreshEntity(fish);
        }
    }

    private static void trySpawnMiniKrakenAround(ServerPlayer player, boolean guaranteed) {
        ServerLevel level = player.serverLevel();
        int nearbyMiniKrakens = level.getEntitiesOfClass(
                MiniKrakenEntity.class,
                player.getBoundingBox().inflate(MINI_KRAKEN_LOCAL_CAP_RADIUS),
                Entity::isAlive
        ).size();

        if (getLoadedAbyssalCreatureCount(level) >= MAX_LOADED_ABYSSAL_CREATURES
                || nearbyMiniKrakens >= MAX_MINI_KRAKENS_NEAR_PLAYER
                || (!guaranteed && level.random.nextFloat() > 0.55F)) {
            return;
        }

        for (int attempt = 0; attempt < 28; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double distance = MINI_KRAKEN_MIN_SPAWN_DISTANCE
                    + level.random.nextDouble() * (MINI_KRAKEN_MAX_SPAWN_DISTANCE - MINI_KRAKEN_MIN_SPAWN_DISTANCE);
            double x = player.getX() + Math.cos(angle) * distance;
            double y = player.getY() - 10.0D + level.random.nextDouble() * 16.0D;
            double z = player.getZ() + Math.sin(angle) * distance;
            BlockPos spawnPos = findWaterSpawnPosition(level, BlockPos.containing(x, y, z));

            if (spawnPos == null || !isDeepAbyssal(level, spawnPos)) {
                continue;
            }

            MiniKrakenEntity miniKraken = ModEntities.MINI_KRAKEN.get().create(level);

            if (miniKraken == null) {
                return;
            }

            miniKraken.moveTo(
                    spawnPos.getX() + 0.5D,
                    spawnPos.getY() + 0.2D,
                    spawnPos.getZ() + 0.5D,
                    level.random.nextFloat() * 360.0F,
                    0.0F
            );
            if (!hasSpawnSpace(level, miniKraken)) {
                continue;
            }

            miniKraken.finalizeSpawn(
                    level,
                    level.getCurrentDifficultyAt(spawnPos),
                    MobSpawnType.NATURAL,
                    null,
                    null
            );
            level.addFreshEntity(miniKraken);
            return;
        }
    }

    private static void trySpawnZwoingAround(ServerPlayer player, boolean guaranteed) {
        ServerLevel level = player.serverLevel();
        int nearbyZwoings = countNearbyWildZwoings(level, player);

        if (getLoadedAbyssalCreatureCount(level) >= MAX_LOADED_ABYSSAL_CREATURES
                || nearbyZwoings >= MAX_ZWOINGS_NEAR_PLAYER
                || (!guaranteed && level.random.nextFloat() > ZWOING_SPAWN_CHANCE)) {
            return;
        }

        for (int attempt = 0; attempt < 40; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double distance = ZWOING_MIN_SPAWN_DISTANCE
                    + level.random.nextDouble() * (ZWOING_MAX_SPAWN_DISTANCE - ZWOING_MIN_SPAWN_DISTANCE);
            int x = (int) Math.floor(player.getX() + Math.cos(angle) * distance);
            int z = (int) Math.floor(player.getZ() + Math.sin(angle) * distance);
            BlockPos floorPosition = findWaterFloorPosition(
                    level,
                    new BlockPos(x, ABYSSAL_WATER_SURFACE_Y, z)
            );

            if (floorPosition == null || !isAbyssalValley(level, floorPosition)) {
                continue;
            }

            ZwoingEntity zwoing = ModEntities.ZWOING.get().create(level);
            if (zwoing == null) {
                return;
            }

            zwoing.chooseRandomVariant();
            zwoing.moveTo(
                    floorPosition.getX() + 0.5D,
                    floorPosition.getY() + 0.05D,
                    floorPosition.getZ() + 0.5D,
                    level.random.nextFloat() * 360.0F,
                    0.0F
            );
            if (!hasSpawnSpace(level, zwoing)) {
                continue;
            }

            zwoing.finalizeSpawn(
                    level,
                    level.getCurrentDifficultyAt(floorPosition),
                    MobSpawnType.NATURAL,
                    null,
                    null
            );
            level.addFreshEntity(zwoing);
            return;
        }
    }

    private static void trySpawnBabyKrakenAround(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        boolean valleySpawn = isAbyssalValley(level, player.blockPosition());
        boolean deepSpawn = isDeepAbyssal(level, player.blockPosition()) && hasActiveKrakenBoss(level);

        if (!valleySpawn && !deepSpawn) {
            return;
        }

        boolean nearbyWildBaby = !level.getEntitiesOfClass(
                BabyKrakenEntity.class,
                player.getBoundingBox().inflate(BABY_KRAKEN_LOCAL_CAP_RADIUS),
                babyKraken -> babyKraken.isAlive() && !babyKraken.isTame()
        ).isEmpty();

        float spawnChance = valleySpawn ? BABY_KRAKEN_VALLEY_SPAWN_CHANCE : BABY_KRAKEN_DEEP_SPAWN_CHANCE;
        if (nearbyWildBaby || level.random.nextFloat() >= spawnChance) {
            return;
        }

        for (int attempt = 0; attempt < 48; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double distance = BABY_KRAKEN_MIN_SPAWN_DISTANCE
                    + level.random.nextDouble() * (BABY_KRAKEN_MAX_SPAWN_DISTANCE - BABY_KRAKEN_MIN_SPAWN_DISTANCE);
            int x = Mth.floor(player.getX() + Math.cos(angle) * distance);
            int z = Mth.floor(player.getZ() + Math.sin(angle) * distance);
            BlockPos floorPosition = findWaterFloorPosition(level, new BlockPos(x, ABYSSAL_WATER_SURFACE_Y, z));

            if (floorPosition == null
                    || (valleySpawn && !isAbyssalValley(level, floorPosition))
                    || (deepSpawn && !isDeepAbyssal(level, floorPosition))) {
                continue;
            }

            BabyKrakenEntity babyKraken = ModEntities.BABY_KRAKEN.get().create(level);
            if (babyKraken == null) {
                return;
            }

            babyKraken.moveTo(
                    floorPosition.getX() + 0.5D,
                    floorPosition.getY() + 0.08D,
                    floorPosition.getZ() + 0.5D,
                    level.random.nextFloat() * 360.0F,
                    0.0F
            );
            if (!hasSpawnSpace(level, babyKraken)) {
                continue;
            }

            babyKraken.finalizeSpawn(
                    level,
                    level.getCurrentDifficultyAt(floorPosition),
                    MobSpawnType.NATURAL,
                    null,
                    null
            );
            babyKraken.setPersistenceRequired();

            if (level.addFreshEntity(babyKraken)) {
                announceBabyKrakenSpawn(level, floorPosition);
            }
            return;
        }
    }

    private static boolean hasActiveKrakenBoss(ServerLevel level) {
        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof KrakenBossEntity krakenBoss && krakenBoss.isAlive()) {
                return true;
            }
        }
        return false;
    }

    private static void announceBabyKrakenSpawn(ServerLevel level, BlockPos spawnPosition) {
        Component biomeName = level.getBiome(spawnPosition).unwrapKey()
                .map(key -> {
                    var location = key.location();
                    return Component.translatable("biome." + location.getNamespace() + "." + location.getPath());
                })
                .orElse(Component.translatable("biome.abyssalplanet.abyssal_valley"));
        Component message = Component.translatable(
                "message.abyssalplanet.baby_kraken_spawned",
                biomeName
        );

        for (ServerPlayer serverPlayer : level.players()) {
            serverPlayer.sendSystemMessage(message);
        }
    }

    private static BlockPos findWaterFloorPosition(ServerLevel level, BlockPos origin) {
        int minimumY = level.getMinBuildHeight() + 1;
        for (int y = origin.getY(); y >= minimumY; y--) {
            BlockPos position = new BlockPos(origin.getX(), y, origin.getZ());

            if (!level.hasChunk(position.getX() >> 4, position.getZ() >> 4)) {
                return null;
            }

            BlockPos floor = position.below();
            if (level.getBlockState(position).is(Blocks.WATER)
                    && level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) {
                return position;
            }
        }

        return null;
    }

    private static void tickKrakenBossSpawnSchedule(ServerLevel level) {
        KrakenSpawnData spawnData = KrakenSpawnData.get(level);
        if (hasActiveKrakenBoss(level)) {
            if (!spawnData.isBossAlive()) {
                spawnData.markBossAlive();
            }
            return;
        }
        if (spawnData.isBossAlive()) {
            return;
        }
        if (!spawnData.hasScheduledSpawn()) {
            spawnData.scheduleInitialSpawn(level.getGameTime(), level.random);
            return;
        }
        if (!spawnData.isSpawnDue(level.getGameTime())) {
            return;
        }

        if (trySpawnKrakenBossNearArrival(level)) {
            spawnData.markBossAlive();
        } else {
            spawnData.delayFailedAttempt(level.getGameTime());
        }
    }

    private static boolean trySpawnKrakenBossNearArrival(ServerLevel level) {
        if (hasActiveKrakenBoss(level)) {
            return true;
        }

        for (int attempt = 0; attempt < 24; attempt++) {
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            double distance = KRAKEN_BOSS_MIN_SPAWN_DISTANCE
                    + level.random.nextDouble() * (KRAKEN_BOSS_MAX_SPAWN_DISTANCE - KRAKEN_BOSS_MIN_SPAWN_DISTANCE);
            double x = ARRIVAL_ZONE.getX() + 0.5D + Math.cos(angle) * distance;
            double y = 76.0D + level.random.nextDouble() * 22.0D;
            double z = ARRIVAL_ZONE.getZ() + 0.5D + Math.sin(angle) * distance;
            BlockPos candidate = BlockPos.containing(x, y, z);
            level.getChunk(candidate.getX() >> 4, candidate.getZ() >> 4);
            BlockPos spawnPos = findWaterSpawnPosition(level, candidate);

            if (spawnPos == null || !isDeepAbyssal(level, spawnPos)) {
                continue;
            }

            KrakenBossEntity kraken = ModEntities.KRAKEN_BOSS.get().create(level);

            if (kraken == null) {
                return false;
            }

            kraken.moveTo(
                    spawnPos.getX() + 0.5D,
                    spawnPos.getY() + 0.2D,
                    spawnPos.getZ() + 0.5D,
                    level.random.nextFloat() * 360.0F,
                    0.0F
            );
            if (!hasSpawnSpace(level, kraken)) {
                continue;
            }

            kraken.finalizeSpawn(
                    level,
                    level.getCurrentDifficultyAt(spawnPos),
                    MobSpawnType.NATURAL,
                    null,
                    null
            );
            kraken.setPersistenceRequired();
            level.addFreshEntity(kraken);
            spawnLuminousFishSchool(level, spawnPos, 10);
            return true;
        }
        return false;
    }

    private static BlockPos findWaterSpawnPosition(ServerLevel level, BlockPos origin) {
        for (int distance = 0; distance <= 32; distance++) {
            BlockPos lowerPos = origin.below(distance);
            if (isLoadedWaterSpawnPosition(level, lowerPos)) {
                return lowerPos;
            }

            if (distance > 0) {
                BlockPos upperPos = origin.above(distance);
                if (isLoadedWaterSpawnPosition(level, upperPos)) {
                    return upperPos;
                }
            }
        }

        return null;
    }

    private static boolean isLoadedWaterSpawnPosition(ServerLevel level, BlockPos pos) {
        if (pos.getY() <= level.getMinBuildHeight()
                || pos.getY() >= level.getMaxBuildHeight() - 1
                || !level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)) {
            return false;
        }

        return isWaterSpawnPosition(level, pos);
    }

    private static boolean isWaterSpawnPosition(ServerLevel level, BlockPos pos) {
        return level.getFluidState(pos).is(FluidTags.WATER)
                && level.getFluidState(pos.above()).is(FluidTags.WATER);
    }

    private static boolean hasSpawnSpace(ServerLevel level, Entity entity) {
        return level.getWorldBorder().isWithinBounds(entity.blockPosition())
                && level.noCollision(entity, entity.getBoundingBox());
    }

    private static boolean isDeepAbyssal(ServerLevel level, BlockPos pos) {
        return level.getBiome(pos).is(ModBiomes.DEEP_ABYSSAL);
    }

    private static boolean isAbyssalValley(ServerLevel level, BlockPos pos) {
        return level.getBiome(pos).is(ModBiomes.ABYSSAL_VALLEY);
    }
}
