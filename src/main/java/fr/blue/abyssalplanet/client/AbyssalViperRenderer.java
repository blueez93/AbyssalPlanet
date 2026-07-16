package fr.blue.abyssalplanet.client;

import com.mojang.blaze3d.vertex.PoseStack;
import fr.blue.abyssalplanet.AbyssalPlanet;
import fr.blue.abyssalplanet.entity.AbyssalViperEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public class AbyssalViperRenderer extends MobRenderer<AbyssalViperEntity, AbyssalViperModel> {
    private static final ResourceLocation TEXTURE = AbyssalPlanet.id("textures/entity/abyssal_viper.png");

    public AbyssalViperRenderer(EntityRendererProvider.Context context) {
        super(context, new AbyssalViperModel(context.bakeLayer(AbyssalViperModel.LAYER_LOCATION)), 0.85F);
    }

    @Override
    public ResourceLocation getTextureLocation(AbyssalViperEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(AbyssalViperEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(1.2F, 1.2F, 1.2F);
    }

    @Override
    protected int getBlockLightLevel(AbyssalViperEntity entity, BlockPos position) {
        return Math.max(6, super.getBlockLightLevel(entity, position));
    }
}
