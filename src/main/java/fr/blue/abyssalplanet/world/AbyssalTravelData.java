package fr.blue.abyssalplanet.world;

import net.minecraft.world.entity.player.Player;

public final class AbyssalTravelData {
    private static final String TYPHOON_IMMUNITY_UNTIL_TAG = "AbyssalTyphoonImmunityUntil";

    private AbyssalTravelData() {
    }

    public static void grantTyphoonImmunity(Player player, int durationTicks) {
        player.getPersistentData().putLong(
                TYPHOON_IMMUNITY_UNTIL_TAG,
                player.level().getGameTime() + durationTicks
        );
    }

    public static boolean hasTyphoonImmunity(Player player) {
        return player.getPersistentData().getLong(TYPHOON_IMMUNITY_UNTIL_TAG)
                > player.level().getGameTime();
    }
}
