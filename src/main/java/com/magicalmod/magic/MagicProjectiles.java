package com.magicalmod.magic;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Летящие магические заряды: эффект срабатывает при попадании, а не «из ниоткуда». */
public final class MagicProjectiles {
    private MagicProjectiles() {}

    private static final List<Bolt> BOLTS = new ArrayList<>();
    private static final double SPEED = 0.9D;

    private static final DustParticleOptions FROST_DUST = new DustParticleOptions(0xAAEEFF, 0.9F);
    private static final DustParticleOptions[] BURST = {
            new DustParticleOptions(Spell.FIREBALL.color(), 1.0F),
            new DustParticleOptions(Spell.FROST.color(), 1.0F),
            new DustParticleOptions(Spell.HEAL.color(), 1.0F),
            new DustParticleOptions(Spell.LIGHTNING.color(), 1.0F)
    };

    public static void spawn(ServerPlayer caster, Spell spell, Vec3 origin, Vec3 direction,
                             boolean empowered, float damage, float radius, float range) {
        BOLTS.add(new Bolt(origin, direction.normalize(), caster.level().dimension(),
                caster.getUUID(), spell, empowered, damage, radius, (int) (range / SPEED)));
    }

    public static void tick(MinecraftServer server) {
        if (BOLTS.isEmpty()) return;

        Iterator<Bolt> it = BOLTS.iterator();
        while (it.hasNext()) {
            Bolt b = it.next();
            ServerLevel world = server.getLevel(b.dimension);
            if (world == null) { it.remove(); continue; }

            b.pos = b.pos.add(b.dir.scale(SPEED));
            b.age++;
            trail(world, b);

            ServerPlayer caster = server.getPlayerList().getPlayer(b.caster);
            boolean done = false;

            if (b.spell == Spell.HEAL) {
                if (b.age >= b.maxAge) {
                    if (caster != null) SpellCaster.applyHeal(caster, b.damage, b.empowered);
                    done = true;
                }
            } else {
                AABB box = new AABB(b.pos.x - 1.2D, b.pos.y - 1.2D, b.pos.z - 1.2D,
                        b.pos.x + 1.2D, b.pos.y + 1.2D, b.pos.z + 1.2D);
                List<Entity> hits = world.getEntities(caster, box,
                        e -> e instanceof LivingEntity && (caster == null || !e.getUUID().equals(b.caster)));
                if (!hits.isEmpty()) {
                    SpellCaster.applyEffect(caster, world, b.spell,
                            (LivingEntity) hits.get(0), b.damage, b.radius, b.empowered);
                    done = true;
                }
            }

            if (done || b.age >= b.maxAge) {
                if (done) burst(world, b);
                it.remove();
            }
        }
    }

    private static void trail(ServerLevel world, Bolt b) {
        double x = b.pos.x, y = b.pos.y, z = b.pos.z;
        switch (b.spell) {
            case FIREBALL -> {
                world.sendParticles(ParticleTypes.FLAME, x, y, z, 2, 0.08, 0.08, 0.08, 0.01);
                world.sendParticles(ParticleTypes.SMOKE, x, y, z, 1, 0.04, 0.04, 0.04, 0.01);
            }
            case FROST -> {
                world.sendParticles(ParticleTypes.SNOWFLAKE, x, y, z, 2, 0.08, 0.08, 0.08, 0.01);
                world.sendParticles(FROST_DUST, x, y, z, 1, 0.05, 0.05, 0.05, 0.01);
            }
            case HEAL -> {
                world.sendParticles(ParticleTypes.HEART, x, y, z, 2, 0.08, 0.08, 0.08, 0.01);
                world.sendParticles(ParticleTypes.GLOW, x, y, z, 1, 0.05, 0.05, 0.05, 0.01);
            }
            case LIGHTNING -> {
                world.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 3, 0.12, 0.12, 0.12, 0.02);
                world.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, 0.05, 0.05, 0.05, 0.01);
            }
        }
    }

    private static void burst(ServerLevel world, Bolt b) {
        world.sendParticles(BURST[b.spell.ordinal()], b.pos.x, b.pos.y, b.pos.z,
                b.empowered ? 40 : 16, 0.8, 0.8, 0.8, 0.1);
    }

    private static final class Bolt {
        Vec3 pos;
        final Vec3 dir;
        final ResourceKey<Level> dimension;
        final UUID caster;
        final Spell spell;
        final boolean empowered;
        final float damage;
        final float radius;
        final int maxAge;
        int age = 0;

        Bolt(Vec3 pos, Vec3 dir, ResourceKey<Level> dimension, UUID caster, Spell spell,
             boolean empowered, float damage, float radius, int maxAge) {
            this.pos = pos;
            this.dir = dir;
            this.dimension = dimension;
            this.caster = caster;
            this.spell = spell;
            this.empowered = empowered;
            this.damage = damage;
            this.radius = radius;
            this.maxAge = maxAge;
        }
    }
}
