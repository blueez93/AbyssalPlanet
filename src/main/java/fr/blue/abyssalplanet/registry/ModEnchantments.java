package fr.blue.abyssalplanet.registry;

import fr.blue.abyssalplanet.AbyssalPlanet;
import fr.blue.abyssalplanet.enchantment.AbyssalVenomEnchantment;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEnchantments {
    public static final DeferredRegister<Enchantment> ENCHANTMENTS =
            DeferredRegister.create(ForgeRegistries.ENCHANTMENTS, AbyssalPlanet.MOD_ID);

    public static final RegistryObject<Enchantment> ABYSSAL_VENOM =
            ENCHANTMENTS.register("abyssal_venom", AbyssalVenomEnchantment::new);

    private ModEnchantments() {
    }

    public static void register(IEventBus eventBus) {
        ENCHANTMENTS.register(eventBus);
    }
}
