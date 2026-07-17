package fr.blue.abyssalplanet.client;

import fr.blue.abyssalplanet.AbyssalPlanet;
import fr.blue.abyssalplanet.entity.AbyssalWandererEntity;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;

public final class AbyssalWandererModel extends PlayerModel<AbyssalWandererEntity> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(AbyssalPlanet.id("abyssal_wanderer"), "main");

    public AbyssalWandererModel(ModelPart root) {
        super(root, false);
    }

    public static LayerDefinition createBodyLayer() {
        return LayerDefinition.create(PlayerModel.createMesh(CubeDeformation.NONE, false), 64, 64);
    }

    @Override
    public void setupAnim(
            AbyssalWandererEntity entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        this.rightArmPose = HumanoidModel.ArmPose.ITEM;
        this.leftArmPose = HumanoidModel.ArmPose.ITEM;
        super.setupAnim(entity, limbSwing, limbSwingAmount, ageInTicks, netHeadYaw, headPitch);

        float movement = Mth.clamp(limbSwingAmount, 0.0F, 1.0F);
        float breathing = Mth.sin(ageInTicks * 0.075F + entity.getId() * 0.21F);
        float headScan = Mth.sin(ageInTicks * 0.018F + entity.getId() * 0.23F) * 0.24F;
        this.body.xRot += 0.09F + movement * 0.035F;
        this.body.y += breathing * 0.075F;
        this.head.xRot = 0.0F;
        this.head.yRot = headScan;
        this.head.zRot = 0.0F;
        this.hat.copyFrom(this.head);

        if (entity.isAggressive()) {
            if (this.attackTime < 0.04F) {
                float readiness = Mth.sin(ageInTicks * 0.16F) * 0.055F;
                this.rightArm.xRot = -0.96F + readiness;
                this.leftArm.xRot = -0.96F - readiness;
                this.rightArm.yRot = -0.29F;
                this.leftArm.yRot = 0.29F;
                this.rightArm.zRot = 0.08F;
                this.leftArm.zRot = -0.08F;
            } else {
                HumanoidArm attackingArm = entity.swingingArm == InteractionHand.OFF_HAND
                        ? entity.getMainArm().getOpposite()
                        : entity.getMainArm();
                ModelPart guardArm = this.getArm(attackingArm.getOpposite());
                guardArm.xRot = -1.02F + breathing * 0.04F;
                guardArm.yRot = attackingArm == HumanoidArm.RIGHT ? 0.30F : -0.30F;
                guardArm.zRot = attackingArm == HumanoidArm.RIGHT ? -0.08F : 0.08F;
            }
        } else {
            this.rightArm.xRot = -0.28F + breathing * 0.025F;
            this.leftArm.xRot = -0.28F - breathing * 0.025F;
            this.rightArm.yRot = -0.10F;
            this.leftArm.yRot = 0.10F;
            this.rightArm.zRot = 0.055F;
            this.leftArm.zRot = -0.055F;
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
