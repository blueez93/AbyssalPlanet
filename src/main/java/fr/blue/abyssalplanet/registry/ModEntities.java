package fr.blue.abyssalplanet.registry;

import fr.blue.abyssalplanet.AbyssalPlanet;
import fr.blue.abyssalplanet.entity.AbyssalInkPuddleEntity;
import fr.blue.abyssalplanet.entity.AbyssalBlueMoonfishEntity;
import fr.blue.abyssalplanet.entity.AbyssalOctopusEntity;
import fr.blue.abyssalplanet.entity.AbyssalPouchGhostEntity;
import fr.blue.abyssalplanet.entity.AbyssalPortalEntity;
import fr.blue.abyssalplanet.entity.AbyssalSerpentEntity;
import fr.blue.abyssalplanet.entity.AbyssalShrimpEntity;
import fr.blue.abyssalplanet.entity.AbyssalTyphoonEntity;
import fr.blue.abyssalplanet.entity.AbyssalViperEntity;
import fr.blue.abyssalplanet.entity.BabyKrakenBubbleEntity;
import fr.blue.abyssalplanet.entity.BabyKrakenEntity;
import fr.blue.abyssalplanet.entity.BabyKrakenInkBallEntity;
import fr.blue.abyssalplanet.entity.GeorgesBriochardEntity;
import fr.blue.abyssalplanet.entity.GeorgesJuniorEntity;
import fr.blue.abyssalplanet.entity.GeorgesSeniorEntity;
import fr.blue.abyssalplanet.entity.KrakenBossEntity;
import fr.blue.abyssalplanet.entity.KrakenInkBallEntity;
import fr.blue.abyssalplanet.entity.KrakenTentacleEntity;
import fr.blue.abyssalplanet.entity.LuminousAbyssalFishEntity;
import fr.blue.abyssalplanet.entity.MiniKrakenEntity;
import fr.blue.abyssalplanet.entity.PlayerAbyssalInkBallEntity;
import fr.blue.abyssalplanet.entity.ZwoingEntity;
import fr.blue.abyssalplanet.entity.ZwoingProjectileEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, AbyssalPlanet.MOD_ID);

    public static final RegistryObject<EntityType<AbyssalTyphoonEntity>> ABYSSAL_TYPHOON =
            ENTITIES.register("abyssal_typhoon", () ->
                    EntityType.Builder.<AbyssalTyphoonEntity>of(AbyssalTyphoonEntity::new, MobCategory.MISC)
                            .sized(4.0F, 8.0F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("abyssal_typhoon")
            );

    public static final RegistryObject<EntityType<AbyssalOctopusEntity>> ABYSSAL_OCTOPUS =
            ENTITIES.register("abyssal_octopus", () ->
                    EntityType.Builder.<AbyssalOctopusEntity>of(AbyssalOctopusEntity::new, MobCategory.WATER_CREATURE)
                            .sized(1.0F, 1.0F)
                            .clientTrackingRange(48)
                            .updateInterval(3)
                            .build("abyssal_octopus")
            );

    public static final RegistryObject<EntityType<AbyssalInkPuddleEntity>> ABYSSAL_INK_PUDDLE =
            ENTITIES.register("abyssal_ink_puddle", () ->
                    EntityType.Builder.<AbyssalInkPuddleEntity>of(AbyssalInkPuddleEntity::new, MobCategory.MISC)
                            .sized(0.25F, 0.25F)
                            .clientTrackingRange(32)
                            .updateInterval(5)
                            .build("abyssal_ink_puddle")
            );

    public static final RegistryObject<EntityType<LuminousAbyssalFishEntity>> LUMINOUS_ABYSSAL_FISH =
            ENTITIES.register("luminous_abyssal_fish", () ->
                    EntityType.Builder.<LuminousAbyssalFishEntity>of(LuminousAbyssalFishEntity::new, MobCategory.WATER_AMBIENT)
                            .sized(0.5F, 0.35F)
                            .clientTrackingRange(48)
                            .updateInterval(3)
                            .build("luminous_abyssal_fish")
            );

    public static final RegistryObject<EntityType<AbyssalBlueMoonfishEntity>> ABYSSAL_BLUE_MOONFISH =
            ENTITIES.register("abyssal_blue_moonfish", () ->
                    EntityType.Builder.<AbyssalBlueMoonfishEntity>of(AbyssalBlueMoonfishEntity::new, MobCategory.WATER_AMBIENT)
                            .sized(1.25F, 0.85F)
                            .clientTrackingRange(28)
                            .updateInterval(3)
                            .build("abyssal_blue_moonfish")
            );

    public static final RegistryObject<EntityType<MiniKrakenEntity>> MINI_KRAKEN =
            ENTITIES.register("mini_kraken", () ->
                    EntityType.Builder.<MiniKrakenEntity>of(MiniKrakenEntity::new, MobCategory.WATER_CREATURE)
                            .sized(2.4F, 1.7F)
                            .clientTrackingRange(64)
                            .updateInterval(3)
                            .build("mini_kraken")
            );

    public static final RegistryObject<EntityType<BabyKrakenEntity>> BABY_KRAKEN =
            ENTITIES.register("baby_kraken", () ->
                    EntityType.Builder.<BabyKrakenEntity>of(BabyKrakenEntity::new, MobCategory.WATER_CREATURE)
                            .sized(0.9F, 0.85F)
                            .clientTrackingRange(64)
                            .updateInterval(2)
                            .build("baby_kraken")
            );

    public static final RegistryObject<EntityType<BabyKrakenInkBallEntity>> BABY_KRAKEN_INK_BALL =
            ENTITIES.register("baby_kraken_ink_ball", () ->
                    EntityType.Builder.<BabyKrakenInkBallEntity>of(BabyKrakenInkBallEntity::new, MobCategory.MISC)
                            .sized(0.35F, 0.35F)
                            .clientTrackingRange(72)
                            .updateInterval(1)
                            .build("baby_kraken_ink_ball")
            );

    public static final RegistryObject<EntityType<BabyKrakenBubbleEntity>> BABY_KRAKEN_BUBBLE =
            ENTITIES.register("baby_kraken_bubble", () ->
                    EntityType.Builder.<BabyKrakenBubbleEntity>of(BabyKrakenBubbleEntity::new, MobCategory.MISC)
                            .sized(0.24F, 0.24F)
                            .clientTrackingRange(72)
                            .updateInterval(1)
                            .build("baby_kraken_bubble")
            );

    public static final RegistryObject<EntityType<KrakenBossEntity>> KRAKEN_BOSS =
            ENTITIES.register("kraken_boss", () ->
                    EntityType.Builder.<KrakenBossEntity>of(KrakenBossEntity::new, MobCategory.MONSTER)
                            .sized(18.0F, 12.0F)
                            .clientTrackingRange(160)
                            .updateInterval(2)
                            .build("kraken_boss")
            );

    public static final RegistryObject<EntityType<KrakenTentacleEntity>> KRAKEN_TENTACLE =
            ENTITIES.register("kraken_tentacle", () ->
                    EntityType.Builder.<KrakenTentacleEntity>of(KrakenTentacleEntity::new, MobCategory.MISC)
                            .sized(0.85F, 0.85F)
                            .clientTrackingRange(128)
                            .updateInterval(1)
                            .build("kraken_tentacle")
            );

    public static final RegistryObject<EntityType<KrakenInkBallEntity>> KRAKEN_INK_BALL =
            ENTITIES.register("kraken_ink_ball", () ->
                    EntityType.Builder.<KrakenInkBallEntity>of(KrakenInkBallEntity::new, MobCategory.MISC)
                            .sized(1.2F, 1.2F)
                            .clientTrackingRange(96)
                            .updateInterval(1)
                            .build("kraken_ink_ball")
            );

    public static final RegistryObject<EntityType<PlayerAbyssalInkBallEntity>> PLAYER_ABYSSAL_INK_BALL =
            ENTITIES.register("player_abyssal_ink_ball", () ->
                    EntityType.Builder.<PlayerAbyssalInkBallEntity>of(PlayerAbyssalInkBallEntity::new, MobCategory.MISC)
                            .sized(0.8F, 0.8F)
                            .clientTrackingRange(96)
                            .updateInterval(1)
                            .build("player_abyssal_ink_ball")
            );

    public static final RegistryObject<EntityType<AbyssalPortalEntity>> ABYSSAL_PORTAL =
            ENTITIES.register("abyssal_portal", () ->
                    EntityType.Builder.<AbyssalPortalEntity>of(AbyssalPortalEntity::new, MobCategory.MISC)
                            .sized(1.2F, 1.2F)
                            .clientTrackingRange(128)
                            .updateInterval(1)
                            .build("abyssal_portal")
            );

    public static final RegistryObject<EntityType<AbyssalPouchGhostEntity>> ABYSSAL_POUCH_GHOST =
            ENTITIES.register("abyssal_pouch_ghost", () ->
                    EntityType.Builder.<AbyssalPouchGhostEntity>of(AbyssalPouchGhostEntity::new, MobCategory.MISC)
                            .sized(0.75F, 0.45F)
                            .clientTrackingRange(64)
                            .updateInterval(3)
                            .build("abyssal_pouch_ghost")
            );

    public static final RegistryObject<EntityType<ZwoingEntity>> ZWOING =
            ENTITIES.register("zwoing", () ->
                    EntityType.Builder.<ZwoingEntity>of(ZwoingEntity::new, MobCategory.WATER_AMBIENT)
                            .sized(0.55F, 0.55F)
                            .clientTrackingRange(48)
                            .updateInterval(2)
                            .build("zwoing")
            );

    public static final RegistryObject<EntityType<ZwoingProjectileEntity>> ZWOING_PROJECTILE =
            ENTITIES.register("zwoing_projectile", () ->
                    EntityType.Builder.<ZwoingProjectileEntity>of(ZwoingProjectileEntity::new, MobCategory.MISC)
                            .sized(0.35F, 0.35F)
                            .clientTrackingRange(64)
                            .updateInterval(1)
                            .build("zwoing_projectile")
            );

    public static final RegistryObject<EntityType<AbyssalViperEntity>> ABYSSAL_VIPER =
            ENTITIES.register("abyssal_viper", () ->
                    EntityType.Builder.<AbyssalViperEntity>of(AbyssalViperEntity::new, MobCategory.MONSTER)
                            .sized(1.8F, 0.85F)
                            .clientTrackingRange(96)
                            .updateInterval(2)
                            .build("abyssal_viper")
            );

    public static final RegistryObject<EntityType<AbyssalSerpentEntity>> ABYSSAL_SERPENT =
            ENTITIES.register("abyssal_serpent", () ->
                    EntityType.Builder.<AbyssalSerpentEntity>of(AbyssalSerpentEntity::new, MobCategory.MONSTER)
                            .sized(7.5F, 3.0F)
                            .clientTrackingRange(192)
                            .updateInterval(1)
                            .build("abyssal_serpent")
            );

    public static final RegistryObject<EntityType<GeorgesBriochardEntity>> GEORGES_BRIOCHARD =
            ENTITIES.register("georges_briochard", () ->
                    EntityType.Builder.<GeorgesBriochardEntity>of(
                                    GeorgesBriochardEntity::new,
                                    MobCategory.MONSTER
                            )
                            .sized(3.8F, 2.6F)
                            .clientTrackingRange(160)
                            .updateInterval(1)
                            .build("georges_briochard")
            );

    public static final RegistryObject<EntityType<GeorgesJuniorEntity>> GEORGES_JUNIOR =
            ENTITIES.register("georges_junior", () ->
                    EntityType.Builder.<GeorgesJuniorEntity>of(
                                    GeorgesJuniorEntity::new,
                                    MobCategory.WATER_CREATURE
                            )
                            .sized(1.25F, 0.9F)
                            .clientTrackingRange(96)
                            .updateInterval(2)
                            .build("georges_junior")
            );

    public static final RegistryObject<EntityType<GeorgesSeniorEntity>> GEORGES_SENIOR =
            ENTITIES.register("georges_senior", () ->
                    EntityType.Builder.<GeorgesSeniorEntity>of(
                                    GeorgesSeniorEntity::new,
                                    MobCategory.WATER_CREATURE
                            )
                            .sized(3.8F, 2.6F)
                            .clientTrackingRange(160)
                            .updateInterval(1)
                            .build("georges_senior")
            );

    public static final RegistryObject<EntityType<AbyssalShrimpEntity>> ABYSSAL_SHRIMP =
            ENTITIES.register("abyssal_shrimp", () ->
                    EntityType.Builder.<AbyssalShrimpEntity>of(
                                    AbyssalShrimpEntity::new,
                                    MobCategory.WATER_CREATURE
                            )
                            .sized(0.72F, 0.48F)
                            .clientTrackingRange(80)
                            .updateInterval(2)
                            .build("abyssal_shrimp")
            );

    public static void register(IEventBus eventBus) {
        ENTITIES.register(eventBus);
    }
}
