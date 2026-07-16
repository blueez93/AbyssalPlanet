package fr.blue.abyssalplanet.client;

import com.mojang.blaze3d.vertex.PoseStack;
import fr.blue.abyssalplanet.AbyssalPlanet;
import fr.blue.abyssalplanet.entity.AbyssalOctopusEntity;
import net.minecraft.client.model.SquidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.SquidRenderer;
import net.minecraft.resources.ResourceLocation;

public class AbyssalOctopusRenderer extends SquidRenderer<AbyssalOctopusEntity> {
    private static final ResourceLocation TEXTURE = AbyssalPlanet.id("textures/entity/abyssal_octopus.png");

    public AbyssalOctopusRenderer(EntityRendererProvider.Context context) {
        super(context, new SquidModel<>(context.bakeLayer(ModelLayers.SQUID)));
    }

    @Override
    public ResourceLocation getTextureLocation(AbyssalOctopusEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(AbyssalOctopusEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(1.25F, 1.25F, 1.25F);
    }
}
