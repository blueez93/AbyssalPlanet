package fr.blue.abyssalplanet.client;

import fr.blue.abyssalplanet.AbyssalPlanet;
import fr.blue.abyssalplanet.entity.KrakenBossEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class KrakenBossModel extends GeoModel<KrakenBossEntity> {
    @Override
    public ResourceLocation getModelResource(KrakenBossEntity entity) {
        return AbyssalPlanet.id("geo/entity/kraken_boss.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(KrakenBossEntity entity) {
        return AbyssalPlanet.id("textures/entity/kraken_boss.png");
    }

    @Override
    public ResourceLocation getAnimationResource(KrakenBossEntity entity) {
        return AbyssalPlanet.id("animations/entity/kraken_boss.animation.json");
    }
}
