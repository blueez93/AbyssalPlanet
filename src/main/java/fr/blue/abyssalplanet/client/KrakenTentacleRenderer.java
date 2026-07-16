package fr.blue.abyssalplanet.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import fr.blue.abyssalplanet.AbyssalPlanet;
import fr.blue.abyssalplanet.entity.KrakenTentacleEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class KrakenTentacleRenderer extends EntityRenderer<KrakenTentacleEntity> {
    private static final ResourceLocation TEXTURE = AbyssalPlanet.id("textures/entity/kraken_tentacle.png");

    public KrakenTentacleRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public void render(KrakenTentacleEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        float length = entity.getRenderLength();

        if (length <= 0.1F) {
            return;
        }

        Vec3 direction = entity.getRenderDirection();
        float yaw = (float) (Mth.atan2(direction.x, direction.z) * Mth.RAD_TO_DEG);
        float pitch = (float) (-Math.asin(direction.y) * Mth.RAD_TO_DEG);
        float radius = entity.getRenderWidth() * 0.5F;

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        renderCurvedTentacle(
                entity,
                poseStack,
                bufferSource.getBuffer(RenderType.entityTranslucent(TEXTURE)),
                packedLight,
                radius,
                length,
                entity.tickCount + partialTick
        );
        poseStack.popPose();

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(KrakenTentacleEntity entity) {
        return TEXTURE;
    }

    private static void renderCurvedTentacle(
            KrakenTentacleEntity entity,
            PoseStack poseStack,
            VertexConsumer consumer,
            int packedLight,
            float baseRadius,
            float length,
            float ageInTicks
    ) {
        int segmentCount = Mth.clamp(Mth.ceil(length / 1.65F), 6, 24);
        Vec3 previous = Vec3.ZERO;

        for (int segment = 1; segment <= segmentCount; segment++) {
            float progress = segment / (float) segmentCount;
            float envelope = Mth.sin(progress * Mth.PI);
            float wavePhase = ageInTicks * 0.23F + progress * 8.5F + entity.getId() * 0.37F;
            double waveX = Mth.sin(wavePhase) * baseRadius * 1.55F * envelope;
            double waveY = Mth.cos(wavePhase * 0.83F) * baseRadius * 0.85F * envelope;
            Vec3 current = new Vec3(waveX, waveY, length * progress);
            float midpointProgress = (segment - 0.5F) / segmentCount;
            float radius = Math.max(baseRadius * 0.28F, baseRadius * (1.0F - midpointProgress * 0.68F));
            float textureStart = length * (segment - 1.0F) / segmentCount / 4.5F;
            float textureEnd = length * segment / segmentCount / 4.5F;

            renderSegment(poseStack, consumer, packedLight, previous, current, radius, textureStart, textureEnd);
            previous = current;
        }
    }

    private static void renderSegment(
            PoseStack poseStack,
            VertexConsumer consumer,
            int packedLight,
            Vec3 start,
            Vec3 end,
            float radius,
            float textureStart,
            float textureEnd
    ) {
        Vec3 segmentDirection = end.subtract(start);
        float segmentLength = (float) segmentDirection.length();

        if (segmentLength <= 0.001F) {
            return;
        }

        float yaw = (float) (Mth.atan2(segmentDirection.x, segmentDirection.z) * Mth.RAD_TO_DEG);
        float pitch = (float) (-Math.asin(segmentDirection.y / segmentLength) * Mth.RAD_TO_DEG);

        poseStack.pushPose();
        poseStack.translate(start.x, start.y, start.z);
        poseStack.mulPose(Axis.YP.rotationDegrees(yaw));
        poseStack.mulPose(Axis.XP.rotationDegrees(pitch));
        renderTentacleBox(poseStack, consumer, packedLight, radius, segmentLength, textureStart, textureEnd);
        poseStack.popPose();
    }

    private static void renderTentacleBox(
            PoseStack poseStack,
            VertexConsumer consumer,
            int packedLight,
            float radius,
            float length,
            float textureStart,
            float textureEnd
    ) {
        PoseStack.Pose pose = poseStack.last();
        Matrix4f matrix = pose.pose();
        Matrix3f normal = pose.normal();

        vertex(consumer, matrix, normal, -radius, -radius, 0.0F, 0.00F, textureStart, packedLight, 0.0F, -1.0F, 0.0F);
        vertex(consumer, matrix, normal, -radius, -radius, length, 0.00F, textureEnd, packedLight, 0.0F, -1.0F, 0.0F);
        vertex(consumer, matrix, normal, radius, -radius, length, 0.25F, textureEnd, packedLight, 0.0F, -1.0F, 0.0F);
        vertex(consumer, matrix, normal, radius, -radius, 0.0F, 0.25F, textureStart, packedLight, 0.0F, -1.0F, 0.0F);

        vertex(consumer, matrix, normal, radius, radius, 0.0F, 0.25F, textureStart, packedLight, 0.0F, 1.0F, 0.0F);
        vertex(consumer, matrix, normal, radius, radius, length, 0.25F, textureEnd, packedLight, 0.0F, 1.0F, 0.0F);
        vertex(consumer, matrix, normal, -radius, radius, length, 0.50F, textureEnd, packedLight, 0.0F, 1.0F, 0.0F);
        vertex(consumer, matrix, normal, -radius, radius, 0.0F, 0.50F, textureStart, packedLight, 0.0F, 1.0F, 0.0F);

        vertex(consumer, matrix, normal, -radius, radius, 0.0F, 0.50F, textureStart, packedLight, -1.0F, 0.0F, 0.0F);
        vertex(consumer, matrix, normal, -radius, radius, length, 0.50F, textureEnd, packedLight, -1.0F, 0.0F, 0.0F);
        vertex(consumer, matrix, normal, -radius, -radius, length, 0.75F, textureEnd, packedLight, -1.0F, 0.0F, 0.0F);
        vertex(consumer, matrix, normal, -radius, -radius, 0.0F, 0.75F, textureStart, packedLight, -1.0F, 0.0F, 0.0F);

        vertex(consumer, matrix, normal, radius, -radius, 0.0F, 0.75F, textureStart, packedLight, 1.0F, 0.0F, 0.0F);
        vertex(consumer, matrix, normal, radius, -radius, length, 0.75F, textureEnd, packedLight, 1.0F, 0.0F, 0.0F);
        vertex(consumer, matrix, normal, radius, radius, length, 1.00F, textureEnd, packedLight, 1.0F, 0.0F, 0.0F);
        vertex(consumer, matrix, normal, radius, radius, 0.0F, 1.00F, textureStart, packedLight, 1.0F, 0.0F, 0.0F);
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Matrix3f normal, float x, float y, float z, float u, float v, int packedLight, float normalX, float normalY, float normalZ) {
        consumer.vertex(matrix, x, y, z)
                .color(255, 255, 255, 248)
                .uv(u, v)
                .overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(packedLight)
                .normal(normal, normalX, normalY, normalZ)
                .endVertex();
    }
}
