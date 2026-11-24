package com.magicalmod;

import com.magicalmod.boss.BossContent;
import com.magicalmod.command.MagicCommands;
import com.magicalmod.content.ModContent;
import com.magicalmod.magic.GuideBook;
import com.magicalmod.magic.MagicProjectiles;
import com.magicalmod.magic.ManaHolder;
import com.magicalmod.magic.ManaManager;
import com.magicalmod.magic.ManaPotions;
import com.magicalmod.magic.RitualManager;
import com.magicalmod.net.ModPayloads;
import com.magicalmod.staff.StaffContent;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
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
        LOGGER.info("MagicalMod загружен: жесты, мана, заряды и групповые ритуалы");

        ModPayloads.register();
        MagicCommands.register();
        ManaPotions.register();
        StaffContent.register();
        ModContent.register();
        BossContent.register();

        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            RitualManager.reset();
            ManaManager.resetRegenCounter();
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            ManaManager.tick(server);
            RitualManager.tick(server);
            MagicProjectiles.tick(server);
        });

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ManaManager.sendToClient(handler.getPlayer());
            GuideBook.give(handler.getPlayer());
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            if (handler.getPlayer() != null) RitualManager.onDisconnect(handler.getPlayer());
        });

        // При респавне сервер создаёт нового ServerPlayer — переносим ману и синхронизируем HUD
        ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) -> {
            ManaHolder from = (ManaHolder) oldPlayer;
            ManaHolder to = (ManaHolder) newPlayer;
            to.magicalmod$setManaLevel(from.magicalmod$getManaLevel());
            to.magicalmod$setManaXp(from.magicalmod$getManaXp());
            ManaManager.setSilent(newPlayer, ManaManager.get(oldPlayer));
        });
        ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) ->
                ManaManager.sendToClient(newPlayer)
        );
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
