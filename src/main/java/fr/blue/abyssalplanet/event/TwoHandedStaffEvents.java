package fr.blue.abyssalplanet.event;

import fr.blue.abyssalplanet.AbyssalPlanet;
import fr.blue.abyssalplanet.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = AbyssalPlanet.MOD_ID)
public final class TwoHandedStaffEvents {
    private TwoHandedStaffEvents() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)) {
            return;
        }

        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        if (mainHand.is(ModItems.ABYSSAL_STAFF.get()) && !offHand.isEmpty()) {
            moveOffhandToInventory(player, offHand);
            player.displayClientMessage(Component.literal("Le bâton abyssal nécessite les deux mains."), true);
            return;
        }

        if (!offHand.is(ModItems.ABYSSAL_STAFF.get())) {
            return;
        }

        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        if (mainHand.isEmpty()) {
            player.setItemInHand(InteractionHand.MAIN_HAND, offHand);
        } else {
            returnToInventoryOrDrop(player, offHand);
        }
        player.displayClientMessage(Component.literal("Le bâton abyssal se manie à deux mains."), true);
    }

    private static void moveOffhandToInventory(ServerPlayer player, ItemStack offHand) {
        player.setItemInHand(InteractionHand.OFF_HAND, ItemStack.EMPTY);
        returnToInventoryOrDrop(player, offHand);
    }

    private static void returnToInventoryOrDrop(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
}
