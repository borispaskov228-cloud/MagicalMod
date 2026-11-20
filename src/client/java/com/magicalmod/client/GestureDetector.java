package com.magicalmod.client;

import java.util.ArrayList;
import java.util.List;

import com.magicalmod.magic.Gesture;
import com.magicalmod.net.ModPayloads.CastSpellPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/** Распознаёт жесты по действиям игрока: приседание, прыжок, резкий поворот влево/вправо. */
public final class GestureDetector {
    private static final long WINDOW_MS = 2500L;
    private static final long TURN_COOLDOWN_MS = 180L;
    private static final float TURN_THRESHOLD = 28.0F;

    private static final List<Gesture.Step> SEQUENCE = new ArrayList<>();
    private static long lastStepMs = 0L;
    private static long lastTurnMs = 0L;
    private static int lastTurnDir = 0;
    private static float turnAccum = 0.0F;
    private static boolean prevCrouch = false;
    private static boolean prevJump = false;
    private static float prevYaw = 0.0F;
    private static boolean initialized = false;

    private GestureDetector() {}

    public static void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.screen != null) {
            resetTracking();
            return;
        }

        float yaw = player.getYRot();
        boolean crouch = player.isCrouching();
        boolean jump = player.input.keyPresses.jump();

        if (!initialized) {
            initialized = true;
            prevYaw = yaw;
            prevCrouch = crouch;
            prevJump = jump;
            return;
        }

        long now = System.currentTimeMillis();
        if (!SEQUENCE.isEmpty() && now - lastStepMs > WINDOW_MS) SEQUENCE.clear();

        if (crouch && !prevCrouch) add(Gesture.Step.CROUCH, now, client);
        if (jump && !prevJump) add(Gesture.Step.JUMP, now, client);

        float delta = yaw - prevYaw;
        while (delta > 180.0F) delta -= 360.0F;
        while (delta < -180.0F) delta += 360.0F;

        if (Math.signum(delta) != Math.signum(turnAccum) && turnAccum != 0.0F) turnAccum = 0.0F;
        turnAccum += delta;

        if (Math.abs(turnAccum) >= TURN_THRESHOLD && now - lastTurnMs > TURN_COOLDOWN_MS) {
            int dir = turnAccum > 0 ? 1 : -1;
            // повтор того же направления не считаем — нужен именно «взмах» туда-обратно
            if (dir != lastTurnDir || now - lastTurnMs > 400L) {
                add(dir > 0 ? Gesture.Step.RIGHT : Gesture.Step.LEFT, now, client);
                lastTurnDir = dir;
                lastTurnMs = now;
                turnAccum = 0.0F;
            }
        }

        prevYaw = yaw;
        prevCrouch = crouch;
        prevJump = jump;
    }

    private static void add(Gesture.Step step, long now, Minecraft client) {
        SEQUENCE.add(step);
        lastStepMs = now;

        for (Gesture gesture : Gesture.values()) {
            if (gesture.matches(SEQUENCE)) {
                SEQUENCE.clear();
                turnAccum = 0.0F;
                ClientPlayNetworking.send(new CastSpellPayload(gesture.spell().id()));
                client.gui.setOverlayMessage(Component.literal(
                        "§5✧ Жест распознан: " + gesture.spell().displayName()
                ), false);
                return;
            }
        }

        StringBuilder sb = new StringBuilder("§7Жест: ");
        for (Gesture.Step s : SEQUENCE) sb.append(s.symbol()).append(" → ");
        sb.setLength(Math.max(0, sb.length() - 3));
        client.gui.setOverlayMessage(Component.literal(sb.toString()), false);
    }

    private static void resetTracking() {
        SEQUENCE.clear();
        turnAccum = 0.0F;
        initialized = false;
    }
}
