package fr.blue.abyssalplanet.item;

import fr.blue.abyssalplanet.entity.ZwoingEntity;
import fr.blue.abyssalplanet.entity.ZwoingProjectileEntity;
import fr.blue.abyssalplanet.registry.ModEntities;
import fr.blue.abyssalplanet.registry.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

import java.util.UUID;

public class ZwoingItem extends Item {
    private static final String VARIANT_TAG = "ZwoingVariant";
    private static final String OWNER_TAG = "ZwoingOwner";
    private static final String EQUIPPED_EFFECT_TAG = "ZwoingEquippedEffect";
    private static final int THROW_CHARGE_TICKS = 12;

    public ZwoingItem(Properties properties) {
        super(properties);
    }

    public static ItemStack capture(ZwoingEntity zwoing, Player newOwner) {
        ItemStack stack = createVariantStack(zwoing.getVariant());
        setVariant(stack, zwoing.getVariant());
        setOwner(stack, newOwner.getUUID());
        setEquippedEffect(stack, zwoing.getEquippedEffect());

        if (zwoing.hasCustomName()) {
            stack.setHoverName(zwoing.getCustomName());
        }

        return stack;
    }

    public static void applyStackData(ZwoingEntity zwoing, ItemStack stack, UUID fallbackOwner) {
        zwoing.setVariant(getVariant(stack));
        zwoing.setEquippedEffect(getEquippedEffect(stack));
        zwoing.setOwnerUuid(getOwner(stack, fallbackOwner));

        if (stack.hasCustomHoverName()) {
            zwoing.setCustomName(stack.getHoverName());
            zwoing.setCustomNameVisible(false);
        }
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        player.startUsingItem(hand);
        return InteractionResultHolder.consume(stack);
    }

