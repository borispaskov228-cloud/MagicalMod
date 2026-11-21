package com.magicalmod.magic;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.Vec3;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import com.magicalmod.net.ModPayloads.CastFxPayload;

/** Исполнение заклинаний на сервере. */
public final class SpellCaster {
    private SpellCaster() {}

    public static void handle(ServerPlayer player, String spellId) {
        Spell spell = Spell.byId(spellId);
        if (spell == null) {
            MagicalModHolder.LOGGER.warn("Неизвестное заклинание: {}", spellId);
            return;
        }
        cast(player, spell);
    }

    public static void cast(ServerPlayer player, Spell spell) {
        RitualCircle circle = RitualManager.circleForLeader(player);
        boolean empowered = circle != null && circle.contributorCount() > 0;
        float cost = spell.manaCost();

        if (ManaManager.get(player) < cost) {
            player.displayClientMessage(Component.literal(
                    "Не хватает маны: нужно " + Math.round(cost)
            ), true);
            return;
        }

        float pooled = empowered ? circle.drainPool() : 0.0F;
        ManaManager.consume(player, cost);

        float multiplier = empowered ? 1.0F + Math.min(5.0F, pooled / 40.0F) : 1.0F;
        float damage = spell.basePower() * multiplier;
        float radius = empowered ? 9.0F : 4.0F;

        ServerLevel level = player.level();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);

        List<LivingEntity> targets = findTargets(player, level, eye, look, empowered ? 30.0F : 20.0F);

        Vec3 hit = eye.add(look.scale(empowered ? 14.0 : 10.0));
        spawnEffect(level, player, spell, hit, empowered, pooled);

        for (LivingEntity target : targets) {
            applyEffect(player, level, spell, target, damage, radius, empowered);
        }

        if (empowered) {
            RitualManager.notifyEveryone(player, Component.literal(
                    "§6✦ ГРУППОВОЕ ЗАКЛИНАНИЕ §f" + spell.displayName()
                            + " §7(сила ×" + String.format("%.1f", multiplier) + ")"
            ));
        } else {
            player.displayClientMessage(Component.literal(
                    "§5✧ " + spell.displayName() + " §7(−" + Math.round(cost) + " маны)"
            ), true);
        }
        ManaManager.sendMana(player);

        // Анимация: размахивание рукой + FX-пакет всем nearby игрокам
        player.swing(InteractionHand.MAIN_HAND);
        CastFxPayload fx = new CastFxPayload(spell.id(), empowered,
                player.getUUID().toString(), hit.x, hit.y, hit.z);
        for (ServerPlayer viewer : level.getServer().getPlayerList().getPlayers()) {
            if (viewer.level() == level && viewer.distanceToSqr(hit.x, hit.y, hit.z) < 4096.0D) {
                ServerPlayNetworking.send(viewer, fx);
            }
        }
    }

    private static List<LivingEntity> findTargets(ServerPlayer player, ServerLevel level, Vec3 eye, Vec3 look, float range) {
        List<LivingEntity> result = new ArrayList<>();
        var box = player.getBoundingBox().inflate(range);
        for (Entity entity : level.getEntities(player, box, e -> e != player && e instanceof LivingEntity)) {
            Vec3 to = entity.getEyePosition().subtract(eye);
            if (to.length() > range) continue;
            if (to.normalize().dot(look) < 0.80D) continue;
            result.add((LivingEntity) entity);
        }
        return result;
    }

    private static void applyEffect(ServerPlayer caster, ServerLevel level, Spell spell,
                                    LivingEntity target, float damage, float radius, boolean empowered) {
        switch (spell) {
            case HEAL -> {
                caster.heal(Math.min(damage, caster.getMaxHealth() - caster.getHealth()));
                caster.addEffect(new MobEffectInstance(MobEffects.REGENERATION, empowered ? 160 : 60, empowered ? 2 : 0));
            }
            case FROST -> {
                target.hurtServer(level, caster.damageSources().magic(), damage);
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, empowered ? 200 : 100, empowered ? 2 : 0));
            }
            case FIREBALL -> target.hurtServer(level, caster.damageSources().magic(), damage);
            case LIGHTNING -> {
                target.hurtServer(level, caster.damageSources().lightningBolt(), damage);
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 1));
            }
        }

        // Групповая магия задевает всех рядом с целью
        if (empowered && spell != Spell.HEAL) {
            for (Entity near : level.getEntities(caster, target.getBoundingBox().inflate(radius),
                    e -> e != caster && e != target && e instanceof LivingEntity)) {
                ((LivingEntity) near).hurtServer(level, caster.damageSources().magic(), damage * 0.4F);
            }
        }
    }

    private static void spawnEffect(ServerLevel level, ServerPlayer caster, Spell spell,
                                    Vec3 hit, boolean empowered, float pooled) {
        double x = hit.x, y = hit.y, z = hit.z;
        int color = spell.color();

        if (spell == Spell.HEAL && !empowered) {
            level.sendParticles(ParticleTypes.HEART, caster.getX(), caster.getY() + 1.0, caster.getZ(),
                    18, 0.4, 0.6, 0.4, 0.05);
        } else {
            level.sendParticles(ParticleTypes.FLAME, x, y, z, empowered ? 90 : 30,
                    empowered ? 2.0 : 0.6, empowered ? 2.0 : 0.6, empowered ? 2.0 : 0.6, 0.08);
        }

        if (empowered) {
            // Совершенно другой визуал: цветная сфера, души огонь, концы и свечение
            level.sendParticles(new DustParticleOptions(color, 1.6F), x, y, z, 160,
                    3.5, 3.5, 3.5, 0.25);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 70,
                    2.5, 2.5, 2.5, 0.12);
            level.sendParticles(ParticleTypes.END_ROD, x, y, z, 60,
                    2.0, 2.0, 2.0, 0.15);
            level.sendParticles(ParticleTypes.GLOW, x, y, z, 40,
                    2.0, 2.0, 2.0, 0.05);
            level.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 3,
                    0.4, 0.4, 0.4, 0.0);
            play(level, SoundEvents.ENDER_DRAGON_GROWL, x, y, z, 0.8F, 0.6F);
            play(level, SoundEvents.BEACON_ACTIVATE, x, y, z, 1.0F, 1.2F);
        } else {
            switch (spell) {
                case FIREBALL -> {
                    level.sendParticles(ParticleTypes.SMOKE, x, y, z, 20, 0.5, 0.5, 0.5, 0.03);
                    play(level, SoundEvents.FIRECHARGE_USE, x, y, z, 1.0F, 1.0F);
                }
                case FROST -> {
                    level.sendParticles(new DustParticleOptions(color, 1.0F), x, y, z, 45, 1.0, 1.0, 1.0, 0.05);
                    play(level, SoundEvents.GLASS_BREAK, x, y, z, 0.9F, 1.2F);
                }
                case HEAL -> play(level, SoundEvents.TOTEM_USE, x, y, z, 0.8F, 1.0F);
                case LIGHTNING -> {
                    level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 40, 1.0, 1.0, 1.0, 0.1);
                    play(level, SoundEvents.ENDER_DRAGON_GROWL, x, y, z, 0.4F, 1.5F);
                }
            }
        }
    }

    private static void play(ServerLevel level, SoundEvent sound, double x, double y, double z, float volume, float pitch) {
        level.playSound(null, x, y, z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    /** Утилита доступа к логгеру без циклической зависимости. */
    private static final class MagicalModHolder {
        static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger("magicalmod");
    }
}
