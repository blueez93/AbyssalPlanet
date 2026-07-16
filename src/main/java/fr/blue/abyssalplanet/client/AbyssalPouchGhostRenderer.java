package fr.blue.abyssalplanet.client;

import com.mojang.blaze3d.vertex.PoseStack;
import fr.blue.abyssalplanet.entity.AbyssalPouchGhostEntity;
import fr.blue.abyssalplanet.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

public class AbyssalPouchGhostRenderer extends EntityRenderer<AbyssalPouchGhostEntity> {
    private static final ItemStack POUCH_STACK = new ItemStack(ModItems.ABYSSAL_POUCH.get());

    public AbyssalPouchGhostRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.15F;
    }

    @Override
    public void render(
            AbyssalPouchGhostEntity entity,
            float yaw,
            float partialTick,
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight
    ) {
        poseStack.pushPose();
        float bob = Mth.sin((entity.tickCount + partialTick) * 0.13F) * 0.05F;
        poseStack.translate(0.0D, 0.32D + bob, 0.0D);
        poseStack.mulPose(this.entityRenderDispatcher.cameraOrientation());
        poseStack.scale(0.82F, 0.82F, 0.82F);
        Minecraft.getInstance().getItemRenderer().renderStatic(
                POUCH_STACK,
                ItemDisplayContext.GROUND,
                packedLight,
                OverlayTexture.NO_OVERLAY,
                poseStack,
                buffer,
                entity.level(),
                entity.getId()
        );
        poseStack.popPose();
        super.render(entity, yaw, partialTick, poseStack, buffer, packedLight);
    }

    @Override
    public ResourceLocation getTextureLocation(AbyssalPouchGhostEntity entity) {
        return TextureAtlas.LOCATION_BLOCKS;
    }
}
