package fr.blue.abyssalplanet.item;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class AbyssalHunterCrossbowItem extends CrossbowItem {
    public static final String SHOT_COUNT_TAG = "AbyssalHunterShotCount";
    public static final String PENDING_SHOT_TAG = "AbyssalHunterPendingShot";
    public static final String PENDING_SPECIAL_TAG = "AbyssalHunterPendingSpecial";
    public static final String PENDING_TARGET_X_TAG = "AbyssalHunterTargetX";
    public static final String PENDING_TARGET_Y_TAG = "AbyssalHunterTargetY";
    public static final String PENDING_TARGET_Z_TAG = "AbyssalHunterTargetZ";

    public AbyssalHunterCrossbowItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer && CrossbowItem.isCharged(stack)) {
            int shotCount = (getShotCount(stack) + 1) % 5;
            boolean specialShot = shotCount == 0;
            stack.getOrCreateTag().putInt(SHOT_COUNT_TAG, shotCount);

            CompoundTag playerData = serverPlayer.getPersistentData();
            Vec3 target = serverPlayer.getEyePosition().add(serverPlayer.getViewVector(1.0F).scale(72.0D));
            playerData.putBoolean(PENDING_SHOT_TAG, true);
            playerData.putBoolean(PENDING_SPECIAL_TAG, specialShot);
            playerData.putDouble(PENDING_TARGET_X_TAG, target.x);
            playerData.putDouble(PENDING_TARGET_Y_TAG, target.y);
            playerData.putDouble(PENDING_TARGET_Z_TAG, target.z);

            InteractionResultHolder<ItemStack> result = super.use(level, player, hand);
            clearPendingShot(playerData);
            return result;
        }
        return super.use(level, player, hand);
    }

    public static int getShotCount(ItemStack stack) {
        return stack.hasTag() ? Math.floorMod(stack.getTag().getInt(SHOT_COUNT_TAG), 5) : 0;
    }

    private static void clearPendingShot(CompoundTag data) {
        data.remove(PENDING_SHOT_TAG);
        data.remove(PENDING_SPECIAL_TAG);
        data.remove(PENDING_TARGET_X_TAG);
        data.remove(PENDING_TARGET_Y_TAG);
        data.remove(PENDING_TARGET_Z_TAG);
    }
}
