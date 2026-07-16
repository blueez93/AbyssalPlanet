package fr.blue.abyssalplanet.menu;

import fr.blue.abyssalplanet.item.AbyssalPouchItem;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class AbyssalPouchContainer implements Container {
    private static final Map<UUID, SharedContents> OPEN_CONTENTS = new HashMap<>();

    private final ServerPlayer owner;
    private final UUID pouchId;
    private final SharedContents contents;

    public AbyssalPouchContainer(ServerPlayer owner, UUID pouchId) {
        this.owner = owner;
        this.pouchId = pouchId;
        this.contents = OPEN_CONTENTS.computeIfAbsent(pouchId, ignored -> loadContents(owner, pouchId));
    }

    @Override
    public int getContainerSize() {
        return AbyssalPouchItem.SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        for (ItemStack stack : this.contents.items) {
            if (!stack.isEmpty()) {
                return false;
            }
        }
        return true;
    }

    @Override
    public ItemStack getItem(int slot) {
        if (!isSlotUsable(slot)) {
            return ItemStack.EMPTY;
        }
        return this.contents.items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        if (!isSlotUsable(slot)) {
            return ItemStack.EMPTY;
        }

        ItemStack removed = ContainerHelper.removeItem(this.contents.items, slot, amount);
        if (!removed.isEmpty()) {
            setChanged();
        }
        return removed;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        if (!isSlotUsable(slot)) {
            return ItemStack.EMPTY;
        }
        ItemStack removed = this.contents.items.get(slot);
        this.contents.items.set(slot, ItemStack.EMPTY);
        return removed;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        if (!isSlotUsable(slot)) {
            return;
        }

        this.contents.items.set(slot, stack);
        if (stack.getCount() > getMaxStackSize()) {
            stack.setCount(getMaxStackSize());
        }
        setChanged();
    }

    @Override
    public void setChanged() {
        save();
    }

    @Override
    public boolean stillValid(Player player) {
        return hasOriginalPouch();
    }

    @Override
    public void startOpen(Player player) {
        this.contents.openMenus++;
    }

    @Override
    public void stopOpen(Player player) {
        save();
        this.contents.openMenus = Math.max(0, this.contents.openMenus - 1);
        if (this.contents.openMenus == 0) {
            OPEN_CONTENTS.remove(this.pouchId);
        }
    }

    @Override
    public void clearContent() {
        for (int slot = 0; slot < this.contents.items.size(); slot++) {
            this.contents.items.set(slot, ItemStack.EMPTY);
        }
        setChanged();
    }

    public boolean hasOriginalPouch() {
        return this.owner != null && AbyssalPouchItem.findPouch(this.owner, this.pouchId).isPresent();
    }

    public int getCapacity() {
        Optional<ItemStack> pouchStack = AbyssalPouchItem.findPouch(this.owner, this.pouchId);
        pouchStack.ifPresent(stack -> this.contents.capacity = AbyssalPouchItem.getCapacity(stack));
        return this.contents.capacity;
    }

    public boolean isSlotUsable(int slot) {
        return slot >= 0 && slot < this.contents.items.size() && slot < getCapacity();
    }

    public static void replaceOpenContents(UUID pouchId, NonNullList<ItemStack> items, int capacity) {
        SharedContents contents = OPEN_CONTENTS.get(pouchId);
        if (contents == null) {
            return;
        }

        for (int slot = 0; slot < contents.items.size(); slot++) {
            contents.items.set(slot, items.get(slot).copy());
        }
        contents.capacity = capacity;
    }

    public static void forgetSharedContents(UUID pouchId) {
        OPEN_CONTENTS.remove(pouchId);
    }

    private void save() {
        Optional<ItemStack> pouchStack = AbyssalPouchItem.findPouch(this.owner, this.pouchId);
        pouchStack.ifPresent(stack -> {
            stack.getOrCreateTag().putInt(AbyssalPouchItem.TAG_CAPACITY, getCapacity());
            AbyssalPouchItem.saveItems(stack, this.contents.items);
        });
    }

    private static SharedContents loadContents(ServerPlayer owner, UUID pouchId) {
        NonNullList<ItemStack> items = NonNullList.withSize(AbyssalPouchItem.SLOT_COUNT, ItemStack.EMPTY);
        int capacity = AbyssalPouchItem.SLOT_COUNT;
        Optional<ItemStack> pouchStack = AbyssalPouchItem.findPouch(owner, pouchId);
        if (pouchStack.isPresent()) {
            ItemStack stack = pouchStack.get();
            AbyssalPouchItem.loadItems(stack, items);
            capacity = AbyssalPouchItem.getOrCreateCapacity(stack);
        }
        return new SharedContents(items, capacity);
    }

    private static class SharedContents {
        private final NonNullList<ItemStack> items;
        private int capacity;
        private int openMenus;

        private SharedContents(NonNullList<ItemStack> items, int capacity) {
            this.items = items;
            this.capacity = capacity;
        }
    }
}
