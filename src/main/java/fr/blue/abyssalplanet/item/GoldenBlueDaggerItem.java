package fr.blue.abyssalplanet.item;

import fr.blue.abyssalplanet.effect.AbyssalPoisonEffect;
import fr.blue.abyssalplanet.registry.ModEffects;
import fr.blue.abyssalplanet.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class GoldenBlueDaggerItem extends SwordItem {
    private static final String NEXT_STRIKE_TICK_TAG = "NextAbyssalStrikeTick";
    private static final String COOLDOWN_REMAINING_TAG = "AbyssalStrikeCooldownRemaining";
    private static final int STEALTH_STRIKE_COOLDOWN_TICKS = 20 * 5;
    private static final int POISON_DURATION_PER_STACK = 20 * 4;
    private static final int PLAYER_MAX_STACKS = 4;
    private static final int MOB_MAX_STACKS = 6;
    private static final float STEALTH_STRIKE_DAMAGE = 7.0F;
    private static final double STRIKE_REACH = 4.5D;

    public GoldenBlueDaggerItem(Tier tier, Properties properties) {
        super(tier, 3, -2.4F, properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);

        if (!level.isClientSide || stack.getTag() == null) {
            return;
        }

        long remaining = Math.max(0L, stack.getTag().getLong(NEXT_STRIKE_TICK_TAG) - level.getGameTime());
        if (remaining > 0L) {
            stack.getTag().putInt(COOLDOWN_REMAINING_TAG, (int) Math.min(remaining, STEALTH_STRIKE_COOLDOWN_TICKS));
        } else {
            stack.getTag().remove(COOLDOWN_REMAINING_TAG);
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return getCooldownRemaining(stack) > 0 || super.isBarVisible(stack);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        int remaining = getCooldownRemaining(stack);
        if (remaining <= 0) {
            return super.getBarWidth(stack);
        }

        float progress = 1.0F - remaining / (float) STEALTH_STRIKE_COOLDOWN_TICKS;
        return Math.round(13.0F * progress);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        int remaining = getCooldownRemaining(stack);
        if (remaining <= 0) {
            return super.getBarColor(stack);
        }

        float progress = 1.0F - remaining / (float) STEALTH_STRIKE_COOLDOWN_TICKS;
        return Mth.hsvToRgb(0.76F - progress * 0.25F, 0.82F, 1.0F);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack dagger = player.getItemInHand(hand);

        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResultHolder.pass(dagger);
        }

        if (level.isClientSide) {
            if (isStrikeReady(level, player, dagger)) {
                player.swing(hand);
                return InteractionResultHolder.consume(dagger);
            }
            return InteractionResultHolder.pass(dagger);
        }

        if (player instanceof ServerPlayer serverPlayer && tryStrike(serverPlayer, hand)) {
            return InteractionResultHolder.consume(dagger);
        }
        return InteractionResultHolder.pass(dagger);
    }

    public static void trySecondaryStrike(ServerPlayer player) {
        if (!player.getOffhandItem().is(ModItems.GOLDEN_BLUE_DAGGER.get())) {
            return;
        }

        tryStrike(player, InteractionHand.OFF_HAND);
    }

    private static boolean tryStrike(ServerPlayer player, InteractionHand hand) {
        ItemStack dagger = player.getItemInHand(hand);
        Level level = player.level();
        long gameTime = level.getGameTime();

        if (!dagger.is(ModItems.GOLDEN_BLUE_DAGGER.get())
                || gameTime < dagger.getOrCreateTag().getLong(NEXT_STRIKE_TICK_TAG)) {
            return false;
        }

        LivingEntity target = findTarget(player);
        if (target == null) {
            return false;
        }

        if (!player.getAbilities().instabuild && countToxins(player) == 0) {
            player.sendSystemMessage(Component.literal("Il faut une toxine abyssale pour ce coup.")
                    .withStyle(ChatFormatting.RED));
            return false;
        }

        if (!target.hurt(player.damageSources().playerAttack(player), STEALTH_STRIKE_DAMAGE)) {
            return false;
        }

        consumeToxin(player);
        applyStackingPoison(player, target);
        dagger.getOrCreateTag().putLong(NEXT_STRIKE_TICK_TAG, gameTime + STEALTH_STRIKE_COOLDOWN_TICKS);
        dagger.hurtAndBreak(1, player, brokenPlayer -> brokenPlayer.broadcastBreakEvent(hand));
        player.swing(hand, true);
        player.awardStat(Stats.ITEM_USED.get(ModItems.GOLDEN_BLUE_DAGGER.get()));
        level.playSound(
                null,
                target.blockPosition(),
                SoundEvents.PLAYER_ATTACK_SWEEP,
                SoundSource.PLAYERS,
                0.65F,
                1.45F
        );
        return true;
    }

    private static boolean isStrikeReady(Level level, Player player, ItemStack dagger) {
        return level.getGameTime() >= dagger.getOrCreateTag().getLong(NEXT_STRIKE_TICK_TAG)
                && findTarget(player) != null
                && (player.getAbilities().instabuild || countToxins(player) > 0);
    }

    private static int getCooldownRemaining(ItemStack stack) {
        return stack.getTag() == null ? 0 : stack.getTag().getInt(COOLDOWN_REMAINING_TAG);
    }

    private static LivingEntity findTarget(Player player) {
        Vec3 start = player.getEyePosition();
        Vec3 view = player.getViewVector(1.0F);
        Vec3 end = start.add(view.scale(STRIKE_REACH));
        HitResult blockHit = player.pick(STRIKE_REACH, 1.0F, false);

        if (blockHit.getType() == HitResult.Type.BLOCK) {
            end = blockHit.getLocation();
        }

        Vec3 searchVector = end.subtract(start);
        EntityHitResult entityHit = ProjectileUtil.getEntityHitResult(
                player,
                start,
                end,
                player.getBoundingBox().expandTowards(searchVector).inflate(1.0D),
                GoldenBlueDaggerItem::isValidTarget,
                start.distanceToSqr(end)
        );

        return entityHit != null && entityHit.getEntity() instanceof LivingEntity living ? living : null;
    }

    private static boolean isValidTarget(Entity entity) {
        return entity instanceof LivingEntity living
                && living.isAlive()
                && !living.isSpectator()
                && entity.isPickable();
    }

    private static void applyStackingPoison(ServerPlayer attacker, LivingEntity target) {
        MobEffectInstance current = target.getEffect(ModEffects.ABYSSAL_POISON.get());
        int maxStacks = target instanceof Player ? PLAYER_MAX_STACKS : MOB_MAX_STACKS;
        int currentStacks = current == null ? 0 : current.getAmplifier() + 1;
        int currentDuration = current == null ? 0 : current.getDuration();
        int newStacks = Math.min(maxStacks, currentStacks + 1);
        int newDuration = Math.min(maxStacks * POISON_DURATION_PER_STACK,
                currentDuration + POISON_DURATION_PER_STACK);

        target.getPersistentData().putUUID(AbyssalPoisonEffect.OWNER_TAG, attacker.getUUID());
        target.addEffect(new MobEffectInstance(
                ModEffects.ABYSSAL_POISON.get(),
                newDuration,
                newStacks - 1,
                false,
                true,
                true
        ), attacker);
    }

    private static int countToxins(Player player) {
        int count = 0;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(ModItems.ABYSSAL_TOXIN.get())) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void consumeToxin(Player player) {
        if (player.getAbilities().instabuild) {
            return;
        }

        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack stack = player.getInventory().getItem(slot);
            if (stack.is(ModItems.ABYSSAL_TOXIN.get())) {
                stack.shrink(1);
                return;
            }
        }
    }
}
