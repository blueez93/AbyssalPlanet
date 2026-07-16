package fr.blue.abyssalplanet.entity;

import fr.blue.abyssalplanet.item.AbyssalWhistleItem;
import fr.blue.abyssalplanet.registry.ModEntities;
import fr.blue.abyssalplanet.registry.ModItems;
import fr.blue.abyssalplanet.world.AbyssalCompanionData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
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

public final class GeorgesJuniorEntity extends AbstractAbyssalCompanionEntity {
    private static final RawAnimation IDLE =
            RawAnimation.begin().thenLoop("animation.georges_briochard.idle");
    private static final RawAnimation SWIM =
            RawAnimation.begin().thenLoop("animation.georges_briochard.swim");
    private boolean willGrow;
    private int growthFeedings;

    public GeorgesJuniorEntity(EntityType<? extends GeorgesJuniorEntity> type, Level level) {
        super(type, level);
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 30.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.24D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.2D);
    }

    public void setWillGrow(boolean willGrow) {
        this.willGrow = willGrow;
    }

    @Override
    protected double getWaterFollowSpeed() {
        return super.getWaterFollowSpeed() * 0.5D;
    }

    @Override
    protected double getMaximumWaterSpeed() {
        return 0.21D;
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
                    consumeOne(player, held);
                    heal(2.0F);
                    showHearts();
                    return InteractionResult.SUCCESS;
                }
                return InteractionResult.PASS;
            }

            consumeOne(player, held);
            if (willGrow) {
                growthFeedings++;
                showHearts();
                if (growthFeedings >= 5) {
                    growIntoSenior(player);
                }
            } else if (random.nextInt(4) == 0) {
                tameFor(player);
            } else {
                level().broadcastEntityEvent(this, (byte) 6);
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    private void growIntoSenior(Player player) {
        if (!(level() instanceof ServerLevel serverLevel)) {
            return;
        }
        GeorgesSeniorEntity senior = ModEntities.GEORGES_SENIOR.get().create(serverLevel);
        if (senior == null) {
            return;
        }
        senior.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());
        if (hasCustomName()) {
            senior.setCustomName(getCustomName());
        }
        senior.tameFor(player);
        senior.setHealth(senior.getMaxHealth());
        senior.setPersistenceRequired();
        serverLevel.addFreshEntity(senior);
        senior.syncPersistentPetRecord();
        AbyssalCompanionData data = AbyssalCompanionData.get(serverLevel.getServer());
        data.remove(getUUID());
        data.markSeniorAlive(senior.getUUID());

        if (data.grantWhistleOnce(player.getUUID())) {
            ItemStack whistle = AbyssalWhistleItem.createLinked(senior.getUUID());
            if (!player.getInventory().add(whistle)) {
                player.drop(whistle, false);
            }
        }

        serverLevel.sendParticles(
                ParticleTypes.TOTEM_OF_UNDYING,
                getX(),
                getY() + 1.0D,
                getZ(),
                55,
                1.0D,
                1.0D,
                1.0D,
                0.15D
        );
        serverLevel.playSound(
                null,
                blockPosition(),
                SoundEvents.PLAYER_LEVELUP,
                SoundSource.NEUTRAL,
                1.4F,
                0.7F
        );
        discard();
    }

    private void consumeOne(Player player, ItemStack stack) {
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
    }

    private void showHearts() {
        level().broadcastEntityEvent(this, (byte) 7);
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.HEART,
                    getX(),
                    getY() + getBbHeight(),
                    getZ(),
                    7,
                    0.35D,
                    0.3D,
                    0.35D,
                    0.03D
            );
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("WillGrow", willGrow);
        tag.putInt("GrowthFeedings", growthFeedings);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        willGrow = tag.getBoolean("WillGrow");
        growthFeedings = Math.max(0, tag.getInt("GrowthFeedings"));
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 6, state -> {
            if (isInWaterOrBubble() && getDeltaMovement().lengthSqr() > 0.001D) {
                return state.setAndContinue(SWIM);
            }
            return state.setAndContinue(IDLE);
        }));
    }
}
