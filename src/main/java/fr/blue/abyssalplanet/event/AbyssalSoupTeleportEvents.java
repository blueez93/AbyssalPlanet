package fr.blue.abyssalplanet.event;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import fr.blue.abyssalplanet.AbyssalPlanet;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AbyssalPlanet.MOD_ID)
public class AbyssalSoupTeleportEvents {
    private static final int CHOICE_DURATION_TICKS = 45 * 20;
    private static final int NAUSEA_DURATION_TICKS = 5 * 20;
    private static final int CONNECTION_DELAY_TICKS = 3 * 20;
    private static final int REGENERATION_DURATION_TICKS = 60 * 20;
    private static final float ONE_HEART_HEALTH = 2.0F;
    private static final Map<UUID, Long> PENDING_TELEPORTS = new HashMap<>();
    private static final Map<UUID, Long> PENDING_REGENERATION = new HashMap<>();

    private AbyssalSoupTeleportEvents() {
    }

    public static void offerTeleportChoice(ServerPlayer player) {
        PlayerList playerList = player.server.getPlayerList();
        long expiry = player.serverLevel().getGameTime() + CHOICE_DURATION_TICKS;
        PENDING_TELEPORTS.put(player.getUUID(), expiry);

        MutableComponent message = Component.literal("Soupe des abysses : choisis une destination ")
                .withStyle(ChatFormatting.DARK_AQUA);

        int availableTargets = 0;
        for (ServerPlayer target : playerList.getPlayers()) {
            if (target.getUUID().equals(player.getUUID())) {
                continue;
            }

            if (availableTargets > 0) {
                message.append(Component.literal(" "));
            }

            String targetName = target.getGameProfile().getName();
            message.append(Component.literal("[")
                    .append(Component.literal(targetName)
                            .withStyle(style -> style
                                    .withColor(ChatFormatting.AQUA)
                                    .withClickEvent(new ClickEvent(
                                            ClickEvent.Action.RUN_COMMAND,
                                            "/abyssal_soup_tp " + targetName))
                                    .withHoverEvent(new HoverEvent(
                                            HoverEvent.Action.SHOW_TEXT,
                                            Component.literal("Se téléporter vers " + targetName)))))
                    .append(Component.literal("]")));
            availableTargets++;
        }

        if (availableTargets == 0) {
            PENDING_TELEPORTS.remove(player.getUUID());
            player.sendSystemMessage(Component.literal("Soupe des abysses : aucun autre joueur en ligne.")
                    .withStyle(ChatFormatting.DARK_AQUA));
            return;
        }

        player.sendSystemMessage(message);
        player.sendSystemMessage(Component.literal("Le choix reste ouvert pendant 45 secondes.")
                .withStyle(ChatFormatting.GRAY));
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("abyssal_soup_tp")
                .then(Commands.argument("target", EntityArgument.player())
                        .executes(context -> teleportToChosenPlayer(
                                context.getSource().getPlayerOrException(),
                                EntityArgument.getPlayer(context, "target")))));
    }

    private static int teleportToChosenPlayer(ServerPlayer player, ServerPlayer target) throws CommandSyntaxException {
        Long expiry = PENDING_TELEPORTS.get(player.getUUID());
        long now = player.serverLevel().getGameTime();

        if (expiry == null || expiry < now) {
            PENDING_TELEPORTS.remove(player.getUUID());
            player.sendSystemMessage(Component.literal("La soupe des abysses ne répond plus.")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        if (target.getUUID().equals(player.getUUID())) {
            player.sendSystemMessage(Component.literal("La soupe refuse de viser son propre buveur.")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        PENDING_TELEPORTS.remove(player.getUUID());
        player.stopRiding();
        player.teleportTo(
                target.serverLevel(),
                target.getX(),
                target.getY(),
                target.getZ(),
                target.getYRot(),
                target.getXRot());
        bindPlayersThroughSoup(player, target);
        player.sendSystemMessage(Component.literal("La soupe des abysses t'a attiré vers " + target.getGameProfile().getName() + ".")
                .withStyle(ChatFormatting.DARK_AQUA));
        target.sendSystemMessage(Component.literal(player.getGameProfile().getName() + " a traversé les abysses jusqu'à toi.")
                .withStyle(ChatFormatting.DARK_AQUA));
        return 1;
    }

    private static void bindPlayersThroughSoup(ServerPlayer player, ServerPlayer target) {
        applyConnectionShock(player);
        applyConnectionShock(target);

        long regenerationTick = player.serverLevel().getGameTime() + CONNECTION_DELAY_TICKS;
        PENDING_REGENERATION.put(player.getUUID(), regenerationTick);
        PENDING_REGENERATION.put(target.getUUID(), regenerationTick);
    }

    private static void applyConnectionShock(ServerPlayer player) {
        player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, NAUSEA_DURATION_TICKS, 0, false, true, true));
        player.setHealth(Math.min(ONE_HEART_HEALTH, player.getMaxHealth()));
        player.sendSystemMessage(Component.literal("Le lien abyssal te laisse à un cœur...")
                .withStyle(ChatFormatting.DARK_PURPLE));
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || event.player.level().isClientSide) {
            return;
        }

        if (!(event.player instanceof ServerPlayer player)) {
            return;
        }

        Long regenerationTick = PENDING_REGENERATION.get(player.getUUID());
        if (regenerationTick == null) {
            return;
        }

        if (player.serverLevel().getGameTime() < regenerationTick) {
            if (player.getHealth() > ONE_HEART_HEALTH) {
                player.setHealth(Math.min(ONE_HEART_HEALTH, player.getMaxHealth()));
            }
            return;
        }

        PENDING_REGENERATION.remove(player.getUUID());
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, REGENERATION_DURATION_TICKS, 0, false, true, true));
        player.sendSystemMessage(Component.literal("Le lien abyssal se referme et régénère ton corps.")
                .withStyle(ChatFormatting.AQUA));
    }
}
