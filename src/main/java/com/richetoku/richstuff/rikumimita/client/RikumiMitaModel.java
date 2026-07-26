package com.richetoku.richstuff.rikumimita.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.richetoku.richstuff.RichStuff;
import com.richetoku.richstuff.rikumimita.RikumiMitaEntity;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Native RichStuff model generated from the supplied Bedrock model JSON. */
public final class RikumiMitaModel extends EntityModel<RikumiMitaEntity> {
    public static final ModelLayerLocation LAYER_LOCATION = new ModelLayerLocation(
            ResourceLocation.fromNamespaceAndPath(RichStuff.MODID, "rikumi_mita"), "main");
    private final ModelPart root;
    private final ModelPart allHead;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart leftLeg;
    private final ModelPart rightLeg;

    public RikumiMitaModel(ModelPart root) {
        this.root = root.getChild("Root");
        ModelPart allBody = this.root.getChild("AllBody");
        ModelPart upBody = allBody.getChild("UpBody");
        ModelPart upperBody = upBody.getChild("UpperBody");
        ModelPart arms = upperBody.getChild("Arms");
        ModelPart downBody = allBody.getChild("DownBody");
        ModelPart legs = downBody.getChild("Legs");
        this.allHead = upperBody.getChild("AllHead");
        this.leftArm = arms.getChild("LeftArm");
        this.rightArm = arms.getChild("RightArm");
        this.leftLeg = legs.getChild("LeftLeg");
        this.rightLeg = legs.getChild("RightLeg");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition meshRoot = mesh.getRoot();
        PartDefinition p_Root_0 = meshRoot.addOrReplaceChild("Root", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, 23.3F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition p_AllBody_1 = p_Root_0.addOrReplaceChild("AllBody", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, -19.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition p_UpBody_2 = p_AllBody_1.addOrReplaceChild("UpBody", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, 0.7F, 0.0F, 0.0F, 0.0F, 0.0F));
        p_UpBody_2.addOrReplaceChild("cube_2_0", CubeListBuilder.create().texOffs(47, 10).addBox(-2.5F, -5.6F, -2.1F, 5.0F, 1.0F, 4.175F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        p_UpBody_2.addOrReplaceChild("cube_2_1", CubeListBuilder.create().texOffs(62, 8).addBox(-2.4F, -2.2F, -1.375F, 4.8F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -4.725F, -1.1F, 0.72867F, 0.0F, 0.0F));
        p_UpBody_2.addOrReplaceChild("cube_2_2", CubeListBuilder.create().texOffs(62, 8).addBox(-2.45F, -2.05F, -0.725F, 4.875F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -4.725F, 1.1F, -0.80721F, 0.0F, 0.0F));
        p_UpBody_2.addOrReplaceChild("cube_2_3", CubeListBuilder.create().texOffs(62, 13).addBox(-0.825F, -1.27753F, -2.05F, 1.65F, 2.0F, 4.1F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        p_UpBody_2.addOrReplaceChild("cube_2_4", CubeListBuilder.create().texOffs(100, 1).addBox(-1.825F, -2.20253F, -2.325F, 3.65F, 2.125F, 4.65F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        p_UpBody_2.addOrReplaceChild("cube_2_5", CubeListBuilder.create().mirror().texOffs(102, 8).addBox(-2.5F, -0.875F, -2.3F, 2.65F, 2.0F, 4.6F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-1.1F, -1.20253F, 0.0F, 0.0F, 0.0F, 0.15708F));
        p_UpBody_2.addOrReplaceChild("cube_2_6", CubeListBuilder.create().texOffs(102, 8).addBox(-0.15F, -0.875F, -2.3F, 2.65F, 2.0F, 4.6F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(1.1F, -1.20253F, 0.0F, 0.0F, 0.0F, -0.15708F));
        p_UpBody_2.addOrReplaceChild("cube_2_7", CubeListBuilder.create().texOffs(60, 23).addBox(-0.5F, -5.27753F, -2.05F, 1.0F, 4.0F, 4.1F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        p_UpBody_2.addOrReplaceChild("cube_2_8", CubeListBuilder.create().texOffs(0, 48).addBox(-1.3F, -3.7F, -2.025F, 3.0F, 6.0F, 4.05F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-2.0F, -1.9F, 0.0F, 0.0F, 0.0F, 0.2138F));
        p_UpBody_2.addOrReplaceChild("cube_2_9", CubeListBuilder.create().mirror().texOffs(0, 48).addBox(-1.7F, -3.7F, -2.025F, 3.0F, 6.0F, 4.05F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(2.0F, -1.9F, 0.0F, 0.0F, 0.0F, -0.2138F));
        PartDefinition p_UpperBody_3 = p_UpBody_2.addOrReplaceChild("UpperBody", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, -5.2F, 0.0F, 0.0F, 0.0F, 0.0F));
        p_UpperBody_3.addOrReplaceChild("cube_3_0", CubeListBuilder.create().texOffs(45, 29).addBox(-2.525F, -2.925F, -2.175F, 5.05F, 3.0F, 4.275F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        p_UpperBody_3.addOrReplaceChild("cube_3_1", CubeListBuilder.create().texOffs(133, 42).addBox(-0.5F, -0.5F, -0.425F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -0.21768F, -1.675F, -0.18725F, 0.18405F, 0.76807F));
        p_UpperBody_3.addOrReplaceChild("cube_3_2", CubeListBuilder.create().texOffs(44, 0).addBox(-0.86376F, -2.36976F, -2.1125F, 4.025F, 5.0F, 4.125F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-0.00604F, -4.26314F, 0.08638F, 0.0F, 0.0F, 0.26133F));
        p_UpperBody_3.addOrReplaceChild("cube_3_3", CubeListBuilder.create().texOffs(26, 45).addBox(-1.775F, -2.0F, -2.1F, 4.0F, 5.0F, 4.125F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-1.42831F, -4.26228F, 0.05216F, 0.0F, 0.0F, -0.26133F));
        p_UpperBody_3.addOrReplaceChild("cube_3_4", CubeListBuilder.create().mirror().texOffs(15, 137).addBox(-1.75F, -6.84777F, -2.29784F, 3.5F, 1.0F, 4.625F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        p_UpperBody_3.addOrReplaceChild("cube_3_5", CubeListBuilder.create().texOffs(117, 58).addBox(-0.65F, -0.5F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.61027F, -5.8853F, -1.96136F, 0.0F, 0.0F, 1.26536F));
        PartDefinition p_Arms_4 = p_UpperBody_3.addOrReplaceChild("Arms", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, -5.8F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition p_LeftArm_5 = p_Arms_4.addOrReplaceChild("LeftArm", CubeListBuilder.create(),
                PartPose.offsetAndRotation(3.45F, 1.875F, 0.1F, 0.0F, 0.0F, -0.22689F));
        p_LeftArm_5.addOrReplaceChild("cube_5_0", CubeListBuilder.create().texOffs(28, 55).addBox(-1.09098F, -1.02606F, -1.5F, 2.5F, 6.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        p_LeftArm_5.addOrReplaceChild("cube_5_1", CubeListBuilder.create().texOffs(34, 117).addBox(-1.0F, -2.1F, -1.75F, 2.5F, 4.0F, 3.5F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.65902F, 0.97394F, 0.0F, 0.0F, 0.0F, -0.17453F));
        PartDefinition p_LeftForeArm_6 = p_LeftArm_5.addOrReplaceChild("LeftForeArm", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.225F, 4.675F, 0.025F, 0.0F, 0.0F, 0.0F));
        p_LeftForeArm_6.addOrReplaceChild("cube_6_0", CubeListBuilder.create().texOffs(13, 60).addBox(-1.29098F, 0.07394F, -1.5F, 2.45F, 5.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition p_LeftHand_7 = p_LeftForeArm_6.addOrReplaceChild("LeftHand", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, 3.8F, 0.0F, 0.0F, 0.0F, 0.0F));
        p_LeftHand_7.addOrReplaceChild("cube_7_0", CubeListBuilder.create().texOffs(46, 70).addBox(-1.31598F, -0.72606F, -1.525F, 2.5F, 3.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition p_LeftHandLocator_8 = p_LeftHand_7.addOrReplaceChild("LeftHandLocator", CubeListBuilder.create(),
                PartPose.offsetAndRotation(-0.25F, 1.25F, -0.1F, 0.0F, 0.0F, 0.0F));
        PartDefinition p_RightArm_9 = p_Arms_4.addOrReplaceChild("RightArm", CubeListBuilder.create(),
                PartPose.offsetAndRotation(-3.45F, 0.875F, 0.1F, 0.0F, 0.0F, 0.22689F));
        p_RightArm_9.addOrReplaceChild("cube_9_0", CubeListBuilder.create().mirror().texOffs(28, 55).addBox(-1.40902F, -1.02606F, -1.5F, 2.5F, 6.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        p_RightArm_9.addOrReplaceChild("cube_9_1", CubeListBuilder.create().mirror().texOffs(34, 117).addBox(-1.5F, -2.1F, -1.75F, 2.5F, 4.0F, 3.5F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-0.65902F, 0.97394F, 0.0F, 0.0F, 0.0F, 0.17453F));
        PartDefinition p_RightForeArm_10 = p_RightArm_9.addOrReplaceChild("RightForeArm", CubeListBuilder.create(),
                PartPose.offsetAndRotation(-0.225F, 4.675F, 0.025F, 0.0F, 0.0F, 0.0F));
        p_RightForeArm_10.addOrReplaceChild("cube_10_0", CubeListBuilder.create().mirror().texOffs(13, 60).addBox(-1.15902F, 0.07394F, -1.5F, 2.45F, 5.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition p_RightHand_11 = p_RightForeArm_10.addOrReplaceChild("RightHand", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, 4.8F, 0.0F, 0.0F, 0.0F, 0.0F));
        p_RightHand_11.addOrReplaceChild("cube_11_0", CubeListBuilder.create().mirror().texOffs(46, 70).addBox(-1.18402F, -0.72606F, -1.525F, 2.5F, 3.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition p_RightHandLocator_12 = p_RightHand_11.addOrReplaceChild("RightHandLocator", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.25F, 1.25F, -0.1F, 0.0F, 0.0F, 0.0F));
        PartDefinition p_AllHead_13 = p_UpperBody_3.addOrReplaceChild("AllHead", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, -6.4F, 0.0F, 0.0F, 0.0F, 0.0F));
        p_AllHead_13.addOrReplaceChild("cube_13_0", CubeListBuilder.create().texOffs(61, 54).addBox(-1.5F, -1.6F, -1.525F, 3.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition p_Head_14 = p_AllHead_13.addOrReplaceChild("Head", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, -0.8F, 0.0F, 0.0F, 0.0F, 0.0F));
        p_Head_14.addOrReplaceChild("cube_14_0", CubeListBuilder.create().texOffs(0, 0).addBox(-3.5F, -7.1F, -3.5F, 7.0F, 7.0F, 7.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition p_Tail_15 = p_Head_14.addOrReplaceChild("Tail", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, 0.475F, 2.9F, 0.3927F, 0.0F, 0.0F));
        PartDefinition p_HorseTail_16 = p_Tail_15.addOrReplaceChild("HorseTail", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, -1.625F, 0.8F, -0.3927F, 0.0F, 0.0F));
        p_HorseTail_16.addOrReplaceChild("cube_16_0", CubeListBuilder.create().texOffs(1, 108).addBox(-2.5F, -3.89F, -2.3F, 5.0F, 4.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 2.8F, 2.2F, -0.47997F, 0.0F, 0.0F));
        PartDefinition p_WeiBa5_17 = p_HorseTail_16.addOrReplaceChild("WeiBa5", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, 3.02475F, 1.71068F, 0.61087F, 0.0F, 0.0F));
        p_WeiBa5_17.addOrReplaceChild("cube_17_0", CubeListBuilder.create().texOffs(1, 116).addBox(-2.0F, -1.5F, -1.5F, 4.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.86142F, 0.72824F, 0.51487F, 0.0F, 0.0F));
        PartDefinition p_WeiBa6_18 = p_WeiBa5_17.addOrReplaceChild("WeiBa6", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, 1.39208F, 1.68934F, -0.56723F, 0.0F, 0.0F));
        p_WeiBa6_18.addOrReplaceChild("cube_18_0", CubeListBuilder.create().texOffs(1, 133).addBox(-1.0F, -3.8F, -0.5F, 2.0F, 5.6F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 5.5174F, -0.26134F, -0.20071F, 0.0F, 0.0F));
        p_WeiBa6_18.addOrReplaceChild("cube_18_1", CubeListBuilder.create().texOffs(13, 120).addBox(-2.5F, 0.88F, 0.38F, 3.0F, 4.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(1.0F, -0.40584F, -2.16984F, -0.23562F, 0.0F, 0.0F));
        p_WeiBa6_18.addOrReplaceChild("cube_18_2", CubeListBuilder.create().texOffs(1, 123).addBox(-0.6F, -2.9F, -1.22F, 2.0F, 7.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(1.4F, 1.65471F, -0.38594F, 0.15411F, -0.45721F, -0.11839F));
        p_WeiBa6_18.addOrReplaceChild("cube_18_3", CubeListBuilder.create().texOffs(10, 128).addBox(0.9F, 0.58F, -0.72F, 3.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-1.0F, -2.10584F, -2.46984F, -0.1309F, 0.0F, 0.0F));
        p_WeiBa6_18.addOrReplaceChild("cube_18_4", CubeListBuilder.create().mirror().texOffs(1, 123).addBox(-1.4F, -2.9F, -1.22F, 2.0F, 7.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-1.4F, 1.65471F, -0.38594F, 0.15411F, 0.45721F, 0.11839F));
        p_WeiBa6_18.addOrReplaceChild("cube_18_5", CubeListBuilder.create().mirror().texOffs(10, 128).addBox(-3.9F, 0.58F, -0.72F, 3.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(1.0F, -2.10584F, -2.46984F, -0.1309F, 0.0F, 0.0F));
        PartDefinition p_bone5_19 = p_WeiBa6_18.addOrReplaceChild("bone5", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, 6.07096F, 0.78398F, 0.0F, 0.0F, 0.0F));
        p_bone5_19.addOrReplaceChild("cube_19_0", CubeListBuilder.create().texOffs(18, 108).addBox(-3.5F, 3.98F, 2.18F, 4.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(1.5F, -6.5518F, -2.80382F, 0.02182F, 0.0F, 0.0F));
        p_bone5_19.addOrReplaceChild("cube_19_1", CubeListBuilder.create().texOffs(23, 115).addBox(-1.5F, -3.0F, -1.0F, 3.0F, 6.0F, 1.5F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 3.03578F, -0.73644F, 0.47124F, 0.0F, 0.0F));
        PartDefinition p_Mouth_20 = p_Head_14.addOrReplaceChild("Mouth", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.7F, -1.2F, 1.275F, 0.0F, 0.0F, 0.0F));
        PartDefinition p_Mouth2_21 = p_Mouth_20.addOrReplaceChild("Mouth2", CubeListBuilder.create(),
                PartPose.offsetAndRotation(-0.7F, 0.325F, -4.1F, 0.0F, 0.0F, 0.0F));
        p_Mouth2_21.addOrReplaceChild("cube_21_0", CubeListBuilder.create().texOffs(62, 103).addBox(-0.9F, -0.4F, 0.0F, 1.8F, 0.1F, 0.05F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        p_Mouth2_21.addOrReplaceChild("cube_21_1", CubeListBuilder.create().texOffs(62, 103).addBox(-0.6F, -0.425F, 0.0F, 1.2F, 0.75F, 0.05F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition p_bone23_22 = p_Mouth2_21.addOrReplaceChild("bone23", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.375F, -0.575F, 0.0F, 0.0F, 0.0F, 0.14399F));
        p_bone23_22.addOrReplaceChild("cube_22_0", CubeListBuilder.create().texOffs(61, 104).addBox(-0.38195F, 0.0969F, 0.0F, 0.925F, 0.15F, 0.05F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition p_bone24_23 = p_Mouth2_21.addOrReplaceChild("bone24", CubeListBuilder.create(),
                PartPose.offsetAndRotation(-0.375F, -0.575F, 0.0F, 0.0F, 0.0F, -0.14399F));
        p_bone24_23.addOrReplaceChild("cube_23_0", CubeListBuilder.create().texOffs(61, 104).addBox(-0.54305F, 0.0969F, 0.0F, 0.9F, 0.15F, 0.05F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition p_bone17_24 = p_Mouth2_21.addOrReplaceChild("bone17", CubeListBuilder.create(),
                PartPose.offsetAndRotation(-0.65F, -0.25F, 0.0F, 0.0F, 0.0F, 1.13446F));
        p_bone17_24.addOrReplaceChild("cube_24_0", CubeListBuilder.create().texOffs(61, 104).addBox(-0.125F, -0.2F, 0.0F, 0.675F, 0.4F, 0.05F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition p_bone18_25 = p_Mouth2_21.addOrReplaceChild("bone18", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.65F, -0.25F, 0.0F, 0.0F, 0.0F, -1.13446F));
        p_bone18_25.addOrReplaceChild("cube_25_0", CubeListBuilder.create().texOffs(61, 104).addBox(-0.55F, -0.2F, 0.0F, 0.675F, 0.4F, 0.05F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition p_Mouth_laugh_26 = p_Mouth_20.addOrReplaceChild("Mouth_laugh", CubeListBuilder.create(),
                PartPose.offsetAndRotation(-0.92523F, 0.1875F, -4.075F, 0.0F, 0.0F, 0.0F));
        p_Mouth_laugh_26.addOrReplaceChild("cube_26_0", CubeListBuilder.create().mirror().texOffs(61, 104).addBox(-0.07454F, -0.0625F, -0.025F, 0.6F, 0.175F, 0.05F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition p_mouth_laugh3_27 = p_Mouth_laugh_26.addOrReplaceChild("mouth_laugh3", CubeListBuilder.create(),
                PartPose.offsetAndRotation(-0.27986F, -0.05251F, 0.0F, 0.0F, 0.0F, 1.13446F));
        p_mouth_laugh3_27.addOrReplaceChild("cube_27_0", CubeListBuilder.create().mirror().texOffs(61, 104).addBox(-0.3625F, 0.25F, -0.025F, 0.675F, 0.075F, 0.05F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-0.20985F, -0.11438F, 0.0F, 0.0F, 0.0F, -0.8203F));
        p_mouth_laugh3_27.addOrReplaceChild("cube_27_1", CubeListBuilder.create().mirror().texOffs(61, 104).addBox(-0.2375F, -0.075F, -0.025F, 0.725F, 0.075F, 0.05F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-0.02578F, 0.09145F, 0.0F, 0.0F, 0.0F, -1.02974F));
        p_mouth_laugh3_27.addOrReplaceChild("cube_27_2", CubeListBuilder.create().mirror().texOffs(61, 104).addBox(0.0875F, -0.025F, -0.025F, 0.275F, 0.075F, 0.05F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-0.02578F, 0.09145F, 0.0F, 0.0F, 0.0F, -1.02974F));
        PartDefinition p_mouth_laugh4_28 = p_Mouth_laugh_26.addOrReplaceChild("mouth_laugh4", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.73032F, -0.05251F, 0.0F, 0.0F, 0.0F, -1.13446F));
        p_mouth_laugh4_28.addOrReplaceChild("cube_28_0", CubeListBuilder.create().texOffs(61, 104).addBox(-0.3125F, 0.25F, -0.025F, 0.675F, 0.075F, 0.05F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.20985F, -0.11438F, 0.0F, 0.0F, 0.0F, 0.8203F));
        p_mouth_laugh4_28.addOrReplaceChild("cube_28_1", CubeListBuilder.create().texOffs(61, 104).addBox(-0.4875F, -0.075F, -0.025F, 0.725F, 0.075F, 0.05F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.02578F, 0.09145F, 0.0F, 0.0F, 0.0F, 1.02974F));
        p_mouth_laugh4_28.addOrReplaceChild("cube_28_2", CubeListBuilder.create().texOffs(61, 104).addBox(-0.3625F, -0.025F, -0.025F, 0.275F, 0.075F, 0.05F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.02578F, 0.09145F, 0.0F, 0.0F, 0.0F, 1.02974F));
        PartDefinition p_mouth_laugh2_29 = p_Mouth_laugh_26.addOrReplaceChild("mouth_laugh2", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.75532F, -0.05251F, 0.0F, 0.0F, 0.0F, -1.13446F));
        PartDefinition p_Mouth3_30 = p_Mouth_20.addOrReplaceChild("Mouth3", CubeListBuilder.create(),
                PartPose.offsetAndRotation(-0.7F, 0.225F, -4.075F, 0.0F, 0.0F, 0.0F));
        p_Mouth3_30.addOrReplaceChild("cube_30_0", CubeListBuilder.create().texOffs(61, 104).addBox(-0.4F, -0.1F, -0.025F, 0.8F, 0.2F, 0.05F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition p_Mouth4_31 = p_Mouth_20.addOrReplaceChild("Mouth4", CubeListBuilder.create(),
                PartPose.offsetAndRotation(-0.575F, 0.25F, -3.1F, 0.0F, 0.0F, 0.0F));
        p_Mouth4_31.addOrReplaceChild("cube_31_0", CubeListBuilder.create().texOffs(61, 104).addBox(-0.3F, -0.075F, 0.0F, 0.4F, 0.15F, 0.05F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -0.05F, 0.0F, 0.0F, 0.0F, -0.6545F));
        p_Mouth4_31.addOrReplaceChild("cube_31_1", CubeListBuilder.create().texOffs(61, 104).addBox(-0.575F, -0.375F, 0.0F, 0.4F, 0.15F, 0.05F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.025F, -0.05F, 0.0F, 0.0F, 0.0F, -0.6545F));
        p_Mouth4_31.addOrReplaceChild("cube_31_2", CubeListBuilder.create().texOffs(61, 104).addBox(-0.3F, 0.15F, 0.0F, 0.35F, 0.15F, 0.05F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -0.05F, 0.0F, 0.0F, 0.0F, 0.87266F));
        p_Mouth4_31.addOrReplaceChild("cube_31_3", CubeListBuilder.create().texOffs(61, 104).addBox(-0.225F, -0.025F, -0.05F, 0.4F, 0.15F, 0.05F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-0.8603F, -0.12494F, 0.05F, 0.0F, 0.0F, -0.73304F));
        p_Mouth4_31.addOrReplaceChild("cube_31_4", CubeListBuilder.create().texOffs(61, 104).addBox(-0.375F, 0.125F, 0.0F, 0.4F, 0.15F, 0.05F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-0.4F, -0.05F, 0.0F, 0.0F, 0.0F, 0.87266F));
        p_Mouth4_31.addOrReplaceChild("cube_31_5", CubeListBuilder.create().mirror().texOffs(61, 104).addBox(-0.175F, -0.025F, -0.05F, 0.4F, 0.15F, 0.05F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.6103F, -0.12494F, 0.05F, 0.0F, 0.0F, 0.73304F));
        p_Mouth4_31.addOrReplaceChild("cube_31_6", CubeListBuilder.create().mirror().texOffs(61, 104).addBox(-0.025F, 0.125F, 0.0F, 0.4F, 0.15F, 0.05F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.15F, -0.05F, 0.0F, 0.0F, 0.0F, -0.87266F));
        p_Mouth4_31.addOrReplaceChild("cube_31_7", CubeListBuilder.create().mirror().texOffs(61, 104).addBox(0.175F, -0.375F, 0.0F, 0.4F, 0.15F, 0.05F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-0.275F, -0.05F, 0.0F, 0.0F, 0.0F, 0.6545F));
        PartDefinition p_Mouth5_32 = p_Mouth_20.addOrReplaceChild("Mouth5", CubeListBuilder.create(),
                PartPose.offsetAndRotation(-0.7F, 0.25F, -4.1F, 0.0F, 0.0F, 0.0F));
        p_Mouth5_32.addOrReplaceChild("cube_32_0", CubeListBuilder.create().texOffs(61, 104).addBox(-0.0625F, 0.03588F, 0.0F, 0.125F, 0.125F, 0.05F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        p_Mouth5_32.addOrReplaceChild("cube_32_1", CubeListBuilder.create().texOffs(61, 104).addBox(-0.175F, -0.05F, 0.0F, 0.25F, 0.125F, 0.05F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-0.07673F, 0.05762F, 0.0F, 0.0F, 0.0F, 0.55851F));
        p_Mouth5_32.addOrReplaceChild("cube_32_2", CubeListBuilder.create().mirror().texOffs(61, 104).addBox(-0.075F, -0.05F, 0.0F, 0.25F, 0.125F, 0.05F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.07673F, 0.05762F, 0.0F, 0.0F, 0.0F, -0.55851F));
        PartDefinition p_Mouth6_33 = p_Mouth_20.addOrReplaceChild("Mouth6", CubeListBuilder.create(),
                PartPose.offsetAndRotation(-0.575F, -0.75F, -4.1F, 0.0F, 0.0F, 0.0F));
        p_Mouth6_33.addOrReplaceChild("cube_33_0", CubeListBuilder.create().texOffs(61, 104).addBox(-0.3F, -0.075F, 0.0F, 0.4F, 0.35F, 0.05F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -0.05F, 0.0F, 0.0F, 0.0F, -0.6545F));
        p_Mouth6_33.addOrReplaceChild("cube_33_1", CubeListBuilder.create().texOffs(61, 104).addBox(-0.575F, -0.375F, 0.0F, 0.4F, 0.525F, 0.05F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.025F, -0.05F, 0.0F, 0.0F, 0.0F, -0.6545F));
        p_Mouth6_33.addOrReplaceChild("cube_33_2", CubeListBuilder.create().texOffs(61, 104).addBox(-0.3F, 0.15F, 0.0F, 0.35F, 0.15F, 0.05F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, -0.05F, 0.0F, 0.0F, 0.0F, 0.87266F));
        p_Mouth6_33.addOrReplaceChild("cube_33_3", CubeListBuilder.create().texOffs(61, 104).addBox(-0.225F, -0.025F, -0.05F, 0.4F, 0.375F, 0.05F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-0.8603F, -0.12494F, 0.05F, 0.0F, 0.0F, -0.73304F));
        p_Mouth6_33.addOrReplaceChild("cube_33_4", CubeListBuilder.create().texOffs(60, 104).addBox(-0.875F, -0.15F, -0.05F, 0.7F, 1.075F, 0.05F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-0.8603F, -0.12494F, 0.05F, 0.0F, 0.0F, -1.3439F));
        p_Mouth6_33.addOrReplaceChild("cube_33_5", CubeListBuilder.create().mirror().texOffs(60, 104).addBox(0.175F, -0.15F, -0.05F, 0.7F, 1.125F, 0.05F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.6103F, -0.12494F, 0.05F, 0.0F, 0.0F, 1.3439F));
        p_Mouth6_33.addOrReplaceChild("cube_33_6", CubeListBuilder.create().mirror().texOffs(61, 104).addBox(0.875F, -0.15F, -0.05F, 0.2F, 0.375F, 0.05F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.6103F, -0.12494F, 0.05F, 0.0F, 0.0F, 1.3439F));
        p_Mouth6_33.addOrReplaceChild("cube_33_7", CubeListBuilder.create().mirror().texOffs(61, 104).addBox(0.875F, 0.225F, -0.05F, 0.1F, 0.375F, 0.05F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.6103F, -0.12494F, 0.05F, 0.0F, 0.0F, 1.3439F));
        p_Mouth6_33.addOrReplaceChild("cube_33_8", CubeListBuilder.create().mirror().texOffs(61, 104).addBox(0.875F, 0.525F, -0.05F, 0.1F, 0.175F, 0.05F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.6103F, -0.12494F, 0.05F, 0.0F, 0.0F, 1.3439F));
        p_Mouth6_33.addOrReplaceChild("cube_33_9", CubeListBuilder.create().texOffs(61, 104).addBox(-0.975F, 0.525F, -0.05F, 0.1F, 0.175F, 0.05F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-0.8603F, -0.12494F, 0.05F, 0.0F, 0.0F, -1.3439F));
        p_Mouth6_33.addOrReplaceChild("cube_33_10", CubeListBuilder.create().texOffs(61, 104).addBox(-1.0F, 0.225F, -0.05F, 0.125F, 0.325F, 0.05F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-0.8603F, -0.12494F, 0.05F, 0.0F, 0.0F, -1.3439F));
        p_Mouth6_33.addOrReplaceChild("cube_33_11", CubeListBuilder.create().texOffs(61, 104).addBox(-1.075F, -0.15F, -0.05F, 0.2F, 0.375F, 0.05F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-0.8603F, -0.12494F, 0.05F, 0.0F, 0.0F, -1.3439F));
        p_Mouth6_33.addOrReplaceChild("cube_33_12", CubeListBuilder.create().texOffs(67, 104).addBox(1.0F, -0.375F, -0.05F, 0.1F, 2.25F, 0.05F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.6103F, -0.12494F, 0.05F, 0.0F, 0.0F, 1.56207F));
        p_Mouth6_33.addOrReplaceChild("cube_33_13", CubeListBuilder.create().texOffs(61, 104).addBox(-0.375F, 0.125F, 0.0F, 0.4F, 0.15F, 0.05F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-0.4F, -0.05F, 0.0F, 0.0F, 0.0F, 0.87266F));
        p_Mouth6_33.addOrReplaceChild("cube_33_14", CubeListBuilder.create().mirror().texOffs(61, 104).addBox(-0.175F, -0.025F, -0.05F, 0.4F, 0.35F, 0.05F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.6103F, -0.12494F, 0.05F, 0.0F, 0.0F, 0.73304F));
        p_Mouth6_33.addOrReplaceChild("cube_33_15", CubeListBuilder.create().mirror().texOffs(61, 104).addBox(-0.025F, 0.125F, 0.0F, 0.4F, 0.15F, 0.05F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.15F, -0.05F, 0.0F, 0.0F, 0.0F, -0.87266F));
        p_Mouth6_33.addOrReplaceChild("cube_33_16", CubeListBuilder.create().mirror().texOffs(61, 104).addBox(0.175F, -0.375F, 0.0F, 0.4F, 0.375F, 0.05F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-0.275F, -0.05F, 0.0F, 0.0F, 0.0F, 0.6545F));
        PartDefinition p_Hair_All_34 = p_Head_14.addOrReplaceChild("Hair_All", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, 24.4F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition p_Hair_35 = p_Hair_All_34.addOrReplaceChild("Hair", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition p_HairFemaleH_Matching_36 = p_Hair_35.addOrReplaceChild("HairFemaleH_Matching", CubeListBuilder.create(),
                PartPose.offsetAndRotation(-0.14783F, -28.36957F, 0.52174F, 0.0F, 0.0F, 0.0F));
        p_HairFemaleH_Matching_36.addOrReplaceChild("cube_36_0", CubeListBuilder.create().mirror().texOffs(45, 95).addBox(0.55529F, 0.04214F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.004F)),
                PartPose.offsetAndRotation(-0.20282F, -0.58527F, -5.12174F, 0.0F, 0.0F, 1.52716F));
        p_HairFemaleH_Matching_36.addOrReplaceChild("cube_36_1", CubeListBuilder.create().mirror().texOffs(78, 23).addBox(0.39176F, -0.30451F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.003F)),
                PartPose.offsetAndRotation(-0.20282F, -0.58527F, -5.12174F, 0.0F, 0.0F, 1.09083F));
        p_HairFemaleH_Matching_36.addOrReplaceChild("cube_36_2", CubeListBuilder.create().mirror().texOffs(59, 94).addBox(0.33741F, -2.50903F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-0.20282F, -0.58527F, -5.12174F, 0.0F, 0.0F, 0.5236F));
        p_HairFemaleH_Matching_36.addOrReplaceChild("cube_36_3", CubeListBuilder.create().mirror().texOffs(90, 77).addBox(-0.66259F, -2.50903F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(-0.003F)),
                PartPose.offsetAndRotation(-0.20282F, -0.58527F, -5.12174F, 0.0F, 0.0F, 0.5236F));
        p_HairFemaleH_Matching_36.addOrReplaceChild("cube_36_4", CubeListBuilder.create().mirror().texOffs(70, 90).addBox(-1.21683F, -2.58597F, -0.5F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-0.20282F, -0.58527F, -5.12174F, 0.0F, 0.0F, 0.34907F));
        p_HairFemaleH_Matching_36.addOrReplaceChild("cube_36_5", CubeListBuilder.create().mirror().texOffs(94, 94).addBox(0.5F, -1.25F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-0.68907F, -1.96421F, -5.02174F, 0.0F, 0.0F, 1.09083F));
        p_HairFemaleH_Matching_36.addOrReplaceChild("cube_36_6", CubeListBuilder.create().mirror().texOffs(50, 95).addBox(0.5F, -0.25F, -0.475F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-1.99236F, -1.28017F, -5.02174F, 0.0F, 0.0F, 1.4399F));
        p_HairFemaleH_Matching_36.addOrReplaceChild("cube_36_7", CubeListBuilder.create().mirror().texOffs(95, 48).addBox(-0.4F, -1.7F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-1.7445F, -1.3128F, -4.99674F, 0.0F, 0.0F, 1.4399F));
        p_HairFemaleH_Matching_36.addOrReplaceChild("cube_36_8", CubeListBuilder.create().texOffs(90, 71).addBox(-4.85217F, -1.70543F, 2.37826F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        p_HairFemaleH_Matching_36.addOrReplaceChild("cube_36_9", CubeListBuilder.create().texOffs(95, 44).addBox(-0.375F, -1.3F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-1.85217F, -2.25543F, -4.62174F, 0.0F, 0.0F, 0.17453F));
        p_HairFemaleH_Matching_36.addOrReplaceChild("cube_36_10", CubeListBuilder.create().texOffs(40, 95).addBox(-0.1F, -0.5F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(-0.003F)),
                PartPose.offsetAndRotation(-2.43961F, -1.5152F, -4.92174F, 0.0F, 0.0F, 0.69813F));
        p_HairFemaleH_Matching_36.addOrReplaceChild("cube_36_11", CubeListBuilder.create().texOffs(50, 95).addBox(-0.74143F, 0.11927F, -0.5125F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(1.8229F, -0.7763F, -5.03424F, 0.0F, 0.0F, -1.27845F));
        p_HairFemaleH_Matching_36.addOrReplaceChild("cube_36_12", CubeListBuilder.create().texOffs(94, 94).addBox(-0.65593F, -2.63434F, -0.4875F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(1.8229F, -0.7763F, -5.03424F, 0.0F, 0.0F, -0.92939F));
        p_HairFemaleH_Matching_36.addOrReplaceChild("cube_36_13", CubeListBuilder.create().mirror().texOffs(40, 95).addBox(-0.9F, -0.5F, -0.5F, 1.0F, 2.0F, 1.0F, new CubeDeformation(-0.003F)),
                PartPose.offsetAndRotation(2.73527F, -1.5152F, -4.92174F, 0.0F, 0.0F, -0.69813F));
        p_HairFemaleH_Matching_36.addOrReplaceChild("cube_36_14", CubeListBuilder.create().texOffs(95, 19).addBox(1.44783F, -3.25543F, -5.42174F, 1.0F, 2.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        p_HairFemaleH_Matching_36.addOrReplaceChild("cube_36_15", CubeListBuilder.create().texOffs(69, 66).addBox(-0.5F, -2.0F, -1.5F, 1.0F, 4.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-3.81467F, -1.05543F, -3.07174F, 0.0F, 0.0F, 0.17453F));
        p_HairFemaleH_Matching_36.addOrReplaceChild("cube_36_16", CubeListBuilder.create().texOffs(58, 69).addBox(-0.7F, -2.0F, -1.5F, 1.2F, 4.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(3.98533F, -1.05543F, -3.12174F, 0.0F, 0.0F, -0.17453F));
        p_HairFemaleH_Matching_36.addOrReplaceChild("cube_36_17", CubeListBuilder.create().texOffs(41, 17).addBox(-4.35217F, -3.63043F, -3.07174F, 1.0F, 5.0F, 6.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        p_HairFemaleH_Matching_36.addOrReplaceChild("cube_36_18", CubeListBuilder.create().texOffs(43, 52).addBox(-2.85217F, -2.83043F, 3.70326F, 6.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        p_HairFemaleH_Matching_36.addOrReplaceChild("cube_36_19", CubeListBuilder.create().texOffs(28, 14).addBox(-3.85217F, -2.83043F, 2.70326F, 8.0F, 7.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        p_HairFemaleH_Matching_36.addOrReplaceChild("cube_36_20", CubeListBuilder.create().texOffs(36, 33).addBox(3.64783F, -3.63043F, -3.07174F, 1.0F, 5.0F, 6.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        p_HairFemaleH_Matching_36.addOrReplaceChild("cube_36_21", CubeListBuilder.create().texOffs(78, 65).addBox(-0.25F, -4.1F, 0.5F, 1.0F, 8.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-4.85217F, 1.36957F, -0.09674F, 0.0F, 0.0F, 0.08727F));
        p_HairFemaleH_Matching_36.addOrReplaceChild("cube_36_22", CubeListBuilder.create().texOffs(62, 77).addBox(-0.25F, -4.1F, -0.5F, 1.0F, 8.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-4.85217F, 1.36957F, -0.09674F, 0.0F, 0.0F, 0.08727F));
        p_HairFemaleH_Matching_36.addOrReplaceChild("cube_36_23", CubeListBuilder.create().texOffs(75, 83).addBox(-0.25F, -3.1F, -1.5F, 1.0F, 6.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-4.85217F, 1.36957F, -0.09674F, 0.0F, 0.0F, 0.08727F));
        p_HairFemaleH_Matching_36.addOrReplaceChild("cube_36_24", CubeListBuilder.create().texOffs(35, 94).addBox(-0.45F, -1.7125F, -2.4875F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(4.89783F, 0.08207F, 0.89076F, 0.0F, 0.0F, -0.1309F));
        p_HairFemaleH_Matching_36.addOrReplaceChild("cube_36_25", CubeListBuilder.create().texOffs(15, 88).addBox(-0.45F, -2.7125F, -1.4875F, 1.0F, 5.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(4.89783F, 0.08207F, 0.89076F, 0.0F, 0.0F, -0.1309F));
        p_HairFemaleH_Matching_36.addOrReplaceChild("cube_36_26", CubeListBuilder.create().texOffs(90, 65).addBox(-0.95F, -1.7875F, 1.4875F, 1.0F, 4.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(4.89783F, 0.08207F, 0.89076F, 0.0F, 0.0F, -0.1309F));
        p_HairFemaleH_Matching_36.addOrReplaceChild("cube_36_27", CubeListBuilder.create().texOffs(0, 74).addBox(-4.35217F, 0.96957F, 0.35326F, 1.0F, 3.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        p_HairFemaleH_Matching_36.addOrReplaceChild("cube_36_28", CubeListBuilder.create().texOffs(78, 4).addBox(-4.10217F, 1.36957F, -2.64674F, 1.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        p_HairFemaleH_Matching_36.addOrReplaceChild("cube_36_29", CubeListBuilder.create().texOffs(77, 52).addBox(3.39783F, 1.36957F, -2.64674F, 1.0F, 2.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        p_HairFemaleH_Matching_36.addOrReplaceChild("cube_36_30", CubeListBuilder.create().texOffs(8, 69).addBox(3.64783F, -0.03043F, 0.25326F, 1.0F, 4.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        p_HairFemaleH_Matching_36.addOrReplaceChild("cube_36_31", CubeListBuilder.create().texOffs(0, 15).addBox(-4.25217F, -2.23043F, -4.22174F, 1.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        p_HairFemaleH_Matching_36.addOrReplaceChild("cube_36_32", CubeListBuilder.create().texOffs(67, 74).addBox(-1.85F, -1.7F, -0.9F, 4.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.14783F, -2.73043F, -4.32174F, 0.06545F, 0.0F, -0.14399F));
        p_HairFemaleH_Matching_36.addOrReplaceChild("cube_36_33", CubeListBuilder.create().texOffs(0, 25).addBox(-2.82717F, -4.53043F, -4.22174F, 6.0F, 1.0F, 7.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        p_HairFemaleH_Matching_36.addOrReplaceChild("cube_36_34", CubeListBuilder.create().texOffs(0, 15).addBox(-3.85217F, -3.83043F, -4.29674F, 8.0F, 1.0F, 8.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        p_HairFemaleH_Matching_36.addOrReplaceChild("cube_36_35", CubeListBuilder.create().texOffs(43, 45).addBox(-1.85217F, -5.53043F, -3.22174F, 4.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        p_HairFemaleH_Matching_36.addOrReplaceChild("cube_36_36", CubeListBuilder.create().texOffs(32, 80).addBox(-2.85217F, -5.53043F, -2.22174F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        p_HairFemaleH_Matching_36.addOrReplaceChild("cube_36_37", CubeListBuilder.create().texOffs(80, 23).addBox(2.14783F, -5.53043F, -2.22174F, 1.0F, 1.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        p_HairFemaleH_Matching_36.addOrReplaceChild("cube_36_38", CubeListBuilder.create().texOffs(58, 47).addBox(-3.82717F, -4.53043F, -3.22174F, 1.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        p_HairFemaleH_Matching_36.addOrReplaceChild("cube_36_39", CubeListBuilder.create().texOffs(53, 55).addBox(3.17283F, -4.53043F, -3.22174F, 1.0F, 1.0F, 5.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        p_HairFemaleH_Matching_36.addOrReplaceChild("cube_36_40", CubeListBuilder.create().texOffs(10, 94).addBox(-0.825F, -2.15F, -0.5F, 1.2F, 3.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(3.02283F, -1.65543F, -4.62174F, 0.0F, 0.0F, -0.08727F));
        p_HairFemaleH_Matching_36.addOrReplaceChild("cube_36_41", CubeListBuilder.create().texOffs(5, 94).addBox(-0.425F, -1.5F, -0.5F, 1.0F, 3.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-2.85217F, -1.65543F, -4.52174F, 0.0F, 0.0F, 0.21817F));
        PartDefinition p_HairSideDown_37 = p_Hair_All_34.addOrReplaceChild("HairSideDown", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition p_SideDownHairC_38 = p_HairSideDown_37.addOrReplaceChild("SideDownHairC", CubeListBuilder.create(),
                PartPose.offsetAndRotation(2.98601F, -26.147F, 2.31083F, 0.0F, 0.0F, 0.0F));
        PartDefinition p_Left_SideHairC_39 = p_SideDownHairC_38.addOrReplaceChild("Left_SideHairC", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.40149F, -4.8734F, -4.71498F, 0.05171F, 0.00872F, -0.21785F));
        p_Left_SideHairC_39.addOrReplaceChild("cube_39_0", CubeListBuilder.create().texOffs(95, 68).addBox(-0.5125F, 1.25F, -1.0F, 1.025F, 2.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        p_Left_SideHairC_39.addOrReplaceChild("cube_39_1", CubeListBuilder.create().texOffs(15, 95).addBox(-0.89117F, -0.24156F, -0.8067F, 1.025F, 3.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.415F, 3.0735F, -0.1683F, 0.0F, 0.0F, 0.1309F));
        PartDefinition p_Left_SideHairC1_40 = p_Left_SideHairC_39.addOrReplaceChild("Left_SideHairC1", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.375F, -0.75F, 1.0F, 0.00064F, -0.00413F, -0.1307F));
        p_Left_SideHairC1_40.addOrReplaceChild("cube_40_0", CubeListBuilder.create().texOffs(88, 21).addBox(-0.50378F, -1.69981F, -0.7625F, 1.025F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-0.08372F, 4.54521F, -1.13335F, 0.0F, 0.0F, 0.17453F));
        p_Left_SideHairC1_40.addOrReplaceChild("cube_40_1", CubeListBuilder.create().texOffs(89, 11).addBox(-0.50378F, -1.69981F, -0.7625F, 1.025F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-0.60872F, 6.24521F, -1.08335F, 0.0F, 0.0F, 0.29671F));
        p_Left_SideHairC1_40.addOrReplaceChild("cube_40_2", CubeListBuilder.create().texOffs(5, 89).addBox(-0.51569F, 0.19952F, -0.7375F, 1.025F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-0.60872F, 6.24521F, -1.08335F, 0.0F, 0.0F, 0.34034F));
        p_Left_SideHairC1_40.addOrReplaceChild("cube_40_3", CubeListBuilder.create().texOffs(0, 85).addBox(-0.28518F, -0.02581F, -1.90046F, 1.025F, 3.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        p_Left_SideHairC1_40.addOrReplaceChild("cube_40_4", CubeListBuilder.create().texOffs(85, 90).addBox(-0.2625F, -2.0F, -1.05F, 1.025F, 4.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.22732F, 2.97419F, 0.59954F, 0.00145F, -0.00823F, -0.08687F));
        PartDefinition p_Left_SideHairC2_41 = p_Left_SideHairC_39.addOrReplaceChild("Left_SideHairC2", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.05424F, 4.47741F, 0.84067F, -0.1228F, 0.02269F, 0.174F));
        p_Left_SideHairC2_41.addOrReplaceChild("cube_41_0", CubeListBuilder.create().mirror().texOffs(95, 60).addBox(-0.42731F, -2.18437F, -0.5125F, 1.025F, 2.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-0.52194F, 0.40202F, -0.26298F, 0.00091F, -0.00995F, -0.0514F));
        p_Left_SideHairC2_41.addOrReplaceChild("cube_41_1", CubeListBuilder.create().texOffs(0, 85).addBox(-0.29318F, -3.37781F, -0.76486F, 1.025F, 3.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.32125F, -0.43036F, 0.08892F, -0.03968F, -0.0174F, -0.13065F));
        p_Left_SideHairC2_41.addOrReplaceChild("cube_41_2", CubeListBuilder.create().texOffs(38, 80).addBox(-0.34698F, 1.38431F, -0.73525F, 1.025F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.32125F, -0.43036F, 0.08892F, -0.04251F, -0.0084F, 0.08734F));
        p_Left_SideHairC2_41.addOrReplaceChild("cube_41_3", CubeListBuilder.create().texOffs(88, 21).addBox(-0.3869F, -0.5088F, -0.76025F, 1.025F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.32125F, -0.43036F, 0.08892F, -0.0421F, -0.01025F, 0.04375F));
        PartDefinition p_Right_SideHairC_42 = p_SideDownHairC_38.addOrReplaceChild("Right_SideHairC", CubeListBuilder.create(),
                PartPose.offsetAndRotation(-6.72351F, -4.2234F, -5.06498F, 0.11693F, -0.01306F, 0.21754F));
        p_Right_SideHairC_42.addOrReplaceChild("cube_42_0", CubeListBuilder.create().texOffs(95, 64).addBox(-0.5125F, 0.5F, -1.0F, 1.025F, 2.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        p_Right_SideHairC_42.addOrReplaceChild("cube_42_1", CubeListBuilder.create().mirror().texOffs(15, 95).addBox(-0.13383F, 0.10844F, -0.8067F, 1.025F, 3.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-0.415F, 2.3235F, -0.1683F, 0.0F, 0.0F, -0.1309F));
        PartDefinition p_Right_SideHairC1_43 = p_Right_SideHairC_42.addOrReplaceChild("Right_SideHairC1", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.02076F, -0.47259F, 0.39067F, 0.00507F, -0.02355F, -0.04306F));
        p_Right_SideHairC1_43.addOrReplaceChild("cube_43_0", CubeListBuilder.create().texOffs(95, 60).addBox(-0.59769F, -2.18437F, -0.5125F, 1.025F, 2.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.52194F, 4.70202F, -0.26298F, 0.00091F, 0.00995F, 0.0514F));
        p_Right_SideHairC1_43.addOrReplaceChild("cube_43_1", CubeListBuilder.create().mirror().texOffs(0, 85).addBox(-0.73182F, -3.37781F, -0.76486F, 1.025F, 3.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-0.32125F, 3.86964F, 0.08892F, -0.03968F, 0.0174F, 0.13065F));
        p_Right_SideHairC1_43.addOrReplaceChild("cube_43_2", CubeListBuilder.create().mirror().texOffs(88, 21).addBox(-0.6381F, -0.5088F, -0.76025F, 1.025F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-0.32125F, 3.86964F, 0.08892F, -0.0421F, 0.01025F, -0.04375F));
        p_Right_SideHairC1_43.addOrReplaceChild("cube_43_3", CubeListBuilder.create().mirror().texOffs(5, 89).addBox(-0.5125F, -1.05F, -1.0F, 1.025F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-0.29816F, 6.22246F, 0.51348F, -0.14399F, 0.0F, -0.07854F));
        PartDefinition p_Right_SideHairC2_44 = p_Right_SideHairC_42.addOrReplaceChild("Right_SideHairC2", CubeListBuilder.create(),
                PartPose.offsetAndRotation(-0.375F, -1.5F, 1.0F, 0.00216F, 0.00808F, 0.26141F));
        p_Right_SideHairC2_44.addOrReplaceChild("cube_44_0", CubeListBuilder.create().mirror().texOffs(88, 21).addBox(-0.52122F, -1.69981F, -0.7625F, 1.025F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.08372F, 4.54521F, -1.13335F, 0.0F, 0.0F, -0.17453F));
        p_Right_SideHairC2_44.addOrReplaceChild("cube_44_1", CubeListBuilder.create().mirror().texOffs(38, 80).addBox(-0.50931F, 0.19952F, -0.7375F, 1.025F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.08372F, 4.54521F, -1.13335F, 0.0F, 0.0F, -0.21817F));
        p_Right_SideHairC2_44.addOrReplaceChild("cube_44_2", CubeListBuilder.create().texOffs(67, 84).addBox(-0.73982F, -0.02581F, -1.90046F, 1.025F, 3.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition p_HairSide_45 = p_Hair_All_34.addOrReplaceChild("HairSide", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition p_SideHairA_46 = p_HairSide_45.addOrReplaceChild("SideHairA", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, -7.4F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition p_Left_SideHairA1_47 = p_SideHairA_46.addOrReplaceChild("Left_SideHairA1", CubeListBuilder.create(),
                PartPose.offsetAndRotation(4.1625F, -22.5704F, -1.80415F, -0.8198F, 0.03609F, -0.10093F));
        p_Left_SideHairA1_47.addOrReplaceChild("cube_47_0", CubeListBuilder.create().texOffs(95, 15).addBox(-0.5125F, 0.5F, -1.2F, 1.025F, 2.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        p_Left_SideHairA1_47.addOrReplaceChild("cube_47_1", CubeListBuilder.create().texOffs(93, 5).addBox(-0.89117F, 0.10844F, -0.8067F, 1.025F, 3.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.415F, 2.3235F, -0.3683F, 0.0F, 0.0F, 0.1309F));
        PartDefinition p_Left_SideHairA2_48 = p_Left_SideHairA1_47.addOrReplaceChild("Left_SideHairA2", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.37803F, -0.96972F, 0.49439F, 0.3429F, 0.07018F, -0.33598F));
        p_Left_SideHairA2_48.addOrReplaceChild("cube_48_0", CubeListBuilder.create().texOffs(87, 30).addBox(-0.50378F, -1.69981F, -0.7625F, 1.025F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.51628F, 4.5452F, -0.13334F, 0.0F, 0.0F, -0.17453F));
        p_Left_SideHairA2_48.addOrReplaceChild("cube_48_1", CubeListBuilder.create().texOffs(73, 7).addBox(-0.54069F, 0.19953F, 0.2625F, 1.025F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.51628F, 4.5452F, -0.13334F, 0.0F, 0.0F, -0.3927F));
        p_Left_SideHairA2_48.addOrReplaceChild("cube_48_2", CubeListBuilder.create().texOffs(39, 83).addBox(-0.28518F, -0.02582F, -0.90046F, 1.025F, 3.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        p_Left_SideHairA2_48.addOrReplaceChild("cube_48_3", CubeListBuilder.create().texOffs(65, 90).addBox(-0.6375F, -2.0F, -0.5F, 1.025F, 4.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.74415F, 0.60147F, 1.99468F, 0.28305F, 0.12049F, -0.50474F));
        PartDefinition p_Right_SideHairA1_49 = p_SideHairA_46.addOrReplaceChild("Right_SideHairA1", CubeListBuilder.create(),
                PartPose.offsetAndRotation(-4.1625F, -22.5704F, -1.80415F, -0.8198F, -0.03609F, 0.10093F));
        p_Right_SideHairA1_49.addOrReplaceChild("cube_49_0", CubeListBuilder.create().mirror().texOffs(95, 15).addBox(-0.5125F, 0.5F, -1.0F, 1.025F, 2.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        p_Right_SideHairA1_49.addOrReplaceChild("cube_49_1", CubeListBuilder.create().mirror().texOffs(93, 5).addBox(-0.13383F, 0.10844F, -0.8067F, 1.025F, 3.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-0.415F, 2.3235F, -0.1683F, 0.0F, 0.0F, -0.1309F));
        PartDefinition p_Right_SideHairA2_50 = p_Right_SideHairA1_49.addOrReplaceChild("Right_SideHairA2", CubeListBuilder.create(),
                PartPose.offsetAndRotation(-0.37803F, -0.96972F, 0.49439F, 0.3429F, -0.07018F, 0.33598F));
        p_Right_SideHairA2_50.addOrReplaceChild("cube_50_0", CubeListBuilder.create().mirror().texOffs(87, 30).addBox(-0.52122F, -1.69981F, -0.7625F, 1.025F, 2.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-0.51628F, 4.5452F, -0.13334F, 0.0F, 0.0F, 0.17453F));
        p_Right_SideHairA2_50.addOrReplaceChild("cube_50_1", CubeListBuilder.create().mirror().texOffs(73, 7).addBox(-0.48431F, 0.19953F, 0.2625F, 1.025F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-0.51628F, 4.5452F, -0.13334F, 0.0F, 0.0F, 0.3927F));
        p_Right_SideHairA2_50.addOrReplaceChild("cube_50_2", CubeListBuilder.create().mirror().texOffs(39, 83).addBox(-0.73982F, -0.02582F, -0.90046F, 1.025F, 3.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        p_Right_SideHairA2_50.addOrReplaceChild("cube_50_3", CubeListBuilder.create().mirror().texOffs(65, 90).addBox(-0.3875F, -2.0F, -0.5F, 1.025F, 4.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-0.74415F, 0.60147F, 1.99468F, 0.28305F, -0.12049F, 0.50474F));
        PartDefinition p_Accessory_51 = p_Hair_All_34.addOrReplaceChild("Accessory", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition p_HorseEar_52 = p_Accessory_51.addOrReplaceChild("HorseEar", CubeListBuilder.create(),
                PartPose.offsetAndRotation(-0.14783F, -30.76957F, -1.92826F, 0.0F, 0.0F, 0.0F));
        PartDefinition p_LeftHorseEar_53 = p_HorseEar_52.addOrReplaceChild("LeftHorseEar", CubeListBuilder.create(),
                PartPose.offsetAndRotation(3.4351F, -1.89965F, -0.26645F, -0.27595F, -0.15503F, 0.08623F));
        p_LeftHorseEar_53.addOrReplaceChild("cube_53_0", CubeListBuilder.create().texOffs(83, 70).addBox(0.32853F, -2.15053F, -1.05F, 1.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.4F, -1.6F, 0.5F, 0.12186F, 0.23633F, 0.13207F));
        p_LeftHorseEar_53.addOrReplaceChild("cube_53_1", CubeListBuilder.create().texOffs(83, 64).addBox(0.57853F, -0.02553F, -1.075F, 1.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.4F, -1.6F, 0.525F, 0.22953F, 0.13438F, 0.70033F));
        p_LeftHorseEar_53.addOrReplaceChild("cube_53_2", CubeListBuilder.create().texOffs(53, 83).addBox(-3.1437F, -3.51734F, -1.11181F, 1.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.4F, 0.3F, 0.3F, 0.25528F, 0.07332F, 0.9473F));
        p_LeftHorseEar_53.addOrReplaceChild("cube_53_3", CubeListBuilder.create().texOffs(46, 83).addBox(0.3F, -1.575F, -1.025F, 1.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-1.8732F, -1.28064F, -0.22403F, 0.18588F, 0.19051F, 0.4321F));
        p_LeftHorseEar_53.addOrReplaceChild("cube_53_4", CubeListBuilder.create().texOffs(78, 32).addBox(-1.22896F, -0.79776F, -0.47099F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.4F, -1.5F, 0.3F, -0.24273F, 0.12831F, 0.53222F));
        PartDefinition p_RightHorseEar_54 = p_HorseEar_52.addOrReplaceChild("RightHorseEar", CubeListBuilder.create(),
                PartPose.offsetAndRotation(-3.13944F, -1.89965F, -0.26645F, -0.27595F, 0.15503F, -0.08623F));
        p_RightHorseEar_54.addOrReplaceChild("cube_54_0", CubeListBuilder.create().mirror().texOffs(83, 70).addBox(-1.32853F, -2.20053F, -1.05F, 1.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-0.4F, -1.575F, 0.5F, 0.12186F, -0.23633F, -0.13207F));
        p_RightHorseEar_54.addOrReplaceChild("cube_54_1", CubeListBuilder.create().mirror().texOffs(83, 64).addBox(-1.57853F, -0.02553F, -1.075F, 1.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-0.4F, -1.575F, 0.525F, 0.22953F, -0.13438F, -0.70033F));
        p_RightHorseEar_54.addOrReplaceChild("cube_54_2", CubeListBuilder.create().mirror().texOffs(53, 83).addBox(2.1437F, -3.51734F, -1.11181F, 1.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-0.4F, 0.3F, 0.3F, 0.25528F, -0.07332F, -0.9473F));
        p_RightHorseEar_54.addOrReplaceChild("cube_54_3", CubeListBuilder.create().mirror().texOffs(46, 83).addBox(-1.3F, -1.575F, -1.025F, 1.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(1.8732F, -1.28064F, -0.22403F, 0.18588F, -0.19051F, -0.4321F));
        p_RightHorseEar_54.addOrReplaceChild("cube_54_4", CubeListBuilder.create().mirror().texOffs(78, 32).addBox(-0.77104F, -0.79776F, -0.47099F, 2.0F, 3.0F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-0.4F, -1.5F, 0.3F, -0.24273F, -0.12831F, -0.53222F));
        PartDefinition p_Eyes_55 = p_Head_14.addOrReplaceChild("Eyes", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, 24.4F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition p_Eyebrows_56 = p_Eyes_55.addOrReplaceChild("Eyebrows", CubeListBuilder.create(),
                PartPose.offsetAndRotation(-3.22864F, -27.09562F, -3.6225F, 0.0F, 0.0F, 0.0F));
        PartDefinition p_EyeBrowF_57 = p_Eyebrows_56.addOrReplaceChild("EyeBrowF", CubeListBuilder.create(),
                PartPose.offsetAndRotation(3.22864F, -0.30438F, 0.1475F, 0.0F, 0.0F, 0.0F));
        PartDefinition p_LeftEyebrow_58 = p_EyeBrowF_57.addOrReplaceChild("LeftEyebrow", CubeListBuilder.create(),
                PartPose.offsetAndRotation(2.875F, 0.15F, 0.175F, 0.0F, 0.0F, 0.0F));
        p_LeftEyebrow_58.addOrReplaceChild("cube_58_0", CubeListBuilder.create().texOffs(5, 15).addBox(-0.55F, -0.475F, -0.5F, 1.3F, 1.0F, 0.075F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.15F, -0.5F, 0.0F, 0.0F, 0.0F, 0.04363F));
        p_LeftEyebrow_58.addOrReplaceChild("cube_58_1", CubeListBuilder.create().texOffs(5, 15).addBox(-1.475F, -0.5F, -0.5F, 1.5F, 1.0F, 0.075F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-0.35061F, -0.49683F, 0.0F, 0.0F, 0.0F, -0.03927F));
        PartDefinition p_EyeBrow_Left4_59 = p_LeftEyebrow_58.addOrReplaceChild("EyeBrow_Left4", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.95F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        p_EyeBrow_Left4_59.addOrReplaceChild("cube_59_0", CubeListBuilder.create().texOffs(25, 21).addBox(-0.55F, -0.5F, -0.5F, 1.1F, 1.0F, 0.075F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition p_RightEyebrow_60 = p_EyeBrowF_57.addOrReplaceChild("RightEyebrow", CubeListBuilder.create(),
                PartPose.offsetAndRotation(-2.875F, 0.15F, 0.175F, 0.0F, 0.0F, 0.0F));
        p_RightEyebrow_60.addOrReplaceChild("cube_60_0", CubeListBuilder.create().mirror().texOffs(5, 15).addBox(-0.75F, -0.475F, -0.5F, 1.3F, 1.0F, 0.075F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-0.15F, -0.5F, 0.0F, 0.0F, 0.0F, -0.04363F));
        p_RightEyebrow_60.addOrReplaceChild("cube_60_1", CubeListBuilder.create().mirror().texOffs(5, 15).addBox(-0.025F, -0.5F, -0.5F, 1.5F, 1.0F, 0.075F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.35061F, -0.49683F, 0.0F, 0.0F, 0.0F, 0.03927F));
        PartDefinition p_EyeBrow_Right4_61 = p_RightEyebrow_60.addOrReplaceChild("EyeBrow_Right4", CubeListBuilder.create(),
                PartPose.offsetAndRotation(-0.95F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        p_EyeBrow_Right4_61.addOrReplaceChild("cube_61_0", CubeListBuilder.create().mirror().texOffs(25, 21).addBox(-0.55F, -0.5F, -0.5F, 1.1F, 1.0F, 0.075F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition p_Eyelid_62 = p_Eyes_55.addOrReplaceChild("Eyelid", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, -26.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition p_LeftEyeball_63 = p_Eyelid_62.addOrReplaceChild("LeftEyeball", CubeListBuilder.create(),
                PartPose.offsetAndRotation(2.375F, -0.5F, -3.525F, 0.0F, 0.0F, 0.0F));
        p_LeftEyeball_63.addOrReplaceChild("cube_63_0", CubeListBuilder.create().texOffs(94, 10).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition p_LeftEyesBase2_64 = p_LeftEyeball_63.addOrReplaceChild("LeftEyesBase2", CubeListBuilder.create(),
                PartPose.offsetAndRotation(-0.125F, 0.0F, 0.475F, 0.0F, 0.0F, 0.0F));
        p_LeftEyesBase2_64.addOrReplaceChild("cube_64_0", CubeListBuilder.create().texOffs(25, 18).addBox(-0.75F, -1.0F, -0.5F, 1.4F, 2.0F, 0.025F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition p_LeftEylight_65 = p_LeftEyesBase2_64.addOrReplaceChild("LeftEylight", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.35F, -0.65F, -0.6375F, 0.0F, 0.0F, 0.0F));
        p_LeftEylight_65.addOrReplaceChild("cube_65_0", CubeListBuilder.create().texOffs(6, 103).addBox(-0.25F, -0.25F, -0.0125F, 0.5F, 0.5F, 0.025F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition p_LeftPupil_66 = p_LeftEyesBase2_64.addOrReplaceChild("LeftPupil", CubeListBuilder.create(),
                PartPose.offsetAndRotation(-0.025F, -0.15F, -0.2075F, 0.0F, 0.0F, 0.0F));
        p_LeftPupil_66.addOrReplaceChild("cube_66_0", CubeListBuilder.create().texOffs(1, 102).addBox(-0.35F, -0.25F, -0.3425F, 0.65F, 0.8F, 2.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition p_RightEyeball_67 = p_Eyelid_62.addOrReplaceChild("RightEyeball", CubeListBuilder.create(),
                PartPose.offsetAndRotation(-2.375F, -0.5F, -3.525F, 0.0F, 0.0F, 0.0F));
        p_RightEyeball_67.addOrReplaceChild("cube_67_0", CubeListBuilder.create().texOffs(87, 35).addBox(-1.0F, -1.0F, 0.0F, 2.0F, 2.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition p_RightIris_68 = p_RightEyeball_67.addOrReplaceChild("RightIris", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.125F, 0.0F, 0.475F, 0.0F, 0.0F, 0.0F));
        p_RightIris_68.addOrReplaceChild("cube_68_0", CubeListBuilder.create().texOffs(25, 15).addBox(-0.65F, -1.0F, -0.5F, 1.4F, 2.0F, 0.025F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition p_RightEyelight_69 = p_RightIris_68.addOrReplaceChild("RightEyelight", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.525F, -0.65F, -0.6375F, 0.0F, 0.0F, 0.0F));
        p_RightEyelight_69.addOrReplaceChild("cube_69_0", CubeListBuilder.create().texOffs(6, 103).addBox(-0.25F, -0.25F, -0.0125F, 0.5F, 0.5F, 0.025F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition p_RightPupil_70 = p_RightIris_68.addOrReplaceChild("RightPupil", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.025F, -0.15F, -0.2075F, 0.0F, 0.0F, 0.0F));
        p_RightPupil_70.addOrReplaceChild("cube_70_0", CubeListBuilder.create().texOffs(1, 102).addBox(-0.3F, -0.25F, -0.3425F, 0.65F, 0.8F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition p_Breast_71 = p_UpperBody_3.addOrReplaceChild("Breast", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0125F, -4.28714F, -1.95514F, -0.17453F, 0.0F, 0.0F));
        p_Breast_71.addOrReplaceChild("cube_71_0", CubeListBuilder.create().texOffs(39, 60).addBox(-1.73598F, -1.51742F, -1.48262F, 3.075F, 3.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(1.9411F, 1.52317F, -0.22292F, 0.92701F, 0.06986F, -0.05225F));
        p_Breast_71.addOrReplaceChild("cube_71_1", CubeListBuilder.create().texOffs(11, 102).addBox(-1.73598F, -1.51742F, -1.45762F, 1.075F, 3.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(1.18598F, 1.52317F, -0.22292F, 0.92701F, 0.0F, 0.0F));
        p_Breast_71.addOrReplaceChild("cube_71_2", CubeListBuilder.create().mirror().texOffs(35, 109).addBox(-1.33902F, -1.51742F, -1.48262F, 3.075F, 3.0F, 3.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-1.9661F, 1.52317F, -0.22292F, 0.92701F, -0.06986F, 0.05225F));
        PartDefinition p_RightWaistLocator_72 = p_UpperBody_3.addOrReplaceChild("RightWaistLocator", CubeListBuilder.create(),
                PartPose.offsetAndRotation(-4.0F, 1.2F, 0.0F, 0.08727F, 0.0F, 0.0F));
        PartDefinition p_LeftWaistLocator_73 = p_UpperBody_3.addOrReplaceChild("LeftWaistLocator", CubeListBuilder.create(),
                PartPose.offsetAndRotation(4.0F, 1.2F, 0.0F, 0.34907F, 0.0F, 0.0F));
        PartDefinition p_BackpackLocator_74 = p_UpperBody_3.addOrReplaceChild("BackpackLocator", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, -3.7644F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition p_DownBody_75 = p_AllBody_1.addOrReplaceChild("DownBody", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, 0.7F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition p_Skirt_76 = p_DownBody_75.addOrReplaceChild("Skirt", CubeListBuilder.create(),
                PartPose.offsetAndRotation(-0.2F, -0.32329F, 1.52782F, 0.0F, 0.0F, 0.0F));
        p_Skirt_76.addOrReplaceChild("cube_76_0", CubeListBuilder.create().texOffs(103, 43).addBox(1.94309F, -2.0067F, -2.44375F, 2.25F, 5.3F, 3.625F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.4F, 2.25882F, -2.25907F, 0.25462F, 0.06156F, -0.2321F));
        p_Skirt_76.addOrReplaceChild("cube_76_1", CubeListBuilder.create().texOffs(122, 58).addBox(-0.5F, -0.4F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(2.04298F, 1.33486F, -4.12734F, 0.25889F, 0.03938F, -0.14759F));
        p_Skirt_76.addOrReplaceChild("cube_76_2", CubeListBuilder.create().texOffs(117, 58).addBox(-0.5F, -1.8F, -0.5F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(2.04298F, 1.33486F, -4.12734F, 0.25889F, 0.03938F, -0.14759F));
        p_Skirt_76.addOrReplaceChild("cube_76_3", CubeListBuilder.create().texOffs(117, 58).addBox(3.29309F, -1.7067F, 1.56875F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.4F, 2.25882F, -0.79657F, -0.25462F, -0.06156F, -0.2321F));
        p_Skirt_76.addOrReplaceChild("cube_76_4", CubeListBuilder.create().texOffs(122, 58).addBox(3.29309F, -0.2067F, 1.56875F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.4F, 2.25882F, -0.79657F, -0.25462F, -0.06156F, -0.2321F));
        p_Skirt_76.addOrReplaceChild("cube_76_5", CubeListBuilder.create().mirror().texOffs(117, 58).addBox(-4.29309F, -1.9067F, -2.54375F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 2.25882F, -2.25907F, 0.25462F, -0.06156F, 0.2321F));
        p_Skirt_76.addOrReplaceChild("cube_76_6", CubeListBuilder.create().mirror().texOffs(122, 58).addBox(-4.29309F, -0.4067F, -2.54375F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 2.25882F, -2.25907F, 0.25462F, -0.06156F, 0.2321F));
        p_Skirt_76.addOrReplaceChild("cube_76_7", CubeListBuilder.create().texOffs(103, 54).addBox(0.17403F, -2.37423F, -2.39375F, 2.75F, 5.0F, 3.8F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.2F, 2.25882F, -2.25907F, 0.26176F, -0.00452F, 0.01686F));
        p_Skirt_76.addOrReplaceChild("cube_76_8", CubeListBuilder.create().mirror().texOffs(118, 62).addBox(-2.92403F, -2.37423F, -2.41875F, 3.25F, 5.0F, 3.825F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.2F, 2.25882F, -2.25907F, 0.26176F, 0.00452F, -0.01686F));
        p_Skirt_76.addOrReplaceChild("cube_76_9", CubeListBuilder.create().mirror().texOffs(103, 43).addBox(-4.19309F, -2.0067F, -2.44375F, 2.25F, 5.3F, 3.625F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 2.25882F, -2.25907F, 0.25462F, -0.06156F, 0.2321F));
        p_Skirt_76.addOrReplaceChild("cube_76_10", CubeListBuilder.create().texOffs(117, 48).addBox(1.94309F, -1.9567F, -2.34375F, 2.25F, 5.3F, 3.875F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.4F, 2.0F, 0.0F, -0.2121F, -0.05147F, -0.2345F));
        p_Skirt_76.addOrReplaceChild("cube_76_11", CubeListBuilder.create().texOffs(103, 54).addBox(0.17403F, -2.37423F, -2.39375F, 2.75F, 5.0F, 3.8F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.2F, 2.0F, 0.0F, -0.21813F, 0.00378F, 0.01704F));
        p_Skirt_76.addOrReplaceChild("cube_76_12", CubeListBuilder.create().mirror().texOffs(120, 72).addBox(-2.92403F, -2.37423F, -2.39375F, 3.25F, 5.0F, 3.875F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.2F, 2.0F, 0.0F, -0.21813F, -0.00378F, -0.01704F));
        p_Skirt_76.addOrReplaceChild("cube_76_13", CubeListBuilder.create().mirror().texOffs(117, 48).addBox(-4.19309F, -2.0067F, -2.44375F, 2.25F, 5.3F, 3.95F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 2.0F, 0.0F, -0.2121F, 0.05147F, 0.2345F));
        p_Skirt_76.addOrReplaceChild("cube_76_14", CubeListBuilder.create().mirror().texOffs(122, 58).addBox(-4.29309F, -0.2067F, 1.56875F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 2.25882F, -0.79657F, -0.25462F, 0.06156F, 0.2321F));
        p_Skirt_76.addOrReplaceChild("cube_76_15", CubeListBuilder.create().mirror().texOffs(117, 58).addBox(-4.29309F, -1.7067F, 1.56875F, 1.0F, 1.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 2.25882F, -0.79657F, -0.25462F, 0.06156F, 0.2321F));
        PartDefinition p_Legs_77 = p_DownBody_75.addOrReplaceChild("Legs", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.0F, -1.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition p_LeftLeg_78 = p_Legs_77.addOrReplaceChild("LeftLeg", CubeListBuilder.create(),
                PartPose.offsetAndRotation(2.1F, 1.5F, 0.0F, 0.0F, 0.0F, 0.0F));
        p_LeftLeg_78.addOrReplaceChild("cube_78_0", CubeListBuilder.create().texOffs(15, 35).addBox(-1.625F, -0.5F, -2.0F, 3.25F, 9.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        p_LeftLeg_78.addOrReplaceChild("cube_78_1", CubeListBuilder.create().texOffs(17, 37).addBox(-1.475F, -0.15F, -1.275F, 2.95F, 2.7F, 2.3F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 7.5F, -1.0F, -0.87266F, 0.0F, 0.0F));
        PartDefinition p_LeftLowerLeg_79 = p_LeftLeg_78.addOrReplaceChild("LeftLowerLeg", CubeListBuilder.create(),
                PartPose.offsetAndRotation(-0.325F, 7.95F, 0.325F, 0.0F, 0.0F, 0.0F));
        p_LeftLowerLeg_79.addOrReplaceChild("cube_79_0", CubeListBuilder.create().texOffs(0, 34).addBox(-1.275F, -0.3F, -2.3F, 3.2F, 9.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        p_LeftLowerLeg_79.addOrReplaceChild("cube_79_1", CubeListBuilder.create().texOffs(101, 30).addBox(-1.40639F, 2.38741F, -2.475F, 2.2F, 5.0F, 4.45F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        p_LeftLowerLeg_79.addOrReplaceChild("cube_79_2", CubeListBuilder.create().mirror().texOffs(101, 30).addBox(-0.11444F, 2.44331F, -2.375F, 2.2F, 5.0F, 4.25F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        p_LeftLowerLeg_79.addOrReplaceChild("cube_79_3", CubeListBuilder.create().mirror().texOffs(117, 41).addBox(3.9F, -4.3F, -2.725F, 1.2F, 5.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-3.875F, 5.7F, -0.3F, 0.0F, 0.0F, 0.17453F));
        PartDefinition p_LeftFoot_80 = p_LeftLowerLeg_79.addOrReplaceChild("LeftFoot", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.4F, 7.675F, -0.025F, 0.0F, 0.0F, 0.0F));
        p_LeftFoot_80.addOrReplaceChild("cube_80_0", CubeListBuilder.create().texOffs(51, 37).addBox(-1.72271F, -0.03458F, -3.19851F, 3.325F, 2.0F, 5.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        p_LeftFoot_80.addOrReplaceChild("cube_80_1", CubeListBuilder.create().texOffs(100, 65).addBox(-1.82271F, 0.71542F, -4.19851F, 3.525F, 2.25F, 6.35F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        p_LeftFoot_80.addOrReplaceChild("cube_80_2", CubeListBuilder.create().texOffs(101, 74).addBox(-1.7375F, -1.475F, -2.875F, 3.475F, 2.0F, 6.1F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(-0.06021F, 0.96542F, -1.14851F, -0.23126F, 0.0F, 0.0F));
        PartDefinition p_RightLeg_81 = p_Legs_77.addOrReplaceChild("RightLeg", CubeListBuilder.create(),
                PartPose.offsetAndRotation(-2.1F, 1.5F, 0.0F, 0.0F, 0.0F, 0.0F));
        p_RightLeg_81.addOrReplaceChild("cube_81_0", CubeListBuilder.create().mirror().texOffs(15, 35).addBox(-1.625F, -0.5F, -2.0F, 3.25F, 9.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        p_RightLeg_81.addOrReplaceChild("cube_81_1", CubeListBuilder.create().mirror().texOffs(17, 37).addBox(-1.475F, -0.15F, -1.275F, 2.95F, 2.7F, 2.3F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 7.5F, -1.0F, -0.87266F, 0.0F, 0.0F));
        PartDefinition p_RightLowerLeg_82 = p_RightLeg_81.addOrReplaceChild("RightLowerLeg", CubeListBuilder.create(),
                PartPose.offsetAndRotation(0.325F, 7.95F, 0.325F, 0.0F, 0.0F, 0.0F));
        p_RightLowerLeg_82.addOrReplaceChild("cube_82_0", CubeListBuilder.create().mirror().texOffs(0, 34).addBox(-1.925F, -0.3F, -2.3F, 3.2F, 9.0F, 4.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        p_RightLowerLeg_82.addOrReplaceChild("cube_82_1", CubeListBuilder.create().mirror().texOffs(101, 30).addBox(-0.79361F, 2.38741F, -2.475F, 2.2F, 5.0F, 4.45F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        p_RightLowerLeg_82.addOrReplaceChild("cube_82_2", CubeListBuilder.create().texOffs(117, 41).addBox(-5.1F, -4.3F, -2.225F, 1.2F, 5.0F, 0.5F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(3.875F, 5.7F, -0.3F, 0.0F, 0.0F, -0.17453F));
        p_RightLowerLeg_82.addOrReplaceChild("cube_82_3", CubeListBuilder.create().texOffs(101, 30).addBox(-2.08556F, 2.44331F, -2.875F, 2.2F, 5.0F, 4.75F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        PartDefinition p_LeftFoot2_83 = p_RightLowerLeg_82.addOrReplaceChild("LeftFoot2", CubeListBuilder.create(),
                PartPose.offsetAndRotation(-0.55F, 7.675F, -0.025F, 0.0F, 0.0F, 0.0F));
        p_LeftFoot2_83.addOrReplaceChild("cube_83_0", CubeListBuilder.create().mirror().texOffs(51, 37).addBox(-1.45229F, -0.03458F, -3.19851F, 3.325F, 2.0F, 5.0F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        p_LeftFoot2_83.addOrReplaceChild("cube_83_1", CubeListBuilder.create().mirror().texOffs(100, 65).addBox(-1.55229F, 0.71542F, -4.19851F, 3.525F, 2.25F, 6.35F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F));
        p_LeftFoot2_83.addOrReplaceChild("cube_83_2", CubeListBuilder.create().mirror().texOffs(101, 74).addBox(-1.7375F, -1.475F, -2.875F, 3.475F, 2.0F, 6.1F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.21021F, 0.96542F, -1.14851F, -0.23126F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 256, 256);
    }

    @Override
    public void setupAnim(RikumiMitaEntity entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root.getAllParts().forEach(ModelPart::resetPose);
        this.allHead.yRot += netHeadYaw * Mth.DEG_TO_RAD;
        this.allHead.xRot += headPitch * Mth.DEG_TO_RAD;
        float walk = Mth.cos(limbSwing * 0.6662F) * 1.15F * limbSwingAmount;
        this.rightArm.xRot += walk;
        this.leftArm.xRot -= walk;
        this.rightLeg.xRot -= walk;
        this.leftLeg.xRot += walk;
        if (entity.isOrderedToSit()) {
            this.rightLeg.xRot = -1.15F;
            this.leftLeg.xRot = -1.15F;
            this.rightArm.xRot *= 0.25F;
            this.leftArm.xRot *= 0.25F;
        }
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {
        this.root.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }
}
