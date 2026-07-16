package fr.blue.abyssalplanet.client;

import com.mojang.blaze3d.vertex.PoseStack;
import fr.blue.abyssalplanet.entity.GeorgesSeniorEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class GeorgesSeniorRenderer extends GeoEntityRenderer<GeorgesSeniorEntity> {
    public GeorgesSeniorRenderer(EntityRendererProvider.Context context) {
        super(context, new GeorgesSeniorModel());
        shadowRadius = 1.35F;
    }

    @Override
    public void render(
            GeorgesSeniorEntity entity,
            float yaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        poseStack.pushPose();
        poseStack.scale(1.38F, 1.38F, 1.38F);
        super.render(entity, yaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }
}
