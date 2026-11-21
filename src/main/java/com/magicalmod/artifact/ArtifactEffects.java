package com.magicalmod.artifact;

import com.magicalmod.magic.ManaManager;

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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

/**
 * 20 неповторимых эффектов артефактов.
 * Каждый вызов имеет общий кулдаун 100 тиков (5 секунд).
 */
public final class ArtifactEffects {
    private static final int COOLDOWN_TICKS = 100;

    private ArtifactEffects() {}

    public static void apply(ServerPlayer player, int id, ItemStack stack) {
        if (player == null || stack == null || stack.isEmpty()) return;

        // В 1.21.11 addCooldown принимает ItemStack (не Item), это тот же group по id предмета.
        player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
        if (id < 0 || id >= Artifacts.COUNT) return;

        ServerLevel level = player.level();
        switch (id) {
            case 0 -> ignite(player, level);
            case 1 -> freeze(player, level);
            case 2 -> heal(player, level);
            case 3 -> mana(player, level);
            case 4 -> speed(player, level);
            case 5 -> nightVision(player, level);
            case 6 -> waterBreathing(player, level);
            case 7 -> absorption(player, level);
            case 8 -> knockback(player, level);
            case 9 -> sparkBurst(player, level);
            case 10 -> blink(player, level);
            case 11 -> storm(player, level);
            case 12 -> strength(player, level);
            case 13 -> jump(player, level);
            case 14 -> wither(player, level);
            case 15 -> cleanse(player, level);
            case 16 -> experience(player, level);
            case 17 -> feast(player, level);
            case 18 -> invisibility(player, level);
            case 19 -> magnet(player, level);
            default -> { /* неизвестный id — ничего не делаем */ }
        }
    }

    // ------------------------------------------------------------------ //
    //  0. Огненный зар                                                     //
    // ------------------------------------------------------------------ //
    private static void ignite(ServerPlayer player, ServerLevel level) {
        for (LivingEntity e : nearby(player, 6.0D)) {
            e.hurtServer(level, player.damageSources().magic(), 4.0F);
            e.igniteForSeconds(6.0F);
            level.sendParticles(ParticleTypes.FLAME, e.getX(), e.getY() + 0.8D, e.getZ(),
                    30, 0.5D, 0.8D, 0.5D, 0.06D);
            level.sendParticles(ParticleTypes.SMOKE, e.getX(), e.getY() + 1.2D, e.getZ(),
                    12, 0.4D, 0.6D, 0.4D, 0.03D);
        }
        play(level, SoundEvents.FIRECHARGE_USE, player.getX(), player.getY(), player.getZ(), 1.0F, 1.0F);
        message(player, "§6Огненный зар: враги объяты пламенем!");
    }

