package fr.blue.abyssalplanet.event;

import fr.blue.abyssalplanet.AbyssalPlanet;
import fr.blue.abyssalplanet.entity.GeorgesBriochardEntity;
import fr.blue.abyssalplanet.block.AbyssalEggBlock;
import fr.blue.abyssalplanet.registry.ModBiomes;
import fr.blue.abyssalplanet.registry.ModBlocks;
import fr.blue.abyssalplanet.registry.ModDimensions;
import fr.blue.abyssalplanet.registry.ModEntities;
import fr.blue.abyssalplanet.registry.ModFluids;
import fr.blue.abyssalplanet.world.AbyssalTunnelData;
import fr.blue.abyssalplanet.world.AbyssalTunnelLocator;
import fr.blue.abyssalplanet.world.BlueGoldTreeGenerator;
import fr.blue.abyssalplanet.world.BlueGoldTreeSpawnData;
import fr.blue.abyssalplanet.world.structure.AbyssalTunnelPiece;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.TicketType;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Climate;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.LevelChunkSection;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureStart;
import net.minecraft.world.level.levelgen.structure.pieces.PiecesContainer;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AbyssalPlanet.MOD_ID)
public final class AbyssalTerrainGeneration {
    private static final int STONE_FLOOR_Y = 45;
    private static final int BEDROCK_BOUNDARY_Y = 11;
    private static final int DUNE_CELL_SIZE = 48;
    private static final int DOME_CELL_SIZE = 112;
    private static final int VALLEY_DUNE_CELL_SIZE = 176;
    private static final int ABYSSAL_WATER_SURFACE_Y = 125;
    private static final int DEEP_TRANSITION_INNER_WIDTH = 128;
    private static final int DEEP_TRANSITION_OUTER_WIDTH = 160;
    private static final int DECAY_TRANSITION_WIDTH = 224;
    private static final long TERRAIN_SALT = 0x2B6D8F173A4C91E5L;
    private static final long ORE_SALT = 0x65A3C7419BE02D8FL;
    private static final long ALGAE_SALT = 0x49F2A7C18D53E60BL;
    private static final long BLUE_GOLD_TREE_SALT = 0x73C1B9E45A2D860FL;
    private static final long TUNNEL_ROCK_SALT = 0x41B7D62A9CE3058FL;
    private static final long TUNNEL_EGG_SALT = 0x7E19C45A2D8B603FL;
    private static final int TUNNEL_EGG_SCAN_RADIUS = 36;
    private static final int TUNNEL_EGG_VISIBLE_RADIUS = 27;
    private static final Map<MinecraftServer, Integer> PENDING_GEORGES_CHECKS = new HashMap<>();

    private AbyssalTerrainGeneration() {
    }

    @SubscribeEvent
    public static void onServerStarted(ServerStartedEvent event) {
        ServerLevel level = event.getServer().getLevel(ModDimensions.ABYSSAL_PLANET_LEVEL);
        if (level == null) {
            return;
        }

        BlockPos center = AbyssalTunnelLocator.getCenter(level.getSeed());
        level.getChunkSource().addRegionTicket(
                TicketType.PORTAL,
                new ChunkPos(center),
                3,
                center
        );

        AbyssalTunnelData tunnelData = AbyssalTunnelData.get(level);
        if (tunnelData.needsSouthernTunnelMigration()
                && tunnelData.hasSpawnedGeorges()
                && !tunnelData.isGeorgesDefeated()) {
            BlockPos legacyCenter = AbyssalTunnelLocator.getLegacyCenter(level.getSeed());
            level.getChunkSource().addRegionTicket(
                    TicketType.PORTAL,
                    new ChunkPos(legacyCenter),
                    6,
                    legacyCenter
            );
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        MinecraftServer server = event.getServer();
        Integer dueTick = PENDING_GEORGES_CHECKS.get(server);
        if (dueTick != null && server.getTickCount() >= dueTick) {
            PENDING_GEORGES_CHECKS.remove(server);
            ServerLevel level = server.getLevel(ModDimensions.ABYSSAL_PLANET_LEVEL);
            if (level != null) {
                ensureGeorgesBriochardNow(level);
            }
        }

        if (server.getTickCount() % 20 == 0) {
            ServerLevel level = server.getLevel(ModDimensions.ABYSSAL_PLANET_LEVEL);
            if (level != null
                    && level.hasChunkAt(AbyssalTunnelLocator.getCenter(level.getSeed()))) {
                ensureGeorgesBriochardNow(level);
            }
        }
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        if (!level.dimension().equals(ModDimensions.ABYSSAL_PLANET_LEVEL)) {
            return;
        }

        ChunkAccess chunk = event.getChunk();
        TerrainRegion region = getRegion(chunk.getPos());
        if (event.isNewChunk() || !hasNaturalBiomeTransitions(chunk)) {
            assignNaturalBiomes(level, chunk);
            markNaturalBiomeTransitions(chunk);
        }
        installBedrockBoundary(chunk);

        if (region == TerrainRegion.DECAY_ROAD) {
            boolean currentDecayTerrain = hasCurrentDecayTerrain(chunk);
            if (event.isNewChunk() || !currentDecayTerrain) {
                replaceLegacyAbyssalWater(chunk);
                shapeDecayRoad(
                        chunk,
                        level.getSeed(),
                        !event.isNewChunk() && !currentDecayTerrain
                );
                markCurrentDecayTerrain(chunk);
                chunk.setUnsaved(true);
            }
            ensureTunnelStructureStart(level, chunk);
            ensureGeorgesBriochard(level, chunk.getPos());
        } else if (event.isNewChunk()) {
            boolean valley = region == TerrainRegion.ABYSSAL_VALLEY;
            shapeSeabed(chunk, level.getSeed(), valley);
            generateOreVeins(chunk, level.getSeed(), valley);
            if (region == TerrainRegion.ABYSSAL_VALLEY) {
                generateAbyssalAlgae(chunk, level.getSeed());
            }
        }

        if (region == TerrainRegion.ABYSSAL_VALLEY) {
            generateBlueGoldTreeOnEmergentDune(
                    chunk,
                    level.getSeed(),
                    BlueGoldTreeSpawnData.get(level)
            );
        }
    }

    private static void assignNaturalBiomes(ServerLevel level, ChunkAccess chunk) {
        var registry = level.registryAccess().registryOrThrow(Registries.BIOME);
        Holder<Biome> deep = registry.getHolderOrThrow(ModBiomes.DEEP_ABYSSAL);
        Holder<Biome> valley = registry.getHolderOrThrow(ModBiomes.ABYSSAL_VALLEY);
        Holder<Biome> decay = registry.getHolderOrThrow(ModBiomes.DECAY_ROAD);
        long seed = level.getSeed();
        chunk.fillBiomesFromNoise((quartX, quartY, quartZ, sampler) -> {
            int blockX = QuartPos.toBlock(quartX) + 2;
            int blockZ = QuartPos.toBlock(quartZ) + 2;
            double radius = warpedBiomeRadius(seed, blockX, blockZ);
            if (radius <= ModBiomes.DEEP_ABYSSAL_RADIUS) {
                return deep;
            }
            if (radius <= ModBiomes.ABYSSAL_VALLEY_OUTER_RADIUS) {
                return valley;
            }
            return decay;
        }, Climate.empty());
        chunk.setUnsaved(true);
    }

    private static boolean hasNaturalBiomeTransitions(ChunkAccess chunk) {
        ChunkPos pos = chunk.getPos();
        return chunk.getBlockState(new BlockPos(
                pos.getMinBlockX() + 1,
                BEDROCK_BOUNDARY_Y - 1,
                pos.getMinBlockZ()
        )).is(Blocks.POLISHED_BLACKSTONE);
    }

    private static void markNaturalBiomeTransitions(ChunkAccess chunk) {
        ChunkPos pos = chunk.getPos();
        chunk.setBlockState(
                new BlockPos(pos.getMinBlockX() + 1, BEDROCK_BOUNDARY_Y - 1, pos.getMinBlockZ()),
                Blocks.POLISHED_BLACKSTONE.defaultBlockState(),
                false
        );
        chunk.setUnsaved(true);
    }

    private static void installBedrockBoundary(ChunkAccess chunk) {
        ChunkPos chunkPos = chunk.getPos();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int localX = 0; localX < 16; localX++) {
            for (int localZ = 0; localZ < 16; localZ++) {
                cursor.set(
                        chunkPos.getMinBlockX() + localX,
                        BEDROCK_BOUNDARY_Y,
                        chunkPos.getMinBlockZ() + localZ
                );
                if (!chunk.getBlockState(cursor).is(Blocks.BEDROCK)) {
                    chunk.setBlockState(cursor, Blocks.BEDROCK.defaultBlockState(), false);
                }
            }
        }
    }

