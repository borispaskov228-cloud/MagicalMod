package com.magicalmod.content;

import com.magicalmod.MagicalMod;
import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.GenerationStep;
import net.minecraft.world.level.levelgen.placement.PlacedFeature;

/** Добавляет траву маны в генерацию оверворлда (описание фичи — в data-паке). */
public final class ManaHerbWorldGen {
    private ManaHerbWorldGen() {}

    private static final ResourceKey<PlacedFeature> FEATURE = ResourceKey.create(
            Registries.PLACED_FEATURE, MagicalMod.id("mana_herb"));

    public static void register() {
        BiomeModifications.addFeature(
                BiomeSelectors.foundInOverworld(),
                GenerationStep.Decoration.VEGETAL_DECORATION,
                FEATURE);
    }
}
