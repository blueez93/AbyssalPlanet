package fr.blue.abyssalplanet.entity;

import fr.blue.abyssalplanet.item.AbyssalPouchItem;
import fr.blue.abyssalplanet.registry.ModEntities;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;

public class AbyssalPouchGhostEntity extends Entity {
    private UUID ownerUuid;
    private UUID pouchId;

    public AbyssalPouchGhostEntity(EntityType<? extends AbyssalPouchGhostEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
    }

    public static void spawn(ServerLevel level, UUID ownerUuid, UUID pouchId, Vec3 position) {
        AbyssalPouchGhostEntity ghost = ModEntities.ABYSSAL_POUCH_GHOST.get().create(level);
        if (ghost == null) {
            return;
        }

        ghost.ownerUuid = ownerUuid;
        ghost.pouchId = pouchId;
        ghost.moveTo(position.x, position.y, position.z, 0.0F, 0.0F);
        level.addFreshEntity(ghost);
    }

    public static void removeLoadedCopies(ServerLevel level, UUID ownerUuid, UUID pouchId, Vec3 center) {
        AABB searchBox = new AABB(center, center).inflate(512.0D);
        List<AbyssalPouchGhostEntity> ghosts = level.getEntitiesOfClass(
                AbyssalPouchGhostEntity.class,
                searchBox,
                ghost -> ghost.isAlive()
                        && ownerUuid.equals(ghost.ownerUuid)
                        && pouchId.equals(ghost.pouchId)
        );
        ghosts.forEach(AbyssalPouchGhostEntity::discard);
    }

    @Override
    public void tick() {
        super.tick();

        if (this.level().isClientSide) {
            spawnClientParticles();
            return;
        }

        if (this.tickCount % 20 == 0 && getOwner().filter(owner ->
                AbyssalPouchItem.findPouch(owner, this.pouchId).isPresent()).isEmpty()) {
            discard();
        }
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (this.level().isClientSide) {
            return InteractionResult.sidedSuccess(true);
        }

        if (!(player instanceof ServerPlayer opener)) {
            return InteractionResult.SUCCESS;
        }

        Optional<ServerPlayer> owner = getOwner();
        if (owner.isEmpty() || this.pouchId == null
                || AbyssalPouchItem.findPouch(owner.get(), this.pouchId).isEmpty()) {
            opener.sendSystemMessage(Component.literal("La bourse fantomatique se dissipe.")
                    .withStyle(ChatFormatting.GRAY));
            discard();
            return InteractionResult.SUCCESS;
        }

        if (!AbyssalPouchItem.ghostSearchConsumesSlot(opener, owner.get(), this.pouchId)) {
            discard();
            return InteractionResult.SUCCESS;
        }

        AbyssalPouchItem.openPouch(opener, owner.get(), this.pouchId);
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    @Override
    public float getPickRadius() {
        return 0.75F;
    }

    @Override
    protected void defineSynchedData() {
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.hasUUID("Owner")) {
            this.ownerUuid = tag.getUUID("Owner");
        }
        if (tag.hasUUID("PouchId")) {
            this.pouchId = tag.getUUID("PouchId");
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        if (this.ownerUuid != null) {
            tag.putUUID("Owner", this.ownerUuid);
        }
        if (this.pouchId != null) {
            tag.putUUID("PouchId", this.pouchId);
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    private Optional<ServerPlayer> getOwner() {
        if (!(this.level() instanceof ServerLevel serverLevel) || this.ownerUuid == null) {
            return Optional.empty();
        }

        Player player = serverLevel.getPlayerByUUID(this.ownerUuid);
        return player instanceof ServerPlayer serverPlayer ? Optional.of(serverPlayer) : Optional.empty();
    }

    private void spawnClientParticles() {
        if (this.tickCount % 3 != 0) {
            return;
        }

        double angle = this.tickCount * 0.18D + this.random.nextDouble() * Math.PI * 2.0D;
        double radius = 0.35D + this.random.nextDouble() * 0.25D;
        double x = this.getX() + Math.cos(angle) * radius;
        double y = this.getY() + 0.2D + this.random.nextDouble() * 0.45D;
        double z = this.getZ() + Math.sin(angle) * radius;
        this.level().addParticle(ParticleTypes.REVERSE_PORTAL, x, y, z, 0.0D, 0.01D, 0.0D);
        if (this.tickCount % 9 == 0) {
            this.level().addParticle(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 0.0D, 0.01D, 0.0D);
        }
    }
}
