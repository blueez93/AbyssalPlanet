package fr.blue.abyssalplanet.entity;

import fr.blue.abyssalplanet.AbyssalPlanet;
import fr.blue.abyssalplanet.registry.ModItems;
import fr.blue.abyssalplanet.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.BossEvent;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
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
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

public final class AbyssalWandererEntity extends Zombie {
    private static final int WANDER_RADIUS = 34;
    private static final double RETURN_DISTANCE_SQR = 44.0D * 44.0D;
    private static final String CURRENT_STATS_TAG = "WandererBossStatsV2";
    private static final long COMBAT_SOUND_COOLDOWN_TICKS = 20L * 30L;

    @Nullable
    private BlockPos wanderAnchor;
    private boolean nextAttackUsesOffhand;
    private boolean wasTargetingPlayer;
    private long lastCombatSoundGameTime = Long.MIN_VALUE;
    private final ServerBossEvent bossEvent = (ServerBossEvent) new ServerBossEvent(
            Component.translatable("bossbar.abyssalplanet.abyssal_wanderer"),
            BossEvent.BossBarColor.PURPLE,
            BossEvent.BossBarOverlay.NOTCHED_10
    ).setDarkenScreen(false).setCreateWorldFog(false);

    public AbyssalWandererEntity(EntityType<? extends Zombie> entityType, Level level) {
        super(entityType, level);
        this.xpReward = 160;
        this.setMaxUpStep(1.0F);
        this.setPathfindingMalus(BlockPathTypes.WATER, 0.0F);
        this.setPersistenceRequired();
        this.bossEvent.setVisible(true);
    }

