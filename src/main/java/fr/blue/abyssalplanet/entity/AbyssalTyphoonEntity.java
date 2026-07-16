package fr.blue.abyssalplanet.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;


import java.util.ArrayList;
import java.util.List;

import fr.blue.abyssalplanet.registry.ModDimensions;
import fr.blue.abyssalplanet.world.AbyssalTravelData;
import fr.blue.abyssalplanet.world.AbyssalTeleporter;

public class AbyssalTyphoonEntity extends Entity {
    private int lifeTicks = 0;

    private static final int MAX_LIFE_TICKS = 20 * 60 * 7; // 7 minutes

    // Croissance lente du typhon.
    private static final double START_RADIUS = 2.5D;
    private static final double ABSOLUTE_MAX_RADIUS = 12.0D;
    private static final int GROWTH_INTERVAL_TICKS = 20; // grandit toutes les 1 seconde
    private static final double GROWTH_PER_STEP = 0.25D;

    private double currentRadius = START_RADIUS;
    private double targetRadius = 8.0D;

    private final List<BlockPos> clearedWaterBlocks = new ArrayList<>();
    private final Set<BlockPos> clearedWaterBlockSet = new HashSet<>();
    private final Map<UUID, Integer> teleportTimers = new HashMap<>();
    private static final int TELEPORT_DELAY_TICKS = 20 * 4; // 4 secondes 

