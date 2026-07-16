package fr.blue.abyssalplanet.world;

import fr.blue.abyssalplanet.entity.BabyKrakenEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class BabyKrakenPetData extends SavedData {
    private static final String DATA_NAME = "abyssalplanet_baby_kraken_pets";
    private final Map<UUID, PetRecord> pets = new HashMap<>();

    public static BabyKrakenPetData get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                BabyKrakenPetData::load,
                BabyKrakenPetData::new,
                DATA_NAME
        );
    }

    private static BabyKrakenPetData load(CompoundTag tag) {
        BabyKrakenPetData data = new BabyKrakenPetData();
        ListTag petTags = tag.getList("Pets", Tag.TAG_COMPOUND);
        for (Tag entry : petTags) {
            CompoundTag petTag = (CompoundTag) entry;
            if (!petTag.hasUUID("Pet") || !petTag.hasUUID("Owner")) {
                continue;
            }

            ResourceLocation dimension = ResourceLocation.tryParse(petTag.getString("Dimension"));
            if (dimension == null) {
                continue;
            }

            PetRecord record = new PetRecord(
                    petTag.getUUID("Pet"),
                    petTag.getUUID("Owner"),
                    dimension,
                    BlockPos.of(petTag.getLong("Position")),
                    petTag.getBoolean("Sitting")
            );
            data.pets.put(record.petUuid(), record);
        }
        return data;
    }

    public void track(BabyKrakenEntity pet) {
        UUID ownerUuid = pet.getOwnerUUID();
        if (!pet.isTame() || ownerUuid == null) {
            return;
        }

        PetRecord updated = new PetRecord(
                pet.getUUID(),
                ownerUuid,
                pet.level().dimension().location(),
                pet.blockPosition(),
                pet.isOrderedToSit()
        );
        PetRecord previous = this.pets.put(pet.getUUID(), updated);
        if (!updated.equals(previous)) {
            setDirty();
        }
    }

    public List<PetRecord> getPetsOwnedBy(UUID ownerUuid) {
        List<PetRecord> ownedPets = new ArrayList<>();
        for (PetRecord record : this.pets.values()) {
            if (record.ownerUuid().equals(ownerUuid)) {
                ownedPets.add(record);
            }
        }
        return ownedPets;
    }

    public boolean hasFollowingPetOwnedBy(UUID ownerUuid) {
        for (PetRecord record : this.pets.values()) {
            if (record.ownerUuid().equals(ownerUuid) && !record.sitting()) {
                return true;
            }
        }
        return false;
    }

    public void remove(UUID petUuid) {
        if (this.pets.remove(petUuid) != null) {
            setDirty();
        }
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        ListTag petTags = new ListTag();
        for (PetRecord record : this.pets.values()) {
            CompoundTag petTag = new CompoundTag();
            petTag.putUUID("Pet", record.petUuid());
            petTag.putUUID("Owner", record.ownerUuid());
            petTag.putString("Dimension", record.dimension().toString());
            petTag.putLong("Position", record.position().asLong());
            petTag.putBoolean("Sitting", record.sitting());
            petTags.add(petTag);
        }
        tag.put("Pets", petTags);
        return tag;
    }

    public record PetRecord(
            UUID petUuid,
            UUID ownerUuid,
            ResourceLocation dimension,
            BlockPos position,
            boolean sitting
    ) {
    }
}
