package fr.blue.abyssalplanet.event;

import fr.blue.abyssalplanet.AbyssalPlanet;
import fr.blue.abyssalplanet.registry.ModDimensions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AbyssalPlanet.MOD_ID)
public final class AbyssalItemPhysicsEvents {
    private static final double SINK_ACCELERATION = 0.045D;
    private static final double MAXIMUM_SINK_SPEED = -0.16D;

    private AbyssalItemPhysicsEvents() {
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END
                || !(event.level instanceof ServerLevel level)
                || !level.dimension().equals(ModDimensions.ABYSSAL_PLANET_LEVEL)) {
            return;
        }

        for (Entity entity : level.getAllEntities()) {
            if (!(entity instanceof ItemEntity item)
                    || !item.isInWaterOrBubble()
                    || item.isNoGravity()) {
                continue;
            }

            Vec3 movement = item.getDeltaMovement();
            double vertical = Math.max(
                    MAXIMUM_SINK_SPEED,
                    Math.min(0.0D, movement.y) - SINK_ACCELERATION
            );
            item.setDeltaMovement(movement.x * 0.88D, vertical, movement.z * 0.88D);
            item.hasImpulse = true;
        }
    }
}
