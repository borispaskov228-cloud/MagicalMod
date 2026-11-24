package com.magicalmod.magic;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;

/** Круг групповой магии: лидер, участники и общий резервуар маны. */
public final class RitualCircle {
    public static final float RADIUS = 16.0F;
    public static final long LIFETIME_MS = 5L * 60L * 1000L;

    private final UUID leader;
    private final ResourceKey<Level> dimension;
    private final double x, y, z;
    private final Map<UUID, Float> contributions = new HashMap<>();
    private final long createdAt = System.currentTimeMillis();
    private float pool;

    public RitualCircle(ServerPlayer leader) {
        this.leader = leader.getUUID();
        this.dimension = leader.level().dimension();
        this.x = leader.getX();
        this.y = leader.getY();
        this.z = leader.getZ();
    }

    public UUID leader() { return leader; }
    public ResourceKey<Level> dimension() { return dimension; }
    public double x() { return x; }
    public double y() { return y; }
    public double z() { return z; }
    public float pool() { return pool; }
    public int contributorCount() { return contributions.size(); }

    public boolean isContributor(UUID uuid) { return contributions.containsKey(uuid); }

    public void addContribution(UUID uuid, float mana) {
        contributions.merge(uuid, mana, Float::sum);
        pool += mana;
    }

    /** Выход участника: его мана возвращается в пул круга (не игроку). */
    public boolean removeContributor(UUID uuid) {
        // Взнос остаётся в общем резервуаре круга — игрок его не возвращает
        return contributions.remove(uuid) != null;
    }

    /** Забирает всю ману круга и очищает список участников. */
    public float drainPool() {
        float value = pool;
        pool = 0.0F;
        contributions.clear();
        return value;
    }

    /** Возвращает ману участникам при отмене круга. */
    public void refund(MinecraftServer server) {
        for (Map.Entry<UUID, Float> entry : contributions.entrySet()) {
            ServerPlayer p = server.getPlayerList().getPlayer(entry.getKey());
            if (p != null) ManaManager.add(p, entry.getValue());
        }
        pool = 0.0F;
        contributions.clear();
    }

    public boolean contains(ServerPlayer player) {
        if (!player.level().dimension().equals(dimension)) return false;
        return player.distanceToSqr(x, y, z) <= RADIUS * RADIUS;
    }

    public boolean expired() {
        return System.currentTimeMillis() - createdAt > LIFETIME_MS;
    }
}
