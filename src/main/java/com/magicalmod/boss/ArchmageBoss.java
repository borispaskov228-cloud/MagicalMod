package com.magicalmod.boss;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.BossEvent.BossBarColor;
import net.minecraft.world.BossEvent.BossBarOverlay;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Vex;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.hurtingprojectile.SmallFireball;
import net.minecraft.world.entity.projectile.throwableitemprojectile.Snowball;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Архимаг Пустоты — парящий босс с тремя фазами.
 *
 * <ul>
 *   <li>Фаза 1 (70-100% HP): стихийные снаряды, барьеры, зоны телепортации.</li>
 *   <li>Фаза 2 (30-70% HP): 3-4 вксина, пока живы — босс имеет щит.</li>
 *   <li>Фаза 3 (&lt;30% HP): быстрый каст, притяжение, порталы с рунами.</li>
 * </ul>
 */
public class ArchmageBoss extends Mob {
    /** Magic damage pierces the magical shell: a simple, safe antimagic vulnerability. */
    public static final float ANTI_MAGIC_MULTIPLIER = 1.75F;

    private static final float PHASE_2_AT = 0.70F;
    private static final float PHASE_3_AT = 0.30F;
    private static final int TELEPORT_TICKS = 45;
    private static final int RUNE_TICKS = 55;

    private int phase = 1;
    private int abilityTimer = 60;
    private int animationTick = 0;
    private boolean shieldActive = false;

    private final List<UUID> minions = new ArrayList<>();
    private final List<Hazard> hazards = new ArrayList<>();
    private final ServerBossEvent bossBar = new ServerBossEvent(
            Component.literal("§5Архимаг Пустоты §7— Хранитель Гримуара"),
            BossBarColor.PURPLE, BossBarOverlay.PROGRESS);

    public ArchmageBoss(EntityType<? extends ArchmageBoss> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.bossBar.setProgress(1.0F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 260.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.24D)
                .add(Attributes.FOLLOW_RANGE, 48.0D)
                .add(Attributes.ATTACK_DAMAGE, 12.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 0.6D);
    }

    /** 1, 2 or 3. Public so the client renderer and integrations can read it. */
    public int getPhase() {
        return this.phase;
    }

    public boolean isShieldActive() {
        return this.shieldActive;
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Архимаг Пустоты");
    }

    @Override
    protected int getBaseExperienceReward(ServerLevel level) {
        return 100;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        float damage = amount;
        if (isAntimagic(source)) {
            damage *= ANTI_MAGIC_MULTIPLIER;
            // "Светоснятие": антимагия сбивает щит, данный приспешниками.
            breakShield();
            Vec3 p = this.position();
            level.sendParticles(ParticleTypes.END_ROD, p.x, p.y + 1.2D, p.z,
                    28, 0.9D, 1.2D, 0.9D, 0.25D);
            this.playSound(SoundEvents.GLASS_BREAK, 0.8F, 1.2F);
        }
        return super.hurtServer(level, source, damage);
    }

    private static boolean isAntimagic(DamageSource source) {
        String msg = source.getMsgId();
        return source.is(DamageTypes.MAGIC)
                || source.is(DamageTypes.INDIRECT_MAGIC)
                || source.is(DamageTypes.WITHER)
                || source.is(DamageTypes.DRAGON_BREATH)
                || msg.contains("magic");
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        this.animationTick++;
        updatePhase(level);
        updateTarget(level);
        updateMovement();
        updateBossBar(level);
        updateHazards(level);
        updateMinionShield(level);

        if (this.abilityTimer > 0) {
            this.abilityTimer--;
            return;
        }

        LivingEntity target = this.getTarget();
        if (target == null || !target.isAlive()) {
            this.abilityTimer = 20;
            return;
        }

        switch (this.phase) {
            case 1 -> {
                castElements(level, target);
                this.abilityTimer = 48 + this.random.nextInt(18);
                if (this.random.nextInt(3) == 0) castBarrier(level);
                if (this.random.nextInt(4) == 0) beginTeleportZone(level, target);
            }
            case 2 -> {
                castElements(level, target);
                this.abilityTimer = 36 + this.random.nextInt(12);
                if (this.random.nextBoolean()) castBarrier(level);
                if (this.random.nextInt(3) == 0) beginTeleportZone(level, target);
                if (countLivingMinions(level) == 0 && this.random.nextInt(3) == 0) summonMinions(level, target);
            }
            default -> {
                castElements(level, target);
                pullTarget(target);
                this.abilityTimer = 20 + this.random.nextInt(8);
                if (this.random.nextBoolean()) spawnRune(level, target);
                if (this.random.nextInt(4) == 0) beginTeleportZone(level, target);
            }
        }
    }

