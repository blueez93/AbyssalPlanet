package fr.blue.abyssalplanet.item;

import fr.blue.abyssalplanet.entity.GeorgesSeniorEntity;
import fr.blue.abyssalplanet.registry.ModEntities;
import fr.blue.abyssalplanet.registry.ModItems;
import fr.blue.abyssalplanet.world.AbyssalCompanionData;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.UUID;

public final class AbyssalWhistleItem extends Item {
    private static final String SENIOR_UUID_TAG = "SeniorUuid";
    private static final String STORED_ENTITY_TAG = "StoredSenior";
    private static final int COOLDOWN_TICKS = 20 * 5;

    public AbyssalWhistleItem(Properties properties) {
        super(properties);
    }

    public static ItemStack createLinked(UUID seniorUuid) {
        ItemStack stack = new ItemStack(ModItems.ABYSSAL_WHISTLE.get());
        stack.getOrCreateTag().putUUID(SENIOR_UUID_TAG, seniorUuid);
        return stack;
    }

    public static UUID getLinkedSenior(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.hasUUID(SENIOR_UUID_TAG) ? tag.getUUID(SENIOR_UUID_TAG) : null;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer) {
            toggleSenior(serverPlayer, stack);
            level.playSound(
                    null,
                    player.blockPosition(),
                    SoundEvents.AMETHYST_BLOCK_CHIME,
                    SoundSource.PLAYERS,
                    1.1F,
                    0.75F
            );
            player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        }
        return InteractionResultHolder.consume(stack);
    }

    private static void toggleSenior(ServerPlayer player, ItemStack whistle) {
        UUID seniorUuid = getLinkedSenior(whistle);
        if (seniorUuid == null) {
            player.displayClientMessage(
                    Component.translatable("message.abyssalplanet.whistle.unlinked").withStyle(ChatFormatting.RED),
                    true
            );
            return;
        }

        CompoundTag tag = whistle.getOrCreateTag();
        if (tag.contains(STORED_ENTITY_TAG, CompoundTag.TAG_COMPOUND)) {
            summonStoredSenior(player, whistle, seniorUuid);
            return;
        }

        GeorgesSeniorEntity senior = findSenior(player, seniorUuid);
        if (senior == null) {
            player.displayClientMessage(
                    Component.translatable("message.abyssalplanet.whistle.not_found").withStyle(ChatFormatting.RED),
                    true
            );
            return;
        }
        if (!senior.isOwnedBy(player)) {
            player.displayClientMessage(
                    Component.translatable("message.abyssalplanet.whistle.not_owner").withStyle(ChatFormatting.RED),
                    true
            );
            return;
        }

        senior.ejectPassengers();
        CompoundTag entityData = new CompoundTag();
        senior.saveWithoutId(entityData);
        entityData.putUUID("UUID", seniorUuid);
        tag.put(STORED_ENTITY_TAG, entityData);
        AbyssalCompanionData.get(player.getServer()).remove(seniorUuid);
        senior.discard();
        player.displayClientMessage(
                Component.translatable("message.abyssalplanet.whistle.stored"),
                true
        );
    }

    private static void summonStoredSenior(
            ServerPlayer player,
            ItemStack whistle,
            UUID seniorUuid
    ) {
        CompoundTag tag = whistle.getOrCreateTag();
        CompoundTag entityData = tag.getCompound(STORED_ENTITY_TAG).copy();
        GeorgesSeniorEntity senior = ModEntities.GEORGES_SENIOR.get().create(player.serverLevel());
        if (senior == null) {
            return;
        }
        entityData.putUUID("UUID", seniorUuid);
        senior.load(entityData);
        senior.setUUID(seniorUuid);
        Vec3 position = findSpawnPosition(player);
        senior.moveTo(position.x, position.y, position.z, player.getYRot(), 0.0F);
        senior.setDeltaMovement(Vec3.ZERO);
        senior.setPersistenceRequired();
        if (player.serverLevel().addFreshEntity(senior)) {
            tag.remove(STORED_ENTITY_TAG);
            senior.syncPersistentPetRecord();
            AbyssalCompanionData.get(player.getServer()).markSeniorAlive(seniorUuid);
            player.displayClientMessage(
                    Component.translatable("message.abyssalplanet.whistle.summoned"),
                    true
            );
        }
    }

    private static Vec3 findSpawnPosition(ServerPlayer player) {
        Vec3 look = player.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        if (look.lengthSqr() > 0.001D) {
            look = look.normalize();
        }
        return player.position().add(look.scale(3.0D)).add(0.0D, 0.35D, 0.0D);
    }

    private static GeorgesSeniorEntity findSenior(ServerPlayer player, UUID uuid) {
        for (ServerLevel level : player.getServer().getAllLevels()) {
            Entity entity = level.getEntity(uuid);
            if (entity instanceof GeorgesSeniorEntity senior) {
                return senior;
            }
        }
        for (AbyssalCompanionData.CompanionRecord record :
                AbyssalCompanionData.get(player.getServer()).getOwnedBy(player.getUUID())) {
            if (!record.petUuid().equals(uuid)) {
                continue;
            }
            ServerLevel source = player.getServer().getLevel(
                    net.minecraft.resources.ResourceKey.create(
                            net.minecraft.core.registries.Registries.DIMENSION,
                            record.dimension()
                    )
            );
            if (source != null) {
                source.getChunk(record.position().getX() >> 4, record.position().getZ() >> 4);
                Entity entity = source.getEntity(uuid);
                if (entity instanceof GeorgesSeniorEntity senior) {
                    return senior;
                }
            }
        }
        return null;
    }

    public static void breakLinkedWhistles(ServerPlayer owner, UUID seniorUuid) {
        for (int slot = 0; slot < owner.getInventory().getContainerSize(); slot++) {
            ItemStack stack = owner.getInventory().getItem(slot);
            if (stack.is(ModItems.ABYSSAL_WHISTLE.get()) && seniorUuid.equals(getLinkedSenior(stack))) {
                owner.getInventory().setItem(slot, createBroken(seniorUuid));
            }
        }
    }

    public static ItemStack createBroken(UUID seniorUuid) {
        ItemStack broken = new ItemStack(ModItems.BROKEN_ABYSSAL_WHISTLE.get());
        broken.getOrCreateTag().putUUID(SENIOR_UUID_TAG, seniorUuid);
        return broken;
    }

    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        super.inventoryTick(stack, level, entity, slot, selected);
        if (level.isClientSide || !(entity instanceof ServerPlayer player) || level.getGameTime() % 20 != 0) {
            return;
        }
        UUID seniorUuid = getLinkedSenior(stack);
        if (seniorUuid != null && AbyssalCompanionData.get(player.getServer()).isSeniorDead(seniorUuid)) {
            player.getInventory().setItem(slot, createBroken(seniorUuid));
        }
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 20;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.TOOT_HORN;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        return stack;
    }
}
