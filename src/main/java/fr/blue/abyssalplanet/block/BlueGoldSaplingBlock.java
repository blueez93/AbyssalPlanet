package fr.blue.abyssalplanet.block;

import fr.blue.abyssalplanet.world.BlueGoldTreeGenerator;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

public class BlueGoldSaplingBlock extends BushBlock implements BonemealableBlock {
    public static final IntegerProperty STAGE = BlockStateProperties.STAGE;
    private static final VoxelShape SHAPE = Block.box(2.0D, 0.0D, 2.0D, 14.0D, 13.0D, 14.0D);

    public BlueGoldSaplingBlock(Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(STAGE, 0));
    }

    @Override
    protected boolean mayPlaceOn(BlockState state, BlockGetter level, BlockPos position) {
        return state.is(Blocks.SAND) || state.is(Blocks.RED_SAND);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos position, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos position, RandomSource random) {
        if (level.getMaxLocalRawBrightness(position.above()) >= 9 && random.nextInt(7) == 0) {
            advanceTree(level, position, state, random);
        }
    }

    private void advanceTree(ServerLevel level, BlockPos position, BlockState state, RandomSource random) {
        if (state.getValue(STAGE) == 0) {
            level.setBlock(position, state.cycle(STAGE), 4);
            return;
        }
        BlueGoldTreeGenerator.generate(level, position, random);
    }

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos position, BlockState state, boolean clientSide) {
        return true;
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos position, BlockState state) {
        return level.random.nextFloat() < 0.45F;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos position, BlockState state) {
        advanceTree(level, position, state, random);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(STAGE);
    }
}
