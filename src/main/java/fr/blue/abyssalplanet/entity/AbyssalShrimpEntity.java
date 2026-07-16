package fr.blue.abyssalplanet.entity;

import fr.blue.abyssalplanet.registry.ModItems;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;

public final class AbyssalShrimpEntity extends AbstractAbyssalCompanionEntity {
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.abyssal_shrimp.idle");
    private static final RawAnimation SWIM = RawAnimation.begin().thenLoop("animation.abyssal_shrimp.swim");
    private static final RawAnimation HOP = RawAnimation.begin().thenLoop("animation.abyssal_shrimp.hop");

    public AbyssalShrimpEntity(EntityType<? extends AbyssalShrimpEntity> type, Level level) {
        super(type, level);
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.18D)
                .add(Attributes.FOLLOW_RANGE, 40.0D);
    }

    @Override
    protected double getMaximumWaterSpeed() {
        return 0.36D;
    }

    @Override
    protected double getLandFollowSpeed() {
        return 0.45D;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.is(ModItems.ABYSSAL_ALGAE.get())) {
            if (level().isClientSide) {
                return InteractionResult.CONSUME;
            }
            if (isTame()) {
                if (getHealth() < getMaxHealth()) {
                    if (!player.getAbilities().instabuild) {
                        held.shrink(1);
                    }
                    heal(2.0F);
                    level().broadcastEntityEvent(this, (byte) 7);
                    return InteractionResult.SUCCESS;
                }
                return InteractionResult.PASS;
            }
            if (!player.getAbilities().instabuild) {
                held.shrink(1);
            }
            if (random.nextInt(4) == 0) {
                tameFor(player);
            } else {
                level().broadcastEntityEvent(this, (byte) 6);
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5, state -> {
            if (isInWaterOrBubble()) {
                return state.setAndContinue(getDeltaMovement().lengthSqr() > 0.001D ? SWIM : IDLE);
            }
            return state.setAndContinue(HOP);
        }));
    }
}
