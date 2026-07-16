package fr.blue.abyssalplanet.item;

import fr.blue.abyssalplanet.entity.AbyssalPouchGhostEntity;
import fr.blue.abyssalplanet.menu.AbyssalPouchContainer;
import fr.blue.abyssalplanet.menu.AbyssalPouchMenu;
import fr.blue.abyssalplanet.registry.ModItems;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

public class AbyssalPouchItem extends Item {
    public static final int SLOT_COUNT = 30;
    public static final int MIN_SLOT_COUNT = 1;
    public static final String TAG_POUCH_ID = "AbyssalPouchId";
    public static final String TAG_CAPACITY = "AbyssalPouchCapacity";

    private static final Component TITLE = Component.translatable("container.abyssalplanet.abyssal_pouch");

    public AbyssalPouchItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
        }

        UUID pouchId = getOrCreatePouchId(stack);
        getOrCreateCapacity(stack);
        openPouch(serverPlayer, serverPlayer, pouchId);
        return InteractionResultHolder.success(stack);
    }

    public static void openPouch(ServerPlayer opener, ServerPlayer owner, UUID pouchId) {
        Optional<ItemStack> pouchStack = findPouch(owner, pouchId);
        if (pouchStack.isEmpty()) {
            opener.sendSystemMessage(Component.literal("La bourse abyssale liée n'est plus dans l'inventaire.")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        MenuProvider provider = new SimpleMenuProvider(
                (containerId, inventory, player) -> new AbyssalPouchMenu(containerId, inventory, owner, pouchId),
                TITLE
        );
        NetworkHooks.openScreen(opener, provider);
    }

    public static void tryPlaceGhost(ServerPlayer player) {
        ItemStack pouchStack = getHeldPouch(player);
        if (pouchStack.isEmpty()) {
            return;
        }

        UUID pouchId = getOrCreatePouchId(pouchStack);
        getOrCreateCapacity(pouchStack);
        ServerLevel level = player.serverLevel();
        Vec3 position = findGhostPlacement(player);

        AbyssalPouchGhostEntity.removeLoadedCopies(level, player.getUUID(), pouchId, position);
        AbyssalPouchGhostEntity.spawn(level, player.getUUID(), pouchId, position);
        player.sendSystemMessage(Component.literal("Une copie fantomatique de la bourse est posée.")
                .withStyle(ChatFormatting.DARK_AQUA));
    }

    public static boolean isHoldingPouch(Player player) {
        return player.getMainHandItem().is(ModItems.ABYSSAL_POUCH.get())
                || player.getOffhandItem().is(ModItems.ABYSSAL_POUCH.get());
    }

    public static Optional<ItemStack> findPouch(ServerPlayer owner, UUID pouchId) {
        Inventory inventory = owner.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.is(ModItems.ABYSSAL_POUCH.get()) && pouchId.equals(getPouchId(stack).orElse(null))) {
                return Optional.of(stack);
            }
        }
        return Optional.empty();
    }

    public static UUID getOrCreatePouchId(ItemStack stack) {
        Optional<UUID> existing = getPouchId(stack);
        if (existing.isPresent()) {
            return existing.get();
        }

        UUID pouchId = UUID.randomUUID();
        stack.getOrCreateTag().putUUID(TAG_POUCH_ID, pouchId);
        return pouchId;
    }

    public static int getOrCreateCapacity(ItemStack stack) {
        int capacity = getCapacity(stack);
        stack.getOrCreateTag().putInt(TAG_CAPACITY, capacity);
        return capacity;
    }

    public static int getCapacity(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.contains(TAG_CAPACITY)) {
            return SLOT_COUNT;
        }
        return clampCapacity(tag.getInt(TAG_CAPACITY));
    }

    public static boolean ghostSearchConsumesSlot(ServerPlayer opener, ServerPlayer owner, UUID pouchId) {
        Optional<ItemStack> pouchStack = findPouch(owner, pouchId);
        if (pouchStack.isEmpty()) {
            opener.sendSystemMessage(Component.literal("La bourse abyssale liée n'est plus dans l'inventaire.")
                    .withStyle(ChatFormatting.RED));
            return false;
        }

        ItemStack stack = pouchStack.get();
        NonNullList<ItemStack> items = NonNullList.withSize(SLOT_COUNT, ItemStack.EMPTY);
        loadItems(stack, items);

        int capacity = getOrCreateCapacity(stack);
        if (capacity <= MIN_SLOT_COUNT) {
            dropRemovedSlots(owner, items, 0);
            stack.shrink(1);
            owner.getInventory().setChanged();
            AbyssalPouchContainer.forgetSharedContents(pouchId);

            opener.sendSystemMessage(Component.literal("La bourse fantomatique se déchire après avoir été fouillée.")
                    .withStyle(ChatFormatting.DARK_PURPLE));
            if (!opener.getUUID().equals(owner.getUUID())) {
                owner.sendSystemMessage(Component.literal("Ta bourse abyssale se brise : sa copie fantomatique a été trop fouillée.")
                        .withStyle(ChatFormatting.DARK_PURPLE));
            }
            return false;
        }

        int newCapacity = capacity - 1;
        dropRemovedSlots(owner, items, newCapacity);
        stack.getOrCreateTag().putInt(TAG_CAPACITY, newCapacity);
        saveItems(stack, items);
        owner.getInventory().setChanged();
        AbyssalPouchContainer.replaceOpenContents(pouchId, items, newCapacity);

        opener.sendSystemMessage(Component.literal("La bourse fantomatique perd une place. Places restantes : " + newCapacity + ".")
                .withStyle(ChatFormatting.DARK_AQUA));
        if (!opener.getUUID().equals(owner.getUUID())) {
            owner.sendSystemMessage(Component.literal("Ta bourse abyssale perd une place car sa copie fantomatique a été fouillée.")
                    .withStyle(ChatFormatting.DARK_AQUA));
        }
        return true;
    }

    public static Optional<UUID> getPouchId(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        if (tag == null || !tag.hasUUID(TAG_POUCH_ID)) {
            return Optional.empty();
        }
        return Optional.of(tag.getUUID(TAG_POUCH_ID));
    }

    public static void loadItems(ItemStack stack, net.minecraft.core.NonNullList<ItemStack> items) {
        CompoundTag tag = stack.getTag();
        if (tag != null) {
            ContainerHelper.loadAllItems(tag, items);
        }
    }

    public static void saveItems(ItemStack stack, net.minecraft.core.NonNullList<ItemStack> items) {
        CompoundTag tag = stack.getOrCreateTag();
        ContainerHelper.saveAllItems(tag, items, true);
    }

    public static boolean isPouch(ItemStack stack) {
        return stack.is(ModItems.ABYSSAL_POUCH.get());
    }

    private static int clampCapacity(int capacity) {
        return Math.max(MIN_SLOT_COUNT, Math.min(SLOT_COUNT, capacity));
    }

    private static void dropRemovedSlots(ServerPlayer owner, NonNullList<ItemStack> items, int firstRemovedSlot) {
        int startSlot = Math.max(0, Math.min(SLOT_COUNT, firstRemovedSlot));
        for (int slot = startSlot; slot < SLOT_COUNT; slot++) {
            ItemStack removed = items.get(slot);
            if (removed.isEmpty()) {
                continue;
            }

            owner.drop(removed.copy(), false);
            items.set(slot, ItemStack.EMPTY);
        }
    }

    private static ItemStack getHeldPouch(Player player) {
        ItemStack mainHand = player.getMainHandItem();
        if (mainHand.is(ModItems.ABYSSAL_POUCH.get())) {
            return mainHand;
        }

        ItemStack offhand = player.getOffhandItem();
        return offhand.is(ModItems.ABYSSAL_POUCH.get()) ? offhand : ItemStack.EMPTY;
    }

    private static Vec3 findGhostPlacement(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        HitResult hitResult = level.clip(new ClipContext(
                player.getEyePosition(),
                player.getEyePosition().add(player.getLookAngle().scale(6.0D)),
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                player
        ));

        if (hitResult instanceof BlockHitResult blockHitResult && hitResult.getType() == HitResult.Type.BLOCK) {
            BlockPos blockPos = blockHitResult.getBlockPos();
            if (blockHitResult.getDirection() == Direction.UP) {
                return Vec3.atBottomCenterOf(blockPos.above()).add(0.0D, 0.03D, 0.0D);
            }

            Vec3 normal = Vec3.atLowerCornerOf(blockHitResult.getDirection().getNormal()).scale(0.2D);
            return blockHitResult.getLocation().add(normal);
        }

        return player.position().add(player.getLookAngle().scale(1.4D)).add(0.0D, 0.25D, 0.0D);
    }
}
