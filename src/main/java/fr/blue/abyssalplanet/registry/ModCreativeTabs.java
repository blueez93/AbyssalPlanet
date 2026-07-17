package fr.blue.abyssalplanet.registry;

import fr.blue.abyssalplanet.AbyssalPlanet;
import fr.blue.abyssalplanet.entity.ZwoingEntity;
import fr.blue.abyssalplanet.item.ZwoingItem;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, AbyssalPlanet.MOD_ID);

    public static final RegistryObject<CreativeModeTab> ABYSSAL_PLANET_TAB =
            CREATIVE_MODE_TABS.register("abyssal_planet_tab", () -> CreativeModeTab.builder()
                    .title(Component.translatable("creativetab.abyssalplanet.abyssal_planet_tab"))
                    .icon(() -> ModItems.SOUL_OF_THE_SEAS.get().getDefaultInstance())
                    .displayItems((parameters, output) -> {
                            output.accept(ModItems.SOUL_OF_THE_SEAS.get());
                            output.accept(ModItems.TYPHOON_CALLER.get());
                            output.accept(ModItems.GO_OUT.get());
                            output.accept(ModItems.ABYSSAL_WATER_BUCKET.get());
                            output.accept(ModBlocks.DECAYED_ABYSSAL_SAND.get());
                            output.accept(ModBlocks.GEORGES_HEAD.get());
                            output.accept(ModBlocks.ABYSSAL_EGG.get());

                            output.accept(ModItems.ABYSSAL_TENTACLE.get());
                            output.accept(ModItems.ABYSSAL_INK.get());
                            output.accept(ModItems.OCEANIC_SOUL_FRAGMENT.get());
                            output.accept(ModItems.KRAKEN_TOOTH.get());
                            output.accept(ModItems.ABYSSAL_SCALE.get());
                            output.accept(ModItems.KRAKEN_HEART.get());
                            output.accept(ModItems.FISH_SKIN.get());
                            output.accept(ModItems.ABYSSAL_POUCH.get());
                            output.accept(ModItems.ABYSSAL_SOUP.get());
                            output.accept(ModItems.ABYSSAL_STAFF.get());
                            output.accept(ModItems.ABYSSAL_WHISTLE.get());
                            output.accept(ModItems.BROKEN_ABYSSAL_WHISTLE.get());
                            output.accept(ModBlocks.BLUE_GOLD_ORE.get());
                            output.accept(ModItems.BLUE_GOLD_INGOT.get());
                            output.accept(ModBlocks.BLUE_GOLD_LOG.get());
                            output.accept(ModBlocks.BLUE_GOLD_LEAVES.get());
                            output.accept(ModBlocks.BLUE_GOLD_SAPLING.get());
                            output.accept(ModItems.BLUE_GOLD_LEAF.get());
                            output.accept(ModItems.ABYSSAL_COMPOST.get());
                            output.accept(ModItems.ABYSSAL_TOXIN.get());
                            output.accept(ModItems.GOLDEN_BLUE_DAGGER.get());
                            output.accept(ModItems.ABYSSAL_HUNTER_CROSSBOW.get());
                            output.accept(ModItems.ABYSSAL_VENOM_1.get());
                            output.accept(ModItems.ABYSSAL_VENOM_2.get());
                            output.accept(ModItems.ABYSSAL_VENOM_3.get());
                            output.accept(ModItems.ABYSSAL_WANDERER_MASK.get());
                            output.accept(ModItems.ABYSSAL_POACHER_SPAWN_EGG.get());
                            output.accept(ModItems.ABYSSAL_WANDERER_SPAWN_EGG.get());
                            output.accept(ModBlocks.ZWOING_FLESH.get());
                            output.accept(ModBlocks.FRESH_ABYSSAL_MEAT.get());
                            output.accept(ZwoingItem.createVariantStack(ZwoingEntity.BLUE));
                            output.accept(ZwoingItem.createVariantStack(ZwoingEntity.RED));
                            output.accept(ZwoingItem.createVariantStack(ZwoingEntity.GREEN));
                            output.accept(ZwoingItem.createVariantStack(ZwoingEntity.PURPLE));
                            output.accept(ZwoingItem.createVariantStack(ZwoingEntity.MULTICOLOR));
                            output.accept(ModItems.ABYSSAL_ALGAE.get());
                    })
                    .build());

    public static void register(IEventBus eventBus) {
        CREATIVE_MODE_TABS.register(eventBus);
    }
}