    @Override
    public void releaseUsing(ItemStack stack, Level level, LivingEntity livingEntity, int timeLeft) {
        if (!(livingEntity instanceof ServerPlayer player) || !(level instanceof ServerLevel serverLevel)) {
            return;
        }

        int chargeTicks = getUseDuration(stack) - timeLeft;
        setOwner(stack, player.getUUID());

        boolean used;
        if (chargeTicks >= THROW_CHARGE_TICKS) {
            used = throwZwoing(serverLevel, player, stack, chargeTicks);
        } else {
            used = placeZwoing(serverLevel, player, stack);
        }

        if (!used) {
            return;
        }

        if (!player.getAbilities().instabuild) {
            stack.shrink(1);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
    }

    private static boolean placeZwoing(ServerLevel level, ServerPlayer player, ItemStack stack) {
        BlockPos floorPosition = findFloorNearLook(level, player);
        if (floorPosition == null) {
            player.sendSystemMessage(Component.literal("Le Zwoing a besoin d'un fond solide sous l'eau.")
                    .withStyle(ChatFormatting.AQUA));
            return false;
        }

        ZwoingEntity zwoing = ModEntities.ZWOING.get().create(level);
        if (zwoing == null) {
            return false;
        }

        applyStackData(zwoing, stack, player.getUUID());
        zwoing.moveTo(
                floorPosition.getX() + 0.5D,
                floorPosition.getY() + 0.05D,
                floorPosition.getZ() + 0.5D,
                player.getYRot(),
                0.0F
        );
        zwoing.setPersistenceRequired();
        level.addFreshEntity(zwoing);
        player.swing(player.getUsedItemHand(), true);
        return true;
    }

    private static boolean throwZwoing(
            ServerLevel level,
            ServerPlayer player,
            ItemStack stack,
            int chargeTicks
    ) {
        float power = Math.min(1.0F, chargeTicks / 30.0F);
        ZwoingProjectileEntity projectile = new ZwoingProjectileEntity(level, player);
        projectile.setItem(stack.copyWithCount(1));
        projectile.shootFromRotation(
                player,
                player.getXRot(),
                player.getYRot(),
                0.0F,
                0.75F + power * 0.85F,
                0.35F
        );
        level.addFreshEntity(projectile);
        player.swing(player.getUsedItemHand(), true);
        return true;
    }

    private static BlockPos findFloorNearLook(ServerLevel level, ServerPlayer player) {
        BlockPos origin = BlockPos.containing(
                player.getEyePosition().add(player.getLookAngle().scale(2.5D))
        );

        for (int yOffset = 3; yOffset >= -14; yOffset--) {
            BlockPos feet = origin.offset(0, yOffset, 0);
            BlockPos floor = feet.below();
            if (level.getFluidState(feet).is(net.minecraft.tags.FluidTags.WATER)
                    && level.getBlockState(floor).isFaceSturdy(level, floor, Direction.UP)) {
                return feet;
            }
        }

        return null;
    }

    @Override
    public int getUseDuration(ItemStack stack) {
        return 72000;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.BOW;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.abyssalplanet.zwoing." + variantName(getVariant(stack)));
    }

    @Override
    public Rarity getRarity(ItemStack stack) {
        return switch (getVariant(stack)) {
            case ZwoingEntity.RED -> Rarity.RARE;
            case ZwoingEntity.GREEN, ZwoingEntity.PURPLE, ZwoingEntity.MULTICOLOR -> Rarity.EPIC;
            default -> Rarity.COMMON;
        };
    }

    public static int getVariant(ItemStack stack) {
        if (stack.is(ModItems.MULTICOLOR_ZWOING.get())) {
            return ZwoingEntity.MULTICOLOR;
        }
        CompoundTag tag = stack.getTag();
        return tag == null ? ZwoingEntity.BLUE : Math.max(
                ZwoingEntity.BLUE,
                Math.min(ZwoingEntity.MULTICOLOR, tag.getInt(VARIANT_TAG))
        );
    }

    public static void setVariant(ItemStack stack, int variant) {
        stack.getOrCreateTag().putInt(VARIANT_TAG, variant);
    }

    public static ItemStack createVariantStack(int variant) {
        ItemStack stack = variant == ZwoingEntity.MULTICOLOR
                ? new ItemStack(ModItems.MULTICOLOR_ZWOING.get())
                : new ItemStack(ModItems.ZWOING.get());
        setVariant(stack, variant);
        return stack;
    }

    public static void setOwner(ItemStack stack, UUID ownerUuid) {
        stack.getOrCreateTag().putUUID(OWNER_TAG, ownerUuid);
    }

    public static int getEquippedEffect(ItemStack stack) {
        CompoundTag tag = stack.getTag();
        return tag == null ? ZwoingEntity.EFFECT_NONE : Math.max(
                ZwoingEntity.EFFECT_NONE,
                Math.min(ZwoingEntity.EFFECT_INK, tag.getInt(EQUIPPED_EFFECT_TAG))
        );
    }

    public static void setEquippedEffect(ItemStack stack, int effect) {
        stack.getOrCreateTag().putInt(EQUIPPED_EFFECT_TAG, effect);
    }

    private static UUID getOwner(ItemStack stack, UUID fallbackOwner) {
        CompoundTag tag = stack.getTag();
        return tag != null && tag.hasUUID(OWNER_TAG) ? tag.getUUID(OWNER_TAG) : fallbackOwner;
    }

    private static String variantName(int variant) {
        return switch (variant) {
            case ZwoingEntity.RED -> "red";
            case ZwoingEntity.GREEN -> "green";
            case ZwoingEntity.PURPLE -> "purple";
            case ZwoingEntity.MULTICOLOR -> "multicolor";
            default -> "blue";
        };
    }

    public static int getVariantTint(ItemStack stack) {
        return switch (getVariant(stack)) {
            case ZwoingEntity.RED -> 0xF04C5A;
            case ZwoingEntity.GREEN -> 0x58DC78;
            case ZwoingEntity.PURPLE -> 0xC56CFF;
            case ZwoingEntity.MULTICOLOR -> 0xFFFFFF;
            default -> 0x55B8FF;
        };
    }
}
