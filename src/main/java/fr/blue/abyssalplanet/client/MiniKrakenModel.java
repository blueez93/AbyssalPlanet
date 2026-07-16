package fr.blue.abyssalplanet.client;

import fr.blue.abyssalplanet.AbyssalPlanet;
import fr.blue.abyssalplanet.entity.MiniKrakenEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class MiniKrakenModel extends GeoModel<MiniKrakenEntity> {
    @Override
    public ResourceLocation getModelResource(MiniKrakenEntity entity) {
        return AbyssalPlanet.id("geo/entity/mini_kraken.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(MiniKrakenEntity entity) {
        return AbyssalPlanet.id("textures/entity/mini_kraken.png");
    }

    @Override
    public ResourceLocation getAnimationResource(MiniKrakenEntity entity) {
        return AbyssalPlanet.id("animations/entity/mini_kraken.animation.json");
    }
}
