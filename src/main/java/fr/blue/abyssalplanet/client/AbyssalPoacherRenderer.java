package fr.blue.abyssalplanet.client;

import com.mojang.blaze3d.vertex.PoseStack;
import fr.blue.abyssalplanet.AbyssalPlanet;
import fr.blue.abyssalplanet.entity.AbyssalPoacherEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.layers.HumanoidArmorLayer;
import net.minecraft.resources.ResourceLocation;

public final class AbyssalPoacherRenderer
        extends HumanoidMobRenderer<AbyssalPoacherEntity, AbyssalPoacherModel> {
    private static final ResourceLocation[] RAGGED_TEXTURES = {
            AbyssalPlanet.id("textures/entity/abyssal_poacher_ragged.png"),
            AbyssalPlanet.id("textures/entity/abyssal_poacher_ragged_scaled.png"),
            AbyssalPlanet.id("textures/entity/abyssal_poacher_ragged_scarred.png")
    };
    private static final ResourceLocation INTACT_TEXTURE =
            AbyssalPlanet.id("textures/entity/abyssal_poacher_intact.png");

    public AbyssalPoacherRenderer(EntityRendererProvider.Context context) {
        super(context, new AbyssalPoacherModel(context.bakeLayer(AbyssalPoacherModel.LAYER_LOCATION)), 0.54F);
        this.addLayer(new HumanoidArmorLayer<>(
                this,
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_INNER_ARMOR)),
                new HumanoidModel<>(context.bakeLayer(ModelLayers.PLAYER_OUTER_ARMOR)),
                context.getModelManager()
        ));
    }

    @Override
    public ResourceLocation getTextureLocation(AbyssalPoacherEntity entity) {
        return entity.hasIntactClothes()
                ? INTACT_TEXTURE
                : RAGGED_TEXTURES[Math.floorMod(entity.getAppearanceVariant(), RAGGED_TEXTURES.length)];
    }

    @Override
    protected void scale(AbyssalPoacherEntity entity, PoseStack poseStack, float partialTick) {
        poseStack.scale(1.06F, 1.06F, 1.06F);
    }
}
