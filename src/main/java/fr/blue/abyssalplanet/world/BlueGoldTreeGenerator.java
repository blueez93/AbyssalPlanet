package fr.blue.abyssalplanet.world;

import fr.blue.abyssalplanet.registry.ModBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkAccess;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;

public final class BlueGoldTreeGenerator {
    private BlueGoldTreeGenerator() {
    }

    public static boolean generate(ServerLevel level, BlockPos base, RandomSource random) {
        return generate(
                base,
                random,
                level::getBlockState,
                (position, state) -> level.setBlock(position, state, 3)
        );
    }

    public static boolean generate(ChunkAccess chunk, BlockPos base, RandomSource random) {
        return generate(
                base,
                random,
                chunk::getBlockState,
                (position, state) -> chunk.setBlockState(position, state, false)
        );
    }

    private static boolean generate(
            BlockPos base,
            RandomSource random,
            Function<BlockPos, BlockState> stateGetter,
            BiConsumer<BlockPos, BlockState> stateSetter
    ) {
        if (!stateGetter.apply(base.below()).is(Blocks.SAND)) {
            return false;
        }

        int trunkHeight = 6 + random.nextInt(4);
        for (int height = 0; height <= trunkHeight + 1; height++) {
            if (!canReplace(stateGetter.apply(base.above(height)))) {
                return false;
            }
        }

        BlockState verticalLog = ModBlocks.BLUE_GOLD_LOG.get().defaultBlockState()
                .setValue(RotatedPillarBlock.AXIS, Direction.Axis.Y);
        Set<BlockPos> logPositions = new HashSet<>();
        for (int height = 0; height < trunkHeight; height++) {
            BlockPos logPosition = base.above(height);
            stateSetter.accept(logPosition, verticalLog);
            logPositions.add(logPosition.immutable());
        }

        int branchCount = 1 + random.nextInt(3);
        List<BlockPos> branchTips = new ArrayList<>();
        Set<Direction> usedDirections = new HashSet<>();
        for (int branch = 0; branch < branchCount; branch++) {
            Direction direction = chooseUnusedDirection(random, usedDirections);
            usedDirections.add(direction);
            int branchY = trunkHeight - 2 - random.nextInt(2);
            int branchLength = 1 + random.nextInt(2);
            BlockState branchLog = ModBlocks.BLUE_GOLD_LOG.get().defaultBlockState()
                    .setValue(RotatedPillarBlock.AXIS, direction.getAxis());
            BlockPos tip = base.above(branchY);

            for (int distance = 1; distance <= branchLength; distance++) {
                BlockPos branchPosition = base.above(branchY).relative(direction, distance);
                if (!canReplace(stateGetter.apply(branchPosition))) {
                    break;
                }
                stateSetter.accept(branchPosition, branchLog);
                logPositions.add(branchPosition.immutable());
                tip = branchPosition;
            }
            branchTips.add(tip.immutable());
        }

        int leafTarget = 2 + random.nextInt(5);
        Set<BlockPos> leafPositions = new HashSet<>();
        addLeafIfPossible(base.above(trunkHeight), stateGetter, logPositions, leafPositions);
        for (BlockPos tip : branchTips) {
            addLeafIfPossible(tip.above(), stateGetter, logPositions, leafPositions);
        }

        BlockPos crownCenter = base.above(trunkHeight - 1);
        for (int attempt = 0; leafPositions.size() < leafTarget && attempt < 40; attempt++) {
            int offsetX = random.nextInt(5) - 2;
            int offsetY = random.nextInt(3) - 1;
            int offsetZ = random.nextInt(5) - 2;
            if (Math.abs(offsetX) + Math.abs(offsetZ) == 0
                    || Math.abs(offsetX) + Math.abs(offsetZ) > 3) {
                continue;
            }
            addLeafIfPossible(
                    crownCenter.offset(offsetX, offsetY, offsetZ),
                    stateGetter,
                    logPositions,
                    leafPositions
            );
        }

        BlockState leaves = ModBlocks.BLUE_GOLD_LEAVES.get().defaultBlockState()
                .setValue(LeavesBlock.DISTANCE, 2)
                .setValue(LeavesBlock.PERSISTENT, false);
        int placedLeaves = 0;
        for (BlockPos leafPosition : leafPositions) {
            if (placedLeaves >= leafTarget) {
                break;
            }
            stateSetter.accept(leafPosition, leaves);
            placedLeaves++;
        }
        return true;
    }

    private static Direction chooseUnusedDirection(RandomSource random, Set<Direction> usedDirections) {
        Direction direction;
        int attempts = 0;
        do {
            direction = Direction.from2DDataValue(random.nextInt(4));
            attempts++;
        } while (usedDirections.contains(direction) && attempts < 8);
        return direction;
    }

    private static void addLeafIfPossible(
            BlockPos position,
            Function<BlockPos, BlockState> stateGetter,
            Set<BlockPos> logPositions,
            Set<BlockPos> leafPositions
    ) {
        if (!logPositions.contains(position) && canReplace(stateGetter.apply(position))) {
            leafPositions.add(position.immutable());
        }
    }

    private static boolean canReplace(BlockState state) {
        return state.isAir()
                || state.canBeReplaced()
                || state.is(ModBlocks.BLUE_GOLD_SAPLING.get());
    }
}
