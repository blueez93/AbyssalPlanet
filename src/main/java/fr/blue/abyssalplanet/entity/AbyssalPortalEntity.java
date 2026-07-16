package fr.blue.abyssalplanet.entity;

import fr.blue.abyssalplanet.event.AbyssalPortalEvents;
import fr.blue.abyssalplanet.item.AbyssalStaffItem;
import fr.blue.abyssalplanet.registry.ModEntities;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

public class AbyssalPortalEntity extends Entity {
    private static final EntityDataAccessor<Float> RENDER_RADIUS =
            SynchedEntityData.defineId(AbyssalPortalEntity.class, EntityDataSerializers.FLOAT);
    private static final EntityDataAccessor<Integer> PORTAL_STATE =
            SynchedEntityData.defineId(AbyssalPortalEntity.class, EntityDataSerializers.INT);

    private static final int STATE_WAITING_HELPER = 0;
    private static final int STATE_CHANNELING = 1;
    private static final int STATE_OPEN = 2;
    private static final int WAIT_FOR_HELPER_TICKS = 30 * 20;
    private static final int CHANNEL_TICKS = 3 * 20;
    private static final int OPEN_LIFETIME_TICKS = 60 * 20;
    private static final int PORTAL_INK_REFUND = 5;
    private static final double CHANNEL_DISTANCE = 7.0D;
    private static final double MOVE_TOLERANCE_SQR = 0.08D * 0.08D;
    private static final float CLOSED_RADIUS = 0.9F;
    private static final float OPEN_RADIUS = 4.0F;

    private UUID ownerUuid;
    private UUID helperUuid;
    private BlockPos anchorPos = BlockPos.ZERO;
    private Vec3 ownerChannelStart;
    private Vec3 helperChannelStart;
    private final Set<UUID> invitedPlayers = new HashSet<>();
    private int age;
    private int channelTicks;
    private int openTicks;
    private boolean refundedInk;

