package com.magicalmod.magic;

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
import com.magicalmod.staff.StaffContent;

/** Исполнение заклинаний на сервере. */
public final class SpellCaster {
    private SpellCaster() {}

    private static final java.util.Map<java.util.UUID, Long> LAST_CAST = new java.util.HashMap<>();
    private static final DustParticleOptions FROST_DUST = new DustParticleOptions(0xAAEEFF, 1.2F);
    private static final DustParticleOptions HEAL_DUST = new DustParticleOptions(0x77FFAA, 1.0F);
    private static final DustParticleOptions[] EMPOWER_DUST = dustOptions(1.6F);

    private static DustParticleOptions[] dustOptions(float scale) {
        DustParticleOptions[] result = new DustParticleOptions[Spell.values().length];
        for (Spell spell : Spell.values()) {
            result[spell.ordinal()] = new DustParticleOptions(spell.color(), scale);
        }
        return result;
    }

    public static void handle(ServerPlayer player, String spellId) {
        long now = System.currentTimeMillis();
        Long last = LAST_CAST.get(player.getUUID());
        if (last != null && now - last < 250L) return;
        LAST_CAST.put(player.getUUID(), now);

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
        float damage = spell.basePower() * multiplier * StaffContent.multiplier(player);
        float radius = empowered ? 6.0F : 3.0F;

        ServerLevel level = player.level();
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getViewVector(1.0F);
        Vec3 hit = eye.add(look.scale(empowered ? 9.0 : 6.0));

        // Магия летит зарядом; эффект применяется только при попадании
        float range = spell == Spell.HEAL ? 6.0F : (empowered ? 18.0F : 12.0F);
        MagicProjectiles.spawn(player, spell, eye.add(look.scale(0.6D)), look,
                empowered, damage, radius, range);
        spawnEffect(level, player, spell, eye, empowered, pooled);

        if (empowered) {
            RitualManager.notifyNear(player, Component.literal(
                    "§6✦ ГРУППОВОЕ ЗАКЛИНАНИЕ §f" + spell.displayName()
                            + " §7(сила ×" + String.format("%.1f", multiplier) + ")"
            ));
        } else {
            player.displayClientMessage(Component.literal(
                    "§5✧ " + spell.displayName() + " §7(−" + Math.round(cost) + " маны)"
            ), true);
        }
        ManaManager.sendMana(player);
        ManaManager.awardXp(player, empowered ? 4 : 2);

        // Анимация: размахивание рукой + FX-пакет всем nearby игрокам
        player.swing(InteractionHand.MAIN_HAND);
        CastFxPayload fx = new CastFxPayload(spell.id(), empowered,
                player.getUUID().toString(), hit.x, hit.y, hit.z);
        for (ServerPlayer viewer : level.getServer().getPlayerList().getPlayers()) {
            if (viewer.level() == level && viewer.distanceToSqr(hit.x, hit.y, hit.z) < 1024.0D) {
                ServerPlayNetworking.send(viewer, fx);
            }
        }
    }

    public static void applyHeal(ServerPlayer caster, float amount, boolean empowered) {
        float missing = caster.getMaxHealth() - caster.getHealth();
        if (missing > 0.0F) caster.heal(Math.min(amount, missing));
        caster.addEffect(new MobEffectInstance(MobEffects.REGENERATION,
                empowered ? 160 : 60, empowered ? 2 : 0));
        caster.level().sendParticles(net.minecraft.core.particles.ParticleTypes.HEART,
                caster.getX(), caster.getY() + 1.0D, caster.getZ(),
                empowered ? 30 : 12, 0.4D, 0.6D, 0.4D, 0.05D);
    }

    /** Эффекты на самого кастера (лечение и щит) — срабатывают по окончании полёта заряда. */
    public static void applySelf(ServerPlayer caster, Spell spell, float amount, boolean empowered) {
        if (spell == Spell.HEAL) {
            applyHeal(caster, amount, empowered);
        } else if (spell == Spell.SHIELD) {
            caster.addEffect(new MobEffectInstance(MobEffects.ABSORPTION,
                    empowered ? 400 : 200, empowered ? 4 : 1));
            caster.addEffect(new MobEffectInstance(MobEffects.RESISTANCE,
                    empowered ? 300 : 150, empowered ? 2 : 0));
            caster.level().sendParticles(ParticleTypes.END_ROD,
                    caster.getX(), caster.getY() + 1.0D, caster.getZ(),
                    empowered ? 60 : 24, 0.6D, 0.8D, 0.6D, 0.05D);
        }
    }

