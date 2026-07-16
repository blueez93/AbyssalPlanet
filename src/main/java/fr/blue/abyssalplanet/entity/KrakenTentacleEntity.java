package fr.blue.abyssalplanet.entity;

import fr.blue.abyssalplanet.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.UUID;

public class KrakenTentacleEntity extends Entity {
    private static final EntityDataAccessor<Float> DIRECTION_X = SynchedEntityData.defineId(KrakenTentacleEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DIRECTION_Y = SynchedEntityData.defineId(KrakenTentacleEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> DIRECTION_Z = SynchedEntityData.defineId(KrakenTentacleEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> CURRENT_LENGTH = SynchedEntityData.defineId(KrakenTentacleEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Float> TENTACLE_WIDTH = SynchedEntityData.defineId(KrakenTentacleEntity.class, EntityDataSerializers.FLOAT);

    private static final int MAX_LIFETIME_TICKS = 34;
    private static final double MAX_REACH = 56.0D;
    private static final double BREAK_RANGE = 72.0D;
    private static final double BASE_EXTEND_SPEED = 2.8D;

    private UUID ownerUuid;
    private UUID targetUuid;
    private float damage = 13.0F;
    private int phase = 1;
    private int age;
    private boolean hit;

    public KrakenTentacleEntity(EntityType<? extends KrakenTentacleEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public static void spawn(ServerLevel level, KrakenBossEntity owner, Player target, float damage, int phase) {
        KrakenTentacleEntity tentacle = ModEntities.KRAKEN_TENTACLE.get().create(level);

        if (tentacle == null) {
            return;
        }

        tentacle.ownerUuid = owner.getUUID();
        tentacle.targetUuid = target.getUUID();
        tentacle.damage = damage;
        tentacle.phase = phase;
        tentacle.setTentacleWidth(0.7F + phase * 0.12F);
        tentacle.updateFromOwnerAndTarget(owner, target);
        level.addFreshEntity(tentacle);
    }

    @Override
    public void tick() {
        super.tick();
        this.age++;

        if (this.level().isClientSide) {
            spawnClientParticles();
            return;
        }

        if (!(this.level() instanceof ServerLevel serverLevel)) {
            this.discard();
            return;
        }

        KrakenBossEntity owner = getOwner(serverLevel);
        Player target = getTarget(serverLevel);

        if (owner == null || target == null || !owner.isAlive() || !target.isAlive() || target.isCreative() || target.isSpectator()) {
            this.discard();
            return;
        }

        if (owner.distanceToSqr(target) > BREAK_RANGE * BREAK_RANGE || this.age > MAX_LIFETIME_TICKS) {
            this.discard();
            return;
        }

        updateFromOwnerAndTarget(owner, target);
        extendTowardTarget(target);
    }

    public Vec3 getRenderDirection() {
        Vec3 direction = new Vec3(
                this.entityData.get(DIRECTION_X),
                this.entityData.get(DIRECTION_Y),
                this.entityData.get(DIRECTION_Z)
        );

        if (direction.lengthSqr() < 0.001D) {
            return new Vec3(0.0D, 0.0D, 1.0D);
        }

        return direction.normalize();
    }

    public float getRenderLength() {
        return this.entityData.get(CURRENT_LENGTH);
    }

    public float getRenderWidth() {
        return this.entityData.get(TENTACLE_WIDTH);
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 220.0D * 220.0D;
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(DIRECTION_X, 0.0F);
        this.entityData.define(DIRECTION_Y, 0.0F);
        this.entityData.define(DIRECTION_Z, 1.0F);
        this.entityData.define(CURRENT_LENGTH, 0.5F);
        this.entityData.define(TENTACLE_WIDTH, 0.85F);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.age = tag.getInt("Age");
        this.damage = tag.getFloat("Damage");
        this.phase = Math.max(1, tag.getInt("Phase"));
        this.hit = tag.getBoolean("Hit");

        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }

        if (tag.hasUUID("Target")) {
            this.targetUuid = tag.getUUID("Target");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Age", this.age);
        tag.putFloat("Damage", this.damage);
        tag.putInt("Phase", this.phase);
        tag.putBoolean("Hit", this.hit);

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

    private KrakenBossEntity getOwner(ServerLevel level) {
        if (this.ownerUuid == null) {
            return null;
        }

        Entity entity = level.getEntity(this.ownerUuid);
        return entity instanceof KrakenBossEntity kraken ? kraken : null;
    }

    private Player getTarget(ServerLevel level) {
        if (this.targetUuid == null) {
            return null;
        }

        Entity entity = level.getEntity(this.targetUuid);
        return entity instanceof Player player ? player : null;
    }

    private void updateFromOwnerAndTarget(KrakenBossEntity owner, Player target) {
        Vec3 anchor = owner.position().add(0.0D, owner.getBbHeight() * 0.54D, 0.0D);
        Vec3 toTarget = target.getEyePosition().subtract(anchor);

        if (toTarget.lengthSqr() < 0.001D) {
            return;
        }

        Vec3 direction = toTarget.normalize();
        this.moveTo(anchor.x, anchor.y, anchor.z, owner.getYRot(), owner.getXRot());
        setDirection(direction);
    }

    private void extendTowardTarget(Player target) {
        Vec3 direction = getRenderDirection();
        double distanceToTarget = target.getEyePosition().distanceTo(this.position());
        double targetLength = Math.min(MAX_REACH, distanceToTarget + 1.5D);
        double speed = BASE_EXTEND_SPEED + this.phase * 0.45D;
        float currentLength = this.entityData.get(CURRENT_LENGTH);
        float nextLength = (float) Math.min(targetLength, currentLength + speed);
        this.entityData.set(CURRENT_LENGTH, nextLength);

        Vec3 tip = this.position().add(direction.scale(nextLength));

        if (!this.hit && nextLength >= distanceToTarget - 1.2D && tip.distanceTo(target.getEyePosition()) <= 3.6D) {
            hitTarget(target);
        }
    }

    private void hitTarget(Player target) {
        KrakenBossEntity owner = this.level() instanceof ServerLevel serverLevel ? getOwner(serverLevel) : null;
        DamageSource source = owner != null ? owner.damageSources().mobAttack(owner) : this.damageSources().generic();

        this.hit = true;

        if (target.hurt(source, this.damage)) {
            Vec3 pull = this.position().subtract(target.position()).normalize().scale(0.55D + this.phase * 0.16D);
            target.setDeltaMovement(target.getDeltaMovement().add(pull.x, 0.18D, pull.z));
            target.hurtMarked = true;
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 20 * 4, this.phase >= 3 ? 2 : 1, false, true, true));
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20 * 3, 0, false, true, true));
        }

        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.SQUID_INK,
                    target.getX(),
                    target.getY() + target.getBbHeight() * 0.5D,
                    target.getZ(),
                    28,
                    0.7D,
                    0.7D,
                    0.7D,
                    0.04D
            );
        }

        this.discard();
    }

    private void setDirection(Vec3 direction) {
        this.entityData.set(DIRECTION_X, (float) direction.x);
        this.entityData.set(DIRECTION_Y, (float) direction.y);
        this.entityData.set(DIRECTION_Z, (float) direction.z);
    }

    private void setTentacleWidth(float width) {
        this.entityData.set(TENTACLE_WIDTH, width);
    }

    private void spawnClientParticles() {
        if (this.tickCount % 2 != 0) {
            return;
        }

        Vec3 direction = getRenderDirection();
        float length = getRenderLength();

        if (length <= 1.0F) {
            return;
        }

        Vec3 point = this.position().add(direction.scale(length * (0.35D + this.random.nextDouble() * 0.55D)));
        this.level().addParticle(
                ParticleTypes.SOUL_FIRE_FLAME,
                point.x,
                point.y,
                point.z,
                0.0D,
                0.01D,
                0.0D
        );
    }
}
