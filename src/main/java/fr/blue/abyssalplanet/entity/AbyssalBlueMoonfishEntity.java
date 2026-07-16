package fr.blue.abyssalplanet.entity;

import fr.blue.abyssalplanet.registry.ModDimensions;
import fr.blue.abyssalplanet.registry.ModBiomes;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
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

public class AbyssalBlueMoonfishEntity extends AbstractSchoolingFish {
    private static final EntityDataAccessor<Integer> VARIANT =
            SynchedEntityData.defineId(AbyssalBlueMoonfishEntity.class, EntityDataSerializers.INT);
    public static final int VARIANT_COUNT = 3;

    public AbyssalBlueMoonfishEntity(EntityType<? extends AbstractSchoolingFish> entityType, Level level) {
        super(entityType, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 6.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.9D);
    }

    public static boolean canSpawn(
            EntityType<AbyssalBlueMoonfishEntity> entityType,
            ServerLevelAccessor level,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random
    ) {
        return level.getLevel().dimension().equals(ModDimensions.ABYSSAL_PLANET_LEVEL)
                && level.getBiome(pos).is(ModBiomes.ABYSSAL_VALLEY)
                && level.getFluidState(pos).is(FluidTags.WATER)
                && level.getFluidState(pos.above()).is(FluidTags.WATER);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(VARIANT, 0);
    }

    @Override
    public int getMaxSchoolSize() {
        return 7;
    }

    public int getVariant() {
        return this.entityData.get(VARIANT);
    }

    public void chooseRandomVariant() {
        this.entityData.set(VARIANT, this.random.nextInt(VARIANT_COUNT));
    }

    @Override
    public ItemStack getBucketItemStack() {
        return new ItemStack(Items.TROPICAL_FISH_BUCKET);
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.TROPICAL_FISH_AMBIENT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.TROPICAL_FISH_DEATH;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource damageSource) {
        return SoundEvents.TROPICAL_FISH_HURT;
    }

    @Override
    protected SoundEvent getFlopSound() {
        return SoundEvents.TROPICAL_FISH_FLOP;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", getVariant());
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.entityData.set(VARIANT, Math.max(0, Math.min(VARIANT_COUNT - 1, tag.getInt("Variant"))));
    }
}
