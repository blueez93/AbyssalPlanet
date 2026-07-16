package fr.blue.abyssalplanet.entity;

import fr.blue.abyssalplanet.registry.ModDimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.AbstractSchoolingFish;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;

public class LuminousAbyssalFishEntity extends AbstractSchoolingFish {
    public LuminousAbyssalFishEntity(EntityType<? extends AbstractSchoolingFish> entityType, Level level) {
        super(entityType, level);
        this.setGlowingTag(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 4.0D)
                .add(Attributes.MOVEMENT_SPEED, 1.0D);
    }

    public static boolean canSpawn(EntityType<LuminousAbyssalFishEntity> entityType, ServerLevelAccessor level, MobSpawnType spawnType, BlockPos pos, RandomSource random) {
        return level.getLevel().dimension().equals(ModDimensions.ABYSSAL_PLANET_LEVEL)
                && level.getFluidState(pos).is(FluidTags.WATER)
                && level.getFluidState(pos.above()).is(FluidTags.WATER);
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (!this.level().isClientSide) {
            this.setGlowingTag(true);
            return;
        }

        int particleOffset = Math.floorMod(this.getId(), 8);
        Vec3 forward = Vec3.directionFromRotation(0.0F, this.getYRot());
        Vec3 trailPosition = this.position()
                .subtract(forward.scale(0.48D))
                .add(0.0D, this.getBbHeight() * 0.45D, 0.0D);

        if ((this.tickCount + particleOffset) % 8 == 0) {
            this.level().addParticle(
                    ParticleTypes.GLOW,
                    trailPosition.x + (this.random.nextDouble() - 0.5D) * 0.12D,
                    trailPosition.y + (this.random.nextDouble() - 0.5D) * 0.1D,
                    trailPosition.z + (this.random.nextDouble() - 0.5D) * 0.12D,
                    -this.getDeltaMovement().x * 0.12D,
                    0.003D,
                    -this.getDeltaMovement().z * 0.12D
            );
        }

        if ((this.tickCount + particleOffset) % 20 == 0) {
            this.level().addParticle(
                    ParticleTypes.BUBBLE,
                    trailPosition.x,
                    trailPosition.y,
                    trailPosition.z,
                    -this.getDeltaMovement().x * 0.08D,
                    0.012D,
                    -this.getDeltaMovement().z * 0.08D
            );
        }
    }

    @Override
    public int getMaxSchoolSize() {
        return 8;
    }

    @Override
    public ItemStack getBucketItemStack() {
        return new ItemStack(Items.TROPICAL_FISH_BUCKET);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.COD_AMBIENT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.COD_DEATH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.COD_HURT;
    }

    @Override
    protected SoundEvent getFlopSound() {
        return SoundEvents.COD_FLOP;
    }
}
