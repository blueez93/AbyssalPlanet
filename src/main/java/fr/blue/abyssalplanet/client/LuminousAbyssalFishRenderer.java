package fr.blue.abyssalplanet.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import fr.blue.abyssalplanet.AbyssalPlanet;
import fr.blue.abyssalplanet.entity.LuminousAbyssalFishEntity;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class LuminousAbyssalFishRenderer extends MobRenderer<LuminousAbyssalFishEntity, LuminousAbyssalFishModel> {
    private static final ResourceLocation TEXTURE = AbyssalPlanet.id("textures/entity/luminous_abyssal_fish.png");
    private static final RenderType GLOW = RenderType.eyes(
            AbyssalPlanet.id("textures/entity/luminous_abyssal_fish_glow.png")
    );

    public LuminousAbyssalFishRenderer(EntityRendererProvider.Context context) {
        super(context, new LuminousAbyssalFishModel(context.bakeLayer(LuminousAbyssalFishModel.LAYER_LOCATION)), 0.3F);
        this.addLayer(new EyesLayer<>(this) {
            @Override
            public RenderType renderType() {
                return GLOW;
            }
        });
    }

    @Override
    public ResourceLocation getTextureLocation(LuminousAbyssalFishEntity entity) {
        return TEXTURE;
    }

    @Override
    protected int getBlockLightLevel(LuminousAbyssalFishEntity entity, BlockPos pos) {
        return Math.max(5, super.getBlockLightLevel(entity, pos));
    }

    @Override
    protected void setupRotations(LuminousAbyssalFishEntity entity, PoseStack poseStack, float ageInTicks, float rotationYaw, float partialTick) {
        super.setupRotations(entity, poseStack, ageInTicks, rotationYaw, partialTick);

        if (!entity.isInWater()) {
            poseStack.translate(0.1F, 0.1F, -0.1F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
            return;
        }

        float divePitch = Mth.clamp((float) (-entity.getDeltaMovement().y * 32.0D), -11.0F, 11.0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(divePitch));
    }

    @Override
    protected void scale(LuminousAbyssalFishEntity entity, PoseStack poseStack, float partialTick) {
        float individualScale = 0.88F + (entity.getId() & 3) * 0.025F;
        poseStack.scale(individualScale, individualScale, individualScale);
    }
}
