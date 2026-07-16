package fr.blue.abyssalplanet.entity;

import fr.blue.abyssalplanet.item.AbyssalWhistleItem;
import fr.blue.abyssalplanet.registry.ModItems;
import fr.blue.abyssalplanet.world.AbyssalCompanionData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;

import java.util.Comparator;
import java.util.UUID;

public final class GeorgesSeniorEntity extends AbstractAbyssalCompanionEntity {
    private static final EntityDataAccessor<Boolean> SADDLED =
            SynchedEntityData.defineId(GeorgesSeniorEntity.class, EntityDataSerializers.BOOLEAN);
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("animation.georges_briochard.idle");
    private static final RawAnimation SWIM = RawAnimation.begin().thenLoop("animation.georges_briochard.swim");
    private static final RawAnimation BITE = RawAnimation.begin().thenPlay("animation.georges_briochard.bite");
    private static final int BITE_COOLDOWN_TICKS = 20;
    private static final double UNMOUNTED_MAX_WATER_SPEED = 0.29D;
    private static final double RIDDEN_MAX_WATER_SPEED = 0.45D;
    private static final float RIDDEN_WATER_ACCELERATION = 0.045F;
    private static final double RIDDEN_WATER_DRAG = 0.90D;
    private int biteCooldown;
    private int autonomousAttackCooldown;
    private float permanentHealthBonus;

