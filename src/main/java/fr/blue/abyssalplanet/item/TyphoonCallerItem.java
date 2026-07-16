package fr.blue.abyssalplanet.item;

import fr.blue.abyssalplanet.entity.AbyssalTyphoonEntity;
import fr.blue.abyssalplanet.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class TyphoonCallerItem extends Item {
    public TyphoonCallerItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();

        if (!level.isClientSide) {
            BlockPos clickedPos = context.getClickedPos();
            BlockPos surfacePos = findWaterSurface(level, clickedPos);

            if (surfacePos == null) {
                return InteractionResult.FAIL;
            }

            if (!hasEnoughWaterSpace(level, surfacePos, 5, 6)) {
    return InteractionResult.FAIL;
            }

            AbyssalTyphoonEntity typhoon = new AbyssalTyphoonEntity(
                    ModEntities.ABYSSAL_TYPHOON.get(),
                    level
            );

            typhoon.moveTo(
                    surfacePos.getX() + 0.5D,
                    surfacePos.getY(),
                    surfacePos.getZ() + 0.5D,
                    0.0F,
                    0.0F
            );

            level.addFreshEntity(typhoon);

            if (context.getPlayer() != null && !context.getPlayer().getAbilities().instabuild) {
                context.getItemInHand().shrink(1);
            }
        }

        return InteractionResult.SUCCESS;
    }

    private BlockPos findWaterSurface(Level level, BlockPos startPos) {
        BlockPos.MutableBlockPos mutablePos = startPos.mutable();

        // On remonte jusqu'à trouver le haut de l'eau.
        for (int i = 0; i < 40; i++) {
            BlockPos current = mutablePos.immutable();
            BlockPos above = current.above();

            boolean currentIsWater = level.getFluidState(current).is(FluidTags.WATER);
            boolean aboveIsAir = level.isEmptyBlock(above);

            if (currentIsWater && aboveIsAir) {
                return above;
            }

            mutablePos.move(0, 1, 0);
        }

        // Si le joueur a cliqué au-dessus, on cherche aussi vers le bas.
        mutablePos.set(startPos);

        for (int i = 0; i < 40; i++) {
            BlockPos current = mutablePos.immutable();
            BlockPos below = current.below();

            boolean belowIsWater = level.getFluidState(below).is(FluidTags.WATER);
            boolean currentIsAir = level.isEmptyBlock(current);

            if (belowIsWater && currentIsAir) {
                return current;
            }

            mutablePos.move(0, -1, 0);
        }

        return null;
    }

    private boolean hasEnoughWaterSpace(Level level, BlockPos surfacePos, int radius, int depth) {
    int validColumns = 0;
    int checkedColumns = 0;

    for (int x = -radius; x <= radius; x += 3) {
        for (int z = -radius; z <= radius; z += 3) {
            double distance = Math.sqrt(x * x + z * z);

            if (distance > radius) {
                continue;
            }

            checkedColumns++;

            int waterDepth = 0;

            for (int y = 1; y <= depth; y++) {
                BlockPos waterCheck = surfacePos.offset(x, -y, z);

                if (level.getFluidState(waterCheck).is(FluidTags.WATER)) {
                    waterDepth++;
                }
            }

            if (waterDepth >= Math.max(3, depth / 2)) {
                validColumns++;
            }
        }
    }

    return checkedColumns > 0 && validColumns >= checkedColumns * 0.55D;
   }
}
