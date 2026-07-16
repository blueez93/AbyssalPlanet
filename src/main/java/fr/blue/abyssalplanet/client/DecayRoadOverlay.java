package fr.blue.abyssalplanet.client;

import com.mojang.blaze3d.systems.RenderSystem;
import fr.blue.abyssalplanet.AbyssalPlanet;
import fr.blue.abyssalplanet.registry.ModBiomes;
import fr.blue.abyssalplanet.registry.ModDimensions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AbyssalPlanet.MOD_ID, value = Dist.CLIENT)
public final class DecayRoadOverlay {
    private static final int TINT_RGB = 0x6E0018;
    private static final int VIGNETTE_RGB = 0x8F001F;
    private static final int VIGNETTE_BANDS = 32;
    private static float previousIntensity;
    private static float intensity;

    private DecayRoadOverlay() {
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        previousIntensity = intensity;

        if (minecraft.player == null || minecraft.level == null) {
            previousIntensity = 0.0F;
            intensity = 0.0F;
            return;
        }

        boolean inDecayRoad = minecraft.player.level().dimension().equals(ModDimensions.ABYSSAL_PLANET_LEVEL)
                && minecraft.level.getBiome(minecraft.player.blockPosition()).is(ModBiomes.DECAY_ROAD);
        float change = inDecayRoad ? 0.045F : -0.065F;
        intensity = Mth.clamp(intensity + change, 0.0F, 1.0F);
    }

    @SubscribeEvent
    public static void beforeHud(RenderGuiEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        float visibleIntensity = Mth.lerp(event.getPartialTick(), previousIntensity, intensity);
        if (visibleIntensity <= 0.001F || minecraft.player == null) {
            return;
        }

        GuiGraphics graphics = event.getGuiGraphics();
        int width = event.getWindow().getGuiScaledWidth();
        int height = event.getWindow().getGuiScaledHeight();
        boolean underwater = minecraft.player.isUnderWater();
        float immersion = underwater ? 1.0F : 0.72F;

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();

        int tintAlpha = Mth.floor(visibleIntensity * immersion * (underwater ? 18.0F : 12.0F));
        graphics.fill(0, 0, width, height, withAlpha(TINT_RGB, tintAlpha));

        int edgeSize = Mth.clamp(Math.min(width, height) / 6, 24, 72);
        for (int band = 0; band < VIGNETTE_BANDS; band++) {
            int outer = band * edgeSize / VIGNETTE_BANDS;
            int inner = (band + 1) * edgeSize / VIGNETTE_BANDS;
            if (inner <= outer) {
                continue;
            }

            float distance = band / (float) (VIGNETTE_BANDS - 1);
            float falloff = 1.0F - distance;
            int alpha = Mth.floor(visibleIntensity * immersion * (10.0F + 76.0F * falloff * falloff));
            int color = withAlpha(VIGNETTE_RGB, alpha);

            graphics.fill(outer, outer, width - outer, inner, color);
            graphics.fill(outer, height - inner, width - outer, height - outer, color);
            graphics.fill(outer, inner, inner, height - inner, color);
            graphics.fill(width - inner, inner, width - outer, height - inner, color);
        }

        RenderSystem.disableBlend();
    }

    private static int withAlpha(int rgb, int alpha) {
        return Mth.clamp(alpha, 0, 255) << 24 | rgb;
    }
}
