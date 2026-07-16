package fr.blue.abyssalplanet.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fr.blue.abyssalplanet.entity.BabyKrakenEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.core.object.Color;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.AutoGlowingGeoLayer;

public class BabyKrakenRenderer extends GeoEntityRenderer<BabyKrakenEntity> {
    private static final float VISUAL_SCALE = 0.72F;

    public BabyKrakenRenderer(EntityRendererProvider.Context context) {
        super(context, new BabyKrakenModel());
        this.shadowRadius = 0.32F;
        this.addRenderLayer(new AutoGlowingGeoLayer<>(this) {
            @Override
            public void render(
                    PoseStack poseStack,
                    BabyKrakenEntity entity,
                    BakedGeoModel bakedModel,
                    RenderType renderType,
                    MultiBufferSource bufferSource,
                    VertexConsumer buffer,
                    float partialTick,
                    int packedLight,
                    int packedOverlay
            ) {
                if (!entity.isCamouflaged()) {
                    super.render(
                            poseStack,
                            entity,
                            bakedModel,
                            renderType,
                            bufferSource,
                            buffer,
                            partialTick,
                            packedLight,
                            packedOverlay
                    );
                }
            }
        });
    }

    @Override
    public RenderType getRenderType(
            BabyKrakenEntity entity,
            ResourceLocation texture,
            MultiBufferSource bufferSource,
            float partialTick
    ) {
        return RenderType.entityTranslucent(texture);
    }

    @Override
    public Color getRenderColor(BabyKrakenEntity entity, float partialTick, int packedLight) {
        if (!entity.isCamouflaged()) {
            return Color.WHITE;
        }

        float shimmer = 0.23F + (float) Math.sin((entity.tickCount + partialTick) * 0.35F) * 0.045F;
        return Color.ofRGBA(0.72F, 0.86F, 1.0F, shimmer);
    }

    @Override
    public void render(
            BabyKrakenEntity entity,
            float entityYaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource bufferSource,
            int packedLight
    ) {
        poseStack.pushPose();
        poseStack.scale(VISUAL_SCALE, VISUAL_SCALE, VISUAL_SCALE);

        if (entity.isCamouflaged()) {
            float wave = (float) Math.sin((entity.tickCount + partialTick) * 0.42F);
            poseStack.translate(wave * 0.012F, 0.0F, -wave * 0.009F);
            poseStack.scale(1.0F + wave * 0.018F, 1.0F - wave * 0.012F, 1.0F + wave * 0.014F);
        }

        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
        poseStack.popPose();
    }
}
