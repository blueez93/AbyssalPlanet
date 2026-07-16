package fr.blue.abyssalplanet.block.entity;

import fr.blue.abyssalplanet.block.AbyssalEggBlock;
import fr.blue.abyssalplanet.entity.AbyssalShrimpEntity;
import fr.blue.abyssalplanet.entity.GeorgesJuniorEntity;
import fr.blue.abyssalplanet.registry.ModBlockEntities;
import fr.blue.abyssalplanet.registry.ModDimensions;
import fr.blue.abyssalplanet.registry.ModEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoBlockEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

public final class AbyssalEggBlockEntity extends BlockEntity implements GeoBlockEntity {
    public static final int HATCH_DURATION_TICKS = 20 * 60 * 5;
    private static final int HATCH_ANIMATION_TICKS = 20 * 10;
    private static final RawAnimation IDLE_ANIMATION =
            RawAnimation.begin().thenLoop("animation.abyssal_egg.idle");
    private static final RawAnimation HATCH_ANIMATION =
            RawAnimation.begin().thenPlayAndHold("animation.abyssal_egg.hatch");

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);
    private int hatchTicks;
    private boolean hatchAnimationStarted;

    public AbyssalEggBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ABYSSAL_EGG.get(), pos, state);
    }

    public static void serverTick(
            Level level,
            BlockPos pos,
            BlockState state,
            AbyssalEggBlockEntity egg
    ) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        if (level.dimension().equals(ModDimensions.ABYSSAL_PLANET_LEVEL)) {
            egg.resetForbiddenHatchingProgress();
            return;
        }

        if (!state.hasProperty(AbyssalEggBlock.WATERLOGGED)
                || !state.getValue(AbyssalEggBlock.WATERLOGGED)
                || level.getFluidState(pos).getType() != Fluids.WATER) {
            return;
        }

        egg.hatchTicks++;
        int remaining = HATCH_DURATION_TICKS - egg.hatchTicks;

        if (!egg.hatchAnimationStarted && remaining <= HATCH_ANIMATION_TICKS) {
            egg.hatchAnimationStarted = true;
            egg.triggerAnim("hatching", "hatch");
            level.playSound(
                    null,
                    pos,
                    SoundEvents.TURTLE_EGG_CRACK,
                    SoundSource.BLOCKS,
                    1.1F,
                    0.55F
            );
            egg.sync();
        }

        if (egg.hatchTicks % 20 == 0) {
            double intensity = 1.0D - Math.max(0, remaining) / (double) HATCH_DURATION_TICKS;
            serverLevel.sendParticles(
                    ParticleTypes.BUBBLE,
                    pos.getX() + 0.5D,
                    pos.getY() + 0.75D,
                    pos.getZ() + 0.5D,
                    2 + (int) Math.round(intensity * 7.0D),
                    0.32D,
                    0.34D,
                    0.32D,
                    0.035D
            );
            egg.sync();
        }

        if (remaining <= HATCH_ANIMATION_TICKS && egg.hatchTicks % 10 == 0) {
            serverLevel.sendParticles(
                    ParticleTypes.DRAGON_BREATH,
                    pos.getX() + 0.5D,
                    pos.getY() + 0.55D,
                    pos.getZ() + 0.5D,
                    5,
                    0.38D,
                    0.32D,
                    0.38D,
                    0.015D
            );
        }

        if (egg.hatchTicks >= HATCH_DURATION_TICKS) {
            egg.hatch(serverLevel, pos);
        } else {
            egg.setChanged();
        }
    }

    private void resetForbiddenHatchingProgress() {
        if (hatchTicks == 0 && !hatchAnimationStarted) {
            return;
        }

        hatchTicks = 0;
        hatchAnimationStarted = false;
        sync();
    }

    private void hatch(ServerLevel level, BlockPos pos) {
        int roll = level.random.nextInt(10_000);
        if (roll == 0) {
            AbyssalShrimpEntity shrimp = ModEntities.ABYSSAL_SHRIMP.get().create(level);
            if (shrimp != null) {
                shrimp.moveTo(pos.getX() + 0.5D, pos.getY() + 0.2D, pos.getZ() + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
                shrimp.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.BREEDING, null, null);
                shrimp.setPersistenceRequired();
                level.addFreshEntity(shrimp);
            }
        } else {
            GeorgesJuniorEntity junior = ModEntities.GEORGES_JUNIOR.get().create(level);
            if (junior != null) {
                junior.setWillGrow(roll <= 1_000);
                junior.moveTo(pos.getX() + 0.5D, pos.getY() + 0.2D, pos.getZ() + 0.5D, level.random.nextFloat() * 360.0F, 0.0F);
                junior.finalizeSpawn(level, level.getCurrentDifficultyAt(pos), MobSpawnType.BREEDING, null, null);
                junior.setPersistenceRequired();
                level.addFreshEntity(junior);
            }
        }

        level.sendParticles(
                ParticleTypes.EXPLOSION,
                pos.getX() + 0.5D,
                pos.getY() + 0.55D,
                pos.getZ() + 0.5D,
                2,
                0.2D,
                0.2D,
                0.2D,
                0.0D
        );
        level.sendParticles(
                ParticleTypes.DRAGON_BREATH,
                pos.getX() + 0.5D,
                pos.getY() + 0.55D,
                pos.getZ() + 0.5D,
                42,
                0.75D,
                0.65D,
                0.75D,
                0.07D
        );
        level.playSound(
                null,
                pos,
                SoundEvents.GENERIC_EXPLODE,
                SoundSource.BLOCKS,
                1.5F,
                0.65F
        );
        level.setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
    }

    private void sync() {
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
    }

    public float getHatchProgress() {
        return Math.min(1.0F, hatchTicks / (float) HATCH_DURATION_TICKS);
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        tag.putInt("HatchTicks", hatchTicks);
        tag.putBoolean("HatchAnimationStarted", hatchAnimationStarted);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        hatchTicks = Math.max(0, tag.getInt("HatchTicks"));
        hatchAnimationStarted = tag.getBoolean("HatchAnimationStarted");
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(
            Connection connection,
            ClientboundBlockEntityDataPacket packet
    ) {
        CompoundTag tag = packet.getTag();
        if (tag != null) {
            load(tag);
        }
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(
                this,
                "idle",
                6,
                state -> state.setAndContinue(IDLE_ANIMATION)
        ));
        controllers.add(new AnimationController<>(this, "hatching", 0, state -> software.bernie.geckolib.core.object.PlayState.STOP)
                .triggerableAnim("hatch", HATCH_ANIMATION));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }
}
