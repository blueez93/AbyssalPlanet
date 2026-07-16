package fr.blue.abyssalplanet.client;

import fr.blue.abyssalplanet.AbyssalPlanet;
import fr.blue.abyssalplanet.registry.ModEffects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AbyssalPlanet.MOD_ID, value = Dist.CLIENT)
public final class BlueGoldHeartHud {
    private static final net.minecraft.resources.ResourceLocation HEART_TEXTURE =
            AbyssalPlanet.id("textures/gui/blue_gold_hearts.png");

    private BlueGoldHeartHud() {
    }

    @SubscribeEvent
    public static void afterPlayerHealth(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.PLAYER_HEALTH.type()) {
            return;
        }

        Player player = Minecraft.getInstance().player;
        if (player == null || player.isCreative() || player.isSpectator()) {
            return;
        }

        MobEffectInstance marker = player.getEffect(ModEffects.BLUE_GOLD_HEARTS.get());
        int absorptionPoints = Mth.ceil(player.getAbsorptionAmount());
        if (marker == null || absorptionPoints <= 0) {
            return;
        }

        int bluePoints = Math.min(marker.getAmplifier() + 1, absorptionPoints);
        int blueHearts = Mth.ceil(bluePoints / 2.0F);
        float maximumHealth = Math.max(
                (float) player.getAttributeValue(Attributes.MAX_HEALTH),
                player.getHealth()
        );
        int normalHearts = Mth.ceil(maximumHealth / 2.0F);
        int absorptionHearts = Mth.ceil(absorptionPoints / 2.0F);
        int totalRows = Mth.ceil((maximumHealth + absorptionPoints) / 2.0F / 10.0F);
        int rowHeight = Math.max(10 - (totalRows - 2), 3);
        int firstBlueHeart = normalHearts + absorptionHearts - blueHearts;
        int left = event.getWindow().getGuiScaledWidth() / 2 - 91;
        int top = event.getWindow().getGuiScaledHeight() - 39;
        GuiGraphics graphics = event.getGuiGraphics();

        for (int index = 0; index < blueHearts; index++) {
            int heartIndex = firstBlueHeart + index;
            int x = left + heartIndex % 10 * 8;
            int y = top - heartIndex / 10 * rowHeight;
            boolean halfHeart = index == blueHearts - 1 && (bluePoints & 1) == 1;
            graphics.blit(
                    HEART_TEXTURE,
                    x,
                    y,
                    halfHeart ? 9.0F : 0.0F,
                    0.0F,
                    9,
                    9,
                    18,
                    9
            );
        }
    }
}
