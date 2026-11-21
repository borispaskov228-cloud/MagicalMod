package com.magicalmod.client;

import com.magicalmod.MagicalMod;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;

/** Все бинды мода в одном месте; , и . свободны в ванильном игре. */
public final class ModKeys {
    private ModKeys() {}

    public static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(MagicalMod.id("category"));

    /** Меню круга. */
    public static final KeyMapping RITUAL = KeyBindingHelper.registerKeyBinding(
            new KeyMapping("key.magicalmod.ritual", InputConstants.KEY_G, CATEGORY));

    /** Каст по введённому жесту. */
    public static final KeyMapping CAST = KeyBindingHelper.registerKeyBinding(
            new KeyMapping("key.magicalmod.cast", InputConstants.KEY_RBRACKET, CATEGORY));

    /** Шаг жеста «влево» (,). */
    public static final KeyMapping GESTURE_LEFT = KeyBindingHelper.registerKeyBinding(
            new KeyMapping("key.magicalmod.gesture_left", InputConstants.KEY_COMMA, CATEGORY));

    /** Шаг жеста «вправо» (.). */
    public static final KeyMapping GESTURE_RIGHT = KeyBindingHelper.registerKeyBinding(
            new KeyMapping("key.magicalmod.gesture_right", InputConstants.KEY_PERIOD, CATEGORY));
}
