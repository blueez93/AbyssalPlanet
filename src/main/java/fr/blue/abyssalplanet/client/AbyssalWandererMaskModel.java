package fr.blue.abyssalplanet.client;

import fr.blue.abyssalplanet.AbyssalPlanet;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.world.entity.LivingEntity;

public final class AbyssalWandererMaskModel extends HumanoidModel<LivingEntity> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(AbyssalPlanet.id("abyssal_wanderer_mask"), "main");

    public AbyssalWandererMaskModel(ModelPart root) {
        super(root);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = HumanoidModel.createMesh(CubeDeformation.NONE, 0.0F);
        PartDefinition root = mesh.getRoot();
        PartDefinition head = root.getChild("head");

        head.addOrReplaceChild(
                "mask_plate",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-4.5F, -8.5F, -5.15F, 9.0F, 9.0F, 1.2F, new CubeDeformation(0.08F)),
                PartPose.ZERO
        );
        head.addOrReplaceChild(
                "left_scale_patch",
                CubeListBuilder.create()
                        .texOffs(0, 12)
                        .addBox(2.8F, -7.8F, -5.55F, 2.1F, 5.6F, 0.65F, new CubeDeformation(0.04F)),
                PartPose.rotation(0.0F, 0.0F, -0.08F)
        );
        head.addOrReplaceChild(
                "right_serpent_patch",
                CubeListBuilder.create()
                        .texOffs(8, 12)
                        .addBox(-4.9F, -6.7F, -5.52F, 2.2F, 6.1F, 0.65F, new CubeDeformation(0.04F)),
                PartPose.rotation(0.0F, 0.0F, 0.09F)
        );
        head.addOrReplaceChild(
                "left_zwoing_eye",
                CubeListBuilder.create()
                        .texOffs(20, 0)
                        .addBox(-3.65F, -6.65F, -6.35F, 3.1F, 3.1F, 1.3F, new CubeDeformation(0.05F)),
                PartPose.rotation(0.0F, 0.0F, -0.06F)
        );
        head.addOrReplaceChild(
                "right_zwoing_eye",
                CubeListBuilder.create()
                        .texOffs(20, 6)
                        .addBox(0.55F, -6.65F, -6.35F, 3.1F, 3.1F, 1.3F, new CubeDeformation(0.05F)),
                PartPose.rotation(0.0F, 0.0F, 0.06F)
        );
        head.addOrReplaceChild(
                "mouth_seam",
                CubeListBuilder.create()
                        .texOffs(0, 21)
                        .addBox(-2.9F, -2.15F, -5.9F, 5.8F, 0.55F, 0.55F),
                PartPose.rotation(0.0F, 0.0F, 0.03F)
        );
        for (int stitch = 0; stitch < 5; stitch++) {
            float x = -2.4F + stitch * 1.2F;
            head.addOrReplaceChild(
                    "mouth_stitch_" + stitch,
                    CubeListBuilder.create()
                            .texOffs(0, 23)
                            .addBox(x, -2.65F, -6.0F, 0.35F, 1.45F, 0.4F),
                    PartPose.rotation(0.0F, 0.0F, stitch % 2 == 0 ? 0.12F : -0.12F)
            );
        }

        PartDefinition ponytailRoot = head.addOrReplaceChild(
                "tentacle_ponytail_1",
                CubeListBuilder.create()
                        .texOffs(32, 0)
                        .addBox(-1.4F, -1.0F, -0.8F, 2.8F, 5.2F, 2.8F, new CubeDeformation(0.08F)),
                PartPose.offsetAndRotation(0.0F, -5.8F, 4.1F, 0.42F, 0.0F, 0.0F)
        );
        PartDefinition ponytailMiddle = ponytailRoot.addOrReplaceChild(
                "tentacle_ponytail_2",
                CubeListBuilder.create()
                        .texOffs(32, 11)
                        .addBox(-1.15F, 0.0F, -1.15F, 2.3F, 5.1F, 2.3F, new CubeDeformation(0.05F)),
                PartPose.offsetAndRotation(0.0F, 3.7F, 0.65F, 0.3F, 0.0F, 0.14F)
        );
        ponytailMiddle.addOrReplaceChild(
                "tentacle_ponytail_3",
                CubeListBuilder.create()
                        .texOffs(43, 11)
                        .addBox(-0.85F, 0.0F, -0.85F, 1.7F, 4.6F, 1.7F, new CubeDeformation(0.03F)),
                PartPose.offsetAndRotation(0.0F, 4.6F, 0.2F, 0.2F, 0.0F, -0.22F)
        );

        return LayerDefinition.create(mesh, 64, 32);
    }
}
