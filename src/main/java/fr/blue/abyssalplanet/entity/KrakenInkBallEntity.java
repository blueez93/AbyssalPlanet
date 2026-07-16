package fr.blue.abyssalplanet.entity;

import fr.blue.abyssalplanet.registry.ModEntities;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;
import java.util.UUID;

public class KrakenInkBallEntity extends Entity {
    private static final int MAX_LIFETIME_TICKS = 20 * 5;
    private static final double HIT_RADIUS = 2.2D;

    private UUID ownerUuid;
    private float damage = 6.0F;
    private int phase = 1;
    private int age;

    public KrakenInkBallEntity(EntityType<? extends KrakenInkBallEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public static void spawn(ServerLevel level, KrakenBossEntity owner, Player target, int phase) {
        KrakenInkBallEntity inkBall = ModEntities.KRAKEN_INK_BALL.get().create(level);

        if (inkBall == null) {
            return;
        }

        Vec3 start = owner.getEyePosition().add(0.0D, -1.0D, 0.0D);
        Vec3 direction = target.getEyePosition().subtract(start);

        if (direction.lengthSqr() < 0.001D) {
            return;
        }

        double speed = 1.05D + phase * 0.12D;
        inkBall.ownerUuid = owner.getUUID();
        inkBall.phase = phase;
        inkBall.damage = 5.0F + phase * 1.5F;
        inkBall.moveTo(start.x, start.y, start.z, owner.getYRot(), owner.getXRot());
        inkBall.setDeltaMovement(direction.normalize().scale(speed));
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

        Vec3 movement = this.getDeltaMovement();
        this.setPos(this.getX() + movement.x, this.getY() + movement.y, this.getZ() + movement.z);

        if (this.age > MAX_LIFETIME_TICKS) {
            burst();
            return;
        }

        hitPlayersInside();
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 180.0D * 180.0D;
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.age = tag.getInt("Age");
        this.damage = tag.getFloat("Damage");
        this.phase = Math.max(1, tag.getInt("Phase"));

        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Age", this.age);
        tag.putFloat("Damage", this.damage);
        tag.putInt("Phase", this.phase);

        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private void hitPlayersInside() {
        AABB hitBox = this.getBoundingBox().inflate(HIT_RADIUS);
        List<Player> players = this.level().getEntitiesOfClass(Player.class, hitBox, player ->
                player.isAlive() && !player.isCreative() && !player.isSpectator()
        );

        for (Player player : players) {
            hitPlayer(player);
            return;
        }
    }

    private void hitPlayer(Player player) {
        DamageSource source = this.damageSources().generic();

        if (this.level() instanceof ServerLevel serverLevel && this.ownerUuid != null) {
            Entity owner = serverLevel.getEntity(this.ownerUuid);

            if (owner instanceof KrakenBossEntity kraken) {
                source = kraken.damageSources().mobAttack(kraken);
            }
        }

        player.hurt(source, this.damage);
        player.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 20 * (4 + this.phase), 0, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20 * 2, 0, false, true, true));
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 20 * 4, 0, false, true, true));

        Vec3 push = player.position().subtract(this.position()).normalize().scale(0.35D + this.phase * 0.08D);
        player.setDeltaMovement(player.getDeltaMovement().add(push.x, 0.12D, push.z));
        player.hurtMarked = true;
        burst();
    }

    private void burst() {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.SQUID_INK,
                    this.getX(),
                    this.getY(),
                    this.getZ(),
                    45,
                    1.2D,
                    1.2D,
                    1.2D,
                    0.08D
            );
            AbyssalInkPuddleEntity.spawn(serverLevel, this.position());
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
