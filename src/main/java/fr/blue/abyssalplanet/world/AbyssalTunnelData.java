package fr.blue.abyssalplanet.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public final class AbyssalTunnelData extends SavedData {
    private static final String DATA_NAME = "abyssalplanet_tunnel";
    private static final int CURRENT_LAYOUT_VERSION = 1;
    private static final int CURRENT_EGG_PLACEMENT_VERSION = 1;

    private boolean georgesSpawned;
    private boolean georgesDefeated;
    private boolean eggsGenerated;
    private int generatedEggCount;
    private int eggPlacementVersion;
    private int layoutVersion;

    public static AbyssalTunnelData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                AbyssalTunnelData::load,
                AbyssalTunnelData::new,
                DATA_NAME
        );
    }

    private static AbyssalTunnelData load(CompoundTag tag) {
        AbyssalTunnelData data = new AbyssalTunnelData();
        data.georgesSpawned = tag.getBoolean("GeorgesSpawned");
        data.georgesDefeated = tag.getBoolean("GeorgesDefeated");
        data.eggsGenerated = tag.getBoolean("EggsGenerated");
        data.generatedEggCount = tag.getInt("GeneratedEggCount");
        data.eggPlacementVersion = tag.getInt("EggPlacementVersion");
        data.layoutVersion = tag.getInt("LayoutVersion");
        return data;
    }

    public boolean shouldSpawnGeorges() {
        return !this.georgesSpawned && !this.georgesDefeated;
    }

    public boolean hasSpawnedGeorges() {
        return this.georgesSpawned;
    }

    public boolean isGeorgesDefeated() {
        return this.georgesDefeated;
    }

    public boolean needsSouthernTunnelMigration() {
        return this.layoutVersion < CURRENT_LAYOUT_VERSION;
    }

    public void markSouthernTunnelMigrated() {
        if (this.layoutVersion < CURRENT_LAYOUT_VERSION) {
            this.layoutVersion = CURRENT_LAYOUT_VERSION;
            setDirty();
        }
    }

    public void markGeorgesSpawned() {
        if (!this.georgesSpawned) {
            this.georgesSpawned = true;
            setDirty();
        }
    }

    public void markGeorgesDefeated() {
        this.georgesSpawned = true;
        this.georgesDefeated = true;
        setDirty();
    }

    public boolean shouldGenerateEggs() {
        return !this.eggsGenerated;
    }

    public boolean needsEggPlacementValidation() {
        return this.eggPlacementVersion < CURRENT_EGG_PLACEMENT_VERSION;
    }

    public int getGeneratedEggCount() {
        return this.generatedEggCount;
    }

    public void markEggsGenerated(int count) {
        this.eggsGenerated = true;
        this.generatedEggCount = Math.max(1, Math.min(6, count));
        this.eggPlacementVersion = CURRENT_EGG_PLACEMENT_VERSION;
        setDirty();
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putBoolean("GeorgesSpawned", this.georgesSpawned);
        tag.putBoolean("GeorgesDefeated", this.georgesDefeated);
        tag.putBoolean("EggsGenerated", this.eggsGenerated);
        tag.putInt("GeneratedEggCount", this.generatedEggCount);
        tag.putInt("EggPlacementVersion", this.eggPlacementVersion);
        tag.putInt("LayoutVersion", this.layoutVersion);
        return tag;
    }
}
