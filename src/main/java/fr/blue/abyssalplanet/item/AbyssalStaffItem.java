package fr.blue.abyssalplanet.item;

import fr.blue.abyssalplanet.entity.AbyssalPortalEntity;
import fr.blue.abyssalplanet.entity.PlayerAbyssalInkBallEntity;
import fr.blue.abyssalplanet.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class AbyssalStaffItem extends Item {
    private static final int MELEE_INK_COST = 1;
    private static final int RANGED_INK_COST = 1;
    private static final int PORTAL_INK_COST = 5;
    private static final float MELEE_DAMAGE = 3.0F;
    private static final double MELEE_KNOCKBACK = 1.15D;
    private static final int MIN_RANGED_CHARGE_TICKS = 8;

    public AbyssalStaffItem(Properties properties) {
        super(properties);
    }

    @Override
    public boolean onLeftClickEntity(ItemStack stack, Player player, Entity target) {
        if (player.level().isClientSide) {
            return false;
        }

        if (!(target instanceof LivingEntity livingTarget) || !livingTarget.isAlive()) {
            return false;
        }

        if (!consumeAbyssalInk(player, MELEE_INK_COST)) {
            notifyMissingInk(player, MELEE_INK_COST);
            player.getCooldowns().addCooldown(this, 8);
            return true;
        }

        boolean hurt = livingTarget.hurt(player.damageSources().playerAttack(player), MELEE_DAMAGE);
        if (hurt) {
            Vec3 knockback = livingTarget.position().subtract(player.position());
            if (knockback.lengthSqr() < 0.001D) {
                knockback = player.getLookAngle();
            }
            knockback = knockback.normalize().scale(MELEE_KNOCKBACK);
            livingTarget.setDeltaMovement(
                    livingTarget.getDeltaMovement().add(knockback.x, 0.22D, knockback.z)
            );
            livingTarget.hurtMarked = true;
            player.level().playSound(
                    null,
                    livingTarget.blockPosition(),
                    SoundEvents.SQUID_SQUIRT,
                    SoundSource.PLAYERS,
                    0.75F,
                    0.75F
            );
        }

        player.getCooldowns().addCooldown(this, 12);
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!player.getAbilities().instabuild && countAbyssalInk(player) < RANGED_INK_COST) {
            if (!level.isClientSide) {
                notifyMissingInk(player, RANGED_INK_COST);
            }
            return InteractionResultHolder.fail(stack);
        }

        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeLeft) {
        if (!(livingEntity instanceof ServerPlayer player) || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        int chargeTicks = this.getUseDuration(stack) - timeLeft;
        if (chargeTicks < MIN_RANGED_CHARGE_TICKS) {
            return;
        }

        float power = BowItem.getPowerForTime(chargeTicks);
        if (power < 0.2F) {
            return;
        }

        if (!consumeAbyssalInk(player, RANGED_INK_COST)) {
            notifyMissingInk(player, RANGED_INK_COST);
            return;
        }

        PlayerAbyssalInkBallEntity.spawn(serverLevel, player, power);
        level.playSound(
                null,
                player.blockPosition(),
                SoundEvents.SQUID_SQUIRT,
                SoundSource.PLAYERS,
                0.95F,
                0.45F + power * 0.35F
        );
        player.awardStat(Stats.ITEM_USED.get(this));
        player.getCooldowns().addCooldown(this, 18);
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    public static void tryOpenPortal(ServerPlayer player) {
        if (!isHoldingStaff(player)) {
            return;
        }

        ServerLevel level = player.serverLevel();
        HitResult hitResult = player.level().clip(new ClipContext(
                player.getEyePosition(),
                player.getEyePosition().add(player.getLookAngle().scale(6.0D)),
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE,
                player
        ));

        if (!(hitResult instanceof BlockHitResult blockHitResult)
                || hitResult.getType() != HitResult.Type.BLOCK) {
            player.sendSystemMessage(Component.literal("Le bâton abyssal doit viser un bloc.")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        BlockState state = level.getBlockState(blockHitResult.getBlockPos());
        if (state.isAir()) {
            player.sendSystemMessage(Component.literal("Le portail refuse le vide.")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        if (AbyssalPortalEntity.hasPortalNear(level, blockHitResult.getBlockPos())) {
            player.sendSystemMessage(Component.literal("Un trou noir abyssal est déjà ouvert ici.")
                    .withStyle(ChatFormatting.RED));
            return;
        }

        if (!consumeAbyssalInk(player, PORTAL_INK_COST)) {
            notifyMissingInk(player, PORTAL_INK_COST);
            return;
        }

        Vec3 faceOffset = Vec3.atLowerCornerOf(blockHitResult.getDirection().getNormal()).scale(0.58D);
        Vec3 portalPosition = Vec3.atCenterOf(blockHitResult.getBlockPos()).add(faceOffset);
        AbyssalPortalEntity.spawn(level, blockHitResult.getBlockPos(), portalPosition, player.getUUID());
        level.playSound(
                null,
                blockHitResult.getBlockPos(),
                SoundEvents.RESPAWN_ANCHOR_CHARGE,
                SoundSource.PLAYERS,
                0.85F,
                0.45F
        );
        player.sendSystemMessage(Component.literal("Un début de portail abyssal s'ouvre. Une autre personne doit le canaliser.")
                .withStyle(ChatFormatting.DARK_PURPLE));
    }

    public static boolean isHoldingStaff(Player player) {
        return player.getMainHandItem().is(ModItems.ABYSSAL_STAFF.get())
                || player.getOffhandItem().is(ModItems.ABYSSAL_STAFF.get());
    }

    public static boolean consumeAbyssalInk(Player player, int amount) {
        if (player.getAbilities().instabuild) {
            return true;
        }

        if (countAbyssalInk(player) < amount) {
            return false;
        }

        int remaining = amount;
        for (int slot = 0; slot < player.getInventory().getContainerSize() && remaining > 0; slot++) {
            ItemStack inventoryStack = player.getInventory().getItem(slot);
            if (!inventoryStack.is(ModItems.ABYSSAL_INK.get())) {
                continue;
            }

            int taken = Math.min(remaining, inventoryStack.getCount());
            inventoryStack.shrink(taken);
            remaining -= taken;
        }

        return true;
    }

    public static void refundAbyssalInk(ServerPlayer player, int amount) {
        if (player.getAbilities().instabuild) {
            return;
        }

        ItemStack refund = new ItemStack(ModItems.ABYSSAL_INK.get(), amount);
        if (!player.getInventory().add(refund)) {
            player.drop(refund, false);
        }
    }

    private static int countAbyssalInk(Player player) {
        int count = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack inventoryStack = player.getInventory().getItem(slot);
            if (inventoryStack.is(ModItems.ABYSSAL_INK.get())) {
                count += inventoryStack.getCount();
            }
        }
        return count;
    }

    private static void notifyMissingInk(Player player, int amount) {
        player.sendSystemMessage(Component.literal("Il faut " + amount + " encre(s) abyssale(s) pour nourrir le bâton.")
                .withStyle(ChatFormatting.RED));
    }
}
