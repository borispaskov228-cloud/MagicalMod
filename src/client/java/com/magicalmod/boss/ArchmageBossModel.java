package com.magicalmod.boss;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/** Парящая фигура в облачении, фолиант и три кристалла маны. */
public final class ArchmageBossModel extends EntityModel<ArchmageBossRenderer.ArchmageRenderState> {
    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart hood;
    private final ModelPart book;
    private final ModelPart crystal1;
    private final ModelPart crystal2;
    private final ModelPart crystal3;

    public ArchmageBossModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = root.getChild("head");
        this.hood = root.getChild("hood");
        this.book = root.getChild("book");
        this.crystal1 = root.getChild("crystal1");
        this.crystal2 = root.getChild("crystal2");
        this.crystal3 = root.getChild("crystal3");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();

        root.addOrReplaceChild("body",
                CubeListBuilder.create().texOffs(0, 0)
                        .addBox(-8.0F, -24.0F, -5.0F, 16.0F, 24.0F, 10.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, 24.0F, 0.0F));

        root.addOrReplaceChild("head",
                CubeListBuilder.create().texOffs(0, 34)
                        .addBox(-6.0F, -10.0F, -6.0F, 12.0F, 12.0F, 12.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, 30.0F, 0.0F));

        root.addOrReplaceChild("hood",
                CubeListBuilder.create().texOffs(48, 34)
                        .addBox(-7.0F, -11.0F, -7.0F, 14.0F, 13.0F, 14.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, 30.0F, 0.0F));

        root.addOrReplaceChild("book",
                CubeListBuilder.create().texOffs(0, 58)
                        .addBox(-6.0F, -8.0F, -1.0F, 12.0F, 16.0F, 2.0F, CubeDeformation.NONE),
                PartPose.offset(15.0F, 23.0F, 2.0F));

        root.addOrReplaceChild("crystal1",
                CubeListBuilder.create().texOffs(40, 58)
                        .addBox(-3.0F, -6.0F, -3.0F, 6.0F, 12.0F, 6.0F, CubeDeformation.NONE),
                PartPose.offset(-13.0F, 27.0F, -3.0F));

        root.addOrReplaceChild("crystal2",
                CubeListBuilder.create().texOffs(64, 58)
                        .addBox(-3.0F, -6.0F, -3.0F, 6.0F, 12.0F, 6.0F, CubeDeformation.NONE),
                PartPose.offset(13.0F, 30.0F, -3.0F));

        root.addOrReplaceChild("crystal3",
                CubeListBuilder.create().texOffs(88, 58)
                        .addBox(-3.0F, -6.0F, -3.0F, 6.0F, 12.0F, 6.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, 38.0F, -6.0F));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(ArchmageBossRenderer.ArchmageRenderState state) {
        float t = state.ageInTicks;
        float bob = (float) Math.sin(t * 0.05F) * 2.0F;

        this.body.y = 24.0F + bob;
        this.head.y = 30.0F + bob;
        this.hood.y = this.head.y;

        this.head.yRot = state.yRot * 0.017453292F;
        this.head.xRot = state.xRot * 0.017453292F;
        this.hood.yRot = this.head.yRot;
        this.hood.xRot = this.head.xRot;

        this.book.y = 23.0F + (float) Math.sin(t * 0.07F) * 2.5F;
        this.book.yRot = t * 0.03F;
        this.book.zRot = (float) Math.sin(t * 0.04F) * 0.14F;

        this.crystal1.yRot = t * 0.05F;
        this.crystal2.yRot = -t * 0.04F;
        this.crystal3.yRot = t * 0.06F;
        this.crystal1.zRot = (float) Math.sin(t * 0.06F) * 0.2F;
        this.crystal2.zRot = (float) Math.cos(t * 0.05F) * 0.2F;

        if (state.phase >= 3) {
            this.body.yRot = (float) Math.sin(t * 0.12F) * 0.12F;
            this.book.yRot = t * 0.09F;
        } else {
            this.body.yRot = 0.0F;
        }
    }
}
