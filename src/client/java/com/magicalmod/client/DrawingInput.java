package com.magicalmod.client;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.magicalmod.magic.Spell;
import com.magicalmod.net.ModPayloads.CastSpellPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/** Рисование магии: удерживайте клавишу, ведите мышь, отпустите — заклинание кастуется. */
public final class DrawingInput {
    private DrawingInput() {}

    private static final Map<String, String> SHAPE_TO_SPELL = Map.of(
            "circle", Spell.FIREBALL.id(),
            "triangle", Spell.FROST.id(),
            "v", Spell.HEAL.id(),
            "zigzag", Spell.LIGHTNING.id(),
            "spiral", Spell.POISON.id(),
            "wave", Spell.WATER.id(),
            "line", Spell.WIND.id(),
            "square", Spell.SHIELD.id(),
            "line_down", Spell.METEOR.id(),
            "inward", Spell.PULL.id()
    );

    private static final List<DrawingRecognizer.Point> POINTS = new ArrayList<>();
    private static boolean drawing = false;
    private static boolean wasDown = false;
    private static float penX = 0.0F;
    private static float penY = 0.0F;
    private static float prevYaw = 0.0F;
    private static float prevPitch = 0.0F;

    public static void tick(Minecraft client) {
        LocalPlayer player = client.player;

        // Выход из мира/открытие экрана во время рисования — тихо отменяем жест
        if (player == null || client.screen != null) {
            if (drawing || wasDown) cancel();
            return;
        }

        boolean down = ModKeys.CAST.isDown();
        if (down && !wasDown) start(player);
        else if (down && drawing) record(player);
        else if (!down && wasDown) finish(client);

        wasDown = down;
    }

    private static void start(LocalPlayer player) {
        POINTS.clear();
        drawing = true;
        penX = 0.0F;
        penY = 0.0F;
        prevYaw = player.getYRot();
        prevPitch = player.getXRot();
        POINTS.add(new DrawingRecognizer.Point(penX, penY));
    }

    private static void record(LocalPlayer player) {
        float yaw = player.getYRot();
        float pitch = player.getXRot();

        float dyaw = yaw - prevYaw;
        while (dyaw > 180.0F) dyaw -= 360.0F;
        while (dyaw < -180.0F) dyaw += 360.0F;
        float dpitch = pitch - prevPitch;

        prevYaw = yaw;
        prevPitch = pitch;

        if (Math.abs(dyaw) < 0.01F && Math.abs(dpitch) < 0.01F) return;

        penX += dyaw;
        penY += dpitch;
        POINTS.add(new DrawingRecognizer.Point(penX, penY));
    }

    private static void finish(Minecraft client) {
        drawing = false;
        wasDown = false;
        String shape = DrawingRecognizer.recognize(POINTS);
        POINTS.clear();

        if (shape.isEmpty() || client.getConnection() == null) return;

        String spellId = SHAPE_TO_SPELL.get(shape);
        if (spellId == null) {
            client.gui.setOverlayMessage(Component.literal("§cФорма не распознана"), false);
            return;
        }

        ClientPlayNetworking.send(new CastSpellPayload(spellId));
        Spell spell = Spell.byId(spellId);
        client.gui.setOverlayMessage(Component.literal(
                "§5✧ " + (spell == null ? spellId : spell.displayName())
        ), false);
    }

    private static void cancel() {
        POINTS.clear();
        drawing = false;
        wasDown = false;
    }

    /** Отрисовка нарисованного пути поверх HUD. */
    public static void render(GuiGraphics graphics, DeltaTracker tracker) {
        if (!drawing || POINTS.size() < 2) return;
        Minecraft client = Minecraft.getInstance();
        int cx = graphics.guiWidth() / 2;
        int cy = graphics.guiHeight() / 2;
        float scale = 2.0F;

        DrawingRecognizer.Point prev = POINTS.get(0);
        for (int i = 1; i < POINTS.size(); i++) {
            DrawingRecognizer.Point p = POINTS.get(i);
            int x1 = cx + Math.round(prev.x() * scale);
            int y1 = cy + Math.round(prev.y() * scale);
            int x2 = cx + Math.round(p.x() * scale);
            int y2 = cy + Math.round(p.y() * scale);
            graphics.fill(x1 - 1, y1 - 1, x2 + 1, y2 + 1, 0x66B15CFF);
            graphics.fill(x1, y1, x1 + 2, y1 + 2, 0xFFD9A6FF);
            prev = p;
        }
        graphics.drawString(client.font, "Рисуем магию...", cx - 40, cy - 60, 0xFFD9A6FF);
    }
}