    public AbyssalTyphoonEntity(EntityType<?> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    @Override
    public void tick() {
        super.tick();

        lifeTicks++;

        if (lifeTicks == 1 && !this.level().isClientSide) {
            chooseRandomTargetRadius();
        }

        if (this.level().isClientSide) {
            spawnClientParticles();
            return;
        }

        keepAtWaterSurface();

        growSlowly();

        carveWaterVortex();

        pullNearbyPlayers();

        if (lifeTicks >= MAX_LIFE_TICKS) {
            restoreWater();
            this.discard();
        }
    }

    private void chooseRandomTargetRadius() {
        // Taille finale aléatoire entre 6 et 12 blocs.
        this.targetRadius = 6.0D + this.random.nextDouble() * (ABSOLUTE_MAX_RADIUS - 6.0D);
    }

    private void keepAtWaterSurface() {
    BlockPos pos = this.blockPosition();

    // Le typhon doit rester dans un bloc d'air, juste au-dessus de la surface.
    if (!this.level().isEmptyBlock(pos)) {
        restoreWater();
        this.discard();
        return;
    }

    // On ne vérifie pas seulement le bloc juste dessous,
    // car le typhon creuse justement l'eau sous son centre.
    int validWaterOrClearedBlocks = 0;
    int checkedBlocks = 0;

    int checkRadius = 4;

    for (int x = -checkRadius; x <= checkRadius; x++) {
        for (int z = -checkRadius; z <= checkRadius; z++) {
            double distance = Math.sqrt(x * x + z * z);

            if (distance > checkRadius) {
                continue;
            }

            BlockPos checkPos = pos.offset(x, -1, z);
            checkedBlocks++;

            boolean isWater = this.level().getFluidState(checkPos).is(FluidTags.WATER);
            boolean wasClearedByTyphoon = clearedWaterBlockSet.contains(checkPos);

            if (isWater || wasClearedByTyphoon) {
                validWaterOrClearedBlocks++;
            }
        }
    }

    // S'il n'y a presque plus d'eau autour, là seulement il disparaît.
    if (checkedBlocks > 0 && validWaterOrClearedBlocks < checkedBlocks * 0.35D) {
        restoreWater();
        this.discard();
    }
}

    private void growSlowly() {
        if (lifeTicks % GROWTH_INTERVAL_TICKS != 0) {
            return;
        }

        if (currentRadius < targetRadius) {
            currentRadius = Math.min(targetRadius, currentRadius + GROWTH_PER_STEP);
        }
    }

    private void pullNearbyPlayers() {
    double pullArea = currentRadius + 8.0D;

    List<Player> players = new ArrayList<>(this.level().getEntitiesOfClass(
            Player.class,
            this.getBoundingBox().inflate(pullArea, 14.0D, pullArea)
    ));

    if (this.level() instanceof net.minecraft.server.level.ServerLevel serverLevel
            && !teleportTimers.isEmpty()) {
        for (UUID playerId : new ArrayList<>(teleportTimers.keySet())) {
            ServerPlayer armedPlayer = serverLevel.getServer().getPlayerList().getPlayer(playerId);
            if (armedPlayer == null || armedPlayer.level() != this.level()) {
                teleportTimers.remove(playerId);
            } else if (!players.contains(armedPlayer)) {
                players.add(armedPlayer);
            }
        }
    }

    for (Player player : players) {
        if (AbyssalTravelData.hasTyphoonImmunity(player)) {
            teleportTimers.remove(player.getUUID());
            continue;
        }

        if (teleportTimers.containsKey(player.getUUID())) {
            tickTeleportCountdown(player);
            continue;
        }

        Vec3 center = this.position().add(0.0D, -4.0D, 0.0D);
        Vec3 playerPos = player.position();

        Vec3 toCenter = center.subtract(playerPos);

        if (toCenter.lengthSqr() < 0.01D) {
            continue;
        }

        double distance = toCenter.length();

        if (distance > pullArea) {
            continue;
        }

        Vec3 radialPull = toCenter.normalize();
        Vec3 swirl = new Vec3(-radialPull.z, 0.0D, radialPull.x);

        double closeness = 1.0D - Math.min(distance / pullArea, 1.0D);

        // Aspiration lente.
        double pullStrength = 0.006D + closeness * 0.055D;
        double swirlStrength = 0.010D + closeness * 0.030D;
        double downwardStrength = -0.004D - closeness * 0.030D;

        // On aspire seulement si le joueur est dans l'eau OU proche du trou du typhon.
        boolean closeEnoughToBePulled = distance <= currentRadius + 2.0D;

        if (player.isInWater() || closeEnoughToBePulled) {
            Vec3 movement = radialPull.scale(pullStrength)
                    .add(swirl.scale(swirlStrength))
                    .add(0.0D, downwardStrength, 0.0D);

            player.setDeltaMovement(player.getDeltaMovement().add(movement));
            player.hurtMarked = true;
        }

        // Zone considérée comme "le cœur" du typhon.
        boolean isInsideTyphoonCore = isInsideClearedWaterVolume(player);

        if (isInsideTyphoonCore) {
    UUID playerId = player.getUUID();

    teleportTimers.putIfAbsent(playerId, TELEPORT_DELAY_TICKS);

    player.addEffect(new MobEffectInstance(
            MobEffects.BLINDNESS,
            TELEPORT_DELAY_TICKS + 20,
            0,
            false,
            true,
            true
    ));

    int remainingTicks = teleportTimers.get(playerId) - 1;
    teleportTimers.put(playerId, remainingTicks);

    // Secousse légère pendant l'attente.
    Vec3 shake = new Vec3(
            (this.random.nextDouble() - 0.5D) * 0.08D,
            (this.random.nextDouble() - 0.5D) * 0.04D,
            (this.random.nextDouble() - 0.5D) * 0.08D
    );

    player.setDeltaMovement(player.getDeltaMovement().add(shake));
    player.hurtMarked = true;

    if (remainingTicks <= 0) {
        teleportTimers.remove(playerId);

        if (player instanceof ServerPlayer serverPlayer
                && !serverPlayer.level().dimension().equals(ModDimensions.ABYSSAL_PLANET_LEVEL)) {
            AbyssalTeleporter.teleportToAbyss(serverPlayer, this.position());
        }
    }
}
    }
}

    private void tickTeleportCountdown(Player player) {
        UUID playerId = player.getUUID();
        Integer timer = teleportTimers.get(playerId);
        if (timer == null) {
            return;
        }

        player.addEffect(new MobEffectInstance(
                MobEffects.BLINDNESS,
                Math.max(20, timer + 20),
                0,
                false,
                true,
                true
        ));

        int remainingTicks = timer - 1;
        teleportTimers.put(playerId, remainingTicks);

        Vec3 shake = new Vec3(
                (this.random.nextDouble() - 0.5D) * 0.08D,
                (this.random.nextDouble() - 0.5D) * 0.04D,
                (this.random.nextDouble() - 0.5D) * 0.08D
        );
        player.setDeltaMovement(player.getDeltaMovement().add(shake));
        player.hurtMarked = true;

        if (remainingTicks > 0) {
            return;
        }

        teleportTimers.remove(playerId);
        if (player instanceof ServerPlayer serverPlayer
                && !serverPlayer.level().dimension().equals(ModDimensions.ABYSSAL_PLANET_LEVEL)) {
            AbyssalTeleporter.teleportToAbyss(serverPlayer, this.position());
        }
    }

    private boolean isInsideClearedWaterVolume(Player player) {
        if (clearedWaterBlockSet.isEmpty()) {
            return false;
        }

        AABB playerBox = player.getBoundingBox().inflate(0.35D, 0.2D, 0.35D);
        int minX = (int) Math.floor(playerBox.minX);
        int minY = (int) Math.floor(playerBox.minY);
        int minZ = (int) Math.floor(playerBox.minZ);
        int maxX = (int) Math.floor(playerBox.maxX);
        int maxY = (int) Math.floor(playerBox.maxY);
        int maxZ = (int) Math.floor(playerBox.maxZ);

        for (BlockPos checkPos : BlockPos.betweenClosed(minX, minY, minZ, maxX, maxY, maxZ)) {
            if (!clearedWaterBlockSet.contains(checkPos) || !this.level().isEmptyBlock(checkPos)) {
                continue;
            }

            AABB clearedBlockBox = new AABB(
                    checkPos.getX(),
                    checkPos.getY(),
                    checkPos.getZ(),
                    checkPos.getX() + 1.0D,
                    checkPos.getY() + 1.0D,
                    checkPos.getZ() + 1.0D
            );

            if (playerBox.intersects(clearedBlockBox)) {
                return true;
            }
        }

        return false;
    }

    private void carveWaterVortex() {
        // Le creusement se fait lentement pour donner l'effet que le typhon aspire l'eau petit à petit.
        if (lifeTicks % 15 != 0) {
            return;
        }

        BlockPos surface = this.blockPosition();

        int radius = (int) Math.floor(currentRadius);
        int depth = Math.min(14, 5 + (int) currentRadius);

        for (int y = 1; y <= depth; y++) {
            // Forme de cône inversé :
            // large à la surface, plus étroit en profondeur.
            double layerFactor = 1.0D - ((double) y / (double) depth);
            double layerRadius = Math.max(1.2D, currentRadius * layerFactor);

            for (int x = -radius; x <= radius; x++) {
                for (int z = -radius; z <= radius; z++) {
                    double distance = Math.sqrt(x * x + z * z);

                    if (distance > layerRadius) {
                        continue;
                    }

                    BlockPos pos = surface.offset(x, -y, z);

                    if (this.level().getFluidState(pos).is(FluidTags.WATER)) {
                        this.level().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);

                        BlockPos clearedPos = pos.immutable();
                        if (clearedWaterBlockSet.add(clearedPos)) {
                            clearedWaterBlocks.add(clearedPos);
                        }
                    }
                }
            }
        }
    }

