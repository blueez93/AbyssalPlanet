package fr.blue.abyssalplanet.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import fr.blue.abyssalplanet.AbyssalPlanet;
import fr.blue.abyssalplanet.entity.AbyssalBlueMoonfishEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

public class AbyssalBlueMoonfishRenderer extends MobRenderer<AbyssalBlueMoonfishEntity, AbyssalBlueMoonfishModel<AbyssalBlueMoonfishEntity>> {
    private static final ResourceLocation[] TEXTURES = new ResourceLocation[]{
            AbyssalPlanet.id("textures/entity/abyssal_blue_moonfish_model_0.png"),
            AbyssalPlanet.id("textures/entity/abyssal_blue_moonfish_model_1.png"),
            AbyssalPlanet.id("textures/entity/abyssal_blue_moonfish_model_2.png")
    };

    public AbyssalBlueMoonfishRenderer(EntityRendererProvider.Context context) {
        super(
                context,
                new AbyssalBlueMoonfishModel<>(context.bakeLayer(AbyssalBlueMoonfishModel.LAYER_LOCATION)),
                0.36F
        );
    }

    @Override
    public ResourceLocation getTextureLocation(AbyssalBlueMoonfishEntity entity) {
        return TEXTURES[Math.max(0, Math.min(TEXTURES.length - 1, entity.getVariant()))];
    }

    @Override
    protected void scale(AbyssalBlueMoonfishEntity entity, PoseStack poseStack, float partialTick) {
        float scale = switch (entity.getVariant()) {
            case 1 -> 1.18F;
            case 2 -> 1.08F;
            default -> 0.98F;
        };
        poseStack.scale(scale, scale, scale);
    }

    @Override
    protected void setupRotations(
            AbyssalBlueMoonfishEntity entity,
            PoseStack poseStack,
            float ageInTicks,
            float rotationYaw,
            float partialTick
    ) {
        super.setupRotations(entity, poseStack, ageInTicks, rotationYaw, partialTick);
        float wiggle = Mth.sin(ageInTicks * 0.45F) * 4.0F;
        poseStack.mulPose(Axis.YP.rotationDegrees(wiggle));

        if (!entity.isInWater()) {
            poseStack.translate(0.1F, 0.1F, -0.1F);
            poseStack.mulPose(Axis.ZP.rotationDegrees(90.0F));
        }
    }
}
