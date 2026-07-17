package fr.blue.abyssalplanet.registry;

import fr.blue.abyssalplanet.AbyssalPlanet;
import fr.blue.abyssalplanet.item.AbyssalAlgaeItem;
import fr.blue.abyssalplanet.item.AbyssalHunterCrossbowItem;
import fr.blue.abyssalplanet.item.AbyssalPouchItem;
import fr.blue.abyssalplanet.item.AbyssalStaffItem;
import fr.blue.abyssalplanet.item.AbyssalSoupItem;
import fr.blue.abyssalplanet.item.AbyssalVenomItem;
import fr.blue.abyssalplanet.item.AbyssalWandererMaskItem;
import fr.blue.abyssalplanet.item.AbyssalWhistleItem;
import fr.blue.abyssalplanet.item.BrokenAbyssalWhistleItem;
import fr.blue.abyssalplanet.item.BlueGoldLeafItem;
import fr.blue.abyssalplanet.item.GoldenBlueDaggerItem;
import fr.blue.abyssalplanet.item.GoOutItem;
import fr.blue.abyssalplanet.item.ModArmorMaterials;
import fr.blue.abyssalplanet.item.SoulOfTheSeasItem;
import fr.blue.abyssalplanet.item.TyphoonCallerItem;
import fr.blue.abyssalplanet.item.ZwoingItem;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.Tiers;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems {
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, AbyssalPlanet.MOD_ID);

    public static final RegistryObject<Item> SOUL_OF_THE_SEAS = ITEMS.register(
            "soul_of_the_seas",
            () -> new SoulOfTheSeasItem(new Item.Properties()
                    .stacksTo(1)
                    .rarity(Rarity.EPIC)
                    .fireResistant())
    );

    public static final RegistryObject<Item> TYPHOON_CALLER = ITEMS.register(
            "typhoon_caller",
            () -> new TyphoonCallerItem(new Item.Properties()
                    .stacksTo(1)
                    .rarity(Rarity.RARE))
    );

    public static final RegistryObject<Item> GO_OUT = ITEMS.register(
            "go_out",
            () -> new GoOutItem(new Item.Properties()
                    .stacksTo(1)
                    .rarity(Rarity.RARE))
    );

    public static final RegistryObject<Item> ABYSSAL_WATER_BUCKET = ITEMS.register(
            "abyssal_water_bucket",
            () -> new BucketItem(
                    ModFluids.ABYSSAL_WATER_SOURCE,
                    new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)
            )
    );
    public static final RegistryObject<Item> ABYSSAL_TENTACLE = ITEMS.register(
        "abyssal_tentacle",
        () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON))
);

public static final RegistryObject<Item> ABYSSAL_INK = ITEMS.register(
        "abyssal_ink",
        () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON))
);

public static final RegistryObject<Item> OCEANIC_SOUL_FRAGMENT = ITEMS.register(
        "oceanic_soul_fragment",
        () -> new Item(new Item.Properties().rarity(Rarity.RARE))
);

public static final RegistryObject<Item> KRAKEN_TOOTH = ITEMS.register(
        "kraken_tooth",
        () -> new Item(new Item.Properties().rarity(Rarity.RARE))
);

public static final RegistryObject<Item> ABYSSAL_SCALE = ITEMS.register(
        "abyssal_scale",
        () -> new Item(new Item.Properties().rarity(Rarity.RARE))
);

public static final RegistryObject<Item> BLUE_GOLD_INGOT = ITEMS.register(
        "blue_gold_ingot",
        () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON).fireResistant())
);

public static final RegistryObject<Item> BLUE_GOLD_LEAF = ITEMS.register(
        "blue_gold_leaf",
        () -> new BlueGoldLeafItem(new Item.Properties()
                .stacksTo(64)
                .rarity(Rarity.UNCOMMON)
                .food(new FoodProperties.Builder()
                        .nutrition(1)
                        .saturationMod(0.15F)
                        .alwaysEat()
                        .build()))
);

public static final RegistryObject<Item> ABYSSAL_COMPOST = ITEMS.register(
        "abyssal_compost",
        () -> new Item(new Item.Properties().rarity(Rarity.COMMON))
);

public static final RegistryObject<Item> ABYSSAL_TOXIN = ITEMS.register(
        "abyssal_toxin",
        () -> new Item(new Item.Properties().rarity(Rarity.UNCOMMON))
);

public static final RegistryObject<Item> GOLDEN_BLUE_DAGGER = ITEMS.register(
        "golden_blue_dagger",
        () -> new GoldenBlueDaggerItem(
                Tiers.DIAMOND,
                new Item.Properties().rarity(Rarity.RARE).fireResistant()
        )
);

