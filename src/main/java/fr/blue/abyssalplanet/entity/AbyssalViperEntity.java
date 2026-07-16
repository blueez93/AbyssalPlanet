package fr.blue.abyssalplanet.entity;

import fr.blue.abyssalplanet.AbyssalPlanet;
import fr.blue.abyssalplanet.registry.ModBlocks;
import fr.blue.abyssalplanet.registry.ModDimensions;
import fr.blue.abyssalplanet.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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

public class AbyssalViperEntity extends PathfinderMob {
    private static final double TARGET_RANGE = 52.0D;
    private static final double FOCUS_LOST_RANGE = 80.0D;
    private static final double BITE_RANGE = 2.4D;
    private static final float BITE_DAMAGE = 8.0F;
    private static final int BITE_COOLDOWN_TICKS = 32;
    private static final int SERPENT_DELAY_AFTER_BAIT_TICKS = 20 * 5;
    private static final double SERPENT_INTRO_MIN_SPAWN_DISTANCE = 26.0D;
    private static final double SERPENT_INTRO_MAX_SPAWN_DISTANCE = 48.0D;
    private static final double SERPENT_INTRO_FALLBACK_MAX_DISTANCE = 64.0D;
    private static final double SERPENT_BAIT_ORBIT_RADIUS = 2.6D;
    private static final double SERPENT_BAIT_ORBIT_SPEED = 0.11D;

    @Nullable
    private BlockPos baitPos;
    @Nullable
    private UUID targetUuid;
    private int biteCooldown;
    private int wanderCooldown;
    private boolean baitEaten;
    private boolean summonsSerpent;
    private boolean serpentSummoned;
    private int serpentDelayTicks;

