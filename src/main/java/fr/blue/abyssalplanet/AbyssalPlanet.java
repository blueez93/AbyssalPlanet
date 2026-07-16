package fr.blue.abyssalplanet;

import com.mojang.logging.LogUtils;
import fr.blue.abyssalplanet.network.ModNetwork;
import fr.blue.abyssalplanet.registry.ModBlocks;
import fr.blue.abyssalplanet.registry.ModBlockEntities;
import fr.blue.abyssalplanet.registry.ModEntities;
import fr.blue.abyssalplanet.registry.ModFluids;
import fr.blue.abyssalplanet.registry.ModCreativeTabs;
import fr.blue.abyssalplanet.registry.ModEffects;
import fr.blue.abyssalplanet.registry.ModEnchantments;
import fr.blue.abyssalplanet.registry.ModItems;
import fr.blue.abyssalplanet.registry.ModMenuTypes;
import fr.blue.abyssalplanet.registry.ModSounds;
import fr.blue.abyssalplanet.registry.ModStructures;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(AbyssalPlanet.MOD_ID)
public class AbyssalPlanet {
    public static final String MOD_ID = "abyssalplanet";
    public static final Logger LOGGER = LogUtils.getLogger();

    public AbyssalPlanet() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        ModFluids.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModItems.register(modEventBus);
        ModEffects.register(modEventBus);
        ModEnchantments.register(modEventBus);
        ModCreativeTabs.register(modEventBus);
        ModEntities.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        ModSounds.register(modEventBus);
        ModStructures.register(modEventBus);
        ModNetwork.register();

        MinecraftForge.EVENT_BUS.register(this);
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }
}
