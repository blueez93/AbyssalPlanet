package fr.blue.abyssalplanet.event;

import fr.blue.abyssalplanet.AbyssalPlanet;
import fr.blue.abyssalplanet.registry.ModBiomes;
import fr.blue.abyssalplanet.registry.ModDimensions;
import fr.blue.abyssalplanet.registry.ModItems;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.event.entity.player.FillBucketEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AbyssalPlanet.MOD_ID)
public final class AbyssalWaterEvents {
    private AbyssalWaterEvents() {
    }

    @SubscribeEvent
    public static void onFillBucket(FillBucketEvent event) {
        if (!event.getEmptyBucket().is(Items.BUCKET)
                || !(event.getTarget() instanceof BlockHitResult hit)
                || !event.getLevel().dimension().equals(ModDimensions.ABYSSAL_PLANET_LEVEL)) {
            return;
        }

        var pos = hit.getBlockPos();
        var fluid = event.getLevel().getFluidState(pos);
        if (!fluid.is(FluidTags.WATER)
                || !fluid.isSource()
                || !event.getLevel().getBiome(pos).is(ModBiomes.DECAY_ROAD)) {
            return;
        }

        event.setFilledBucket(new ItemStack(ModItems.ABYSSAL_WATER_BUCKET.get()));
        event.setResult(Event.Result.ALLOW);

        if (!event.getLevel().isClientSide) {
            event.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), 11);
            event.getLevel().playSound(
                    null,
                    pos,
                    SoundEvents.BUCKET_FILL,
                    SoundSource.BLOCKS,
                    1.0F,
                    1.0F
            );
            event.getLevel().gameEvent(event.getEntity(), GameEvent.FLUID_PICKUP, pos);
        }
    }
}
