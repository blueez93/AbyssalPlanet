package fr.blue.abyssalplanet.client;

import com.mojang.blaze3d.vertex.PoseStack;
import fr.blue.abyssalplanet.AbyssalPlanet;
import fr.blue.abyssalplanet.entity.AbyssalWandererEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

public final class AbyssalWandererRenderer
        extends HumanoidMobRenderer<AbyssalWandererEntity, AbyssalWandererModel> {
    private static final ResourceLocation TEXTURE =
            AbyssalPlanet.id("textures/entity/abyssal_poacher_intact.png");

    public AbyssalWandererRenderer(EntityRendererProvider.Context context) {
        super(context, new AbyssalWandererModel(context.bakeLayer(AbyssalWandererModel.LAYER_LOCATION)), 0.68F);
        this.addLayer(new HumanoidArmorLayer<>(
                this,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()
        ));
    }

    @Override
    public ResourceLocation getTextureLocation(AbyssalWandererEntity entity) {
        return TEXTURE;
    }

    @Override
    protected void scale(AbyssalWandererEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(1.22F, 1.22F, 1.22F);
    }
}
