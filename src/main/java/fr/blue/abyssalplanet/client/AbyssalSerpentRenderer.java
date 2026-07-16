package fr.blue.abyssalplanet.client;

import com.mojang.blaze3d.vertex.PoseStack;
import fr.blue.abyssalplanet.AbyssalPlanet;
import fr.blue.abyssalplanet.entity.AbyssalSerpentEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public class AbyssalSerpentRenderer extends MobRenderer<AbyssalSerpentEntity, AbyssalSerpentModel<AbyssalSerpentEntity>> {
    private static final ResourceLocation TEXTURE = AbyssalPlanet.id("textures/entity/abyssal_serpent.png");

    public AbyssalSerpentRenderer(EntityRendererProvider.Context context) {
        super(context, new AbyssalSerpentModel<>(context.bakeLayer(AbyssalSerpentModel.LAYER_LOCATION)), 2.8F);
    }

    @Override
    public ResourceLocation getTextureLocation(AbyssalSerpentEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(AbyssalSerpentEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(4.2F, 4.2F, 4.2F);
    }

    @Override
    protected int getBlockLightLevel(AbyssalSerpentEntity entity, BlockPos position) {
        return Math.max(9, super.getBlockLightLevel(entity, position));
    }
}
