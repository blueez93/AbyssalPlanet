package fr.blue.abyssalplanet.entity;

import fr.blue.abyssalplanet.registry.ModDimensions;
import fr.blue.abyssalplanet.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class AbyssalSerpentEntity extends PathfinderMob {
    private static final byte SERPENT_SHAKE_EVENT = 72;
    private static final EntityDataAccessor<Boolean> STUNNED =
            SynchedEntityData.defineId(AbyssalSerpentEntity.class, EntityDataSerializers.BOOLEAN);

    private static final double TARGET_RANGE = 110.0D;
    private static final double FOCUS_LOST_RANGE = 160.0D;
    private static final double FORCED_SOUND_RANGE = 180.0D;
    private static final double INTRO_CHARGE_SPEED = 1.75D;
    private static final double PLAYER_CHARGE_SPEED = 1.45D;
    private static final double LEAVE_SPEED = 1.05D;
    private static final double STUN_DASH_OUT_SPEED = 1.70D;
    private static final double HIT_RANGE = 5.8D;
    private static final int LEAVE_TICKS_AFTER_VIPER = 20 * 4;
    private static final int LEAVE_TICKS_AFTER_ATTACK = 20 * 3;
    private static final int PLAYER_CHARGE_TICKS = 20 * 4;
    private static final int ATTACK_PAUSE_TICKS = 20 * 2;
    private static final int STUN_DURATION_TICKS = 20 * 3;
    private static final int STUN_DASH_OUT_TICKS = 20 * 2;
    private static final float MAX_DAMAGE_PER_HIT = 2.0F;
    private static final float PLAYER_MAX_HEALTH_DAMAGE_FRACTION = 0.35F;
    private static final int SPAWN_CLEAR_HORIZONTAL_RADIUS = 5;
    private static final int SPAWN_CLEAR_BELOW = 2;
    private static final int SPAWN_CLEAR_ABOVE = 3;

    private final ServerBossEvent bossEvent = (ServerBossEvent) new ServerBossEvent(
            Component.translatable("entity.abyssalplanet.abyssal_serpent"),
            BossEvent.BossBarColor.BLUE,
            BossEvent.BossBarOverlay.NOTCHED_6
    ).setDarkenScreen(true).setCreateWorldFog(true);

    @Nullable
    private UUID introViperUuid;
    @Nullable
    private UUID targetUuid;
    private int leaveTicks;
    private int chargeTicks;
    private int attackPauseTicks;
    private int stunTicks;
    private int clientShakeTicks;
    private boolean introComplete;
    private boolean combatStarted;
    private boolean hitDuringCharge;
    private boolean stunAvailable;
    private boolean dashAfterStun;
    private boolean boostedDashOut;
    private Vec3 leaveDirection = Vec3.ZERO;

    public AbyssalSerpentEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 120;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 70.0D)
                .add(Attributes.ATTACK_DAMAGE, 18.0D)
                .add(Attributes.FOLLOW_RANGE, TARGET_RANGE)
                .add(Attributes.MOVEMENT_SPEED, 0.30D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.9D);
    }

    public static boolean canSpawn(
            EntityType<AbyssalSerpentEntity> entityType,
            ServerLevelAccessor level,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random
    ) {
        return spawnType == MobSpawnType.TRIGGERED
                && level.getLevel().dimension().equals(ModDimensions.ABYSSAL_PLANET_LEVEL)
                && hasOpenWaterSpawnSpace(level, pos);
    }

    public static boolean hasOpenWaterSpawnSpace(ServerLevelAccessor level, BlockPos center) {
        for (int x = -SPAWN_CLEAR_HORIZONTAL_RADIUS; x <= SPAWN_CLEAR_HORIZONTAL_RADIUS; x++) {
            for (int y = -SPAWN_CLEAR_BELOW; y <= SPAWN_CLEAR_ABOVE; y++) {
                for (int z = -SPAWN_CLEAR_HORIZONTAL_RADIUS; z <= SPAWN_CLEAR_HORIZONTAL_RADIUS; z++) {
                    BlockPos checkPos = center.offset(x, y, z);
                    if (!level.getFluidState(checkPos).is(FluidTags.WATER)
                            || !level.getBlockState(checkPos).getCollisionShape(level, checkPos).isEmpty()) {
                        return false;
                    }
                }
            }
        }

        return true;
    }

    public void startIntroAgainst(AbyssalViperEntity viper) {
        this.introViperUuid = viper.getUUID();
        this.introComplete = false;
        this.combatStarted = false;
        this.chargeTicks = Mth.clamp((int) (this.distanceTo(viper) * 5.0F), 20 * 6, 20 * 14);
        this.hitDuringCharge = false;
        this.setPersistenceRequired();
        Vec3 direction = viper.getEyePosition().subtract(this.position());
        if (direction.lengthSqr() > 0.001D) {
            Vec3 normalized = direction.normalize();
            faceMovement(normalized);
            this.setDeltaMovement(normalized.scale(INTRO_CHARGE_SPEED * 0.8D));
            this.hasImpulse = true;
        }
        broadcastShakeAndRoar();
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(STUNNED, false);
    }

    @Override
    public void aiStep() {
        if (!this.level().isClientSide) {
            tickSerpentAi();
        } else if (this.clientShakeTicks > 0) {
            this.clientShakeTicks--;
        }

        if (this.level().isClientSide && this.tickCount % 3 == 0) {
            this.level().addParticle(
                    ParticleTypes.SCULK_SOUL,
                    this.getX() + (this.random.nextDouble() - 0.5D) * 4.0D,
                    this.getY() + 0.8D + this.random.nextDouble() * 1.5D,
                    this.getZ() + (this.random.nextDouble() - 0.5D) * 4.0D,
                    0.0D,
                    0.02D,
                    0.0D
            );
        }

        super.aiStep();
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.combatStarted) {
            this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
        }
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (this.isInWater()) {
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.88D));
            return;
        }

        super.travel(travelVector);
    }

    private void tickSerpentAi() {
        if (!this.introComplete) {
            tickIntroCharge();
            return;
        }

        if (this.stunTicks > 0) {
            tickStunned();
            return;
        }

        if (this.leaveTicks > 0) {
            this.leaveTicks--;
            swimDirection(this.leaveDirection, this.boostedDashOut ? STUN_DASH_OUT_SPEED : LEAVE_SPEED);
            if (this.leaveTicks == 0) {
                this.boostedDashOut = false;
            }
            return;
        }

        if (this.attackPauseTicks > 0) {
            this.attackPauseTicks--;
            return;
        }

        if (this.chargeTicks > 0) {
            tickPlayerCharge();
            return;
        }

        LivingEntity target = findTarget();
        if (target == null) {
            swimDirection(this.leaveDirection.lengthSqr() > 0.001D ? this.leaveDirection : randomHorizontalDirection(), 0.35D);
            return;
        }

        startPlayerCharge(target);
    }

    private void tickIntroCharge() {
        LivingEntity viper = getIntroViper();

        if (viper == null || !viper.isAlive()) {
            finishIntro(null);
            return;
        }

        swimToward(viper.getEyePosition(), INTRO_CHARGE_SPEED);
        this.chargeTicks--;

        if (this.distanceToSqr(viper) <= HIT_RANGE * HIT_RANGE || this.chargeTicks <= 0) {
            viper.hurt(this.damageSources().mobAttack(this), 1000.0F);
            if (viper.isAlive()) {
                viper.kill();
            }
            finishIntro(viper.position());
        }
    }

    private void finishIntro(@Nullable Vec3 viperPosition) {
        this.introComplete = true;
        this.combatStarted = true;
        this.introViperUuid = null;
        this.chargeTicks = 0;
        this.leaveTicks = LEAVE_TICKS_AFTER_VIPER;
        this.stunTicks = 0;
        this.stunAvailable = false;
        this.dashAfterStun = false;
        this.boostedDashOut = false;
        this.entityData.set(STUNNED, false);
        this.leaveDirection = chooseLeaveDirection(viperPosition);
        showBossbarToNearbyPlayers();
        broadcastShakeAndRoar();
    }

    private void startPlayerCharge(LivingEntity target) {
        this.targetUuid = target.getUUID();
        this.chargeTicks = PLAYER_CHARGE_TICKS;
        this.hitDuringCharge = false;
        this.stunAvailable = true;
        this.dashAfterStun = false;
        this.boostedDashOut = false;
        this.swing(InteractionHand.MAIN_HAND, true);
        broadcastShakeAndRoar();
    }

    private void tickPlayerCharge() {
        LivingEntity target = getFocusedTarget();

        if (target == null || !isValidTarget(target)) {
            finishPlayerCharge(null);
            return;
        }

        swimToward(target.getEyePosition(), PLAYER_CHARGE_SPEED);
        this.chargeTicks--;

        if (!this.hitDuringCharge && this.distanceToSqr(target) <= HIT_RANGE * HIT_RANGE) {
            this.hitDuringCharge = true;
            applySerpentHit(target);
            finishPlayerCharge(target.position());
            return;
        }

        if (this.chargeTicks <= 0) {
            finishPlayerCharge(target.position());
        }
    }

    private void finishPlayerCharge(@Nullable Vec3 targetPosition) {
        this.targetUuid = null;
        this.chargeTicks = 0;
        this.leaveTicks = LEAVE_TICKS_AFTER_ATTACK;
        this.attackPauseTicks = ATTACK_PAUSE_TICKS;
        this.boostedDashOut = false;
        this.leaveDirection = chooseLeaveDirection(targetPosition);
    }

    private void beginStun(DamageSource source) {
        boolean interruptedDashOut = this.leaveTicks > 0 && this.leaveDirection.lengthSqr() > 0.001D;
        if (!interruptedDashOut) {
            Entity attacker = source.getEntity();
            LivingEntity focusedTarget = getFocusedTarget();
            Vec3 dangerPosition = attacker != null
                    ? attacker.position()
                    : focusedTarget != null ? focusedTarget.position() : null;
            this.leaveDirection = chooseLeaveDirection(dangerPosition);
        }

        this.stunTicks = STUN_DURATION_TICKS;
        this.stunAvailable = false;
        this.dashAfterStun = true;
        this.boostedDashOut = false;
        this.entityData.set(STUNNED, true);
        this.targetUuid = null;
        this.chargeTicks = 0;
        this.leaveTicks = 0;
        this.attackPauseTicks = 0;
        this.hitDuringCharge = false;
        this.navigation.stop();
        this.setDeltaMovement(Vec3.ZERO);
        this.hasImpulse = true;
        releaseStunBurst();
    }

    private void tickStunned() {
        this.navigation.stop();
        this.setDeltaMovement(this.getDeltaMovement().scale(0.08D));
        this.hasImpulse = true;

        if (this.level() instanceof ServerLevel serverLevel && this.stunTicks % 5 == 0) {
            serverLevel.sendParticles(
                    ParticleTypes.ELECTRIC_SPARK,
                    this.getX(),
                    this.getY() + this.getBbHeight() * 0.55D,
                    this.getZ(),
                    9,
                    2.6D,
                    1.2D,
                    2.6D,
                    0.035D
            );
        }

        this.stunTicks--;
        if (this.stunTicks == 0) {
            this.entityData.set(STUNNED, false);
        }
        if (this.stunTicks == 0 && this.dashAfterStun) {
            this.dashAfterStun = false;
            this.boostedDashOut = true;
            this.leaveTicks = STUN_DASH_OUT_TICKS;
            this.attackPauseTicks = ATTACK_PAUSE_TICKS;
            swimDirection(this.leaveDirection, STUN_DASH_OUT_SPEED);
        }
    }

    private void releaseStunBurst() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        serverLevel.sendParticles(
                ParticleTypes.ELECTRIC_SPARK,
                this.getX(),
                this.getY() + this.getBbHeight() * 0.55D,
                this.getZ(),
                42,
                3.2D,
                1.6D,
                3.2D,
                0.08D
        );
        serverLevel.sendParticles(
                ParticleTypes.BUBBLE_POP,
                this.getX(),
                this.getY() + this.getBbHeight() * 0.5D,
                this.getZ(),
                30,
                2.8D,
                1.3D,
                2.8D,
                0.12D
        );
    }

    private void applySerpentHit(LivingEntity target) {
        if (target instanceof Player player) {
            float damage = player.getMaxHealth() * PLAYER_MAX_HEALTH_DAMAGE_FRACTION;
            float before = player.getHealth();

            if (before <= damage) {
                player.hurt(this.damageSources().mobAttack(this), 1000.0F);
                if (!player.isAlive()) {
                    this.heal(this.getMaxHealth());
                }
                return;
            }

            player.invulnerableTime = 0;
            player.hurt(this.damageSources().mobAttack(this), 0.01F);
            player.setHealth(Math.max(1.0F, before - damage));
            player.hurtMarked = true;
            return;
        }

        target.hurt(this.damageSources().mobAttack(this), 18.0F);
    }

    @Nullable
    private LivingEntity getIntroViper() {
        if (this.introViperUuid == null || !(this.level() instanceof ServerLevel serverLevel)) {
            return null;
        }

        Entity entity = serverLevel.getEntity(this.introViperUuid);
        return entity instanceof LivingEntity living ? living : null;
    }

    @Nullable
    private LivingEntity getFocusedTarget() {
        if (this.targetUuid == null || !(this.level() instanceof ServerLevel serverLevel)) {
            return null;
        }

        Entity entity = serverLevel.getEntity(this.targetUuid);
        return entity instanceof LivingEntity living ? living : null;
    }

    @Nullable
    private LivingEntity findTarget() {
        LivingEntity focused = getFocusedTarget();
        if (focused != null && isValidTarget(focused) && this.distanceToSqr(focused) <= FOCUS_LOST_RANGE * FOCUS_LOST_RANGE) {
            return focused;
        }

        AABB box = this.getBoundingBox().inflate(TARGET_RANGE);
        List<Player> players = this.level().getEntitiesOfClass(Player.class, box, player ->
                isValidTarget(player) && this.distanceToSqr(player) <= TARGET_RANGE * TARGET_RANGE
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

        this.targetUuid = nearest == null ? null : nearest.getUUID();
        return nearest;
    }

    private boolean isValidTarget(LivingEntity target) {
        return target.isAlive()
                && target.level().dimension().equals(this.level().dimension())
                && (!(target instanceof Player player) || (!player.isCreative() && !player.isSpectator()));
    }

    private void swimToward(Vec3 targetPosition, double speed) {
        Vec3 toTarget = targetPosition.subtract(this.position());
        if (toTarget.lengthSqr() < 0.001D) {
            return;
        }

        swimDirection(toTarget.normalize(), speed);
    }

    private void swimDirection(Vec3 direction, double speed) {
        if (direction.lengthSqr() < 0.001D) {
            return;
        }

        Vec3 normalized = direction.normalize();
        Vec3 sideWave = new Vec3(-normalized.z, 0.0D, normalized.x)
                .scale(Mth.sin(this.tickCount * 0.32F) * 0.14D);
        Vec3 swimVector = normalized.add(sideWave).normalize();
        this.setDeltaMovement(this.getDeltaMovement().scale(0.22D).add(swimVector.scale(speed)));
        this.hasImpulse = true;
        faceMovement(swimVector);

        if (this.level() instanceof ServerLevel serverLevel && this.tickCount % 2 == 0) {
            serverLevel.sendParticles(
                    ParticleTypes.SQUID_INK,
                    this.getX(),
                    this.getY() + 1.0D,
                    this.getZ(),
                    16,
                    2.4D,
                    0.8D,
                    2.4D,
                    0.06D
            );
        }
    }

    private Vec3 chooseLeaveDirection(@Nullable Vec3 dangerPosition) {
        Vec3 direction = dangerPosition == null ? randomHorizontalDirection() : this.position().subtract(dangerPosition);
        direction = direction.multiply(1.0D, 0.0D, 1.0D);
        if (direction.lengthSqr() < 0.001D) {
            direction = randomHorizontalDirection();
        }
        return direction.normalize().add(0.0D, -0.18D, 0.0D).normalize();
    }

    private Vec3 randomHorizontalDirection() {
        double angle = this.random.nextDouble() * Math.PI * 2.0D;
        return new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
    }

    private void faceMovement(Vec3 direction) {
        this.setYRot((float) (Mth.atan2(direction.z, direction.x) * Mth.RAD_TO_DEG) - 90.0F);
        this.yBodyRot = this.getYRot();
        this.yHeadRot = this.getYRot();
    }

    private void broadcastShakeAndRoar() {
        this.level().broadcastEntityEvent(this, SERPENT_SHAKE_EVENT);

        if (!(this.level() instanceof ServerLevel serverLevel)) {
            this.level().playSound(null, this.blockPosition(), ModSounds.KRAKEN_HUNT_ROAR.get(), SoundSource.HOSTILE, 2.0F, 0.65F);
            return;
        }

        for (ServerPlayer player : serverLevel.players()) {
            if (player.level().dimension().equals(this.level().dimension())
                    && player.distanceToSqr(this) <= FORCED_SOUND_RANGE * FORCED_SOUND_RANGE) {
                player.playNotifySound(ModSounds.KRAKEN_HUNT_ROAR.get(), SoundSource.HOSTILE, 1.9F, 0.65F);
            }
        }
    }

    private void showBossbarToNearbyPlayers() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        for (ServerPlayer player : serverLevel.players()) {
            if (player.level().dimension().equals(this.level().dimension()) && player.distanceToSqr(this) <= 160.0D * 160.0D) {
                this.bossEvent.addPlayer(player);
            }
        }
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (amount > MAX_DAMAGE_PER_HIT) {
            amount = MAX_DAMAGE_PER_HIT;
        }

        boolean damaged = super.hurt(source, amount);
        if (damaged
                && !this.level().isClientSide
                && this.isAlive()
                && this.introComplete
                && this.combatStarted
                && this.stunAvailable
                && this.stunTicks == 0
                && source.getEntity() instanceof LivingEntity) {
            beginStun(source);
        }
        return damaged;
    }

    public boolean isStunned() {
        return this.entityData.get(STUNNED);
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
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    protected int decreaseAirSupply(int airSupply) {
        return airSupply;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    public boolean canChangeDimensions() {
        return false;
    }

    @Override
    public void handleEntityEvent(byte eventId) {
        if (eventId == SERPENT_SHAKE_EVENT) {
            this.clientShakeTicks = 28;
            return;
        }

        super.handleEntityEvent(eventId);
    }

    public float getClientShakeStrength(float partialTick) {
        if (this.clientShakeTicks <= 0) {
            return 0.0F;
        }

        return Math.min(1.0F, (this.clientShakeTicks - partialTick) / 28.0F);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.introViperUuid != null) {
            tag.putUUID("IntroViper", this.introViperUuid);
        }
        if (this.targetUuid != null) {
            tag.putUUID("Target", this.targetUuid);
        }
        tag.putInt("LeaveTicks", this.leaveTicks);
        tag.putInt("ChargeTicks", this.chargeTicks);
        tag.putInt("AttackPauseTicks", this.attackPauseTicks);
        tag.putInt("StunTicks", this.stunTicks);
        tag.putBoolean("IntroComplete", this.introComplete);
        tag.putBoolean("CombatStarted", this.combatStarted);
        tag.putBoolean("HitDuringCharge", this.hitDuringCharge);
        tag.putBoolean("StunAvailable", this.stunAvailable);
        tag.putBoolean("DashAfterStun", this.dashAfterStun);
        tag.putBoolean("BoostedDashOut", this.boostedDashOut);
        tag.putDouble("LeaveX", this.leaveDirection.x);
        tag.putDouble("LeaveY", this.leaveDirection.y);
        tag.putDouble("LeaveZ", this.leaveDirection.z);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.introViperUuid = tag.hasUUID("IntroViper") ? tag.getUUID("IntroViper") : null;
        this.targetUuid = tag.hasUUID("Target") ? tag.getUUID("Target") : null;
        this.leaveTicks = tag.getInt("LeaveTicks");
        this.chargeTicks = tag.getInt("ChargeTicks");
        this.attackPauseTicks = tag.getInt("AttackPauseTicks");
        this.stunTicks = Math.max(0, tag.getInt("StunTicks"));
        this.introComplete = tag.getBoolean("IntroComplete");
        this.combatStarted = tag.getBoolean("CombatStarted");
        this.hitDuringCharge = tag.getBoolean("HitDuringCharge");
        this.stunAvailable = tag.contains("StunAvailable")
                ? tag.getBoolean("StunAvailable")
                : this.introComplete && this.chargeTicks > 0;
        this.dashAfterStun = tag.getBoolean("DashAfterStun");
        this.boostedDashOut = tag.getBoolean("BoostedDashOut");
        this.entityData.set(STUNNED, this.stunTicks > 0);
        this.leaveDirection = new Vec3(tag.getDouble("LeaveX"), tag.getDouble("LeaveY"), tag.getDouble("LeaveZ"));

        if (this.hasCustomName()) {
            this.bossEvent.setName(this.getDisplayName());
        }
    }
}
