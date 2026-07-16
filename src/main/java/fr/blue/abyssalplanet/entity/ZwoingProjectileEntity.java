package fr.blue.abyssalplanet.entity;

import fr.blue.abyssalplanet.item.ZwoingItem;
import fr.blue.abyssalplanet.registry.ModEntities;
import fr.blue.abyssalplanet.registry.ModItems;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public class ZwoingProjectileEntity extends ThrowableItemProjectile {
    private boolean released;

    public ZwoingProjectileEntity(EntityType<? extends ZwoingProjectileEntity> entityType, Level level) {
        super(entityType, level);
    }

    public ZwoingProjectileEntity(Level level, LivingEntity owner) {
        super(ModEntities.ZWOING_PROJECTILE.get(), owner, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModItems.ZWOING.get();
    }

    @Override
    protected void onHitEntity(EntityHitResult hitResult) {
        super.onHitEntity(hitResult);
        if (hitResult.getEntity() instanceof LivingEntity target) {
            releaseZwoing(target, hitResult.getLocation());
        }
    }

    @Override
    protected void onHitBlock(BlockHitResult hitResult) {
        super.onHitBlock(hitResult);
        Vec3 offset = Vec3.atLowerCornerOf(hitResult.getDirection().getNormal()).scale(0.35D);
        releaseZwoing(null, hitResult.getLocation().add(offset));
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide && this.tickCount >= 20 * 10 && !this.released) {
            releaseZwoing(null, this.position());
        }
    }

    private void releaseZwoing(LivingEntity target, Vec3 position) {
        if (this.released || !(this.level() instanceof ServerLevel serverLevel)) {
            return;
        }
        this.released = true;

        ZwoingEntity zwoing = ModEntities.ZWOING.get().create(serverLevel);
        if (zwoing == null) {
            this.discard();
            return;
        }

        Entity projectileOwner = this.getOwner();
        UUID fallbackOwner = projectileOwner == null ? UUID.randomUUID() : projectileOwner.getUUID();
        ZwoingItem.applyStackData(zwoing, this.getItem(), fallbackOwner);
        zwoing.moveTo(position.x, position.y, position.z, this.getYRot(), 0.0F);
        zwoing.setPersistenceRequired();

        if (target != null) {
            zwoing.attachTo(target);
        }

        serverLevel.addFreshEntity(zwoing);
        this.discard();
    }
}
