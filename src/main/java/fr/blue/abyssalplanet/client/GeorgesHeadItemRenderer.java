package fr.blue.abyssalplanet.client;

import fr.blue.abyssalplanet.item.GeorgesHeadItem;
import software.bernie.geckolib.renderer.GeoItemRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public final class GeorgesHeadItemRenderer extends GeoItemRenderer<GeorgesHeadItem> {
    public GeorgesHeadItemRenderer() {
        super(new GeorgesHeadItemModel());
        withScale(0.72F);
        useAlternateGuiLighting();
        addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }
}
