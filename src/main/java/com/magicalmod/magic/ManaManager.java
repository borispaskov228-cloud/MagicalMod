package com.magicalmod.magic;

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
        ((ManaHolder) player).magicalmod$setMana(Math.max(0.0F, Math.min(ManaHolder.MAX_MANA, value)));
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

    public static void sendMana(ServerPlayer player) {
        player.displayClientMessage(Component.literal(
                "Мана: " + Math.round(get(player)) + " / " + Math.round(ManaHolder.MAX_MANA)
        ), true);
    }
}
