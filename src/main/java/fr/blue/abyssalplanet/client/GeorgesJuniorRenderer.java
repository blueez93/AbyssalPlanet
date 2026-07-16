package fr.blue.abyssalplanet.client;

import com.mojang.blaze3d.vertex.PoseStack;
import fr.blue.abyssalplanet.entity.GeorgesJuniorEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public final class GeorgesJuniorRenderer extends GeoEntityRenderer<GeorgesJuniorEntity> {
    public GeorgesJuniorRenderer(EntityRendererProvider.Context context) {
        super(context, new GeorgesJuniorModel());
        shadowRadius = 0.35F;
    }

    @Override
    public void render(
            GeorgesJuniorEntity entity,
            float yaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        poseStack.pushPose();
        poseStack.scale(0.42F, 0.42F, 0.42F);
        super.render(entity, yaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }
}
