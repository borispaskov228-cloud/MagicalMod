package com.magicalmod.boss;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;

/**
 * Модель босса, перенесённая из Blockbench-файла {@code void_lich_boss.bbmodel}.
 *
 * <p>Координаты Blockbench (Y вверх, стопы в y=0) переведены в пространство моделей Minecraft
 * (Y вниз, стопы в y=24): {@code yMc = 24 - yBb}. Повороты X и Z при этом меняют знак.
 * Пить развёрнута на 180° по Y рендером, ось Z оставлена как в Blockbench.
 */
public final class ArchmageBossModel extends EntityModel<ArchmageBossRenderer.ArchmageRenderState> {
    private static final float DEG_TO_RAD = (float) Math.PI / 180.0F;
    /** Model-space Y corresponding to Blockbench y=0 (feet). */
    private static final float ROOT_Y = 24.0F;

    private final ModelPart root;
    private final ModelPart torso;
    private final ModelPart head;
    private final ModelPart leftArm;
    private final ModelPart rightArm;
    private final ModelPart tailCloak;

    public ArchmageBossModel(ModelPart root) {
        super(root);
        this.root = root.getChild("root");
        this.torso = this.root.getChild("torso");
        this.head = this.torso.getChild("head");
        this.leftArm = this.torso.getChild("left_arm");
        this.rightArm = this.torso.getChild("right_arm");
        this.tailCloak = this.root.getChild("tail_cloak");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition meshRoot = mesh.getRoot();

        // Blockbench root bone: pivot (0, 0, 0) -> model (0, 24, 0).
        PartDefinition root = meshRoot.addOrReplaceChild("root", CubeListBuilder.create(),
                PartPose.offset(0.0F, ROOT_Y, 0.0F));

        // Torso: Blockbench pivot (0, 46, 0); offset from root is -46 by Y (flipped).
        PartDefinition torso = root.addOrReplaceChild("torso", CubeListBuilder.create()
                .texOffs(64, 0)
                .addBox(-18.0F, -18.0F, -10.0F, 36.0F, 18.0F, 20.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, -46.0F, 0.0F));

        // Rotated elements get their own pivot (Blockbench element "origin").
        torso.addOrReplaceChild("void_core", CubeListBuilder.create()
                        .texOffs(0, 32)
                        .addBox(-5.0F, -5.0F, -5.0F, 10.0F, 10.0F, 10.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(0.0F, -7.0F, 0.0F,
                        rad(-45.0F), rad(45.0F), 0.0F));
        torso.addOrReplaceChild("rib_plate_l", CubeListBuilder.create()
                        .texOffs(64, 28)
                        .addBox(-3.0F, -8.0F, -2.0F, 6.0F, 15.0F, 4.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(11.0F, -6.0F, -10.0F,
                        0.0F, rad(-15.0F), 0.0F));
        torso.addOrReplaceChild("rib_plate_r", CubeListBuilder.create()
                        .texOffs(64, 28)
                        .addBox(-3.0F, -8.0F, -2.0F, 6.0F, 15.0F, 4.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-11.0F, -6.0F, -10.0F,
                        0.0F, rad(15.0F), 0.0F));

        // Head: Blockbench pivot (0, 64, 0), i.e. +18 above the torso pivot.
        PartDefinition head = torso.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 0)
                        .addBox(-7.0F, -16.0F, -7.0F, 14.0F, 16.0F, 14.0F, CubeDeformation.NONE)
                        .texOffs(0, 0)
                        .addBox(-11.0F, -22.0F, -10.0F, 22.0F, 24.0F, 20.0F,
                                new CubeDeformation(0.5F)),
                PartPose.offset(0.0F, -18.0F, 0.0F));

        head.addOrReplaceChild("floating_horn_l", CubeListBuilder.create()
                        .texOffs(48, 0)
                        .addBox(-2.0F, -14.0F, -2.0F, 4.0F, 14.0F, 4.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(15.0F, -10.0F, 0.0F,
                        0.0F, 0.0F, rad(12.0F)));
        head.addOrReplaceChild("floating_horn_r", CubeListBuilder.create()
                        .texOffs(48, 0)
                        .addBox(-2.0F, -14.0F, -2.0F, 4.0F, 14.0F, 4.0F, CubeDeformation.NONE),
                PartPose.offsetAndRotation(-15.0F, -10.0F, 0.0F,
                        0.0F, 0.0F, rad(-12.0F)));

        // Arms: Blockbench pivots (+/-22, 60, 0), +14 above the torso pivot.
        PartDefinition leftArm = torso.addOrReplaceChild("left_arm", CubeListBuilder.create()
                        .texOffs(0, 56)
                        .addBox(-4.0F, -6.0F, -8.0F, 16.0F, 16.0F, 16.0F, CubeDeformation.NONE)
                        .texOffs(32, 56)
                        .addBox(-1.0F, 14.0F, -5.0F, 10.0F, 24.0F, 10.0F, CubeDeformation.NONE)
                        .texOffs(0, 80)
                        .addBox(-2.0F, 40.0F, -6.0F, 12.0F, 12.0F, 12.0F, CubeDeformation.NONE)
                        .texOffs(48, 88)
                        .addBox(-2.0F, 52.0F, -5.0F, 3.0F, 8.0F, 3.0F, CubeDeformation.NONE)
                        .addBox(7.0F, 52.0F, -5.0F, 3.0F, 8.0F, 3.0F, CubeDeformation.NONE),
                PartPose.offset(22.0F, -14.0F, 0.0F));

        PartDefinition rightArm = torso.addOrReplaceChild("right_arm", CubeListBuilder.create()
                        .texOffs(0, 56)
                        .addBox(-12.0F, -6.0F, -8.0F, 16.0F, 16.0F, 16.0F, CubeDeformation.NONE)
                        .texOffs(32, 56)
                        .addBox(-9.0F, 14.0F, -5.0F, 10.0F, 24.0F, 10.0F, CubeDeformation.NONE)
                        .texOffs(0, 80)
                        .addBox(-10.0F, 40.0F, -6.0F, 12.0F, 12.0F, 12.0F, CubeDeformation.NONE)
                        .texOffs(48, 88)
                        .addBox(-10.0F, 52.0F, -5.0F, 3.0F, 8.0F, 3.0F, CubeDeformation.NONE)
                        .addBox(-1.0F, 52.0F, -5.0F, 3.0F, 8.0F, 3.0F, CubeDeformation.NONE),
                PartPose.offset(-22.0F, -14.0F, 0.0F));

        // Cloak: Blockbench pivot (0, 46, 0), same height as the torso pivot.
        root.addOrReplaceChild("tail_cloak", CubeListBuilder.create()
                        .texOffs(64, 48)
                        .addBox(-9.0F, 0.0F, -7.0F, 18.0F, 12.0F, 14.0F, CubeDeformation.NONE)
                        .texOffs(64, 64)
                        .addBox(-10.0F, 12.0F, -8.0F, 20.0F, 26.0F, 2.0F, CubeDeformation.NONE)
                        .texOffs(84, 64)
                        .addBox(-12.0F, 12.0F, 6.0F, 24.0F, 28.0F, 2.0F, CubeDeformation.NONE)
                        .texOffs(108, 64)
                        .addBox(8.0F, 12.0F, -7.0F, 2.0F, 24.0F, 14.0F, CubeDeformation.NONE)
                        .addBox(-10.0F, 12.0F, -7.0F, 2.0F, 24.0F, 14.0F, CubeDeformation.NONE),
                PartPose.offset(0.0F, -46.0F, 0.0F));

        return LayerDefinition.create(mesh, 128, 128);
    }

    @Override
    public void setupAnim(ArchmageBossRenderer.ArchmageRenderState state) {
        super.setupAnim(state); // reset to the baked pose

        float seconds = state.ageInTicks / 20.0F;
        float time = seconds * (state.phase >= 3 ? 1.5F : 1.0F);

        // animation.void_lich_boss.idle: 4 s loop, keys at 0 / 2 / 4 s (Blockbench degrees).
        float idle = time % 4.0F;
        this.root.y = ROOT_Y - curve3(idle, 0.0F, 3.0F);
        this.torso.xRot = radX(curve3(idle, 2.0F, -2.0F));
        this.leftArm.xRot = radX(curve3(idle, 0.0F, 5.0F));
        this.leftArm.zRot = radZ(curve3(idle, -5.0F, -10.0F));
        this.rightArm.xRot = radX(curve3(idle, 0.0F, 5.0F));
        this.rightArm.zRot = radZ(curve3(idle, 5.0F, 10.0F));
        this.tailCloak.xRot = radX(curve3(idle, 5.0F, 12.0F));

        // animation.void_lich_boss.attack_beam: keys at 0 / 0.6 / 2.0 / 2.5 s.
        // Phase >= 2 casts every 4-second cycle; phase 1 every third cycle.
        float beam = time % 4.0F;
        boolean attacking = beam < 2.5F && (state.phase >= 2 || (long) (time / 4.0F) % 3L == 0L);
        if (attacking) {
            this.head.xRot = radX(attackCurve(beam, 0.0F, -25.0F, -25.0F, 0.0F));
            this.leftArm.xRot = radX(attackCurve(beam, 0.0F, -30.0F, -30.0F, 0.0F));
            this.leftArm.zRot = radZ(attackCurve(beam, -5.0F, -55.0F, -55.0F, -5.0F));
            this.rightArm.xRot = radX(attackCurve(beam, 0.0F, -30.0F, -30.0F, 0.0F));
            this.rightArm.zRot = radZ(attackCurve(beam, 5.0F, 55.0F, 55.0F, 5.0F));
        }
        this.head.xRot += state.xRot * DEG_TO_RAD;
        this.head.yRot += state.yRot * DEG_TO_RAD;
    }

    /** Linear interpolation of a 4-second animation with keys at 0, 2 and 4 seconds. */
    private static float curve3(float t, float a, float b) {
        float u = t % 4.0F;
        return u < 2.0F ? a + (b - a) * (u * 0.5F) : b + (a - b) * ((u - 2.0F) * 0.5F);
    }

    /** Linear interpolation of the beam keys at 0, 0.6, 2.0 and 2.5 seconds. */
    private static float attackCurve(float t, float a, float b, float c, float d) {
        if (t <= 0.6F) {
            return a + (b - a) * (t / 0.6F);
        }
        if (t <= 2.0F) {
            return b + (c - b) * ((t - 0.6F) / 1.4F);
        }
        return c + (d - c) * ((t - 2.0F) / 0.5F);
    }

    private static float rad(float bedrockDegrees) {
        return bedrockDegrees * DEG_TO_RAD;
    }

    /** Mirrored Y inverts rotations around X and Z. */
    private static float radX(float bedrockDegrees) {
        return -rad(bedrockDegrees);
    }

    /** Mirrored Y inverts rotations around X and Z. */
    private static float radZ(float bedrockDegrees) {
        return -rad(bedrockDegrees);
    }
}
