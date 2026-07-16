package fr.blue.abyssalplanet.entity;

import fr.blue.abyssalplanet.registry.ModDimensions;
import fr.blue.abyssalplanet.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
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

import java.util.List;
import java.util.UUID;

public class KrakenBossEntity extends Squid implements GeoEntity {
    private static final byte CHARGE_SHAKE_EVENT = 64;

    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("animation.kraken_boss.idle");
    private static final RawAnimation SWIM_ANIM = RawAnimation.begin().thenLoop("animation.kraken_boss.swim");
    private static final RawAnimation ATTACK_ANIM = RawAnimation.begin().thenPlay("animation.kraken_boss.attack");
    private static final RawAnimation ROAR_ANIM = RawAnimation.begin().thenPlay("animation.kraken_boss.roar");
    private static final RawAnimation CHARGE_ANIM = RawAnimation.begin().thenPlay("animation.kraken_boss.charge");
    private static final RawAnimation DASH_OUT_ANIM = RawAnimation.begin().thenPlay("animation.kraken_boss.dash_out");
    private static final RawAnimation INK_CAST_ANIM = RawAnimation.begin().thenPlay("animation.kraken_boss.ink_cast");

    private static final double DETECTION_RANGE = 72.0D;
    private static final double FOCUS_LOST_RANGE = 115.0D;
    private static final double TENTACLE_ATTACK_RANGE = 18.0D;
    private static final double INK_BALL_MIN_RANGE = 18.0D;
    private static final double INK_BALL_MAX_RANGE = 64.0D;
    private static final double CLOSE_SWIM_SLOW_RANGE = 12.0D;
    private static final float MELEE_DAMAGE = 13.0F;
    private static final int MELEE_COOLDOWN_TICKS = 58;
    private static final int INK_BALL_COOLDOWN_TICKS = 20 * 5;
    private static final int INK_COOLDOWN_TICKS = 20 * 10;
    private static final int RANGED_ATTACKS_BEFORE_CHARGE = 3;
    private static final int TENTACLE_ATTACKS_BEFORE_DASH_OUT = 20;
    private static final int PREDATORY_HUNT_TICKS = 20 * 20;
    private static final double PREDATORY_HUNT_CLOSE_RADIUS = 9.0D;
    private static final double PREDATORY_HUNT_WIDE_RADIUS = 36.0D;
    private static final int OPENING_CHARGE_TICKS = 26;
    private static final int DASH_OUT_CHARGE_TICKS = 30;
    private static final double OPENING_CHARGE_SPEED = 1.35D;
    private static final double DASH_OUT_CHARGE_SPEED = 1.28D;
    private static final double OPENING_CHARGE_HIT_RANGE = 12.0D;
    private static final double DASH_OUT_TARGET_DISTANCE = 54.0D;
    private static final float OPENING_CHARGE_DAMAGE = 4.0F;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private final ServerBossEvent bossEvent = (ServerBossEvent) new ServerBossEvent(
            this.getDisplayName(),
            BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.NOTCHED_10
    ).setDarkenScreen(true).setCreateWorldFog(true);

    private int meleeCooldown;
    private int inkBallCooldown;
    private int inkCooldown;
    private int wanderCooldown;
    private int lastPhase = 1;
    private int rangedAttackCounter;
    private int tentacleAttackCounter;
    private int predatoryHuntTicks;
    private int openingChargeTicks;
    private int clientChargeShakeTicks;
    private boolean predatoryHuntStarted;
    private boolean openingChargeDone;
    private boolean openingChargeStartsCombat;
    private boolean dashOutCharge;
    private boolean combatStarted;
    private boolean openingChargeHit;
    @Nullable
    private UUID focusedTargetUuid;
    @Nullable
    private UUID predatoryHuntTargetUuid;
    @Nullable
    private UUID openingChargeTargetUuid;

