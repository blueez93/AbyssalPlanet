package fr.blue.abyssalplanet.entity;

import fr.blue.abyssalplanet.registry.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class AbyssalOctopusEntity extends Squid {
    private static final double DETECTION_RANGE = 22.0D;
    private static final double ATTACK_RANGE = 1.7D;
    private static final float ATTACK_DAMAGE = 3.0F;
    private static final int ATTACK_COOLDOWN_TICKS = 25;
    private static final int BLINDNESS_TICKS = 20 * 3;
    private static final int INK_COOLDOWN_TICKS = 45;
    private static final int DEFENSE_DURATION_TICKS = 20 * 20;
    private static final double FOCUS_LOST_RANGE = DETECTION_RANGE * 2.0D;

    private int attackCooldown;
    private int inkCooldown;
    private int wanderCooldown;
    private int defenseTicks;
    @Nullable
    private UUID defensiveTargetUuid;

    public AbyssalOctopusEntity(EntityType<? extends Squid> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 5;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 16.0D)
                .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
                .add(Attributes.FOLLOW_RANGE, DETECTION_RANGE);
    }

    public static boolean canSpawn(EntityType<AbyssalOctopusEntity> entityType, ServerLevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        return level.getLevel().dimension().equals(ModDimensions.ABYSSAL_PLANET_LEVEL)
                && level.getFluidState(pos).is(FluidTags.WATER)
                && level.getFluidState(pos.above()).is(FluidTags.WATER);
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public void aiStep() {
        if (!this.level().isClientSide) {
            tickCombatAi();
        }

        super.aiStep();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);

        if (hurt && !this.level().isClientSide) {
            releaseInk();
            rememberAttacker(source);
        }

        return hurt;
    }

    private void tickCombatAi() {
        if (this.attackCooldown > 0) {
            this.attackCooldown--;
        }

        if (this.inkCooldown > 0) {
            this.inkCooldown--;
        }

        if (this.defenseTicks > 0) {
            this.defenseTicks--;
        }

        LivingEntity target = getDefensiveTarget();

        if (target == null) {
            clearDefensiveTarget();
            swimRandomly();
            return;
        }

        swimToward(target);
        tryAttack(target);
    }

    private void rememberAttacker(DamageSource source) {
        if (!(source.getEntity() instanceof LivingEntity attacker) || !isValidDefensiveTarget(attacker)) {
            return;
        }

        this.defensiveTargetUuid = attacker.getUUID();
        this.defenseTicks = DEFENSE_DURATION_TICKS;
    }

    @Nullable
    private LivingEntity getDefensiveTarget() {
        if (this.defenseTicks <= 0
                || this.defensiveTargetUuid == null
                || !(this.level() instanceof ServerLevel serverLevel)) {
            return null;
        }

        if (!(serverLevel.getEntity(this.defensiveTargetUuid) instanceof LivingEntity target)
                || !isValidDefensiveTarget(target)
                || this.distanceToSqr(target) > FOCUS_LOST_RANGE * FOCUS_LOST_RANGE) {
            return null;
        }

        return target;
    }

    private boolean isValidDefensiveTarget(LivingEntity target) {
        return target != this
                && target.isAlive()
                && target.level().dimension().equals(this.level().dimension())
                && (!(target instanceof Player player) || (!player.isCreative() && !player.isSpectator()));
    }

    private void clearDefensiveTarget() {
        this.defensiveTargetUuid = null;
        this.defenseTicks = 0;
    }

    private void swimToward(LivingEntity target) {
        Vec3 toTarget = target.getEyePosition().subtract(this.position());

        if (toTarget.lengthSqr() < 0.001D) {
            return;
        }

        Vec3 direction = toTarget.normalize();
        float speed = this.distanceToSqr(target) <= 16.0D ? 0.12F : 0.20F;
        this.setMovementVector(
                (float) direction.x * speed,
                (float) direction.y * speed,
                (float) direction.z * speed
        );
    }

    private void tryAttack(LivingEntity target) {
        if (this.attackCooldown > 0 || this.distanceToSqr(target) > ATTACK_RANGE * ATTACK_RANGE) {
            return;
        }

        this.attackCooldown = ATTACK_COOLDOWN_TICKS;

        if (target.hurt(this.damageSources().mobAttack(this), ATTACK_DAMAGE)) {
            target.addEffect(new MobEffectInstance(
                    MobEffects.BLINDNESS,
                    BLINDNESS_TICKS,
                    0,
                    false,
                    true,
                    true
            ));

            releaseInk();
        }
    }

    private void swimRandomly() {
        this.wanderCooldown--;

        if (this.wanderCooldown > 0 && this.hasMovementVector()) {
            return;
        }

        float angle = this.random.nextFloat() * ((float) Math.PI * 2.0F);
        float x = (float) Math.cos(angle) * 0.11F;
        float y = -0.04F + this.random.nextFloat() * 0.08F;
        float z = (float) Math.sin(angle) * 0.11F;

        this.setMovementVector(x, y, z);
        this.wanderCooldown = 40 + this.random.nextInt(80);
    }

    private void releaseInk() {
        if (this.inkCooldown > 0 || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        this.inkCooldown = INK_COOLDOWN_TICKS;
        this.playSound(this.getSquirtSound(), this.getSoundVolume(), this.getVoicePitch());

        serverLevel.sendParticles(
                ParticleTypes.SQUID_INK,
                this.getX(),
                this.getY() + 0.35D,
                this.getZ(),
                36,
                0.55D,
                0.35D,
                0.55D,
                0.08D
        );

        AbyssalInkPuddleEntity.spawn(serverLevel, this.position().add(0.0D, -0.2D, 0.0D));
    }
}
