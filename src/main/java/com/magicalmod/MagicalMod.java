package com.magicalmod;

import com.magicalmod.command.MagicCommands;
import com.magicalmod.magic.ManaManager;
import com.magicalmod.magic.RitualManager;
import com.magicalmod.net.ModPayloads;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MagicalMod implements ModInitializer {
    public static final String MOD_ID = "magicalmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("MagicalMod загружен: жесты, мана и групповые ритуалы");

        ModPayloads.register();
        MagicCommands.register();

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            RitualManager.reset();
            ManaManager.resetRegenCounter();
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ManaManager.tick(server);
            RitualManager.tick(server);
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                ManaManager.sendToClient(handler.getPlayer())
        );

        // При респавне сервер создаёт нового ServerPlayer — переносим ману и синхронизируем HUD
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) ->
                ManaManager.setSilent(newPlayer, ManaManager.get(oldPlayer))
        );
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) ->
                ManaManager.sendToClient(newPlayer)
        );
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
