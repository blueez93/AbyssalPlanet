package fr.blue.abyssalplanet.world;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;

public final class BlueGoldTreeSpawnData extends SavedData {
    private static final String DATA_NAME = "abyssalplanet_blue_gold_trees";
    private final Set<Long> processedDunes = new HashSet<>();

    public static BlueGoldTreeSpawnData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                BlueGoldTreeSpawnData::load,
                BlueGoldTreeSpawnData::new,
                DATA_NAME
        );
    }

    private static BlueGoldTreeSpawnData load(CompoundTag tag) {
        BlueGoldTreeSpawnData data = new BlueGoldTreeSpawnData();
        for (long duneKey : tag.getLongArray("ProcessedDunes")) {
            data.processedDunes.add(duneKey);
        }
        return data;
    }

    public boolean markDuneProcessed(int cellX, int cellZ) {
        boolean added = this.processedDunes.add(ChunkPos.asLong(cellX, cellZ));
        if (added) {
            setDirty();
        }
        return added;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag.putLongArray("ProcessedDunes", this.processedDunes.stream().mapToLong(Long::longValue).toArray());
        return tag;
    }
}
