package fr.blue.abyssalplanet.block;

import fr.blue.abyssalplanet.entity.AbyssalViperEntity;
import fr.blue.abyssalplanet.registry.ModBiomes;
import fr.blue.abyssalplanet.registry.ModDimensions;
import fr.blue.abyssalplanet.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.item.context.BlockPlaceContext;
import org.jetbrains.annotations.Nullable;

public class AbyssalBaitBlock extends Block implements SimpleWaterloggedBlock {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final BooleanProperty SUMMONED = BooleanProperty.create("summoned");

    private static final VoxelShape SHAPE = Block.box(1.0D, 0.0D, 1.0D, 15.0D, 1.5D, 15.0D);

    private final boolean summonsSerpent;

    public AbyssalBaitBlock(Properties properties, boolean summonsSerpent) {
        super(properties);
        this.summonsSerpent = summonsSerpent;
        this.registerDefaultState(this.stateDefinition.any()
                .setValue(WATERLOGGED, false)
                .setValue(SUMMONED, false));
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        FluidState fluidState = context.getLevel().getFluidState(context.getClickedPos());
        return this.defaultBlockState()
                .setValue(WATERLOGGED, fluidState.is(FluidTags.WATER))
                .setValue(SUMMONED, false);
    }

    @Override
    public void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean isMoving) {
        super.onPlace(state, level, pos, oldState, isMoving);
        if (!level.isClientSide && !oldState.is(state.getBlock())) {
            scheduleSummon(level, pos);
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(SUMMONED)) {
            return;
        }

        if (!canSummonHere(level, pos)) {
            scheduleSummon(level, pos);
            return;
        }

        BlockPos spawnPos = findViperSpawnPos(level, pos, random);
        if (spawnPos == null) {
            scheduleSummon(level, pos);
            return;
        }

        AbyssalViperEntity viper = ModEntities.ABYSSAL_VIPER.get().create(level);
        if (viper == null) {
            return;
        }

        viper.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY() + 0.2D, spawnPos.getZ() + 0.5D, random.nextFloat() * 360.0F, 0.0F);
        viper.finalizeSpawn(level, level.getCurrentDifficultyAt(spawnPos), MobSpawnType.TRIGGERED, null, null);
        viper.startFromBait(pos, this.summonsSerpent);
        viper.setPersistenceRequired();
        level.addFreshEntity(viper);

        level.setBlock(pos, state.setValue(SUMMONED, true), Block.UPDATE_ALL);
        level.sendParticles(
                ParticleTypes.SQUID_INK,
                pos.getX() + 0.5D,
                pos.getY() + 0.25D,
                pos.getZ() + 0.5D,
                this.summonsSerpent ? 32 : 18,
                0.35D,
                0.12D,
                0.35D,
                0.03D
        );
    }

    private void scheduleSummon(LevelAccessor level, BlockPos pos) {
        RandomSource random = level.getRandom();
        int minimum = this.summonsSerpent ? 20 * 10 : 20 * 5;
        int maximumExtra = this.summonsSerpent ? 20 * 5 : 20 * 10;
        level.scheduleTick(pos, this, minimum + random.nextInt(maximumExtra + 1));
    }

    private boolean canSummonHere(ServerLevel level, BlockPos pos) {
        return level.dimension().equals(ModDimensions.ABYSSAL_PLANET_LEVEL)
                && level.getBiome(pos).is(ModBiomes.ABYSSAL_VALLEY)
                && level.getFluidState(pos).is(FluidTags.WATER)
                && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Nullable
    private BlockPos findViperSpawnPos(ServerLevel level, BlockPos baitPos, RandomSource random) {
        for (int attempt = 0; attempt < 28; attempt++) {
            double angle = random.nextDouble() * Math.PI * 2.0D;
            int distance = 10 + random.nextInt(this.summonsSerpent ? 15 : 10);
            int x = baitPos.getX() + (int) Math.round(Math.cos(angle) * distance);
            int z = baitPos.getZ() + (int) Math.round(Math.sin(angle) * distance);
            int y = baitPos.getY() - 4 + random.nextInt(9);
            BlockPos candidate = new BlockPos(x, y, z);

            if (level.getFluidState(candidate).is(FluidTags.WATER)
                    && level.getFluidState(candidate.above()).is(FluidTags.WATER)
                    && level.getBlockState(candidate).getCollisionShape(level, candidate).isEmpty()) {
                return candidate;
            }
        }

        return baitPos.above(2);
    }

    @Override
    public BlockState updateShape(
            BlockState state,
            Direction direction,
            BlockState neighborState,
            LevelAccessor level,
            BlockPos pos,
            BlockPos neighborPos
    ) {
        if (state.getValue(WATERLOGGED)) {
            level.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        }

        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    @Override
    public FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED, SUMMONED);
    }
}
