package fr.blue.abyssalplanet.event;

import fr.blue.abyssalplanet.AbyssalPlanet;
import fr.blue.abyssalplanet.entity.BabyKrakenEntity;
import fr.blue.abyssalplanet.world.BabyKrakenPetData;
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
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = AbyssalPlanet.MOD_ID)
public final class BabyKrakenPetEvents {
    private static final int CATCH_UP_RETRY_INTERVAL_TICKS = 5;
    private static final int CATCH_UP_RETRY_COUNT = 12;
    private static final int PERIODIC_CATCH_UP_INTERVAL_TICKS = 20;
    private static final int TRACKED_PET_CHUNK_SEARCH_RADIUS = 1;
    private static final Map<UUID, CatchUpRequest> PENDING_OWNER_CATCHUPS = new HashMap<>();

    private BabyKrakenPetEvents() {
    }

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            schedulePetCatchUp(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            schedulePetCatchUp(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            schedulePetCatchUp(player);
        }
    }

    @SubscribeEvent
    public static void onPlayerTeleported(EntityTeleportEvent event) {
        if (!event.isCanceled() && event.getEntity() instanceof ServerPlayer player) {
            schedulePetCatchUp(player);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        MinecraftServer server = event.getServer();
        if (!PENDING_OWNER_CATCHUPS.isEmpty()) {
            Iterator<Map.Entry<UUID, CatchUpRequest>> iterator = PENDING_OWNER_CATCHUPS.entrySet().iterator();
            while (iterator.hasNext()) {
                Map.Entry<UUID, CatchUpRequest> entry = iterator.next();
                CatchUpRequest request = entry.getValue();
                if (--request.ticksUntilAttempt > 0) {
                    continue;
                }

                ServerPlayer owner = server.getPlayerList().getPlayer(entry.getKey());
                if (owner != null && owner.isAlive() && !owner.hasDisconnected()) {
                    catchUpPetsNow(owner);
                }

                if (--request.attemptsRemaining <= 0 || owner == null) {
                    iterator.remove();
                } else {
                    request.ticksUntilAttempt = CATCH_UP_RETRY_INTERVAL_TICKS;
                }
            }
        }

        if (server.getTickCount() % PERIODIC_CATCH_UP_INTERVAL_TICKS == 0) {
            BabyKrakenPetData petData = BabyKrakenPetData.get(server);
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (petData.hasFollowingPetOwnedBy(player.getUUID())) {
                    catchUpPetsNow(player);
                }
            }
        }
    }

    public static void schedulePetCatchUp(ServerPlayer player) {
        catchUpPetsNow(player);
        PENDING_OWNER_CATCHUPS.put(
                player.getUUID(),
                new CatchUpRequest(CATCH_UP_RETRY_INTERVAL_TICKS, CATCH_UP_RETRY_COUNT)
        );
    }

    public static void catchUpPetsNow(ServerPlayer owner) {
        MinecraftServer server = owner.getServer();
        if (server == null) {
            return;
        }
        if (!owner.isAlive() || owner.hasDisconnected()) {
            return;
        }

        Map<UUID, BabyKrakenEntity> ownedPets = new HashMap<>();
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof BabyKrakenEntity babyKraken
                        && babyKraken.isTame()
                        && babyKraken.isOwnedByUuid(owner.getUUID())
                        && !babyKraken.isOrderedToSit()) {
                    ownedPets.put(babyKraken.getUUID(), babyKraken);
                }
            }
        }

        for (BabyKrakenPetData.PetRecord record : BabyKrakenPetData.get(server).getPetsOwnedBy(owner.getUUID())) {
            if (record.sitting() || ownedPets.containsKey(record.petUuid())) {
                continue;
            }

            BabyKrakenEntity trackedPet = loadTrackedPet(server, owner, record);
            if (trackedPet != null && !trackedPet.isOrderedToSit()) {
                ownedPets.put(trackedPet.getUUID(), trackedPet);
            }
        }

        for (BabyKrakenEntity babyKraken : ownedPets.values()) {
            if (!babyKraken.isRemoved()) {
                babyKraken.followOwnerAfterTeleport(owner);
            }
        }
    }

    private static BabyKrakenEntity loadTrackedPet(
            MinecraftServer server,
            ServerPlayer owner,
            BabyKrakenPetData.PetRecord record
    ) {
        ResourceKey<Level> sourceKey = ResourceKey.create(Registries.DIMENSION, record.dimension());
        ServerLevel sourceLevel = server.getLevel(sourceKey);
        if (sourceLevel == null) {
            return null;
        }

        int centerChunkX = record.position().getX() >> 4;
        int centerChunkZ = record.position().getZ() >> 4;
        for (int xOffset = -TRACKED_PET_CHUNK_SEARCH_RADIUS; xOffset <= TRACKED_PET_CHUNK_SEARCH_RADIUS; xOffset++) {
            for (int zOffset = -TRACKED_PET_CHUNK_SEARCH_RADIUS; zOffset <= TRACKED_PET_CHUNK_SEARCH_RADIUS; zOffset++) {
                sourceLevel.getChunk(centerChunkX + xOffset, centerChunkZ + zOffset);
            }
        }

        Entity entity = sourceLevel.getEntity(record.petUuid());
        if (entity instanceof BabyKrakenEntity babyKraken
                && babyKraken.isTame()
                && babyKraken.isOwnedByUuid(owner.getUUID())) {
            return babyKraken;
        }
        return null;
    }

    private static final class CatchUpRequest {
        private int ticksUntilAttempt;
        private int attemptsRemaining;

        private CatchUpRequest(int ticksUntilAttempt, int attemptsRemaining) {
            this.ticksUntilAttempt = ticksUntilAttempt;
            this.attemptsRemaining = attemptsRemaining;
        }
    }
}
