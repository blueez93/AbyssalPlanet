package fr.blue.abyssalplanet.client;

import fr.blue.abyssalplanet.AbyssalPlanet;
import fr.blue.abyssalplanet.entity.GeorgesBriochardEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class GeorgesBriochardModel extends GeoModel<GeorgesBriochardEntity> {
    @Override
    public ResourceLocation getModelResource(GeorgesBriochardEntity entity) {
        return AbyssalPlanet.id("geo/entity/georges_briochard.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GeorgesBriochardEntity entity) {
        return AbyssalPlanet.id("textures/entity/georges_briochard.png");
    }

    @Override
    public ResourceLocation getAnimationResource(GeorgesBriochardEntity entity) {
        return AbyssalPlanet.id("animations/entity/georges_briochard.animation.json");
    }
}
