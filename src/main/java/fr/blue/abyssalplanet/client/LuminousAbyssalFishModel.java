package fr.blue.abyssalplanet.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fr.blue.abyssalplanet.AbyssalPlanet;
import fr.blue.abyssalplanet.entity.LuminousAbyssalFishEntity;
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

public final class LuminousAbyssalFishModel extends EntityModel<LuminousAbyssalFishEntity> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(AbyssalPlanet.id("luminous_abyssal_fish"), "main");

    private static final float ROOT_Y = 20.0F;

    private final ModelPart root;
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart mouth;
    private final ModelPart tailBase;
    private final ModelPart tailFin;
    private final ModelPart leftFin;
    private final ModelPart rightFin;
    private final ModelPart dorsalFin;

    public LuminousAbyssalFishModel(ModelPart bakedRoot) {
        this.root = bakedRoot.getChild("root");
        this.body = root.getChild("body");
        this.head = root.getChild("head");
        this.mouth = head.getChild("mouth");
        this.tailBase = root.getChild("tail_base");
        this.tailFin = tailBase.getChild("tail_fin");
        this.leftFin = body.getChild("left_fin");
        this.rightFin = body.getChild("right_fin");
        this.dorsalFin = body.getChild("dorsal_fin");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition bakedRoot = mesh.getRoot();
        PartDefinition root = bakedRoot.addOrReplaceChild(
                "root",
                CubeListBuilder.create(),
                PartPose.offset(0.0F, ROOT_Y, 0.0F)
        );

        PartDefinition body = root.addOrReplaceChild(
                "body",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-4.0F, -3.0F, -6.0F, 8.0F, 6.0F, 12.0F, new CubeDeformation(0.18F))
                        .texOffs(0, 18)
                        .addBox(-3.4F, 1.6F, -4.8F, 6.8F, 1.8F, 9.5F, new CubeDeformation(0.08F)),
                PartPose.ZERO
        );
        body.addOrReplaceChild(
                "left_fin",
                CubeListBuilder.create()
                        .texOffs(0, 40)
                        .addBox(0.0F, -0.35F, -1.0F, 5.0F, 0.7F, 5.0F, new CubeDeformation(0.04F)),
                PartPose.offsetAndRotation(3.75F, 0.65F, -0.6F, 0.0F, 0.1F, -0.38F)
        );
        body.addOrReplaceChild(
                "right_fin",
                CubeListBuilder.create()
                        .texOffs(0, 40)
                        .mirror()
                        .addBox(-5.0F, -0.35F, -1.0F, 5.0F, 0.7F, 5.0F, new CubeDeformation(0.04F)),
                PartPose.offsetAndRotation(-3.75F, 0.65F, -0.6F, 0.0F, -0.1F, 0.38F)
        );
        body.addOrReplaceChild(
                "dorsal_fin",
                CubeListBuilder.create()
                        .texOffs(20, 40)
                        .addBox(-0.45F, -4.0F, -2.5F, 0.9F, 4.0F, 6.5F, new CubeDeformation(0.03F)),
                PartPose.offset(0.0F, -2.75F, 0.0F)
        );

        PartDefinition head = root.addOrReplaceChild(
                "head",
                CubeListBuilder.create()
                        .texOffs(0, 24)
                        .addBox(-3.6F, -2.8F, -5.2F, 7.2F, 5.6F, 5.8F, new CubeDeformation(0.2F))
                        .texOffs(26, 24)
                        .addBox(-2.8F, -1.8F, -7.1F, 5.6F, 3.7F, 2.3F, new CubeDeformation(0.1F)),
                PartPose.offset(0.0F, -0.1F, -5.6F)
        );
        head.addOrReplaceChild(
                "left_eye",
                CubeListBuilder.create()
                        .texOffs(56, 0)
                        .addBox(-0.45F, -0.6F, -0.45F, 0.9F, 1.2F, 0.9F, new CubeDeformation(0.08F)),
                PartPose.offset(3.45F, -1.0F, -3.0F)
        );
        head.addOrReplaceChild(
                "right_eye",
                CubeListBuilder.create()
                        .texOffs(56, 0)
                        .mirror()
                        .addBox(-0.45F, -0.6F, -0.45F, 0.9F, 1.2F, 0.9F, new CubeDeformation(0.08F)),
                PartPose.offset(-3.45F, -1.0F, -3.0F)
        );
        head.addOrReplaceChild(
                "mouth",
                CubeListBuilder.create()
                        .texOffs(42, 24)
                        .addBox(-2.1F, -0.4F, -2.0F, 4.2F, 0.9F, 2.2F, new CubeDeformation(0.04F)),
                PartPose.offset(0.0F, 1.45F, -5.35F)
        );

        PartDefinition tailBase = root.addOrReplaceChild(
                "tail_base",
                CubeListBuilder.create()
                        .texOffs(40, 0)
                        .addBox(-2.5F, -2.0F, 0.0F, 5.0F, 4.0F, 5.0F, new CubeDeformation(0.08F)),
                PartPose.offset(0.0F, 0.0F, 5.5F)
        );
        tailBase.addOrReplaceChild(
                "tail_fin",
                CubeListBuilder.create()
                        .texOffs(42, 10)
                        .addBox(-0.45F, -5.0F, -0.5F, 0.9F, 10.0F, 7.5F, new CubeDeformation(0.04F)),
                PartPose.offset(0.0F, 0.0F, 4.2F)
        );

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(
            LuminousAbyssalFishEntity entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        float speed = (float) Math.min(1.0D, entity.getDeltaMovement().length() * 7.0D);
        float individualOffset = (entity.getId() % 17) * 0.47F;
        float phase = ageInTicks * (0.31F + speed * 0.56F) + individualOffset;
        float tailAmplitude = 0.18F + speed * 0.34F;

        root.y = ROOT_Y + Mth.sin(ageInTicks * 0.13F + individualOffset) * 0.22F;
        root.xRot = headPitch * Mth.DEG_TO_RAD * 0.08F;
        root.yRot = Mth.sin(phase * 0.5F) * 0.018F;
        root.zRot = Mth.sin(ageInTicks * 0.09F + individualOffset) * (0.018F + speed * 0.025F);

        body.yRot = Mth.sin(phase + 0.7F) * tailAmplitude * 0.16F;
        body.xRot = Mth.sin(phase * 0.55F + 1.2F) * 0.018F;
        head.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.08F
                - Mth.sin(phase + 0.9F) * tailAmplitude * 0.12F;
        head.xRot = Mth.sin(phase * 0.48F + 1.8F) * 0.018F;
        mouth.xRot = 0.05F + Mth.sin(ageInTicks * 0.16F + individualOffset) * 0.035F;

        tailBase.yRot = Mth.sin(phase) * tailAmplitude;
        tailFin.yRot = Mth.sin(phase - 0.62F) * tailAmplitude * 1.38F;
        tailFin.zRot = Mth.sin(phase * 0.42F + individualOffset) * 0.035F;

        float finBeat = Mth.sin(phase * 1.25F) * (0.07F + speed * 0.13F);
        leftFin.zRot = -0.38F - finBeat;
        rightFin.zRot = 0.38F + finBeat;
        dorsalFin.xRot = Mth.sin(phase * 0.68F + 0.4F) * 0.035F;
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
