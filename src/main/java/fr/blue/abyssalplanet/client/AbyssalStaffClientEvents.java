package fr.blue.abyssalplanet.client;

import fr.blue.abyssalplanet.AbyssalPlanet;
import fr.blue.abyssalplanet.item.AbyssalStaffItem;
import fr.blue.abyssalplanet.network.ModNetwork;
import fr.blue.abyssalplanet.network.GeorgesSeniorBitePacket;
import fr.blue.abyssalplanet.network.OpenAbyssalPortalPacket;
import fr.blue.abyssalplanet.network.PlaceAbyssalPouchGhostPacket;
import fr.blue.abyssalplanet.network.SecondaryDaggerStrikePacket;
import fr.blue.abyssalplanet.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@Mod.EventBusSubscriber(modid = AbyssalPlanet.MOD_ID, value = Dist.CLIENT)
public class AbyssalStaffClientEvents {
    private AbyssalStaffClientEvents() {
    }

    @SubscribeEvent
    public static void onMouseButton(InputEvent.MouseButton.Pre event) {
        if (event.getButton() != GLFW.GLFW_MOUSE_BUTTON_MIDDLE || event.getAction() != GLFW.GLFW_PRESS) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        if (player == null || minecraft.screen != null) {
            return;
        }

        if (player.getVehicle() instanceof fr.blue.abyssalplanet.entity.GeorgesSeniorEntity) {
            ModNetwork.CHANNEL.sendToServer(new GeorgesSeniorBitePacket());
            event.setCanceled(true);
            return;
        }

        if (player.getMainHandItem().is(ModItems.ABYSSAL_POUCH.get())
                || player.getOffhandItem().is(ModItems.ABYSSAL_POUCH.get())) {
            ModNetwork.CHANNEL.sendToServer(new PlaceAbyssalPouchGhostPacket());
            event.setCanceled(true);
            return;
        }

        if (player.getMainHandItem().is(ModItems.ABYSSAL_STAFF.get())) {
            ModNetwork.CHANNEL.sendToServer(new OpenAbyssalPortalPacket());
            event.setCanceled(true);
            return;
        }

        if (player.getOffhandItem().is(ModItems.GOLDEN_BLUE_DAGGER.get())) {
            ModNetwork.CHANNEL.sendToServer(new SecondaryDaggerStrikePacket());
            event.setCanceled(true);
            return;
        }

        if (AbyssalStaffItem.isHoldingStaff(player)) {
            ModNetwork.CHANNEL.sendToServer(new OpenAbyssalPortalPacket());
            event.setCanceled(true);
        }
    }
}
