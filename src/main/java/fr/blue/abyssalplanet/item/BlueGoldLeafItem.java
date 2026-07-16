package fr.blue.abyssalplanet.item;

import fr.blue.abyssalplanet.registry.ModEffects;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public final class BlueGoldLeafItem extends Item {
    public static final String BONUS_ABSORPTION_TAG = "AbyssalPlanetBlueGoldAbsorption";
    private static final float HEAL_OR_BONUS_POINTS = 2.0F;
    private static final float MAX_BONUS_ABSORPTION_POINTS = 12.0F;

    public BlueGoldLeafItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        ItemStack result = super.finishUsingItem(stack, level, livingEntity);
        if (!level.isClientSide && livingEntity instanceof ServerPlayer player) {
            if (player.getHealth() < player.getMaxHealth()) {
                player.heal(HEAL_OR_BONUS_POINTS);
            } else {
                grantBonusHeart(player);
            }
        }
        return result;
    }

    private static void grantBonusHeart(ServerPlayer player) {
        CompoundTag persistentData = player.getPersistentData();
        float trackedBonus = Mth.clamp(
                persistentData.getFloat(BONUS_ABSORPTION_TAG),
                0.0F,
                MAX_BONUS_ABSORPTION_POINTS
        );
        float granted = Math.min(HEAL_OR_BONUS_POINTS, MAX_BONUS_ABSORPTION_POINTS - trackedBonus);
        if (granted <= 0.0F) {
            syncHeartMarker(player, trackedBonus);
            return;
        }

        trackedBonus += granted;
        persistentData.putFloat(BONUS_ABSORPTION_TAG, trackedBonus);
        player.setAbsorptionAmount(player.getAbsorptionAmount() + granted);
        syncHeartMarker(player, trackedBonus);
    }

    public static void reconcileBonusHearts(ServerPlayer player) {
        CompoundTag persistentData = player.getPersistentData();
        float trackedBonus = Mth.clamp(
                persistentData.getFloat(BONUS_ABSORPTION_TAG),
                0.0F,
                MAX_BONUS_ABSORPTION_POINTS
        );
        trackedBonus = Math.min(trackedBonus, Math.max(0.0F, player.getAbsorptionAmount()));

        if (trackedBonus <= 0.0F) {
            persistentData.remove(BONUS_ABSORPTION_TAG);
            player.removeEffect(ModEffects.BLUE_GOLD_HEARTS.get());
            return;
        }

        persistentData.putFloat(BONUS_ABSORPTION_TAG, trackedBonus);
        syncHeartMarker(player, trackedBonus);
    }

    public static void clearBonusHearts(ServerPlayer player) {
        player.getPersistentData().remove(BONUS_ABSORPTION_TAG);
        player.removeEffect(ModEffects.BLUE_GOLD_HEARTS.get());
    }

    private static void syncHeartMarker(ServerPlayer player, float bonusPoints) {
        int displayedPoints = Mth.clamp(Mth.ceil(bonusPoints), 1, (int) MAX_BONUS_ABSORPTION_POINTS);
        int amplifier = displayedPoints - 1;
        MobEffectInstance current = player.getEffect(ModEffects.BLUE_GOLD_HEARTS.get());
        if (current == null || current.getAmplifier() != amplifier || current.getDuration() < 20 * 60) {
            if (current != null) {
                player.removeEffect(ModEffects.BLUE_GOLD_HEARTS.get());
            }
            player.addEffect(new MobEffectInstance(
                    ModEffects.BLUE_GOLD_HEARTS.get(),
                    Integer.MAX_VALUE,
                    amplifier,
                    false,
                    false,
                    false
            ));
        }
    }
}
