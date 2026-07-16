package fr.blue.abyssalplanet.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fr.blue.abyssalplanet.AbyssalPlanet;
import fr.blue.abyssalplanet.entity.AbyssalBlueMoonfishEntity;
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

public class AbyssalBlueMoonfishModel<T extends AbyssalBlueMoonfishEntity> extends EntityModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(AbyssalPlanet.id("abyssal_blue_moonfish"), "main");

    private final ModelPart body;
    private final ModelPart tail;
    private final ModelPart leftFin;
    private final ModelPart rightFin;
    private final ModelPart topFin;
    private final ModelPart bottomFin;

    public AbyssalBlueMoonfishModel(ModelPart root) {
        this.body = root.getChild("body");
        this.tail = this.body.getChild("tail");
        this.leftFin = this.body.getChild("left_fin");
        this.rightFin = this.body.getChild("right_fin");
        this.topFin = this.body.getChild("top_fin");
        this.bottomFin = this.body.getChild("bottom_fin");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild(
                "body",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-5.0F, -6.5F, -4.5F, 10.0F, 13.0F, 8.0F, new CubeDeformation(0.25F))
                        .texOffs(0, 22)
                        .addBox(-3.8F, -4.4F, -7.0F, 7.6F, 9.0F, 3.0F, new CubeDeformation(0.15F))
                        .texOffs(34, 0)
                        .addBox(-3.3F, -7.8F, -2.5F, 6.6F, 2.2F, 5.0F, CubeDeformation.NONE)
                        .texOffs(34, 8)
                        .addBox(-3.0F, 5.6F, -2.0F, 6.0F, 2.4F, 4.5F, CubeDeformation.NONE),
                PartPose.offset(0.0F, 18.0F, 0.0F)
        );

        body.addOrReplaceChild(
                "tail",
                CubeListBuilder.create()
                        .texOffs(26, 22)
                        .addBox(-0.7F, -5.5F, -0.2F, 1.4F, 11.0F, 5.8F, CubeDeformation.NONE)
                        .texOffs(40, 24)
                        .addBox(-1.1F, -3.2F, 4.5F, 2.2F, 6.4F, 3.8F, CubeDeformation.NONE),
                PartPose.offset(0.0F, 0.0F, 3.0F)
        );

        body.addOrReplaceChild(
                "left_fin",
                CubeListBuilder.create()
                        .texOffs(0, 40)
                        .addBox(0.0F, -2.0F, -1.8F, 5.5F, 4.0F, 1.2F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(4.7F, 0.6F, -0.8F, 0.0F, -0.25F, 0.3F)
        );
        body.addOrReplaceChild(
                "right_fin",
                CubeListBuilder.create()
                        .texOffs(14, 40)
                        .addBox(-5.5F, -2.0F, -1.8F, 5.5F, 4.0F, 1.2F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-4.7F, 0.6F, -0.8F, 0.0F, 0.25F, -0.3F)
        );
        body.addOrReplaceChild(
                "top_fin",
                CubeListBuilder.create()
                        .texOffs(28, 40)
                        .addBox(-1.0F, -5.5F, -2.8F, 2.0F, 5.5F, 5.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, -6.4F, -0.1F)
        );
        body.addOrReplaceChild(
                "bottom_fin",
                CubeListBuilder.create()
                        .texOffs(42, 40)
                        .addBox(-0.9F, 0.0F, -2.5F, 1.8F, 5.0F, 4.5F, CubeDeformation.NONE),
                PartPose.offset(0.0F, 6.5F, 0.0F)
        );
        body.addOrReplaceChild(
                "left_eye",
                CubeListBuilder.create()
                        .texOffs(56, 52)
                        .addBox(-2.65F, -2.35F, -7.32F, 1.25F, 1.25F, 0.16F, CubeDeformation.NONE)
                        .texOffs(60, 52)
                        .addBox(-2.18F, -1.95F, -7.44F, 0.42F, 0.42F, 0.08F, CubeDeformation.NONE),
                PartPose.ZERO
        );
        body.addOrReplaceChild(
                "right_eye",
                CubeListBuilder.create()
                        .texOffs(56, 52)
                        .addBox(1.4F, -2.35F, -7.32F, 1.25F, 1.25F, 0.16F, CubeDeformation.NONE)
                        .texOffs(60, 52)
                        .addBox(1.76F, -1.95F, -7.44F, 0.42F, 0.42F, 0.08F, CubeDeformation.NONE),
                PartPose.ZERO
        );

        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(
            T entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        float swim = Mth.sin(ageInTicks * 0.55F) * 0.32F;
        this.tail.yRot = swim;
        this.body.yRot = Mth.sin(ageInTicks * 0.16F) * 0.05F;
        this.body.zRot = Mth.sin(ageInTicks * 0.22F) * 0.035F;
        this.body.y = 18.0F + Mth.sin(ageInTicks * 0.18F) * 0.22F;
        this.leftFin.zRot = 0.3F + Mth.sin(ageInTicks * 0.4F) * 0.12F;
        this.rightFin.zRot = -0.3F - Mth.sin(ageInTicks * 0.4F) * 0.12F;
        this.topFin.xRot = Mth.sin(ageInTicks * 0.28F) * 0.04F;
        this.bottomFin.xRot = -this.topFin.xRot;
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
        this.body.render(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue, alpha);
    }
}
