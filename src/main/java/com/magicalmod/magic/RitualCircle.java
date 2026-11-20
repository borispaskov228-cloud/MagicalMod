package com.magicalmod.magic;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Круг групповой магии: лидер и участники, отдавшие ману в общий резервуар. */
public final class RitualCircle {
    public static final float RADIUS = 16.0F;
    public static final long LIFETIME_MS = 5L * 60L * 1000L;

    private final UUID leader;
    private final double x, y, z;
    private final Set<UUID> contributors = new HashSet<>();
    private final long createdAt = System.currentTimeMillis();
    private float pool;

    public RitualCircle(net.minecraft.server.level.ServerPlayer leader) {
        this.leader = leader.getUUID();
        this.x = leader.getX();
        this.y = leader.getY();
        this.z = leader.getZ();
    }

    public UUID leader() { return leader; }
    public float pool() { return pool; }
    public int contributorCount() { return contributors.size(); }

    public boolean isContributor(UUID uuid) { return contributors.contains(uuid); }

    public void addContribution(UUID uuid, float mana) {
        contributors.add(uuid);
        pool += mana;
    }

    public boolean removeContributor(UUID uuid) { return contributors.remove(uuid); }

    /** Забирает всю ману круга (используется при усиленном заклинании). */
    public float drainPool() {
        float value = pool;
        pool = 0.0F;
        return value;
    }

    /** Возвращает ману участникам при отмене круга. */
    public void refund(net.minecraft.server.MinecraftServer server) {
        if (contributors.isEmpty()) return;
        float share = pool / contributors.size();
        for (UUID id : contributors) {
            net.minecraft.server.level.ServerPlayer p = server.getPlayerList().getPlayer(id);
            if (p != null) ManaManager.add(p, share);
        }
        pool = 0.0F;
    }

    public boolean contains(net.minecraft.server.level.ServerPlayer player) {
        if (player.distanceToSqr(x, y, z) > RADIUS * RADIUS) return false;
        return true;
    }

    public boolean expired() {
        return System.currentTimeMillis() - createdAt > LIFETIME_MS;
    }
}
