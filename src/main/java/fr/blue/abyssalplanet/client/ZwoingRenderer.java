package fr.blue.abyssalplanet.client;

import com.mojang.blaze3d.vertex.PoseStack;
import fr.blue.abyssalplanet.AbyssalPlanet;
import fr.blue.abyssalplanet.entity.ZwoingEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class ZwoingRenderer extends MobRenderer<ZwoingEntity, ZwoingModel> {
    private static final ResourceLocation BLUE_TEXTURE = AbyssalPlanet.id("textures/entity/zwoing_blue.png");
    private static final ResourceLocation RED_TEXTURE = AbyssalPlanet.id("textures/entity/zwoing_red.png");
    private static final ResourceLocation GREEN_TEXTURE = AbyssalPlanet.id("textures/entity/zwoing_green.png");
    private static final ResourceLocation PURPLE_TEXTURE = AbyssalPlanet.id("textures/entity/zwoing_purple.png");
    private static final ResourceLocation PURPLE_BRIGHT_TEXTURE =
            AbyssalPlanet.id("textures/entity/zwoing_purple_bright.png");
    private static final ResourceLocation MULTICOLOR_TEXTURE = AbyssalPlanet.id("textures/entity/zwoing_multicolor.png");

    public ZwoingRenderer(EntityRendererProvider.Context context) {
        super(context, new ZwoingModel(context.bakeLayer(ZwoingModel.LAYER_LOCATION)), 0.24F);
    }

    @Override
    public ResourceLocation getTextureLocation(ZwoingEntity entity) {
        return switch (entity.getVariant()) {
            case ZwoingEntity.RED -> RED_TEXTURE;
            case ZwoingEntity.GREEN -> GREEN_TEXTURE;
            case ZwoingEntity.PURPLE ->
                    entity.isPurpleBlinkingBright() ? PURPLE_BRIGHT_TEXTURE : PURPLE_TEXTURE;
            case ZwoingEntity.MULTICOLOR -> MULTICOLOR_TEXTURE;
            default -> BLUE_TEXTURE;
        };
    }

    @Override
    protected void scale(ZwoingEntity entity, PoseStack poseStack, float partialTick) {
        float deformation = Mth.clamp(entity.getSquish(partialTick) * 0.48F, -0.24F, 0.30F);
        float horizontalScale = 1.0F / (1.0F + deformation);
        float verticalScale = 1.0F + deformation;
        poseStack.scale(0.72F * horizontalScale, 0.72F * verticalScale, 0.72F * horizontalScale);
    }

    @Override
    protected int getBlockLightLevel(ZwoingEntity entity, BlockPos position) {
        if (entity.isPurpleBlinkingBright()) {
            return 15;
        }
        return super.getBlockLightLevel(entity, position);
    }
}
