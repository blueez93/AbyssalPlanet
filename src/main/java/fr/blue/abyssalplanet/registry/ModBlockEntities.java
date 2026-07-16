package fr.blue.abyssalplanet.registry;

import fr.blue.abyssalplanet.AbyssalPlanet;
import fr.blue.abyssalplanet.block.entity.AbyssalEggBlockEntity;
import fr.blue.abyssalplanet.block.entity.GeorgesHeadBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, AbyssalPlanet.MOD_ID);

    public static final RegistryObject<BlockEntityType<AbyssalEggBlockEntity>> ABYSSAL_EGG =
            BLOCK_ENTITIES.register(
                    "abyssal_egg",
                    () -> BlockEntityType.Builder.of(
                            AbyssalEggBlockEntity::new,
                            ModBlocks.ABYSSAL_EGG.get()
                    ).build(null)
            );

    public static final RegistryObject<BlockEntityType<GeorgesHeadBlockEntity>> GEORGES_HEAD =
            BLOCK_ENTITIES.register(
                    "georges_head",
                    () -> BlockEntityType.Builder.of(
                            GeorgesHeadBlockEntity::new,
                            ModBlocks.GEORGES_HEAD.get()
                    ).build(null)
            );

    private ModBlockEntities() {
    }

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
