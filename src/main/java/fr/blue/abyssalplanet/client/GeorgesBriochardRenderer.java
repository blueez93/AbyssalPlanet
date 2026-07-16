package fr.blue.abyssalplanet.client;

import com.mojang.blaze3d.vertex.PoseStack;
import fr.blue.abyssalplanet.entity.GeorgesBriochardEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public final class GeorgesBriochardRenderer extends GeoEntityRenderer<GeorgesBriochardEntity> {
    private static final float VISUAL_SCALE = 1.38F;

    public GeorgesBriochardRenderer(EntityRendererProvider.Context context) {
        super(context, new GeorgesBriochardModel());
        this.shadowRadius = 1.35F;
        this.addRenderLayer(new AutoGlowingGeoLayer<>(this));
    }

    @Override
    public void render(
            GeorgesBriochardEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        poseStack.pushPose();
        poseStack.scale(VISUAL_SCALE, VISUAL_SCALE, VISUAL_SCALE);
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }
}
