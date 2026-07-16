package fr.blue.abyssalplanet.client;

import fr.blue.abyssalplanet.AbyssalPlanet;
import fr.blue.abyssalplanet.entity.GeorgesSeniorEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class GeorgesSeniorModel extends GeoModel<GeorgesSeniorEntity> {
    @Override
    public ResourceLocation getModelResource(GeorgesSeniorEntity entity) {
        return AbyssalPlanet.id("geo/entity/georges_briochard.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GeorgesSeniorEntity entity) {
        return AbyssalPlanet.id("textures/entity/georges_briochard.png");
    }

    @Override
    public ResourceLocation getAnimationResource(GeorgesSeniorEntity entity) {
        return AbyssalPlanet.id("animations/entity/georges_briochard.animation.json");
    }
}
