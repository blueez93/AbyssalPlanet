package fr.blue.abyssalplanet.event;

import fr.blue.abyssalplanet.AbyssalPlanet;
import fr.blue.abyssalplanet.registry.ModEnchantments;
import net.minecraft.ChatFormatting;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.UUID;

@Mod.EventBusSubscriber(modid = AbyssalPlanet.MOD_ID)
public class AbyssalVenomEvents {
    private static final String VENOM_LEVEL_TAG = "AbyssalVenomLevel";
    private static final String VENOM_OWNER_TAG = "AbyssalVenomOwner";
    private static final String VENOM_OWNER_NAME_TAG = "AbyssalVenomOwnerName";

    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel) || event.getEntity().isDeadOrDying()) {
            return;
        }

        Entity attackerEntity = event.getSource().getEntity();
        if (!(attackerEntity instanceof LivingEntity attacker)) {
            return;
        }

        ItemStack weapon = attacker.getMainHandItem();
        int venomLevel = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.ABYSSAL_VENOM.get(), weapon);
        if (venomLevel <= 0) {
            weapon = attacker.getOffhandItem();
            venomLevel = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.ABYSSAL_VENOM.get(), weapon);
        }

        if (venomLevel <= 0) {
            return;
        }

        applyPermanentVenom(attacker, event.getEntity(), venomLevel);
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        LivingEntity target = event.getEntity();
        if (!(target.level() instanceof ServerLevel level) || target.tickCount % 20 != 0 || target.isDeadOrDying()) {
            return;
        }

        CompoundTag data = target.getPersistentData();
        int venomLevel = data.getInt(VENOM_LEVEL_TAG);
        if (venomLevel <= 0) {
            return;
        }

        target.addEffect(new MobEffectInstance(
                MobEffects.POISON,
                20 * 5,
                Math.max(0, venomLevel - 1),
                false,
                true,
                true
        ));

        float executeThreshold = getExecuteThreshold(venomLevel);
        if (target.getHealth() > executeThreshold) {
            return;
        }

        LivingEntity owner = findOwner(level, data);
        if (owner != null) {
            target.hurt(owner.damageSources().mobAttack(owner), 1000.0F);
        } else {
            target.hurt(target.damageSources().magic(), 1000.0F);
        }
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        CompoundTag data = event.getEntity().getPersistentData();
        data.remove(VENOM_LEVEL_TAG);
        data.remove(VENOM_OWNER_TAG);
        data.remove(VENOM_OWNER_NAME_TAG);
    }

    private static void applyPermanentVenom(LivingEntity attacker, LivingEntity target, int venomLevel) {
        if (target instanceof Player player && (player.isCreative() || player.isSpectator())) {
            return;
        }

        CompoundTag data = target.getPersistentData();
        int currentLevel = data.getInt(VENOM_LEVEL_TAG);
        if (currentLevel > venomLevel) {
            return;
        }

        data.putInt(VENOM_LEVEL_TAG, Math.max(1, Math.min(3, venomLevel)));
        data.putUUID(VENOM_OWNER_TAG, attacker.getUUID());
        data.putString(VENOM_OWNER_NAME_TAG, attacker.getName().getString());
    }

    private static LivingEntity findOwner(ServerLevel level, CompoundTag data) {
        if (!data.hasUUID(VENOM_OWNER_TAG)) {
            return null;
        }

        UUID ownerId = data.getUUID(VENOM_OWNER_TAG);
        Entity owner = level.getEntity(ownerId);
        if (owner instanceof LivingEntity living && living.isAlive()) {
            return living;
        }

        ServerPlayer player = level.getServer().getPlayerList().getPlayer(ownerId);
        return player != null && player.isAlive() ? player : null;
    }

    private static float getExecuteThreshold(int venomLevel) {
        return switch (venomLevel) {
            case 3 -> 10.0F;
            case 2 -> 6.0F;
            default -> 4.0F;
        };
    }
}
