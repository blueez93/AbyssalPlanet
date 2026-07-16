package fr.blue.abyssalplanet.entity;

import fr.blue.abyssalplanet.item.ZwoingItem;
import fr.blue.abyssalplanet.registry.ModDimensions;
import fr.blue.abyssalplanet.registry.ModItems;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

public class ZwoingEntity extends PathfinderMob {
    public static final int BLUE = 0;
    public static final int RED = 1;
    public static final int GREEN = 2;
    public static final int PURPLE = 3;
    public static final int MULTICOLOR = 4;
    public static final int EFFECT_NONE = 0;
    public static final int EFFECT_TOXIN = 1;
    public static final int EFFECT_INK = 2;

    public static final int MOTION_BOUNCING = 0;
    public static final int MOTION_RISING = 1;
    public static final int MOTION_HOVERING = 2;
    public static final int MOTION_DESCENDING = 3;

    private static final EntityDataAccessor<Integer> VARIANT =
            SynchedEntityData.defineId(ZwoingEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> EQUIPPED_EFFECT =
            SynchedEntityData.defineId(ZwoingEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<Integer> MOTION_STATE =
            SynchedEntityData.defineId(ZwoingEntity.class, EntityDataSerializers.INT);
    private static final double FLEE_RANGE = 7.0D;
    private static final double FLEE_SPEED = 0.075D;
    private static final double WANDER_SPEED = 0.018D;
    private static final int ARMOR_EFFECT_INTERVAL = 10;
    private static final double NORMAL_MAX_HEALTH = 5.0D;
    private static final double MULTICOLOR_MAX_HEALTH = 50.0D;
    private static final float COMPOST_HEAL_AMOUNT = 8.0F;
    private static final double BOUNCE_GRAVITY = 0.018D;
    private static final double MAX_FALL_SPEED = -0.095D;
    private static final double FLOAT_RISE_SPEED = 0.052D;
    private static final double FLOAT_DESCENT_SPEED = -0.082D;

    @Nullable
    private UUID ownerUuid;
    @Nullable
    private UUID attachedTargetUuid;
    private int wanderTicks;
    private Vec3 wanderDirection = Vec3.ZERO;
    private int bounceCooldown = 8;
    private int motionStateTicks;
    private Vec3 floatingDirection = Vec3.ZERO;
    private boolean wasTouchingBottom;
    private float targetSquish;
    private float squish;
    private float oldSquish;
    private float floatTurn;
    private float oldFloatTurn;

    public ZwoingEntity(EntityType<? extends PathfinderMob> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 1;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, NORMAL_MAX_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, 0.10D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.15D);
    }

    public static boolean canSpawn(
            EntityType<ZwoingEntity> entityType,
            ServerLevelAccessor level,
            MobSpawnType spawnType,
            BlockPos pos,
            RandomSource random
    ) {
        BlockPos floor = pos.below();
        return level.getLevel().dimension().equals(ModDimensions.ABYSSAL_PLANET_LEVEL)
                && level.getFluidState(pos).is(FluidTags.WATER)
                && level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(VARIANT, BLUE);
        this.entityData.define(EQUIPPED_EFFECT, EFFECT_NONE);
        this.entityData.define(MOTION_STATE, MOTION_BOUNCING);
    }

    @Override
    protected void registerGoals() {
    }

    @Override
    public void tick() {
        this.oldSquish = this.squish;
        this.squish += (this.targetSquish - this.squish) * 0.38F;
        this.oldFloatTurn = this.floatTurn;

        super.tick();

        if (this.attachedTargetUuid == null && this.isInWater()) {
            boolean touchingBottom = isTouchingBottom();
            if (this.tickCount > 2 && touchingBottom && !this.wasTouchingBottom) {
                this.targetSquish = -0.48F;
                spawnLandingBubbles();
            } else if (!touchingBottom && this.wasTouchingBottom
                    && this.getMotionState() != MOTION_DESCENDING) {
                this.targetSquish = 0.62F;
            }
            this.wasTouchingBottom = touchingBottom;

            float targetTurn = switch (this.getMotionState()) {
                case MOTION_RISING -> -0.20F;
                case MOTION_HOVERING -> Mth.sin((this.tickCount + this.getId() * 3) * 0.11F) * 0.10F;
                case MOTION_DESCENDING -> 0.88F;
                default -> 0.0F;
            };
            this.floatTurn += (targetTurn - this.floatTurn) * 0.11F;
        } else {
            this.wasTouchingBottom = false;
            this.floatTurn += (0.0F - this.floatTurn) * 0.18F;
        }

        this.targetSquish *= 0.68F;
    }

    @Override
    public void aiStep() {
        super.aiStep();

        if (this.level().isClientSide) {
            return;
        }

        if (this.attachedTargetUuid != null) {
            tickAttached();
        } else {
            tickBottomMovement();
        }
    }

    @Override
    public void travel(Vec3 travelVector) {
        if (this.isInWater()) {
            this.move(MoverType.SELF, this.getDeltaMovement());
            this.setDeltaMovement(this.getDeltaMovement().scale(0.78D));
            return;
        }

        super.travel(travelVector);
    }

    private void tickBottomMovement() {
        this.noPhysics = false;
        Player nearbyPlayer = findNearbyPlayer();

        Vec3 desiredMovement;
        if (nearbyPlayer != null) {
            Vec3 away = this.position().subtract(nearbyPlayer.position()).multiply(1.0D, 0.0D, 1.0D);
            desiredMovement = away.lengthSqr() > 0.001D
                    ? away.normalize().scale(FLEE_SPEED)
                    : randomHorizontalDirection(FLEE_SPEED);
            this.wanderTicks = 0;
        } else {
            if (this.wanderTicks-- <= 0) {
                this.wanderDirection = this.random.nextFloat() < 0.55F
                        ? Vec3.ZERO
                        : randomHorizontalDirection(WANDER_SPEED);
                this.wanderTicks = 100 + this.random.nextInt(141);
            }
            desiredMovement = this.wanderDirection;
        }

        if (!hasFloorAt(this.position().add(desiredMovement.scale(5.0D)))
                && this.getMotionState() == MOTION_BOUNCING) {
            desiredMovement = Vec3.ZERO;
            this.wanderDirection = Vec3.ZERO;
        }

        switch (this.getMotionState()) {
            case MOTION_RISING -> tickFloatingRise();
            case MOTION_HOVERING -> tickFloatingPause();
            case MOTION_DESCENDING -> tickFloatingDescent();
            default -> tickBottomBounces(desiredMovement);
        }

        turnTowardMovement();
        this.hurtMarked = true;
    }

    private void tickBottomBounces(Vec3 desiredMovement) {
        Vec3 current = this.getDeltaMovement();
        boolean touchingBottom = isTouchingBottom();

        if (touchingBottom) {
            if (this.bounceCooldown-- <= 0) {
                if (this.random.nextFloat() < 0.11F && hasWaterAbove(4)) {
                    beginFloatingCycle();
                    return;
                }

                double jumpStrength = 0.19D + this.random.nextDouble() * 0.065D;
                Vec3 hopDirection = desiredMovement.lengthSqr() > 0.0001D
                        ? desiredMovement
                        : randomHorizontalDirection(0.012D + this.random.nextDouble() * 0.018D);
                this.setDeltaMovement(hopDirection.x, jumpStrength, hopDirection.z);
                this.bounceCooldown = 8 + this.random.nextInt(15);
                this.hasImpulse = true;
                return;
            }

            this.setDeltaMovement(
                    current.x * 0.35D + desiredMovement.x * 0.35D,
                    -0.012D,
                    current.z * 0.35D + desiredMovement.z * 0.35D
            );
            return;
        }

        double verticalMovement = Math.max(MAX_FALL_SPEED, current.y - BOUNCE_GRAVITY);
        this.setDeltaMovement(
                current.x * 0.72D + desiredMovement.x * 0.18D,
                verticalMovement,
                current.z * 0.72D + desiredMovement.z * 0.18D
        );
    }

    private void beginFloatingCycle() {
        setMotionState(MOTION_RISING);
        this.motionStateTicks = 42 + this.random.nextInt(35);
        this.floatingDirection = randomHorizontalDirection(0.008D + this.random.nextDouble() * 0.009D);
        Vec3 current = this.getDeltaMovement();
        this.setDeltaMovement(this.floatingDirection.x, Math.max(0.035D, current.y), this.floatingDirection.z);
        this.hasImpulse = true;
    }

    private void tickFloatingRise() {
        if (--this.motionStateTicks <= 0 || !hasWaterAbove(2)) {
            setMotionState(MOTION_HOVERING);
            this.motionStateTicks = 28 + this.random.nextInt(35);
            return;
        }

        Vec3 current = this.getDeltaMovement();
        this.setDeltaMovement(
                current.x * 0.82D + this.floatingDirection.x * 0.35D,
                current.y * 0.55D + FLOAT_RISE_SPEED * 0.45D,
                current.z * 0.82D + this.floatingDirection.z * 0.35D
        );
    }

    private void tickFloatingPause() {
        if (--this.motionStateTicks <= 0) {
            setMotionState(MOTION_DESCENDING);
            this.motionStateTicks = 140;
            return;
        }

        Vec3 current = this.getDeltaMovement();
        double gentleBob = Math.sin((this.tickCount + this.getId() * 5) * 0.16D) * 0.006D;
        this.setDeltaMovement(current.x * 0.70D, gentleBob, current.z * 0.70D);
    }

    private void tickFloatingDescent() {
        if (isTouchingBottom() || --this.motionStateTicks <= 0) {
            setMotionState(MOTION_BOUNCING);
            this.bounceCooldown = 10 + this.random.nextInt(16);
            this.setDeltaMovement(this.getDeltaMovement().multiply(0.25D, 0.0D, 0.25D));
            return;
        }

        Vec3 current = this.getDeltaMovement();
        this.setDeltaMovement(
                current.x * 0.82D,
                current.y * 0.62D + FLOAT_DESCENT_SPEED * 0.38D,
                current.z * 0.82D
        );
    }

    private void turnTowardMovement() {
        Vec3 movement = this.getDeltaMovement();
        if (movement.horizontalDistanceSqr() < 0.00002D) {
            return;
        }

        float targetYaw = (float) (Mth.atan2(movement.z, movement.x) * Mth.RAD_TO_DEG) - 90.0F;
        this.setYRot(Mth.rotLerp(0.18F, this.getYRot(), targetYaw));
        this.yBodyRot = this.getYRot();
        this.yHeadRot = this.getYRot();
    }

    private boolean hasWaterAbove(int blocks) {
        BlockPos origin = this.blockPosition();
        for (int offset = 1; offset <= blocks; offset++) {
            if (!this.level().getFluidState(origin.above(offset)).is(FluidTags.WATER)) {
                return false;
            }
        }
        return true;
    }

    private boolean isTouchingBottom() {
        double feetY = this.getBoundingBox().minY;
        BlockPos floor = BlockPos.containing(this.getX(), feetY - 0.08D, this.getZ());
        return this.level().getBlockState(floor).isFaceSturdy(this.level(), floor, Direction.UP)
                && feetY <= floor.getY() + 1.14D;
    }

    private void spawnLandingBubbles() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        serverLevel.sendParticles(
                ParticleTypes.BUBBLE,
                this.getX(),
                this.getBoundingBox().minY + 0.12D,
                this.getZ(),
                9,
                0.26D,
                0.06D,
                0.26D,
                0.035D
        );
        serverLevel.sendParticles(
                ParticleTypes.BUBBLE_POP,
                this.getX(),
                this.getBoundingBox().minY + 0.10D,
                this.getZ(),
                3,
                0.18D,
                0.03D,
                0.18D,
                0.015D
        );
    }

