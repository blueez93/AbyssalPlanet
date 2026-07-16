package fr.blue.abyssalplanet.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.saveddata.SavedData;

public final class KrakenSpawnData extends SavedData {
    private static final String DATA_NAME = "abyssalplanet_kraken_spawn";
    private static final long NO_SPAWN_SCHEDULED = -1L;
    private static final int INITIAL_MIN_DELAY_TICKS = 20 * 60;
    private static final int INITIAL_MAX_DELAY_TICKS = 20 * 90;
    private static final int RESPAWN_MIN_DELAY_TICKS = 20 * 60 * 5;
    private static final int RESPAWN_MAX_DELAY_TICKS = 20 * 60 * 8;

    private long nextSpawnGameTime = NO_SPAWN_SCHEDULED;
    private boolean bossAlive;

    public static KrakenSpawnData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                KrakenSpawnData::load,
                KrakenSpawnData::new,
                DATA_NAME
        );
    }

    private static KrakenSpawnData load(CompoundTag tag) {
        KrakenSpawnData data = new KrakenSpawnData();
        data.nextSpawnGameTime = tag.contains("NextSpawnGameTime")
                ? tag.getLong("NextSpawnGameTime")
                : NO_SPAWN_SCHEDULED;
        data.bossAlive = tag.getBoolean("BossAlive");
        return data;
    }

    public void scheduleInitialSpawn(long currentGameTime, RandomSource random) {
        if (this.bossAlive || hasScheduledSpawn()) {
            return;
        }
        this.nextSpawnGameTime = currentGameTime + randomDelay(
                random,
                INITIAL_MIN_DELAY_TICKS,
                INITIAL_MAX_DELAY_TICKS
        );
        setDirty();
    }

    public void scheduleRespawn(long currentGameTime, RandomSource random) {
        this.bossAlive = false;
        this.nextSpawnGameTime = currentGameTime + randomDelay(
                random,
                RESPAWN_MIN_DELAY_TICKS,
                RESPAWN_MAX_DELAY_TICKS
        );
        setDirty();
    }

    public void delayFailedAttempt(long currentGameTime) {
        this.nextSpawnGameTime = currentGameTime + 20 * 5L;
        setDirty();
    }

    public void markBossAlive() {
        this.bossAlive = true;
        this.nextSpawnGameTime = NO_SPAWN_SCHEDULED;
        setDirty();
    }

    public boolean isBossAlive() {
        return this.bossAlive;
    }

    public boolean hasScheduledSpawn() {
        return this.nextSpawnGameTime >= 0L;
    }

    public boolean isSpawnDue(long currentGameTime) {
        return hasScheduledSpawn() && currentGameTime >= this.nextSpawnGameTime;
    }

    private static int randomDelay(RandomSource random, int minimum, int maximum) {
        return minimum + random.nextInt(maximum - minimum + 1);
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putLong("NextSpawnGameTime", this.nextSpawnGameTime);
        tag.putBoolean("BossAlive", this.bossAlive);
        return tag;
    }
}
