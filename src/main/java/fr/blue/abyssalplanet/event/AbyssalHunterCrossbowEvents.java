package fr.blue.abyssalplanet.event;

import fr.blue.abyssalplanet.AbyssalPlanet;
import fr.blue.abyssalplanet.item.AbyssalHunterCrossbowItem;
import fr.blue.abyssalplanet.registry.ModBiomes;
import fr.blue.abyssalplanet.registry.ModBlocks;
import fr.blue.abyssalplanet.registry.ModDimensions;
import fr.blue.abyssalplanet.registry.ModEffects;
import fr.blue.abyssalplanet.registry.ModItems;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AbyssalPlanet.MOD_ID)
public final class AbyssalHunterCrossbowEvents {
    private static final String ABYSSAL_ARROW_TAG = "AbyssalHunterArrow";
    private static final String SPECIAL_ARROW_TAG = "AbyssalHunterSpecialArrow";
    private static final String PAUSE_TICKS_TAG = "AbyssalHunterPauseTicks";
    private static final String TARGET_X_TAG = "AbyssalHunterArrowTargetX";
    private static final String TARGET_Y_TAG = "AbyssalHunterArrowTargetY";
    private static final String TARGET_Z_TAG = "AbyssalHunterArrowTargetZ";
    private static final String BURN_END_TAG = "AbyssalHunterBurnEnd";
    private static final String NEXT_BURN_DAMAGE_TAG = "AbyssalHunterNextBurnDamage";
    private static final String GROUNDED_CROSSBOW_TAG = "AbyssalHunterGroundedCrossbow";
    private static final String GROUND_X_TAG = "AbyssalHunterGroundX";
    private static final String GROUND_Y_TAG = "AbyssalHunterGroundY";
    private static final String GROUND_Z_TAG = "AbyssalHunterGroundZ";
    private static final String SPAWN_CHECK_TAG = "AbyssalHunterCrossbowSpawnCheck";
    private static final int BURN_STEP_TICKS = 20 * 10;
    private static final int MAX_BURN_TICKS = 20 * 60;

    private AbyssalHunterCrossbowEvents() {
    }