    @Nullable
    private Player findNearbyPlayer() {
        List<Player> players = this.level().getEntitiesOfClass(
                Player.class,
                this.getBoundingBox().inflate(FLEE_RANGE),
                player -> player.isAlive() && !player.isCreative() && !player.isSpectator()
        );
        Player nearest = null;
        double nearestDistance = Double.MAX_VALUE;

        for (Player player : players) {
            double distance = this.distanceToSqr(player);
            if (distance < nearestDistance) {
                nearest = player;
                nearestDistance = distance;
            }
        }

        return nearest;
    }

    private void tickAttached() {
        if (!(this.level() instanceof ServerLevel serverLevel)
                || !(serverLevel.getEntity(this.attachedTargetUuid) instanceof LivingEntity target)
                || !target.isAlive()) {
            detach();
            return;
        }

        this.noPhysics = true;
        this.setDeltaMovement(Vec3.ZERO);
        double angle = (this.getId() * 0.73D + target.tickCount * 0.025D) % (Math.PI * 2.0D);
        double radius = Math.max(0.22D, target.getBbWidth() * 0.42D);
        this.setPos(
                target.getX() + Math.cos(angle) * radius,
                target.getY() + target.getBbHeight() * 0.68D,
                target.getZ() + Math.sin(angle) * radius
        );

        if (this.tickCount % 20 == 0) {
            target.addEffect(new MobEffectInstance(
                    MobEffects.WEAKNESS,
                    35,
                    0,
                    false,
                    true,
                    true
            ), this);
            applyEquippedEffect(target);
        }

        if (this.tickCount % ARMOR_EFFECT_INTERVAL != 0) {
            return;
        }

        if (target instanceof Player playerTarget) {
            damageArmor(playerTarget);
        } else {
            repairOwnerArmor(serverLevel);
        }
    }

