package fr.blue.abyssalplanet.event;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import fr.blue.abyssalplanet.AbyssalPlanet;
import fr.blue.abyssalplanet.entity.AbyssalPortalEntity;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AbyssalPlanet.MOD_ID)
public class AbyssalPortalEvents {
    private AbyssalPortalEvents() {
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("abyssal_portal_invite")
                .then(Commands.argument("portal", IntegerArgumentType.integer(1))
                        .then(Commands.argument("target", EntityArgument.player())
                                .executes(context -> invitePlayer(
                                        context.getSource().getPlayerOrException(),
                                        IntegerArgumentType.getInteger(context, "portal"),
                                        EntityArgument.getPlayer(context, "target"))))));

        event.getDispatcher().register(Commands.literal("abyssal_portal_accept")
                .then(Commands.argument("portal", IntegerArgumentType.integer(1))
                        .executes(context -> acceptInvite(
                                context.getSource().getPlayerOrException(),
                                IntegerArgumentType.getInteger(context, "portal")))));

        event.getDispatcher().register(Commands.literal("abyssal_portal_refuse")
                .then(Commands.argument("portal", IntegerArgumentType.integer(1))
                        .executes(context -> refuseInvite(
                                context.getSource().getPlayerOrException(),
                                IntegerArgumentType.getInteger(context, "portal")))));
    }

    public static void showInviteMenu(ServerPlayer owner, AbyssalPortalEntity portal) {
        MutableComponent message = Component.literal("Inviter au portail abyssal : ")
                .withStyle(ChatFormatting.DARK_AQUA);

        int count = 0;
        for (ServerPlayer target : owner.server.getPlayerList().getPlayers()) {
            if (target.getUUID().equals(owner.getUUID())) {
                continue;
            }

            if (count > 0) {
                message.append(Component.literal(" "));
            }

            String targetName = target.getGameProfile().getName();
            message.append(Component.literal("[")
                    .append(Component.literal(targetName)
                            .withStyle(style -> style
                                    .withColor(ChatFormatting.AQUA)
                                    .withClickEvent(new ClickEvent(
                                            ClickEvent.Action.RUN_COMMAND,
                                            "/abyssal_portal_invite " + portal.getPortalId() + " " + targetName))
                                    .withHoverEvent(new HoverEvent(
                                            HoverEvent.Action.SHOW_TEXT,
                                            Component.literal("Inviter " + targetName)))))
                    .append(Component.literal("]")));
            count++;
        }

        if (count == 0) {
            owner.sendSystemMessage(Component.literal("Portail abyssal ouvert, mais aucun autre joueur n'est connecté.")
                    .withStyle(ChatFormatting.GRAY));
            return;
        }

        owner.sendSystemMessage(message);
    }

    public static MutableComponent buildInviteResponseMessage(AbyssalPortalEntity portal, ServerPlayer owner) {
        int portalId = portal.getPortalId();
        return Component.literal("")
                .append(Component.literal("[ACCEPTER]")
                        .withStyle(style -> style
                                .withColor(ChatFormatting.GREEN)
                                .withClickEvent(new ClickEvent(
                                        ClickEvent.Action.RUN_COMMAND,
                                        "/abyssal_portal_accept " + portalId))
                                .withHoverEvent(new HoverEvent(
                                        HoverEvent.Action.SHOW_TEXT,
                                        Component.literal("Rejoindre le portail de " + owner.getGameProfile().getName())))))
                .append(Component.literal(" "))
                .append(Component.literal("[REFUSER]")
                        .withStyle(style -> style
                                .withColor(ChatFormatting.RED)
                                .withClickEvent(new ClickEvent(
                                        ClickEvent.Action.RUN_COMMAND,
                                        "/abyssal_portal_refuse " + portalId))
                                .withHoverEvent(new HoverEvent(
                                        HoverEvent.Action.SHOW_TEXT,
                                        Component.literal("Refuser l'appel abyssal")))));
    }

    private static int invitePlayer(ServerPlayer owner, int portalId, ServerPlayer target) throws CommandSyntaxException {
        AbyssalPortalEntity portal = findPortal(owner.server, portalId);
        if (portal == null) {
            owner.sendSystemMessage(Component.literal("Ce portail abyssal n'existe plus.")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        portal.invite(owner, target);
        return 1;
    }

    private static int acceptInvite(ServerPlayer player, int portalId) throws CommandSyntaxException {
        AbyssalPortalEntity portal = findPortal(player.server, portalId);
        if (portal == null) {
            player.sendSystemMessage(Component.literal("Ce portail abyssal n'existe plus.")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        portal.accept(player);
        return 1;
    }

    private static int refuseInvite(ServerPlayer player, int portalId) throws CommandSyntaxException {
        AbyssalPortalEntity portal = findPortal(player.server, portalId);
        if (portal == null) {
            player.sendSystemMessage(Component.literal("Ce portail abyssal n'existe plus.")
                    .withStyle(ChatFormatting.RED));
            return 0;
        }

        portal.refuse(player);
        return 1;
    }

    private static AbyssalPortalEntity findPortal(MinecraftServer server, int portalId) {
        for (ServerLevel level : server.getAllLevels()) {
            Entity entity = level.getEntity(portalId);
            if (entity instanceof AbyssalPortalEntity portal) {
                return portal;
            }
        }
        return null;
    }
}
