package fr.blue.abyssalplanet.client;

import com.mojang.blaze3d.vertex.PoseStack;
import fr.blue.abyssalplanet.entity.KrakenBossEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class KrakenBossRenderer extends GeoEntityRenderer<KrakenBossEntity> {
    private static final float VISUAL_SCALE = 2.8F;

    public KrakenBossRenderer(EntityRendererProvider.Context context) {
        super(context, new KrakenBossModel());
        this.shadowRadius = 11.0F;
        this.addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override
    public void render(KrakenBossEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        poseStack.pushPose();
        poseStack.scale(VISUAL_SCALE, VISUAL_SCALE, VISUAL_SCALE);
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }
}