    private static boolean hasCurrentDecayTerrain(ChunkAccess chunk) {
        ChunkPos pos = chunk.getPos();
        return chunk.getBlockState(new BlockPos(
                pos.getMinBlockX(),
                BEDROCK_BOUNDARY_Y - 1,
                pos.getMinBlockZ()
        )).is(Blocks.CUT_RED_SANDSTONE);
    }

    private static void markCurrentDecayTerrain(ChunkAccess chunk) {
        ChunkPos pos = chunk.getPos();
        chunk.setBlockState(
                new BlockPos(pos.getMinBlockX(), BEDROCK_BOUNDARY_Y - 1, pos.getMinBlockZ()),
                Blocks.CUT_RED_SANDSTONE.defaultBlockState(),
                false
        );
    }

    private static TerrainRegion getRegion(ChunkPos chunkPos) {
        BlockPos center = new BlockPos(
                chunkPos.getMinBlockX() + 8,
                STONE_FLOOR_Y,
                chunkPos.getMinBlockZ() + 8
        );
        if (ModBiomes.isDeepAbyssal(center)) {
            return TerrainRegion.DEEP_ABYSSAL;
        }
        if (ModBiomes.isAbyssalValley(center)) {
            return TerrainRegion.ABYSSAL_VALLEY;
        }
        return TerrainRegion.DECAY_ROAD;
    }

