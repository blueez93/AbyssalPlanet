package fr.blue.abyssalplanet.registry;

import fr.blue.abyssalplanet.AbyssalPlanet;
import fr.blue.abyssalplanet.effect.AbyssalPoisonEffect;
import fr.blue.abyssalplanet.effect.BlueGoldHeartsEffect;
import fr.blue.abyssalplanet.effect.BleedingEffect;
import fr.blue.abyssalplanet.effect.ToxicBurnEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, AbyssalPlanet.MOD_ID);

    public static final RegistryObject<MobEffect> ABYSSAL_POISON =
            EFFECTS.register("abyssal_poison", AbyssalPoisonEffect::new);

    public static final RegistryObject<MobEffect> BLUE_GOLD_HEARTS =
            EFFECTS.register("blue_gold_hearts", BlueGoldHeartsEffect::new);

    public static final RegistryObject<MobEffect> TOXIC_BURN =
            EFFECTS.register("toxic_burn", ToxicBurnEffect::new);

    public static final RegistryObject<MobEffect> BLEEDING =
            EFFECTS.register("bleeding", BleedingEffect::new);

    private ModEffects() {
    }

    public static void register(IEventBus eventBus) {
        EFFECTS.register(eventBus);
    }
}
