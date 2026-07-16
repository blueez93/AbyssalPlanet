package fr.blue.abyssalplanet.entity;

import fr.blue.abyssalplanet.registry.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
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

public class MiniKrakenEntity extends Squid implements GeoEntity {
    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("animation.mini_kraken.idle");
    private static final RawAnimation SWIM_ANIM = RawAnimation.begin().thenLoop("animation.mini_kraken.swim");
    private static final RawAnimation ATTACK_ANIM = RawAnimation.begin().thenPlay("animation.mini_kraken.attack");

    private static final double DETECTION_RANGE = 30.0D;
    private static final double ATTACK_RANGE = 2.6D;
    private static final float ATTACK_DAMAGE = 7.0F;
    private static final int ATTACK_COOLDOWN_TICKS = 35;
    private static final int INK_COOLDOWN_TICKS = 70;
    private static final int DEFENSE_DURATION_TICKS = 20 * 20;
    private static final double FOCUS_LOST_RANGE = DETECTION_RANGE * 2.0D;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    private int attackCooldown;
    private int inkCooldown;
    private int wanderCooldown;
    private int defenseTicks;
    @Nullable
    private UUID defensiveTargetUuid;

    public MiniKrakenEntity(EntityType<? extends Squid> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 18;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 42.0D)
                .add(Attributes.ATTACK_DAMAGE, ATTACK_DAMAGE)
                .add(Attributes.FOLLOW_RANGE, DETECTION_RANGE);
    }

    public static boolean canSpawn(EntityType<MiniKrakenEntity> entityType, ServerLevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
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
            releaseInkBurst();
            rememberAttacker(source);
        }

        return hurt;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 5, this::movementController));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }

    private PlayState movementController(AnimationState<MiniKrakenEntity> state) {
        if (this.swinging) {
            return state.setAndContinue(ATTACK_ANIM);
        }

        if (this.getDeltaMovement().lengthSqr() > 0.0025D) {
            return state.setAndContinue(SWIM_ANIM);
        }

        return state.setAndContinue(IDLE_ANIM);
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
        float speed = this.distanceToSqr(target) <= 25.0D ? 0.13F : 0.18F;
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
        this.swing(InteractionHand.MAIN_HAND, true);

        if (target.hurt(this.damageSources().mobAttack(this), ATTACK_DAMAGE)) {
            target.addEffect(new MobEffectInstance(
                    MobEffects.MOVEMENT_SLOWDOWN,
                    20 * 3,
                    1,
                    false,
                    true,
                    true
            ));
            target.addEffect(new MobEffectInstance(
                    MobEffects.BLINDNESS,
                    20 * 2,
                    0,
                    false,
                    true,
                    true
            ));

            releaseInkBurst();
        }
    }

    private void swimRandomly() {
        this.wanderCooldown--;

        if (this.wanderCooldown > 0 && this.hasMovementVector()) {
            return;
        }

        float angle = this.random.nextFloat() * ((float) Math.PI * 2.0F);
        float x = (float) Math.cos(angle) * 0.08F;
        float y = -0.03F + this.random.nextFloat() * 0.06F;
        float z = (float) Math.sin(angle) * 0.08F;

        this.setMovementVector(x, y, z);
        this.wanderCooldown = 50 + this.random.nextInt(100);
    }

    private void releaseInkBurst() {
        if (this.inkCooldown > 0 || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        this.inkCooldown = INK_COOLDOWN_TICKS;
        this.playSound(this.getSquirtSound(), this.getSoundVolume(), this.getVoicePitch());

        serverLevel.sendParticles(
                ParticleTypes.SQUID_INK,
                this.getX(),
                this.getY() + 0.6D,
                this.getZ(),
                52,
                0.8D,
                0.45D,
                0.8D,
                0.08D
        );

        AbyssalInkPuddleEntity.spawn(serverLevel, this.position().add(0.0D, -0.25D, 0.0D));
    }
}
