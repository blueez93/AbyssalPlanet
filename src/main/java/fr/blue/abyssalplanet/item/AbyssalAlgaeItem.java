package fr.blue.abyssalplanet.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class AbyssalAlgaeItem extends BlockItem {
    private static final int USE_DURATION_TICKS = 32;
    private static final int QUICK_PLACE_MAX_TICKS = 8;
    private static final int EXPERIENCE_REWARD = 5;

    public AbyssalAlgaeItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null) {
            return InteractionResult.FAIL;
        }

        player.startUsingItem(context.getHand());
        return InteractionResult.sidedSuccess(context.getLevel().isClientSide);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(player.getItemInHand(hand));
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeLeft) {
        if (!(livingEntity instanceof Player player)
                || getUseDuration(stack) - timeLeft > QUICK_PLACE_MAX_TICKS) {
            return;
        }

        BlockHitResult hitResult = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        if (hitResult.getType() != HitResult.Type.BLOCK) {
            return;
        }

        InteractionHand hand = player.getMainHandItem() == stack
                ? InteractionHand.MAIN_HAND
                : InteractionHand.OFF_HAND;
        place(new BlockPlaceContext(new UseOnContext(player, hand, hitResult)));
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        ItemStack result = super.finishUsingItem(stack, level, livingEntity);

        if (!level.isClientSide) {
            livingEntity.addEffect(new MobEffectInstance(MobEffects.POISON, 20 * 5, 0));
            if (livingEntity instanceof Player player) {
                player.giveExperiencePoints(EXPERIENCE_REWARD);
            }
        }

        return result;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.EAT;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return USE_DURATION_TICKS;
    }
}
