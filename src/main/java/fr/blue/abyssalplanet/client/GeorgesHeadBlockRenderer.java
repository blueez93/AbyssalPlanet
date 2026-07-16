package fr.blue.abyssalplanet.client;

import fr.blue.abyssalplanet.block.entity.GeorgesHeadBlockEntity;
import software.bernie.geckolib.renderer.GeoBlockRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public final class GeorgesHeadBlockRenderer extends GeoBlockRenderer<GeorgesHeadBlockEntity> {
    public GeorgesHeadBlockRenderer() {
        super(new GeorgesHeadBlockModel());
        withScale(0.75F);
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }
}
