package fr.blue.abyssalplanet.registry;

import fr.blue.abyssalplanet.AbyssalPlanet;
import fr.blue.abyssalplanet.block.AbyssalAlgaeBlock;
import fr.blue.abyssalplanet.block.AbyssalBaitBlock;
import fr.blue.abyssalplanet.block.AbyssalEggBlock;
import fr.blue.abyssalplanet.block.BlueGoldSaplingBlock;
import fr.blue.abyssalplanet.block.GeorgesHeadBlock;
import fr.blue.abyssalplanet.item.GeorgesHeadItem;
import net.minecraft.util.valueproviders.UniformInt;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DropExperienceBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SandBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Supplier;

public final class ModBlocks {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(ForgeRegistries.BLOCKS, AbyssalPlanet.MOD_ID);

    public static final RegistryObject<Block> BLUE_GOLD_ORE = registerBlock(
            "blue_gold_ore",
            () -> new DropExperienceBlock(
                    BlockBehaviour.Properties.copy(Blocks.GOLD_ORE)
                            .strength(3.5F, 4.0F)
                            .requiresCorrectToolForDrops(),
                    UniformInt.of(2, 5)
            )
    );

    public static final RegistryObject<LiquidBlock> ABYSSAL_WATER = BLOCKS.register(
            "abyssal_water",
            () -> new LiquidBlock(
                    ModFluids.ABYSSAL_WATER_SOURCE,
                    BlockBehaviour.Properties.copy(Blocks.WATER).noLootTable()
            )
    );

    public static final RegistryObject<Block> DECAYED_ABYSSAL_SAND = registerBlock(
            "decayed_abyssal_sand",
            () -> new SandBlock(
                    0x7A1727,
                    BlockBehaviour.Properties.copy(Blocks.RED_SAND)
                            .strength(0.6F)
                            .sound(net.minecraft.world.level.block.SoundType.SAND)
            )
    );

    public static final RegistryObject<GeorgesHeadBlock> GEORGES_HEAD = BLOCKS.register(
            "georges_head",
            () -> new GeorgesHeadBlock(
                    BlockBehaviour.Properties.copy(Blocks.CREEPER_HEAD)
                            .strength(1.0F)
                            .noOcclusion()
            )
    );

    static {
        ModItems.ITEMS.register(
                "georges_head",
                () -> new GeorgesHeadItem(
                        GEORGES_HEAD.get(),
                        new Item.Properties().stacksTo(1).rarity(net.minecraft.world.item.Rarity.RARE)
                )
        );
    }

    public static final RegistryObject<Block> ABYSSAL_EGG = registerBlock(
            "abyssal_egg",
            () -> new AbyssalEggBlock(
                    BlockBehaviour.Properties.copy(Blocks.DRAGON_EGG)
                            .strength(3.0F, 9.0F)
                            .lightLevel(state -> 3)
                            .noOcclusion()
            )
    );

    public static final RegistryObject<Block> BLUE_GOLD_LOG = registerBlock(
            "blue_gold_log",
            () -> new RotatedPillarBlock(BlockBehaviour.Properties.copy(Blocks.OAK_LOG))
    );

    public static final RegistryObject<Block> BLUE_GOLD_LEAVES = registerBlock(
            "blue_gold_leaves",
            () -> new LeavesBlock(BlockBehaviour.Properties.copy(Blocks.OAK_LEAVES))
    );

    public static final RegistryObject<Block> BLUE_GOLD_SAPLING = registerBlock(
            "blue_gold_sapling",
            () -> new BlueGoldSaplingBlock(BlockBehaviour.Properties.copy(Blocks.OAK_SAPLING))
    );

    public static final RegistryObject<Block> ABYSSAL_ALGAE = BLOCKS.register(
            "abyssal_algae",
            () -> new AbyssalAlgaeBlock(BlockBehaviour.Properties.copy(Blocks.TALL_SEAGRASS))
    );

    public static final RegistryObject<Block> ZWOING_FLESH = registerBlock(
            "zwoing_flesh",
            () -> new AbyssalBaitBlock(
                    BlockBehaviour.Properties.copy(Blocks.CLAY)
                            .strength(0.15F)
                            .noOcclusion(),
                    false
            )
    );

    public static final RegistryObject<Block> FRESH_ABYSSAL_MEAT = registerBlock(
            "fresh_abyssal_meat",
            () -> new AbyssalBaitBlock(
                    BlockBehaviour.Properties.copy(Blocks.CLAY)
                            .strength(0.15F)
                            .noOcclusion(),
                    true
            )
    );

    private ModBlocks() {
    }

    private static <T extends Block> RegistryObject<T> registerBlock(String name, Supplier<T> supplier) {
        RegistryObject<T> block = BLOCKS.register(name, supplier);
        ModItems.ITEMS.register(name, () -> new BlockItem(block.get(), new Item.Properties()));
        return block;
    }

    public static void register(IEventBus eventBus) {
        BLOCKS.register(eventBus);
    }
}
