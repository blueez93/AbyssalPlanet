package fr.blue.abyssalplanet.world;

import fr.blue.abyssalplanet.entity.AbstractAbyssalCompanionEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public final class AbyssalCompanionData extends SavedData {
    private static final String DATA_NAME = "abyssalplanet_companions";
    private final Map<UUID, CompanionRecord> companions = new HashMap<>();
    private final Set<UUID> deadSeniors = new HashSet<>();
    private final Set<UUID> whistleRecipients = new HashSet<>();

    public static AbyssalCompanionData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                AbyssalCompanionData::load,
                AbyssalCompanionData::new,
                DATA_NAME
        );
    }

    private static AbyssalCompanionData load(CompoundTag tag) {
        AbyssalCompanionData data = new AbyssalCompanionData();
        for (Tag raw : tag.getList("Companions", Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) raw;
            ResourceLocation type = ResourceLocation.tryParse(entry.getString("Type"));
            ResourceLocation dimension = ResourceLocation.tryParse(entry.getString("Dimension"));
            if (!entry.hasUUID("Pet") || !entry.hasUUID("Owner") || type == null || dimension == null) {
                continue;
            }
            CompanionRecord record = new CompanionRecord(
                    entry.getUUID("Pet"),
                    entry.getUUID("Owner"),
                    type,
                    dimension,
                    BlockPos.of(entry.getLong("Position")),
                    entry.getBoolean("Sitting")
            );
            data.companions.put(record.petUuid(), record);
        }
        readUuidList(tag, "DeadSeniors", data.deadSeniors);
        readUuidList(tag, "WhistleRecipients", data.whistleRecipients);
        return data;
    }

    private static void readUuidList(CompoundTag tag, String key, Set<UUID> output) {
        for (Tag raw : tag.getList(key, Tag.TAG_COMPOUND)) {
            CompoundTag entry = (CompoundTag) raw;
            if (entry.hasUUID("Id")) {
                output.add(entry.getUUID("Id"));
            }
        }
    }

    public void track(AbstractAbyssalCompanionEntity companion) {
        UUID owner = companion.getOwnerUUID();
        if (!companion.isTame() || owner == null) {
            return;
        }
        ResourceLocation type = BuiltInRegistries.ENTITY_TYPE.getKey(companion.getType());
        CompanionRecord updated = new CompanionRecord(
                companion.getUUID(),
                owner,
                type,
                companion.level().dimension().location(),
                companion.blockPosition(),
                companion.isOrderedToSit()
        );
        if (!updated.equals(companions.put(companion.getUUID(), updated))) {
            setDirty();
        }
    }

    public List<CompanionRecord> getOwnedBy(UUID owner) {
        List<CompanionRecord> result = new ArrayList<>();
        for (CompanionRecord record : companions.values()) {
            if (record.ownerUuid().equals(owner)) {
                result.add(record);
            }
        }
        return result;
    }

    public void remove(UUID petUuid) {
        if (companions.remove(petUuid) != null) {
            setDirty();
        }
    }

    public void markSeniorDead(UUID seniorUuid) {
        companions.remove(seniorUuid);
        if (deadSeniors.add(seniorUuid)) {
            setDirty();
        }
    }

    public void markSeniorAlive(UUID seniorUuid) {
        if (deadSeniors.remove(seniorUuid)) {
            setDirty();
        }
    }

    public boolean isSeniorDead(UUID seniorUuid) {
        return deadSeniors.contains(seniorUuid);
    }

    public boolean grantWhistleOnce(UUID ownerUuid) {
        if (!whistleRecipients.add(ownerUuid)) {
            return false;
        }
        setDirty();
        return true;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag records = new ListTag();
        for (CompanionRecord record : companions.values()) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Pet", record.petUuid());
            entry.putUUID("Owner", record.ownerUuid());
            entry.putString("Type", record.entityType().toString());
            entry.putString("Dimension", record.dimension().toString());
            entry.putLong("Position", record.position().asLong());
            entry.putBoolean("Sitting", record.sitting());
            records.add(entry);
        }
        tag.put("Companions", records);
        writeUuidList(tag, "DeadSeniors", deadSeniors);
        writeUuidList(tag, "WhistleRecipients", whistleRecipients);
        return tag;
    }

    private static void writeUuidList(CompoundTag tag, String key, Set<UUID> values) {
        ListTag list = new ListTag();
        for (UUID value : values) {
            CompoundTag entry = new CompoundTag();
            entry.putUUID("Id", value);
            list.add(entry);
        }
        tag.put(key, list);
    }

    public record CompanionRecord(
            UUID petUuid,
            UUID ownerUuid,
            ResourceLocation entityType,
            ResourceLocation dimension,
            BlockPos position,
            boolean sitting
    ) {
    }
}
