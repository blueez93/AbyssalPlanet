package fr.blue.abyssalplanet.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fr.blue.abyssalplanet.AbyssalPlanet;
import fr.blue.abyssalplanet.entity.AbyssalViperEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

public final class AbyssalViperModel extends EntityModel<AbyssalViperEntity> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(AbyssalPlanet.id("abyssal_viper"), "main");

    private static final float ROOT_Y = 19.0F;
    private static final float HEAD_Z = -18.0F;

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart jaw;
    private final ModelPart neck;
    private final ModelPart body;
    private final ModelPart tailOne;
    private final ModelPart tailTwo;
    private final ModelPart tailThree;
    private final ModelPart tailTip;
    private final ModelPart leftFin;
    private final ModelPart rightFin;
    private final ModelPart dorsalFin;
    private final ModelPart tailFin;

    public AbyssalViperModel(ModelPart bakedRoot) {
        this.root = bakedRoot.getChild("root");
        this.head = root.getChild("head");
        this.jaw = head.getChild("jaw");
        this.neck = root.getChild("neck");
        this.body = root.getChild("body");
        this.leftFin = body.getChild("left_fin");
        this.rightFin = body.getChild("right_fin");
        this.dorsalFin = body.getChild("dorsal_fin");
        this.tailOne = body.getChild("tail_one");
        this.tailTwo = tailOne.getChild("tail_two");
        this.tailThree = tailTwo.getChild("tail_three");
        this.tailTip = tailThree.getChild("tail_tip");
        this.tailFin = tailTip.getChild("tail_fin");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition bakedRoot = mesh.getRoot();
        PartDefinition root = bakedRoot.addOrReplaceChild(
                "root",
                CubeListBuilder.create(),
                PartPose.offset(0.0F, ROOT_Y, 0.0F)
        );

        PartDefinition head = root.addOrReplaceChild(
                "head",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-5.5F, -4.2F, -7.0F, 11.0F, 8.0F, 10.0F, new CubeDeformation(0.25F))
                        .texOffs(42, 0)
                        .addBox(-4.1F, -2.7F, -10.0F, 8.2F, 5.2F, 4.0F, new CubeDeformation(0.12F)),
                PartPose.offset(0.0F, 0.0F, HEAD_Z)
        );
        head.addOrReplaceChild(
                "left_eye",
                CubeListBuilder.create()
                        .texOffs(112, 0)
                        .addBox(-0.5F, -0.75F, -0.45F, 1.2F, 1.5F, 0.9F, new CubeDeformation(0.08F)),
                PartPose.offset(4.15F, -1.85F, -6.95F)
        );
        head.addOrReplaceChild(
                "right_eye",
                CubeListBuilder.create()
                        .texOffs(112, 0)
                        .mirror()
                        .addBox(-0.7F, -0.75F, -0.45F, 1.2F, 1.5F, 0.9F, new CubeDeformation(0.08F)),
                PartPose.offset(-4.15F, -1.85F, -6.95F)
        );

        PartDefinition jaw = head.addOrReplaceChild(
                "jaw",
                CubeListBuilder.create()
                        .texOffs(70, 0)
                        .addBox(-4.0F, -0.45F, -4.6F, 8.0F, 2.0F, 5.2F, new CubeDeformation(0.08F)),
                PartPose.offset(0.0F, 2.35F, -5.5F)
        );
        jaw.addOrReplaceChild(
                "fangs",
                CubeListBuilder.create()
                        .texOffs(120, 0)
                        .addBox(2.45F, 0.0F, -3.9F, 0.8F, 2.6F, 0.8F)
                        .texOffs(120, 0)
                        .mirror()
                        .addBox(-3.25F, 0.0F, -3.9F, 0.8F, 2.6F, 0.8F),
                PartPose.ZERO
        );

        root.addOrReplaceChild(
                "neck",
                CubeListBuilder.create()
                        .texOffs(0, 22)
                        .addBox(-4.0F, -3.75F, -7.0F, 8.0F, 7.5F, 14.0F, new CubeDeformation(0.15F)),
                PartPose.offset(0.0F, 0.0F, -9.0F)
        );

        PartDefinition body = root.addOrReplaceChild(
                "body",
                CubeListBuilder.create()
                        .texOffs(44, 20)
                        .addBox(-4.5F, -4.25F, -8.0F, 9.0F, 8.5F, 16.0F, new CubeDeformation(0.12F)),
                PartPose.offset(0.0F, 0.0F, 3.0F)
        );
        body.addOrReplaceChild(
                "left_fin",
                CubeListBuilder.create()
                        .texOffs(92, 14)
                        .addBox(0.0F, -0.4F, -3.5F, 7.0F, 0.8F, 8.0F, new CubeDeformation(0.05F)),
                PartPose.offsetAndRotation(4.2F, 0.3F, -1.0F, 0.0F, 0.12F, -0.32F)
        );
        body.addOrReplaceChild(
                "right_fin",
                CubeListBuilder.create()
                        .texOffs(92, 14)
                        .mirror()
                        .addBox(-7.0F, -0.4F, -3.5F, 7.0F, 0.8F, 8.0F, new CubeDeformation(0.05F)),
                PartPose.offsetAndRotation(-4.2F, 0.3F, -1.0F, 0.0F, -0.12F, 0.32F)
        );
        body.addOrReplaceChild(
                "dorsal_fin",
                CubeListBuilder.create()
                        .texOffs(98, 26)
                        .addBox(-0.55F, -5.5F, -4.0F, 1.1F, 5.5F, 9.0F, new CubeDeformation(0.04F)),
                PartPose.offset(0.0F, -3.85F, 0.5F)
        );

        PartDefinition tailOne = body.addOrReplaceChild(
                "tail_one",
                CubeListBuilder.create()
                        .texOffs(0, 44)
                        .addBox(-4.0F, -3.7F, 0.0F, 8.0F, 7.4F, 14.0F, new CubeDeformation(0.08F)),
                PartPose.offset(0.0F, 0.0F, 7.5F)
        );
        PartDefinition tailTwo = tailOne.addOrReplaceChild(
                "tail_two",
                CubeListBuilder.create()
                        .texOffs(44, 46)
                        .addBox(-3.4F, -3.1F, 0.0F, 6.8F, 6.2F, 13.0F, new CubeDeformation(0.05F)),
                PartPose.offset(0.0F, 0.0F, 13.0F)
        );
        PartDefinition tailThree = tailTwo.addOrReplaceChild(
                "tail_three",
                CubeListBuilder.create()
                        .texOffs(74, 46)
                        .addBox(-2.7F, -2.5F, 0.0F, 5.4F, 5.0F, 11.0F),
                PartPose.offset(0.0F, 0.0F, 12.0F)
        );
        PartDefinition tailTip = tailThree.addOrReplaceChild(
                "tail_tip",
                CubeListBuilder.create()
                        .texOffs(102, 44)
                        .addBox(-1.8F, -1.8F, 0.0F, 3.6F, 3.6F, 10.0F),
                PartPose.offset(0.0F, 0.0F, 10.0F)
        );
        tailTip.addOrReplaceChild(
                "tail_fin",
                CubeListBuilder.create()
                        .texOffs(94, 0)
                        .addBox(-0.5F, -5.0F, -1.0F, 1.0F, 10.0F, 8.0F, new CubeDeformation(0.04F)),
                PartPose.offset(0.0F, 0.0F, 8.0F)
        );

        return LayerDefinition.create(mesh, 128, 64);
    }

    @Override
    public void setupAnim(
            AbyssalViperEntity entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        float movement = (float) Math.min(1.0D, entity.getDeltaMovement().length() * 3.2D);
        float phase = ageInTicks * (0.16F + movement * 0.22F);
        float amplitude = 0.055F + movement * 0.115F;
        float partialTick = Mth.clamp(ageInTicks - entity.tickCount, 0.0F, 1.0F);
        float bite = Mth.sin(entity.getAttackAnim(partialTick) * Mth.PI);

        root.y = ROOT_Y + Mth.sin(ageInTicks * 0.12F) * 0.32F;
        root.xRot = headPitch * Mth.DEG_TO_RAD * 0.035F;
        root.yRot = Mth.sin(ageInTicks * 0.075F) * 0.025F;
        root.zRot = Mth.sin(ageInTicks * 0.095F) * 0.018F;

        neck.yRot = Mth.sin(phase + 0.7F) * amplitude * 0.55F;
        neck.xRot = Mth.sin(phase * 0.72F + 1.1F) * (0.012F + movement * 0.018F);
        body.yRot = Mth.sin(phase) * amplitude * 0.38F;
        body.xRot = Mth.sin(phase * 0.68F + 1.5F) * (0.01F + movement * 0.014F);
        tailOne.yRot = Mth.sin(phase - 0.62F) * amplitude * 0.72F;
        tailTwo.yRot = Mth.sin(phase - 1.26F) * amplitude * 0.88F;
        tailThree.yRot = Mth.sin(phase - 1.92F) * amplitude * 1.05F;
        tailTip.yRot = Mth.sin(phase - 2.58F) * amplitude * 1.22F;
        tailOne.xRot = Mth.sin(phase * 0.62F - 0.3F) * 0.018F;
        tailTwo.xRot = Mth.sin(phase * 0.62F - 0.9F) * 0.024F;
        tailThree.xRot = Mth.sin(phase * 0.62F - 1.5F) * 0.03F;
        tailTip.xRot = Mth.sin(phase * 0.62F - 2.1F) * 0.038F;

        head.z = HEAD_Z - bite * 1.15F;
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.14F + Mth.sin(phase + 1.05F) * amplitude * 0.36F;
        head.xRot = headPitch * Mth.DEG_TO_RAD * 0.14F
                + Mth.sin(phase * 0.7F + 1.8F) * 0.025F
                - bite * 0.13F;
        jaw.xRot = 0.06F + Mth.sin(ageInTicks * 0.10F) * 0.018F + bite * 0.72F;

        float finPulse = Mth.sin(phase * 1.35F) * (0.06F + movement * 0.12F);
        leftFin.zRot = -0.32F - finPulse;
        rightFin.zRot = 0.32F + finPulse;
        dorsalFin.xRot = Mth.sin(phase * 0.8F + 0.5F) * 0.025F;
        tailFin.yRot = Mth.sin(phase - 3.0F) * amplitude * 1.45F;
    }

    @Override
    public void renderToBuffer(
            PoseStack poseStack,
            VertexConsumer vertexConsumer,
            int packedLight,
            int packedOverlay,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        root.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