    private void applyEquippedEffect(LivingEntity target) {
        if (!isMulticolor()) {
            return;
        }

        int effect = getEquippedEffect();
        if (effect == EFFECT_TOXIN) {
            target.addEffect(new MobEffectInstance(
                    MobEffects.POISON,
                    45,
                    0,
                    false,
                    true,
                    true
            ), this);
        } else if (effect == EFFECT_INK) {
            target.addEffect(new MobEffectInstance(
                    MobEffects.BLINDNESS,
                    45,
                    0,
                    false,
                    true,
                    true
            ), this);
        }
    }

    private void damageArmor(Player target) {
        for (EquipmentSlot slot : armorSlots()) {
            ItemStack armor = target.getItemBySlot(slot);
            if (!armor.isEmpty() && armor.isDamageableItem()) {
                armor.hurtAndBreak(1, target, entity -> entity.broadcastBreakEvent(slot));
            }
        }
    }

    private void repairOwnerArmor(ServerLevel level) {
        if (this.ownerUuid == null) {
            return;
        }

        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(this.ownerUuid);
        if (owner == null || !owner.isAlive()) {
            return;
        }

        for (EquipmentSlot slot : armorSlots()) {
            ItemStack armor = owner.getItemBySlot(slot);
            if (!armor.isEmpty() && armor.isDamaged()) {
                armor.setDamageValue(Math.max(0, armor.getDamageValue() - 1));
            }
        }
    }

