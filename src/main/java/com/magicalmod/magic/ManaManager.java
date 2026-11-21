package com.magicalmod.magic;

import com.magicalmod.net.ModPayloads.ManaUpdatePayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class ManaManager {
    private ManaManager() {}

    private static int regenCounter = 0;

    public static void resetRegenCounter() { regenCounter = 0; }

    public static float get(ServerPlayer player) {
        return ((ManaHolder) player).magicalmod$getMana();
    }

    public static void set(ServerPlayer player, float value) {
        float clamped = Math.max(0.0F, Math.min(ManaHolder.MAX_MANA, value));
        float old = get(player);
        ((ManaHolder) player).magicalmod$setMana(clamped);
        // HUD обновляем только при реальном изменении
        if (Math.abs(old - clamped) > 0.001F) sendToClient(player, clamped);
    }

    /** Списывает ману. Возвращает false, если её не хватило. */
    public static boolean consume(ServerPlayer player, float amount) {
        float current = get(player);
        if (current < amount) return false;
        set(player, current - amount);
        return true;
    }

    public static void add(ServerPlayer player, float amount) {
        set(player, get(player) + amount);
    }

    /** +1 мана в секунду каждому игроку. */
    public static void tick(MinecraftServer server) {
        if (++regenCounter < 20) return;
        regenCounter = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (get(player) < ManaHolder.MAX_MANA) add(player, 1.0F);
        }
    }

    public static void sendToClient(ServerPlayer player) {
        sendToClient(player, get(player));
    }

    private static void sendToClient(ServerPlayer player, float mana) {
        ServerPlayNetworking.send(player, new ManaUpdatePayload(mana));
    }

    public static void sendMana(ServerPlayer player) {
        player.displayClientMessage(Component.literal(
                "Мана: " + Math.round(get(player)) + " / " + Math.round(ManaHolder.MAX_MANA)
        ), true);
    }
}
