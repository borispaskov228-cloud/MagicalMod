package com.magicalmod.magic;

import java.util.List;

import com.magicalmod.MagicalMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.crafting.Recipe;

/** Открывает все рецепты мода в ванильной книге рецептов сразу при входе. */
public final class RecipeBookUnlocker {
    private RecipeBookUnlocker() {}

    private static final List<ResourceKey<Recipe<?>>> RECIPES = List.of(
            key("staff_novice"),
            key("staff_adept"),
            key("staff_archmage"),
            key("mana_potion_small"),
            key("mana_potion_medium"),
            key("mana_potion_large"),
            key("boss_summoner")
    );

    private static ResourceKey<Recipe<?>> key(String path) {
        return ResourceKey.<Recipe<?>>create(Registries.RECIPE, MagicalMod.id(path));
    }

    public static void unlock(ServerPlayer player) {
        player.awardRecipesByKey(RECIPES);
    }
}