    public static void applyEffect(ServerPlayer caster, ServerLevel level, Spell spell,
                                    LivingEntity target, float damage, float radius, boolean empowered) {
        switch (spell) {
            case HEAL, SHIELD -> applySelf(caster, spell, damage, empowered);
            case POISON -> {
                target.hurtServer(level, caster.damageSources().magic(), damage);
                target.addEffect(new MobEffectInstance(MobEffects.POISON,
                        empowered ? 200 : 100, empowered ? 2 : 0));
                target.addEffect(new MobEffectInstance(MobEffects.WITHER, empowered ? 100 : 40, 0));
                level.sendParticles(EMPOWER_DUST[spell.ordinal()], target.getX(), target.getY() + 1.0D, target.getZ(),
                        empowered ? 60 : 26, 0.6D, 0.9D, 0.6D, 0.04);
            }
            case WATER -> {
                target.hurtServer(level, caster.damageSources().magic(), damage);
                Vec3 away = target.getEyePosition().subtract(caster.getEyePosition()).normalize();
                target.setDeltaMovement(away.scale(empowered ? 2.5D : 1.4D).add(0.0D, 0.3D, 0.0D));
                caster.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 200, 0));
                level.sendParticles(ParticleTypes.BUBBLE, target.getX(), target.getY() + 0.5D, target.getZ(),
                        empowered ? 60 : 24, 0.7D, 0.8D, 0.7D, 0.05D);
            }
            case WIND -> {
                target.hurtServer(level, caster.damageSources().magic(), damage);
                Vec3 away = target.getEyePosition().subtract(caster.getEyePosition()).normalize();
                target.setDeltaMovement(away.scale(empowered ? 3.5D : 2.0D).add(0.0D, 0.5D, 0.0D));
                caster.addEffect(new MobEffectInstance(MobEffects.SPEED, 200, 1));
                level.sendParticles(ParticleTypes.CLOUD, target.getX(), target.getY() + 0.8D, target.getZ(),
                        empowered ? 60 : 26, 0.9D, 0.9D, 0.9D, 0.05D);
            }
            case METEOR -> {
                target.hurtServer(level, caster.damageSources().magic(), damage);
                target.igniteForSeconds(empowered ? 10.0F : 5.0F);
                level.sendParticles(ParticleTypes.LAVA, target.getX(), target.getY(), target.getZ(),
                        empowered ? 70 : 30, 0.8D, 0.8D, 0.8D, 0.08D);
                level.sendParticles(ParticleTypes.EXPLOSION, target.getX(), target.getY() + 1.0D, target.getZ(),
                        empowered ? 5 : 2, 0.4D, 0.4D, 0.4D, 0.0D);
            }
            case PULL -> {
                target.hurtServer(level, caster.damageSources().magic(), damage);
                Vec3 toward = caster.getEyePosition().subtract(target.getEyePosition()).normalize();
                target.setDeltaMovement(toward.scale(empowered ? 2.0D : 1.2D).add(0.0D, 0.2D, 0.0D));
                level.sendParticles(ParticleTypes.PORTAL, target.getX(), target.getY() + 0.8D, target.getZ(),
                        empowered ? 50 : 22, 0.6D, 0.8D, 0.6D, 0.05D);
            }
            case FROST -> {
                target.hurtServer(level, caster.damageSources().magic(), damage);
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS,
                        empowered ? 240 : 140, empowered ? 4 : 2));
                // Полная заморозка: враг не двигается и получает урон от холода
                if (target.canFreeze()) {
                    target.setTicksFrozen(target.getTicksRequiredToFreeze() + (empowered ? 160 : 60));
                }
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.SNOWFLAKE,
                        target.getX(), target.getY() + 1.0D, target.getZ(),
                        empowered ? 50 : 24, 0.6D, 0.9D, 0.6D, 0.05D);
            }
            case FIREBALL -> {
                target.hurtServer(level, caster.damageSources().magic(), damage);
                // Поджог: 5 секунд обычным заклинанием, 10 — групповым
                target.igniteForSeconds(empowered ? 10.0F : 5.0F);
                level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
                        target.getX(), target.getY() + 0.8D, target.getZ(),
                        empowered ? 60 : 26, 0.5D, 0.8D, 0.5D, 0.06D);
            }
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
        int k = empowered ? 2 : 1;

        // У каждого заклинания свой набор частиц — цвет один, а картинка разная
        switch (spell) {
            case FIREBALL -> {
                level.sendParticles(ParticleTypes.FLAME, x, y, z, 60 * k, 1.4, 1.8, 1.4, 0.12);
                level.sendParticles(ParticleTypes.SMALL_FLAME, x, y + 0.5D, z, 30 * k, 1.0, 1.0, 1.0, 0.08);
                level.sendParticles(ParticleTypes.SMOKE, x, y + 1.0D, z, 24 * k, 0.8, 1.2, 0.8, 0.03);
                play(level, SoundEvents.FIRECHARGE_USE, x, y, z, 1.0F, 1.0F);
                play(level, SoundEvents.BLAZE_SHOOT, x, y, z, 0.6F, 0.8F);
            }
            case FROST -> {
                level.sendParticles(ParticleTypes.SNOWFLAKE, x, y, z, 70 * k, 2.0, 1.4, 2.0, 0.04);
                level.sendParticles(ParticleTypes.CLOUD, x, y, z, 22 * k, 1.5, 1.0, 1.5, 0.02);
                level.sendParticles(FROST_DUST, x, y, z, 32 * k, 2.0, 1.0, 2.0, 0.03);
                play(level, SoundEvents.GLASS_BREAK, x, y, z, 0.9F, 1.2F);
            }
            case HEAL -> {
                double hx = caster.getX(), hy = caster.getY(), hz = caster.getZ();
                level.sendParticles(ParticleTypes.HEART, hx, hy + 0.5D, hz, 20 * k, 0.5, 1.2, 0.5, 0.04);
                level.sendParticles(ParticleTypes.GLOW, hx, hy + 1.0D, hz, 24 * k, 0.8, 1.2, 0.8, 0.03);
                level.sendParticles(HEAL_DUST, hx, hy + 0.5D, hz,
                        32 * k, 0.9, 1.0, 0.9, 0.04);
                play(level, SoundEvents.TOTEM_USE, x, y, z, 0.8F, 1.0F);
            }
            case LIGHTNING -> {
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK, x, y, z, 60 * k, 1.2, 2.0, 1.2, 0.2);
                level.sendParticles(ParticleTypes.END_ROD, x, y, z, 24 * k, 0.8, 1.5, 0.8, 0.15);
                level.sendParticles(ParticleTypes.GLOW, x, y, z, 20 * k, 1.0, 1.0, 1.0, 0.05);
                play(level, SoundEvents.ENDER_DRAGON_GROWL, x, y, z, 0.4F, 1.6F);
            }
            case POISON -> {
                level.sendParticles(EMPOWER_DUST[spell.ordinal()], x, y, z, 50 * k, 1.2, 1.4, 1.2, 0.03);
                level.sendParticles(ParticleTypes.CLOUD, x, y, z, 18 * k, 1.0, 1.0, 1.0, 0.02);
                play(level, SoundEvents.GLASS_BREAK, x, y, z, 0.7F, 0.7F);
            }
            case WATER -> {
                level.sendParticles(ParticleTypes.BUBBLE, x, y, z, 60 * k, 1.4, 1.2, 1.4, 0.06);
                level.sendParticles(ParticleTypes.DRIPPING_WATER, x, y, z, 22 * k, 1.0, 1.0, 1.0, 0.04);
                play(level, SoundEvents.GLASS_BREAK, x, y, z, 0.6F, 1.4F);
            }
            case WIND -> {
                level.sendParticles(ParticleTypes.CLOUD, x, y, z, 60 * k, 2.0, 1.0, 2.0, 0.05);
                level.sendParticles(EMPOWER_DUST[spell.ordinal()], x, y, z, 30 * k, 2.0, 0.6, 2.0, 0.03);
                play(level, SoundEvents.FIRECHARGE_USE, x, y, z, 0.5F, 1.6F);
            }
            case SHIELD -> {
                level.sendParticles(ParticleTypes.END_ROD, caster.getX(), caster.getY() + 1.0D, caster.getZ(),
                        40 * k, 1.0D, 1.2D, 1.0D, 0.05D);
                level.sendParticles(ParticleTypes.GLOW, caster.getX(), caster.getY() + 1.0D, caster.getZ(),
                        30 * k, 0.8D, 1.0D, 0.8D, 0.03D);
                play(level, SoundEvents.BEACON_ACTIVATE, x, y, z, 0.7F, 1.3F);
            }
            case METEOR -> {
                level.sendParticles(ParticleTypes.LAVA, x, y, z, 60 * k, 1.5, 1.5, 1.5, 0.08);
                level.sendParticles(ParticleTypes.FLAME, x, y, z, 70 * k, 1.6, 1.6, 1.6, 0.1);
                level.sendParticles(ParticleTypes.EXPLOSION, x, y + 1.0D, z, 3 * k, 0.5, 0.5, 0.5, 0.0);
                play(level, SoundEvents.FIRECHARGE_USE, x, y, z, 1.2F, 0.6F);
            }
            case PULL -> {
                level.sendParticles(ParticleTypes.PORTAL, x, y, z, 50 * k, 1.2, 1.2, 1.2, 0.06);
                level.sendParticles(EMPOWER_DUST[spell.ordinal()], x, y, z, 30 * k, 1.0, 1.0, 1.0, 0.03);
                play(level, SoundEvents.ENDER_DRAGON_GROWL, x, y, z, 0.3F, 1.8F);
            }
        }

        if (empowered) {
            level.sendParticles(EMPOWER_DUST[spell.ordinal()], x, y, z, 100,
                    3.5, 3.5, 3.5, 0.25);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, x, y, z, 50, 2.5, 2.5, 2.5, 0.12);
            level.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 3, 0.4, 0.4, 0.4, 0.0);
            play(level, SoundEvents.BEACON_ACTIVATE, x, y, z, 1.0F, 1.2F);
            play(level, SoundEvents.ENDER_DRAGON_GROWL, x, y, z, 0.8F, 0.6F);
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
