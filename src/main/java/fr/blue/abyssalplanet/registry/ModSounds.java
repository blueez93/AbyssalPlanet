package fr.blue.abyssalplanet.registry;

import fr.blue.abyssalplanet.AbyssalPlanet;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, AbyssalPlanet.MOD_ID);

    public static final RegistryObject<SoundEvent> TOTEM_EQUIP = SOUND_EVENTS.register(
            "item.totem_equip",
            () -> SoundEvent.createVariableRangeEvent(AbyssalPlanet.id("item.totem_equip"))
    );

    public static final RegistryObject<SoundEvent> ABYSSAL_THEME = SOUND_EVENTS.register(
            "music.abyssal_theme",
            () -> SoundEvent.createVariableRangeEvent(AbyssalPlanet.id("music.abyssal_theme"))
    );

    public static final RegistryObject<SoundEvent> KRAKEN_ATTACK_SCREAM = SOUND_EVENTS.register(
            "entity.kraken.attack_scream",
            () -> SoundEvent.createVariableRangeEvent(AbyssalPlanet.id("entity.kraken.attack_scream"))
    );

    public static final RegistryObject<SoundEvent> KRAKEN_HUNT_ROAR = SOUND_EVENTS.register(
            "entity.kraken.hunt_roar",
            () -> SoundEvent.createVariableRangeEvent(AbyssalPlanet.id("entity.kraken.hunt_roar"))
    );

    public static final RegistryObject<SoundEvent> ABYSSAL_POACHER_ALERT = SOUND_EVENTS.register(
            "entity.abyssal_poacher.alert",
            () -> SoundEvent.createVariableRangeEvent(AbyssalPlanet.id("entity.abyssal_poacher.alert"))
    );

    public static final RegistryObject<SoundEvent> ABYSSAL_WANDERER_COMBAT = SOUND_EVENTS.register(
            "entity.abyssal_wanderer.combat",
            () -> SoundEvent.createVariableRangeEvent(AbyssalPlanet.id("entity.abyssal_wanderer.combat"))
    );

    public static final RegistryObject<SoundEvent> ABYSSAL_HUMANOID_HURT = SOUND_EVENTS.register(
            "entity.abyssal_humanoid.hurt",
            () -> SoundEvent.createVariableRangeEvent(AbyssalPlanet.id("entity.abyssal_humanoid.hurt"))
    );

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}
