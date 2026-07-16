package fr.blue.abyssalplanet.client;

import fr.blue.abyssalplanet.entity.AbyssalShrimpEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class AbyssalShrimpRenderer extends GeoEntityRenderer<AbyssalShrimpEntity> {
    public AbyssalShrimpRenderer(EntityRendererProvider.Context context) {
        super(context, new AbyssalShrimpModel());
        shadowRadius = 0.22F;
    }
}
