package com.magicalmod.client;

import com.magicalmod.net.ModPayloads.CastFxPayload;
import com.magicalmod.net.ModPayloads.ManaUpdatePayload;
import com.magicalmod.net.ModPayloads.RitualActionPayload;
import com.magicalmod.net.ModPayloads.RitualStatusPayload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudStatusBarHeightRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;

public class MagicalModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // Регистрация биндов должна произойти до инициализации GameOptions
        ModKeys.init();
        com.magicalmod.boss.BossClient.register();

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player != null) {
                if (ModKeys.RITUAL.consumeClick()) {
                    ClientPlayNetworking.send(new RitualActionPayload("open"));
                }
            }
            DrawingInput.tick(client);
            SpellAnimations.tick(client);
        });

        ClientPlayNetworking.registerGlobalReceiver(RitualStatusPayload.TYPE, (payload, context) ->
                context.client().setScreen(new RitualScreen(payload.text()))
        );
        ClientPlayNetworking.registerGlobalReceiver(CastFxPayload.TYPE, (payload, context) ->
                SpellAnimations.add(payload, context.client())
        );
        ClientPlayNetworking.registerGlobalReceiver(ManaUpdatePayload.TYPE, (payload, context) ->
                ManaHud.setMana(payload.mana(), payload.maxMana(), payload.level(), payload.xp())
        );

        HudElementRegistry.addLast(com.magicalmod.MagicalMod.id("drawing_overlay"), DrawingInput::render);
        HudElementRegistry.attachElementAfter(VanillaHudElements.FOOD_BAR, ManaHud.ID, ManaHud::render);
        HudStatusBarHeightRegistry.addRight(ManaHud.ID, player -> ManaHud.isVisible() ? 10 : 0);
    }
}
