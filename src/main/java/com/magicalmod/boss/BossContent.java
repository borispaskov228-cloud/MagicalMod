package com.magicalmod.boss;

import java.util.List;

import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;

/**
 * Common-side registration and public entry points for the Archmage boss.
 *
 * <p>Call {@link #register()} once from your ModInitializer, then use
 * {@link #spawn(ServerLevel, Vec3)} or {@link #BOSS_SUMMONER} to create the boss.
 */
public final class BossContent {
    public static final String MOD_ID = "magicalmod";

    public static final Identifier BOSS_ID = id("archmage_boss");
    public static final Identifier SUMMONER_ID = id("boss_summoner");
    public static final Identifier CORE_ID = id("boss_core");

    public static final EntityType<ArchmageBoss> BOSS_ENTITY_TYPE = EntityType.Builder
            .of(ArchmageBoss::new, MobCategory.MONSTER)
            .sized(1.0F, 2.6F)
            .clientTrackingRange(10)
            .updateInterval(3)
            .fireImmune()
            .build(ResourceKey.create(Registries.ENTITY_TYPE, BOSS_ID));

    public static final Item BOSS_SUMMONER = new BossSummonerItem();
    public static final Item BOSS_CORE = new BossCoreItem();

    private static boolean registered = false;

    private BossContent() {}

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    /** Registers the entity type, its attributes and both items. Safe to call more than once. */
    public static void register() {
        if (registered) return;
        registered = true;

        Registry.register(BuiltInRegistries.ENTITY_TYPE, BOSS_ID, BOSS_ENTITY_TYPE);
        FabricDefaultAttributeRegistry.register(BOSS_ENTITY_TYPE, ArchmageBoss.createAttributes());
        Registry.register(BuiltInRegistries.ITEM, SUMMONER_ID, BOSS_SUMMONER);
        Registry.register(BuiltInRegistries.ITEM, CORE_ID, BOSS_CORE);
    }

    /** Spawns the boss at the given position and returns the spawned entity. */
    public static ArchmageBoss spawn(ServerLevel level, Vec3 pos) {
        ArchmageBoss boss = new ArchmageBoss(BOSS_ENTITY_TYPE, level);
        boss.setPos(pos);
        boss.setDeltaMovement(Vec3.ZERO);
        level.addFreshEntity(boss);

        level.sendParticles(ParticleTypes.PORTAL, pos.x, pos.y + 1.0D, pos.z,
                60, 1.4D, 2.0D, 1.4D, 0.12D);
        boss.playSound(SoundEvents.ENDER_DRAGON_GROWL, 0.9F, 0.7F);
        return boss;
    }

    /** Drops the boss core plus a few rare components. */
    public static void dropRewards(ServerLevel level, ArchmageBoss boss) {
        for (ItemStack stack : List.of(
                new ItemStack(BOSS_CORE),
                new ItemStack(Items.AMETHYST_SHARD, 8),
                new ItemStack(Items.ENDER_PEARL, 4),
                new ItemStack(Items.NETHERITE_SCRAP, 2))) {
            boss.spawnAtLocation(level, stack);
        }
    }
}
