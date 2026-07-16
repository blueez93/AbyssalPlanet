package fr.blue.abyssalplanet.client;

import fr.blue.abyssalplanet.menu.AbyssalPouchMenu;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

public class AbyssalPouchScreen extends AbstractContainerScreen<AbyssalPouchMenu> {
    public AbyssalPouchScreen(AbyssalPouchMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.imageWidth = 176;
        this.imageHeight = 204;
        this.titleLabelX = 8;
        this.titleLabelY = 6;
        this.inventoryLabelX = 8;
        this.inventoryLabelY = 110;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(guiGraphics);
        super.render(guiGraphics, mouseX, mouseY, partialTick);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY) {
        int left = this.leftPos;
        int top = this.topPos;
        guiGraphics.fill(left, top, left + this.imageWidth, top + this.imageHeight, 0xEE09111D);
        guiGraphics.fill(left + 3, top + 3, left + this.imageWidth - 3, top + this.imageHeight - 3, 0xCC10223A);
        guiGraphics.fill(left + 6, top + 14, left + this.imageWidth - 6, top + 108, 0x88040A12);
        guiGraphics.fill(left + 6, top + 118, left + this.imageWidth - 6, top + 198, 0x88040A12);

        drawSlotGrid(guiGraphics, left + 34, top + 18, AbyssalPouchMenu.POUCH_COLUMNS, AbyssalPouchMenu.POUCH_ROWS);
        drawLockedPouchSlots(guiGraphics, left + 34, top + 18, this.menu.getCapacity());
        drawSlotGrid(guiGraphics, left + 8, top + 122, 9, 3);
        drawSlotGrid(guiGraphics, left + 8, top + 180, 9, 1);
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY) {
        guiGraphics.drawString(this.font, this.title, this.titleLabelX, this.titleLabelY, 0x9DF8FF, false);
        guiGraphics.drawString(this.font, this.playerInventoryTitle, this.inventoryLabelX, this.inventoryLabelY, 0xD8EAF2, false);
    }

    private static void drawSlotGrid(GuiGraphics guiGraphics, int x, int y, int columns, int rows) {
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                int slotX = x + column * 18;
                int slotY = y + row * 18;
                guiGraphics.fill(slotX - 1, slotY - 1, slotX + 17, slotY + 17, 0xFF2D3B50);
                guiGraphics.fill(slotX, slotY, slotX + 16, slotY + 16, 0xFF07101C);
                guiGraphics.fill(slotX, slotY, slotX + 16, slotY + 1, 0x6638DCEB);
                guiGraphics.fill(slotX, slotY + 15, slotX + 16, slotY + 16, 0x663D2458);
            }
        }
    }

    private static void drawLockedPouchSlots(GuiGraphics guiGraphics, int x, int y, int capacity) {
        int clampedCapacity = Math.max(0, Math.min(AbyssalPouchMenu.POUCH_COLUMNS * AbyssalPouchMenu.POUCH_ROWS, capacity));
        for (int slot = clampedCapacity; slot < AbyssalPouchMenu.POUCH_COLUMNS * AbyssalPouchMenu.POUCH_ROWS; slot++) {
            int column = slot % AbyssalPouchMenu.POUCH_COLUMNS;
            int row = slot / AbyssalPouchMenu.POUCH_COLUMNS;
            int slotX = x + column * 18;
            int slotY = y + row * 18;
            guiGraphics.fill(slotX, slotY, slotX + 16, slotY + 16, 0xCC02040A);
            guiGraphics.fill(slotX + 3, slotY + 7, slotX + 13, slotY + 9, 0xAA4A234F);
        }
    }
}
