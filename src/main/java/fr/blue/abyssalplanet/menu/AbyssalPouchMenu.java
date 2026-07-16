package fr.blue.abyssalplanet.menu;

import fr.blue.abyssalplanet.item.AbyssalPouchItem;
import fr.blue.abyssalplanet.registry.ModMenuTypes;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public class AbyssalPouchMenu extends AbstractContainerMenu {
    public static final int POUCH_COLUMNS = 6;
    public static final int POUCH_ROWS = 5;
    private static final int POUCH_SLOT_COUNT = POUCH_COLUMNS * POUCH_ROWS;
    private static final int PLAYER_INVENTORY_START = POUCH_SLOT_COUNT;
    private static final int PLAYER_INVENTORY_END = PLAYER_INVENTORY_START + 27;
    private static final int HOTBAR_END = PLAYER_INVENTORY_END + 9;

    private final Container pouchContainer;
    private int clientCapacity = POUCH_SLOT_COUNT;

    public AbyssalPouchMenu(int containerId, Inventory playerInventory, FriendlyByteBuf ignored) {
        this(containerId, playerInventory, new SimpleContainer(POUCH_SLOT_COUNT));
    }

    public AbyssalPouchMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, new SimpleContainer(POUCH_SLOT_COUNT));
    }

    public AbyssalPouchMenu(int containerId, Inventory playerInventory, ServerPlayer owner, UUID pouchId) {
        this(containerId, playerInventory, new AbyssalPouchContainer(owner, pouchId));
    }

    private AbyssalPouchMenu(int containerId, Inventory playerInventory, Container pouchContainer) {
        super(ModMenuTypes.ABYSSAL_POUCH.get(), containerId);
        checkContainerSize(pouchContainer, POUCH_SLOT_COUNT);
        this.pouchContainer = pouchContainer;
        this.pouchContainer.startOpen(playerInventory.player);

        int pouchStartX = 34;
        int pouchStartY = 18;
        for (int row = 0; row < POUCH_ROWS; row++) {
            for (int column = 0; column < POUCH_COLUMNS; column++) {
                int slot = column + row * POUCH_COLUMNS;
                addSlot(new PouchSlot(this, pouchContainer, slot, pouchStartX + column * 18, pouchStartY + row * 18));
            }
        }

        addDataSlot(new DataSlot() {
            @Override
            public int get() {
                if (AbyssalPouchMenu.this.pouchContainer instanceof AbyssalPouchContainer abyssalPouchContainer) {
                    return abyssalPouchContainer.getCapacity();
                }
                return AbyssalPouchMenu.this.clientCapacity;
            }

            @Override
            public void set(int value) {
                AbyssalPouchMenu.this.clientCapacity = Math.max(1, Math.min(POUCH_SLOT_COUNT, value));
            }
        });

        int playerInventoryY = 122;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9, 8 + column * 18, playerInventoryY + row * 18));
            }
        }

        int hotbarY = 180;
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, hotbarY));
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack result = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            result = stack.copy();

            if (index < POUCH_SLOT_COUNT) {
                if (!moveItemStackTo(stack, PLAYER_INVENTORY_START, HOTBAR_END, true)) {
                    return ItemStack.EMPTY;
                }
            } else if (!moveItemStackTo(stack, 0, getCapacity(), false)) {
                return ItemStack.EMPTY;
            }

            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }

        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return this.pouchContainer.stillValid(player);
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        this.pouchContainer.stopOpen(player);
    }

    public int getCapacity() {
        if (this.pouchContainer instanceof AbyssalPouchContainer abyssalPouchContainer) {
            return abyssalPouchContainer.getCapacity();
        }
        return this.clientCapacity;
    }

    public boolean isPouchSlotUsable(int slot) {
        return slot >= 0 && slot < getCapacity();
    }

    private static class PouchSlot extends Slot {
        private final AbyssalPouchMenu menu;
        private final int pouchSlot;

        private PouchSlot(AbyssalPouchMenu menu, Container container, int slot, int x, int y) {
            super(container, slot, x, y);
            this.menu = menu;
            this.pouchSlot = slot;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return this.menu.isPouchSlotUsable(this.pouchSlot) && !AbyssalPouchItem.isPouch(stack);
        }

        @Override
        public boolean mayPickup(Player player) {
            return this.menu.isPouchSlotUsable(this.pouchSlot);
        }

        @Override
        public boolean isActive() {
            return this.menu.isPouchSlotUsable(this.pouchSlot);
        }
    }
}
