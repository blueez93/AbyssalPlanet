package fr.blue.abyssalplanet.registry;

import fr.blue.abyssalplanet.AbyssalPlanet;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.RegistryObject;

public final class ModBannerPatterns {
    public static final DeferredRegister<BannerPattern> BANNER_PATTERNS =
            DeferredRegister.create(Registries.BANNER_PATTERN, AbyssalPlanet.MOD_ID);

    public static final RegistryObject<BannerPattern> ABYSSAL_CULT = BANNER_PATTERNS.register(
            "abyssal_cult",
            () -> new BannerPattern("ap_cult")
    );

    private ModBannerPatterns() {
    }

    public static void register(IEventBus eventBus) {
        BANNER_PATTERNS.register(eventBus);
    }
}
