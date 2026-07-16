package fr.blue.abyssalplanet.world;

import fr.blue.abyssalplanet.event.AbyssalDimensionEvents;
import fr.blue.abyssalplanet.event.BabyKrakenPetEvents;
import fr.blue.abyssalplanet.item.GoOutItem;
import fr.blue.abyssalplanet.registry.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public class AbyssalTeleporter {
    public static void teleportToAbyss(ServerPlayer player, Vec3 sourceTyphoonPosition) {
        ServerLevel abyssLevel = player.server.getLevel(ModDimensions.ABYSSAL_PLANET_LEVEL);

        if (abyssLevel == null) {
            return;
        }

        BlockPos destination = new BlockPos(0, 90, 0);
        ItemStack goOut = GoOutItem.createBoundToTyphoon(player.level().dimension(), sourceTyphoonPosition);

        prepareArrivalZone(abyssLevel, destination);

        player.addEffect(new MobEffectInstance(
        MobEffects.BLINDNESS,
        20 * 1,
        0,
        false,
        true,
        true
));

player.addEffect(new MobEffectInstance(
        MobEffects.DARKNESS,
        20 * 12,
        0,
        false,
        true,
        true
));

        player.teleportTo(
                abyssLevel,
                destination.getX() + 0.5D,
                destination.getY(),
                destination.getZ() + 0.5D,
                0.0F,
                0.0F
        );
        AbyssalDimensionEvents.handleAbyssalArrival(player);
        BabyKrakenPetEvents.schedulePetCatchUp(player);

        if (!player.getInventory().add(goOut)) {
            player.drop(goOut, false);
        }
    }

    private static void prepareArrivalZone(ServerLevel level, BlockPos center) {
    BlockPos floorCenter = center.below(2);

    for (int x = -3; x <= 3; x++) {
        for (int z = -3; z <= 3; z++) {
            BlockPos floorPos = floorCenter.offset(x, 0, z);

            if (Math.abs(x) == 3 || Math.abs(z) == 3) {
                level.setBlock(floorPos, Blocks.DEEPSLATE_BRICKS.defaultBlockState(), 3);
            } else {
                level.setBlock(floorPos, Blocks.DEEPSLATE.defaultBlockState(), 3);
            }
        }
    }
   }
}
