package com.magicalmod.client;

import com.magicalmod.magic.Spell;
import com.magicalmod.net.ModPayloads.CastSpellPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/**
 * Быстрый ввод магии: удерживайте `]`, наведите мышь на сектор с заклинанием,
 * отпустите клавишу — заклинание летит. Не нужно рисовать фигуры.
 */
public final class SpellWheel {
    private SpellWheel() {}

    private static final Spell[] SPELLS = Spell.values();
    private static final int SECTORS = SPELLS.length;
    private static final double STEP = Math.PI * 2.0D / SECTORS;

    private static boolean active = false;
    private static boolean wasDown = false;
    private static int selected = 0;
    private static int lastSelected = 0;

    public static void tick(Minecraft client) {
        LocalPlayer player = client.player;
        boolean down = ModKeys.CAST.isDown();

        if (player == null || client.screen != null) {
            if (active || wasDown) cancel();
            return;
        }

        if (down && !wasDown) {
            active = true;
            selected = lastSelected;
        } else if (active && down) {
            updateSelection(client);
        } else if (!down && wasDown && active) {
            cast(client);
            active = false;
        }

        wasDown = down;
    }

    private static void updateSelection(Minecraft client) {
        double mx = client.mouseHandler.getScaledXPos(client.getWindow());
        double my = client.mouseHandler.getScaledYPos(client.getWindow());
        int cx = client.getWindow().getGuiScaledWidth() / 2;
        int cy = client.getWindow().getGuiScaledHeight() / 2;
        double dx = mx - cx, dy = my - cy;
        if (Math.hypot(dx, dy) < 32.0D) return; // мёртвая зона в центре

        // Нулевой сектор сверху, further по часовой стрелке.
        double angle = Math.atan2(dy, dx) + Math.PI / 2.0D;
        while (angle < 0.0D) angle += Math.PI * 2.0D;
        int index = (int) Math.round(angle / STEP) % SECTORS;
        selected = Math.max(0, Math.min(SECTORS - 1, index));
    }

    private static void cast(Minecraft client) {
        Spell spell = SPELLS[selected];
        lastSelected = selected;
        if (client.getConnection() == null) return;
        ClientPlayNetworking.send(new CastSpellPayload(spell.id()));
        client.gui.setOverlayMessage(Component.literal(
                "§5✧ " + spell.displayName() + " §7(" + Math.round(spell.manaCost()) + " маны)"
        ), false);
    }

    private static void cancel() {
        active = false;
        wasDown = false;
    }

    public static void render(GuiGraphics graphics, DeltaTracker delta) {
        if (!active) return;
        Minecraft client = Minecraft.getInstance();
        int cx = graphics.guiWidth() / 2;
        int cy = graphics.guiHeight() / 2;
        int radius = 88;

        // Затемнение центра и рамка.
        graphics.fill(cx - 78, cy - 78, cx + 78, cy + 78, 0xD0110E1D);
        graphics.fill(cx - 80, cy - 80, cx + 80, cy - 78, 0xFF9B5CFF);
        graphics.fill(cx - 80, cy + 78, cx + 80, cy + 80, 0xFF9B5CFF);
        graphics.fill(cx - 80, cy - 80, cx - 78, cy + 80, 0xFF9B5CFF);
        graphics.fill(cx + 78, cy - 80, cx + 80, cy + 80, 0xFF9B5CFF);

        for (int i = 0; i < SECTORS; i++) {
            Spell spell = SPELLS[i];
            double angle = i * STEP - Math.PI / 2.0D;
            int x = cx + (int) Math.round(Math.cos(angle) * radius);
            int y = cy + (int) Math.round(Math.sin(angle) * radius);
            boolean chosen = i == selected;
            int border = chosen ? 0xFFFFFFFF : (spell.color() | 0xFF000000);
            int box = chosen ? 0xFF3B2C5C : 0xDD1A162B;

            graphics.fill(x - 20, y - 14, x + 20, y + 14, border);
            graphics.fill(x - 18, y - 12, x + 18, y + 12, box);
            String label = spell.displayName().substring(0, Math.min(2, spell.displayName().length()));
            graphics.drawCenteredString(client.font, label, x, y - 5, border);
            graphics.drawCenteredString(client.font, String.valueOf(i + 1), x, y + 4,
                    chosen ? 0xFFF0C0FF : 0xFF9E90C0);
        }

        Spell spell = SPELLS[selected];
        graphics.drawCenteredString(client.font, spell.displayName(), cx, cy - 12, 0xFFFFFFFF);
        graphics.drawCenteredString(client.font,
                "§6" + Math.round(spell.manaCost()) + " маны §7· отпустите ]",
                cx, cy + 4, 0xFFB9A8D8);
        graphics.drawCenteredString(client.font,
                "§8Наведите мышь на сектор", cx, cy + 20, 0xFF8A7BA8);
    }
}