    private void restoreWater() {
        for (BlockPos pos : clearedWaterBlocks) {
            if (this.level().isEmptyBlock(pos)) {
                this.level().setBlock(pos, Blocks.WATER.defaultBlockState(), 3);
            }
        }

        clearedWaterBlocks.clear();
        clearedWaterBlockSet.clear();
    }
    

    private void spawnClientParticles() {
        double visualRadius = currentRadius;

        // Spirale lente, large, majestueuse.
        double rotationSpeed = 0.055D;

        int layers = 26;

        for (int layer = 0; layer < layers; layer++) {
            double height = -layer * 0.35D;

            double layerProgress = (double) layer / (double) layers;

            // Large en haut, resserré en bas.
            double radius = Math.max(1.0D, visualRadius * (1.0D - layerProgress * 0.85D));

            double angle = this.tickCount * rotationSpeed + layer * 0.55D;

            double x = this.getX() + Math.cos(angle) * radius;
            double y = this.getY() + height;
            double z = this.getZ() + Math.sin(angle) * radius;

            this.level().addParticle(
                    ParticleTypes.BUBBLE,
                    x,
                    y,
                    z,
                    -Math.sin(angle) * 0.025D,
                    -0.015D,
                    Math.cos(angle) * 0.025D
            );

            if (layer % 5 == 0) {
                this.level().addParticle(
                        ParticleTypes.SQUID_INK,
                        x,
                        y,
                        z,
                        0.0D,
                        -0.01D,
                        0.0D
                );
            }
        }

        // Grand anneau lent à la surface.
        int ringParticles = 32;

        for (int i = 0; i < ringParticles; i++) {
            double angle = this.tickCount * rotationSpeed + i * (Math.PI * 2.0D / ringParticles);
            double radius = visualRadius;

            double x = this.getX() + Math.cos(angle) * radius;
            double y = this.getY() - 0.05D;
            double z = this.getZ() + Math.sin(angle) * radius;

            this.level().addParticle(
                    ParticleTypes.SPLASH,
                    x,
                    y,
                    z,
                    -Math.sin(angle) * 0.02D,
                    0.01D,
                    Math.cos(angle) * 0.02D
            );
        }

        // Quelques bulles aspirées vers le centre.
        for (int i = 0; i < 10; i++) {
            double angle = this.random.nextDouble() * Math.PI * 2.0D;
            double radius = this.random.nextDouble() * visualRadius;

            double x = this.getX() + Math.cos(angle) * radius;
            double y = this.getY() - this.random.nextDouble() * 7.0D;
            double z = this.getZ() + Math.sin(angle) * radius;

            this.level().addParticle(
                    ParticleTypes.BUBBLE,
                    x,
                    y,
                    z,
                    -Math.cos(angle) * 0.03D,
                    0.02D,
                    -Math.sin(angle) * 0.03D
            );
        }
    }

    @Override
    public void remove(RemovalReason reason) {
        if (!this.level().isClientSide) {
            restoreWater();
        }

        super.remove(reason);
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.lifeTicks = tag.getInt("LifeTicks");
        this.currentRadius = tag.getDouble("CurrentRadius");
        this.targetRadius = tag.getDouble("TargetRadius");

        if (this.currentRadius <= 0.0D) {
            this.currentRadius = START_RADIUS;
        }

        if (this.targetRadius <= 0.0D) {
            this.targetRadius = 8.0D;
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("LifeTicks", this.lifeTicks);
        tag.putDouble("CurrentRadius", this.currentRadius);
        tag.putDouble("TargetRadius", this.targetRadius);
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }
}