    public AbyssalPortalEntity(EntityType<? extends AbyssalPortalEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public static void spawn(ServerLevel level, BlockPos anchorPos, Vec3 portalPosition, UUID ownerUuid) {
        AbyssalPortalEntity portal = ModEntities.ABYSSAL_PORTAL.get().create(level);
        if (portal == null) {
            return;
        }

        portal.anchorPos = anchorPos.immutable();
        portal.ownerUuid = ownerUuid;
        portal.moveTo(portalPosition.x, portalPosition.y, portalPosition.z);
        level.addFreshEntity(portal);
    }

    public static boolean hasPortalNear(ServerLevel level, BlockPos anchorPos) {
        AABB searchBox = new AABB(anchorPos).inflate(5.0D);
        List<AbyssalPortalEntity> portals = level.getEntitiesOfClass(
                AbyssalPortalEntity.class,
                searchBox,
                portal -> portal.isAlive() && portal.getState() != STATE_OPEN
        );
        return !portals.isEmpty();
    }

    @Override
    public void tick() {
        super.tick();
        this.age++;

        if (this.level().isClientSide) {
            spawnClientParticles();
            return;
        }

        switch (getState()) {
            case STATE_WAITING_HELPER -> tickWaitingForHelper();
            case STATE_CHANNELING -> tickChanneling();
            case STATE_OPEN -> tickOpen();
            default -> discard();
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (this.level().isClientSide || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }

        if (getState() == STATE_WAITING_HELPER) {
            if (serverPlayer.getUUID().equals(this.ownerUuid)) {
                serverPlayer.sendSystemMessage(ComponentHelper.literal("Une autre personne doit canaliser le trou noir avec toi.", ChatFormatting.GRAY));
                return InteractionResult.SUCCESS;
            }

            beginChannel(serverPlayer);
            return InteractionResult.SUCCESS;
        }

        if (getState() == STATE_OPEN && serverPlayer.getUUID().equals(this.ownerUuid)) {
            AbyssalPortalEvents.showInviteMenu(serverPlayer, this);
            return InteractionResult.SUCCESS;
        }

        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public float getPickRadius() {
        return Math.max(1.0F, getRenderRadius());
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return distance < 192.0D * 192.0D;
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return this.getBoundingBox().inflate(getRenderRadius() + 1.0F);
    }

    @Override
    protected void defineSynchedData() {
        this.entityData.define(RENDER_RADIUS, CLOSED_RADIUS);
        this.entityData.define(PORTAL_STATE, STATE_WAITING_HELPER);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        this.age = tag.getInt("Age");
        this.channelTicks = tag.getInt("ChannelTicks");
        this.openTicks = tag.getInt("OpenTicks");
        this.refundedInk = tag.getBoolean("RefundedInk");
        this.anchorPos = BlockPos.of(tag.getLong("AnchorPos"));
        this.entityData.set(RENDER_RADIUS, tag.contains("RenderRadius") ? tag.getFloat("RenderRadius") : CLOSED_RADIUS);
        this.entityData.set(PORTAL_STATE, tag.getInt("PortalState"));
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        if (tag.hasUUID("Helper")) {
            this.helperUuid = tag.getUUID("Helper");
        }
        this.invitedPlayers.clear();
        int invitedCount = tag.getInt("InvitedCount");
        for (int index = 0; index < invitedCount; index++) {
            String key = "Invited" + index;
            if (tag.hasUUID(key)) {
                this.invitedPlayers.add(tag.getUUID(key));
            }
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Age", this.age);
        tag.putInt("ChannelTicks", this.channelTicks);
        tag.putInt("OpenTicks", this.openTicks);
        tag.putBoolean("RefundedInk", this.refundedInk);
        tag.putLong("AnchorPos", this.anchorPos.asLong());
        tag.putFloat("RenderRadius", getRenderRadius());
        tag.putInt("PortalState", getState());
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        if (this.helperUuid != null) {
            tag.putUUID("Helper", this.helperUuid);
        }
        tag.putInt("InvitedCount", this.invitedPlayers.size());
        int index = 0;
        for (UUID invitedPlayer : this.invitedPlayers) {
            tag.putUUID("Invited" + index, invitedPlayer);
            index++;
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    public float getRenderRadius() {
        return this.entityData.get(RENDER_RADIUS);
    }

    public boolean isOpen() {
        return getState() == STATE_OPEN;
    }

    public boolean isOwner(ServerPlayer player) {
        return player.getUUID().equals(this.ownerUuid);
    }

    public void invite(ServerPlayer owner, ServerPlayer target) {
        if (!isOpen() || !isOwner(owner)) {
            owner.sendSystemMessage(ComponentHelper.literal("Ce portail abyssal n'est plus sous ton contrôle.", ChatFormatting.RED));
            return;
        }

        if (target.getUUID().equals(owner.getUUID())) {
            owner.sendSystemMessage(ComponentHelper.literal("Tu contrôles déjà le portail.", ChatFormatting.RED));
            return;
        }

        this.invitedPlayers.add(target.getUUID());
        target.sendSystemMessage(ComponentHelper.literal("Un portail des abysses a été ouvert, quelqu'un souhaite vous téléporter.", ChatFormatting.DARK_PURPLE));
        target.sendSystemMessage(AbyssalPortalEvents.buildInviteResponseMessage(this, owner));
        owner.sendSystemMessage(ComponentHelper.literal("Invitation abyssale envoyée à " + target.getGameProfile().getName() + ".", ChatFormatting.DARK_AQUA));
    }

    public void accept(ServerPlayer player) {
        if (!isOpen()) {
            player.sendSystemMessage(ComponentHelper.literal("Le portail des abysses s'est refermé.", ChatFormatting.RED));
            return;
        }

        if (!this.invitedPlayers.remove(player.getUUID())) {
            player.sendSystemMessage(ComponentHelper.literal("Ce portail ne t'a pas invité.", ChatFormatting.RED));
            return;
        }

        player.stopRiding();
        player.teleportTo(
                (ServerLevel) this.level(),
                this.getX(),
                this.getY() + 0.15D,
                this.getZ(),
                player.getYRot(),
                player.getXRot()
        );
        player.sendSystemMessage(ComponentHelper.literal("Le portail des abysses t'attire en son centre.", ChatFormatting.DARK_AQUA));
        this.level().playSound(null, this.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 0.55F);
    }

    public void refuse(ServerPlayer player) {
        this.invitedPlayers.remove(player.getUUID());
        player.sendSystemMessage(ComponentHelper.literal("Tu refuses l'appel du portail abyssal.", ChatFormatting.GRAY));
        getOwner().ifPresent(owner -> owner.sendSystemMessage(ComponentHelper.literal(player.getGameProfile().getName() + " refuse la téléportation abyssale.", ChatFormatting.GRAY)));
    }

    public int getPortalId() {
        return this.getId();
    }

    private void tickWaitingForHelper() {
        if (this.age >= WAIT_FOR_HELPER_TICKS) {
            refundOwnerInk();
            getOwner().ifPresent(owner -> owner.sendSystemMessage(ComponentHelper.literal("Personne n'a aidé à canaliser le portail. L'encre abyssale te revient.", ChatFormatting.GRAY)));
            closePortal();
        }
    }

    private void beginChannel(ServerPlayer helper) {
        Optional<ServerPlayer> owner = getOwner();
        if (owner.isEmpty() || owner.get().distanceToSqr(this) > CHANNEL_DISTANCE * CHANNEL_DISTANCE
                || helper.distanceToSqr(this) > CHANNEL_DISTANCE * CHANNEL_DISTANCE) {
            helper.sendSystemMessage(ComponentHelper.literal("Les deux joueurs doivent rester près du trou noir.", ChatFormatting.RED));
            return;
        }

        this.helperUuid = helper.getUUID();
        this.ownerChannelStart = owner.get().position();
        this.helperChannelStart = helper.position();
        this.channelTicks = 0;
        setState(STATE_CHANNELING);
        setRenderRadius(1.25F);

        owner.get().sendSystemMessage(ComponentHelper.literal("Canalisation abyssale commencée. Ne bougez plus pendant 3 secondes.", ChatFormatting.DARK_PURPLE));
        helper.sendSystemMessage(ComponentHelper.literal("Canalisation abyssale commencée. Ne bougez plus pendant 3 secondes.", ChatFormatting.DARK_PURPLE));
        this.level().playSound(null, this.blockPosition(), SoundEvents.CONDUIT_ACTIVATE, SoundSource.PLAYERS, 1.0F, 0.45F);
    }

    private void tickChanneling() {
        Optional<ServerPlayer> owner = getOwner();
        Optional<ServerPlayer> helper = getHelper();
        if (owner.isEmpty() || helper.isEmpty()) {
            refundOwnerInk();
            closePortal();
            return;
        }

        ServerPlayer ownerPlayer = owner.get();
        ServerPlayer helperPlayer = helper.get();
        if (hasMoved(ownerPlayer, this.ownerChannelStart) || hasMoved(helperPlayer, this.helperChannelStart)
                || ownerPlayer.distanceToSqr(this) > CHANNEL_DISTANCE * CHANNEL_DISTANCE
                || helperPlayer.distanceToSqr(this) > CHANNEL_DISTANCE * CHANNEL_DISTANCE) {
            ownerPlayer.sendSystemMessage(ComponentHelper.literal("La canalisation a cassé. L'encre abyssale te revient.", ChatFormatting.RED));
            helperPlayer.sendSystemMessage(ComponentHelper.literal("La canalisation a cassé.", ChatFormatting.RED));
            refundOwnerInk();
            closePortal();
            return;
        }

        this.channelTicks++;
        setRenderRadius(1.25F + (OPEN_RADIUS - 1.25F) * (this.channelTicks / (float) CHANNEL_TICKS));
        applyChannelParticles(ownerPlayer, helperPlayer);

        if (this.channelTicks >= CHANNEL_TICKS) {
            openPortal(ownerPlayer, helperPlayer);
        }
    }

    private void tickOpen() {
        this.openTicks++;
        if (this.openTicks >= OPEN_LIFETIME_TICKS) {
            closePortal();
        }
    }

    private void openPortal(ServerPlayer owner, ServerPlayer helper) {
        setState(STATE_OPEN);
        setRenderRadius(OPEN_RADIUS);
        this.openTicks = 0;
        this.level().playSound(null, this.blockPosition(), SoundEvents.END_PORTAL_SPAWN, SoundSource.PLAYERS, 0.75F, 0.6F);
        owner.sendSystemMessage(ComponentHelper.literal("Le portail des abysses est ouvert pendant 1 minute.", ChatFormatting.DARK_AQUA));
        helper.sendSystemMessage(ComponentHelper.literal("Tu as stabilisé le portail abyssal.", ChatFormatting.DARK_AQUA));
        AbyssalPortalEvents.showInviteMenu(owner, this);
    }

    private void applyChannelParticles(ServerPlayer owner, ServerPlayer helper) {
        if (!(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }

        if (this.channelTicks % 10 == 0) {
            owner.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 25, 4, false, true, true));
            helper.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 25, 4, false, true, true));
        }

        serverLevel.sendParticles(ParticleTypes.REVERSE_PORTAL, this.getX(), this.getY(), this.getZ(), 12, 1.2D, 1.2D, 1.2D, 0.06D);
        serverLevel.sendParticles(ParticleTypes.SQUID_INK, owner.getX(), owner.getY(0.75D), owner.getZ(), 2, 0.2D, 0.2D, 0.2D, 0.02D);
        serverLevel.sendParticles(ParticleTypes.SQUID_INK, helper.getX(), helper.getY(0.75D), helper.getZ(), 2, 0.2D, 0.2D, 0.2D, 0.02D);
    }

    private void spawnClientParticles() {
        float radius = getRenderRadius();
        double angle = this.tickCount * 0.25D + this.random.nextDouble() * Math.PI * 2.0D;
        double particleRadius = radius * (0.25D + this.random.nextDouble() * 0.75D);
        double px = this.getX() + Math.cos(angle) * particleRadius;
        double py = this.getY() + (this.random.nextDouble() - 0.5D) * radius * 0.45D;
        double pz = this.getZ() + Math.sin(angle) * particleRadius;

        this.level().addParticle(ParticleTypes.SQUID_INK, px, py, pz, 0.0D, 0.01D, 0.0D);
        if (this.tickCount % 2 == 0) {
            this.level().addParticle(ParticleTypes.REVERSE_PORTAL, px, py, pz, 0.0D, 0.0D, 0.0D);
        }
        if (isOpen() && this.tickCount % 3 == 0) {
            this.level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, px, py, pz, 0.0D, 0.01D, 0.0D);
        }
    }

    private boolean hasMoved(ServerPlayer player, Vec3 start) {
        return start == null || player.position().subtract(start).horizontalDistanceSqr() > MOVE_TOLERANCE_SQR;
    }

    private void refundOwnerInk() {
        if (this.refundedInk) {
            return;
        }

        getOwner().ifPresent(owner -> AbyssalStaffItem.refundAbyssalInk(owner, PORTAL_INK_REFUND));
        this.refundedInk = true;
    }

    private Optional<ServerPlayer> getOwner() {
        if (!(this.level() instanceof ServerLevel serverLevel) || this.ownerUuid == null) {
            return Optional.empty();
        }
        Player player = serverLevel.getPlayerByUUID(this.ownerUuid);
        return player instanceof ServerPlayer serverPlayer ? Optional.of(serverPlayer) : Optional.empty();
    }

    private Optional<ServerPlayer> getHelper() {
        if (!(this.level() instanceof ServerLevel serverLevel) || this.helperUuid == null) {
            return Optional.empty();
        }
        Player player = serverLevel.getPlayerByUUID(this.helperUuid);
        return player instanceof ServerPlayer serverPlayer ? Optional.of(serverPlayer) : Optional.empty();
    }

    private void closePortal() {
        if (this.level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(ParticleTypes.SQUID_INK, this.getX(), this.getY(), this.getZ(), 55, getRenderRadius(), 0.7D, getRenderRadius(), 0.08D);
        }
        this.discard();
    }

    private int getState() {
        return this.entityData.get(PORTAL_STATE);
    }

    private void setState(int state) {
        this.entityData.set(PORTAL_STATE, state);
    }

    private void setRenderRadius(float radius) {
        this.entityData.set(RENDER_RADIUS, radius);
    }

    private static class ComponentHelper {
        private static net.minecraft.network.chat.Component literal(String text, ChatFormatting color) {
            return net.minecraft.network.chat.Component.literal(text).withStyle(color);
        }
    }
}
