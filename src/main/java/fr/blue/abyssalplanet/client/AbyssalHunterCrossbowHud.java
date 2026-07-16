package fr.blue.abyssalplanet.client;

import com.mojang.blaze3d.systems.RenderSystem;
import fr.blue.abyssalplanet.AbyssalPlanet;
import fr.blue.abyssalplanet.item.AbyssalHunterCrossbowItem;
import fr.blue.abyssalplanet.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AbyssalPlanet.MOD_ID, value = Dist.CLIENT)
public final class AbyssalHunterCrossbowHud {
    private AbyssalHunterCrossbowHud() {
    }

    @SubscribeEvent
    public static void renderShotCounter(RenderGuiEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.options.hideGui) {
            return;
        }

        ItemStack stack = minecraft.player.getMainHandItem();
        if (!stack.is(ModItems.ABYSSAL_HUNTER_CROSSBOW.get())) {
            stack = minecraft.player.getOffhandItem();
        }
        if (!stack.is(ModItems.ABYSSAL_HUNTER_CROSSBOW.get())) {
            return;
        }

        int fired = AbyssalHunterCrossbowItem.getShotCount(stack);
        GuiGraphics graphics = event.getGuiGraphics();
        int centerX = graphics.guiWidth() / 2;
        int y = graphics.guiHeight() - 67;
        int startX = centerX - 27;

        RenderSystem.enableBlend();
        graphics.fill(startX - 3, y - 3, startX + 57, y + 8, 0xA0100710);
        for (int index = 0; index < 5; index++) {
            int x = startX + index * 12;
            boolean completed = index < fired;
            boolean special = index == 4;
            int border = special ? 0xFFFF5668 : 0xFF68404A;
            int fill = completed ? 0xFFE0364C : 0xFF25131A;
            graphics.fill(x, y, x + 9, y + 5, border);
            graphics.fill(x + 1, y + 1, x + 8, y + 4, fill);
        }
        RenderSystem.disableBlend();
    }
}
