package com.magicalmod.client;

import com.magicalmod.MagicalMod;
import com.magicalmod.net.ModPayloads.CastFxPayload;
import com.magicalmod.net.ModPayloads.RitualActionPayload;
import com.magicalmod.net.ModPayloads.RitualStatusPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;

public class MagicalModClient implements ClientModInitializer {
    private static final KeyMapping RITUAL_KEY = KeyBindingHelper.registerKeyBinding(
            new KeyMapping("key.magicalmod.ritual", InputConstants.KEY_G,
                    KeyMapping.Category.register(MagicalMod.id("category")))
    );

    @Override
    public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (RITUAL_KEY.consumeClick() && client.player != null) {
                ClientPlayNetworking.send(new RitualActionPayload("open"));
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
    }
}
