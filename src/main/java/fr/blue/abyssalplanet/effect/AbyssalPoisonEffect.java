package fr.blue.abyssalplanet.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

import java.util.UUID;

public class AbyssalPoisonEffect extends MobEffect {
    public static final String OWNER_TAG = "AbyssalPoisonOwner";
    private static final int DAMAGE_INTERVAL_TICKS = 25;

    public AbyssalPoisonEffect() {
        super(MobEffectCategory.HARMFUL, 0x39D6B4);
    }

    @Override
    public void applyEffectTick(LivingEntity target, int amplifier) {
        if (!(target.level() instanceof ServerLevel level) || target.getHealth() <= 1.0F) {
            return;
        }

        float damage = Math.min(amplifier + 1.0F, target.getHealth() - 1.0F);
        float healthBefore = target.getHealth();

        if (!target.hurt(target.damageSources().magic(), damage)) {
            return;
        }

        float poisonDamageDealt = Math.max(0.0F, healthBefore - target.getHealth());
        if (poisonDamageDealt <= 0.0F || !target.getPersistentData().hasUUID(OWNER_TAG)) {
            return;
        }

        UUID ownerId = target.getPersistentData().getUUID(OWNER_TAG);
        ServerPlayer owner = level.getServer().getPlayerList().getPlayer(ownerId);
        if (owner != null && owner.isAlive()) {
            owner.heal(poisonDamageDealt);
        }
    }

    @Override
    public boolean isDurationEffectTick(int duration, int amplifier) {
        return duration % DAMAGE_INTERVAL_TICKS == 0;
    }
}
