package fr.blue.abyssalplanet.entity;

import fr.blue.abyssalplanet.world.AbyssalCompanionData;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.MoveControl;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.util.ITeleporter;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.util.GeckoLibUtil;

import java.util.UUID;
import java.util.function.Function;

public abstract class AbstractAbyssalCompanionEntity extends TamableAnimal implements GeoEntity {
    private static final double OWNER_TELEPORT_RANGE = 52.0D;
    private final AnimatableInstanceCache geoCache = GeckoLibUtil.createInstanceCache(this);

    protected AbstractAbyssalCompanionEntity(
            EntityType<? extends TamableAnimal> entityType,
            Level level
    ) {
        super(entityType, level);
        moveControl = new CompanionMoveControl(this);
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide || !isTame()) {
            return;
        }

        if (tickCount % 20 == 0) {
            syncPersistentPetRecord();
        }

        if (isOrderedToSit()) {
            setInSittingPose(true);
            navigation.stop();
            setDeltaMovement(getDeltaMovement().scale(isInWaterOrBubble() ? 0.55D : 0.72D));
            lookAtOwnerWhileSitting();
            return;
        }

        setInSittingPose(false);
        ServerPlayer owner = getServerOwner();
        if (owner != null && owner.level() != level()) {
            followOwnerAfterTeleport(owner);
            return;
        }
        tickFollowOwner(owner);
    }

    protected void tickFollowOwner(@Nullable ServerPlayer owner) {
        if (owner == null || isPassenger() || isVehicle()) {
            return;
        }

        double distanceSquared = distanceToSqr(owner);
        if (distanceSquared > OWNER_TELEPORT_RANGE * OWNER_TELEPORT_RANGE
                && tryTeleportNearOwner(owner)) {
            return;
        }

        Vec3 destination = stableFollowPosition(owner);
        boolean ownerMoving = owner.getDeltaMovement().horizontalDistanceSqr() > 0.003D
                || Math.abs(owner.getDeltaMovement().y) > 0.025D;
        boolean shouldFollow = distanceSquared > 3.75D * 3.75D
                || ownerMoving && distanceSquared > 2.55D * 2.55D;
        if (shouldFollow) {
            if (isInWaterOrBubble()) {
                navigation.stop();
                double speed = getWaterFollowSpeed() * Mth.clamp(
                        0.9D + Math.sqrt(distanceSquared) / 18.0D,
                        1.0D,
                        1.45D
                );
                moveControl.setWantedPosition(
                        destination.x,
                        destination.y,
                        destination.z,
                        speed
                );
            } else {
                navigation.moveTo(destination.x, destination.y, destination.z, getLandFollowSpeed());
                tickLandHop();
            }
        } else {
            navigation.stop();
            if (moveControl instanceof CompanionMoveControl control) {
                control.stopMoving();
            }
            setDeltaMovement(getDeltaMovement().scale(0.68D));
        }
    }

    protected double getWaterFollowSpeed() {
        return 1.15D;
    }

    protected double getLandFollowSpeed() {
        return 0.75D;
    }

    protected double getMaximumWaterSpeed() {
        return 0.28D;
    }

    protected void tickLandHop() {
        if (onGround() && tickCount % 18 == 0) {
            setDeltaMovement(getDeltaMovement().add(0.0D, 0.28D, 0.0D));
            hasImpulse = true;
        }
    }

    private Vec3 stableFollowPosition(ServerPlayer owner) {
        Vec3 look = new Vec3(owner.getLookAngle().x, 0.0D, owner.getLookAngle().z);
        if (look.lengthSqr() < 0.001D) {
            look = new Vec3(0.0D, 0.0D, 1.0D);
        } else {
            look = look.normalize();
        }
        double y = owner.isInWaterOrBubble()
                ? owner.getY() + owner.getBbHeight() * 0.42D
                : owner.getY() + 0.1D;
        double followDistance = Math.max(2.25D, getBbWidth() * 1.05D);
        return new Vec3(owner.getX(), y, owner.getZ()).subtract(look.scale(followDistance));
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (isTame() && isOwnedBy(player) && held.isEmpty()) {
            if (!level().isClientSide) {
                boolean sitting = !isOrderedToSit();
                setOrderedToSit(sitting);
                setInSittingPose(sitting);
                navigation.stop();
                setTarget(null);
                syncPersistentPetRecord();
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }
        return super.mobInteract(player, hand);
    }

    protected void tameFor(Player player) {
        tame(player);
        setHealth(getMaxHealth());
        setOrderedToSit(false);
        setInSittingPose(false);
        setPersistenceRequired();
        navigation.stop();
        level().broadcastEntityEvent(this, (byte) 7);
        syncPersistentPetRecord();
    }

    public void syncPersistentPetRecord() {
        if (level() instanceof ServerLevel serverLevel && isTame() && getOwnerUUID() != null) {
            AbyssalCompanionData.get(serverLevel.getServer()).track(this);
        }
    }

    public boolean isOwnedByUuid(UUID ownerUuid) {
        return ownerUuid != null && ownerUuid.equals(getOwnerUUID());
    }

    public boolean followOwnerAfterTeleport(ServerPlayer owner) {
        if (!isTame() || isOrderedToSit() || !isOwnedByUuid(owner.getUUID())) {
            return false;
        }
        if (owner.level() == level()) {
            return distanceToSqr(owner) > 10.0D * 10.0D && tryTeleportNearOwner(owner);
        }

        ServerLevel destinationLevel = owner.serverLevel();
        Vec3 destination = findTeleportDestination(destinationLevel, owner);
        Entity transported = changeDimension(destinationLevel, new ITeleporter() {
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
                Entity moved = repositionEntity.apply(false);
                if (moved instanceof AbstractAbyssalCompanionEntity companion) {
                    companion.moveTo(destination.x, destination.y, destination.z, owner.getYRot(), 0.0F);
                    companion.setDeltaMovement(Vec3.ZERO);
                    companion.setPersistenceRequired();
                    companion.syncPersistentPetRecord();
                }
                return moved;
            }
        });
        return transported != null;
    }

    private boolean tryTeleportNearOwner(ServerPlayer owner) {
        Vec3 destination = findTeleportDestination(owner.serverLevel(), owner);
        Vec3 previous = position();
        moveTo(destination.x, destination.y, destination.z, owner.getYRot(), 0.0F);
        if (level().noCollision(this, getBoundingBox())) {
            setDeltaMovement(Vec3.ZERO);
            hasImpulse = true;
            hurtMarked = true;
            return true;
        }
        setPos(previous);
        return false;
    }

    private Vec3 findTeleportDestination(ServerLevel level, ServerPlayer owner) {
        BlockPos origin = owner.blockPosition();
        for (int attempt = 0; attempt < 40; attempt++) {
            BlockPos candidate = origin.offset(
                    random.nextInt(9) - 4,
                    random.nextInt(5) - 2,
                    random.nextInt(9) - 4
            );
            boolean water = level.getFluidState(candidate).is(FluidTags.WATER)
                    && level.getFluidState(candidate.above()).is(FluidTags.WATER);
            boolean ground = level.getBlockState(candidate).isAir()
                    && level.getBlockState(candidate.above()).isAir()
                    && level.getBlockState(candidate.below()).isFaceSturdy(
                            level,
                            candidate.below(),
                            Direction.UP
                    );
            if (water || ground) {
                return Vec3.atBottomCenterOf(candidate);
            }
        }
        return owner.position().add(0.0D, 0.35D, 0.0D);
    }

    @Nullable
    protected ServerPlayer getServerOwner() {
        if (!(level() instanceof ServerLevel serverLevel) || getOwnerUUID() == null) {
            return null;
        }
        return serverLevel.getServer().getPlayerList().getPlayer(getOwnerUUID());
    }

    private void lookAtOwnerWhileSitting() {
        ServerPlayer owner = getServerOwner();
        if (owner == null || owner.level() != level()) {
            return;
        }
        getLookControl().setLookAt(owner, 40.0F, 35.0F);
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (isControlledByLocalInstance() && isInWater()) {
            moveRelative(getSpeed(), travelVector);
            move(MoverType.SELF, getDeltaMovement());
            setDeltaMovement(getDeltaMovement().scale(0.9D));
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
            setPersistenceRequired();
        }
    }

    @Override
    public boolean requiresCustomPersistence() {
        return isTame() || super.requiresCustomPersistence();
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return !isTame() && !hasCustomName();
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
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return geoCache;
    }

    private static final class CompanionMoveControl extends MoveControl {
        private final AbstractAbyssalCompanionEntity companion;

        private CompanionMoveControl(AbstractAbyssalCompanionEntity companion) {
            super(companion);
            this.companion = companion;
        }

        private void stopMoving() {
            operation = Operation.WAIT;
        }

        @Override
        public void tick() {
            if (!companion.isInWaterOrBubble() || operation != Operation.MOVE_TO) {
                super.tick();
                return;
            }

            Vec3 delta = new Vec3(
                    wantedX - companion.getX(),
                    wantedY - companion.getY(),
                    wantedZ - companion.getZ()
            );
            double distance = delta.length();
            if (distance < 0.7D) {
                stopMoving();
                companion.setDeltaMovement(companion.getDeltaMovement().scale(0.5D));
                return;
            }

            double maximum = companion.getMaximumWaterSpeed();
            double speedScale = Mth.clamp(speedModifier, 0.55D, 1.65D);
            double desiredSpeed = Math.min(
                    maximum,
                    0.04D + distance * 0.048D * speedScale
            );
            Vec3 desired = delta.normalize().scale(desiredSpeed);
            double steering = distance < 3.0D ? 0.52D : 0.38D;
            Vec3 movement = companion.getDeltaMovement().scale(1.0D - steering)
                    .add(desired.scale(steering));
            if (movement.lengthSqr() > maximum * maximum) {
                movement = movement.normalize().scale(maximum);
            }
            companion.setDeltaMovement(movement);
            companion.setSpeed((float) (speedModifier * companion.getAttributeValue(Attributes.MOVEMENT_SPEED)));
            if (movement.horizontalDistanceSqr() > 0.0001D) {
                float yaw = (float) (Mth.atan2(movement.z, movement.x) * Mth.RAD_TO_DEG) - 90.0F;
                companion.setYRot(Mth.rotLerp(0.32F, companion.getYRot(), yaw));
                companion.yBodyRot = companion.getYRot();
            }
            companion.hasImpulse = true;
        }
    }
}
