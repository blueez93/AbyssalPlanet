package fr.blue.abyssalplanet.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fr.blue.abyssalplanet.AbyssalPlanet;
import fr.blue.abyssalplanet.entity.ZwoingEntity;
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

public class ZwoingModel extends EntityModel<ZwoingEntity> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(AbyssalPlanet.id("zwoing"), "main");

    private final ModelPart body;

    public ZwoingModel(ModelPart root) {
        this.body = root.getChild("body");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild(
                "body",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-4.0F, -4.0F, -4.0F, 8.0F, 8.0F, 8.0F, new CubeDeformation(0.65F)),
                PartPose.offset(0.0F, 20.0F, 0.0F)
        );
        return LayerDefinition.create(mesh, 32, 32);
    }

    @Override
    public void setupAnim(
            ZwoingEntity entity,
            float limbSwing,
            float limbSwingAmount,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        float partialTick = ageInTicks - entity.tickCount;
        float floatingTurn = entity.getFloatTurn(partialTick);
        boolean floating = entity.getMotionState() != ZwoingEntity.MOTION_BOUNCING;

        this.body.yRot = netHeadYaw * Mth.DEG_TO_RAD * 0.15F;
        this.body.xRot = floatingTurn + Mth.sin(ageInTicks * 0.09F) * (floating ? 0.045F : 0.018F);
        this.body.zRot = Mth.sin(ageInTicks * 0.075F + entity.getId()) * (floating ? 0.055F : 0.018F);
        this.body.y = 20.0F + (floating ? Mth.sin(ageInTicks * 0.16F) * 0.22F : 0.0F);
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
