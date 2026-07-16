package fr.blue.abyssalplanet.item;

import fr.blue.abyssalplanet.event.BabyKrakenPetEvents;
import fr.blue.abyssalplanet.registry.ModDimensions;
import fr.blue.abyssalplanet.registry.ModItems;
import fr.blue.abyssalplanet.world.AbyssalTravelData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class GoOutItem extends Item {
    private static final String RETURN_DIMENSION_TAG = "ReturnDimension";
    private static final String RETURN_X_TAG = "ReturnX";
    private static final String RETURN_Y_TAG = "ReturnY";
    private static final String RETURN_Z_TAG = "ReturnZ";
    private static final int RETURN_DISTANCE = 10;

    public GoOutItem(Properties properties) {
        super(properties);
    }

    public static ItemStack createBoundToTyphoon(ResourceKey<Level> dimension, Vec3 typhoonPosition) {
        ItemStack stack = new ItemStack(ModItems.GO_OUT.get());
        CompoundTag tag = stack.getOrCreateTag();
        tag.putString(RETURN_DIMENSION_TAG, dimension.location().toString());
        tag.putDouble(RETURN_X_TAG, typhoonPosition.x);
        tag.putDouble(RETURN_Y_TAG, typhoonPosition.y);
        tag.putDouble(RETURN_Z_TAG, typhoonPosition.z);
        return stack;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, net.minecraft.world.entity.player.Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (level.isClientSide) {
            return InteractionResultHolder.consume(stack);
        }

        if (!(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResultHolder.fail(stack);
        }

        if (!serverPlayer.level().dimension().equals(ModDimensions.ABYSSAL_PLANET_LEVEL)) {
            serverPlayer.sendSystemMessage(Component.literal("Go Out ne répond que depuis le monde abyssal.")
                    .withStyle(ChatFormatting.RED));
            return InteractionResultHolder.fail(stack);
        }

        CompoundTag tag = stack.getTag();
        if (tag == null
                || !tag.contains(RETURN_DIMENSION_TAG)
                || !tag.contains(RETURN_X_TAG)
                || !tag.contains(RETURN_Y_TAG)
                || !tag.contains(RETURN_Z_TAG)) {
            serverPlayer.sendSystemMessage(Component.literal("Ce Go Out a perdu le lien avec son typhon.")
                    .withStyle(ChatFormatting.RED));
            return InteractionResultHolder.fail(stack);
        }

        ResourceLocation dimensionId = ResourceLocation.tryParse(tag.getString(RETURN_DIMENSION_TAG));
        if (dimensionId == null) {
            return InteractionResultHolder.fail(stack);
        }

        ResourceKey<Level> destinationKey = ResourceKey.create(Registries.DIMENSION, dimensionId);
        ServerLevel destinationLevel = serverPlayer.server.getLevel(destinationKey);
        if (destinationLevel == null) {
            serverPlayer.sendSystemMessage(Component.literal("Le monde d'origine du typhon est inaccessible.")
                    .withStyle(ChatFormatting.RED));
            return InteractionResultHolder.fail(stack);
        }

        Vec3 typhoonPosition = new Vec3(
                tag.getDouble(RETURN_X_TAG),
                tag.getDouble(RETURN_Y_TAG),
                tag.getDouble(RETURN_Z_TAG)
        );
        Vec3 destination = findReturnPosition(destinationLevel, serverPlayer, typhoonPosition);

        serverPlayer.teleportTo(
                destinationLevel,
                destination.x,
                destination.y,
                destination.z,
                serverPlayer.getYRot(),
                serverPlayer.getXRot()
        );
        BabyKrakenPetEvents.schedulePetCatchUp(serverPlayer);
        serverPlayer.fallDistance = 0.0F;
        AbyssalTravelData.grantTyphoonImmunity(serverPlayer, 20 * 10);
        destinationLevel.playSound(
                null,
                BlockPos.containing(destination),
                SoundEvents.ENDERMAN_TELEPORT,
                SoundSource.PLAYERS,
                0.8F,
                0.65F
        );

        if (!serverPlayer.getAbilities().instabuild) {
            stack.shrink(1);
        }

        return InteractionResultHolder.consume(stack);
    }

    private static Vec3 findReturnPosition(ServerLevel level, ServerPlayer player, Vec3 typhoonPosition) {
        double baseAngle = ((player.getUUID().getLeastSignificantBits() & 0xFFFFL) / 65535.0D) * Math.PI * 2.0D;

        for (int radius = RETURN_DISTANCE; radius <= RETURN_DISTANCE + 4; radius += 2) {
            for (int attempt = 0; attempt < 16; attempt++) {
                double angle = baseAngle + attempt * (Math.PI * 2.0D / 16.0D);
                int x = (int) Math.floor(typhoonPosition.x + Math.cos(angle) * radius);
                int z = (int) Math.floor(typhoonPosition.z + Math.sin(angle) * radius);
                int centerY = (int) Math.floor(typhoonPosition.y);

                for (int verticalOffset = 5; verticalOffset >= -6; verticalOffset--) {
                    BlockPos feet = new BlockPos(x, centerY + verticalOffset, z);
                    if (isSafeReturnPosition(level, feet)) {
                        return Vec3.atBottomCenterOf(feet);
                    }
                }
            }
        }

        return new Vec3(
                typhoonPosition.x + RETURN_DISTANCE,
                typhoonPosition.y + 1.0D,
                typhoonPosition.z
        );
    }

    private static boolean isSafeReturnPosition(ServerLevel level, BlockPos feet) {
        if (!level.getBlockState(feet).getCollisionShape(level, feet).isEmpty()
                || !level.getBlockState(feet.above()).getCollisionShape(level, feet.above()).isEmpty()) {
            return false;
        }

        BlockPos below = feet.below();
        return !level.getBlockState(below).getCollisionShape(level, below).isEmpty()
                || !level.getFluidState(below).isEmpty();
    }
}
