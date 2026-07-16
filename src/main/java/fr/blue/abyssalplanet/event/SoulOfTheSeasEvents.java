package fr.blue.abyssalplanet.event;

import fr.blue.abyssalplanet.AbyssalPlanet;
import fr.blue.abyssalplanet.registry.ModItems;
import fr.blue.abyssalplanet.registry.ModSounds;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Mod.EventBusSubscriber(modid = AbyssalPlanet.MOD_ID)
public class SoulOfTheSeasEvents {
    private static final UUID SOUL_OF_THE_SEAS_HEALTH_UUID =
            UUID.fromString("1e6d03b0-4fd2-4d99-bfa6-1d331b04f91b");
    private static final UUID SOUL_OF_THE_SEAS_SWIM_SPEED_UUID =
            UUID.fromString("bb80dd42-d0dd-4b76-8f26-e58d1c07771c");

    private static final String HEALTH_MODIFIER_NAME = "Totem of Abyssal Souls health bonus";
    private static final String SWIM_SPEED_MODIFIER_NAME = "Totem of Abyssal Souls swim speed bonus";
    private static final double HEALTH_BONUS = 12.0D;
    private static final double SWIM_SPEED_BONUS = 0.5D;
    private static final int PASSIVE_EFFECT_DURATION_TICKS = 20 * 14;
    private static final int DOLPHINS_GRACE_DURATION_TICKS = 20 * 4;
    private static final Set<UUID> PLAYERS_WITH_TOTEM_IN_OFFHAND = new HashSet<>();

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        Player player = event.player;
        if (player.level().isClientSide) return;

        boolean hasSoulInOffhand = player.getOffhandItem().is(ModItems.SOUL_OF_THE_SEAS.get());

        updateTotemEquipSound(player, hasSoulInOffhand);
        updateHealthBonus(player, hasSoulInOffhand);
        updateSwimSpeedBonus(player, hasSoulInOffhand);

        if (hasSoulInOffhand && player.tickCount % 20 == 0) {
            applyPassiveEffects(player);
        }
    }

    private static void updateTotemEquipSound(Player player, boolean hasSoulInOffhand) {
        UUID playerId = player.getUUID();
        boolean hadSoulInOffhand = PLAYERS_WITH_TOTEM_IN_OFFHAND.contains(playerId);

        if (hasSoulInOffhand && !hadSoulInOffhand) {
            PLAYERS_WITH_TOTEM_IN_OFFHAND.add(playerId);
            if (player instanceof ServerPlayer serverPlayer) {
                serverPlayer.playNotifySound(ModSounds.TOTEM_EQUIP.get(), SoundSource.PLAYERS, 0.9F, 1.0F);
            }
            player.level().playSound(
                    player,
                    player.getX(),
                    player.getY(),
                    player.getZ(),
                    ModSounds.TOTEM_EQUIP.get(),
                    SoundSource.PLAYERS,
                    0.9F,
                    1.0F
            );
        }

        if (!hasSoulInOffhand && hadSoulInOffhand) {
            PLAYERS_WITH_TOTEM_IN_OFFHAND.remove(playerId);
        }
    }

    private static void updateHealthBonus(Player player, boolean hasSoulInOffhand) {
        AttributeInstance maxHealth = player.getAttribute(Attributes.MAX_HEALTH);

        if (maxHealth == null) {
            return;
        }

        AttributeModifier existingHealthModifier = maxHealth.getModifier(SOUL_OF_THE_SEAS_HEALTH_UUID);

        if (hasSoulInOffhand && existingHealthModifier == null) {
            maxHealth.addPermanentModifier(new AttributeModifier(
                    SOUL_OF_THE_SEAS_HEALTH_UUID,
                    HEALTH_MODIFIER_NAME,
                    HEALTH_BONUS,
                    AttributeModifier.Operation.ADDITION
            ));
        }

        if (!hasSoulInOffhand && existingHealthModifier != null) {
            maxHealth.removeModifier(SOUL_OF_THE_SEAS_HEALTH_UUID);

            if (player.getHealth() > player.getMaxHealth()) {
                player.setHealth(player.getMaxHealth());
            }
        }
    }

    private static void updateSwimSpeedBonus(Player player, boolean hasSoulInOffhand) {
        AttributeInstance swimSpeed = player.getAttribute(ForgeMod.SWIM_SPEED.get());

        if (swimSpeed == null) {
            return;
        }

        AttributeModifier existingSwimModifier = swimSpeed.getModifier(SOUL_OF_THE_SEAS_SWIM_SPEED_UUID);

        if (hasSoulInOffhand && existingSwimModifier == null) {
            swimSpeed.addPermanentModifier(new AttributeModifier(
                    SOUL_OF_THE_SEAS_SWIM_SPEED_UUID,
                    SWIM_SPEED_MODIFIER_NAME,
                    SWIM_SPEED_BONUS,
                    AttributeModifier.Operation.MULTIPLY_TOTAL
            ));
        }

        if (!hasSoulInOffhand && existingSwimModifier != null) {
            swimSpeed.removeModifier(SOUL_OF_THE_SEAS_SWIM_SPEED_UUID);
        }
    }

    private static void applyPassiveEffects(Player player) {
        player.addEffect(new MobEffectInstance(
                MobEffects.WATER_BREATHING,
                PASSIVE_EFFECT_DURATION_TICKS,
                0,
                false,
                false,
                true
        ));

        player.addEffect(new MobEffectInstance(
                MobEffects.NIGHT_VISION,
                PASSIVE_EFFECT_DURATION_TICKS,
                0,
                false,
                false,
                true
        ));

        player.addEffect(new MobEffectInstance(
                MobEffects.DOLPHINS_GRACE,
                DOLPHINS_GRACE_DURATION_TICKS,
                0,
                false,
                false,
                true
        ));
    }
}
