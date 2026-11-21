package com.magicalmod;

import com.magicalmod.artifact.ArtifactUseHandler;
import com.magicalmod.artifact.Artifacts;
import com.magicalmod.command.MagicCommands;
import com.magicalmod.magic.GuideBook;
import com.magicalmod.magic.MagicProjectiles;
import com.magicalmod.magic.ManaHolder;
import com.magicalmod.magic.ManaManager;
import com.magicalmod.magic.RitualManager;
import com.magicalmod.net.ModPayloads;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
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
        ArtifactUseHandler.register();

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
            giveStarterArtifacts(handler.getPlayer());
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

    /** Первые три артефакта при первом входе, чтобы сразу можно было попробовать. */
    private static void giveStarterArtifacts(ServerPlayer player) {
        boolean hasAny = false;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            if (Artifacts.isArtifact(player.getInventory().getItem(i))) { hasAny = true; break; }
        }
        if (hasAny) return;
        for (int i = 0; i < 3; i++) {
            ItemStack stack = Artifacts.createStack(i);
            if (!player.getInventory().add(stack)) player.drop(stack, false);
        }
        player.displayClientMessage(net.minecraft.network.chat.Component.literal(
                "§6Вы получили 3 стартовых артефакта. Остальные: /magic artifacts"
        ), false);
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }
}