    public AbyssalViperEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 24;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 36.0D)
                .add(Attributes.ATTACK_DAMAGE, BITE_DAMAGE)
                .add(Attributes.FOLLOW_RANGE, TARGET_RANGE)
                .add(Attributes.MOVEMENT_SPEED, 0.24D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.35D);
    }

    @Override
    protected ResourceLocation getDefaultLootTable() {
        return AbyssalPlanet.id("entities/abyssal_viper");
    }

    public static boolean canSpawn(
            EntityType<AbyssalViperEntity> entityType,
            ServerLevelAccessor level,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random
    ) {
        return spawnType == MobSpawnType.TRIGGERED
                && level.getLevel().dimension().equals(ModDimensions.ABYSSAL_PLANET_LEVEL)
                && level.getFluidState(pos).is(FluidTags.WATER)
                && level.getFluidState(pos.above()).is(FluidTags.WATER);
    }

    public void startFromBait(BlockPos baitPos, boolean summonsSerpent) {
        this.baitPos = baitPos.immutable();
        this.baitEaten = false;
        this.summonsSerpent = summonsSerpent;
        this.serpentSummoned = false;
        this.serpentDelayTicks = -1;
        this.setPersistenceRequired();
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public void aiStep() {
        if (!this.level().isClientSide) {
            tickViperAi();
        }

        super.aiStep();
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (this.isInWater()) {
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.82D));
            return;
        }

        super.travel(travelVector);
    }

    private void tickViperAi() {
        if (this.biteCooldown > 0) {
            this.biteCooldown--;
        }

        if (tickSerpentSummon()) {
            return;
        }

        if (!this.baitEaten && this.baitPos != null && isBaitStillThere()) {
            swimToward(Vec3.atCenterOf(this.baitPos), 0.36D);

            if (this.distanceToSqr(Vec3.atCenterOf(this.baitPos)) <= 4.0D) {
                eatBait();
            }
            return;
        }

        this.baitEaten = true;
        if (this.summonsSerpent) {
            tickSerpentBaitOrbit();
            return;
        }

        LivingEntity target = findTarget();

        if (target == null) {
            swimRandomly();
            return;
        }

        swimToward(target.getEyePosition(), this.distanceToSqr(target) <= 36.0D ? 0.27D : 0.38D);
        tryBite(target);
    }

    private boolean tickSerpentSummon() {
        if (!this.summonsSerpent || this.serpentSummoned) {
            return false;
        }

        if (this.serpentDelayTicks < 0) {
            return false;
        }

        this.serpentDelayTicks--;

        if (this.serpentDelayTicks > 0) {
            return false;
        }

        if (summonSerpent()) {
            this.serpentSummoned = true;
        } else {
            this.serpentDelayTicks = 20 * 2;
        }
        return false;
    }

    private boolean isBaitStillThere() {
        if (this.baitPos == null) {
            return false;
        }

        return this.level().getBlockState(this.baitPos).is(ModBlocks.ZWOING_FLESH.get())
                || this.level().getBlockState(this.baitPos).is(ModBlocks.FRESH_ABYSSAL_MEAT.get());
    }

    private void eatBait() {
        if (!(this.level() instanceof ServerLevel serverLevel) || this.baitPos == null) {
            return;
        }

        serverLevel.destroyBlock(this.baitPos, false, this);
        serverLevel.sendParticles(
                ParticleTypes.SQUID_INK,
                this.baitPos.getX() + 0.5D,
                this.baitPos.getY() + 0.25D,
                this.baitPos.getZ() + 0.5D,
                this.summonsSerpent ? 46 : 24,
                0.45D,
                0.2D,
                0.45D,
                0.04D
        );

        this.baitEaten = true;
        this.targetUuid = null;

        if (this.summonsSerpent) {
            this.serpentDelayTicks = SERPENT_DELAY_AFTER_BAIT_TICKS;
        } else {
            this.baitPos = null;
        }
    }

    private void tickSerpentBaitOrbit() {
        this.targetUuid = null;
        if (this.baitPos == null) {
            this.setDeltaMovement(this.getDeltaMovement().scale(0.55D));
            return;
        }

        double angle = this.getId() * 0.73D + this.tickCount * 0.027D;
        double radius = SERPENT_BAIT_ORBIT_RADIUS + Math.sin(this.tickCount * 0.045D) * 0.35D;
        Vec3 anchor = Vec3.atCenterOf(this.baitPos).add(0.0D, 1.15D, 0.0D);
        Vec3 orbitTarget = anchor.add(
                Math.cos(angle) * radius,
                Math.sin(this.tickCount * 0.065D + this.getId()) * 0.28D,
                Math.sin(angle) * radius
        );

        BlockPos orbitBlock = BlockPos.containing(orbitTarget);
        if (!this.level().getFluidState(orbitBlock).is(FluidTags.WATER)
                || !this.level().getFluidState(orbitBlock.above()).is(FluidTags.WATER)) {
            orbitTarget = anchor;
        }

        if (this.distanceToSqr(orbitTarget) > 0.36D) {
            swimToward(orbitTarget, SERPENT_BAIT_ORBIT_SPEED);
        } else {
            this.setDeltaMovement(this.getDeltaMovement().scale(0.72D));
        }
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

    @Nullable
    private LivingEntity getFocusedTarget() {
        if (this.targetUuid == null || !(this.level() instanceof ServerLevel serverLevel)) {
            return null;
        }

        Entity entity = serverLevel.getEntity(this.targetUuid);
        return entity instanceof LivingEntity living ? living : null;
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

        Vec3 direction = toTarget.normalize();
        this.setDeltaMovement(this.getDeltaMovement().scale(0.35D).add(direction.scale(speed)));
        this.hasImpulse = true;
        faceMovement(direction);
    }

    private void tryBite(LivingEntity target) {
        if (this.biteCooldown > 0 || this.distanceToSqr(target) > BITE_RANGE * BITE_RANGE) {
            return;
        }

        this.biteCooldown = BITE_COOLDOWN_TICKS;
        this.swing(InteractionHand.MAIN_HAND, true);

        if (target.hurt(this.damageSources().mobAttack(this), BITE_DAMAGE)) {
            target.addEffect(new MobEffectInstance(MobEffects.POISON, 20 * 5, 0, false, true, true), this);
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * 2, 0, false, true, true), this);
        }
    }

    private void swimRandomly() {
        this.wanderCooldown--;

        if (this.wanderCooldown > 0 && this.getDeltaMovement().lengthSqr() > 0.002D) {
            return;
        }

        double angle = this.random.nextDouble() * Math.PI * 2.0D;
        Vec3 direction = new Vec3(Math.cos(angle), -0.12D + this.random.nextDouble() * 0.24D, Math.sin(angle)).normalize();
        this.setDeltaMovement(direction.scale(0.16D));
        faceMovement(direction);
        this.wanderCooldown = 45 + this.random.nextInt(90);
    }

    private boolean summonSerpent() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return false;
        }

        AbyssalSerpentEntity serpent = ModEntities.ABYSSAL_SERPENT.get().create(serverLevel);
        if (serpent == null) {
            return false;
        }

        Vec3 spawnPosition = findSerpentSpawnPosition(serverLevel);
        if (spawnPosition == null) {
            return false;
        }

        Vec3 toViper = this.getEyePosition().subtract(spawnPosition);
        float yaw = toViper.lengthSqr() > 0.001D
                ? (float) (Mth.atan2(toViper.z, toViper.x) * Mth.RAD_TO_DEG) - 90.0F
                : this.random.nextFloat() * 360.0F;
        serpent.moveTo(spawnPosition.x, spawnPosition.y, spawnPosition.z, yaw, 0.0F);
        serpent.finalizeSpawn(serverLevel, serverLevel.getCurrentDifficultyAt(BlockPos.containing(spawnPosition)), MobSpawnType.TRIGGERED, null, null);
        serpent.startIntroAgainst(this);
        serpent.setPersistenceRequired();
        serverLevel.addFreshEntity(serpent);
        return true;
    }

    @Nullable
    private Vec3 findSerpentSpawnPosition(ServerLevel level) {
        Player nearest = level.getNearestPlayer(this, 96.0D);
        Vec3 awayFromPlayer = nearest == null
                ? randomHorizontalDirection()
                : this.position().subtract(nearest.position()).multiply(1.0D, 0.0D, 1.0D);

        if (awayFromPlayer.lengthSqr() < 0.001D) {
            awayFromPlayer = randomHorizontalDirection();
        }

        Vec3 preferredDirection = awayFromPlayer.normalize();
        Vec3 closeSpawn = findSerpentSpawnPositionInRing(
                level,
                preferredDirection,
                SERPENT_INTRO_MIN_SPAWN_DISTANCE,
                SERPENT_INTRO_MAX_SPAWN_DISTANCE,
                96
        );
        if (closeSpawn != null) {
            return closeSpawn;
        }

        return findSerpentSpawnPositionInRing(
                level,
                preferredDirection,
                SERPENT_INTRO_MAX_SPAWN_DISTANCE,
                SERPENT_INTRO_FALLBACK_MAX_DISTANCE,
                72
        );
    }

    @Nullable
    private Vec3 findSerpentSpawnPositionInRing(
            ServerLevel level,
            Vec3 preferredDirection,
            double minDistance,
            double maxDistance,
            int attempts
    ) {
        double preferredAngle = Math.atan2(preferredDirection.z, preferredDirection.x);
        for (int attempt = 0; attempt < attempts; attempt++) {
            double angleOffset = (this.random.nextDouble() - 0.5D) * 1.25D;
            double baseAngle = preferredAngle + angleOffset;
            double distance = minDistance + this.random.nextDouble() * (maxDistance - minDistance);
            int yOffset = 2 + this.random.nextInt(15);
            BlockPos candidate = BlockPos.containing(
                    this.getX() + Math.cos(baseAngle) * distance,
                    this.getY() + yOffset,
                    this.getZ() + Math.sin(baseAngle) * distance
            );
            Vec3 spawnCenter = Vec3.atCenterOf(candidate);

            if (AbyssalSerpentEntity.hasOpenWaterSpawnSpace(level, candidate)
                    && hasOpenWaterChargeLine(level, spawnCenter, this.getEyePosition())) {
                return spawnCenter;
            }
        }

        return null;
    }

    private boolean hasOpenWaterChargeLine(ServerLevel level, Vec3 start, Vec3 end) {
        Vec3 path = end.subtract(start);
        double distance = path.length();
        if (distance < 1.0D) {
            return true;
        }

        Vec3 step = path.normalize().scale(3.0D);
        int samples = Math.max(1, Mth.ceil(distance / 3.0D));
        Vec3 current = start;
        for (int sample = 0; sample < samples; sample++) {
            current = current.add(step);
            if (!hasSmallWaterPocket(level, BlockPos.containing(current))) {
                return false;
            }
        }

        return true;
    }

    private boolean hasSmallWaterPocket(ServerLevel level, BlockPos center) {
        for (int x = -1; x <= 1; x++) {
            for (int y = -1; y <= 1; y++) {
                for (int z = -1; z <= 1; z++) {
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

    private Vec3 randomHorizontalDirection() {
        double angle = this.random.nextDouble() * Math.PI * 2.0D;
        return new Vec3(Math.cos(angle), 0.0D, Math.sin(angle));
    }

    private void faceMovement(Vec3 direction) {
        this.setYRot((float) (Mth.atan2(direction.z, direction.x) * Mth.RAD_TO_DEG) - 90.0F);
        this.yBodyRot = this.getYRot();
        this.yHeadRot = this.getYRot();
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
    public boolean hurt(DamageSource source, float amount) {
        if (this.summonsSerpent) {
            if (!(source.getEntity() instanceof AbyssalSerpentEntity)) {
                return false;
            }

            this.invulnerableTime = 0;
            amount = Math.max(amount, this.getMaxHealth() + this.getAbsorptionAmount() + 1.0F);
        }

        return super.hurt(source, amount);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.baitPos != null) {
            tag.putLong("BaitPos", this.baitPos.asLong());
        }
        if (this.targetUuid != null) {
            tag.putUUID("Target", this.targetUuid);
        }
        tag.putBoolean("BaitEaten", this.baitEaten);
        tag.putBoolean("SummonsSerpent", this.summonsSerpent);
        tag.putBoolean("SerpentSummoned", this.serpentSummoned);
        tag.putInt("SerpentDelayTicks", this.serpentDelayTicks);
        tag.putInt("BiteCooldown", this.biteCooldown);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.baitPos = tag.contains("BaitPos") ? BlockPos.of(tag.getLong("BaitPos")) : null;
        this.targetUuid = tag.hasUUID("Target") ? tag.getUUID("Target") : null;
        this.baitEaten = tag.getBoolean("BaitEaten");
        this.summonsSerpent = tag.getBoolean("SummonsSerpent");
        this.serpentSummoned = tag.getBoolean("SerpentSummoned");
        this.serpentDelayTicks = tag.getInt("SerpentDelayTicks");
        this.biteCooldown = tag.getInt("BiteCooldown");
    }
}
