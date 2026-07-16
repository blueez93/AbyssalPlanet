package fr.blue.abyssalplanet.item;

import fr.blue.abyssalplanet.event.AbyssalSoupTeleportEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

public class AbyssalSoupItem extends Item {
    public AbyssalSoupItem(Properties properties) {
        super(properties);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity livingEntity) {
        ItemStack result = super.finishUsingItem(stack, level, livingEntity);

        if (!level.isClientSide && livingEntity instanceof ServerPlayer player) {
            AbyssalSoupTeleportEvents.offerTeleportChoice(player);
        }

        if (livingEntity instanceof Player player && !player.getAbilities().instabuild) {
            ItemStack bowl = new ItemStack(Items.BOWL);
            if (result.isEmpty()) {
                return bowl;
            }
            if (!player.getInventory().add(bowl)) {
                player.drop(bowl, false);
            }
        }

        return result;
    }

    @Override
    public UseAnim getUseAnimation(ItemStack stack) {
        return UseAnim.DRINK;
    }
}
