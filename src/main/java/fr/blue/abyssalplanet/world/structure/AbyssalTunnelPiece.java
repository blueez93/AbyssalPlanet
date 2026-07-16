package fr.blue.abyssalplanet.world.structure;

import fr.blue.abyssalplanet.registry.ModStructures;
import fr.blue.abyssalplanet.world.AbyssalTunnelLocator;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;

public final class AbyssalTunnelPiece extends StructurePiece {
    public AbyssalTunnelPiece(BlockPos center) {
        super(
                ModStructures.ABYSSAL_TUNNEL_PIECE.get(),
                0,
                new BoundingBox(
                        center.getX(),
                        AbyssalTunnelLocator.BOTTOM_FLOOR_Y,
                        center.getZ(),
                        center.getX(),
                        AbyssalTunnelLocator.CAVERN_CEILING_Y,
                        center.getZ()
                )
        );
    }

    public AbyssalTunnelPiece(CompoundTag tag) {
        super(ModStructures.ABYSSAL_TUNNEL_PIECE.get(), tag);
    }

    @Override
    protected void addAdditionalSaveData(
            StructurePieceSerializationContext context,
            CompoundTag tag
    ) {
    }

    @Override
    public void postProcess(
            WorldGenLevel level,
            StructureManager structureManager,
            ChunkGenerator chunkGenerator,
            RandomSource random,
            BoundingBox chunkBounds,
            ChunkPos chunkPos,
            BlockPos pivot
    ) {
        // Terrain carving remains in AbyssalTerrainGeneration so old worlds can migrate safely.
    }
}