    @SubscribeEvent
    public static void onArrowSpawn(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel level)
                || !(event.getEntity() instanceof AbstractArrow arrow)
                || !(arrow.getOwner() instanceof ServerPlayer owner)) {
            return;
        }

        CompoundTag ownerData = owner.getPersistentData();
        if (!ownerData.getBoolean(AbyssalHunterCrossbowItem.PENDING_SHOT_TAG)) {
            return;
        }

        CompoundTag arrowData = arrow.getPersistentData();
        arrowData.putBoolean(ABYSSAL_ARROW_TAG, true);
        arrow.setBaseDamage(2.35D);

        if (ownerData.getBoolean(AbyssalHunterCrossbowItem.PENDING_SPECIAL_TAG)) {
            arrowData.putBoolean(SPECIAL_ARROW_TAG, true);
            arrowData.putInt(PAUSE_TICKS_TAG, 40);
            arrowData.putDouble(TARGET_X_TAG, ownerData.getDouble(AbyssalHunterCrossbowItem.PENDING_TARGET_X_TAG));
            arrowData.putDouble(TARGET_Y_TAG, ownerData.getDouble(AbyssalHunterCrossbowItem.PENDING_TARGET_Y_TAG));
            arrowData.putDouble(TARGET_Z_TAG, ownerData.getDouble(AbyssalHunterCrossbowItem.PENDING_TARGET_Z_TAG));
            arrow.setBaseDamage(7.5D);
            arrow.setDeltaMovement(Vec3.ZERO);
            arrow.setNoGravity(true);
            arrow.setGlowingTag(true);
            level.playSound(null, arrow.blockPosition(), SoundEvents.RESPAWN_ANCHOR_CHARGE, SoundSource.PLAYERS, 0.75F, 0.65F);
        }
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) {
            return;
        }

        for (Entity entity : level.getAllEntities()) {
            if (entity instanceof ItemEntity item && item.getPersistentData().getBoolean(GROUNDED_CROSSBOW_TAG)) {
                CompoundTag itemData = item.getPersistentData();
                item.setNoGravity(true);
                item.setDeltaMovement(Vec3.ZERO);
                item.setPos(itemData.getDouble(GROUND_X_TAG), itemData.getDouble(GROUND_Y_TAG), itemData.getDouble(GROUND_Z_TAG));
                if (item.tickCount % 30 == 0) {
                    level.sendParticles(ParticleTypes.CRIMSON_SPORE, item.getX(), item.getY() + 0.15D, item.getZ(), 2, 0.2D, 0.08D, 0.2D, 0.0D);
                }
                continue;
            }
            if (!(entity instanceof AbstractArrow arrow)) {
                continue;
            }
            CompoundTag data = arrow.getPersistentData();
            if (!data.getBoolean(SPECIAL_ARROW_TAG)) {
                continue;
            }

            int pauseTicks = data.getInt(PAUSE_TICKS_TAG);
            if (pauseTicks > 0) {
                data.putInt(PAUSE_TICKS_TAG, pauseTicks - 1);
                arrow.setDeltaMovement(Vec3.ZERO);
                level.sendParticles(ParticleTypes.CRIMSON_SPORE, arrow.getX(), arrow.getY(), arrow.getZ(), 3, 0.12D, 0.12D, 0.12D, 0.005D);
                level.sendParticles(ParticleTypes.SMOKE, arrow.getX(), arrow.getY(), arrow.getZ(), 1, 0.05D, 0.05D, 0.05D, 0.0D);
                if (pauseTicks == 1) {
                    launchSpecialArrow(level, arrow, data);
                }
            } else if (arrow.tickCount % 2 == 0) {
                level.sendParticles(ParticleTypes.FLAME, arrow.getX(), arrow.getY(), arrow.getZ(), 2, 0.04D, 0.04D, 0.04D, 0.01D);
                level.sendParticles(ParticleTypes.CRIMSON_SPORE, arrow.getX(), arrow.getY(), arrow.getZ(), 2, 0.06D, 0.06D, 0.06D, 0.0D);
            }
        }

        if (level.dimension().equals(ModDimensions.ABYSSAL_PLANET_LEVEL) && level.getGameTime() % 200L == 0L) {
            trySpawnCrossbow(level);
        }
    }

    private static void launchSpecialArrow(ServerLevel level, AbstractArrow arrow, CompoundTag data) {
        Vec3 target = new Vec3(data.getDouble(TARGET_X_TAG), data.getDouble(TARGET_Y_TAG), data.getDouble(TARGET_Z_TAG));
        Vec3 direction = target.subtract(arrow.position());
        if (direction.lengthSqr() < 0.01D) {
            direction = arrow.getOwner() == null ? new Vec3(0.0D, 0.0D, 1.0D) : arrow.getOwner().getLookAngle();
        }
        arrow.setDeltaMovement(direction.normalize().scale(5.25D));
        arrow.hasImpulse = true;
        level.playSound(null, arrow.blockPosition(), SoundEvents.TRIDENT_RIPTIDE_3, SoundSource.PLAYERS, 1.4F, 0.7F);
        level.sendParticles(ParticleTypes.EXPLOSION, arrow.getX(), arrow.getY(), arrow.getZ(), 1, 0.0D, 0.0D, 0.0D, 0.0D);
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getDirectEntity() instanceof AbstractArrow arrow)
                || !arrow.getPersistentData().getBoolean(ABYSSAL_ARROW_TAG)) {
            return;
        }

        LivingEntity target = event.getEntity();
        long now = target.level().getGameTime();
        CompoundTag data = target.getPersistentData();
        long remaining = Math.max(0L, data.getLong(BURN_END_TAG) - now);
        long duration = Math.min(MAX_BURN_TICKS, remaining + BURN_STEP_TICKS);
        data.putLong(BURN_END_TAG, now + duration);
        if (data.getLong(NEXT_BURN_DAMAGE_TAG) <= now) {
            data.putLong(NEXT_BURN_DAMAGE_TAG, now + BURN_STEP_TICKS);
        }
        target.addEffect(new MobEffectInstance(ModEffects.TOXIC_BURN.get(), (int) duration, 0, false, true, true));
    }

    @SubscribeEvent
    public static void onLivingTick(net.minecraftforge.event.entity.living.LivingEvent.LivingTickEvent event) {
        LivingEntity living = event.getEntity();
        if (living.level().isClientSide) {
            return;
        }

        CompoundTag data = living.getPersistentData();
        long now = living.level().getGameTime();
        long burnEnd = data.getLong(BURN_END_TAG);
        if (burnEnd < now) {
            data.remove(BURN_END_TAG);
            data.remove(NEXT_BURN_DAMAGE_TAG);
            return;
        }

        long nextDamage = data.getLong(NEXT_BURN_DAMAGE_TAG);
        if (nextDamage > 0L && now >= nextDamage) {
            living.hurt(living.damageSources().magic(), 2.0F);
            data.putLong(NEXT_BURN_DAMAGE_TAG, now + BURN_STEP_TICKS);
        }
    }

    public static boolean hasActiveWeaponBurn(LivingEntity living) {
        return living.getPersistentData().getLong(BURN_END_TAG) > living.level().getGameTime();
    }

    private static void trySpawnCrossbow(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || !level.getBiome(player.blockPosition()).is(ModBiomes.DECAY_ROAD)) {
                continue;
            }
            long checks = player.getPersistentData().getLong(SPAWN_CHECK_TAG) + 1L;
            player.getPersistentData().putLong(SPAWN_CHECK_TAG, checks);
            if (level.random.nextInt(400) != 0 || hasNearbyCrossbow(level, player.blockPosition())) {
                continue;
            }

            BlockPos spawnPos = findDecayFloor(level, player.blockPosition());
            if (spawnPos == null) {
                continue;
            }
            ItemEntity item = new ItemEntity(level, spawnPos.getX() + 0.5D, spawnPos.getY() + 0.18D, spawnPos.getZ() + 0.5D,
                    new ItemStack(ModItems.ABYSSAL_HUNTER_CROSSBOW.get()));
            item.setUnlimitedLifetime();
            item.setDeltaMovement(Vec3.ZERO);
            item.setGlowingTag(true);
            item.setNoGravity(true);
            CompoundTag itemData = item.getPersistentData();
            itemData.putBoolean(GROUNDED_CROSSBOW_TAG, true);
            itemData.putDouble(GROUND_X_TAG, item.getX());
            itemData.putDouble(GROUND_Y_TAG, item.getY());
            itemData.putDouble(GROUND_Z_TAG, item.getZ());
            level.addFreshEntity(item);
            level.sendParticles(ParticleTypes.CRIMSON_SPORE, item.getX(), item.getY() + 0.25D, item.getZ(), 20, 0.4D, 0.15D, 0.4D, 0.01D);
            break;
        }
    }

    private static boolean hasNearbyCrossbow(ServerLevel level, BlockPos center) {
        AABB area = new AABB(center).inflate(256.0D);
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, area,
                item -> item.getItem().is(ModItems.ABYSSAL_HUNTER_CROSSBOW.get()));
        return !items.isEmpty();
    }

    private static BlockPos findDecayFloor(ServerLevel level, BlockPos center) {
        for (int attempt = 0; attempt < 18; attempt++) {
            int distance = Mth.nextInt(level.random, 24, 72);
            double angle = level.random.nextDouble() * Math.PI * 2.0D;
            int x = center.getX() + Mth.floor(Math.cos(angle) * distance);
            int z = center.getZ() + Mth.floor(Math.sin(angle) * distance);
            BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos(x, Math.min(level.getMaxBuildHeight() - 1, center.getY() + 32), z);
            for (int y = cursor.getY(); y > level.getMinBuildHeight() + 1; y--) {
                cursor.setY(y);
                if (level.getBlockState(cursor).is(ModBlocks.DECAYED_ABYSSAL_SAND.get())
                        && level.getBlockState(cursor.above()).getCollisionShape(level, cursor.above()).isEmpty()) {
                    return cursor.above().immutable();
                }
            }
        }
        return null;
    }
}
