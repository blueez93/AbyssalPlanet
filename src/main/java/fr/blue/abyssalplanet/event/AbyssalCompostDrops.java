package fr.blue.abyssalplanet.event;

import fr.blue.abyssalplanet.AbyssalPlanet;
import fr.blue.abyssalplanet.registry.ModDimensions;
import fr.blue.abyssalplanet.registry.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AbyssalPlanet.MOD_ID)
public final class AbyssalCompostDrops {
    private AbyssalCompostDrops() {
    }

    @SubscribeEvent
    public static void onLivingDrops(LivingDropsEvent event) {
        if (!(event.getEntity() instanceof Mob mob)
                || !(mob.level() instanceof ServerLevel level)
                || !level.dimension().equals(ModDimensions.ABYSSAL_PLANET_LEVEL)) {
            return;
        }

        int amount = 1 + level.random.nextInt(5);
        event.getDrops().add(new ItemEntity(
                level,
                mob.getX(),
                mob.getY(),
                mob.getZ(),
                new ItemStack(ModItems.ABYSSAL_COMPOST.get(), amount)
        ));
    }
}