    private static EquipmentSlot[] armorSlots() {
        return new EquipmentSlot[]{
                EquipmentSlot.HEAD,
                EquipmentSlot.CHEST,
                EquipmentSlot.LEGS,
                EquipmentSlot.FEET
        };
    }

    private boolean hasFloorAt(Vec3 position) {
        BlockPos feet = BlockPos.containing(position.x, this.getBoundingBox().minY + 0.05D, position.z);
        BlockPos floor = feet.below();
        return this.level().getBlockState(floor).isFaceSturdy(this.level(), floor, Direction.UP);
    }

    private Vec3 randomHorizontalDirection(double speed) {
        double angle = this.random.nextDouble() * Math.PI * 2.0D;
        return new Vec3(Math.cos(angle) * speed, 0.0D, Math.sin(angle) * speed);
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack heldItem = player.getItemInHand(hand);
        if (isMulticolor()) {
            if (player.isShiftKeyDown() && (heldItem.is(ModItems.ABYSSAL_TOXIN.get()) || heldItem.is(ModItems.ABYSSAL_INK.get()))) {
                if (!this.level().isClientSide) {
                    setEquippedEffect(heldItem.is(ModItems.ABYSSAL_TOXIN.get()) ? EFFECT_TOXIN : EFFECT_INK);
                    player.sendSystemMessage(heldItem.is(ModItems.ABYSSAL_TOXIN.get())
                            ? net.minecraft.network.chat.Component.literal("Le Zwoing multicolore porte une toxine abyssale.")
                            : net.minecraft.network.chat.Component.literal("Le Zwoing multicolore porte une encre abyssale."));
                }
                player.swing(hand);
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }

            if (player.isShiftKeyDown()) {
                if (!this.level().isClientSide) {
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                            "Tiens une toxine abyssale ou une encre abyssale en main, puis Shift + clic droit sur le Zwoing."
                    ));
                }
                player.swing(hand);
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }

