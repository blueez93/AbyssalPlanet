package fr.blue.abyssalplanet.client;

import fr.blue.abyssalplanet.AbyssalPlanet;
import fr.blue.abyssalplanet.block.entity.GeorgesHeadBlockEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class GeorgesHeadBlockModel extends GeoModel<GeorgesHeadBlockEntity> {
    @Override
    public ResourceLocation getModelResource(GeorgesHeadBlockEntity head) {
        return AbyssalPlanet.id("geo/block/georges_head.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GeorgesHeadBlockEntity head) {
        return AbyssalPlanet.id("textures/entity/georges_briochard.png");
    }

    @Override
    public ResourceLocation getAnimationResource(GeorgesHeadBlockEntity head) {
        return AbyssalPlanet.id("animations/entity/georges_briochard.animation.json");
    }
}
