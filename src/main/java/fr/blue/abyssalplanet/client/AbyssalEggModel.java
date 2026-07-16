package fr.blue.abyssalplanet.client;

import fr.blue.abyssalplanet.AbyssalPlanet;
import fr.blue.abyssalplanet.block.entity.AbyssalEggBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class AbyssalEggModel extends GeoModel<AbyssalEggBlockEntity> {
    @Override
    public ResourceLocation getModelResource(AbyssalEggBlockEntity egg) {
        return AbyssalPlanet.id("geo/block/abyssal_egg.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(AbyssalEggBlockEntity egg) {
        return AbyssalPlanet.id("textures/block/abyssal_egg.png");
    }

    @Override
    public ResourceLocation getAnimationResource(AbyssalEggBlockEntity egg) {
        return AbyssalPlanet.id("animations/block/abyssal_egg.animation.json");
    }
}
