package fr.blue.abyssalplanet.item;

import fr.blue.abyssalplanet.AbyssalPlanet;
import fr.blue.abyssalplanet.registry.ModItems;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.LazyLoadedValue;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.crafting.Ingredient;

public enum ModArmorMaterials implements ArmorMaterial {
    ABYSSAL_WANDERER_MASK;

    private final LazyLoadedValue<Ingredient> repairIngredient =
            new LazyLoadedValue<>(() -> Ingredient.of(ModItems.ABYSSAL_SCALE.get()));

    @Override
    public int getDurabilityForType(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> 462;
            case CHESTPLATE -> 672;
            case LEGGINGS -> 630;
            case BOOTS -> 546;
        };
    }

    @Override
    public int getDefenseForType(ArmorItem.Type type) {
        return switch (type) {
            case HELMET, BOOTS -> 4;
            case LEGGINGS -> 7;
            case CHESTPLATE -> 9;
        };
    }

    @Override
    public int getEnchantmentValue() {
        return 17;
    }

    @Override
    public SoundEvent getEquipSound() {
        return SoundEvents.ARMOR_EQUIP_NETHERITE;
    }

    @Override
    public Ingredient getRepairIngredient() {
        return this.repairIngredient.get();
    }

    @Override
    public String getName() {
        return AbyssalPlanet.MOD_ID + ":abyssal_wanderer_mask";
    }

    @Override
    public float getToughness() {
        return 3.5F;
    }

    @Override
    public float getKnockbackResistance() {
        return 0.12F;
    }
}
