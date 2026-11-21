package com.magicalmod.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.magicalmod.magic.Spell;
import com.magicalmod.net.ModPayloads.CastFxPayload;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.InteractionHand;

/** Клиентские анимации заклинаний: вращающиеся кольца и усиленный вариант. */
public final class SpellAnimations {
    private SpellAnimations() {}

    private static final List<Effect> EFFECTS = new ArrayList<>();

    public static void add(CastFxPayload payload, Minecraft client) {
        Spell spell = Spell.byId(payload.spellId());
        if (spell == null) return;

        EFFECTS.add(new Effect(spell.color(), payload.empowered(),
                payload.x(), payload.y(), payload.z(),
                payload.empowered() ? 90 : 45));

        if (client.player != null && payload.casterUuid().equals(client.player.getUUID().toString())) {
            client.player.swing(InteractionHand.MAIN_HAND);
        }
    }

    public static void tick(Minecraft client) {
        ClientLevel level = client.level;
        if (level == null) {
            EFFECTS.clear();
            return;
        }

        Iterator<Effect> it = EFFECTS.iterator();
        while (it.hasNext()) {
            Effect e = it.next();
            e.age++;
            spawn(level, e);
            if (e.age >= e.duration) it.remove();
        }
    }

    private static void spawn(ClientLevel level, Effect e) {
        double radius = 1.0D + e.age * 0.09D;
        int points = e.empowered ? 14 : 8;

        for (int i = 0; i < points; i++) {
            double angle = e.age * 0.28D + i * (Math.PI * 2.0D / points);
            double px = e.x + Math.cos(angle) * radius;
            double pz = e.z + Math.sin(angle) * radius;
            double py = e.y + Math.sin(angle * 2.0D + e.age * 0.18D) * 0.7D;

            level.addParticle(new DustParticleOptions(e.color, e.empowered ? 1.5F : 1.0F),
                    px, py, pz, 0.0D, 0.015D, 0.0D);

            if (e.empowered) {
                level.addParticle(ParticleTypes.END_ROD, px, py, pz, 0.0D, 0.01D, 0.0D);
                level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, px, py, pz, 0.0D, 0.02D, 0.0D);
            }
        }

        if (e.empowered) {
            double beamY = e.y + (e.age % 30) * 0.25D;
            level.addParticle(ParticleTypes.GLOW, e.x, beamY, e.z, 0.0D, 0.05D, 0.0D);
            level.addParticle(ParticleTypes.EXPLOSION, e.x, e.y, e.z, 0.0D, 0.0D, 0.0D);
        }
    }

    private static final class Effect {
        final int color;
        final boolean empowered;
        final double x, y, z;
        final int duration;
        int age = 0;

        Effect(int color, boolean empowered, double x, double y, double z, int duration) {
            this.color = color;
            this.empowered = empowered;
            this.x = x;
            this.y = y;
            this.z = z;
            this.duration = duration;
        }
    }
}
