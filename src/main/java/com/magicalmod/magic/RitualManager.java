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
    private static int tickCounter = 0;

    public static void reset() { CIRCLES.clear(); tickCounter = 0; }

    public static RitualCircle start(ServerPlayer leader) {
        RitualCircle old = CIRCLES.remove(leader.getUUID());
        if (old != null) old.refund(leader.level().getServer());
        RitualCircle circle = new RitualCircle(leader);
        CIRCLES.put(leader.getUUID(), circle);
        return circle;
    }

    public static RitualCircle get(UUID leader) { return CIRCLES.get(leader); }

    public static java.util.Collection<RitualCircle> all() { return CIRCLES.values(); }

    /** Убирает игрока из круга (его взнос остаётся в пуле). */
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

    /** Ближайший круг в том же измерении. */
    public static RitualCircle nearest(ServerPlayer player) {
        RitualCircle best = null;
        double bestDist = Double.MAX_VALUE;
        for (RitualCircle circle : CIRCLES.values()) {
            if (!circle.dimension().equals(player.level().dimension())) continue;
            ServerPlayer leader = player.level().getServer().getPlayerList().getPlayer(circle.leader());
            if (leader == null || !circle.contains(player)) continue;
            double d = player.distanceToSqr(leader.getX(), leader.getY(), leader.getZ());
            if (d < bestDist) { bestDist = d; best = circle; }
        }
        return best;
    }

    public static RitualCircle circleForLeader(ServerPlayer player) {
        return CIRCLES.get(player.getUUID());
    }

    /** Игрок вышел из сети — убираем его из кругов, а его круг расформировываем. */
    public static void onDisconnect(ServerPlayer player) {
        leave(player);
        RitualCircle own = CIRCLES.remove(player.getUUID());
        if (own != null) own.refund(player.level().getServer());
    }

    /** Удаляет просроченные круги раз в секунду. */
    public static void tick(MinecraftServer server) {
        if (CIRCLES.isEmpty() || ++tickCounter < 20) return;
        tickCounter = 0;

        List<UUID> expired = new ArrayList<>();
        for (Map.Entry<UUID, RitualCircle> e : CIRCLES.entrySet()) {
            if (e.getValue().expired()) expired.add(e.getKey());
        }
        for (UUID id : expired) {
            RitualCircle circle = CIRCLES.remove(id);
            if (circle != null) circle.refund(server);
        }
    }

    /** Оповещение только игроков рядом и в том же измерении. */
    public static void notifyNear(ServerPlayer source, Component message) {
        for (ServerPlayer player : source.level().getServer().getPlayerList().getPlayers()) {
            if (player.level().dimension().equals(source.level().dimension())
                    && player.distanceToSqr(source) < 64.0D * 64.0D) {
                player.displayClientMessage(message, false);
            }
        }
    }
}
