package fr.blue.abyssalplanet.entity;

import fr.blue.abyssalplanet.registry.ModDimensions;
import fr.blue.abyssalplanet.registry.ModItems;
import fr.blue.abyssalplanet.world.BabyKrakenPetData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtTargetGoal;
import net.minecraft.world.entity.ai.util.DefaultRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;
import net.minecraftforge.event.ForgeEventFactory;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;
import java.util.function.Function;

public class BabyKrakenEntity extends TamableAnimal implements GeoEntity {
    private static final EntityDataAccessor<Boolean> CAMOUFLAGED =
            SynchedEntityData.defineId(BabyKrakenEntity.class, EntityDataSerializers.BOOLEAN);

    private static final RawAnimation IDLE_ANIM =
            RawAnimation.begin().thenLoop("animation.baby_kraken.idle");
    private static final RawAnimation SWIM_ANIM =
            RawAnimation.begin().thenLoop("animation.baby_kraken.swim");
    private static final RawAnimation CRAWL_ANIM =
            RawAnimation.begin().thenLoop("animation.baby_kraken.crawl");
    private static final RawAnimation SIT_ANIM =
            RawAnimation.begin().thenLoop("animation.baby_kraken.sit");
    private static final RawAnimation FLEE_ANIM =
            RawAnimation.begin().thenPlay("animation.baby_kraken.flee");
    private static final RawAnimation INK_SPIT_ANIM =
            RawAnimation.begin().thenPlay("animation.baby_kraken.ink_spit");
    private static final RawAnimation CAMOUFLAGE_ANIM =
            RawAnimation.begin().thenPlay("animation.baby_kraken.camouflage");
    private static final RawAnimation BUBBLE_BURST_ANIM =
            RawAnimation.begin().thenPlay("animation.baby_kraken.bubble_burst");

    private static final double MAX_HEALTH = 50.0D;
    private static final double PROXIMITY_RANGE = 6.0D;
    private static final int PROXIMITY_COOLDOWN_TICKS = 20 * 20;
    private static final int INK_COOLDOWN_TICKS = 20 * 20;
    private static final int BUBBLE_VOLLEY_INTERVAL_TICKS = 20 * 5;
    private static final int BUBBLE_VOLLEYS_PER_RECOVERY = 3;
    private static final int CAMOUFLAGE_DURATION_TICKS = 20 * 6;
    private static final int FLEE_DURATION_TICKS = 30;
    private static final double INK_RANGE = 18.0D;
    private static final double TARGET_LOST_RANGE = 48.0D;
    private static final double OWNER_TELEPORT_RANGE = 24.0D;
    private static final double OWNER_EVENT_TELEPORT_RANGE = 12.0D;

    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    private int proximityCooldown;
    private int inkCooldown;
    private int camouflageTicks;
    private int fleeTicks;
    private int wanderCooldown;
    private int bubbleVolleyTimer;
    private int bubbleVolleysRemaining;
    @Nullable
    private UUID bubbleTargetUuid;
    private Vec3 fleeDirection = Vec3.ZERO;