            if (heldItem.is(ModItems.ABYSSAL_COMPOST.get()) && this.getHealth() < this.getMaxHealth()) {
                if (!this.level().isClientSide) {
                    this.heal(COMPOST_HEAL_AMOUNT);
                    if (!player.getAbilities().instabuild) {
                        heldItem.shrink(1);
                    }
                }
                player.swing(hand);
                return InteractionResult.sidedSuccess(this.level().isClientSide);
            }
        }

        if (!heldItem.isEmpty()) {
            return super.mobInteract(player, hand);
        }

        if (!this.level().isClientSide) {
            ItemStack captured = ZwoingItem.capture(this, player);
            if (!player.getInventory().add(captured)) {
                player.drop(captured, false);
            }
            this.discard();
        }

        player.swing(hand);
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    public void attachTo(LivingEntity target) {
        this.attachedTargetUuid = target.getUUID();
        this.noPhysics = true;
        this.setPersistenceRequired();
    }

    private void detach() {
        this.attachedTargetUuid = null;
        this.noPhysics = false;
    }

    public void setOwnerUuid(@Nullable UUID ownerUuid) {
        this.ownerUuid = ownerUuid;
        if (ownerUuid != null) {
            this.setPersistenceRequired();
        }
    }

    @Nullable
    public UUID getOwnerUuid() {
        return this.ownerUuid;
    }

    public int getVariant() {
        return this.entityData.get(VARIANT);
    }

    public void setVariant(int variant) {
        this.entityData.set(VARIANT, Math.max(BLUE, Math.min(MULTICOLOR, variant)));
        updateVariantHealth();
    }

    public boolean isMulticolor() {
        return getVariant() == MULTICOLOR;
    }

    public int getEquippedEffect() {
        return this.entityData.get(EQUIPPED_EFFECT);
    }

    public void setEquippedEffect(int effect) {
        this.entityData.set(EQUIPPED_EFFECT, Math.max(EFFECT_NONE, Math.min(EFFECT_INK, effect)));
    }

    public int getMotionState() {
        return this.entityData.get(MOTION_STATE);
    }

    private void setMotionState(int state) {
        this.entityData.set(MOTION_STATE, Mth.clamp(state, MOTION_BOUNCING, MOTION_DESCENDING));
    }

    public float getSquish(float partialTick) {
        return Mth.lerp(partialTick, this.oldSquish, this.squish);
    }

    public float getFloatTurn(float partialTick) {
        return Mth.lerp(partialTick, this.oldFloatTurn, this.floatTurn);
    }

    public void chooseRandomVariant() {
        int roll = this.random.nextInt(100);
        if (roll < 3) {
            setVariant(PURPLE);
        } else if (roll < 10) {
            setVariant(GREEN);
        } else if (roll < 30) {
            setVariant(RED);
        } else {
            setVariant(BLUE);
        }
    }

    public boolean isPurpleBlinkingBright() {
        return getVariant() == PURPLE && (this.tickCount / 6) % 2 == 0;
    }

    private void updateVariantHealth() {
        double targetMaxHealth = isMulticolor() ? MULTICOLOR_MAX_HEALTH : NORMAL_MAX_HEALTH;
        if (this.getAttribute(Attributes.MAX_HEALTH) == null
                || this.getAttribute(Attributes.MAX_HEALTH).getBaseValue() == targetMaxHealth) {
            return;
        }

        double previousMaxHealth = this.getAttribute(Attributes.MAX_HEALTH).getBaseValue();
        boolean wasFull = this.getHealth() >= previousMaxHealth - 0.1F;
        this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(targetMaxHealth);
        if (wasFull) {
            this.setHealth((float) targetMaxHealth);
        } else if (this.getHealth() > targetMaxHealth) {
            this.setHealth((float) targetMaxHealth);
        }
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
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return this.ownerUuid == null && !this.hasCustomName();
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return super.hurt(source, amount);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putInt("Variant", getVariant());
        tag.putInt("EquippedEffect", getEquippedEffect());

        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        if (this.attachedTargetUuid != null) {
            tag.putUUID("AttachedTarget", this.attachedTargetUuid);
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        setVariant(tag.getInt("Variant"));
        setEquippedEffect(tag.getInt("EquippedEffect"));
        if (tag.contains("Health", 99)) {
            this.setHealth(Math.min(this.getMaxHealth(), Math.max(1.0F, tag.getFloat("Health"))));
        }
        this.ownerUuid = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        this.attachedTargetUuid = tag.hasUUID("AttachedTarget") ? tag.getUUID("AttachedTarget") : null;

        if (this.ownerUuid != null || this.attachedTargetUuid != null) {
            this.setPersistenceRequired();
        }
    }
}
