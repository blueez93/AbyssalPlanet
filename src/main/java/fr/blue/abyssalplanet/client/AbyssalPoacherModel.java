package fr.blue.abyssalplanet.client;

import fr.blue.abyssalplanet.AbyssalPlanet;
import fr.blue.abyssalplanet.entity.AbyssalPoacherEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

public final class AbyssalPoacherModel extends PlayerModel<AbyssalPoacherEntity> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(AbyssalPlanet.id("abyssal_poacher"), "main");

    public AbyssalPoacherModel(ModelPart root) {
        super(root, false);
    }

    public static LayerDefinition createBodyLayer() {
        return LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE, false), 64, 64);
    }

    @Override
    public void setupAnim(
            AbyssalPoacherEntity entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        this.rightArmPose = entity.getMainHandItem().isEmpty()
                ? HumanoidModel.ArmPose.EMPTY
                : HumanoidModel.ArmPose.ITEM;
        this.leftArmPose = entity.getOffhandItem().isEmpty()
                ? HumanoidModel.ArmPose.EMPTY
                : HumanoidModel.ArmPose.ITEM;
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        float movement = Mth.clamp(limbSwingAmount, 0.0F, 1.0F);
        float breathing = Mth.sin(ageInTicks * 0.085F + entity.getId() * 0.37F);
        float headScan = Mth.sin(ageInTicks * 0.020F + entity.getId() * 0.31F) * 0.28F;
        this.body.xRot += 0.055F + movement * 0.035F;
        this.body.y += breathing * 0.045F;
        this.head.xRot = 0.0F;
        this.head.yRot = headScan;
        this.head.zRot = 0.0F;
        this.hat.copyFrom(this.head);

        if (entity.isAggressive() && this.attackTime < 0.04F) {
            HumanoidArm weaponArm = entity.getMainArm();
            ModelPart weapon = this.getArm(weaponArm);
            ModelPart balance = this.getArm(weaponArm.getOpposite());
            float readiness = 0.08F * Mth.sin(ageInTicks * 0.20F);

            weapon.xRot = -0.78F + readiness;
            weapon.yRot = weaponArm == HumanoidArm.RIGHT ? -0.18F : 0.18F;
            weapon.zRot = weaponArm == HumanoidArm.RIGHT ? 0.07F : -0.07F;
            balance.xRot = -0.24F - movement * 0.18F;
            balance.yRot = weaponArm == HumanoidArm.RIGHT ? 0.16F : -0.16F;
        } else if (!entity.isAggressive()) {
            this.rightArm.zRot += 0.025F + breathing * 0.012F;
            this.leftArm.zRot -= 0.025F + breathing * 0.012F;
        }

        copyOuterLayers();
    }

    private void copyOuterLayers() {
        this.leftPants.copyFrom(this.leftLeg);
        this.rightPants.copyFrom(this.rightLeg);
        this.leftSleeve.copyFrom(this.leftArm);
        this.rightSleeve.copyFrom(this.rightArm);
        this.jacket.copyFrom(this.body);
    }
}
