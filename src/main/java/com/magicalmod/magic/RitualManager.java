package com.magicalmod.magic;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class RitualManager {
    private RitualManager() {}

    private static final Map<UUID, RitualCircle> CIRCLES = new HashMap<>();

    public static void reset() { CIRCLES.clear(); }

    public static RitualCircle start(ServerPlayer leader) {
        RitualCircle circle = new RitualCircle(leader);
        CIRCLES.put(leader.getUUID(), circle);
        return circle;
    }

    public static RitualCircle get(UUID leader) { return CIRCLES.get(leader); }

    public static java.util.Collection<RitualCircle> all() { return CIRCLES.values(); }

    /** Убирает игрока из круга (без возврата маны). */
    public static boolean leave(ServerPlayer player) {
        for (RitualCircle circle : CIRCLES.values()) {
            if (circle.removeContributor(player.getUUID())) return true;
        }
        return false;
    }

    public static void stop(ServerPlayer leader) {
        RitualCircle circle = CIRCLES.remove(leader.getUUID());
        if (circle != null) circle.refund(leader.level().getServer());
    }

    /** Ближайший круг, в радиусе которого находится игрок. */
    public static RitualCircle nearest(ServerPlayer player) {
        RitualCircle best = null;
        double bestDist = Double.MAX_VALUE;
        for (RitualCircle circle : CIRCLES.values()) {
            ServerPlayer leader = player.level().getServer().getPlayerList().getPlayer(circle.leader());
            if (leader == null || !circle.contains(player)) continue;
            double d = player.distanceToSqr(leader.getX(), leader.getY(), leader.getZ());
            if (d < bestDist) { bestDist = d; best = circle; }
        }
        return best;
    }

    /** Усиливает лидера: возвращает-circle, если круг активен. */
    public static RitualCircle circleForLeader(ServerPlayer player) {
        return CIRCLES.get(player.getUUID());
    }

    /** Удаляет просроченные круги. */
    public static void tick(MinecraftServer server) {
        List<UUID> expired = new ArrayList<>();
        for (Map.Entry<UUID, RitualCircle> e : CIRCLES.entrySet()) {
            if (e.getValue().expired()) expired.add(e.getKey());
        }
        for (UUID id : expired) {
            RitualCircle circle = CIRCLES.remove(id);
            if (circle != null) circle.refund(server);
        }
    }

    public static void notifyEveryone(ServerPlayer source, Component message) {
        for (ServerPlayer player : source.level().getServer().getPlayerList().getPlayers()) {
            player.displayClientMessage(message, false);
        }
    }
}