    public BabyKrakenEntity(EntityType<? extends TamableAnimal> entityType, Level level) {
        super(entityType, level);
        this.moveControl = new BabyKrakenMoveControl(this);
        this.xpReward = 4;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, MAX_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, 0.28D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.25D);
    }

    public static boolean canSpawn(
            EntityType<BabyKrakenEntity> entityType,
            ServerLevelAccessor level,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random
    ) {
        return level.getLevel().dimension().equals(ModDimensions.ABYSSAL_PLANET_LEVEL)
                && level.getFluidState(pos).is(FluidTags.WATER)
                && level.getBlockState(pos.below()).isFaceSturdy(level, pos.below(), Direction.UP);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(CAMOUFLAGED, false);
    }

    @Override
    protected void registerGoals() {
        this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new OwnerHurtTargetGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (this.level().isClientSide) {
            tickClientCamouflage();
            return;
        }

        tickCooldowns();

        if (this.isTame() && this.tickCount % 20 == 0) {
            syncPersistentPetRecord();
        }

        if (this.isOrderedToSit()) {
            this.setInSittingPose(true);
            this.navigation.stop();
            this.setTarget(null);
            lookAtOwnerWhileSitting();
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.55D, this.isInWater() ? 0.55D : 1.0D, 0.55D));
            return;
        }

        this.setInSittingPose(false);

        if (this.isTame()) {
            ServerPlayer owner = getServerOwner();
            if (owner != null && owner.level() != this.level() && followOwnerAfterTeleport(owner)) {
                return;
            }
            tickRecoveryBubbleAttack();
            tickTamedBehavior();
        } else {
            tickWildBehavior();
        }
    }

    private void tickCooldowns() {
        if (this.proximityCooldown > 0) {
            this.proximityCooldown--;
        }
        if (this.inkCooldown > 0) {
            this.inkCooldown--;
        }
        if (this.bubbleVolleyTimer > 0) {
            this.bubbleVolleyTimer--;
        }
        if (this.camouflageTicks > 0 && --this.camouflageTicks == 0) {
            setCamouflaged(false);
        }
    }

    private void tickWildBehavior() {
        if (this.fleeTicks > 0) {
            tickFleeMovement();
            return;
        }

        Player nearbyPlayer = findNearestThreateningPlayer();
        if (nearbyPlayer != null && this.proximityCooldown == 0) {
            nearbyPlayer.addEffect(new MobEffectInstance(MobEffects.POISON, 20 * 4, 0, false, true, true), this);
            nearbyPlayer.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20 * 2, 0, false, true, true), this);
            this.proximityCooldown = PROXIMITY_COOLDOWN_TICKS;
            beginFleeFrom(nearbyPlayer);
            releaseDefensiveInk();
            return;
        }

        wanderNearFloor();
    }

    private void tickTamedBehavior() {
        LivingEntity target = this.getTarget();
        if (!isValidDefensiveTarget(target)) {
            this.setTarget(null);
            tickFollowOwner();
            return;
        }

        double distanceSquared = this.distanceToSqr(target);
        if (distanceSquared > TARGET_LOST_RANGE * TARGET_LOST_RANGE) {
            this.setTarget(null);
            tickFollowOwner();
            return;
        }

        this.getLookControl().setLookAt(target, 25.0F, 25.0F);
        if (this.inkCooldown == 0 && distanceSquared <= INK_RANGE * INK_RANGE) {
            BabyKrakenInkBallEntity.spawn((ServerLevel) this.level(), this, target);
            this.inkCooldown = INK_COOLDOWN_TICKS;
            beginBubbleRecovery(target);
            this.triggerAnim("actions", "ink_spit");
            releaseInkMuzzleParticles();
        }

        if (this.inkCooldown > 0) {
            tickFollowOwner();
        } else if (distanceSquared > 9.0D * 9.0D) {
            moveToward(target, 1.1D);
        }
    }

    private boolean isValidDefensiveTarget(@Nullable LivingEntity target) {
        if (target == null || !target.isAlive() || target.level() != this.level() || this.isAlliedTo(target)) {
            return false;
        }

        return !(target instanceof Player player) || (!player.isCreative() && !player.isSpectator());
    }

    private void beginBubbleRecovery(LivingEntity target) {
        this.bubbleTargetUuid = target.getUUID();
        this.bubbleVolleysRemaining = BUBBLE_VOLLEYS_PER_RECOVERY;
        this.bubbleVolleyTimer = BUBBLE_VOLLEY_INTERVAL_TICKS;
    }

    private void tickRecoveryBubbleAttack() {
        if (this.bubbleVolleysRemaining <= 0 || this.bubbleTargetUuid == null || this.bubbleVolleyTimer > 0) {
            return;
        }
        if (!(this.level() instanceof ServerLevel serverLevel)
                || !(serverLevel.getEntity(this.bubbleTargetUuid) instanceof LivingEntity target)
                || !isValidDefensiveTarget(target)
                || this.distanceToSqr(target) > TARGET_LOST_RANGE * TARGET_LOST_RANGE) {
            clearBubbleRecovery();
            return;
        }

        this.getLookControl().setLookAt(target, 35.0F, 35.0F);
        BabyKrakenBubbleEntity.spawnVolley(serverLevel, this, target);
        this.triggerAnim("actions", "bubble_burst");
        releaseBubbleMuzzleParticles();

        this.bubbleVolleysRemaining--;
        if (this.bubbleVolleysRemaining > 0) {
            this.bubbleVolleyTimer = BUBBLE_VOLLEY_INTERVAL_TICKS;
        } else {
            clearBubbleRecovery();
        }
    }

    private void clearBubbleRecovery() {
        this.bubbleVolleyTimer = 0;
        this.bubbleVolleysRemaining = 0;
        this.bubbleTargetUuid = null;
    }

    @Override
    public boolean wantsToAttack(LivingEntity target, LivingEntity owner) {
        if (target == owner || target == this || this.isAlliedTo(target)) {
            return false;
        }

        if (target instanceof Player targetPlayer && owner instanceof Player ownerPlayer) {
            return ownerPlayer.canHarmPlayer(targetPlayer);
        }

        return !(target instanceof TamableAnimal tamable) || !tamable.isTame();
    }

    private void tickFollowOwner() {
        ServerPlayer owner = getServerOwner();
        if (owner == null) {
            stopFollowingMovement();
            return;
        }
        if (owner.level() != this.level()) {
            followOwnerAfterTeleport(owner);
            return;
        }

        double distanceSquared = this.distanceToSqr(owner);
        if (distanceSquared > OWNER_TELEPORT_RANGE * OWNER_TELEPORT_RANGE && tryTeleportNearOwner(owner)) {
            return;
        }

        Vec3 destination = getStableFollowPosition(owner);
        boolean ownerMoving = owner.getDeltaMovement().horizontalDistanceSqr() > 0.004D;
        if (distanceSquared > 4.25D * 4.25D || ownerMoving && distanceSquared > 2.65D * 2.65D) {
            moveToward(destination, 1.15D);
        } else {
            stopFollowingMovement();
        }
    }

    private Vec3 getStableFollowPosition(LivingEntity owner) {
        Vec3 horizontalLook = new Vec3(owner.getLookAngle().x, 0.0D, owner.getLookAngle().z);
        if (horizontalLook.lengthSqr() < 0.001D) {
            horizontalLook = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            horizontalLook = horizontalLook.normalize();
        }

        double targetY = owner.isInWaterOrBubble()
                ? owner.getY() + owner.getBbHeight() * 0.45D
                : owner.getY() + 0.2D;
        return new Vec3(owner.getX(), targetY, owner.getZ()).subtract(horizontalLook.scale(2.15D));
    }

    private void stopFollowingMovement() {
        this.navigation.stop();
        if (this.moveControl instanceof BabyKrakenMoveControl krakenMoveControl) {
            krakenMoveControl.stopMoving();
        }
        double damping = this.isInWaterOrBubble() ? 0.55D : 0.72D;
        this.setDeltaMovement(this.getDeltaMovement().scale(damping));
    }

    private void moveToward(LivingEntity target, double speedModifier) {
        if (this.isInWaterOrBubble()) {
            Vec3 destination = target.getEyePosition().add(0.0D, -0.35D, 0.0D);
            this.moveControl.setWantedPosition(destination.x, destination.y, destination.z, speedModifier);
        } else {
            this.navigation.moveTo(target, speedModifier);
        }
    }

    private void moveToward(Vec3 destination, double speedModifier) {
        if (this.isInWaterOrBubble()) {
            this.navigation.stop();
            this.moveControl.setWantedPosition(destination.x, destination.y, destination.z, speedModifier);
        } else {
            this.navigation.moveTo(destination.x, destination.y, destination.z, speedModifier);
        }
    }

    private boolean tryTeleportNearOwner(LivingEntity owner) {
        BlockPos origin = owner.blockPosition();
        for (int attempt = 0; attempt < 36; attempt++) {
            int x = origin.getX() + this.random.nextInt(9) - 4;
            int y = origin.getY() + this.random.nextInt(5) - 2;
            int z = origin.getZ() + this.random.nextInt(9) - 4;
            BlockPos candidate = new BlockPos(x, y, z);
            boolean waterSpace = this.level().getFluidState(candidate).is(FluidTags.WATER)
                    && this.level().getFluidState(candidate.above()).is(FluidTags.WATER);
            boolean groundSpace = this.level().getBlockState(candidate).isAir()
                    && this.level().getBlockState(candidate.above()).isAir()
                    && this.level().getBlockState(candidate.below()).isFaceSturdy(this.level(), candidate.below(), Direction.UP);

            if (!waterSpace && !groundSpace) {
                continue;
            }

            Vec3 oldPosition = this.position();
            this.moveTo(x + 0.5D, y, z + 0.5D, this.getYRot(), this.getXRot());
            if (this.level().noCollision(this, this.getBoundingBox())) {
                this.setDeltaMovement(Vec3.ZERO);
                stopFollowingMovement();
                this.hasImpulse = true;
                this.hurtMarked = true;
                return true;
            }
            this.setPos(oldPosition);
        }

        return false;
    }

    @Nullable
    private ServerPlayer getServerOwner() {
        if (!(this.level() instanceof ServerLevel serverLevel) || this.getOwnerUUID() == null) {
            return null;
        }
        return serverLevel.getServer().getPlayerList().getPlayer(this.getOwnerUUID());
    }

    public boolean isOwnedByUuid(UUID ownerUuid) {
        return ownerUuid != null && ownerUuid.equals(this.getOwnerUUID());
    }

    public void syncPersistentPetRecord() {
        if (this.level() instanceof ServerLevel serverLevel && this.isTame() && this.getOwnerUUID() != null) {
            BabyKrakenPetData.get(serverLevel.getServer()).track(this);
        }
    }

    public boolean followOwnerAfterTeleport(ServerPlayer owner) {
        if (!this.isTame() || this.isOrderedToSit() || !isOwnedByUuid(owner.getUUID())) {
            return false;
        }

        if (owner.level() == this.level()) {
            if (this.distanceToSqr(owner) > OWNER_EVENT_TELEPORT_RANGE * OWNER_EVENT_TELEPORT_RANGE) {
                return tryTeleportNearOwner(owner);
            }
            return false;
        }

        ServerLevel destinationLevel = owner.serverLevel();
        Vec3 destination = findOwnerTeleportDestination(destinationLevel, owner);
        Entity transported = this.changeDimension(destinationLevel, new ITeleporter() {
            @Override
            public PortalInfo getPortalInfo(
                    Entity entity,
                    ServerLevel destinationWorld,
                    Function<ServerLevel, PortalInfo> defaultPortalInfo
            ) {
                return new PortalInfo(destination, Vec3.ZERO, owner.getYRot(), 0.0F);
            }

            @Override
            public Entity placeEntity(
                    Entity entity,
                    ServerLevel currentWorld,
                    ServerLevel destinationWorld,
                    float yaw,
                    Function<Boolean, Entity> repositionEntity
            ) {
                Entity movedEntity = repositionEntity.apply(false);
                if (movedEntity instanceof BabyKrakenEntity movedKraken) {
                    movedKraken.moveTo(destination.x, destination.y, destination.z, owner.getYRot(), 0.0F);
                    movedKraken.setDeltaMovement(Vec3.ZERO);
                    movedKraken.setPersistenceRequired();
                    movedKraken.setOrderedToSit(false);
                    movedKraken.setInSittingPose(false);
                    movedKraken.syncPersistentPetRecord();
                }
                return movedEntity;
            }
        });
        return transported != null;
    }

    private Vec3 findOwnerTeleportDestination(ServerLevel level, LivingEntity owner) {
        BlockPos origin = owner.blockPosition();
        for (int attempt = 0; attempt < 36; attempt++) {
            int x = origin.getX() + this.random.nextInt(7) - 3;
            int y = origin.getY() + this.random.nextInt(5) - 2;
            int z = origin.getZ() + this.random.nextInt(7) - 3;
            BlockPos candidate = new BlockPos(x, y, z);
            boolean waterSpace = level.getFluidState(candidate).is(FluidTags.WATER)
                    && level.getFluidState(candidate.above()).is(FluidTags.WATER);
            boolean groundSpace = level.getBlockState(candidate).isAir()
                    && level.getBlockState(candidate.above()).isAir()
                    && level.getBlockState(candidate.below()).isFaceSturdy(level, candidate.below(), Direction.UP);
            if (waterSpace || groundSpace) {
                return new Vec3(x + 0.5D, y, z + 0.5D);
            }
        }
        return owner.position().add(0.0D, 0.35D, 0.0D);
    }

    private void beginFleeFrom(LivingEntity threat) {
        Vec3 away = this.position().subtract(threat.position()).multiply(1.0D, 0.35D, 1.0D);
        if (away.lengthSqr() < 0.001D) {
            double angle = this.random.nextDouble() * Math.PI * 2.0D;
            away = new Vec3(Math.cos(angle), 0.2D, Math.sin(angle));
        }

        this.fleeDirection = away.normalize();
        this.fleeTicks = FLEE_DURATION_TICKS;
        this.navigation.stop();
        this.setOrderedToSit(false);
        this.setInSittingPose(false);
        this.setDeltaMovement(this.fleeDirection.scale(0.72D).add(0.0D, 0.16D, 0.0D));
        this.hasImpulse = true;
        this.hurtMarked = true;
        this.triggerAnim("actions", "flee");
    }

    private void tickFleeMovement() {
        this.fleeTicks--;
        Vec3 desired = this.fleeDirection.scale(this.isInWater() ? 0.19D : 0.11D);
        Vec3 current = this.getDeltaMovement();
        this.setDeltaMovement(
                current.x * 0.72D + desired.x,
                current.y * 0.72D + desired.y * 0.35D,
                current.z * 0.72D + desired.z
        );
        turnToward(this.getDeltaMovement());
        this.hasImpulse = true;
        this.hurtMarked = true;
    }

    @Nullable
    private Player findNearestThreateningPlayer() {
        Player nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Player player : this.level().getEntitiesOfClass(
                Player.class,
                this.getBoundingBox().inflate(PROXIMITY_RANGE),
                candidate -> candidate.isAlive() && !candidate.isCreative() && !candidate.isSpectator()
        )) {
            double distance = this.distanceToSqr(player);
            if (distance < nearestDistance) {
                nearest = player;
                nearestDistance = distance;
            }
        }

        return nearest;
    }

    private void wanderNearFloor() {
        if (--this.wanderCooldown > 0 && (this.moveControl.hasWanted() || !this.navigation.isDone())) {
            return;
        }

        this.wanderCooldown = 70 + this.random.nextInt(100);
        if (this.isInWaterOrBubble()) {
            double angle = this.random.nextDouble() * Math.PI * 2.0D;
            double distance = 4.0D + this.random.nextDouble() * 8.0D;
            int x = Mth.floor(this.getX() + Math.cos(angle) * distance);
            int z = Mth.floor(this.getZ() + Math.sin(angle) * distance);
            int floorWaterY = findFloorWaterY(x, Math.min(this.level().getMaxBuildHeight() - 2, this.blockPosition().getY() + 7), z);
            double targetY = floorWaterY == Integer.MIN_VALUE ? this.getY() : floorWaterY + 0.35D;
            this.moveControl.setWantedPosition(x + 0.5D, targetY, z + 0.5D, 0.72D);
            return;
        }

        Vec3 position = DefaultRandomPos.getPos(this, 8, 3);
        if (position != null) {
            this.navigation.moveTo(position.x, position.y, position.z, 0.8D);
        }
    }

    private int findFloorWaterY(int x, int startY, int z) {
        int minimumY = Math.max(this.level().getMinBuildHeight() + 1, startY - 24);
        for (int y = startY; y >= minimumY; y--) {
            BlockPos water = new BlockPos(x, y, z);
            BlockPos floor = water.below();
            if (this.level().getFluidState(water).is(FluidTags.WATER)
                    && this.level().getBlockState(floor).isFaceSturdy(this.level(), floor, Direction.UP)) {
                return y;
            }
        }
        return Integer.MIN_VALUE;
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack heldItem = player.getItemInHand(hand);
        if (heldItem.is(ModItems.ABYSSAL_COMPOST.get())) {
            if (this.level().isClientSide) {
                return InteractionResult.CONSUME;
            }

            if (this.isTame()) {
                if (this.getHealth() < this.getMaxHealth()) {
                    this.heal(2.0F);
                    consumeCompost(player, hand, heldItem);
                    this.gameEvent(GameEvent.EAT, this);
                    this.level().broadcastEntityEvent(this, (byte) 7);
                    releaseHealingHearts();
                }
                return InteractionResult.SUCCESS;
            }

            consumeCompost(player, hand, heldItem);
            if (this.random.nextInt(4) == 0 && !ForgeEventFactory.onAnimalTame(this, player)) {
                this.tame(player);
                this.setHealth(this.getMaxHealth());
                this.setOrderedToSit(false);
                this.setInSittingPose(false);
                this.navigation.stop();
                this.setTarget(null);
                this.setPersistenceRequired();
                syncPersistentPetRecord();
                this.level().broadcastEntityEvent(this, (byte) 7);
            } else {
                this.level().broadcastEntityEvent(this, (byte) 6);
            }
            return InteractionResult.SUCCESS;
        }

        if (this.isTame() && this.isOwnedBy(player)) {
            if (!this.level().isClientSide) {
                boolean sitting = !this.isOrderedToSit();
                this.setOrderedToSit(sitting);
                this.setInSittingPose(sitting);
                this.navigation.stop();
                this.setTarget(null);
                if (sitting) {
                    clearBubbleRecovery();
                }
                syncPersistentPetRecord();
            }
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        return super.mobInteract(player, hand);
    }

    private void consumeCompost(Player player, InteractionHand hand, ItemStack stack) {
        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        player.swing(hand, true);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (!hurt || this.level().isClientSide) {
            return hurt;
        }

        this.camouflageTicks = CAMOUFLAGE_DURATION_TICKS;
        setCamouflaged(true);
        this.triggerAnim("actions", "camouflage");
        releaseCamouflageParticles();

        if (source.getEntity() instanceof LivingEntity attacker) {
            beginFleeFrom(attacker);
        }

        return true;
    }

    @Override
    public void die(DamageSource source) {
        super.die(source);
        if (this.level() instanceof ServerLevel serverLevel) {
            BabyKrakenPetData.get(serverLevel.getServer()).remove(this.getUUID());
        }
    }

    private void releaseDefensiveInk() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        serverLevel.sendParticles(
                ParticleTypes.SQUID_INK,
                this.getX(),
                this.getY() + this.getBbHeight() * 0.45D,
                this.getZ(),
                28,
                0.45D,
                0.3D,
                0.45D,
                0.07D
        );
    }

    private void releaseInkMuzzleParticles() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        Vec3 look = this.getLookAngle();
        serverLevel.sendParticles(
                ParticleTypes.SQUID_INK,
                this.getX() + look.x * 0.45D,
                this.getEyeY() - 0.18D,
                this.getZ() + look.z * 0.45D,
                10,
                0.12D,
                0.12D,
                0.12D,
                0.025D
        );
    }

    private void releaseBubbleMuzzleParticles() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        Vec3 look = this.getLookAngle();
        serverLevel.sendParticles(
                ParticleTypes.BUBBLE,
                this.getX() + look.x * 0.55D,
                this.getEyeY() - 0.12D,
                this.getZ() + look.z * 0.55D,
                18,
                0.18D,
                0.16D,
                0.18D,
                0.08D
        );
    }

    private void releaseHealingHearts() {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                    ParticleTypes.HEART,
                    this.getX(),
                    this.getY() + this.getBbHeight() * 0.8D,
                    this.getZ(),
                    7,
                    0.42D,
                    0.35D,
                    0.42D,
                    0.04D
            );
        }
    }

    private void releaseCamouflageParticles() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        serverLevel.sendParticles(
                ParticleTypes.BUBBLE,
                this.getX(),
                this.getY() + this.getBbHeight() * 0.5D,
                this.getZ(),
                26,
                0.5D,
                0.45D,
                0.5D,
                0.05D
        );
    }

    private void tickClientCamouflage() {
        if (!isCamouflaged() || this.tickCount % 5 != 0) {
            return;
        }

        this.level().addParticle(
                ParticleTypes.BUBBLE,
                this.getRandomX(0.7D),
                this.getRandomY(),
                this.getRandomZ(0.7D),
                0.0D,
                0.015D,
                0.0D
        );
    }

    private void turnToward(Vec3 movement) {
        if (movement.horizontalDistanceSqr() < 0.0001D) {
            return;
        }

        float targetYaw = (float) (Mth.atan2(movement.z, movement.x) * Mth.RAD_TO_DEG) - 90.0F;
        this.setYRot(Mth.rotLerp(0.3F, this.getYRot(), targetYaw));
        this.yBodyRot = this.getYRot();
        this.yHeadRot = this.getYRot();
    }

    private void lookAtOwnerWhileSitting() {
        ServerPlayer owner = getServerOwner();
        if (owner == null || owner.level() != this.level()) {
            return;
        }

        this.getLookControl().setLookAt(owner, 40.0F, 35.0F);
        Vec3 towardOwner = owner.position().subtract(this.position());
        if (towardOwner.horizontalDistanceSqr() > 0.001D) {
            float targetYaw = (float) (Mth.atan2(towardOwner.z, towardOwner.x) * Mth.RAD_TO_DEG) - 90.0F;
            float smoothedYaw = Mth.rotLerp(0.25F, this.getYRot(), targetYaw);
            this.setYRot(smoothedYaw);
            this.yBodyRot = smoothedYaw;
            this.yHeadRot = smoothedYaw;
        }
    }

    public boolean isCamouflaged() {
        return this.entityData.get(CAMOUFLAGED);
    }

    private void setCamouflaged(boolean camouflaged) {
        this.entityData.set(CAMOUFLAGED, camouflaged);
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (this.isControlledByLocalInstance() && this.isInWater()) {
            this.moveRelative(this.getSpeed(), travelVector);
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.9D));
            return;
        }

        super.travel(travelVector);
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    protected int decreaseAirSupply(int airSupply) {
        return airSupply;
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    @Override
    public void setTame(boolean tame) {
        super.setTame(tame);
        if (tame) {
            this.setPersistenceRequired();
        }
    }

    @Override
    public boolean requiresCustomPersistence() {
        return this.isTame() || super.requiresCustomPersistence();
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return !this.isTame() && !this.hasCustomName();
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return false;
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("ProximityCooldown", this.proximityCooldown);
        tag.putInt("InkCooldown", this.inkCooldown);
        tag.putInt("CamouflageTicks", this.camouflageTicks);
        tag.putInt("BubbleVolleyTimer", this.bubbleVolleyTimer);
        tag.putInt("BubbleVolleysRemaining", this.bubbleVolleysRemaining);
        if (this.bubbleTargetUuid != null) {
            tag.putUUID("BubbleTarget", this.bubbleTargetUuid);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.proximityCooldown = Math.max(0, tag.getInt("ProximityCooldown"));
        this.inkCooldown = Math.max(0, tag.getInt("InkCooldown"));
        this.camouflageTicks = Math.max(0, tag.getInt("CamouflageTicks"));
        this.bubbleVolleyTimer = Math.max(0, tag.getInt("BubbleVolleyTimer"));
        this.bubbleVolleysRemaining = Mth.clamp(tag.getInt("BubbleVolleysRemaining"), 0, BUBBLE_VOLLEYS_PER_RECOVERY);
        this.bubbleTargetUuid = tag.hasUUID("BubbleTarget") ? tag.getUUID("BubbleTarget") : null;
        if (this.isTame()) {
            this.setPersistenceRequired();
        }
        setCamouflaged(this.camouflageTicks > 0);
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "movement", 6, this::movementController));

        AnimationController<BabyKrakenEntity> actions = new AnimationController<>(
                this,
                "actions",
                2,
                state -> PlayState.STOP
        );
        actions.triggerableAnim("flee", FLEE_ANIM)
                .triggerableAnim("ink_spit", INK_SPIT_ANIM)
                .triggerableAnim("camouflage", CAMOUFLAGE_ANIM)
                .triggerableAnim("bubble_burst", BUBBLE_BURST_ANIM);
        controllers.add(actions);
    }

    private PlayState movementController(AnimationState<BabyKrakenEntity> state) {
        if (this.isInSittingPose()) {
            return state.setAndContinue(SIT_ANIM);
        }
        if (!this.isInWaterOrBubble()) {
            return state.setAndContinue(this.getDeltaMovement().horizontalDistanceSqr() > 0.001D ? CRAWL_ANIM : SIT_ANIM);
        }
        if (this.getDeltaMovement().lengthSqr() > 0.0025D) {
            return state.setAndContinue(SWIM_ANIM);
        }
        return state.setAndContinue(IDLE_ANIM);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.geoCache;
    }

    private static final class BabyKrakenMoveControl extends MoveControl {
        private final BabyKrakenEntity kraken;

        private BabyKrakenMoveControl(BabyKrakenEntity kraken) {
            super(kraken);
            this.kraken = kraken;
        }

        private void stopMoving() {
            this.operation = Operation.WAIT;
        }

        @Override
        public void tick() {
            if (!this.kraken.isInWaterOrBubble() || this.operation != Operation.MOVE_TO) {
                super.tick();
                return;
            }

            Vec3 toTarget = new Vec3(
                    this.wantedX - this.kraken.getX(),
                    this.wantedY - this.kraken.getY(),
                    this.wantedZ - this.kraken.getZ()
            );
            double distance = toTarget.length();
            if (distance < 0.72D) {
                stopMoving();
                this.kraken.setDeltaMovement(this.kraken.getDeltaMovement().scale(0.45D));
                return;
            }

            Vec3 direction = toTarget.normalize();
            double speedScale = Mth.clamp(this.speedModifier, 0.4D, 1.4D);
            double maximumSpeed = 0.16D + 0.11D * speedScale;
            double desiredSpeed = Math.min(maximumSpeed, 0.035D + distance * 0.045D);
            Vec3 desiredMovement = direction.scale(desiredSpeed);
            double steering = distance < 2.5D ? 0.52D : 0.34D;
            Vec3 movement = this.kraken.getDeltaMovement().scale(1.0D - steering)
                    .add(desiredMovement.scale(steering));
            if (movement.lengthSqr() > maximumSpeed * maximumSpeed) {
                movement = movement.normalize().scale(maximumSpeed);
            }

            this.kraken.setDeltaMovement(movement);
            this.kraken.turnToward(movement);
            this.kraken.setSpeed((float) (this.speedModifier * this.kraken.getAttributeValue(Attributes.MOVEMENT_SPEED)));
            this.kraken.hasImpulse = true;
        }
    }
}
