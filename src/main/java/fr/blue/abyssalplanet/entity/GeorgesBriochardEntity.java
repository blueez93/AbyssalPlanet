package fr.blue.abyssalplanet.entity;

import fr.blue.abyssalplanet.registry.ModBiomes;
import fr.blue.abyssalplanet.registry.ModDimensions;
import fr.blue.abyssalplanet.registry.ModEffects;
import fr.blue.abyssalplanet.world.AbyssalTunnelData;
import fr.blue.abyssalplanet.world.AbyssalTunnelLocator;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;

public final class GeorgesBriochardEntity extends Squid implements GeoEntity {
    private static final EntityDataAccessor<Boolean> ANGRY =
            SynchedEntityData.defineId(GeorgesBriochardEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> LATCHED =
            SynchedEntityData.defineId(GeorgesBriochardEntity.class, EntityDataSerializers.BOOLEAN);

    private static final RawAnimation IDLE_ANIMATION =
            RawAnimation.begin().thenLoop("animation.georges_briochard.idle");
    private static final RawAnimation SWIM_ANIMATION =
            RawAnimation.begin().thenLoop("animation.georges_briochard.swim");
    private static final RawAnimation LATCH_ANIMATION =
            RawAnimation.begin().thenLoop("animation.georges_briochard.latch");
    private static final RawAnimation BITE_ANIMATION =
            RawAnimation.begin().thenPlay("animation.georges_briochard.bite");
    private static final RawAnimation DETACH_ANIMATION =
            RawAnimation.begin().thenPlay("animation.georges_briochard.detach");

    private static final double FOCUS_LOST_RANGE = 112.0D;
    private static final double BITE_RANGE = 4.2D;
    private static final int BITE_COOLDOWN_TICKS = 45;
    private static final int INITIAL_BLEED_DURATION_TICKS = 20 * 3;
    private static final int MAXIMUM_BLEED_DURATION_TICKS = 20 * 16;
    private static final int BLEED_DAMAGE_INTERVAL_TICKS = 20;
    private static final float BLEED_DAMAGE = 2.0F;
    private static final double PASSIVE_SWIM_SPEED = 0.09D;
    private static final double CHASE_SWIM_SPEED = 0.34D;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent bossEvent = (ServerBossEvent) new ServerBossEvent(
            this.getDisplayName(),
            BossEvent.BossBarColor.RED,
            BossEvent.BossBarOverlay.NOTCHED_10
    ).setDarkenScreen(false).setCreateWorldFog(false);

    @Nullable
    private UUID focusedTargetUuid;
    @Nullable
    private UUID latchedTargetUuid;
    @Nullable
    private BlockPos tunnelHome;
    private int biteCooldown;
    private int wanderCooldown;
    private int latchTicks;
    private int currentBleedDuration;

    public GeorgesBriochardEntity(EntityType<? extends Squid> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 90;
        this.bossEvent.setVisible(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 150.0D)
                .add(Attributes.ATTACK_DAMAGE, 2.0D)
                .add(Attributes.FOLLOW_RANGE, FOCUS_LOST_RANGE)
                .add(Attributes.ARMOR, 5.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.65D);
    }

    public static boolean canSpawn(
            EntityType<GeorgesBriochardEntity> entityType,
            ServerLevelAccessor level,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random
    ) {
        return level.getLevel().dimension().equals(ModDimensions.ABYSSAL_PLANET_LEVEL)
                && level.getBiome(pos).is(ModBiomes.DECAY_ROAD)
                && level.getFluidState(pos).is(FluidTags.WATER)
                && level.getFluidState(pos.above()).is(FluidTags.WATER);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(ANGRY, false);
        this.entityData.define(LATCHED, false);
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public void aiStep() {
        if (!this.level().isClientSide) {
            migrateToCurrentTunnelIfNeeded();
            tickBossAi();
        } else if (this.tickCount % 6 == 0) {
            Vec3 forward = this.getLookAngle().scale(1.2D);
            this.level().addParticle(
                    net.minecraft.core.particles.ParticleTypes.GLOW,
                    this.getX() + forward.x,
                    this.getY() + 2.0D,
                    this.getZ() + forward.z,
                    0.0D,
                    0.004D,
                    0.0D
            );
        }

        super.aiStep();
    }

    private void migrateToCurrentTunnelIfNeeded() {
        if (!(this.level() instanceof ServerLevel serverLevel)
                || this.tunnelHome == null
                || !serverLevel.dimension().equals(ModDimensions.ABYSSAL_PLANET_LEVEL)) {
            return;
        }

        BlockPos expectedHome = AbyssalTunnelLocator.getCenter(serverLevel.getSeed());
        if (this.tunnelHome.equals(expectedHome)) {
            return;
        }

        if (!serverLevel.hasChunkAt(expectedHome)) {
            return;
        }

        boolean replacementExists = !serverLevel.getEntitiesOfClass(
                GeorgesBriochardEntity.class,
                new net.minecraft.world.phys.AABB(expectedHome).inflate(
                        AbyssalTunnelLocator.CAVERN_RADIUS,
                        32.0D,
                        AbyssalTunnelLocator.CAVERN_RADIUS
                ),
                georges -> georges != this && georges.isAlive()
        ).isEmpty();
        if (replacementExists) {
            this.discard();
            return;
        }

        if (isLatched()) {
            detachFromTarget(true);
        }
        this.focusedTargetUuid = null;
        this.latchedTargetUuid = null;
        this.entityData.set(ANGRY, false);
        this.entityData.set(LATCHED, false);
        this.setDeltaMovement(Vec3.ZERO);
        this.setMovementVector(0.0F, 0.0F, 0.0F);
        this.tunnelHome = expectedHome.immutable();
        this.moveTo(
                expectedHome.getX() + 0.5D,
                AbyssalTunnelLocator.BOSS_SPAWN_Y,
                expectedHome.getZ() + 0.5D,
                this.getYRot(),
                this.getXRot()
        );
        AbyssalTunnelData.get(serverLevel).markSouthernTunnelMigrated();
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        this.bossEvent.setVisible(isAngry());
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        LivingEntity attacker = source.getEntity() instanceof LivingEntity living ? living : null;

        if (!this.level().isClientSide
                && isLatched()
                && attacker instanceof Player player
                && this.latchedTargetUuid != null
                && !player.getUUID().equals(this.latchedTargetUuid)) {
            detachFromTarget(true);
        }

        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide && attacker != null && isValidTarget(attacker)) {
            if (!isAngry() || getFocusedTarget() == null) {
                focusOn(attacker);
            }
        }
        return hurt;
    }

    @Override
    public void die(DamageSource source) {
        if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
            AbyssalTunnelData.get(serverLevel).markGeorgesDefeated();
        }
        super.die(source);
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    public void setTunnelHome(BlockPos tunnelCenter) {
        this.tunnelHome = tunnelCenter.immutable();
    }

    public boolean isAngry() {
        return this.entityData.get(ANGRY);
    }

    public boolean isLatched() {
        return this.entityData.get(LATCHED);
    }

    private void tickBossAi() {
        if (this.biteCooldown > 0) {
            this.biteCooldown--;
        }

        if (isLatched()) {
            tickLatchedAttack();
            return;
        }

        LivingEntity target = getFocusedTarget();
        if (target == null) {
            clearFocus();
            swimPassively();
            return;
        }

        swimToward(target.getEyePosition(), CHASE_SWIM_SPEED);
        if (this.biteCooldown <= 0 && this.distanceToSqr(target) <= BITE_RANGE * BITE_RANGE) {
            beginLatch(target);
        }
    }

    private void focusOn(LivingEntity target) {
        this.focusedTargetUuid = target.getUUID();
        this.entityData.set(ANGRY, true);
        this.bossEvent.setVisible(true);
    }

    private void clearFocus() {
        this.focusedTargetUuid = null;
        this.entityData.set(ANGRY, false);
        this.bossEvent.setVisible(false);
    }

    @Nullable
    private LivingEntity getFocusedTarget() {
        if (this.focusedTargetUuid == null || !(this.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        if (!(serverLevel.getEntity(this.focusedTargetUuid) instanceof LivingEntity target)
                || !isValidTarget(target)
                || this.distanceToSqr(target) > FOCUS_LOST_RANGE * FOCUS_LOST_RANGE) {
            return null;
        }
        return target;
    }

    @Nullable
    private LivingEntity getLatchedTarget() {
        if (this.latchedTargetUuid == null || !(this.level() instanceof ServerLevel serverLevel)) {
            return null;
        }
        if (serverLevel.getEntity(this.latchedTargetUuid) instanceof LivingEntity target
                && isValidTarget(target)) {
            return target;
        }
        return null;
    }

    private boolean isValidTarget(LivingEntity target) {
        return target instanceof Player player
                && player.isAlive()
                && player.level().dimension().equals(this.level().dimension())
                && !player.isCreative()
                && !player.isSpectator();
    }

    private void beginLatch(LivingEntity target) {
        this.latchedTargetUuid = target.getUUID();
        this.latchTicks = 0;
        this.currentBleedDuration = INITIAL_BLEED_DURATION_TICKS;
        this.entityData.set(LATCHED, true);
        this.setMovementVector(0.0F, 0.0F, 0.0F);
        this.setDeltaMovement(Vec3.ZERO);
        this.triggerAnim("actions", "bite");
        this.playSound(SoundEvents.FOX_BITE, 1.7F, 0.65F);
        updateBleedingMarker(target);
    }

    private void tickLatchedAttack() {
        LivingEntity target = getLatchedTarget();
        if (target == null) {
            detachFromTarget(false);
            return;
        }

        this.latchTicks++;
        anchorToTarget(target);

        if (this.latchTicks % BLEED_DAMAGE_INTERVAL_TICKS == 0) {
            target.hurt(this.damageSources().magic(), BLEED_DAMAGE);
            this.currentBleedDuration = Math.min(
                    MAXIMUM_BLEED_DURATION_TICKS,
                    this.currentBleedDuration + BLEED_DAMAGE_INTERVAL_TICKS
            );
            updateBleedingMarker(target);
        }

        if (!target.isAlive() || this.latchTicks >= this.currentBleedDuration) {
            detachFromTarget(false);
        }
    }

    private void anchorToTarget(LivingEntity target) {
        Vec3 look = target.getLookAngle();
        Vec3 side = new Vec3(-look.z, 0.0D, look.x);
        if (side.lengthSqr() < 0.001D) {
            side = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            side = side.normalize();
        }

        Vec3 anchor = target.position()
                .add(side.scale(1.05D))
                .add(look.scale(-0.35D))
                .add(0.0D, target.getBbHeight() * 0.42D, 0.0D);
        this.setPos(anchor.x, anchor.y, anchor.z);
        this.setDeltaMovement(target.getDeltaMovement());
        this.setYRot(target.getYRot() + 90.0F);
        this.yBodyRot = this.getYRot();
    }

    private void updateBleedingMarker(LivingEntity target) {
        int remaining = Math.max(2, this.currentBleedDuration - this.latchTicks + 5);
        target.addEffect(new MobEffectInstance(
                ModEffects.BLEEDING.get(),
                remaining,
                0,
                false,
                true,
                true
        ));
    }

    private void detachFromTarget(boolean interrupted) {
        LivingEntity target = getLatchedTarget();
        if (target != null) {
            target.removeEffect(ModEffects.BLEEDING.get());
        }

        this.latchedTargetUuid = null;
        this.latchTicks = 0;
        this.currentBleedDuration = 0;
        this.entityData.set(LATCHED, false);
        this.biteCooldown = interrupted ? 55 : BITE_COOLDOWN_TICKS;
        this.triggerAnim("actions", "detach");

        Vec3 backwards = this.getLookAngle().scale(-0.75D).add(0.0D, 0.12D, 0.0D);
        this.setDeltaMovement(backwards);
        this.setMovementVector((float) backwards.x, (float) backwards.y, (float) backwards.z);
    }

    private void swimPassively() {
        if (this.tunnelHome == null) {
            this.tunnelHome = this.blockPosition();
        }

        Vec3 home = Vec3.atCenterOf(this.tunnelHome);
        double horizontalDistanceSqr = square(this.getX() - home.x) + square(this.getZ() - home.z);
        boolean outsideHome = horizontalDistanceSqr > square(AbyssalTunnelLocator.CAVERN_RADIUS - 12.0D)
                || this.getY() < AbyssalTunnelLocator.BOTTOM_FLOOR_Y + 2.0D
                || this.getY() > AbyssalTunnelLocator.CAVERN_CEILING_Y - 2.0D;

        if (outsideHome) {
            swimToward(home.add(0.0D, 5.5D, 0.0D), PASSIVE_SWIM_SPEED * 1.6D);
            this.wanderCooldown = 30;
            return;
        }

        this.wanderCooldown--;
        if (this.wanderCooldown > 0 && this.hasMovementVector()) {
            return;
        }

        float angle = this.random.nextFloat() * ((float) Math.PI * 2.0F);
        float x = (float) Math.cos(angle) * (float) PASSIVE_SWIM_SPEED;
        float y = -0.025F + this.random.nextFloat() * 0.05F;
        float z = (float) Math.sin(angle) * (float) PASSIVE_SWIM_SPEED;
        this.setMovementVector(x, y, z);
        faceMovement(new Vec3(x, y, z));
        this.wanderCooldown = 55 + this.random.nextInt(110);
    }

    private void swimToward(Vec3 targetPosition, double speed) {
        Vec3 direction = targetPosition.subtract(this.position());
        if (direction.lengthSqr() < 0.001D) {
            return;
        }

        direction = direction.normalize();
        this.setMovementVector(
                (float) (direction.x * speed),
                (float) (direction.y * speed),
                (float) (direction.z * speed)
        );
        faceMovement(direction);
    }

    private void faceMovement(Vec3 direction) {
        float yaw = (float) (Mth.atan2(direction.z, direction.x) * Mth.RAD_TO_DEG) - 90.0F;
        this.setYRot(Mth.rotLerp(0.3F, this.getYRot(), yaw));
        this.yBodyRot = this.getYRot();
    }

    private static double square(double value) {
        return value * value;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return isAngry() ? SoundEvents.GUARDIAN_AMBIENT : SoundEvents.COD_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.GUARDIAN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.GUARDIAN_DEATH;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.focusedTargetUuid != null) {
            tag.putUUID("FocusedTarget", this.focusedTargetUuid);
        }
        if (this.tunnelHome != null) {
            tag.putLong("TunnelHome", this.tunnelHome.asLong());
        }
        tag.putInt("BiteCooldown", this.biteCooldown);
        tag.putBoolean("Angry", isAngry());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.focusedTargetUuid = tag.hasUUID("FocusedTarget") ? tag.getUUID("FocusedTarget") : null;
        this.tunnelHome = tag.contains("TunnelHome") ? BlockPos.of(tag.getLong("TunnelHome")) : null;
        this.biteCooldown = Math.max(0, tag.getInt("BiteCooldown"));
        this.latchedTargetUuid = null;
        this.entityData.set(LATCHED, false);
        this.entityData.set(ANGRY, tag.getBoolean("Angry") && this.focusedTargetUuid != null);
        this.setPersistenceRequired();
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5, this::movementController));

        AnimationController<GeorgesBriochardEntity> actions = new AnimationController<>(
                this,
                "actions",
                1,
                state -> PlayState.STOP
        );
        actions.triggerableAnim("bite", BITE_ANIMATION)
                .triggerableAnim("detach", DETACH_ANIMATION);
        controllers.add(actions);
    }

    private PlayState movementController(AnimationState<GeorgesBriochardEntity> state) {
        if (isLatched()) {
            return state.setAndContinue(LATCH_ANIMATION);
        }
        if (this.getDeltaMovement().lengthSqr() > 0.0025D || this.hasMovementVector()) {
            return state.setAndContinue(SWIM_ANIMATION);
        }
        return state.setAndContinue(IDLE_ANIMATION);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }
}