public static final RegistryObject<Item> ABYSSAL_HUNTER_CROSSBOW = ITEMS.register(
        "abyssal_hunter_crossbow",
        () -> new AbyssalHunterCrossbowItem(new Item.Properties()
                .durability(512)
                .rarity(Rarity.EPIC)
                .fireResistant())
);

public static final RegistryObject<Item> ZWOING = ITEMS.register(
        "zwoing",
        () -> new ZwoingItem(new Item.Properties().stacksTo(1))
);

public static final RegistryObject<Item> MULTICOLOR_ZWOING = ITEMS.register(
        "multicolor_zwoing",
        () -> new ZwoingItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC))
);

public static final RegistryObject<Item> ABYSSAL_ALGAE = ITEMS.register(
        "abyssal_algae",
        () -> new AbyssalAlgaeItem(
                ModBlocks.ABYSSAL_ALGAE.get(),
                new Item.Properties()
                        .stacksTo(64)
                        .rarity(Rarity.UNCOMMON)
                        .food(new FoodProperties.Builder()
                                .nutrition(1)
                                .saturationMod(0.1F)
                                .alwaysEat()
                                .build())
        )
);

public static final RegistryObject<Item> KRAKEN_HEART = ITEMS.register(
        "kraken_heart",
        () -> new Item(new Item.Properties()
                .rarity(Rarity.EPIC)
                .fireResistant())
);

public static final RegistryObject<Item> FISH_SKIN = ITEMS.register(
        "fish_skin",
        () -> new Item(new Item.Properties().rarity(Rarity.COMMON))
);

public static final RegistryObject<Item> ABYSSAL_POUCH = ITEMS.register(
        "abyssal_pouch",
        () -> new AbyssalPouchItem(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.RARE))
);

public static final RegistryObject<Item> ABYSSAL_SOUP = ITEMS.register(
        "abyssal_soup",
        () -> new AbyssalSoupItem(new Item.Properties()
                .stacksTo(16)
                .rarity(Rarity.UNCOMMON)
                .food(new FoodProperties.Builder()
                        .nutrition(6)
                        .saturationMod(0.6F)
                        .alwaysEat()
                        .build()))
);

public static final RegistryObject<Item> ABYSSAL_STAFF = ITEMS.register(
        "abyssal_staff",
        () -> new AbyssalStaffItem(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.EPIC)
                .fireResistant())
);

public static final RegistryObject<Item> ABYSSAL_WHISTLE = ITEMS.register(
        "abyssal_whistle",
        () -> new AbyssalWhistleItem(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.EPIC)
                .fireResistant())
);

public static final RegistryObject<Item> BROKEN_ABYSSAL_WHISTLE = ITEMS.register(
        "broken_abyssal_whistle",
        () -> new BrokenAbyssalWhistleItem(new Item.Properties()
                .stacksTo(1)
                .rarity(Rarity.RARE))
);

public static final RegistryObject<Item> ABYSSAL_VENOM_1 = ITEMS.register(
        "abyssal_venom_1",
        () -> new AbyssalVenomItem(1, new Item.Properties().stacksTo(16).rarity(Rarity.EPIC).fireResistant())
);

public static final RegistryObject<Item> ABYSSAL_VENOM_2 = ITEMS.register(
        "abyssal_venom_2",
        () -> new AbyssalVenomItem(2, new Item.Properties().stacksTo(16).rarity(Rarity.EPIC).fireResistant())
);

public static final RegistryObject<Item> ABYSSAL_VENOM_3 = ITEMS.register(
        "abyssal_venom_3",
        () -> new AbyssalVenomItem(3, new Item.Properties().stacksTo(16).rarity(Rarity.EPIC).fireResistant())
);

public static final RegistryObject<Item> ABYSSAL_WANDERER_MASK = ITEMS.register(
        "abyssal_wanderer_mask",
        () -> new AbyssalWandererMaskItem(
                ModArmorMaterials.ABYSSAL_WANDERER_MASK,
                new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)
        )
);

public static final RegistryObject<Item> ABYSSAL_POACHER_SPAWN_EGG = ITEMS.register(
        "abyssal_poacher_spawn_egg",
        () -> new ForgeSpawnEggItem(
                ModEntities.ABYSSAL_POACHER,
                0xC6B7A9,
                0x5A241F,
                new Item.Properties()
        )
);

public static final RegistryObject<Item> ABYSSAL_WANDERER_SPAWN_EGG = ITEMS.register(
        "abyssal_wanderer_spawn_egg",
        () -> new ForgeSpawnEggItem(
                ModEntities.ABYSSAL_WANDERER,
                0x8E2C2C,
                0x35205B,
                new Item.Properties()
        )
);

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
