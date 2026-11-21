package com.magicalmod.client;

import com.magicalmod.MagicalMod;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.entity.player.Player;

/** Полоса маны над шкалой голода. */
public final class ManaHud {
    public static final net.minecraft.resources.Identifier ID = MagicalMod.id("mana_bar");

    private static float mana = 0.0F;
    private static boolean initialized = false;

    private ManaHud() {}

    public static void setMana(float value) {
        mana = value;
        initialized = true;
    }

    /** Должен совпадать с условием отрисовки — иначе height provider сдвинет ванильные шкалы впустую. */
    public static boolean isVisible() {
        Minecraft client = Minecraft.getInstance();
        return initialized
                && client.player != null
                && !client.options.hideGui
                && client.getCameraEntity() instanceof Player;
    }

    public static void render(GuiGraphics graphics, DeltaTracker tracker) {
        if (!isVisible()) return;
        Minecraft client = Minecraft.getInstance();

        int x = graphics.guiWidth() / 2 + 91;
        int y = graphics.guiHeight()
                - net.fabricmc.fabric.api.client.rendering.v1.hud.HudStatusBarHeightRegistry.getHeight(ID);
        int width = 81;
        int height = 7;

        graphics.fill(x - 1, y - 1, x + width + 1, y + height + 1, 0xFF1A0A2E);
        graphics.fill(x, y, x + width, y + height, 0xFF3B1A5C);

        int filled = (int) (width * Math.max(0.0F, Math.min(1.0F, mana / 100.0F)));
        if (filled > 0) graphics.fill(x, y, x + filled, y + height, 0xFFB15CFF);

        graphics.drawString(client.font, "MANA " + Math.round(mana), x, y - 10, 0xD9A6FF);
    }
}
