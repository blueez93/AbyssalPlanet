package fr.blue.abyssalplanet.registry;

import fr.blue.abyssalplanet.AbyssalPlanet;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.dimension.DimensionType;

public class ModDimensions {
    public static final ResourceKey<Level> ABYSSAL_PLANET_LEVEL =
            ResourceKey.create(
                    Registries.DIMENSION,
                    AbyssalPlanet.id("abyssal_planet")
            );

    public static final ResourceKey<DimensionType> ABYSSAL_PLANET_TYPE =
            ResourceKey.create(
                    Registries.DIMENSION_TYPE,
                    AbyssalPlanet.id("abyssal_planet_type")
            );
}