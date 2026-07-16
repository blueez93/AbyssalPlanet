package fr.blue.abyssalplanet.enchantment;

import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.BowItem;
import net.minecraft.world.item.CrossbowItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentCategory;

public class AbyssalVenomEnchantment extends Enchantment {
    public AbyssalVenomEnchantment() {
        super(Rarity.VERY_RARE, EnchantmentCategory.BREAKABLE, new EquipmentSlot[]{
                EquipmentSlot.MAINHAND,
                EquipmentSlot.OFFHAND
        });
    }

    @Override
    public int getMaxLevel() {
        return 3;
    }

    @Override
    public int getMinCost(int level) {
        return 40 + level * 18;
    }

    @Override
    public int getMaxCost(int level) {
        return getMinCost(level) + 40;
    }

    @Override
    public boolean isTreasureOnly() {
        return true;
    }

    @Override
    public boolean isDiscoverable() {
        return false;
    }

    @Override
    public boolean isTradeable() {
        return false;
    }

    @Override
    public boolean isCurse() {
        return true;
    }

    @Override
    public boolean canEnchant(ItemStack stack) {
        return isWeaponLike(stack) || super.canEnchant(stack);
    }

    public static boolean isWeaponLike(ItemStack stack) {
        return stack.getItem() instanceof SwordItem
                || stack.getItem() instanceof AxeItem
                || stack.getItem() instanceof TridentItem
                || stack.getItem() instanceof BowItem
                || stack.getItem() instanceof CrossbowItem
                || stack.getItem() instanceof fr.blue.abyssalplanet.item.AbyssalStaffItem
                || stack.getAttributeModifiers(EquipmentSlot.MAINHAND).containsKey(Attributes.ATTACK_DAMAGE);
    }
}
