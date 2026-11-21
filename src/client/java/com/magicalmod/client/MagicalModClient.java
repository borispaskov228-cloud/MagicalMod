package com.magicalmod.client;

import com.magicalmod.MagicalMod;
import com.magicalmod.net.ModPayloads.CastFxPayload;
import com.magicalmod.net.ModPayloads.ManaUpdatePayload;
import com.magicalmod.net.ModPayloads.RitualActionPayload;
import com.magicalmod.net.ModPayloads.RitualStatusPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudStatusBarHeightRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.KeyMapping;

public class MagicalModClient implements ClientModInitializer {
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(MagicalMod.id("category"));

    /** Меню круга — G. Каст жеста — ] (не конфликтует со стандартными биндами). */
    private static final KeyMapping RITUAL_KEY = KeyBindingHelper.registerKeyBinding(
            new KeyMapping("key.magicalmod.ritual", InputConstants.KEY_G, CATEGORY)
    );
    private static final KeyMapping CAST_KEY = KeyBindingHelper.registerKeyBinding(
            new KeyMapping("key.magicalmod.cast", InputConstants.KEY_RBRACKET, CATEGORY)
    );

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                if (RITUAL_KEY.consumeClick()) {
                    ClientPlayNetworking.send(new RitualActionPayload("open"));
                }
                if (CAST_KEY.consumeClick()) {
                    GestureDetector.tryCast(client);
                }
            }
            GestureDetector.tick(client);
            SpellAnimations.tick(client);
        });

        ClientPlayNetworking.registerGlobalReceiver(RitualStatusPayload.TYPE, (payload, context) ->
                context.client().setScreen(new RitualScreen(payload.text()))
        );
        ClientPlayNetworking.registerGlobalReceiver(CastFxPayload.TYPE, (payload, context) ->
                SpellAnimations.add(payload, context.client())
        );
        ClientPlayNetworking.registerGlobalReceiver(ManaUpdatePayload.TYPE, (payload, context) ->
                ManaHud.setMana(payload.mana())
        );

        HudElementRegistry.attachElementAfter(VanillaHudElements.FOOD_BAR, ManaHud.ID, ManaHud::render);
        HudStatusBarHeightRegistry.addRight(ManaHud.ID, player -> 10);
    }
}