    // ------------------------------------------------------------------ //
    //  1. Ледяная печать                                                   //
    // ------------------------------------------------------------------ //
    private static void freeze(ServerPlayer player, ServerLevel level) {
        for (LivingEntity e : nearby(player, 6.0D)) {
            e.hurtServer(level, player.damageSources().magic(), 3.0F);
            e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 200, 3));
            if (e.canFreeze()) e.setTicksFrozen(e.getTicksRequiredToFreeze() + 100);
            level.sendParticles(ParticleTypes.SNOWFLAKE, e.getX(), e.getY() + 1.0D, e.getZ(),
                    36, 0.6D, 0.9D, 0.6D, 0.05D);
        }
        play(level, SoundEvents.GLASS_BREAK, player.getX(), player.getY(), player.getZ(), 0.9F, 1.2F);
        message(player, "§bЛедяная печать: враги скованы льдом!");
    }

    // ------------------------------------------------------------------ //
    //  2. Живой источник                                                   //
    // ------------------------------------------------------------------ //
    private static void heal(ServerPlayer player, ServerLevel level) {
        float missing = player.getMaxHealth() - player.getHealth();
        if (missing > 0.0F) player.heal(Math.min(10.0F, missing));
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 160, 1));
        level.sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1.0D, player.getZ(),
                24, 0.5D, 1.0D, 0.5D, 0.05D);
        level.sendParticles(new DustParticleOptions(0x77FFAA, 1.0F),
                player.getX(), player.getY() + 0.5D, player.getZ(), 40, 0.9D, 1.0D, 0.9D, 0.04D);
        play(level, SoundEvents.TOTEM_USE, player.getX(), player.getY(), player.getZ(), 0.8F, 1.0F);
        message(player, "§aЖивой источник: раны заживают.");
    }

    // ------------------------------------------------------------------ //
    //  3. Магический резервуар                                             //
    // ------------------------------------------------------------------ //
    private static void mana(ServerPlayer player, ServerLevel level) {
        ManaManager.add(player, 40.0F);
        ManaManager.awardXp(player, 1);
        level.sendParticles(new DustParticleOptions(0x55AAFF, 1.2F),
                player.getX(), player.getY() + 1.0D, player.getZ(), 60, 0.7D, 1.2D, 0.7D, 0.05D);
        play(level, SoundEvents.EXPERIENCE_ORB_PICKUP, player.getX(), player.getY(), player.getZ(), 0.9F, 1.3F);
        message(player, "§9Магический резервуар: +40 маны.");
    }

    // ------------------------------------------------------------------ //
    //  4. Ветреный шаг                                                     //
    // ------------------------------------------------------------------ //
    private static void speed(ServerPlayer player, ServerLevel level) {
        player.addEffect(new MobEffectInstance(MobEffects.SPEED, 400, 2));
        player.addEffect(new MobEffectInstance(MobEffects.HASTE, 400, 1));
        level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.6D, player.getZ(),
            24, 0.5D, 0.4D, 0.5D, 0.04D);
        play(level, SoundEvents.BLAZE_SHOOT, player.getX(), player.getY(), player.getZ(), 0.5F, 1.6F);
        message(player, "§aВетреный шаг: скорость и проворство.");
    }

    // ------------------------------------------------------------------ //
    //  5. Око ночи                                                         //
    // ------------------------------------------------------------------ //
    private static void nightVision(ServerPlayer player, ServerLevel level) {
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 1200, 0));
        level.sendParticles(new DustParticleOptions(0x2244AA, 1.4F),
                player.getX(), player.getY() + 1.4D, player.getZ(), 30, 0.6D, 0.8D, 0.6D, 0.02D);
        play(level, SoundEvents.BEACON_AMBIENT, player.getX(), player.getY(), player.getZ(), 0.6F, 1.1F);
        message(player, "§9Око ночи: тьма отступает.");
    }

    // ------------------------------------------------------------------ //
    //  6. Дыхание бездны                                                   //
    // ------------------------------------------------------------------ //
    private static void waterBreathing(ServerPlayer player, ServerLevel level) {
        player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 1200, 0));
        player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 600, 0));
        level.sendParticles(ParticleTypes.BUBBLE, player.getX(), player.getY() + 1.0D, player.getZ(),
                32, 0.4D, 1.2D, 0.4D, 0.06D);
        play(level, SoundEvents.PLAYER_HURT_DROWN, player.getX(), player.getY(), player.getZ(), 0.4F, 1.4F);
        message(player, "§bДыхание бездны: воздух в лёгких.");
    }

    // ------------------------------------------------------------------ //
    //  7. Щит поглощения                                                   //
    // ------------------------------------------------------------------ //
    private static void absorption(ServerPlayer player, ServerLevel level) {
        player.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 600, 2));
        player.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 300, 1));
        level.sendParticles(ParticleTypes.GLOW, player.getX(), player.getY() + 1.0D, player.getZ(),
            28, 0.7D, 1.0D, 0.7D, 0.03D);
        play(level, SoundEvents.BEACON_ACTIVATE, player.getX(), player.getY(), player.getZ(), 0.7F, 1.2F);
        message(player, "§eЩит поглощения: урон рассеивается.");
    }

    // ------------------------------------------------------------------ //
    //  8. Громовой откат                                                   //
    // ------------------------------------------------------------------ //
    private static void knockback(ServerPlayer player, ServerLevel level) {
        Vec3 look = player.getViewVector(1.0F);
        double dx = look.x, dz = look.z;
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 0.001D) { dx = 0.0D; dz = 1.0D; len = 1.0D; }
        dx /= len;
        dz /= len;

        for (LivingEntity e : nearby(player, 5.0D)) {
            e.hurtServer(level, player.damageSources().magic(), 3.0F);
            e.knockback(1.8D, dx, dz);
            e.setDeltaMovement(e.getDeltaMovement().add(0.0D, 0.5D, 0.0D));
            level.sendParticles(ParticleTypes.CRIT, e.getX(), e.getY() + 1.0D, e.getZ(),
                    16, 0.5D, 0.7D, 0.5D, 0.08D);
        }
        play(level, SoundEvents.ANVIL_LAND, player.getX(), player.getY(), player.getZ(), 0.8F, 1.4F);
        message(player, "§eГромовой откат: враги отброшены!");
    }

    // ------------------------------------------------------------------ //
    //  9. Искрящий взрыв                                                   //
    // ------------------------------------------------------------------ //
    private static void sparkBurst(ServerPlayer player, ServerLevel level) {
        for (LivingEntity e : nearby(player, 5.0D)) {
            e.hurtServer(level, player.damageSources().magic(), 6.0F);
        }
        double x = player.getX(), y = player.getY() + 0.8D, z = player.getZ();
        level.sendParticles(ParticleTypes.EXPLOSION, x, y, z, 8, 0.6D, 0.6D, 0.6D, 0.0D);
        level.sendParticles(ParticleTypes.CRIT, x, y, z, 80, 1.6D, 1.2D, 1.6D, 0.14D);
        level.sendParticles(new DustParticleOptions(0xFFDD55, 1.5F), x, y, z,
                70, 2.0D, 1.5D, 2.0D, 0.10D);
        play(level, SoundEvents.FIREWORK_ROCKET_BLAST, x, y, z, 1.0F, 1.0F);
        message(player, "§6Искрящий взрыв: искры во все стороны!");
    }

    // ------------------------------------------------------------------ //
    //  10. Прыжок сквозь пространство (телепорт на 5 блоков вперёд)        //
    // ------------------------------------------------------------------ //
    private static void blink(ServerPlayer player, ServerLevel level) {
        Vec3 look = player.getViewVector(1.0F);
        double dx = look.x, dz = look.z;
        double len = Math.sqrt(dx * dx + dz * dz);
        if (len < 0.001D) { dx = 0.0D; dz = 1.0D; len = 1.0D; }
        dx /= len;
        dz /= len;

        double ox = player.getX(), oy = player.getY(), oz = player.getZ();
        level.sendParticles(ParticleTypes.END_ROD, ox, oy + 1.0D, oz, 40, 0.5D, 1.0D, 0.5D, 0.12D);
        player.teleportTo(ox + dx * 5.0D, oy, oz + dz * 5.0D);
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0D, player.getZ(),
                40, 0.5D, 1.0D, 0.5D, 0.12D);
        play(level, SoundEvents.CHORUS_FRUIT_TELEPORT, ox, oy, oz, 1.0F, 1.2F);
        message(player, "§5Прыжок сквозь пространство: 5 блоков вперёд.");
    }

    // ------------------------------------------------------------------ //
    //  11. Грозовой призыв (дождь + гроза + урон молнией)                  //
    // ------------------------------------------------------------------ //
    private static void storm(ServerPlayer player, ServerLevel level) {
        level.setWeatherParameters(0, 6000, true, true);
        Vec3 hit = player.getEyePosition().add(player.getViewVector(1.0F).scale(8.0D));
        level.sendParticles(ParticleTypes.ELECTRIC_SPARK, hit.x, hit.y, hit.z,
                90, 1.4D, 2.0D, 1.4D, 0.25D);
        level.sendParticles(ParticleTypes.END_ROD, hit.x, hit.y, hit.z,
                30, 1.0D, 1.5D, 1.0D, 0.18D);
        play(level, SoundEvents.LIGHTNING_BOLT_THUNDER, hit.x, hit.y, hit.z, 1.0F, 1.0F);
        for (LivingEntity e : nearby(player, 8.0D)) {
            e.hurtServer(level, player.damageSources().lightningBolt(), 5.0F);
        }
        message(player, "§bГрозовой призыв: небо обрушило дождь и молнии!");
    }

    // ------------------------------------------------------------------ //
    //  12. Сила титана                                                     //
    // ------------------------------------------------------------------ //
    private static void strength(ServerPlayer player, ServerLevel level) {
        player.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 600, 2));
        level.sendParticles(new DustParticleOptions(0xFF3333, 1.3F),
                player.getX(), player.getY() + 1.0D, player.getZ(), 40, 0.7D, 1.0D, 0.7D, 0.05D);
        play(level, SoundEvents.BEACON_POWER_SELECT, player.getX(), player.getY(), player.getZ(), 0.7F, 0.9F);
        message(player, "§cСила титана: удар стал смертоносным.");
    }

    // ------------------------------------------------------------------ //
    //  13. Пружина холма                                                   //
    // ------------------------------------------------------------------ //
    private static void jump(ServerPlayer player, ServerLevel level) {
        player.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 600, 2));
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 600, 0));
        player.setDeltaMovement(player.getDeltaMovement().add(0.0D, 0.9D, 0.0D));
        level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 0.2D, player.getZ(),
                28, 0.6D, 0.3D, 0.6D, 0.05D);
        play(level, SoundEvents.CHORUS_FLOWER_GROW, player.getX(), player.getY(), player.getZ(), 0.6F, 1.4F);
        message(player, "§aПружина холма: прыжок высоко и мягко.");
    }

    // ------------------------------------------------------------------ //
    //  14. Пепел увядания                                                   //
    // ------------------------------------------------------------------ //
    private static void wither(ServerPlayer player, ServerLevel level) {
        for (LivingEntity e : nearby(player, 6.0D)) {
            e.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 1));
            e.hurtServer(level, player.damageSources().wither(), 3.0F);
            level.sendParticles(ParticleTypes.SOUL_FIRE_FLAME, e.getX(), e.getY() + 1.0D, e.getZ(),
                    24, 0.5D, 0.9D, 0.5D, 0.06D);
        }
        play(level, SoundEvents.WITHER_SHOOT, player.getX(), player.getY(), player.getZ(), 0.8F, 1.0F);
        message(player, "§8Пепел увядания: враги гниют заживо.");
    }

    // ------------------------------------------------------------------ //
    //  15. Чистый родник (снять эффекты и огонь с себя)                    //
    // ------------------------------------------------------------------ //
    private static void cleanse(ServerPlayer player, ServerLevel level) {
        player.removeAllEffects();
        player.clearFire();
        player.setTicksFrozen(0);
        level.sendParticles(ParticleTypes.CLOUD, player.getX(), player.getY() + 1.0D, player.getZ(),
                32, 0.6D, 1.2D, 0.6D, 0.04D);
        play(level, SoundEvents.FIRE_EXTINGUISH, player.getX(), player.getY(), player.getZ(), 0.8F, 1.2F);
        message(player, "§fЧистый родник: все эффекты сняты.");
    }

    // ------------------------------------------------------------------ //
    //  16. Кристалл опыта                                                  //
    // ------------------------------------------------------------------ //
    private static void experience(ServerPlayer player, ServerLevel level) {
        player.giveExperiencePoints(120);
        level.sendParticles(ParticleTypes.ENCHANTED_HIT, player.getX(), player.getY() + 1.0D, player.getZ(),
                30, 0.5D, 1.0D, 0.5D, 0.06D);
        level.sendParticles(ParticleTypes.GLOW, player.getX(), player.getY() + 1.0D, player.getZ(),
                20, 0.6D, 0.8D, 0.6D, 0.03D);
        play(level, SoundEvents.EXPERIENCE_ORB_PICKUP, player.getX(), player.getY(), player.getZ(), 1.0F, 1.0F);
        message(player, "§6Кристалл опыта: +120 опыта.");
    }

    // ------------------------------------------------------------------ //
    //  17. Вечный пир                                                      //
    // ------------------------------------------------------------------ //
    private static void feast(ServerPlayer player, ServerLevel level) {
        player.getFoodData().eat(12, 8.0F);
        player.addEffect(new MobEffectInstance(MobEffects.SATURATION, 200, 0));
        level.sendParticles(ParticleTypes.HEART, player.getX(), player.getY() + 1.2D, player.getZ(),
                18, 0.4D, 0.8D, 0.4D, 0.04D);
        play(level, SoundEvents.PLAYER_BURP, player.getX(), player.getY(), player.getZ(), 1.0F, 1.0F);
        message(player, "§6Вечный пир: голод утолён.");
    }

    // ------------------------------------------------------------------ //
    //  18. Тень странника                                                  //
    // ------------------------------------------------------------------ //
    private static void invisibility(ServerPlayer player, ServerLevel level) {
        player.addEffect(new MobEffectInstance(MobEffects.INVISIBILITY, 600, 0));
        level.sendParticles(ParticleTypes.SMOKE, player.getX(), player.getY() + 0.8D, player.getZ(),
                30, 0.5D, 1.2D, 0.5D, 0.03D);
        play(level, SoundEvents.ENDERMAN_TELEPORT, player.getX(), player.getY(), player.getZ(), 0.7F, 1.3F);
        message(player, "§8Тень странника: ты стал невидимым.");
    }

    // ------------------------------------------------------------------ //
    //  19. Магнит вселенной (притянуть всё вокруг к себе)                  //
    // ------------------------------------------------------------------ //
    private static void magnet(ServerPlayer player, ServerLevel level) {
        for (Entity e : level.getEntities(player, player.getBoundingBox().inflate(10.0D), e -> e != player)) {
            Vec3 to = new Vec3(player.getX() - e.getX(), player.getY() - e.getY(), player.getZ() - e.getZ());
            double distance = to.length();
            if (distance > 0.6D && distance < 10.0D) {
                double speed = Math.min(1.4D, 8.0D / distance);
                e.setDeltaMovement(to.scale(speed / distance).add(0.0D, 0.12D, 0.0D));
            }
        }
        level.sendParticles(ParticleTypes.END_ROD, player.getX(), player.getY() + 1.0D, player.getZ(),
                50, 1.2D, 1.2D, 1.2D, 0.10D);
        play(level, SoundEvents.BEACON_POWER_SELECT, player.getX(), player.getY(), player.getZ(), 0.8F, 1.6F);
        message(player, "§5Магнит вселенной: всё тянется к тебе.");
    }

    // ------------------------------------------------------------------ //
    //  Утилиты                                                             //
    // ------------------------------------------------------------------ //
    private static List<LivingEntity> nearby(ServerPlayer player, double radius) {
        List<LivingEntity> result = new ArrayList<>();
        for (Entity e : player.level().getEntities(player, player.getBoundingBox().inflate(radius),
                e -> e != player && e instanceof LivingEntity)) {
            result.add((LivingEntity) e);
        }
        return result;
    }

    private static void play(ServerLevel level, SoundEvent sound,
                             double x, double y, double z, float volume, float pitch) {
        level.playSound(null, x, y, z, sound, SoundSource.PLAYERS, volume, pitch);
    }

    private static void message(ServerPlayer player, String text) {
        player.displayClientMessage(Component.literal(text), true);
    }
}