    private void updatePhase(ServerLevel level) {
        float ratio = this.getHealth() / Math.max(1.0F, this.getMaxHealth());
        int next = ratio > PHASE_2_AT ? 1 : (ratio > PHASE_3_AT ? 2 : 3);
        if (next == this.phase) return;

        this.phase = next;
        this.abilityTimer = 20;
        Vec3 p = this.position();
        level.sendParticles(ParticleTypes.PORTAL, p.x, p.y + 1.0D, p.z,
                90, 2.0D, 2.6D, 2.0D, 0.2D);
        level.sendParticles(ParticleTypes.END_ROD, p.x, p.y + 1.0D, p.z,
                40, 1.2D, 1.6D, 1.2D, 0.18D);
        this.playSound(SoundEvents.ENDER_DRAGON_GROWL, 1.0F, 0.8F);
        if (next == 2) summonMinions(level, this.getTarget());
        if (next == 3) this.bossBar.setColor(BossBarColor.RED);
    }

    private void updateTarget(ServerLevel level) {
        LivingEntity current = this.getTarget();
        if (current != null && current.isAlive() && this.distanceToSqr(current) < 64.0D * 64.0D) return;

        LivingEntity best = null;
        double bestDist = Double.MAX_VALUE;
        for (Player player : level.getPlayers(p -> p.isAlive() && !p.isCreative())) {
            double d = this.distanceToSqr(player);
            if (d < bestDist) {
                bestDist = d;
                best = player;
            }
        }
        this.setTarget(best);
    }

    private void updateMovement() {
        LivingEntity target = this.getTarget();
        if (target == null) {
            this.getMoveControl().setWantedPosition(this.getX(), this.getY(), this.getZ(), 0.4D);
            return;
        }
        double dist = this.distanceTo(target);
        double radius = this.phase == 3 ? 7.0D : 10.0D;
        Vec3 to = target.position().subtract(this.position());
        double x = this.getX();
        double y = target.getY() + 2.2D + Math.sin(this.animationTick * 0.04F) * 0.6D;
        double z = this.getZ();
        if (dist > radius) {
            x = target.getX() - to.x / dist * radius;
            z = target.getZ() - to.z / dist * radius;
        }
        double speed = this.phase == 3 ? 1.15D : (this.phase == 2 ? 0.9D : 0.7D);
        this.getMoveControl().setWantedPosition(x, y, z, speed);
        if (dist < 24.0D) this.getLookControl().setLookAt(target, 30.0F, 30.0F);
    }

    private void updateBossBar(ServerLevel level) {
        this.bossBar.setProgress(this.getHealth() / Math.max(1.0F, this.getMaxHealth()));
        for (Player player : level.getPlayers(p -> this.distanceToSqr(p) < 96.0D * 96.0D)) {
            this.bossBar.addPlayer((net.minecraft.server.level.ServerPlayer) player);
        }
        Collection<net.minecraft.server.level.ServerPlayer> viewers = this.bossBar.getPlayers();
        for (net.minecraft.server.level.ServerPlayer viewer : List.copyOf(viewers)) {
            if (viewer.isDeadOrDying() || this.distanceToSqr(viewer) > 112.0D * 112.0D) {
                this.bossBar.removePlayer(viewer);
            }
        }
        this.bossBar.setVisible(this.isAlive());
    }

