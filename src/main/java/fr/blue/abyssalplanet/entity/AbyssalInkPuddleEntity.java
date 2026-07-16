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
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

import java.util.List;

public class AbyssalInkPuddleEntity extends Entity {
    private static final int LIFETIME_TICKS = 20 * 8;
    private static final int POISON_STACK_TICKS = 20 * 2;
    private static final int MAX_POISON_TICKS = 20 * 12;
    private static final double RADIUS = 2.25D;
    private static final double HEIGHT = 1.2D;

    private int age;

    public AbyssalInkPuddleEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public static void spawn(ServerLevel level, Vec3 position) {
        AbyssalInkPuddleEntity puddle = new AbyssalInkPuddleEntity(ModEntities.ABYSSAL_INK_PUDDLE.get(), level);
        puddle.moveTo(position.x, position.y, position.z, 0.0F, 0.0F);
        level.addFreshEntity(puddle);
    }

    @Override
    public void tick() {
        super.tick();
        this.age++;

        if (this.level().isClientSide) {
            return;
        }

        if (this.age % 5 == 0 && this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.SQUID_INK,
                    this.getX(),
                    this.getY() + 0.15D,
                    this.getZ(),
                    10,
                    RADIUS * 0.55D,
                    0.1D,
                    RADIUS * 0.55D,
                    0.01D
            );
        }

        if (this.age % 20 == 0) {
            poisonPlayersInside();
        }

        if (this.age >= LIFETIME_TICKS) {
            this.discard();
        }
    }

    @Override
    public boolean isPickable() {
        return false;
    }

    private void poisonPlayersInside() {
        AABB area = this.getBoundingBox().inflate(RADIUS, HEIGHT, RADIUS);
        List<Player> players = this.level().getEntitiesOfClass(Player.class, area, player ->
                player.isAlive() && !player.isCreative() && !player.isSpectator()
        );

        for (Player player : players) {
            double horizontalDistanceSqr = player.position().subtract(this.position()).multiply(1.0D, 0.0D, 1.0D).lengthSqr();

            if (horizontalDistanceSqr > RADIUS * RADIUS || Math.abs(player.getY() - this.getY()) > HEIGHT) {
                continue;
            }

            MobEffectInstance currentPoison = player.getEffect(MobEffects.POISON);
            int duration = POISON_STACK_TICKS;
            int amplifier = 0;

            if (currentPoison != null) {
                duration = Math.min(MAX_POISON_TICKS, currentPoison.getDuration() + POISON_STACK_TICKS);
                amplifier = Math.max(0, currentPoison.getAmplifier());
            }

            player.addEffect(new MobEffectInstance(
                    MobEffects.POISON,
                    duration,
                    amplifier,
                    false,
                    true,
                    true
            ));
        }
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.age = tag.getInt("Age");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Age", this.age);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
