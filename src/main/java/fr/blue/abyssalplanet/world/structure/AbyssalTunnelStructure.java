package fr.blue.abyssalplanet.world.structure;

import com.mojang.serialization.Codec;
import fr.blue.abyssalplanet.registry.ModStructures;
import fr.blue.abyssalplanet.world.AbyssalTunnelLocator;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.levelgen.structure.Structure;
import net.minecraft.world.level.levelgen.structure.StructureType;

public final class AbyssalTunnelStructure extends Structure {
    public static final Codec<AbyssalTunnelStructure> CODEC =
            simpleCodec(AbyssalTunnelStructure::new);

    public AbyssalTunnelStructure(StructureSettings settings) {
        super(settings);
    }

    @Override
    public Optional<GenerationStub> findGenerationPoint(GenerationContext context) {
        BlockPos center = AbyssalTunnelLocator.getCenter(context.seed());
        if (!context.chunkPos().equals(new ChunkPos(center))) {
            return Optional.empty();
        }

        return Optional.of(new GenerationStub(center, builder ->
                builder.addPiece(new AbyssalTunnelPiece(center))
        ));
    }

    @Override
    public StructureType<?> type() {
        return ModStructures.ABYSSAL_TUNNEL.get();
    }
}
