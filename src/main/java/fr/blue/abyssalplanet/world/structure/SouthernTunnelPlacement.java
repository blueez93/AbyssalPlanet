package fr.blue.abyssalplanet.world.structure;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import fr.blue.abyssalplanet.registry.ModStructures;
import fr.blue.abyssalplanet.world.AbyssalTunnelLocator;
import java.util.Optional;
import net.minecraft.core.Vec3i;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadStructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.RandomSpreadType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacement;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;

public final class SouthernTunnelPlacement extends RandomSpreadStructurePlacement {
    private static final int SPACING_CHUNKS =
            (AbyssalTunnelLocator.MINIMUM_SEPARATION_BLOCKS + 15) / 16;
    public static final Codec<SouthernTunnelPlacement> CODEC = RecordCodecBuilder.create(instance ->
            placementCodec(instance).apply(instance, SouthernTunnelPlacement::new)
    );

    public SouthernTunnelPlacement(
            Vec3i locateOffset,
            StructurePlacement.FrequencyReductionMethod frequencyReductionMethod,
            float frequency,
            int salt,
            Optional<StructurePlacement.ExclusionZone> exclusionZone
    ) {
        super(
                locateOffset,
                frequencyReductionMethod,
                frequency,
                salt,
                exclusionZone,
                SPACING_CHUNKS,
                SPACING_CHUNKS - 1,
                RandomSpreadType.LINEAR
        );
    }

    @Override
    public ChunkPos getPotentialStructureChunk(long seed, int chunkX, int chunkZ) {
        return new ChunkPos(AbyssalTunnelLocator.getCenter(seed));
    }

    @Override
    public StructurePlacementType<?> type() {
        return ModStructures.SOUTHERN_TUNNEL_PLACEMENT.get();
    }
}