    public GeorgesSeniorEntity(EntityType<? extends GeorgesSeniorEntity> type, Level level) {
        super(type, level);
        xpReward = 0;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 150.0D)
                .add(Attributes.ATTACK_DAMAGE, 12.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.22D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.ARMOR, 5.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        entityData.define(SADDLED, false);
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0D, true));
        targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
    }

    @Override
    public void aiStep() {
        if (biteCooldown > 0) {
            biteCooldown--;
        }
        if (autonomousAttackCooldown > 0) {
            autonomousAttackCooldown--;
        }
        super.aiStep();
        if (!level().isClientSide) {
            tickAutonomousCombat();
        }
        if (!level().isClientSide && getControllingPassenger() instanceof ServerPlayer rider) {
            rider.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 40, 0, false, false, true));
            rider.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 240, 0, false, false, true));
        }
    }

    private void tickAutonomousCombat() {
        LivingEntity target = getTarget();
        if (target == null || !target.isAlive() || target.level() != level()
                || isVehicle() || isOrderedToSit()) {
            return;
        }
        double distanceSquared = distanceToSqr(target);
        if (distanceSquared > 48.0D * 48.0D) {
            setTarget(null);
            return;
        }
        getLookControl().setLookAt(target, 35.0F, 30.0F);
        if (isInWaterOrBubble() && distanceSquared > 3.6D * 3.6D) {
            moveControl.setWantedPosition(
                    target.getX(),
                    target.getEyeY() - 0.4D,
                    target.getZ(),
                    1.25D
            );
        }
        if (distanceSquared <= 4.2D * 4.2D && autonomousAttackCooldown == 0) {
            doHurtTarget(target);
            autonomousAttackCooldown = 24;
        }
    }

    @Override
    protected double getMaximumWaterSpeed() {
        return isVehicle() ? RIDDEN_MAX_WATER_SPEED : UNMOUNTED_MAX_WATER_SPEED;
    }

    @Override
    protected double getLandFollowSpeed() {
        return 0.62D;
    }

    @Override
    protected void tickLandHop() {
        if (onGround() && tickCount % 22 == 0) {
            setDeltaMovement(getDeltaMovement().add(0.0D, 0.58D, 0.0D));
            hasImpulse = true;
        }
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.is(ModItems.BROKEN_ABYSSAL_WHISTLE.get())
                && (!isTame() || isOwnedBy(player))) {
            if (!level().isClientSide) {
                if (!isTame()) {
                    tameFor(player);
                }
                ItemStack repaired = AbyssalWhistleItem.createLinked(getUUID());
                player.setItemInHand(hand, repaired);
                AbyssalCompanionData.get(((ServerLevel) level()).getServer()).markSeniorAlive(getUUID());
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }

        if (held.is(ModItems.ABYSSAL_COMPOST.get()) && isTame() && isOwnedBy(player)) {
            if (!level().isClientSide && getHealth() < getMaxHealth()) {
                heal(2.0F);
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                level().broadcastEntityEvent(this, (byte) 7);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }

        if (held.is(Items.SADDLE) && isTame() && isOwnedBy(player) && !isSaddled()) {
            if (!level().isClientSide) {
                setSaddled(true);
                if (!player.getAbilities().instabuild) {
                    held.shrink(1);
                }
                playSound(SoundEvents.HORSE_SADDLE, 0.7F, 0.8F);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }

        if (held.isEmpty() && isSaddled() && isTame() && isOwnedBy(player) && !player.isShiftKeyDown()) {
            if (!level().isClientSide) {
                setOrderedToSit(false);
                player.startRiding(this);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    @Override
    protected void tickFollowOwner(@Nullable ServerPlayer owner) {
        LivingEntity combatTarget = getTarget();
        if (combatTarget != null && combatTarget.isAlive() && !isVehicle()) {
            return;
        }
        super.tickFollowOwner(owner);
    }

    public boolean performRiderBite(ServerPlayer rider) {
        if (biteCooldown > 0 || rider.getVehicle() != this || !isOwnedBy(rider)) {
            return false;
        }
        biteCooldown = BITE_COOLDOWN_TICKS;
        triggerAnim("actions", "bite");
        playSound(SoundEvents.FOX_BITE, 1.1F, 0.65F);

        Vec3 look = rider.getLookAngle().normalize();
        LivingEntity target = level().getEntitiesOfClass(
                        LivingEntity.class,
                        getBoundingBox().inflate(6.0D),
                        candidate -> candidate != this
                                && candidate != rider
                                && candidate.isAlive()
                                && !isAlliedTo(candidate)
                ).stream()
                .filter(candidate -> {
                    Vec3 direction = candidate.getEyePosition().subtract(rider.getEyePosition()).normalize();
                    return direction.dot(look) > 0.55D;
                })
                .min(Comparator.comparingDouble(this::distanceToSqr))
                .orElse(null);
        if (target == null) {
            return true;
        }

        boolean damaged = target.hurt(damageSources().mobAttack(this), 12.0F);
        if (damaged && !target.isAlive() && target instanceof Mob) {
            increasePermanentHealth(1.0F);
        }
        return true;
    }

    private void increasePermanentHealth(float amount) {
        permanentHealthBonus += amount;
        AttributeInstance maxHealth = getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(150.0D + permanentHealthBonus);
            heal(amount);
        }
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.HEART,
                    getX(),
                    getY() + getBbHeight() * 0.8D,
                    getZ(),
                    10,
                    0.8D,
                    0.6D,
                    0.8D,
                    0.05D
            );
        }
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        triggerAnim("actions", "bite");
        return super.doHurtTarget(target);
    }

    public boolean isSaddled() {
        return entityData.get(SADDLED);
    }

    public void setSaddled(boolean saddled) {
        entityData.set(SADDLED, saddled);
    }

    @Nullable
    @Override
    public LivingEntity getControllingPassenger() {
        Entity passenger = getFirstPassenger();
        return isSaddled() && passenger instanceof Player player ? player : null;
    }

    @Override
    protected void tickRidden(Player rider, Vec3 input) {
        super.tickRidden(rider, input);
        setRot(rider.getYRot(), rider.getXRot() * 0.45F);
        yRotO = yBodyRot = yHeadRot = getYRot();
    }

    @Override
    protected Vec3 getRiddenInput(Player rider, Vec3 input) {
        double vertical = isInWaterOrBubble()
                ? rider.getLookAngle().y * Math.max(0.0F, rider.zza)
                : input.y;
        return new Vec3(rider.xxa * 0.55D, vertical, rider.zza);
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (isControlledByLocalInstance() && isVehicle() && isInWater()) {
            moveRelative(RIDDEN_WATER_ACCELERATION, travelVector);
            Vec3 movement = getDeltaMovement();
            if (movement.lengthSqr() > RIDDEN_MAX_WATER_SPEED * RIDDEN_MAX_WATER_SPEED) {
                movement = movement.normalize().scale(RIDDEN_MAX_WATER_SPEED);
            }
            move(MoverType.SELF, movement);
            setDeltaMovement(movement.scale(RIDDEN_WATER_DRAG));
            return;
        }
        super.travel(travelVector);
    }

    @Override
    protected float getRiddenSpeed(Player rider) {
        return isInWaterOrBubble() ? RIDDEN_WATER_ACCELERATION : 0.18F;
    }

    @Override
    public double getPassengersRidingOffset() {
        return getBbHeight() * 0.72D;
    }

    @Override
    public void die(DamageSource source) {
        if (level() instanceof ServerLevel serverLevel) {
            AbyssalCompanionData.get(serverLevel.getServer()).markSeniorDead(getUUID());
            UUID ownerUuid = getOwnerUUID();
            if (ownerUuid != null) {
                ServerPlayer owner = serverLevel.getServer().getPlayerList().getPlayer(ownerUuid);
                if (owner != null) {
                    AbyssalWhistleItem.breakLinkedWhistles(owner, getUUID());
                }
            }
        }
        super.die(source);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean("Saddled", isSaddled());
        tag.putFloat("PermanentHealthBonus", permanentHealthBonus);
        tag.putInt("BiteCooldown", biteCooldown);
        tag.putInt("AutonomousAttackCooldown", autonomousAttackCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setSaddled(tag.getBoolean("Saddled"));
        permanentHealthBonus = Math.max(0.0F, tag.getFloat("PermanentHealthBonus"));
        biteCooldown = Math.max(0, tag.getInt("BiteCooldown"));
        autonomousAttackCooldown = Math.max(0, tag.getInt("AutonomousAttackCooldown"));
        AttributeInstance maxHealth = getAttribute(Attributes.MAX_HEALTH);
        if (maxHealth != null) {
            maxHealth.setBaseValue(150.0D + permanentHealthBonus);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 6, state -> {
            if (getDeltaMovement().lengthSqr() > 0.001D) {
                return state.setAndContinue(SWIM);
            }
            return state.setAndContinue(IDLE);
        }));
        controllers.add(new AnimationController<>(this, "actions", 1, state -> PlayState.STOP)
                .triggerableAnim("bite", BITE));
    }
}
