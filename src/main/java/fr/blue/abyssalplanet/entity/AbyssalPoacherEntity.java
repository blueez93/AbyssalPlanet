package fr.blue.abyssalplanet.entity;

import fr.blue.abyssalplanet.AbyssalPlanet;
import fr.blue.abyssalplanet.registry.ModBannerPatterns;
import fr.blue.abyssalplanet.registry.ModSounds;
import fr.blue.abyssalplanet.world.AbyssalPoacherGroupData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class AbyssalPoacherEntity extends Zombie {
    private static final EntityDataAccessor<Boolean> INTACT_CLOTHES =
            SynchedEntityData.defineId(AbyssalPoacherEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Boolean> BANNER_BEARER =
            SynchedEntityData.defineId(AbyssalPoacherEntity.class, EntityDataSerializers.BOOLEAN);
    private static final EntityDataAccessor<Integer> APPEARANCE_VARIANT =
            SynchedEntityData.defineId(AbyssalPoacherEntity.class, EntityDataSerializers.INT);

    private static final int MIN_TRANSFORMATION_TICKS = 20 * 60 * 10;
    private static final int TRANSFORMATION_VARIANCE_TICKS = 20 * 60 * 10;
    private static final long STANDALONE_ALERT_COOLDOWN_TICKS = 20L * 30L;
    private static final float ABYSSAL_CULT_BANNER_DROP_CHANCE = 0.01F;

    @Nullable
    private UUID groupId;
    @Nullable
    private UUID groupLeaderId;
    private int transformationTicks = -1;
    private boolean wasTargetingPlayer;
    private long lastStandaloneAlertGameTime = Long.MIN_VALUE;

    public AbyssalPoacherEntity(EntityType<? extends Zombie> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 14;
        this.setMaxUpStep(1.0F);
        this.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
        this.setPersistenceRequired();
    }

    @Override
    public void setBaby(boolean ignored) {
        super.setBaby(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 32.0D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.29D)
                .add(ForgeMod.SWIM_SPEED.get(), 1.45D)
                .add(Attributes.FOLLOW_RANGE, 34.0D)
                .add(Attributes.ARMOR, 4.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.18D)
                .add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0.0D);
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(INTACT_CLOTHES, false);
        this.entityData.define(BANNER_BEARER, false);
        this.entityData.define(APPEARANCE_VARIANT, 0);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.15D, false));
        this.goalSelector.addGoal(5, new RandomStrollGoal(this, 0.85D, 45));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 10.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this).setAlertOthers(AbyssalPoacherEntity.class));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(
                this,
                Player.class,
                8,
                true,
                false,
                target -> target instanceof Player player && !player.isCreative() && !player.isSpectator()
        ));
        this.targetSelector.addGoal(3, new NearestAttackableTargetGoal<>(
                this,
                Mob.class,
                10,
                true,
                false,
                this::isAbyssalPrey
        ));
    }

    private boolean isAbyssalPrey(LivingEntity target) {
        if (target == this || target instanceof AbyssalPoacherEntity || target instanceof AbyssalWandererEntity) {
            return false;
        }

        ResourceLocation typeId = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        return typeId != null && AbyssalPlanet.MOD_ID.equals(typeId.getNamespace());
    }

    public void configureGroup(UUID groupId, UUID leaderId, boolean intactClothes, boolean bannerBearer) {
        this.groupId = groupId;
        this.groupLeaderId = leaderId;
        this.entityData.set(INTACT_CLOTHES, intactClothes);
        this.entityData.set(BANNER_BEARER, bannerBearer && !intactClothes);

        if (isBannerBearer()) {
            equipAbyssalCultBanner();
        }
    }

    private void equipAbyssalCultBanner() {
        this.setItemSlot(EquipmentSlot.HEAD, createAbyssalCultBanner());
        this.setDropChance(EquipmentSlot.HEAD, 0.0F);
    }

    private static ItemStack createAbyssalCultBanner() {
        ItemStack banner = new ItemStack(Items.BLACK_BANNER);
        CompoundTag blockEntityData = new CompoundTag();
        ListTag patterns = new ListTag();
        CompoundTag emblem = new CompoundTag();
        emblem.putString("Pattern", ModBannerPatterns.ABYSSAL_CULT.get().getHashname());
        emblem.putInt("Color", DyeColor.RED.getId());
        patterns.add(emblem);
        blockEntityData.put("Patterns", patterns);
        BlockItem.setBlockEntityData(banner, BlockEntityType.BANNER, blockEntityData);
        banner.hideTooltipPart(ItemStack.TooltipPart.ADDITIONAL);
        banner.setHoverName(Component.translatable("item.abyssalplanet.abyssal_cult_banner")
                .withStyle(ChatFormatting.DARK_RED));
        return banner;
    }

    public boolean hasIntactClothes() {
        return this.entityData.get(INTACT_CLOTHES);
    }

    public boolean isBannerBearer() {
        return this.entityData.get(BANNER_BEARER);
    }

    public int getAppearanceVariant() {
        return this.entityData.get(APPEARANCE_VARIANT);
    }

    @Override
    public void aiStep() {
        if (!this.level().isClientSide && hasIntactClothes() && this.groupId != null) {
            tickTransformation();
        }
        super.aiStep();
        keepOnSeabed();
        tickPlayerAlert();
    }

    private void keepOnSeabed() {
        if (!this.isInWaterOrBubble() || this.onGround()) {
            return;
        }

        Vec3 movement = this.getDeltaMovement();
        double vertical = this.horizontalCollision
                ? Math.min(movement.y, 0.12D)
                : Math.min(movement.y - 0.035D, -0.055D);
        this.setDeltaMovement(movement.x, vertical, movement.z);
    }

    private void tickPlayerAlert() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        boolean targetingPlayer = this.getTarget() instanceof ServerPlayer player
                && player.isAlive()
                && !player.isCreative()
                && !player.isSpectator();
        if (targetingPlayer && !this.wasTargetingPlayer && canPlayPlayerAlert(serverLevel)) {
            serverLevel.playSound(
                    null,
                    this.getX(),
                    this.getY(),
                    this.getZ(),
                    ModSounds.ABYSSAL_POACHER_ALERT.get(),
                    SoundSource.HOSTILE,
                    2.5F,
                    0.96F + this.random.nextFloat() * 0.08F
            );
        }
        this.wasTargetingPlayer = targetingPlayer;
    }

    @Override
    protected float getWaterSlowDown() {
        return 0.90F;
    }

    @Nullable
    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModSounds.ABYSSAL_HUMANOID_HURT.get();
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModSounds.ABYSSAL_HUMANOID_HURT.get();
    }

    @Override
    protected void playStepSound(BlockPos pos, BlockState state) {
        SoundType soundType = state.getSoundType(this.level(), pos, this);
        this.playSound(soundType.getStepSound(), soundType.getVolume() * 0.15F, soundType.getPitch());
    }

    private boolean canPlayPlayerAlert(ServerLevel level) {
        long gameTime = level.getGameTime();
        if (this.groupId != null) {
            return AbyssalPoacherGroupData.get(level).tryTriggerPlayerAlert(this.groupId, gameTime);
        }
        if (this.lastStandaloneAlertGameTime != Long.MIN_VALUE
                && gameTime - this.lastStandaloneAlertGameTime < STANDALONE_ALERT_COOLDOWN_TICKS) {
            return false;
        }

        this.lastStandaloneAlertGameTime = gameTime;
        return true;
    }

    private void tickTransformation() {
        if (!(this.level() instanceof ServerLevel serverLevel) || this.groupId == null) {
            return;
        }

        AbyssalPoacherGroupData groupData = AbyssalPoacherGroupData.get(serverLevel);
        if (groupData.getFallenCompanions(this.groupId) < 4) {
            return;
        }

        if (this.transformationTicks < 0) {
            this.transformationTicks = MIN_TRANSFORMATION_TICKS + this.random.nextInt(TRANSFORMATION_VARIANCE_TICKS + 1);
        }

        if (--this.transformationTicks <= 0) {
            transformIntoWanderer(serverLevel, groupData);
        }
    }

    private void transformIntoWanderer(ServerLevel level, AbyssalPoacherGroupData groupData) {
        AbyssalWandererEntity wanderer =
                fr.blue.abyssalplanet.registry.ModEntities.ABYSSAL_WANDERER.get().create(level);
        if (wanderer == null || this.groupId == null) {
            return;
        }

        BlockPos anchor = groupData.getWandererAnchor(this.groupId, this.blockPosition());
        wanderer.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), this.getXRot());
        wanderer.finalizeSpawn(
                level,
                level.getCurrentDifficultyAt(this.blockPosition()),
                MobSpawnType.CONVERSION,
                null,
                null
        );
        wanderer.setWanderAnchor(anchor);
        wanderer.setPersistenceRequired();
        level.addFreshEntity(wanderer);

        groupData.finishGroup(this.groupId);
        this.discard();
    }

    @Override
    protected void dropCustomDeathLoot(DamageSource source, int lootingLevel, boolean recentlyHit) {
        super.dropCustomDeathLoot(source, lootingLevel, recentlyHit);
        if (!isBannerBearer()) {
            return;
        }

        ItemStack banner = this.getItemBySlot(EquipmentSlot.HEAD);
        if (!banner.isEmpty() && this.random.nextFloat() < ABYSSAL_CULT_BANNER_DROP_CHANCE) {
            this.spawnAtLocation(banner.copy());
        }
        this.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        boolean hurt = super.hurt(source, amount);
        if (hurt && !this.level().isClientSide && this.groupId != null && this.level() instanceof ServerLevel serverLevel) {
            AbyssalPoacherGroupData.get(serverLevel).recordCombat(this.groupId, this.blockPosition());
        }
        return hurt;
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hurt = super.doHurtTarget(target);
        if (hurt && !this.level().isClientSide && this.groupId != null && this.level() instanceof ServerLevel serverLevel) {
            AbyssalPoacherGroupData.get(serverLevel).recordCombat(this.groupId, target.blockPosition());
        }
        return hurt;
    }

    @Override
    public void die(DamageSource source) {
        if (!this.level().isClientSide && this.groupId != null && this.level() instanceof ServerLevel serverLevel) {
            AbyssalPoacherGroupData groupData = AbyssalPoacherGroupData.get(serverLevel);
            if (hasIntactClothes()) {
                groupData.recordLeaderDeath(this.groupId);
            } else {
                groupData.recordCompanionDeath(this.groupId, this.blockPosition());
            }
        }
        super.die(source);
    }

    @Override
    protected ResourceLocation getDefaultLootTable() {
        return BuiltInLootTables.EMPTY;
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    protected boolean isSunSensitive() {
        return false;
    }

    @Override
    protected boolean convertsInWater() {
        return false;
    }

    @Override
    public SpawnGroupData finalizeSpawn(
            ServerLevelAccessor level,
            net.minecraft.world.DifficultyInstance difficulty,
            MobSpawnType spawnType,
            @Nullable SpawnGroupData spawnData,
            @Nullable CompoundTag tag
    ) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, spawnData, tag);
        this.entityData.set(APPEARANCE_VARIANT, this.random.nextInt(3));
        equipHuntingWeapon(true);
        this.setPersistenceRequired();
        return result;
    }

    private void equipHuntingWeapon(boolean replaceExisting) {
        if (!replaceExisting && !this.getMainHandItem().isEmpty()) {
            return;
        }

        int roll = this.random.nextInt(100);
        net.minecraft.world.item.ItemStack weapon;
        if (hasIntactClothes()) {
            weapon = roll < 56
                    ? net.minecraft.world.item.Items.IRON_SWORD.getDefaultInstance()
                    : net.minecraft.world.item.Items.IRON_AXE.getDefaultInstance();
        } else if (roll < 32) {
            weapon = net.minecraft.world.item.Items.STONE_SWORD.getDefaultInstance();
        } else if (roll < 64) {
            weapon = net.minecraft.world.item.Items.IRON_SWORD.getDefaultInstance();
        } else if (roll < 84) {
            weapon = net.minecraft.world.item.Items.STONE_AXE.getDefaultInstance();
        } else {
            weapon = net.minecraft.world.item.Items.IRON_AXE.getDefaultInstance();
        }

        this.setItemSlot(EquipmentSlot.MAINHAND, weapon);
        this.setDropChance(EquipmentSlot.MAINHAND, 0.06F);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        if (this.groupId != null) {
            tag.putUUID("PoacherGroup", this.groupId);
        }
        if (this.groupLeaderId != null) {
            tag.putUUID("PoacherLeader", this.groupLeaderId);
        }
        tag.putBoolean("IntactClothes", hasIntactClothes());
        tag.putBoolean("BannerBearer", isBannerBearer());
        tag.putInt("AppearanceVariant", getAppearanceVariant());
        tag.putInt("TransformationTicks", this.transformationTicks);
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        this.groupId = tag.hasUUID("PoacherGroup") ? tag.getUUID("PoacherGroup") : null;
        this.groupLeaderId = tag.hasUUID("PoacherLeader") ? tag.getUUID("PoacherLeader") : null;
        this.entityData.set(INTACT_CLOTHES, tag.getBoolean("IntactClothes"));
        this.entityData.set(BANNER_BEARER, tag.getBoolean("BannerBearer"));
        this.entityData.set(APPEARANCE_VARIANT, Math.floorMod(tag.getInt("AppearanceVariant"), 3));
        this.transformationTicks = tag.contains("TransformationTicks") ? tag.getInt("TransformationTicks") : -1;
        if (isBannerBearer()) {
            equipAbyssalCultBanner();
        }
        equipHuntingWeapon(false);
        this.setPersistenceRequired();
    }
}
