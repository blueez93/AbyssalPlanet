package fr.blue.abyssalplanet.entity;

import fr.blue.abyssalplanet.registry.ModEntities;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

public class PlayerAbyssalInkBallEntity extends Entity {
    private static final int MAX_LIFETIME_TICKS = 20 * 4;
    private static final double HIT_RADIUS = 0.85D;

    private UUID ownerUuid;
    private float damage = 10.0F;
    private int age;

    public PlayerAbyssalInkBallEntity(EntityType<? extends PlayerAbyssalInkBallEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public static void spawn(ServerLevel level, ServerPlayer owner, float power) {
        PlayerAbyssalInkBallEntity inkBall = ModEntities.PLAYER_ABYSSAL_INK_BALL.get().create(level);
        if (inkBall == null) {
            return;
        }

        Vec3 start = owner.getEyePosition().add(owner.getLookAngle().scale(0.65D));
        Vec3 direction = owner.getLookAngle().normalize();
        inkBall.ownerUuid = owner.getUUID();
        inkBall.damage = 7.0F + power * 8.0F;
        inkBall.moveTo(start.x, start.y, start.z, owner.getYRot(), owner.getXRot());
        inkBall.setDeltaMovement(direction.scale(0.85D + power * 1.25D));
        level.addFreshEntity(inkBall);
    }

    @Override
    public void tick() {
        super.tick();
        this.age++;

        if (this.level().isClientSide) {
            spawnClientTrail();
            return;
        }

        Vec3 start = this.position();
        Vec3 movement = this.getDeltaMovement();
        Vec3 end = start.add(movement);

        BlockHitResult blockHit = this.level().clip(new ClipContext(
                start,
                end,
                ClipContext.Block.COLLIDER,
                ClipContext.Fluid.NONE,
                this
        ));
        if (blockHit.getType() == HitResult.Type.BLOCK) {
            this.setPos(blockHit.getLocation().x, blockHit.getLocation().y, blockHit.getLocation().z);
            burst();
            return;
        }

        LivingEntity hitEntity = findEntityHit(start, end);
        if (hitEntity != null) {
            this.setPos(hitEntity.getX(), hitEntity.getY(0.5D), hitEntity.getZ());
            hitEntity(hitEntity);
            return;
        }

        this.setPos(end.x, end.y, end.z);
        if (this.age > MAX_LIFETIME_TICKS) {
            burst();
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 128.0D * 128.0D;
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.age = tag.getInt("Age");
        this.damage = tag.getFloat("Damage");
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Age", this.age);
        tag.putFloat("Damage", this.damage);
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private LivingEntity findEntityHit(Vec3 start, Vec3 end) {
        AABB hitBox = new AABB(start, end).inflate(HIT_RADIUS);
        List<LivingEntity> entities = this.level().getEntitiesOfClass(LivingEntity.class, hitBox, this::canHitEntity);
        return entities.stream()
                .min(Comparator.comparingDouble(entity -> entity.distanceToSqr(this)))
                .orElse(null);
    }

    private boolean canHitEntity(LivingEntity entity) {
        if (!entity.isAlive() || entity.getUUID().equals(this.ownerUuid)) {
            return false;
        }

        if (entity instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return false;
        }

        return true;
    }

    private void hitEntity(LivingEntity target) {
        DamageSource source = this.damageSources().magic();
        if (this.level() instanceof ServerLevel serverLevel && this.ownerUuid != null) {
            Entity owner = serverLevel.getEntity(this.ownerUuid);
            if (owner instanceof Player player) {
                source = player.damageSources().playerAttack(player);
            }
        }

        target.hurt(source, this.damage);
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 2 * 20, 0, false, true, true));
        target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 2 * 20, 0, false, true, true));

        Vec3 push = target.position().subtract(this.position());
        if (push.lengthSqr() > 0.001D) {
            push = push.normalize().scale(0.45D);
            target.setDeltaMovement(target.getDeltaMovement().add(push.x, 0.12D, push.z));
            target.hurtMarked = true;
        }

        burst();
    }

    private void burst() {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.SQUID_INK,
                    this.getX(),
                    this.getY(),
                    this.getZ(),
                    35,
                    0.65D,
                    0.65D,
                    0.65D,
                    0.06D
            );
            serverLevel.sendParticles(
                    ParticleTypes.SOUL_FIRE_FLAME,
                    this.getX(),
                    this.getY(),
                    this.getZ(),
                    10,
                    0.35D,
                    0.35D,
                    0.35D,
                    0.03D
            );
        }

        this.discard();
    }

    private void spawnClientTrail() {
        this.level().addParticle(
                ParticleTypes.SQUID_INK,
                this.getX(),
                this.getY(),
                this.getZ(),
                (this.random.nextDouble() - 0.5D) * 0.03D,
                (this.random.nextDouble() - 0.5D) * 0.03D,
                (this.random.nextDouble() - 0.5D) * 0.03D
        );

        if (this.tickCount % 2 == 0) {
            this.level().addParticle(
                    ParticleTypes.SOUL_FIRE_FLAME,
                    this.getX(),
                    this.getY(),
                    this.getZ(),
                    0.0D,
                    0.01D,
                    0.0D
            );
        }
    }
}
