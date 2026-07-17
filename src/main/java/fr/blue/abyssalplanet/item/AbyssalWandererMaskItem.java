package fr.blue.abyssalplanet.item;

import fr.blue.abyssalplanet.AbyssalPlanet;
import fr.blue.abyssalplanet.client.AbyssalWandererMaskModel;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.extensions.common.IClientItemExtensions;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

public final class AbyssalWandererMaskItem extends ArmorItem {
    public AbyssalWandererMaskItem(ArmorMaterial material, Properties properties) {
        super(material, Type.HELMET, properties);
    }

    @Override
    public void initializeClient(Consumer<IClientItemExtensions> consumer) {
        consumer.accept(new IClientItemExtensions() {
            private HumanoidModel<?> maskModel;

            @Override
            public HumanoidModel<?> getHumanoidArmorModel(
                    LivingEntity livingEntity,
                    ItemStack itemStack,
                    EquipmentSlot equipmentSlot,
                    HumanoidModel<?> original
            ) {
                if (this.maskModel == null) {
                    this.maskModel = new AbyssalWandererMaskModel(
                            Minecraft.getInstance().getEntityModels().bakeLayer(
                                    AbyssalWandererMaskModel.LAYER_LOCATION
                            )
                    );
                }
                copyModelProperties(original, this.maskModel);
                return this.maskModel;
            }

            @SuppressWarnings({"rawtypes", "unchecked"})
            private void copyModelProperties(HumanoidModel<?> source, HumanoidModel<?> destination) {
                ((HumanoidModel) source).copyPropertiesTo((HumanoidModel) destination);
            }
        });
    }

    @Override
    public String getArmorTexture(
            ItemStack stack,
            Entity entity,
            EquipmentSlot slot,
            @Nullable String type
    ) {
        return AbyssalPlanet.MOD_ID + ":textures/models/armor/abyssal_wanderer_mask.png";
    }
}
