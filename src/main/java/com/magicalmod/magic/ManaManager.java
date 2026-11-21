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

    public static float getMax(ServerPlayer player) {
        return ((ManaHolder) player).magicalmod$getMaxMana();
    }

    public static int getLevel(ServerPlayer player) {
        return ((ManaHolder) player).magicalmod$getManaLevel();
    }

    public static int getXp(ServerPlayer player) {
        return ((ManaHolder) player).magicalmod$getManaXp();
    }

    public static void set(ServerPlayer player, float value) {
        float clamped = Math.max(0.0F, Math.min(getMax(player), value));
        float old = get(player);
        ((ManaHolder) player).magicalmod$setMana(clamped);
        if (Math.abs(old - clamped) > 0.001F) sendToClient(player);
    }

    /** Копирование маны при респавне — пакет отправит AFTER_RESPAWN. */
    public static void setSilent(ServerPlayer player, float value) {
        ((ManaHolder) player).magicalmod$setMana(Math.max(0.0F, Math.min(getMax(player), value)));
    }

    public static boolean consume(ServerPlayer player, float amount) {
        if (amount < 0.0F) return false;
        float current = get(player);
        if (current < amount) return false;
        set(player, current - amount);
        return true;
    }

    public static void add(ServerPlayer player, float amount) {
        if (amount <= 0.0F) return;
        set(player, get(player) + amount);
    }

    /** Прокачка: очки за касты повышают уровень и максимальную ману. */
    public static void awardXp(ServerPlayer player, int amount) {
        ManaHolder holder = (ManaHolder) player;
        int xp = holder.magicalmod$getManaXp() + amount;
        int level = holder.magicalmod$getManaLevel();
        boolean leveled = false;

        while (xp >= ManaHolder.XP_PER_LEVEL && level < ManaHolder.MAX_LEVEL) {
            xp -= ManaHolder.XP_PER_LEVEL;
            level++;
            leveled = true;
        }
        if (level >= ManaHolder.MAX_LEVEL) xp = 0;

        holder.magicalmod$setManaXp(xp);
        holder.magicalmod$setManaLevel(level);

        if (leveled) {
            player.displayClientMessage(Component.literal(
                    "§6✦ Уровень магии " + level + "! Максимум маны: " + Math.round(getMax(player))
            ), false);
        }
        sendToClient(player);
    }

    /** +1 мана в секунду каждому игроку. */
    public static void tick(MinecraftServer server) {
        if (++regenCounter < 20) return;
        regenCounter = 0;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (get(player) < getMax(player)) add(player, 1.0F);
        }
    }

    public static void sendToClient(ServerPlayer player) {
        ServerPlayNetworking.send(player, new ManaUpdatePayload(
                get(player), getMax(player), getLevel(player), getXp(player)
        ));
    }

    public static void sendMana(ServerPlayer player) {
        player.displayClientMessage(Component.literal(
                "Мана: " + Math.round(get(player)) + " / " + Math.round(getMax(player))
                        + "  [ур. " + getLevel(player) + "]"
        ), true);
    }
}