    private static void shapeSeabed(ChunkAccess chunk, long seed, boolean valley) {
        ChunkPos chunkPos = chunk.getPos();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int localX = 0; localX < 16; localX++) {
            int worldX = chunkPos.getMinBlockX() + localX;

            for (int localZ = 0; localZ < 16; localZ++) {
                int worldZ = chunkPos.getMinBlockZ() + localZ;
                int sandHeight = calculateTransitionedSandHeight(seed, worldX, worldZ);

                for (int y = STONE_FLOOR_Y - 3; y <= STONE_FLOOR_Y; y++) {
                    cursor.set(worldX, y, worldZ);
                    if (chunk.getBlockState(cursor).is(Blocks.DEEPSLATE)) {
                        chunk.setBlockState(cursor, Blocks.SANDSTONE.defaultBlockState(), false);
                    }
                }

                for (int y = STONE_FLOOR_Y + 1; y <= STONE_FLOOR_Y + sandHeight; y++) {
                    cursor.set(worldX, y, worldZ);
                    BlockState state = chunk.getBlockState(cursor);

                    if (state.is(Blocks.WATER) || state.isAir()) {
                        chunk.setBlockState(cursor, Blocks.SAND.defaultBlockState(), false);
                    }
                }
            }
        }
    }

    private static void shapeDecayRoad(
            ChunkAccess chunk,
            long seed,
            boolean repairLegacyTunnel
    ) {
        ChunkPos chunkPos = chunk.getPos();
        BlockPos tunnelCenter = AbyssalTunnelLocator.getCenter(seed);
        BlockPos legacyTunnelCenter = repairLegacyTunnel
                ? AbyssalTunnelLocator.getLegacyCenter(seed)
                : null;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        BlockState water = Blocks.WATER.defaultBlockState();
        BlockState redSand = ModBlocks.DECAYED_ABYSSAL_SAND.get().defaultBlockState();
        BlockState redStone = Blocks.RED_SANDSTONE.defaultBlockState();

        for (int localX = 0; localX < 16; localX++) {
            int worldX = chunkPos.getMinBlockX() + localX;

            for (int localZ = 0; localZ < 16; localZ++) {
                int worldZ = chunkPos.getMinBlockZ() + localZ;
                double tunnelDistance = AbyssalTunnelLocator.horizontalDistance(
                        tunnelCenter,
                        worldX,
                        worldZ
                );
                if (legacyTunnelCenter != null
                        && tunnelDistance > AbyssalTunnelLocator.CAVERN_RADIUS
                        && AbyssalTunnelLocator.horizontalDistance(
                                legacyTunnelCenter,
                                worldX,
                                worldZ
                        ) <= AbyssalTunnelLocator.CAVERN_RADIUS) {
                    restoreDecayRoadColumn(
                            chunk,
                            cursor,
                            seed,
                            worldX,
                            worldZ,
                            water,
                            redSand,
                            redStone
                    );
                }

                int surfaceY = calculateDecayRoadSurfaceY(seed, worldX, worldZ, tunnelDistance);
                int cavernFloorY = calculateTunnelFloorY(seed, worldX, worldZ);
                int cavernCeilingY = calculateTunnelCeilingY(seed, worldX, worldZ);
                double cavernRadius = calculateIrregularCavernRadius(seed, worldX, worldZ);

                for (int y = BEDROCK_BOUNDARY_Y + 1; y <= surfaceY; y++) {
                    double shaftRadius = calculateIrregularShaftRadius(seed, worldX, worldZ, y);
                    boolean inCavern = y > cavernFloorY
                            && y < cavernCeilingY
                            && tunnelDistance <= cavernRadius;
                    boolean inShaft = y >= cavernCeilingY - 3
                            && tunnelDistance <= shaftRadius;

                    if (inCavern || inShaft) {
                        setIfDifferent(chunk, cursor.set(worldX, y, worldZ), water);
                    } else {
                        setIfDifferent(
                                chunk,
                                cursor.set(worldX, y, worldZ),
                                y >= surfaceY - 2
                                        ? redSand
                                        : selectTunnelRock(seed, worldX, y, worldZ)
                        );
                    }
                }

                fillColumn(
                        chunk,
                        cursor,
                        worldX,
                        worldZ,
                        surfaceY + 1,
                        STONE_FLOOR_Y + 22,
                        water
                );
            }
        }

        chunk.setUnsaved(true);
    }

    private static void restoreDecayRoadColumn(
            ChunkAccess chunk,
            BlockPos.MutableBlockPos cursor,
            long seed,
            int worldX,
            int worldZ,
            BlockState water,
            BlockState redSand,
            BlockState redStone
    ) {
        int surfaceY = calculateDecayRoadBaseSurfaceY(seed, worldX, worldZ);
        for (int y = BEDROCK_BOUNDARY_Y + 1; y <= surfaceY; y++) {
            setIfDifferent(
                    chunk,
                    cursor.set(worldX, y, worldZ),
                    y >= surfaceY - 2 ? redSand : redStone
            );
        }
        fillColumn(
                chunk,
                cursor,
                worldX,
                worldZ,
                surfaceY + 1,
                STONE_FLOOR_Y + 14,
                water
        );
    }

    private static void replaceLegacyAbyssalWater(ChunkAccess chunk) {
        BlockState vanillaWater = Blocks.WATER.defaultBlockState();
        int minSection = chunk.getSectionIndex(BEDROCK_BOUNDARY_Y + 1);
        int maxSection = chunk.getSectionIndex(ABYSSAL_WATER_SURFACE_Y);

        for (int sectionIndex = minSection; sectionIndex <= maxSection; sectionIndex++) {
            LevelChunkSection section = chunk.getSection(sectionIndex);
            int sectionY = chunk.getSectionYFromSectionIndex(sectionIndex);
            int minLocalY = Math.max(0, BEDROCK_BOUNDARY_Y + 1 - (sectionY << 4));
            int maxLocalY = Math.min(15, ABYSSAL_WATER_SURFACE_Y - (sectionY << 4));

            for (int localY = minLocalY; localY <= maxLocalY; localY++) {
                for (int localX = 0; localX < 16; localX++) {
                    for (int localZ = 0; localZ < 16; localZ++) {
                        BlockState state = section.getBlockState(localX, localY, localZ);
                        if (ModFluids.isAbyssalWater(state.getFluidState())) {
                            section.setBlockState(localX, localY, localZ, vanillaWater, false);
                        }
                    }
                }
            }
        }
    }

    private static void fillColumn(
            ChunkAccess chunk,
            BlockPos.MutableBlockPos cursor,
            int x,
            int z,
            int minY,
            int maxY,
            BlockState state
    ) {
        for (int y = minY; y <= maxY; y++) {
            setIfDifferent(chunk, cursor.set(x, y, z), state);
        }
    }

    private static void setIfDifferent(ChunkAccess chunk, BlockPos pos, BlockState state) {
        if (chunk.getBlockState(pos).equals(state)) {
            return;
        }

        if (state.is(Blocks.WATER)) {
            setWaterWithoutBlockCallbacks(chunk, pos, state);
            return;
        }

        chunk.setBlockState(pos, state, false);
    }

    private static void setWaterWithoutBlockCallbacks(
            ChunkAccess chunk,
            BlockPos pos,
            BlockState water
    ) {
        int localX = pos.getX() & 15;
        int localY = pos.getY() & 15;
        int localZ = pos.getZ() & 15;
        LevelChunkSection section = chunk.getSection(chunk.getSectionIndex(pos.getY()));
        section.setBlockState(localX, localY, localZ, water, false);

        for (java.util.Map.Entry<Heightmap.Types, Heightmap> entry : chunk.getHeightmaps()) {
            entry.getValue().update(localX, pos.getY(), localZ, water);
        }
        chunk.setUnsaved(true);
    }

    private static int calculateDecayRoadSurfaceY(long seed, int x, int z, double tunnelDistance) {
        int surfaceY = calculateDecayRoadBaseSurfaceY(seed, x, z);

        if (tunnelDistance > AbyssalTunnelLocator.SHAFT_RADIUS
                && tunnelDistance < AbyssalTunnelLocator.RIM_RADIUS) {
            double rimProgress = (tunnelDistance - AbyssalTunnelLocator.SHAFT_RADIUS)
                    / (AbyssalTunnelLocator.RIM_RADIUS - AbyssalTunnelLocator.SHAFT_RADIUS);
            surfaceY += (int) Math.round(Math.sin(rimProgress * Math.PI) * 8.0D);
        }

        return Mth.clamp(surfaceY, STONE_FLOOR_Y + 1, STONE_FLOOR_Y + 14);
    }

    private static int calculateDecayRoadBaseSurfaceY(long seed, int x, int z) {
        double waves = Math.sin((x + seedOffset(seed, 9)) * 0.035D) * 2.1D
                + Math.cos((z + seedOffset(seed, 10)) * 0.031D) * 1.8D
                + Math.abs(Math.sin((x - z + seedOffset(seed, 11)) * 0.014D)) * 2.5D;
        return Mth.clamp(
                STONE_FLOOR_Y + 3 + (int) Math.round(waves),
                STONE_FLOOR_Y + 1,
                STONE_FLOOR_Y + 14
        );
    }

    private static int calculateTunnelFloorY(long seed, int x, int z) {
        double broad = Math.sin((x + seedOffset(seed, 15)) * 0.15D)
                + Math.cos((z - seedOffset(seed, 16)) * 0.13D);
        int rough = (int) Math.floorMod(mix(seed ^ TUNNEL_ROCK_SALT, x, z), 3L) - 1;
        return Mth.clamp(
                AbyssalTunnelLocator.BOTTOM_FLOOR_Y + 2 + (int) Math.round(broad) + rough,
                AbyssalTunnelLocator.BOTTOM_FLOOR_Y,
                AbyssalTunnelLocator.BOTTOM_FLOOR_Y + 5
        );
    }

    private static int calculateTunnelCeilingY(long seed, int x, int z) {
        double waves = Math.sin((x - seedOffset(seed, 17)) * 0.11D) * 2.0D
                + Math.cos((z + seedOffset(seed, 18)) * 0.09D) * 1.5D;
        return Mth.clamp(
                AbyssalTunnelLocator.CAVERN_CEILING_Y + (int) Math.round(waves),
                AbyssalTunnelLocator.CAVERN_CEILING_Y - 4,
                AbyssalTunnelLocator.CAVERN_CEILING_Y + 5
        );
    }

    private static double calculateIrregularCavernRadius(long seed, int x, int z) {
        double ridges = Math.sin((x + seedOffset(seed, 19)) * 0.19D) * 3.2D
                + Math.cos((z - seedOffset(seed, 20)) * 0.17D) * 2.7D
                + Math.sin((x + z) * 0.071D) * 2.1D;
        double rough = unit(mix(seed ^ TUNNEL_ROCK_SALT, x >> 1, z >> 1)) * 5.0D - 2.5D;
        return AbyssalTunnelLocator.CAVERN_RADIUS + ridges + rough;
    }

    private static double calculateIrregularShaftRadius(long seed, int x, int z, int y) {
        double verticalBend = Math.sin((y + seedOffset(seed, 21)) * 0.27D) * 2.4D;
        double wallNoise = Math.sin((x - z) * 0.16D) * 2.0D
                + Math.cos((x + z) * 0.12D) * 1.7D
                + (unit(mix(seed ^ (TUNNEL_ROCK_SALT + y * 31L), x, z)) * 4.0D - 2.0D);
        return AbyssalTunnelLocator.SHAFT_RADIUS + verticalBend + wallNoise;
    }

    private static BlockState selectTunnelRock(long seed, int x, int y, int z) {
        long hash = mix64(seed ^ TUNNEL_ROCK_SALT
                ^ ((long) (x >> 1) * 341873128712L)
                ^ ((long) (z >> 1) * 132897987541L)
                ^ ((long) (y >> 1) * 42317861L));
        int value = (int) Math.floorMod(hash, 100L);
        if (value < 12) {
            return Blocks.BLACKSTONE.defaultBlockState();
        }
        if (value < 28) {
            return Blocks.TUFF.defaultBlockState();
        }
        if (value < 42) {
            return Blocks.DEEPSLATE.defaultBlockState();
        }
        return Blocks.RED_SANDSTONE.defaultBlockState();
    }

    private static void ensureTunnelStructureStart(ServerLevel level, ChunkAccess chunk) {
        if (!AbyssalTunnelLocator.isCenterChunk(level.getSeed(), chunk.getPos())) {
            return;
        }

        Structure structure = level.registryAccess()
                .registryOrThrow(Registries.STRUCTURE)
                .get(AbyssalPlanet.id("abyssal_tunnel"));
        if (structure == null) {
            AbyssalPlanet.LOGGER.error("Abyssal Tunnel structure is missing from the world registry");
            return;
        }

        StructureStart existingStart = chunk.getStartForStructure(structure);
        if (existingStart != null && existingStart.isValid()) {
            return;
        }

        BlockPos center = AbyssalTunnelLocator.getCenter(level.getSeed());
        StructureStart start = new StructureStart(
                structure,
                chunk.getPos(),
                0,
                new PiecesContainer(List.of(new AbyssalTunnelPiece(center)))
        );
        chunk.setStartForStructure(structure, start);
        chunk.setUnsaved(true);
    }

    private static void ensureGeorgesBriochard(ServerLevel level, ChunkPos chunkPos) {
        if (!AbyssalTunnelLocator.isCenterChunk(level.getSeed(), chunkPos)) {
            return;
        }

        MinecraftServer server = level.getServer();
        PENDING_GEORGES_CHECKS.putIfAbsent(server, server.getTickCount() + 2);
    }

    private static void ensureGeorgesBriochardNow(ServerLevel level) {
        AbyssalTunnelData tunnelData = AbyssalTunnelData.get(level);
        boolean migrating = tunnelData.needsSouthernTunnelMigration();
        BlockPos center = AbyssalTunnelLocator.getCenter(level.getSeed());
        if (!level.hasChunkAt(center)) {
            MinecraftServer server = level.getServer();
            PENDING_GEORGES_CHECKS.put(server, server.getTickCount() + 2);
            return;
        }

        ensureAbyssalEggs(level, tunnelData, center);
        if (tunnelData.isGeorgesDefeated()) {
            tunnelData.markSouthernTunnelMigrated();
            return;
        }

        List<GeorgesBriochardEntity> existingBosses = level.getEntitiesOfClass(
                GeorgesBriochardEntity.class,
                new net.minecraft.world.phys.AABB(center).inflate(
                        AbyssalTunnelLocator.CAVERN_RADIUS,
                        32.0D,
                        AbyssalTunnelLocator.CAVERN_RADIUS
                )
        );
        if (!existingBosses.isEmpty()) {
            GeorgesBriochardEntity keeper = existingBosses.get(0);
            keeper.setTunnelHome(center);
            existingBosses.stream().skip(1).forEach(GeorgesBriochardEntity::discard);
            tunnelData.markGeorgesSpawned();
            tunnelData.markSouthernTunnelMigrated();
            return;
        }

        if (!tunnelData.shouldSpawnGeorges() && !migrating) {
            return;
        }

        GeorgesBriochardEntity georges = ModEntities.GEORGES_BRIOCHARD.get().create(level);
        if (georges == null) {
            return;
        }

        georges.moveTo(
                center.getX() + 0.5D,
                AbyssalTunnelLocator.BOSS_SPAWN_Y,
                center.getZ() + 0.5D,
                level.random.nextFloat() * 360.0F,
                0.0F
        );
        georges.setTunnelHome(center);
        georges.setPersistenceRequired();
        georges.finalizeSpawn(
                level,
                new DifficultyInstance(
                        level.getDifficulty(),
                        level.getDayTime(),
                        0L,
                        level.getMoonBrightness()
                ),
                MobSpawnType.STRUCTURE,
                null,
                null
        );

        if (level.addFreshEntity(georges)) {
            tunnelData.markGeorgesSpawned();
            tunnelData.markSouthernTunnelMigrated();
        }
    }

    private static void ensureAbyssalEggs(
            ServerLevel level,
            AbyssalTunnelData tunnelData,
            BlockPos center
    ) {
        if (!tunnelData.needsEggPlacementValidation()
                || !isTunnelEggAreaReady(level, center)) {
            return;
        }

        RandomSource random = RandomSource.create(level.getSeed() ^ TUNNEL_EGG_SALT);
        int desiredCount = tunnelData.getGeneratedEggCount();
        if (desiredCount < 1) {
            int roll = random.nextInt(100);
            desiredCount = roll < 50 ? 1
                    : roll < 75 ? 2
                    : roll < 88 ? 3
                    : roll < 95 ? 4
                    : roll < 99 ? 5
                    : 6;
        }

        List<BlockPos> visibleEggs = findVisibleTunnelEggs(level, center);
        for (int index = visibleEggs.size(); index < desiredCount; index++) {
            boolean placed = false;
            for (int attempt = 0; attempt < 80; attempt++) {
                double angle = random.nextDouble() * Math.PI * 2.0D;
                double radius = 8.0D + random.nextDouble() * 14.0D;
                int x = center.getX() + Mth.floor(Math.cos(angle) * radius);
                int z = center.getZ() + Mth.floor(Math.sin(angle) * radius);
                BlockPos eggPos = findTunnelFloorWater(level, x, z);
                if (eggPos == null || isTooCloseToAnotherEgg(visibleEggs, eggPos)) {
                    continue;
                }

                boolean changed = level.setBlock(
                        eggPos,
                        ModBlocks.ABYSSAL_EGG.get().defaultBlockState()
                                .setValue(AbyssalEggBlock.WATERLOGGED, true),
                        3
                );
                if (changed && level.getBlockState(eggPos).is(ModBlocks.ABYSSAL_EGG.get())) {
                    visibleEggs.add(eggPos.immutable());
                    placed = true;
                    break;
                }
            }
            if (!placed) {
                break;
            }
        }

        if (visibleEggs.size() >= desiredCount) {
            tunnelData.markEggsGenerated(visibleEggs.size());
            AbyssalPlanet.LOGGER.info(
                    "Validated {} visible Abyssal Egg(s) at the Abyssal Tunnel: {}",
                    visibleEggs.size(),
                    visibleEggs
            );
        }
    }

    private static boolean isTunnelEggAreaReady(ServerLevel level, BlockPos center) {
        int minimumChunkX = Math.floorDiv(center.getX() - TUNNEL_EGG_SCAN_RADIUS, 16);
        int maximumChunkX = Math.floorDiv(center.getX() + TUNNEL_EGG_SCAN_RADIUS, 16);
        int minimumChunkZ = Math.floorDiv(center.getZ() - TUNNEL_EGG_SCAN_RADIUS, 16);
        int maximumChunkZ = Math.floorDiv(center.getZ() + TUNNEL_EGG_SCAN_RADIUS, 16);

        for (int chunkX = minimumChunkX; chunkX <= maximumChunkX; chunkX++) {
            for (int chunkZ = minimumChunkZ; chunkZ <= maximumChunkZ; chunkZ++) {
                LevelChunk chunk = level.getChunkSource().getChunkNow(chunkX, chunkZ);
                if (chunk == null || !hasCurrentDecayTerrain(chunk)) {
                    return false;
                }
            }
        }
        return true;
    }

    private static List<BlockPos> findVisibleTunnelEggs(ServerLevel level, BlockPos center) {
        List<BlockPos> visibleEggs = new ArrayList<>();
        int minimumY = AbyssalTunnelLocator.BOTTOM_FLOOR_Y + 1;
        int maximumY = AbyssalTunnelLocator.CAVERN_CEILING_Y + 5;

        for (int x = center.getX() - TUNNEL_EGG_SCAN_RADIUS;
                x <= center.getX() + TUNNEL_EGG_SCAN_RADIUS;
                x++) {
            for (int z = center.getZ() - TUNNEL_EGG_SCAN_RADIUS;
                    z <= center.getZ() + TUNNEL_EGG_SCAN_RADIUS;
                    z++) {
                if (AbyssalTunnelLocator.horizontalDistance(center, x, z) > TUNNEL_EGG_SCAN_RADIUS) {
                    continue;
                }
                for (int y = minimumY; y <= maximumY; y++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!level.getBlockState(pos).is(ModBlocks.ABYSSAL_EGG.get())) {
                        continue;
                    }
                    if (isVisibleTunnelEgg(level, center, pos)) {
                        visibleEggs.add(pos);
                    } else {
                        level.setBlock(pos, replacementForBuriedEgg(level, pos), 3);
                    }
                }
            }
        }
        return visibleEggs;
    }

    private static boolean isVisibleTunnelEgg(ServerLevel level, BlockPos center, BlockPos pos) {
        if (AbyssalTunnelLocator.horizontalDistance(center, pos.getX(), pos.getZ())
                > TUNNEL_EGG_VISIBLE_RADIUS) {
            return false;
        }
        BlockPos floor = pos.below();
        if (!level.getBlockState(floor).isFaceSturdy(level, floor, net.minecraft.core.Direction.UP)
                || !isVanillaWater(level, pos.above())) {
            return false;
        }

        int openSides = 0;
        for (net.minecraft.core.Direction direction : net.minecraft.core.Direction.Plane.HORIZONTAL) {
            if (isVanillaWater(level, pos.relative(direction))) {
                openSides++;
            }
        }
        return openSides >= 2;
    }

    private static BlockState replacementForBuriedEgg(ServerLevel level, BlockPos pos) {
        if (isVanillaWater(level, pos.above())) {
            return Blocks.WATER.defaultBlockState();
        }
        return selectTunnelRock(level.getSeed(), pos.getX(), pos.getY(), pos.getZ());
    }

    private static boolean isTooCloseToAnotherEgg(List<BlockPos> eggs, BlockPos candidate) {
        for (BlockPos egg : eggs) {
            double dx = egg.getX() - candidate.getX();
            double dy = egg.getY() - candidate.getY();
            double dz = egg.getZ() - candidate.getZ();
            if (dx * dx + dy * dy + dz * dz < 36.0D) {
                return true;
            }
        }
        return false;
    }

    private static boolean isVanillaWater(ServerLevel level, BlockPos pos) {
        return level.getFluidState(pos).getType() == net.minecraft.world.level.material.Fluids.WATER;
    }

    private static BlockPos findTunnelFloorWater(ServerLevel level, int x, int z) {
        for (int y = AbyssalTunnelLocator.CAVERN_CEILING_Y - 2;
                y >= AbyssalTunnelLocator.BOTTOM_FLOOR_Y + 1;
                y--) {
            BlockPos water = new BlockPos(x, y, z);
            BlockPos floor = water.below();
            if (isVanillaWater(level, water)
                    && isVanillaWater(level, water.above())
                    && level.getBlockState(floor).isFaceSturdy(level, floor, net.minecraft.core.Direction.UP)) {
                return water;
            }
        }
        return null;
    }

    private static int calculateValleySandHeight(long seed, int x, int z) {
        return calculateTransitionedSandHeight(seed, x, z);
    }

    private static int calculateTransitionedSandHeight(long seed, int x, int z) {
        double radius = warpedBiomeRadius(seed, x, z);
        int deepHeight = calculateDeepSandHeightRaw(seed, x, z);
        int valleyHeight = calculateValleySandHeightRaw(seed, x, z);
        double deepBlend = smoothStep(
                ModBiomes.DEEP_ABYSSAL_RADIUS - DEEP_TRANSITION_INNER_WIDTH,
                ModBiomes.DEEP_ABYSSAL_RADIUS + DEEP_TRANSITION_OUTER_WIDTH,
                radius
        );
        double blendedHeight = Mth.lerp(deepBlend, deepHeight, valleyHeight);

        double decayBlend = smoothStep(
                ModBiomes.ABYSSAL_VALLEY_OUTER_RADIUS - DECAY_TRANSITION_WIDTH,
                ModBiomes.ABYSSAL_VALLEY_OUTER_RADIUS,
                radius
        );
        int decayHeight = calculateDecayRoadBaseSurfaceY(seed, x, z) - STONE_FLOOR_Y;
        blendedHeight = Mth.lerp(decayBlend, blendedHeight, decayHeight);
        return Mth.clamp((int) Math.round(blendedHeight), 1, 108);
    }

    private static int calculateValleySandHeightRaw(long seed, int x, int z) {
        double waves = Math.sin((x + seedOffset(seed, 4)) * 0.018D)
                + Math.cos((z + seedOffset(seed, 5)) * 0.021D);
        int baseHeight = 3 + Mth.clamp((int) Math.floor(waves + 2.0D), 0, 4);
        int giantDuneHeight = giantDuneContribution(seed, x, z);
        return Mth.clamp(baseHeight + giantDuneHeight, 3, 108);
    }

    private static int giantDuneContribution(long seed, int x, int z) {
        int cellX = Math.floorDiv(x, VALLEY_DUNE_CELL_SIZE);
        int cellZ = Math.floorDiv(z, VALLEY_DUNE_CELL_SIZE);
        int best = 0;

        for (int offsetX = -1; offsetX <= 1; offsetX++) {
            for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
                int candidateX = cellX + offsetX;
                int candidateZ = cellZ + offsetZ;
                long hash = mix(seed ^ (TERRAIN_SALT * 7L), candidateX, candidateZ);
                double centerX = candidateX * VALLEY_DUNE_CELL_SIZE
                        + 24.0D + unit(hash) * (VALLEY_DUNE_CELL_SIZE - 48.0D);
                double centerZ = candidateZ * VALLEY_DUNE_CELL_SIZE
                        + 24.0D + unit(mix64(hash)) * (VALLEY_DUNE_CELL_SIZE - 48.0D);
                double longRadius = 72.0D + unit(mix64(hash + 1)) * 42.0D;
                double shortRadius = 38.0D + unit(mix64(hash + 2)) * 28.0D;
                double angle = unit(mix64(hash + 3)) * Math.PI;
                double dx = x - centerX;
                double dz = z - centerZ;
                double rotatedX = dx * Math.cos(angle) + dz * Math.sin(angle);
                double rotatedZ = -dx * Math.sin(angle) + dz * Math.cos(angle);
                double distance = square(rotatedX / longRadius) + square(rotatedZ / shortRadius);

                if (distance < 1.0D) {
                    double profile = Math.pow(1.0D - distance, 1.35D);
                    double peakHeight = 76.0D + unit(mix64(hash + 4)) * 29.0D;
                    best = Math.max(best, (int) Math.round(profile * peakHeight));
                }
            }
        }

        return best;
    }

    private static int calculateSandHeight(long seed, int x, int z) {
        return calculateTransitionedSandHeight(seed, x, z);
    }

    private static int calculateDeepSandHeightRaw(long seed, int x, int z) {
        double broadWaves = Math.sin((x + seedOffset(seed, 0)) * 0.050D)
                + Math.cos((z + seedOffset(seed, 1)) * 0.044D);
        double crossingWaves = Math.sin((x + z + seedOffset(seed, 2)) * 0.025D);
        int baseHeight = 1 + Mth.clamp((int) Math.floor((broadWaves + crossingWaves + 3.0D) / 2.3D), 0, 2);

        int duneHeight = duneContribution(seed, x, z);
        int domeHeight = domeContribution(seed, x, z);
        return Mth.clamp(baseHeight + Math.max(duneHeight, domeHeight), 1, 12);
    }

    private static double warpedBiomeRadius(long seed, int x, int z) {
        double radius = Math.sqrt(square(x) + square(z));
        double broadWarp = Math.sin((x + seedOffset(seed, 24)) * 0.0085D) * 18.0D
                + Math.cos((z - seedOffset(seed, 25)) * 0.0095D) * 15.0D;
        double crossingWarp = Math.sin((x + z + seedOffset(seed, 26)) * 0.0045D) * 10.0D;
        return radius + broadWarp + crossingWarp;
    }

    private static double smoothStep(double start, double end, double value) {
        double progress = Mth.clamp((value - start) / (end - start), 0.0D, 1.0D);
        return progress * progress * (3.0D - 2.0D * progress);
    }

    private static int duneContribution(long seed, int x, int z) {
        int cellX = Math.floorDiv(x, DUNE_CELL_SIZE);
        int cellZ = Math.floorDiv(z, DUNE_CELL_SIZE);
        int best = 0;

        for (int offsetX = -1; offsetX <= 1; offsetX++) {
            for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
                int candidateX = cellX + offsetX;
                int candidateZ = cellZ + offsetZ;
                long hash = mix(seed ^ TERRAIN_SALT, candidateX, candidateZ);
                double centerX = candidateX * DUNE_CELL_SIZE + 8 + unit(hash) * 32.0D;
                double centerZ = candidateZ * DUNE_CELL_SIZE + 8 + unit(mix64(hash)) * 32.0D;
                double longRadius = 15.0D + unit(mix64(hash + 1)) * 11.0D;
                double shortRadius = 5.5D + unit(mix64(hash + 2)) * 5.0D;
                double angle = unit(mix64(hash + 3)) * Math.PI;
                double dx = x - centerX;
                double dz = z - centerZ;
                double rotatedX = dx * Math.cos(angle) + dz * Math.sin(angle);
                double rotatedZ = -dx * Math.sin(angle) + dz * Math.cos(angle);
                double distance = square(rotatedX / longRadius) + square(rotatedZ / shortRadius);

                if (distance < 1.0D) {
                    double profile = square(1.0D - distance);
                    int height = (int) Math.round(profile * (4.0D + unit(mix64(hash + 4)) * 4.0D));
                    best = Math.max(best, height);
                }
            }
        }

        return best;
    }

    private static int domeContribution(long seed, int x, int z) {
        int cellX = Math.floorDiv(x, DOME_CELL_SIZE);
        int cellZ = Math.floorDiv(z, DOME_CELL_SIZE);
        int best = 0;

        for (int offsetX = -1; offsetX <= 1; offsetX++) {
            for (int offsetZ = -1; offsetZ <= 1; offsetZ++) {
                int candidateX = cellX + offsetX;
                int candidateZ = cellZ + offsetZ;
                long hash = mix(seed ^ (TERRAIN_SALT * 3L), candidateX, candidateZ);

                if ((hash & 3L) != 0L) {
                    continue;
                }

                double centerX = candidateX * DOME_CELL_SIZE + 18 + unit(hash) * 76.0D;
                double centerZ = candidateZ * DOME_CELL_SIZE + 18 + unit(mix64(hash)) * 76.0D;
                double radius = 8.0D + unit(mix64(hash + 1)) * 7.0D;
                double distance = Math.sqrt(square((x - centerX) / radius) + square((z - centerZ) / radius));

                if (distance < 1.0D) {
                    double profile = Math.sqrt(Math.max(0.0D, 1.0D - distance * distance));
                    int height = (int) Math.round(profile * (6.0D + unit(mix64(hash + 2)) * 5.0D));
                    best = Math.max(best, height);
                }
            }
        }

        return best;
    }

    private static void generateOreVeins(ChunkAccess chunk, long seed, boolean valley) {
        RandomSource random = RandomSource.create(mix64(seed ^ ORE_SALT ^ chunk.getPos().toLong()));

        int goldVeins = valley ? 6 + random.nextInt(4) : 2 + random.nextInt(3);
        for (int vein = 0; vein < goldVeins; vein++) {
            generateVein(
                    chunk,
                    random,
                    Blocks.DEEPSLATE_GOLD_ORE.defaultBlockState(),
                    12 + random.nextInt(26),
                    valley ? 7 + random.nextInt(8) : 4 + random.nextInt(5),
                    false,
                    BEDROCK_BOUNDARY_Y + 1,
                    STONE_FLOOR_Y + 7
            );
        }

        int blueGoldVeins = valley ? 5 + random.nextInt(4) : 1 + random.nextInt(2);
        for (int vein = 0; vein < blueGoldVeins; vein++) {
            if (valley || random.nextFloat() <= 0.72F) {
                generateVein(
                        chunk,
                        random,
                        ModBlocks.BLUE_GOLD_ORE.get().defaultBlockState(),
                        34 + random.nextInt(12),
                        valley ? 6 + random.nextInt(8) : 3 + random.nextInt(4),
                        true,
                        BEDROCK_BOUNDARY_Y + 1,
                        STONE_FLOOR_Y + 7
                );
            }
        }

        if (valley) {
            generateVisibleDuneVeins(chunk, seed, random);
        }
    }

    private static void generateVisibleDuneVeins(ChunkAccess chunk, long seed, RandomSource random) {
        int blueDeposits = 7 + random.nextInt(5);
        for (int vein = 0; vein < blueDeposits; vein++) {
            generateVisibleDuneVein(
                    chunk,
                    seed,
                    random,
                    ModBlocks.BLUE_GOLD_ORE.get().defaultBlockState(),
                    8 + random.nextInt(9)
            );
        }

        int goldDeposits = 4 + random.nextInt(4);
        for (int vein = 0; vein < goldDeposits; vein++) {
            generateVisibleDuneVein(
                    chunk,
                    seed,
                    random,
                    Blocks.GOLD_ORE.defaultBlockState(),
                    7 + random.nextInt(8)
            );
        }
    }

    private static void generateVisibleDuneVein(
            ChunkAccess chunk,
            long seed,
            RandomSource random,
            BlockState ore,
            int size
    ) {
        ChunkPos chunkPos = chunk.getPos();
        int x = chunkPos.getMinBlockX() + random.nextInt(16);
        int z = chunkPos.getMinBlockZ() + random.nextInt(16);
        int surfaceY = STONE_FLOOR_Y + calculateValleySandHeight(seed, x, z);
        int startY = Math.max(STONE_FLOOR_Y - 2, surfaceY - random.nextInt(4));
        generateVeinAt(chunk, random, ore, x, startY, z, size, true, STONE_FLOOR_Y - 3, surfaceY);
    }

    private static void generateAbyssalAlgae(ChunkAccess chunk, long seed) {
        RandomSource random = RandomSource.create(
                mix64(seed ^ ALGAE_SALT ^ chunk.getPos().toLong())
        );
        ChunkPos chunkPos = chunk.getPos();
        BlockState lowerAlgae = ModBlocks.ABYSSAL_ALGAE.get()
                .defaultBlockState()
                .setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER);
        BlockState upperAlgae = ModBlocks.ABYSSAL_ALGAE.get()
                .defaultBlockState()
                .setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER);
        int attempts = 6 + random.nextInt(7);

        for (int attempt = 0; attempt < attempts; attempt++) {
            if (random.nextFloat() > 0.75F) {
                continue;
            }

            int x = chunkPos.getMinBlockX() + random.nextInt(16);
            int z = chunkPos.getMinBlockZ() + random.nextInt(16);
            int surfaceY = STONE_FLOOR_Y + calculateValleySandHeight(seed, x, z);
            BlockPos lowerPos = new BlockPos(x, surfaceY + 1, z);
            BlockPos upperPos = lowerPos.above();

            if (!chunk.getBlockState(lowerPos.below()).is(Blocks.SAND)
                    || !chunk.getFluidState(lowerPos).is(FluidTags.WATER)
                    || !chunk.getFluidState(upperPos).is(FluidTags.WATER)) {
                continue;
            }

            chunk.setBlockState(lowerPos, lowerAlgae, false);
            chunk.setBlockState(upperPos, upperAlgae, false);
        }
    }

    private static void generateBlueGoldTreeOnEmergentDune(
            ChunkAccess chunk,
            long seed,
            BlueGoldTreeSpawnData spawnData
    ) {
        ChunkPos chunkPos = chunk.getPos();
        int minimumCellX = Math.floorDiv(chunkPos.getMinBlockX(), VALLEY_DUNE_CELL_SIZE) - 1;
        int maximumCellX = Math.floorDiv(chunkPos.getMaxBlockX(), VALLEY_DUNE_CELL_SIZE) + 1;
        int minimumCellZ = Math.floorDiv(chunkPos.getMinBlockZ(), VALLEY_DUNE_CELL_SIZE) - 1;
        int maximumCellZ = Math.floorDiv(chunkPos.getMaxBlockZ(), VALLEY_DUNE_CELL_SIZE) + 1;

        for (int cellX = minimumCellX; cellX <= maximumCellX; cellX++) {
            for (int cellZ = minimumCellZ; cellZ <= maximumCellZ; cellZ++) {
                long duneHash = mix(seed ^ (TERRAIN_SALT * 7L), cellX, cellZ);
                int duneCenterX = Mth.floor(
                        cellX * (double) VALLEY_DUNE_CELL_SIZE
                                + 24.0D
                                + unit(duneHash) * (VALLEY_DUNE_CELL_SIZE - 48.0D)
                );
                int duneCenterZ = Mth.floor(
                        cellZ * (double) VALLEY_DUNE_CELL_SIZE
                                + 24.0D
                                + unit(mix64(duneHash)) * (VALLEY_DUNE_CELL_SIZE - 48.0D)
                );

                if (!isInsideChunk(chunkPos, duneCenterX, duneCenterZ)
                        || !spawnData.markDuneProcessed(cellX, cellZ)
                        || unit(mix64(duneHash ^ BLUE_GOLD_TREE_SALT)) >= 0.5D) {
                    continue;
                }

                int treeX = Mth.clamp(
                        duneCenterX,
                        chunkPos.getMinBlockX() + 3,
                        chunkPos.getMaxBlockX() - 3
                );
                int treeZ = Mth.clamp(
                        duneCenterZ,
                        chunkPos.getMinBlockZ() + 3,
                        chunkPos.getMaxBlockZ() - 3
                );
                int surfaceY = STONE_FLOOR_Y + calculateValleySandHeight(seed, treeX, treeZ);
                BlockPos base = new BlockPos(treeX, surfaceY + 1, treeZ);

                if (surfaceY <= ABYSSAL_WATER_SURFACE_Y
                        || !chunk.getBlockState(base.below()).is(Blocks.SAND)
                        || !chunk.getBlockState(base).isAir()) {
                    continue;
                }

                RandomSource treeRandom = RandomSource.create(mix64(duneHash ^ BLUE_GOLD_TREE_SALT));
                BlueGoldTreeGenerator.generate(chunk, base, treeRandom);
            }
        }
    }

    private static void generateVein(
            ChunkAccess chunk,
            RandomSource random,
            BlockState ore,
            int startY,
            int size,
            boolean replaceSandstone,
            int minY,
            int maxY
    ) {
        ChunkPos chunkPos = chunk.getPos();
        int x = chunkPos.getMinBlockX() + random.nextInt(16);
        int z = chunkPos.getMinBlockZ() + random.nextInt(16);
        generateVeinAt(chunk, random, ore, x, startY, z, size, replaceSandstone, minY, maxY);
    }

    private static void generateVeinAt(
            ChunkAccess chunk,
            RandomSource random,
            BlockState ore,
            int startX,
            int startY,
            int startZ,
            int size,
            boolean replaceSandstone,
            int minY,
            int maxY
    ) {
        ChunkPos chunkPos = chunk.getPos();
        int x = startX;
        int y = startY;
        int z = startZ;
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int index = 0; index < size; index++) {
            placeOreIfPossible(chunk, cursor.set(x, y, z), ore, replaceSandstone);

            if (random.nextFloat() < 0.45F) {
                int sideX = x + random.nextInt(3) - 1;
                int sideY = y + random.nextInt(3) - 1;
                int sideZ = z + random.nextInt(3) - 1;

                if (isInsideChunk(chunkPos, sideX, sideZ)) {
                    placeOreIfPossible(chunk, cursor.set(sideX, sideY, sideZ), ore, replaceSandstone);
                }
            }

            x = Mth.clamp(x + random.nextInt(3) - 1, chunkPos.getMinBlockX(), chunkPos.getMaxBlockX());
            y = Mth.clamp(y + random.nextInt(3) - 1, minY, maxY);
            z = Mth.clamp(z + random.nextInt(3) - 1, chunkPos.getMinBlockZ(), chunkPos.getMaxBlockZ());
        }
    }

    private static void placeOreIfPossible(
            ChunkAccess chunk,
            BlockPos pos,
            BlockState ore,
            boolean replaceSandstone
    ) {
        BlockState current = chunk.getBlockState(pos);
        boolean canReplace = current.is(Blocks.DEEPSLATE)
                || (replaceSandstone && (current.is(Blocks.SANDSTONE) || current.is(Blocks.SAND)));

        if (canReplace) {
            chunk.setBlockState(pos, ore, false);
        }
    }

    private static boolean isInsideChunk(ChunkPos chunkPos, int x, int z) {
        return x >= chunkPos.getMinBlockX() && x <= chunkPos.getMaxBlockX()
                && z >= chunkPos.getMinBlockZ() && z <= chunkPos.getMaxBlockZ();
    }

    private static int seedOffset(long seed, int index) {
        return (int) Math.floorMod(mix64(seed + index * 31L), 2048L);
    }

    private static long mix(long seed, int x, int z) {
        return mix64(seed ^ (x * 341873128712L) ^ (z * 132897987541L));
    }

    private static long mix64(long value) {
        value = (value ^ (value >>> 30)) * 0xBF58476D1CE4E5B9L;
        value = (value ^ (value >>> 27)) * 0x94D049BB133111EBL;
        return value ^ (value >>> 31);
    }

    private static double unit(long value) {
        return (value >>> 11) * 0x1.0p-53;
    }

    private static double square(double value) {
        return value * value;
    }

    private enum TerrainRegion {
        DEEP_ABYSSAL,
        ABYSSAL_VALLEY,
        DECAY_ROAD
    }
}
