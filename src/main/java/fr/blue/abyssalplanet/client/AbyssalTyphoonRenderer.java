package fr.blue.abyssalplanet.client;

import fr.blue.abyssalplanet.entity.AbyssalTyphoonEntity;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;

public class AbyssalTyphoonRenderer extends EntityRenderer<AbyssalTyphoonEntity> {
    public AbyssalTyphoonRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public ResourceLocation getTextureLocation(AbyssalTyphoonEntity entity) {
        return new ResourceLocation("abyssalplanet", "textures/entity/abyssal_typhoon.png");
    }
}