package fr.blue.abyssalplanet.client;

import fr.blue.abyssalplanet.AbyssalPlanet;
import fr.blue.abyssalplanet.entity.GeorgesJuniorEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class GeorgesJuniorModel extends GeoModel<GeorgesJuniorEntity> {
    @Override
    public ResourceLocation getModelResource(GeorgesJuniorEntity entity) {
        return AbyssalPlanet.id("geo/entity/georges_briochard.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GeorgesJuniorEntity entity) {
        return AbyssalPlanet.id("textures/entity/georges_briochard.png");
    }

    @Override
    public ResourceLocation getAnimationResource(GeorgesJuniorEntity entity) {
        return AbyssalPlanet.id("animations/entity/georges_briochard.animation.json");
    }
}
