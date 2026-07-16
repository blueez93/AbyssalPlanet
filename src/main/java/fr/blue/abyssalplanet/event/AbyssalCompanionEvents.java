package fr.blue.abyssalplanet.event;

import fr.blue.abyssalplanet.AbyssalPlanet;
import fr.blue.abyssalplanet.entity.AbstractAbyssalCompanionEntity;
import fr.blue.abyssalplanet.world.AbyssalCompanionData;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityTeleportEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = AbyssalPlanet.MOD_ID)
public final class AbyssalCompanionEvents {
    private static final Map<UUID, Integer> PENDING_CATCHUPS = new HashMap<>();

    private AbyssalCompanionEvents() {
    }

    @SubscribeEvent
    public static void onDimensionChanged(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            schedule(player);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            schedule(player);
        }
    }

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            schedule(player);
        }
    }

    @SubscribeEvent
    public static void onTeleport(EntityTeleportEvent event) {
        if (!event.isCanceled() && event.getEntity() instanceof ServerPlayer player) {
            schedule(player);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }
        MinecraftServer server = event.getServer();
        if (server.getTickCount() % 20 == 0) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                catchUp(player);
            }
        }
        PENDING_CATCHUPS.replaceAll((uuid, ticks) -> ticks - 1);
        PENDING_CATCHUPS.entrySet().removeIf(entry -> {
            if (entry.getValue() > 0) {
                return false;
            }
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            if (player != null) {
                catchUp(player);
            }
            return true;
        });
    }

    private static void schedule(ServerPlayer player) {
        catchUp(player);
        PENDING_CATCHUPS.put(player.getUUID(), 5);
    }

    private static void catchUp(ServerPlayer owner) {
        MinecraftServer server = owner.getServer();
        if (server == null || !owner.isAlive()) {
            return;
        }

        Map<UUID, AbstractAbyssalCompanionEntity> loaded = new HashMap<>();
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof AbstractAbyssalCompanionEntity companion
                        && companion.isTame()
                        && companion.isOwnedByUuid(owner.getUUID())
                        && !companion.isOrderedToSit()) {
                    loaded.put(companion.getUUID(), companion);
                }
            }
        }

        for (AbyssalCompanionData.CompanionRecord record :
                AbyssalCompanionData.get(server).getOwnedBy(owner.getUUID())) {
            if (record.sitting() || loaded.containsKey(record.petUuid())) {
                continue;
            }
            ResourceKey<Level> levelKey = ResourceKey.create(Registries.DIMENSION, record.dimension());
            ServerLevel source = server.getLevel(levelKey);
            if (source == null) {
                continue;
            }
            source.getChunk(record.position().getX() >> 4, record.position().getZ() >> 4);
            Entity entity = source.getEntity(record.petUuid());
            if (entity instanceof AbstractAbyssalCompanionEntity companion
                    && companion.isOwnedByUuid(owner.getUUID())) {
                loaded.put(companion.getUUID(), companion);
            }
        }

        for (AbstractAbyssalCompanionEntity companion : loaded.values()) {
            if (!companion.isRemoved()) {
                companion.followOwnerAfterTeleport(owner);
            }
        }
    }
}
