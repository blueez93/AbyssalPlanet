package fr.blue.abyssalplanet.entity;

import fr.blue.abyssalplanet.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class BabyKrakenBubbleEntity extends Entity {
    private static final int MAX_LIFETIME_TICKS = 20 * 4;
    private static final double HIT_RADIUS = 0.72D;
    private static final float DAMAGE = 2.0F;

    private UUID ownerUuid;
    private UUID targetUuid;
    private int age;

    public BabyKrakenBubbleEntity(EntityType<? extends BabyKrakenBubbleEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public static void spawnVolley(ServerLevel level, BabyKrakenEntity owner, LivingEntity target) {
        Vec3 baseStart = owner.getEyePosition().add(owner.getLookAngle().scale(0.5D));
        Vec3 toTarget = target.getEyePosition().subtract(baseStart);
        if (toTarget.lengthSqr() < 0.001D) {
            return;
        }

        Vec3 direction = toTarget.normalize();
        Vec3 side = direction.cross(new Vec3(0.0D, 1.0D, 0.0D));
        if (side.lengthSqr() < 0.001D) {
            side = new Vec3(1.0D, 0.0D, 0.0D);
        } else {
            side = side.normalize();
        }

        double[] offsets = {-0.34D, 0.0D, 0.34D};
        for (int index = 0; index < offsets.length; index++) {
            BabyKrakenBubbleEntity bubble = ModEntities.BABY_KRAKEN_BUBBLE.get().create(level);
            if (bubble == null) {
                continue;
            }

            double offset = offsets[index];
            Vec3 start = baseStart.add(side.scale(offset)).add(0.0D, (index - 1) * 0.08D, 0.0D);
            Vec3 aimPoint = target.getEyePosition().add(side.scale(offset * 0.25D));
            Vec3 shotDirection = aimPoint.subtract(start);
            if (shotDirection.lengthSqr() < 0.001D) {
                continue;
            }

            bubble.ownerUuid = owner.getUUID();
            bubble.targetUuid = target.getUUID();
            bubble.moveTo(start.x, start.y, start.z, owner.getYRot(), owner.getXRot());
            bubble.setDeltaMovement(shotDirection.normalize().scale(0.62D));
            level.addFreshEntity(bubble);
        }
    }

    @Override
    public void tick() {
        super.tick();
        this.age++;

        if (this.level().isClientSide) {
            spawnClientTrail();
            return;
        }
        if (!(this.level() instanceof ServerLevel serverLevel) || this.age > MAX_LIFETIME_TICKS) {
            burst();
            return;
        }

        LivingEntity target = getTarget(serverLevel);
        if (target == null) {
            burst();
            return;
        }

        Vec3 toTarget = target.getEyePosition().subtract(this.position());
        if (toTarget.lengthSqr() > 0.001D) {
            Vec3 current = this.getDeltaMovement();
            Vec3 currentDirection = current.lengthSqr() < 0.001D ? toTarget.normalize() : current.normalize();
            Vec3 homingDirection = currentDirection.scale(0.72D)
                    .add(toTarget.normalize().scale(0.28D))
                    .normalize();
            this.setDeltaMovement(homingDirection.scale(0.62D));
        }

        Vec3 movement = this.getDeltaMovement();
        this.setPos(this.getX() + movement.x, this.getY() + movement.y, this.getZ() + movement.z);
        if (this.distanceToSqr(target) <= HIT_RADIUS * HIT_RADIUS
                || this.getBoundingBox().inflate(0.3D).intersects(target.getBoundingBox())) {
            hitTarget(serverLevel, target);
        }
    }

    @Nullable
    private LivingEntity getTarget(ServerLevel level) {
        if (this.targetUuid == null || !(level.getEntity(this.targetUuid) instanceof LivingEntity target)) {
            return null;
        }
        return target.isAlive() ? target : null;
    }

    private void hitTarget(ServerLevel level, LivingEntity target) {
        Entity owner = this.ownerUuid == null ? null : level.getEntity(this.ownerUuid);
        DamageSource damageSource = owner instanceof LivingEntity livingOwner
                ? level.damageSources().mobAttack(livingOwner)
                : level.damageSources().generic();
        target.hurt(damageSource, DAMAGE);
        burst();
    }

    private void spawnClientTrail() {
        for (int index = 0; index < 3; index++) {
            this.level().addParticle(
                    ParticleTypes.BUBBLE,
                    this.getX() + (this.random.nextDouble() - 0.5D) * 0.12D,
                    this.getY() + (this.random.nextDouble() - 0.5D) * 0.12D,
                    this.getZ() + (this.random.nextDouble() - 0.5D) * 0.12D,
                    0.0D,
                    0.015D,
                    0.0D
            );
        }
    }

    private void burst() {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.BUBBLE_POP,
                    this.getX(),
                    this.getY(),
                    this.getZ(),
                    12,
                    0.22D,
                    0.22D,
                    0.22D,
                    0.06D
            );
        }
        this.discard();
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
    public boolean isPickable() {
        return false;
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