    private void castElements(ServerLevel level, LivingEntity target) {
        Vec3 origin = this.position().add(0.0D, 1.3D, 0.0D);
        Vec3 dir = target.getEyePosition().subtract(origin).normalize();
        Vec3 tip = dir.scale(this.phase == 3 ? 1.35D : 1.0D);
        int element = this.random.nextInt(3);

        if (element == 0) {
            SmallFireball fireball = new SmallFireball(level, this, tip);
            fireball.setPos(origin.add(dir.scale(1.2D)));
            fireball.setDeltaMovement(tip);
            level.addFreshEntity(fireball);
            level.sendParticles(ParticleTypes.FLAME, origin.x, origin.y, origin.z,
                    12, 0.25D, 0.25D, 0.25D, 0.06D);
            this.playSound(SoundEvents.FIRECHARGE_USE, 0.8F, 1.0F);
        } else if (element == 1) {
            Snowball snowball = new Snowball(level, this, ItemStack.EMPTY);
            snowball.setPos(origin.add(dir.scale(1.2D)));
            snowball.setDeltaMovement(tip);
            level.addFreshEntity(snowball);
            target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 80, 0));
            level.sendParticles(ParticleTypes.SNOWFLAKE, origin.x, origin.y, origin.z,
                    16, 0.3D, 0.3D, 0.3D, 0.05D);
            this.playSound(SoundEvents.GLASS_BREAK, 0.5F, 1.3F);
        } else {
            for (int i = 1; i <= 12; i++) {
                Vec3 point = origin.add(dir.scale(i * 0.9D));
                level.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                        point.x, point.y, point.z, 2, 0.22D, 0.22D, 0.22D, 0.02D);
                level.sendParticles(ParticleTypes.END_ROD,
                        point.x, point.y, point.z, 1, 0.12D, 0.12D, 0.12D, 0.01D);
            }
            target.hurtServer(level, level.damageSources().lightningBolt(),
                    this.phase == 3 ? 7.0F : 5.0F);
            this.playSound(SoundEvents.LIGHTNING_BOLT_IMPACT, 0.5F, 1.2F);
        }
        this.playSound(SoundEvents.EVOKER_CAST_SPELL, 0.7F, 1.0F);
    }

    private void castBarrier(ServerLevel level) {
        this.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 120, 1));
        this.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 120, 1));
        Vec3 p = this.position();
        for (int i = 0; i < 24; i++) {
            double a = i * Math.PI / 12.0D;
            level.sendParticles(ParticleTypes.END_ROD,
                    p.x + Math.cos(a) * 1.5D, p.y + 1.2D, p.z + Math.sin(a) * 1.5D,
                    1, 0.0D, 0.0D, 0.0D, 0.02D);
        }
        this.playSound(SoundEvents.BEACON_ACTIVATE, 0.6F, 1.4F);
    }

    private void beginTeleportZone(ServerLevel level, LivingEntity target) {
        Vec3 base = target.position();
        double a = this.random.nextDouble() * Math.PI * 2.0D;
        double r = 2.5D + this.random.nextDouble() * 2.5D;
        Vec3 spot = new Vec3(base.x + Math.cos(a) * r, base.y, base.z + Math.sin(a) * r);
        this.hazards.add(new Hazard(spot, TELEPORT_TICKS, 0));
        this.playSound(SoundEvents.ENDERMAN_TELEPORT, 0.5F, 1.4F);
    }

    private void spawnRune(ServerLevel level, LivingEntity target) {
        Vec3 base = target.position();
        double a = this.random.nextDouble() * Math.PI * 2.0D;
        double r = 1.5D + this.random.nextDouble() * 3.0D;
        Vec3 spot = new Vec3(base.x + Math.cos(a) * r, base.y, base.z + Math.sin(a) * r);
        this.hazards.add(new Hazard(spot, RUNE_TICKS, 1));
        this.playSound(SoundEvents.EVOKER_CAST_SPELL, 0.6F, 0.8F);
    }

    private void updateHazards(ServerLevel level) {
        Iterator<Hazard> it = this.hazards.iterator();
        while (it.hasNext()) {
            Hazard h = it.next();
            h.timer--;
            Vec3 p = h.pos;
            if (h.kind == 0) {
                // Телеграф зоны телепортации: круг на полу.
                for (int i = 0; i < 12; i++) {
                    double a = i * Math.PI / 6.0D;
                    level.sendParticles(ParticleTypes.PORTAL,
                            p.x + Math.cos(a) * 2.0D, p.y + 0.1D, p.z + Math.sin(a) * 2.0D,
                            1, 0.0D, 0.06D, 0.0D, 0.01D);
                }
            } else {
                // Луч из книги в руну.
                Vec3 origin = this.position().add(0.0D, 1.4D, 0.0D);
                Vec3 dir = p.subtract(origin);
                int steps = Math.min(16, (int) dir.length());
                for (int i = 1; i <= steps; i++) {
                    Vec3 q = origin.add(dir.scale(i / (double) Math.max(1, steps)));
                    level.sendParticles(ParticleTypes.GLOW, q.x, q.y, q.z,
                            1, 0.08D, 0.08D, 0.08D, 0.0D);
                }
                level.sendParticles(ParticleTypes.FLAME, p.x, p.y + 0.2D, p.z,
                        6, 0.5D, 0.1D, 0.5D, 0.02D);
            }

            if (h.timer > 0) continue;
            it.remove();

            if (h.kind == 0) {
                this.teleportTo(level, p.x, p.y + 1.0D, p.z, Set.<Relative>of(),
                        this.getYRot(), this.getXRot(), false);
                level.sendParticles(ParticleTypes.PORTAL, p.x, p.y + 1.0D, p.z,
                        40, 1.0D, 1.5D, 1.0D, 0.15D);
                damageAround(level, p, 2.6D, 8.0F);
                this.playSound(SoundEvents.ENDERMAN_TELEPORT, 0.9F, 0.8F);
            } else {
                level.sendParticles(ParticleTypes.EXPLOSION, p.x, p.y + 0.5D, p.z,
                        6, 0.6D, 0.6D, 0.6D, 0.0D);
                level.sendParticles(ParticleTypes.FLAME, p.x, p.y + 0.5D, p.z,
                        40, 1.4D, 0.8D, 1.4D, 0.16D);
                damageAround(level, p, 3.6D, 10.0F);
                this.playSound(SoundEvents.GLASS_BREAK, 0.9F, 0.8F);
            }
        }
    }

    private void damageAround(ServerLevel level, Vec3 center, double radius, float damage) {
        for (Player player : level.getPlayers(p -> p.isAlive() && !p.isCreative())) {
            double dx = player.getX() - center.x;
            double dy = player.getY() - center.y;
            double dz = player.getZ() - center.z;
            double dist = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (dist > radius) continue;
            player.hurtServer(level, level.damageSources().magic(), damage);
            if (dist > 0.001D) {
                Vec3 away = new Vec3(player.getX() - center.x, 0.0D, player.getZ() - center.z)
                        .normalize().scale(0.5D).add(0.0D, 0.25D, 0.0D);
                player.setDeltaMovement(player.getDeltaMovement().add(away));
            }
        }
    }

    private void pullTarget(LivingEntity target) {
        Vec3 toBoss = this.position().subtract(target.position()).multiply(1.0D, 0.25D, 1.0D);
        if (toBoss.length() < 0.001D) return;
        target.setDeltaMovement(target.getDeltaMovement().add(toBoss.normalize().scale(0.16D)));
    }

    private void summonMinions(ServerLevel level, LivingEntity target) {
        int count = 3 + this.random.nextInt(2);
        for (int i = 0; i < count; i++) {
            double a = i * (Math.PI * 2.0D / count) + this.random.nextDouble() * 0.4D;
            Vex vex = new Vex(EntityType.VEX, level);
            vex.setPos(this.getX() + Math.cos(a) * 2.5D,
                    this.getY() + 1.0D,
                    this.getZ() + Math.sin(a) * 2.5D);
            vex.setOwner(this);
            vex.setLimitedLife(1200);
            if (target != null) vex.setTarget(target);
            level.addFreshEntity(vex);
            this.minions.add(vex.getUUID());
            level.sendParticles(ParticleTypes.PORTAL, vex.getX(), vex.getY(), vex.getZ(),
                    20, 0.5D, 0.8D, 0.5D, 0.08D);
        }
        this.playSound(SoundEvents.VEX_CHARGE, 0.9F, 0.9F);
    }

    private int countLivingMinions(ServerLevel level) {
        int alive = 0;
        Iterator<UUID> it = this.minions.iterator();
        while (it.hasNext()) {
            Entity e = level.getEntity(it.next());
            if (e == null || !e.isAlive()) it.remove();
            else alive++;
        }
        return alive;
    }

    private void updateMinionShield(ServerLevel level) {
        boolean alive = countLivingMinions(level) > 0;
        if (alive) {
            this.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 40, 2));
            this.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 40, 3));
            this.shieldActive = true;
        } else if (this.shieldActive) {
            breakShield();
        }
    }

    private void breakShield() {
        this.removeEffect(MobEffects.RESISTANCE);
        this.removeEffect(MobEffects.ABSORPTION);
        this.shieldActive = false;
        this.minions.clear();
    }

    @Override
    public void die(DamageSource source) {
        if (!this.level().isClientSide() && this.level() instanceof ServerLevel level) {
            this.bossBar.removeAllPlayers();
            BossContent.dropRewards(level, this);
        }
        super.die(source);
    }

    private static final class Hazard {
        final Vec3 pos;
        int timer;
        final int kind; // 0 = teleport zone, 1 = explosive rune

        Hazard(Vec3 pos, int timer, int kind) {
            this.pos = pos;
            this.timer = timer;
            this.kind = kind;
        }
    }
}