    public KrakenBossEntity(EntityType<? extends Squid> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 75;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 420.0D)
                .add(Attributes.ATTACK_DAMAGE, MELEE_DAMAGE)
                .add(Attributes.FOLLOW_RANGE, DETECTION_RANGE)
                .add(Attributes.ARMOR, 8.0D);
    }

    public static boolean canSpawn(EntityType<KrakenBossEntity> entityType, ServerLevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        return level.getLevel().dimension().equals(ModDimensions.ABYSSAL_PLANET_LEVEL)
                && level.getFluidState(pos).is(FluidTags.WATER)
                && level.getFluidState(pos.above()).is(FluidTags.WATER)
                && level.getFluidState(pos.above(2)).is(FluidTags.WATER);
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public void aiStep() {
        if (!this.level().isClientSide) {
            tickBossAi();
        }

        super.aiStep();

        if (this.level().isClientSide) {
            if (this.clientChargeShakeTicks > 0) {
                this.clientChargeShakeTicks--;
            }

            if (this.tickCount % 5 == 0) {
                this.level().addParticle(
                        ParticleTypes.SOUL_FIRE_FLAME,
                        this.getX() + (this.random.nextDouble() - 0.5D) * 5.0D,
                        this.getY() + 1.5D + this.random.nextDouble() * 2.2D,
                        this.getZ() + (this.random.nextDouble() - 0.5D) * 5.0D,
                        0.0D,
                        0.01D,
                        0.0D
                );
            }
        }
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();

        if (this.combatStarted) {
            this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
            updateBossbarPhase();
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isPredatoryHuntActive()) {
            if (!this.level().isClientSide && this.level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                        ParticleTypes.SQUID_INK,
                        this.getX(),
                        this.getY() + 1.0D,
                        this.getZ(),
                        12,
                        1.6D,
                        0.8D,
                        1.6D,
                        0.03D
                );
            }

            return false;
        }

        boolean hurt = super.hurt(source, amount);

        if (hurt && !this.level().isClientSide) {
            releaseInkCloud();
            updatePhaseBurst();
        }

        return hurt;
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(this.getDisplayName());
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);

        if (this.combatStarted) {
            this.bossEvent.addPlayer(player);
        }
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.lastPhase = Math.max(1, tag.getInt("LastPhase"));
        this.meleeCooldown = tag.getInt("MeleeCooldown");
        this.inkBallCooldown = tag.getInt("InkBallCooldown");
        this.inkCooldown = tag.getInt("InkCooldown");
        this.rangedAttackCounter = tag.getInt("RangedAttackCounter");
        this.tentacleAttackCounter = tag.getInt("TentacleAttackCounter");
        this.predatoryHuntTicks = tag.getInt("PredatoryHuntTicks");
        this.predatoryHuntStarted = tag.getBoolean("PredatoryHuntStarted");
        this.openingChargeTicks = tag.getInt("OpeningChargeTicks");
        this.openingChargeDone = tag.getBoolean("OpeningChargeDone");
        this.openingChargeStartsCombat = tag.getBoolean("OpeningChargeStartsCombat");
        this.dashOutCharge = tag.getBoolean("DashOutCharge");
        this.combatStarted = tag.getBoolean("CombatStarted");
        this.openingChargeHit = tag.getBoolean("OpeningChargeHit");

        if (this.combatStarted) {
            this.predatoryHuntStarted = true;
            this.predatoryHuntTicks = 0;
            this.openingChargeDone = true;
            this.openingChargeTicks = 0;
        }

        if (tag.hasUUID("FocusedTarget")) {
            this.focusedTargetUuid = tag.getUUID("FocusedTarget");
        }

        if (tag.hasUUID("PredatoryHuntTarget")) {
            this.predatoryHuntTargetUuid = tag.getUUID("PredatoryHuntTarget");
        }

        if (tag.hasUUID("OpeningChargeTarget")) {
            this.openingChargeTargetUuid = tag.getUUID("OpeningChargeTarget");
        }

        if (this.hasCustomName()) {
            this.bossEvent.setName(this.getDisplayName());
        }
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("LastPhase", this.lastPhase);
        tag.putInt("MeleeCooldown", this.meleeCooldown);
        tag.putInt("InkBallCooldown", this.inkBallCooldown);
        tag.putInt("InkCooldown", this.inkCooldown);
        tag.putInt("RangedAttackCounter", this.rangedAttackCounter);
        tag.putInt("TentacleAttackCounter", this.tentacleAttackCounter);
        tag.putInt("PredatoryHuntTicks", this.predatoryHuntTicks);
        tag.putBoolean("PredatoryHuntStarted", this.predatoryHuntStarted);
        tag.putInt("OpeningChargeTicks", this.openingChargeTicks);
        tag.putBoolean("OpeningChargeDone", this.openingChargeDone);
        tag.putBoolean("OpeningChargeStartsCombat", this.openingChargeStartsCombat);
        tag.putBoolean("DashOutCharge", this.dashOutCharge);
        tag.putBoolean("CombatStarted", this.combatStarted);
        tag.putBoolean("OpeningChargeHit", this.openingChargeHit);

        if (this.focusedTargetUuid != null) {
            tag.putUUID("FocusedTarget", this.focusedTargetUuid);
        }

        if (this.predatoryHuntTargetUuid != null) {
            tag.putUUID("PredatoryHuntTarget", this.predatoryHuntTargetUuid);
        }

        if (this.openingChargeTargetUuid != null) {
            tag.putUUID("OpeningChargeTarget", this.openingChargeTargetUuid);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 8, this::movementController));

        AnimationController<KrakenBossEntity> actions = new AnimationController<>(
                this,
                "actions",
                2,
                state -> PlayState.STOP
        );
        actions.triggerableAnim("attack", ATTACK_ANIM)
                .triggerableAnim("roar", ROAR_ANIM)
                .triggerableAnim("charge", CHARGE_ANIM)
                .triggerableAnim("dash_out", DASH_OUT_ANIM)
                .triggerableAnim("ink_cast", INK_CAST_ANIM);
        controllers.add(actions);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }

    private PlayState movementController(AnimationState<KrakenBossEntity> state) {
        if (this.getDeltaMovement().lengthSqr() > 0.0025D) {
            return state.setAndContinue(SWIM_ANIM);
        }

        return state.setAndContinue(IDLE_ANIM);
    }

    private void tickBossAi() {
        if (this.meleeCooldown > 0) {
            this.meleeCooldown--;
        }

        if (this.inkBallCooldown > 0) {
            this.inkBallCooldown--;
        }

        if (this.inkCooldown > 0) {
            this.inkCooldown--;
        }

        Player target = findFocusedTarget();

        if (target == null) {
            resetIntroIfNoTarget();
            patrolArrivalZone();
            return;
        }

        if (!this.predatoryHuntStarted) {
            startPredatoryHunt(target);
            return;
        }

        if (this.predatoryHuntTicks > 0) {
            tickPredatoryHunt(target);
            return;
        }

        if (!this.openingChargeDone) {
            startOpeningCharge(target);
            return;
        }

        if (this.openingChargeTicks > 0) {
            if (this.dashOutCharge) {
                tickDashOutCharge(target);
            } else {
                tickOpeningCharge(target);
            }
            return;
        }

        swimToward(target);

        double distanceSqr = this.distanceToSqr(target);

        if (distanceSqr <= TENTACLE_ATTACK_RANGE * TENTACLE_ATTACK_RANGE) {
            tryTentacleStrike(target);
        } else if (distanceSqr >= INK_BALL_MIN_RANGE * INK_BALL_MIN_RANGE) {
            tryInkBallAttack(target);
        }
    }

    @Nullable
    private Player findFocusedTarget() {
        Player focusedTarget = getFocusedTarget();

        if (focusedTarget != null) {
            if (isValidTarget(focusedTarget) && this.distanceToSqr(focusedTarget) <= FOCUS_LOST_RANGE * FOCUS_LOST_RANGE) {
                return focusedTarget;
            }

            this.focusedTargetUuid = null;
        }

        Player nearestTarget = findNearestTarget();

        if (nearestTarget != null) {
            this.focusedTargetUuid = nearestTarget.getUUID();
        }

        return nearestTarget;
    }

    @Nullable
    private Player getFocusedTarget() {
        if (this.focusedTargetUuid == null || !(this.level() instanceof ServerLevel serverLevel)) {
            return null;
        }

        Entity entity = serverLevel.getEntity(this.focusedTargetUuid);
        return entity instanceof Player player ? player : null;
    }

    @Nullable
    private Player findNearestTarget() {
        AABB searchBox = this.getBoundingBox().inflate(DETECTION_RANGE);
        List<Player> players = this.level().getEntitiesOfClass(Player.class, searchBox, player ->
                isValidTarget(player)
                        && this.distanceToSqr(player) <= DETECTION_RANGE * DETECTION_RANGE
        );

        Player nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Player player : players) {
            double distance = this.distanceToSqr(player);

            if (distance < nearestDistance) {
                nearest = player;
                nearestDistance = distance;
            }
        }

        return nearest;
    }

    private boolean isValidTarget(Player player) {
        return player.isAlive()
                && !player.isCreative()
                && !player.isSpectator()
                && player.level().dimension().equals(this.level().dimension());
    }

    private void swimToward(Player target) {
        Vec3 toTarget = target.getEyePosition().subtract(this.position());

        if (toTarget.lengthSqr() < 0.001D) {
            return;
        }

        Vec3 direction = toTarget.normalize();
        float speed = getCurrentPhase() >= 3 ? 0.20F : getCurrentPhase() == 2 ? 0.17F : 0.14F;

        if (this.distanceToSqr(target) < CLOSE_SWIM_SLOW_RANGE * CLOSE_SWIM_SLOW_RANGE) {
            speed *= 0.45F;
        }

        this.setMovementVector(
                (float) direction.x * speed,
                (float) direction.y * speed,
                (float) direction.z * speed
        );
    }

    private void patrolArrivalZone() {
        this.wanderCooldown--;

        if (this.wanderCooldown > 0 && this.hasMovementVector()) {
            return;
        }

        Vec3 arrival = new Vec3(0.5D, 90.0D, 0.5D);
        Vec3 toArrival = arrival.subtract(this.position());

        if (toArrival.horizontalDistanceSqr() > 95.0D * 95.0D) {
            Vec3 direction = toArrival.normalize();
            this.setMovementVector((float) direction.x * 0.11F, (float) direction.y * 0.06F, (float) direction.z * 0.11F);
        } else {
            float angle = this.random.nextFloat() * ((float) Math.PI * 2.0F);
            this.setMovementVector(
                    (float) Math.cos(angle) * 0.08F,
                    -0.02F + this.random.nextFloat() * 0.05F,
                    (float) Math.sin(angle) * 0.08F
            );
        }

        this.wanderCooldown = 55 + this.random.nextInt(90);
    }

    private void tryTentacleStrike(Player target) {
        if (this.meleeCooldown > 0 || this.distanceToSqr(target) > TENTACLE_ATTACK_RANGE * TENTACLE_ATTACK_RANGE) {
            return;
        }

        int phase = getCurrentPhase();
        this.meleeCooldown = Math.max(34, MELEE_COOLDOWN_TICKS - (phase - 1) * 7);
        this.swing(InteractionHand.MAIN_HAND, true);
        this.triggerAnim("actions", "attack");

        if (this.level() instanceof ServerLevel serverLevel) {
            KrakenTentacleEntity.spawn(serverLevel, this, target, MELEE_DAMAGE + (phase - 1) * 3.0F, phase);
            this.tentacleAttackCounter++;

            if (this.tentacleAttackCounter >= TENTACLE_ATTACKS_BEFORE_DASH_OUT) {
                startDashOutCharge(target);
            }
        }
    }

    private void tryInkBallAttack(Player target) {
        double distanceSqr = this.distanceToSqr(target);

        if (this.inkBallCooldown > 0 || distanceSqr > INK_BALL_MAX_RANGE * INK_BALL_MAX_RANGE) {
            return;
        }

        int phase = getCurrentPhase();
        this.inkBallCooldown = Math.max(20 * 3, INK_BALL_COOLDOWN_TICKS - (phase - 1) * 18);
        this.triggerAnim("actions", "ink_cast");

        if (this.level() instanceof ServerLevel serverLevel) {
            KrakenInkBallEntity.spawn(serverLevel, this, target, phase);
            this.rangedAttackCounter++;
            serverLevel.sendParticles(
                    ParticleTypes.SQUID_INK,
                    this.getX(),
                    this.getY() + 1.2D,
                    this.getZ(),
                    32,
                    2.0D,
                    0.8D,
                    2.0D,
                    0.07D
            );

            if (this.rangedAttackCounter >= RANGED_ATTACKS_BEFORE_CHARGE) {
                startCombatCharge(target);
            }
        }
    }

    private void startPredatoryHunt(Player target) {
        this.predatoryHuntStarted = true;
        this.predatoryHuntTicks = PREDATORY_HUNT_TICKS;
        this.predatoryHuntTargetUuid = target.getUUID();
        this.openingChargeDone = false;
        this.openingChargeTicks = 0;
        this.openingChargeTargetUuid = null;
        this.meleeCooldown = 20 * 3;
        this.inkBallCooldown = 20 * 4;
        this.triggerAnim("actions", "roar");
        playForcedKrakenSound(target, ModSounds.KRAKEN_HUNT_ROAR.get(), 1.7F, 1.0F);
        tickPredatoryHunt(target);
    }

    private void tickPredatoryHunt(Player fallbackTarget) {
        Player target = getPredatoryHuntTarget();

        if (target == null) {
            target = fallbackTarget;
        }

        if (target == null || !isValidTarget(target) || this.distanceToSqr(target) > FOCUS_LOST_RANGE * FOCUS_LOST_RANGE) {
            resetIntroIfNoTarget();
            return;
        }

        int elapsed = PREDATORY_HUNT_TICKS - this.predatoryHuntTicks;
        double directionSign = (this.getUUID().getLeastSignificantBits() & 1L) == 0L ? 1.0D : -1.0D;
        double angle = elapsed * 0.095D * directionSign;
        double radiusPulse = (Math.sin(elapsed * 0.145D) + 1.0D) * 0.5D;
        double radius = PREDATORY_HUNT_CLOSE_RADIUS
                + radiusPulse * (PREDATORY_HUNT_WIDE_RADIUS - PREDATORY_HUNT_CLOSE_RADIUS);
        double yOffset = Math.sin(elapsed * 0.08D) * 5.0D;
        Vec3 orbitPoint = target.position().add(
                Math.cos(angle) * radius,
                2.0D + yOffset,
                Math.sin(angle) * radius
        );
        Vec3 toOrbitPoint = orbitPoint.subtract(this.position());

        if (toOrbitPoint.lengthSqr() > 0.001D) {
            Vec3 direction = toOrbitPoint.normalize();
            double speed = ((elapsed / 45) % 2 == 0) ? 0.34D : 0.58D;

            if (toOrbitPoint.lengthSqr() > 18.0D * 18.0D) {
                speed += 0.12D;
            }

            this.setMovementVector(
                    (float) direction.x * (float) speed,
                    (float) direction.y * (float) (speed * 0.55D),
                    (float) direction.z * (float) speed
            );
        }

        if (this.level() instanceof ServerLevel serverLevel && this.tickCount % 8 == 0) {
            serverLevel.sendParticles(
                    ParticleTypes.SQUID_INK,
                    this.getX(),
                    this.getY() + 1.2D,
                    this.getZ(),
                    10,
                    2.4D,
                    0.8D,
                    2.4D,
                    0.04D
            );
        }

        this.predatoryHuntTicks--;

        if (this.predatoryHuntTicks <= 0) {
            this.predatoryHuntTicks = 0;
            this.predatoryHuntTargetUuid = null;
            startOpeningCharge(target);
        }
    }

    @Nullable
    private Player getPredatoryHuntTarget() {
        if (this.predatoryHuntTargetUuid == null || !(this.level() instanceof ServerLevel serverLevel)) {
            return null;
        }

        Entity entity = serverLevel.getEntity(this.predatoryHuntTargetUuid);
        return entity instanceof Player player ? player : null;
    }

    private void resetIntroIfNoTarget() {
        if (this.combatStarted) {
            return;
        }

        this.predatoryHuntStarted = false;
        this.predatoryHuntTicks = 0;
        this.predatoryHuntTargetUuid = null;
        this.openingChargeDone = false;
        this.openingChargeTicks = 0;
        this.openingChargeTargetUuid = null;
        this.openingChargeStartsCombat = false;
        this.dashOutCharge = false;
        this.openingChargeHit = false;
    }

    private void startOpeningCharge(Player target) {
        this.openingChargeDone = true;
        this.openingChargeStartsCombat = true;
        startChargeTowardTarget(target, OPENING_CHARGE_TICKS);
    }

    private void startCombatCharge(Player target) {
        this.rangedAttackCounter = 0;
        this.openingChargeStartsCombat = false;
        startChargeTowardTarget(target, OPENING_CHARGE_TICKS);
    }

    private void startChargeTowardTarget(Player target, int durationTicks) {
        this.openingChargeTicks = durationTicks;
        this.openingChargeHit = false;
        this.openingChargeTargetUuid = target.getUUID();
        this.dashOutCharge = false;
        this.meleeCooldown = 20 * 2;
        this.inkBallCooldown = 20 * 3;
        this.swing(InteractionHand.MAIN_HAND, true);
        this.triggerAnim("actions", "charge");
        this.level().broadcastEntityEvent(this, CHARGE_SHAKE_EVENT);
        playForcedKrakenSound(target, ModSounds.KRAKEN_ATTACK_SCREAM.get(), 1.45F, 1.0F);
        tickOpeningCharge(target);
    }

    private void startDashOutCharge(Player target) {
        this.tentacleAttackCounter = 0;
        this.openingChargeStartsCombat = false;
        this.dashOutCharge = true;
        this.openingChargeTicks = DASH_OUT_CHARGE_TICKS;
        this.openingChargeHit = true;
        this.openingChargeTargetUuid = target.getUUID();
        this.meleeCooldown = 20 * 4;
        this.inkBallCooldown = 20;
        this.swing(InteractionHand.MAIN_HAND, true);
        this.triggerAnim("actions", "dash_out");
        this.level().broadcastEntityEvent(this, CHARGE_SHAKE_EVENT);
        playForcedKrakenSound(target, ModSounds.KRAKEN_ATTACK_SCREAM.get(), 1.25F, 0.92F);
        tickDashOutCharge(target);
    }

    private void tickOpeningCharge(Player fallbackTarget) {
        Player target = getOpeningChargeTarget();

        if (target == null) {
            target = fallbackTarget;
        }

        if (target == null || !isValidTarget(target)) {
            finishOpeningCharge();
            return;
        }

        Vec3 toTarget = target.getEyePosition().subtract(this.position());

        if (toTarget.lengthSqr() < 0.001D) {
            finishOpeningCharge();
            return;
        }

        Vec3 direction = toTarget.normalize();
        this.setMovementVector(
                (float) direction.x * (float) OPENING_CHARGE_SPEED,
                (float) direction.y * (float) (OPENING_CHARGE_SPEED * 0.55D),
                (float) direction.z * (float) OPENING_CHARGE_SPEED
        );
        this.setDeltaMovement(direction.scale(OPENING_CHARGE_SPEED * 0.72D));
        this.hasImpulse = true;

        if (!this.openingChargeHit && this.distanceToSqr(target) <= OPENING_CHARGE_HIT_RANGE * OPENING_CHARGE_HIT_RANGE) {
            this.openingChargeHit = true;
            target.hurt(this.damageSources().mobAttack(this), OPENING_CHARGE_DAMAGE);
            Vec3 push = target.position().subtract(this.position()).normalize().scale(1.6D);
            target.setDeltaMovement(target.getDeltaMovement().add(push.x, 0.35D, push.z));
            target.hurtMarked = true;
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * 2, 0, false, true, true));
        }

        if (this.level() instanceof ServerLevel serverLevel && this.tickCount % 2 == 0) {
            serverLevel.sendParticles(
                    ParticleTypes.SQUID_INK,
                    this.getX(),
                    this.getY() + 1.2D,
                    this.getZ(),
                    18,
                    3.5D,
                    1.0D,
                    3.5D,
                    0.08D
            );
        }

        this.openingChargeTicks--;

        if (this.openingChargeTicks <= 0 || this.distanceToSqr(target) <= OPENING_CHARGE_HIT_RANGE * OPENING_CHARGE_HIT_RANGE * 0.7D) {
            finishOpeningCharge();
        }
    }

    private void tickDashOutCharge(Player fallbackTarget) {
        Player target = getOpeningChargeTarget();

        if (target == null) {
            target = fallbackTarget;
        }

        if (target == null || !isValidTarget(target)) {
            finishOpeningCharge();
            return;
        }

        Vec3 awayFromTarget = this.position().subtract(target.position()).multiply(1.0D, 0.25D, 1.0D);

        if (awayFromTarget.lengthSqr() < 0.001D) {
            awayFromTarget = new Vec3(
                    this.random.nextDouble() - 0.5D,
                    0.08D,
                    this.random.nextDouble() - 0.5D
            );
        }

        Vec3 direction = awayFromTarget.normalize();
        this.setMovementVector(
                (float) direction.x * (float) DASH_OUT_CHARGE_SPEED,
                (float) direction.y * (float) (DASH_OUT_CHARGE_SPEED * 0.55D),
                (float) direction.z * (float) DASH_OUT_CHARGE_SPEED
        );
        this.setDeltaMovement(direction.scale(DASH_OUT_CHARGE_SPEED * 0.65D));
        this.hasImpulse = true;

        if (this.level() instanceof ServerLevel serverLevel && this.tickCount % 2 == 0) {
            serverLevel.sendParticles(
                    ParticleTypes.SQUID_INK,
                    this.getX(),
                    this.getY() + 1.2D,
                    this.getZ(),
                    18,
                    3.5D,
                    1.0D,
                    3.5D,
                    0.08D
            );
        }

        this.openingChargeTicks--;

        if (this.openingChargeTicks <= 0 || this.distanceToSqr(target) >= DASH_OUT_TARGET_DISTANCE * DASH_OUT_TARGET_DISTANCE) {
            finishOpeningCharge();
        }
    }

    @Nullable
    private Player getOpeningChargeTarget() {
        if (this.openingChargeTargetUuid == null || !(this.level() instanceof ServerLevel serverLevel)) {
            return null;
        }

        Entity entity = serverLevel.getEntity(this.openingChargeTargetUuid);
        return entity instanceof Player player ? player : null;
    }

    private void finishOpeningCharge() {
        boolean wasDashOutCharge = this.dashOutCharge;
        this.openingChargeTicks = 0;
        this.openingChargeTargetUuid = null;
        this.predatoryHuntTicks = 0;
        this.predatoryHuntTargetUuid = null;
        this.dashOutCharge = false;

        if (this.openingChargeStartsCombat || !this.combatStarted) {
            this.combatStarted = true;
            showBossbarToNearbyPlayers();
        }

        this.openingChargeStartsCombat = false;
        this.meleeCooldown = Math.max(this.meleeCooldown, wasDashOutCharge ? 20 * 3 : 20);
        this.inkBallCooldown = Math.max(this.inkBallCooldown, wasDashOutCharge ? 15 : 40);
    }

    private void playForcedKrakenSound(Player target, SoundEvent sound, float volume, float pitch) {
        if (target instanceof ServerPlayer serverPlayer) {
            serverPlayer.playNotifySound(sound, SoundSource.HOSTILE, volume, pitch);
            return;
        }

        this.level().playSound(
                null,
                target.getX(),
                target.getY(),
                target.getZ(),
                sound,
                SoundSource.HOSTILE,
                volume,
                pitch
        );
    }

    private boolean isPredatoryHuntActive() {
        return this.predatoryHuntStarted && this.predatoryHuntTicks > 0 && !this.combatStarted;
    }

    private void showBossbarToNearbyPlayers() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        for (ServerPlayer player : serverLevel.players()) {
            if (player.level().dimension().equals(this.level().dimension()) && player.distanceToSqr(this) <= 128.0D * 128.0D) {
                this.bossEvent.addPlayer(player);
            }
        }
    }

    private void releaseInkCloud() {
        if (this.inkCooldown > 0 || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        this.inkCooldown = INK_COOLDOWN_TICKS;
        this.playSound(this.getSquirtSound(), this.getSoundVolume(), this.getVoicePitch());

        serverLevel.sendParticles(
                ParticleTypes.SQUID_INK,
                this.getX(),
                this.getY() + 1.0D,
                this.getZ(),
                120,
                3.0D,
                1.2D,
                3.0D,
                0.08D
        );

        for (int i = 0; i < 3; i++) {
            Vec3 offset = new Vec3(
                    (this.random.nextDouble() - 0.5D) * 6.0D,
                    -0.4D + this.random.nextDouble(),
                    (this.random.nextDouble() - 0.5D) * 6.0D
            );
            AbyssalInkPuddleEntity.spawn(serverLevel, this.position().add(offset));
        }
    }

    private void updatePhaseBurst() {
        int phase = getCurrentPhase();

        if (phase <= this.lastPhase) {
            return;
        }

        this.lastPhase = phase;
        this.inkBallCooldown = 20;
        this.inkCooldown = 0;
        this.triggerAnim("actions", "roar");

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.SOUL_FIRE_FLAME,
                    this.getX(),
                    this.getY() + 2.0D,
                    this.getZ(),
                    110,
                    4.0D,
                    2.0D,
                    4.0D,
                    0.08D
            );
        }
    }

    private void updateBossbarPhase() {
        int phase = getCurrentPhase();

        if (phase >= 3) {
            this.bossEvent.setColor(BossEvent.BossBarColor.RED);
        } else if (phase == 2) {
            this.bossEvent.setColor(BossEvent.BossBarColor.BLUE);
        } else {
            this.bossEvent.setColor(BossEvent.BossBarColor.PURPLE);
        }
    }

    private int getCurrentPhase() {
        float healthRatio = this.getHealth() / this.getMaxHealth();

        if (healthRatio <= 0.33F) {
            return 3;
        }

        if (healthRatio <= 0.66F) {
            return 2;
        }

        return 1;
    }

    @Override
    public boolean canChangeDimensions() {
        return false;
    }

    @Override
    protected boolean canRide(Entity entity) {
        return false;
    }

    @Override
    public void handleEntityEvent(byte eventId) {
        if (eventId == CHARGE_SHAKE_EVENT) {
            this.clientChargeShakeTicks = 16;
            return;
        }

        super.handleEntityEvent(eventId);
    }

    public float getClientChargeShakeStrength(float partialTick) {
        if (this.clientChargeShakeTicks <= 0) {
            return 0.0F;
        }

        return Math.min(1.0F, (this.clientChargeShakeTicks - partialTick) / 16.0F);
    }
}
