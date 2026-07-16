package fr.blue.abyssalplanet.item;

import fr.blue.abyssalplanet.enchantment.AbyssalVenomEnchantment;
import fr.blue.abyssalplanet.registry.ModEnchantments;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class AbyssalVenomItem extends Item {
    private final int venomLevel;

    public AbyssalVenomItem(int venomLevel, Properties properties) {
        super(properties);
        this.venomLevel = venomLevel;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack venom = player.getItemInHand(hand);
        InteractionHand weaponHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack weapon = player.getItemInHand(weaponHand);

        if (weapon.isEmpty() || !AbyssalVenomEnchantment.isWeaponLike(weapon)) {
            if (!level.isClientSide) {
                player.sendSystemMessage(Component.literal("Tiens une arme dans l'autre main pour y lier le Venin abyssal.")
                        .withStyle(ChatFormatting.AQUA));
            }
            return InteractionResultHolder.fail(venom);
        }

        int currentLevel = EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.ABYSSAL_VENOM.get(), weapon);
        if (currentLevel > 0) {
            if (!level.isClientSide) {
                player.sendSystemMessage(Component.literal("Cette arme porte déjà le Venin abyssal.")
                        .withStyle(ChatFormatting.RED));
            }
            return InteractionResultHolder.fail(venom);
        }

        if (!level.isClientSide) {
            applyVenom(weapon, this.venomLevel);
            if (!player.getAbilities().instabuild) {
                venom.shrink(1);
            }
            player.awardStat(Stats.ITEM_USED.get(this));
            level.playSound(null, player.blockPosition(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 0.8F, 0.6F);
            player.sendSystemMessage(Component.literal("Le Venin abyssal s'est lié à l'arme.")
                    .withStyle(ChatFormatting.DARK_AQUA));
        }

        player.swing(hand, true);
        return InteractionResultHolder.sidedSuccess(venom, level.isClientSide);
    }

    private static void applyVenom(ItemStack weapon, int level) {
        if (weapon.getItem() instanceof EnchantedBookItem) {
            EnchantedBookItem.addEnchantment(weapon, new EnchantmentInstance(ModEnchantments.ABYSSAL_VENOM.get(), level));
            return;
        }

        Map<net.minecraft.world.item.enchantment.Enchantment, Integer> enchantments = EnchantmentHelper.getEnchantments(weapon);
        enchantments.put(ModEnchantments.ABYSSAL_VENOM.get(), level);
        EnchantmentHelper.setEnchantments(enchantments, weapon);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public Component getName(ItemStack stack) {
        return Component.translatable("item.abyssalplanet.abyssal_venom." + this.venomLevel);
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            @Nullable Level level,
            List<Component> tooltip,
            TooltipFlag flag
    ) {
        tooltip.add(Component.translatable("item.abyssalplanet.abyssal_venom.tooltip")
                .withStyle(ChatFormatting.DARK_AQUA));
        tooltip.add(Component.translatable("item.abyssalplanet.abyssal_venom.execute." + this.venomLevel)
                .withStyle(ChatFormatting.GRAY));
    }
}
