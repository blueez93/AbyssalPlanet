package fr.blue.abyssalplanet.client;

import fr.blue.abyssalplanet.AbyssalPlanet;
import fr.blue.abyssalplanet.entity.AbyssalShrimpEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public final class AbyssalShrimpModel extends GeoModel<AbyssalShrimpEntity> {
    @Override
    public ResourceLocation getModelResource(AbyssalShrimpEntity entity) {
        return AbyssalPlanet.id("geo/entity/abyssal_shrimp.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(AbyssalShrimpEntity entity) {
        return AbyssalPlanet.id("textures/entity/abyssal_shrimp.png");
    }

    @Override
    public ResourceLocation getAnimationResource(AbyssalShrimpEntity entity) {
        return AbyssalPlanet.id("animations/entity/abyssal_shrimp.animation.json");
    }
}
