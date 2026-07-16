package fr.blue.abyssalplanet.client;

import fr.blue.abyssalplanet.block.entity.AbyssalEggBlockEntity;
import software.bernie.geckolib.renderer.GeoBlockRenderer;

public final class AbyssalEggRenderer extends GeoBlockRenderer<AbyssalEggBlockEntity> {
    public AbyssalEggRenderer() {
        super(new AbyssalEggModel());
        withScale(1.0F);
    }
}
