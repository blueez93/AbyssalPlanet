package fr.blue.abyssalplanet.fluid;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.fluids.FluidType;
import org.jetbrains.annotations.NotNull;
import org.joml.Vector3f;

import java.util.function.Consumer;

public final class AbyssalWaterFluidType extends FluidType {
    private static final ResourceLocation STILL_TEXTURE =
            new ResourceLocation("minecraft", "block/water_still");
    private static final ResourceLocation FLOWING_TEXTURE =
            new ResourceLocation("minecraft", "block/water_flow");
    private static final ResourceLocation OVERLAY_TEXTURE =
            new ResourceLocation("minecraft", "block/water_overlay");
    private static final ResourceLocation UNDERWATER_TEXTURE =
            new ResourceLocation("minecraft", "textures/misc/underwater.png");
    private static final int WATER_TINT = 0xFFB21C35;

    public AbyssalWaterFluidType() {
        super(FluidType.Properties.create()
                .descriptionId("block.abyssalplanet.abyssal_water")
                .canConvertToSource(true)
                .canExtinguish(true)
                .supportsBoating(true)
                .canHydrate(true)
                .sound(SoundActions.BUCKET_FILL, SoundEvents.BUCKET_FILL)
                .sound(SoundActions.BUCKET_EMPTY, SoundEvents.BUCKET_EMPTY));
    }

    @Override
    public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
        consumer.accept(new IClientFluidTypeExtensions() {
            @Override
            public ResourceLocation getStillTexture() {
                return STILL_TEXTURE;
            }

            @Override
            public ResourceLocation getFlowingTexture() {
                return FLOWING_TEXTURE;
            }

            @Override
            public ResourceLocation getOverlayTexture() {
                return OVERLAY_TEXTURE;
            }

            @Override
            public ResourceLocation getRenderOverlayTexture(Minecraft minecraft) {
                return UNDERWATER_TEXTURE;
            }

            @Override
            public int getTintColor() {
                return WATER_TINT;
            }

            @Override
            public @NotNull Vector3f modifyFogColor(
                    Camera camera,
                    float partialTick,
                    ClientLevel level,
                    int renderDistance,
                    float darkenWorldAmount,
                    Vector3f fluidFogColor
            ) {
                return new Vector3f(0.24F, 0.018F, 0.035F);
            }
        });
    }
}
