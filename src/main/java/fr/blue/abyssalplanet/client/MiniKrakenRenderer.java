package fr.blue.abyssalplanet.client;

import fr.blue.abyssalplanet.entity.MiniKrakenEntity;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;

public class MiniKrakenRenderer extends GeoEntityRenderer<MiniKrakenEntity> {
    public MiniKrakenRenderer(EntityRendererProvider.Context context) {
        super(context, new MiniKrakenModel());
        this.shadowRadius = 1.4F;
    }
}
