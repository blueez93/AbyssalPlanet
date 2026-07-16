package fr.blue.abyssalplanet.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import fr.blue.abyssalplanet.AbyssalPlanet;
import fr.blue.abyssalplanet.entity.AbyssalPortalEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class AbyssalPortalRenderer extends EntityRenderer<AbyssalPortalEntity> {
    private static final ResourceLocation TEXTURE = AbyssalPlanet.id("textures/entity/abyssal_portal.png");

    public AbyssalPortalRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(AbyssalPortalEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        float radius = entity.getRenderRadius();

        poseStack.pushPose();
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.mulPose(Axis.YP.rotationDegrees(180.0F));
        renderDisc(poseStack, bufferSource.getBuffer(RenderType.entityTranslucent(TEXTURE)), packedLight, radius);
        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(AbyssalPortalEntity entity) {
        return TEXTURE;
    }

    private static void renderDisc(PoseStack poseStack, VertexConsumer consumer, int packedLight, float radius) {
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();

        vertex(consumer, matrix, normal, -radius, -radius, 0.0F, 0.0F, 1.0F, packedLight);
        vertex(consumer, matrix, normal, radius, -radius, 0.0F, 1.0F, 1.0F, packedLight);
        vertex(consumer, matrix, normal, radius, radius, 0.0F, 1.0F, 0.0F, packedLight);
        vertex(consumer, matrix, normal, -radius, radius, 0.0F, 0.0F, 0.0F, packedLight);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, float x, float y, float z, float u, float v, int packedLight) {
        consumer.vertex(matrix, x, y, z)
                .color(255, 255, 255, 230)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(normal, 0.0F, 1.0F, 0.0F)
                .endVertex();
    }
}
