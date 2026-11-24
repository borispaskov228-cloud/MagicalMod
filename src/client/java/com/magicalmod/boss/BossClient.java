package com.magicalmod.boss;

import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;

/** Helper to be called from your ClientModInitializer. */
public final class BossClient {
    private BossClient() {}

    public static void register() {
        EntityRendererRegistry.register(BossContent.BOSS_ENTITY_TYPE, ArchmageBossRenderer::new);
    }
}
