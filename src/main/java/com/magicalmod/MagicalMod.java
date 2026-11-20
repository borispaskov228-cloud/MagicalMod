package com.magicalmod;

import com.magicalmod.command.MagicCommands;
import com.magicalmod.magic.ManaManager;
import com.magicalmod.magic.RitualManager;
import com.magicalmod.net.ModPayloads;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
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
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
