package ua.meekeyn.requiemunofficial.client.render.entity.model;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartNames;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.AbstractVillager;
import ua.meekeyn.requiemunofficial.RequiemUnofficial;
import ua.meekeyn.requiemunofficial.common.entity.MorticianEntity;

public class MorticianModel<T extends Entity> extends HierarchicalModel<T> implements HeadedModel {
    public static final ModelLayerLocation MODEL_LAYER = new ModelLayerLocation(
            new ResourceLocation(RequiemUnofficial.MODID, "mortician"),
            "main"
    );
    public static final String SLEEVES = "sleeves";

    private final ModelPart root;
    private final ModelPart head;
    private final ModelPart arms;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;
    private float baseAlpha = 1.0F;

    public MorticianModel(ModelPart root) {
        super(RenderType::entityTranslucent);
        this.root = root;
        this.head = root.getChild(PartNames.HEAD);
        this.arms = root.getChild(PartNames.ARMS);
        this.rightLeg = root.getChild(PartNames.RIGHT_LEG);
        this.leftLeg = root.getChild(PartNames.LEFT_LEG);
        this.rightArm = root.getChild(PartNames.RIGHT_ARM);
        this.leftArm = root.getChild(PartNames.LEFT_ARM);
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild(PartNames.HEAD, CubeListBuilder.create()
                        .mirror()
                        .texOffs(0, 0).addBox(-5.0F, -8.0F, -4.0F, 10.0F, 8.0F, 8.0F, true)
                        .texOffs(31, 1).addBox(-2.0F, -4.0F, -5.0F, 4.0F, 4.0F, 1.0F)
                        .texOffs(2, 0).addBox(-3.0F, -2.0F, -5.0F, 1.0F, 2.0F, 1.0F)
                        .texOffs(2, 4).addBox(2.0F, -2.0F, -5.0F, 1.0F, 2.0F, 1.0F),
                PartPose.ZERO);
        root.addOrReplaceChild(PartNames.BODY, CubeListBuilder.create()
                        .mirror()
                        .texOffs(16, 16).addBox(-4.0F, 0.0F, -3.0F, 8.0F, 12.0F, 6.0F)
                        .texOffs(0, 34).addBox(
                                -4.0F,
                                0.0F,
                                -3.0F,
                                8.0F,
                                18.0F,
                                6.0F,
                                new CubeDeformation(0.5F)
                        ),
                PartPose.ZERO);
        root.addOrReplaceChild(PartNames.RIGHT_LEG, CubeListBuilder.create()
                        .texOffs(0, 18).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, true),
                PartPose.offset(-2.0F, 12.0F, 0.0F));
        root.addOrReplaceChild(PartNames.LEFT_LEG, CubeListBuilder.create()
                        .texOffs(0, 18).addBox(-2.0F, 0.0F, -2.0F, 4.0F, 12.0F, 4.0F, false),
                PartPose.offset(2.0F, 12.0F, 0.0F));

        PartDefinition arms = root.addOrReplaceChild(PartNames.ARMS, CubeListBuilder.create()
                        .texOffs(40, 30).addBox(-4.0F, 2.0F, -2.0F, 8.0F, 4.0F, 4.0F, true)
                        .texOffs(34, 38).addBox(-6.0F, 2.5F, -1.5F, 12.0F, 3.0F, 3.0F, false)
                        .texOffs(44, 18).addBox(4.0F, -2.0F, -2.0F, 3.0F, 8.0F, 4.0F, true)
                        .texOffs(44, 18).addBox(-7.0F, -2.0F, -2.0F, 3.0F, 8.0F, 4.0F, false),
                PartPose.offsetAndRotation(0.0F, 2.0F, 0.0F, -0.9163F, 0.0F, 0.0F));
        arms.addOrReplaceChild(SLEEVES, CubeListBuilder.create()
                        .texOffs(42, 0).addBox(-6.999F, -0.2F, -0.4F, 5.0F, 3.0F, 6.0F, true)
                        .texOffs(42, 0).addBox(1.999F, -0.2F, -0.4F, 5.0F, 3.0F, 6.0F, false),
                PartPose.offsetAndRotation(0.0F, 4.0F, 0.0F, -0.6545F, 0.0F, 0.0F));

        root.addOrReplaceChild(PartNames.RIGHT_ARM, CubeListBuilder.create()
                        .texOffs(28, 43).addBox(-1.0F, -2.0F, -2.0F, 3.0F, 9.0F, 4.0F, true)
                        .texOffs(42, 43).addBox(-0.5F, -1.0F, -1.5F, 2.0F, 11.0F, 3.0F, true),
                PartPose.offset(5.0F, 2.0F, 0.0F));
        root.addOrReplaceChild(PartNames.LEFT_ARM, CubeListBuilder.create()
                        .texOffs(28, 43).addBox(-2.0F, -2.0F, -2.0F, 3.0F, 9.0F, 4.0F, false)
                        .texOffs(42, 43).addBox(-1.5F, -1.0F, -1.5F, 2.0F, 11.0F, 3.0F, false),
                PartPose.offset(-5.0F, 2.0F, 0.0F));

        return LayerDefinition.create(mesh, 64, 64);
    }

    public void setBaseAlpha(float baseAlpha) {
        this.baseAlpha = baseAlpha;
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight,
                               int packedOverlay, float red, float green, float blue, float alpha) {
        super.renderToBuffer(poseStack, vertexConsumer, packedLight, packedOverlay, red, green, blue,
                alpha * this.baseAlpha);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks,
                          float netHeadYaw, float headPitch) {
        boolean rollingHead = entity instanceof AbstractVillager villager && villager.getUnhappyCounter() > 0;

        this.head.yRot = netHeadYaw * Mth.DEG_TO_RAD;
        this.head.xRot = headPitch * Mth.DEG_TO_RAD;

        if (rollingHead) {
            this.head.zRot = 0.3F * Mth.sin(0.45F * ageInTicks);
            this.head.xRot = 0.4F;
        } else {
            this.head.zRot = 0.0F;
        }

        this.rightLeg.xRot = Mth.cos(limbSwing * 0.6662F) * 1.4F * limbSwingAmount * 0.5F;
        this.leftLeg.xRot = Mth.cos(limbSwing * 0.6662F + Mth.PI) * 1.4F * limbSwingAmount * 0.5F;
        this.rightLeg.yRot = 0.0F;
        this.leftLeg.yRot = 0.0F;

        if (entity instanceof MorticianEntity mortician && mortician.isSpellcasting()) {
            this.rightArm.z = 0.0F;
            this.rightArm.x = -5.0F;
            this.leftArm.z = 0.0F;
            this.leftArm.x = 5.0F;
            this.rightArm.xRot = Mth.cos(ageInTicks * 0.6662F) * 0.25F;
            this.leftArm.xRot = Mth.cos(ageInTicks * 0.6662F) * 0.25F;
            this.rightArm.zRot = Mth.PI * 0.75F;
            this.leftArm.zRot = -Mth.PI * 0.75F;
            this.rightArm.yRot = 0.0F;
            this.leftArm.yRot = 0.0F;
            this.arms.visible = false;
            this.rightArm.visible = true;
            this.leftArm.visible = true;
        } else {
            this.arms.visible = true;
            this.rightArm.visible = false;
            this.leftArm.visible = false;
        }
    }

    @Override
    public ModelPart root() {
        return this.root;
    }

    @Override
    public ModelPart getHead() {
        return this.head;
    }
}
