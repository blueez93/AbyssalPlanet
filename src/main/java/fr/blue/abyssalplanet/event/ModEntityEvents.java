package fr.blue.abyssalplanet.event;

import fr.blue.abyssalplanet.AbyssalPlanet;
import fr.blue.abyssalplanet.entity.AbyssalBlueMoonfishEntity;
import fr.blue.abyssalplanet.entity.AbyssalOctopusEntity;
import fr.blue.abyssalplanet.entity.AbyssalSerpentEntity;
import fr.blue.abyssalplanet.entity.AbyssalShrimpEntity;
import fr.blue.abyssalplanet.entity.AbyssalViperEntity;
import fr.blue.abyssalplanet.entity.AbyssalPoacherEntity;
import fr.blue.abyssalplanet.entity.AbyssalWandererEntity;
import fr.blue.abyssalplanet.entity.BabyKrakenEntity;
import fr.blue.abyssalplanet.entity.GeorgesBriochardEntity;
import fr.blue.abyssalplanet.entity.GeorgesJuniorEntity;
import fr.blue.abyssalplanet.entity.GeorgesSeniorEntity;
import fr.blue.abyssalplanet.entity.KrakenBossEntity;
import fr.blue.abyssalplanet.entity.LuminousAbyssalFishEntity;
import fr.blue.abyssalplanet.entity.MiniKrakenEntity;
import fr.blue.abyssalplanet.entity.ZwoingEntity;
import fr.blue.abyssalplanet.registry.ModEntities;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(
        modid = AbyssalPlanet.MOD_ID,
        bus = Mod.EventBusSubscriber.Bus.MOD
)
public class ModEntityEvents {
    @SubscribeEvent
    public static void registerAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.ABYSSAL_OCTOPUS.get(), AbyssalOctopusEntity.createAttributes().build());
        event.put(ModEntities.ABYSSAL_BLUE_MOONFISH.get(), AbyssalBlueMoonfishEntity.createAttributes().build());
        event.put(ModEntities.LUMINOUS_ABYSSAL_FISH.get(), LuminousAbyssalFishEntity.createAttributes().build());
        event.put(ModEntities.MINI_KRAKEN.get(), MiniKrakenEntity.createAttributes().build());
        event.put(ModEntities.BABY_KRAKEN.get(), BabyKrakenEntity.createAttributes().build());
        event.put(ModEntities.KRAKEN_BOSS.get(), KrakenBossEntity.createAttributes().build());
        event.put(ModEntities.ZWOING.get(), ZwoingEntity.createAttributes().build());
        event.put(ModEntities.ABYSSAL_VIPER.get(), AbyssalViperEntity.createAttributes().build());
        event.put(ModEntities.ABYSSAL_SERPENT.get(), AbyssalSerpentEntity.createAttributes().build());
        event.put(ModEntities.GEORGES_BRIOCHARD.get(), GeorgesBriochardEntity.createAttributes().build());
        event.put(ModEntities.GEORGES_JUNIOR.get(), GeorgesJuniorEntity.createAttributes().build());
        event.put(ModEntities.GEORGES_SENIOR.get(), GeorgesSeniorEntity.createAttributes().build());
        event.put(ModEntities.ABYSSAL_SHRIMP.get(), AbyssalShrimpEntity.createAttributes().build());
        event.put(ModEntities.ABYSSAL_POACHER.get(), AbyssalPoacherEntity.createAttributes().build());
        event.put(ModEntities.ABYSSAL_WANDERER.get(), AbyssalWandererEntity.createAttributes().build());
    }

    @SubscribeEvent
    public static void registerSpawnPlacements(SpawnPlacementRegisterEvent event) {
        event.register(
                ModEntities.ABYSSAL_OCTOPUS.get(),
                SpawnPlacements.Type.IN_WATER,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                AbyssalOctopusEntity::canSpawn,
                SpawnPlacementRegisterEvent.Operation.REPLACE
        );
        event.register(
                ModEntities.ABYSSAL_BLUE_MOONFISH.get(),
                SpawnPlacements.Type.IN_WATER,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                AbyssalBlueMoonfishEntity::canSpawn,
                SpawnPlacementRegisterEvent.Operation.REPLACE
        );
        event.register(
                ModEntities.LUMINOUS_ABYSSAL_FISH.get(),
                SpawnPlacements.Type.IN_WATER,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                LuminousAbyssalFishEntity::canSpawn,
                SpawnPlacementRegisterEvent.Operation.REPLACE
        );
        event.register(
                ModEntities.MINI_KRAKEN.get(),
                SpawnPlacements.Type.IN_WATER,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                MiniKrakenEntity::canSpawn,
                SpawnPlacementRegisterEvent.Operation.REPLACE
        );
        event.register(
                ModEntities.BABY_KRAKEN.get(),
                SpawnPlacements.Type.IN_WATER,
                Heightmap.Types.OCEAN_FLOOR,
                BabyKrakenEntity::canSpawn,
                SpawnPlacementRegisterEvent.Operation.REPLACE
        );
        event.register(
                ModEntities.KRAKEN_BOSS.get(),
                SpawnPlacements.Type.IN_WATER,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                KrakenBossEntity::canSpawn,
                SpawnPlacementRegisterEvent.Operation.REPLACE
        );
        event.register(
                ModEntities.ZWOING.get(),
                SpawnPlacements.Type.IN_WATER,
                Heightmap.Types.OCEAN_FLOOR,
                ZwoingEntity::canSpawn,
                SpawnPlacementRegisterEvent.Operation.REPLACE
        );
        event.register(
                ModEntities.ABYSSAL_VIPER.get(),
                SpawnPlacements.Type.IN_WATER,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                AbyssalViperEntity::canSpawn,
                SpawnPlacementRegisterEvent.Operation.REPLACE
        );
        event.register(
                ModEntities.ABYSSAL_SERPENT.get(),
                SpawnPlacements.Type.IN_WATER,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                AbyssalSerpentEntity::canSpawn,
                SpawnPlacementRegisterEvent.Operation.REPLACE
        );
        event.register(
                ModEntities.GEORGES_BRIOCHARD.get(),
                SpawnPlacements.Type.IN_WATER,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                GeorgesBriochardEntity::canSpawn,
                SpawnPlacementRegisterEvent.Operation.REPLACE
        );
    }
}
