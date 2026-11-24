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

/** Разные анимации для разных заклинаний + усиленный вариант. */
public final class SpellAnimations {
    private SpellAnimations() {}

    private static final List<Effect> EFFECTS = new ArrayList<>();
    private static ClientLevel lastLevel = null;

    public static void add(CastFxPayload payload, Minecraft client) {
        Spell spell = Spell.byId(payload.spellId());
        if (spell == null) return;

        if (EFFECTS.size() >= 4) EFFECTS.remove(0);
        EFFECTS.add(new Effect(spell, payload.empowered(),
                payload.x(), payload.y(), payload.z(),
                payload.empowered() ? 90 : 45));

        if (client.player != null && payload.casterUuid().equals(client.player.getUUID().toString())) {
            client.player.swing(InteractionHand.MAIN_HAND);
        }
    }

    public static void tick(Minecraft client) {
        ClientLevel level = client.level;
        if (level == null || level != lastLevel) {
            EFFECTS.clear();
            lastLevel = level;
            if (level == null) return;
        }

        Iterator<Effect> it = EFFECTS.iterator();
        while (it.hasNext()) {
            Effect e = it.next();
            e.age++;
            if (e.age < e.duration) spawn(level, e);
            else it.remove();
        }
    }

    private static void spawn(ClientLevel level, Effect e) {
        double radius = 1.0D + e.age * 0.09D;

        switch (e.spell) {
            case FIREBALL -> {
                // Восходящая спираль пламени
                double y = e.y + (e.age % 20) * 0.08D;
                for (int i = 0; i < 7; i++) {
                    double a = e.age * 0.35D + i * (Math.PI * 2.0D / 7.0D);
                    double px = e.x + Math.cos(a) * radius;
                    double pz = e.z + Math.sin(a) * radius;
                    level.addParticle(ParticleTypes.FLAME, px, y, pz, 0.0D, 0.05D, 0.0D);
                    level.addParticle(e.options, px, y, pz, 0.0D, 0.02D, 0.0D);
                }
            }
            case FROST -> {
                // Расширяющийся ледяной круг у земли
                double r = 1.0D + e.age * 0.16D;
                for (int i = 0; i < 11; i++) {
                    double a = i * (Math.PI * 2.0D / 11.0D) + e.age * 0.08D;
                    double px = e.x + Math.cos(a) * r;
                    double pz = e.z + Math.sin(a) * r;
                    level.addParticle(ParticleTypes.SNOWFLAKE, px, e.y + 0.1D, pz, 0.0D, 0.005D, 0.0D);
                    level.addParticle(e.options, px, e.y + 0.2D, pz, 0.0D, 0.01D, 0.0D);
                }
            }
            case HEAL -> {
                // Столбики сердечек и свечения
                for (int i = 0; i < 5; i++) {
                    double a = i * (Math.PI * 2.0D / 5.0D);
                    double px = e.x + Math.cos(a) * 1.2D;
                    double pz = e.z + Math.sin(a) * 1.2D;
                    double py = e.y + (e.age % 20) * 0.10D;
                    level.addParticle(ParticleTypes.HEART, px, py, pz, 0.0D, 0.02D, 0.0D);
                    level.addParticle(ParticleTypes.GLOW, px, py, pz, 0.0D, 0.01D, 0.0D);
                }
            }
            case LIGHTNING -> {
                // Вертикальная зигзагообразная молния
                double py = e.y + (e.age % 15) * 0.18D;
                double px = e.x + Math.sin(e.age * 1.7D) * 0.8D;
                double pz = e.z + Math.cos(e.age * 1.3D) * 0.8D;
                level.addParticle(ParticleTypes.ELECTRIC_SPARK, px, py, pz, 0.0D, 0.02D, 0.0D);
                level.addParticle(ParticleTypes.END_ROD, px, py, pz, 0.0D, 0.01D, 0.0D);
                level.addParticle(e.options, e.x, e.y + e.age * 0.1D, e.z, 0.0D, 0.01D, 0.0D);
            }
            case POISON -> {
                // Ядовитое облако, опадающее вниз
                for (int i = 0; i < 6; i++) {
                    double a = i * (Math.PI / 3.0D) + e.age * 0.12D;
                    double px = e.x + Math.cos(a) * radius;
                    double pz = e.z + Math.sin(a) * radius;
                    level.addParticle(e.options, px, e.y + (e.age % 12) * 0.06D, pz, 0.0D, -0.01D, 0.0D);
                }
            }
            case WATER -> {
                // Пузыри, расходящиеся волнами
                double r = 1.0D + (e.age % 14) * 0.18D;
                for (int i = 0; i < 8; i++) {
                    double a = i * (Math.PI / 4.0D) + e.age * 0.1D;
                    level.addParticle(ParticleTypes.BUBBLE,
                            e.x + Math.cos(a) * r, e.y + (e.age % 10) * 0.09D,
                            e.z + Math.sin(a) * r, 0.0D, 0.03D, 0.0D);
                }
            }
            case WIND -> {
                // Горизонтальные вихри
                for (int i = 0; i < 6; i++) {
                    double a = e.age * 0.5D + i * (Math.PI / 3.0D);
                    level.addParticle(ParticleTypes.CLOUD,
                            e.x + Math.cos(a) * (radius + 1.0D), e.y + 0.3D,
                            e.z + Math.sin(a) * (radius + 1.0D), 0.02D, 0.0D, 0.02D);
                }
            }
            case SHIELD -> {
                // Вращающаяся оборонительная сфера
                for (int i = 0; i < 8; i++) {
                    double a = e.age * 0.25D + i * (Math.PI / 4.0D);
                    level.addParticle(ParticleTypes.END_ROD,
                            e.x + Math.cos(a) * 1.6D, e.y + Math.sin(a * 2.0D) * 0.8D,
                            e.z + Math.sin(a) * 1.6D, 0.0D, 0.01D, 0.0D);
                }
            }
            case METEOR -> {
                // Падающие лава и искры
                for (int i = 0; i < 7; i++) {
                    double a = i * (Math.PI * 2.0D / 7.0D);
                    level.addParticle(ParticleTypes.LAVA,
                            e.x + Math.cos(a) * radius, e.y + 2.0D - (e.age % 10) * 0.2D,
                            e.z + Math.sin(a) * radius, 0.0D, -0.08D, 0.0D);
                }
            }
            case PULL -> {
                // Спираль, стягивающаяся к центру
                double r = Math.max(0.3D, 3.0D - e.age * 0.12D);
                for (int i = 0; i < 6; i++) {
                    double a = e.age * 0.4D + i * (Math.PI / 3.0D);
                    level.addParticle(ParticleTypes.PORTAL,
                            e.x + Math.cos(a) * r, e.y + 0.5D, e.z + Math.sin(a) * r,
                            -Math.cos(a) * 0.05D, 0.0D, -Math.sin(a) * 0.05D);
                }
            }
        }

        if (e.empowered) {
            for (int i = 0; i < 8; i++) {
                double a = e.age * 0.2D + i * (Math.PI / 4.0D);
                double px = e.x + Math.cos(a) * (radius + 1.0D);
                double pz = e.z + Math.sin(a) * (radius + 1.0D);
                level.addParticle(ParticleTypes.END_ROD, px, e.y + 0.5D, pz, 0.0D, 0.01D, 0.0D);
                level.addParticle(ParticleTypes.SOUL_FIRE_FLAME, px, e.y, pz, 0.0D, 0.03D, 0.0D);
            }
            level.addParticle(ParticleTypes.GLOW, e.x, e.y + (e.age % 30) * 0.25D, e.z, 0.0D, 0.05D, 0.0D);
        }
    }

    private static final class Effect {
        final Spell spell;
        final boolean empowered;
        final DustParticleOptions options;
        final double x, y, z;
        final int duration;
        int age = 0;

        Effect(Spell spell, boolean empowered, double x, double y, double z, int duration) {
            this.spell = spell;
            this.empowered = empowered;
            this.options = new DustParticleOptions(spell.color(), empowered ? 1.5F : 1.0F);
            this.x = x;
            this.y = y;
            this.z = z;
            this.duration = duration;
        }
    }
}
