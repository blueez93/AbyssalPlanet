package fr.blue.abyssalplanet.client;

import fr.blue.abyssalplanet.AbyssalPlanet;
import fr.blue.abyssalplanet.item.GeorgesHeadItem;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class GeorgesHeadItemModel extends GeoModel<GeorgesHeadItem> {
    @Override
    public ResourceLocation getModelResource(GeorgesHeadItem head) {
        return AbyssalPlanet.id("geo/block/georges_head.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(GeorgesHeadItem head) {
        return AbyssalPlanet.id("textures/entity/georges_briochard.png");
    }

    @Override
    public ResourceLocation getAnimationResource(GeorgesHeadItem head) {
        return AbyssalPlanet.id("animations/entity/georges_briochard.animation.json");
    }
}
