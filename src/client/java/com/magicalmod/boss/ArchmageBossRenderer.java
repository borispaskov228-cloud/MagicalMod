package com.magicalmod.boss;

import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;

/** Рендер парящего архимага: модель без регистрации отдельного ModelLayer. */
public final class ArchmageBossRenderer extends MobRenderer<
        ArchmageBoss, ArchmageBossRenderer.ArchmageRenderState, ArchmageBossModel> {
    private static final Identifier TEXTURE =
            Identifier.fromNamespaceAndPath("magicalmod", "textures/entity/archmage_boss.png");

    public ArchmageBossRenderer(EntityRendererProvider.Context context) {
        super(context, new ArchmageBossModel(ArchmageBossModel.createBodyLayer().bakeRoot()), 0.5F);
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
