package fr.blue.abyssalplanet.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import fr.blue.abyssalplanet.AbyssalPlanet;
import fr.blue.abyssalplanet.entity.AbyssalSerpentEntity;
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
import net.minecraft.world.entity.Entity;

public class AbyssalSerpentModel<T extends Entity> extends EntityModel<T> {
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(AbyssalPlanet.id("abyssal_serpent"), "main");

    private final ModelPart body;
    private final ModelPart[] segments;

    public AbyssalSerpentModel(ModelPart root) {
        this.body = root.getChild("body");
        this.segments = new ModelPart[]{
                this.body.getChild("head"),
                this.body.getChild("neck"),
                this.body.getChild("mid_front"),
                this.body.getChild("mid_back"),
                this.body.getChild("tail_base"),
                this.body.getChild("tail_tip")
        };
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild(
                "body",
                CubeListBuilder.create(),
                PartPose.offset(0.0F, 19.0F, 0.0F)
        );

        body.addOrReplaceChild(
                "head",
                CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-5.0F, -5.0F, -5.0F, 10.0F, 10.0F, 10.0F, new CubeDeformation(0.35F)),
                PartPose.offset(0.0F, 0.0F, -23.0F)
        );
        body.addOrReplaceChild(
                "neck",
                CubeListBuilder.create()
                        .texOffs(0, 20)
                        .addBox(-4.0F, -4.0F, -6.0F, 8.0F, 8.0F, 12.0F, new CubeDeformation(0.2F)),
                PartPose.offset(0.0F, 0.0F, -12.0F)
        );
        body.addOrReplaceChild(
                "mid_front",
                CubeListBuilder.create()
                        .texOffs(28, 20)
                        .addBox(-3.6F, -3.6F, -6.0F, 7.2F, 7.2F, 12.0F, new CubeDeformation(0.1F)),
                PartPose.offset(0.0F, 0.0F, 0.0F)
        );
        body.addOrReplaceChild(
                "mid_back",
                CubeListBuilder.create()
                        .texOffs(0, 40)
                        .addBox(-3.2F, -3.2F, -6.0F, 6.4F, 6.4F, 12.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, 0.0F, 12.0F)
        );
        body.addOrReplaceChild(
                "tail_base",
                CubeListBuilder.create()
                        .texOffs(28, 40)
                        .addBox(-2.6F, -2.6F, -6.0F, 5.2F, 5.2F, 12.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, 0.0F, 24.0F)
        );
        body.addOrReplaceChild(
                "tail_tip",
                CubeListBuilder.create()
                        .texOffs(48, 0)
                        .addBox(-1.8F, -1.8F, -6.5F, 3.6F, 3.6F, 13.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, 0.0F, 36.5F)
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
        if (entity instanceof AbyssalSerpentEntity serpent && serpent.isStunned()) {
            this.body.yRot = Mth.sin(ageInTicks * 1.15F) * 0.018F;
            this.body.xRot = 0.08F;
            this.body.zRot = Mth.sin(ageInTicks * 1.7F) * 0.022F;
            this.body.y = 19.0F;

            for (int index = 0; index < this.segments.length; index++) {
                this.segments[index].yRot = Mth.sin(index * 1.12F) * (0.035F + index * 0.012F);
                this.segments[index].xRot = -0.025F + index * 0.006F;
                this.segments[index].zRot = Mth.sin(ageInTicks * 1.9F + index * 0.8F) * 0.012F;
            }
            return;
        }

        float speed = (float) Math.min(1.0D, entity.getDeltaMovement().horizontalDistance() * 3.0D);
        float wave = ageInTicks * (0.18F + speed * 0.28F);
        float amplitude = 0.10F + speed * 0.24F;

        this.body.yRot = Mth.sin(ageInTicks * 0.08F) * 0.035F;
        this.body.xRot = headPitch * Mth.DEG_TO_RAD * 0.05F;
        this.body.zRot = 0.0F;
        this.body.y = 19.0F + Mth.sin(ageInTicks * 0.16F) * 0.35F;

        for (int index = 0; index < this.segments.length; index++) {
            float segmentPhase = wave - index * 0.72F;
            float segmentAmplitude = amplitude * (0.55F + index * 0.12F);
            this.segments[index].yRot = Mth.sin(segmentPhase) * segmentAmplitude;
            this.segments[index].xRot = Mth.sin(segmentPhase + 1.45F) * (0.025F + speed * 0.035F);
            this.segments[index].zRot = Mth.sin(segmentPhase + 0.8F) * (0.018F + speed * 0.025F);
        }
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