    @Override
    public void setBaby(boolean ignored) {
        super.setBaby(false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 400.0D)
                .add(Attributes.ATTACK_DAMAGE, 5.0D)
                .add(Attributes.ATTACK_KNOCKBACK, 1.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.27D)
                .add(ForgeMod.SWIM_SPEED.get(), 1.35D)
                .add(Attributes.FOLLOW_RANGE, 40.0D)
                .add(Attributes.ARMOR, 10.0D)
                .add(Attributes.ARMOR_TOUGHNESS, 4.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.60D)
                .add(Attributes.SPAWN_REINFORCEMENTS_CHANCE, 0.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(2, new FastDualAxeAttackGoal(this, 1.22D, true));
        this.goalSelector.addGoal(5, new RandomStrollGoal(this, 0.68D, 70));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 12.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
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

    public void setWanderAnchor(BlockPos anchor) {
        this.wanderAnchor = anchor.immutable();
        this.restrictTo(this.wanderAnchor, WANDER_RADIUS);
    }

    @Override
    public void aiStep() {
        if (!this.level().isClientSide) {
            this.bossEvent.setProgress(this.getHealth() / this.getMaxHealth());
            this.bossEvent.setName(getBossBarName());
            this.bossEvent.setVisible(true);
            if (this.wanderAnchor != null) {
                this.restrictTo(this.wanderAnchor, WANDER_RADIUS);
                if (this.distanceToSqr(
                        this.wanderAnchor.getX() + 0.5D,
                        this.wanderAnchor.getY() + 0.5D,
                        this.wanderAnchor.getZ() + 0.5D
                ) > RETURN_DISTANCE_SQR) {
                    this.getNavigation().moveTo(
                            this.wanderAnchor.getX() + 0.5D,
                            this.wanderAnchor.getY() + 0.5D,
                            this.wanderAnchor.getZ() + 0.5D,
                            1.0D
                    );
                }
            }
        }
        super.aiStep();
        keepOnSeabed();
        tickCombatSound();
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

    private void tickCombatSound() {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        boolean targetingPlayer = this.getTarget() instanceof ServerPlayer player
                && player.isAlive()
                && !player.isCreative()
                && !player.isSpectator();
        if (targetingPlayer && !this.wasTargetingPlayer) {
            long gameTime = serverLevel.getGameTime();
            if (this.lastCombatSoundGameTime == Long.MIN_VALUE
                    || gameTime - this.lastCombatSoundGameTime >= COMBAT_SOUND_COOLDOWN_TICKS) {
                serverLevel.playSound(
                        null,
                        this.getX(),
                        this.getY(),
                        this.getZ(),
                        ModSounds.ABYSSAL_WANDERER_COMBAT.get(),
                        SoundSource.HOSTILE,
                        3.0F,
                        1.0F
                );
                this.lastCombatSoundGameTime = gameTime;
            }
        }
        this.wasTargetingPlayer = targetingPlayer;
    }

    private boolean isAbyssalPrey(LivingEntity target) {
        if (target == this || target instanceof AbyssalPoacherEntity || target instanceof AbyssalWandererEntity) {
            return false;
        }

        ResourceLocation typeId = ForgeRegistries.ENTITY_TYPES.getKey(target.getType());
        return typeId != null && AbyssalPlanet.MOD_ID.equals(typeId.getNamespace());
    }

    private void equipMask() {
        if (this.getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
            this.setItemSlot(EquipmentSlot.HEAD, ModItems.ABYSSAL_WANDERER_MASK.get().getDefaultInstance());
            this.setDropChance(EquipmentSlot.HEAD, 2.0F);
        }
    }

    private void equipDualAxes() {
        this.setItemSlot(EquipmentSlot.MAINHAND, Items.NETHERITE_AXE.getDefaultInstance());
        this.setItemSlot(EquipmentSlot.OFFHAND, Items.NETHERITE_AXE.getDefaultInstance());
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
        this.setDropChance(EquipmentSlot.OFFHAND, 0.0F);
    }

    private InteractionHand getNextAttackHand() {
        InteractionHand hand = this.nextAttackUsesOffhand ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        this.nextAttackUsesOffhand = !this.nextAttackUsesOffhand;
        return hand;
    }

    @Override
    public boolean doHurtTarget(net.minecraft.world.entity.Entity target) {
        boolean hurt = super.doHurtTarget(target);
        if (hurt) {
            this.playSound(
                    SoundEvents.PLAYER_ATTACK_STRONG,
                    1.2F,
                    0.76F + this.random.nextFloat() * 0.12F
            );
        }
        return hurt;
    }

    @Override
    public SpawnGroupData finalizeSpawn(
            ServerLevelAccessor level,
            DifficultyInstance difficulty,
            MobSpawnType spawnType,
            @Nullable SpawnGroupData spawnData,
            @Nullable CompoundTag tag
    ) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, spawnType, spawnData, tag);
        equipMask();
        equipDualAxes();
        if (this.wanderAnchor == null) {
            setWanderAnchor(this.blockPosition());
        }
        this.setHealth(this.getMaxHealth());
        this.setPersistenceRequired();
        return result;
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
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putBoolean(CURRENT_STATS_TAG, true);
        if (this.wanderAnchor != null) {
            tag.putLong("WanderAnchor", this.wanderAnchor.asLong());
        }
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        boolean legacyStats = !tag.getBoolean(CURRENT_STATS_TAG);
        if (tag.contains("WanderAnchor")) {
            setWanderAnchor(BlockPos.of(tag.getLong("WanderAnchor")));
        }
        equipMask();
        equipDualAxes();
        if (legacyStats) {
            this.setHealth(this.getMaxHealth());
        }
        this.setPersistenceRequired();
    }

    @Override
    public void setCustomName(@Nullable Component name) {
        super.setCustomName(name);
        this.bossEvent.setName(getBossBarName());
    }

    @Override
    public void startSeenByPlayer(ServerPlayer player) {
        super.startSeenByPlayer(player);
        this.bossEvent.setName(getBossBarName());
        this.bossEvent.setVisible(true);
        this.bossEvent.addPlayer(player);
    }

    @Override
    public void stopSeenByPlayer(ServerPlayer player) {
        super.stopSeenByPlayer(player);
        this.bossEvent.removePlayer(player);
    }

    private Component getBossBarName() {
        Component customName = this.getCustomName();
        return customName != null
                ? customName
                : Component.translatable("bossbar.abyssalplanet.abyssal_wanderer");
    }

    private static final class FastDualAxeAttackGoal extends MeleeAttackGoal {
        private static final int ATTACK_INTERVAL_TICKS = 16;
        private final AbyssalWandererEntity wanderer;
        private int attackCooldown;

        private FastDualAxeAttackGoal(
                AbyssalWandererEntity wanderer,
                double speedModifier,
                boolean followingTargetEvenIfNotSeen
        ) {
            super(wanderer, speedModifier, followingTargetEvenIfNotSeen);
            this.wanderer = wanderer;
        }

        @Override
        public void tick() {
            this.attackCooldown = Math.max(0, this.attackCooldown - 1);
            super.tick();
        }

        @Override
        protected void checkAndPerformAttack(LivingEntity target, double distanceToTargetSqr) {
            if (distanceToTargetSqr <= getAttackReachSqr(target) && this.attackCooldown <= 0) {
                this.attackCooldown = this.adjustedTickDelay(ATTACK_INTERVAL_TICKS);
                this.wanderer.swing(this.wanderer.getNextAttackHand());
                this.wanderer.doHurtTarget(target);
            }
        }
    }
}
