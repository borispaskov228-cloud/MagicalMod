package com.magicalmod.boss;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/** Рендер архимага: модель без регистрации отдельного ModelLayer + свечение на отдельном слое. */
public final class ArchmageBossRenderer extends MobRenderer<
        ArchmageBoss, ArchmageBossRenderer.ArchmageRenderState, ArchmageBossModel> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("magicalmod", "textures/entity/archmage_boss.png");
    private static final Identifier GLOW_TEXTURE =
            Identifier.fromNamespaceAndPath("magicalmod", "textures/entity/archmage_boss_glow.png");

    public ArchmageBossRenderer(EntityRendererProvider.Context context) {
        super(context, new ArchmageBossModel(ArchmageBossModel.createBodyLayer().bakeRoot()), 0.5F);
        this.addLayer(new RenderLayer<ArchmageRenderState, ArchmageBossModel>(this) {
            @Override
            public void submit(PoseStack poseStack, SubmitNodeCollector collector, int packedLight,
                               ArchmageRenderState state, float yRot, float xRot) {
                if (state.isInvisible) {
                    return;
                }
                collector.order(0).submitModel(
                        this.getParentModel(), state, poseStack,
                        RenderTypes.eyes(GLOW_TEXTURE),
                        packedLight,
                        LivingEntityRenderer.getOverlayCoords(state, 0.0F),
                        -1, null, state.outlineColor, null);
            }
        });
    }

    @Override
    public ArchmageRenderState createRenderState() {
        return new ArchmageRenderState();
    }

    @Override
    public void extractRenderState(ArchmageBoss boss, ArchmageRenderState state, float partialTick) {
        super.extractRenderState(boss, state, partialTick);
        state.phase = boss.getPhase();
    }

    @Override
    public Identifier getTextureLocation(ArchmageRenderState state) {
        return TEXTURE;
    }

    public static final class ArchmageRenderState extends LivingEntityRenderState {
        public int phase = 1;
    }
}
