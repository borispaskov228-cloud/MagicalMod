package com.magicalmod.client;

import java.util.ArrayList;
import java.util.List;

import com.magicalmod.magic.Gesture;
import com.magicalmod.net.ModPayloads.CastSpellPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;

/**
 * Распознаёт жесты только клавиатурой: приседание, прыжок, A/D.
 * Мышь не используется — прицел не сбивается.
 */
public final class GestureDetector {
    private static final long WINDOW_MS = 5000L;
    private static final long CAST_COOLDOWN_MS = 500L;

    private static final List<Gesture.Step> SEQUENCE = new ArrayList<>();
    private static long lastStepMs = 0L;
    private static long lastCastMs = 0L;
    private static boolean prevCrouch = false;
    private static boolean prevJump = false;
    private static boolean prevLeft = false;
    private static boolean prevRight = false;
    private static boolean initialized = false;

    private GestureDetector() {}

    public static void tick(Minecraft client) {
        LocalPlayer player = client.player;
        if (player == null || client.screen != null) {
            resetTracking();
            return;
        }

        boolean crouch = player.isCrouching();
        boolean jump = player.input.keyPresses.jump();
        boolean left = client.options.keyLeft.isDown();
        boolean right = client.options.keyRight.isDown();

        if (!initialized) {
            initialized = true;
            prevCrouch = crouch;
            prevJump = jump;
            prevLeft = left;
            prevRight = right;
            return;
        }

        long now = System.currentTimeMillis();
        if (!SEQUENCE.isEmpty() && now - lastStepMs > WINDOW_MS) SEQUENCE.clear();

        if (crouch && !prevCrouch) add(Gesture.Step.CROUCH, now);
        if (jump && !prevJump) add(Gesture.Step.JUMP, now);
        if (left && !prevLeft) add(Gesture.Step.LEFT, now);
        if (right && !prevRight) add(Gesture.Step.RIGHT, now);

        prevCrouch = crouch;
        prevJump = jump;
        prevLeft = left;
        prevRight = right;
    }

    private static void add(Gesture.Step step, long now) {
        SEQUENCE.add(step);
        lastStepMs = now;
    }

    /** Вызывается по нажатию отдельной клавиши каста. */
    public static void tryCast(Minecraft client) {
        if (client.player == null || client.screen != null) return;
        long now = System.currentTimeMillis();
        if (now - lastCastMs < CAST_COOLDOWN_MS) return;
        lastCastMs = now;

        for (Gesture gesture : Gesture.values()) {
            if (gesture.matches(SEQUENCE)) {
                SEQUENCE.clear();
                ClientPlayNetworking.send(new CastSpellPayload(gesture.spell().id()));
                client.gui.setOverlayMessage(Component.literal(
                        "§5✧ " + gesture.spell().displayName()
                ), false);
                return;
            }
        }

        SEQUENCE.clear();
        client.gui.setOverlayMessage(Component.literal("§cЖест не распознан"), false);
    }

    private static void resetTracking() {
        SEQUENCE.clear();
        initialized = false;
    }
}
