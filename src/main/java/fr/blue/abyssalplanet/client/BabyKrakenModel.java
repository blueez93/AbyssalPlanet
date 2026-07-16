package fr.blue.abyssalplanet.client;

import fr.blue.abyssalplanet.AbyssalPlanet;
import fr.blue.abyssalplanet.entity.BabyKrakenEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class BabyKrakenModel extends GeoModel<BabyKrakenEntity> {
    @Override
    public ResourceLocation getModelResource(BabyKrakenEntity entity) {
        return AbyssalPlanet.id("geo/entity/baby_kraken.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(BabyKrakenEntity entity) {
        return AbyssalPlanet.id("textures/entity/baby_kraken.png");
    }

    @Override
    public ResourceLocation getAnimationResource(BabyKrakenEntity entity) {
        return AbyssalPlanet.id("animations/entity/baby_kraken.animation.json");
    }
}
