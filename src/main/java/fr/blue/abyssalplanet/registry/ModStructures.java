package fr.blue.abyssalplanet.registry;

import fr.blue.abyssalplanet.AbyssalPlanet;
import fr.blue.abyssalplanet.world.structure.AbyssalTunnelPiece;
import fr.blue.abyssalplanet.world.structure.AbyssalTunnelStructure;
import fr.blue.abyssalplanet.world.structure.SouthernTunnelPlacement;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.structure.StructureType;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.placement.StructurePlacementType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModStructures {
    private static final DeferredRegister<StructureType<?>> STRUCTURE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_TYPE, AbyssalPlanet.MOD_ID);
    private static final DeferredRegister<StructurePieceType> PIECE_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_PIECE, AbyssalPlanet.MOD_ID);
    private static final DeferredRegister<StructurePlacementType<?>> PLACEMENT_TYPES =
            DeferredRegister.create(Registries.STRUCTURE_PLACEMENT, AbyssalPlanet.MOD_ID);

    public static final RegistryObject<StructureType<AbyssalTunnelStructure>> ABYSSAL_TUNNEL =
            STRUCTURE_TYPES.register("abyssal_tunnel", () -> () -> AbyssalTunnelStructure.CODEC);
    public static final RegistryObject<StructurePieceType> ABYSSAL_TUNNEL_PIECE =
            PIECE_TYPES.register("abyssal_tunnel", () ->
                    (context, tag) -> new AbyssalTunnelPiece(tag));
    public static final RegistryObject<StructurePlacementType<SouthernTunnelPlacement>> SOUTHERN_TUNNEL_PLACEMENT =
            PLACEMENT_TYPES.register("southern_tunnel", () -> () -> SouthernTunnelPlacement.CODEC);

    private ModStructures() {
    }

    public static void register(IEventBus eventBus) {
        STRUCTURE_TYPES.register(eventBus);
        PIECE_TYPES.register(eventBus);
        PLACEMENT_TYPES.register(eventBus);
    }
}
