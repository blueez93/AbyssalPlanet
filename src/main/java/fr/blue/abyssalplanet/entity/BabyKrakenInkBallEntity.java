package fr.blue.abyssalplanet.entity;

import fr.blue.abyssalplanet.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.UUID;

public class BabyKrakenInkBallEntity extends Entity {
    private static final int MAX_LIFETIME_TICKS = 20 * 5;
    private static final double HIT_RADIUS = 0.9D;

    private UUID ownerUuid;
    private UUID targetUuid;
    private int age;

    public BabyKrakenInkBallEntity(EntityType<? extends BabyKrakenInkBallEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public static void spawn(ServerLevel level, BabyKrakenEntity owner, LivingEntity target) {
        BabyKrakenInkBallEntity inkBall = ModEntities.BABY_KRAKEN_INK_BALL.get().create(level);
        if (inkBall == null) {
            return;
        }

        Vec3 start = owner.getEyePosition().add(owner.getLookAngle().scale(0.45D));
        Vec3 direction = target.getEyePosition().subtract(start);
        if (direction.lengthSqr() < 0.001D) {
            return;
        }

        inkBall.ownerUuid = owner.getUUID();
        inkBall.targetUuid = target.getUUID();
        inkBall.moveTo(start.x, start.y - 0.15D, start.z, owner.getYRot(), owner.getXRot());
        inkBall.setDeltaMovement(direction.normalize().scale(0.72D));
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

        if (this.age > MAX_LIFETIME_TICKS || !(this.level() instanceof ServerLevel serverLevel)) {
            burst();
            return;
        }

        LivingEntity target = getTarget(serverLevel);
        if (target == null) {
            burst();
            return;
        }

        Vec3 desiredDirection = target.getEyePosition().subtract(this.position());
        if (desiredDirection.lengthSqr() > 0.001D) {
            Vec3 currentDirection = this.getDeltaMovement().normalize();
            Vec3 homingDirection = currentDirection.scale(0.82D)
                    .add(desiredDirection.normalize().scale(0.18D))
                    .normalize();
            this.setDeltaMovement(homingDirection.scale(0.72D));
        }

        Vec3 movement = this.getDeltaMovement();
        this.setPos(this.getX() + movement.x, this.getY() + movement.y, this.getZ() + movement.z);

        if (this.distanceToSqr(target) <= HIT_RADIUS * HIT_RADIUS
                || this.getBoundingBox().inflate(0.45D).intersects(target.getBoundingBox())) {
            hitTarget(serverLevel, target);
        }
    }

    private LivingEntity getTarget(ServerLevel level) {
        if (this.targetUuid == null || !(level.getEntity(this.targetUuid) instanceof LivingEntity target)) {
            return null;
        }
        return target.isAlive() ? target : null;
    }

    private void hitTarget(ServerLevel level, LivingEntity target) {
        Entity owner = this.ownerUuid == null ? null : level.getEntity(this.ownerUuid);
        target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20 * 3, 0, false, true, true), owner);
        if (!(target instanceof Player)) {
            target.addEffect(new MobEffectInstance(MobEffects.POISON, 20 * 4, 0, false, true, true), owner);
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
                    22,
                    0.45D,
                    0.45D,
                    0.45D,
                    0.055D
            );
        }
        this.discard();
    }

    private void spawnClientTrail() {
        for (int index = 0; index < 2; index++) {
            this.level().addParticle(
                    ParticleTypes.SQUID_INK,
                    this.getX(),
                    this.getY(),
                    this.getZ(),
                    (this.random.nextDouble() - 0.5D) * 0.018D,
                    (this.random.nextDouble() - 0.5D) * 0.018D,
                    (this.random.nextDouble() - 0.5D) * 0.018D
            );
        }
        this.level().addParticle(
                ParticleTypes.PORTAL,
                this.getX(),
                this.getY(),
                this.getZ(),
                0.0D,
                0.0D,
                0.0D
        );
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.age = tag.getInt("Age");
        this.ownerUuid = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        this.targetUuid = tag.hasUUID("Target") ? tag.getUUID("Target") : null;
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Age", this.age);
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        if (this.targetUuid != null) {
            tag.putUUID("Target", this.targetUuid);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
