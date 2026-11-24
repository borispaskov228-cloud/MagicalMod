package com.magicalmod.content;

import com.magicalmod.MagicalMod;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * Регистрация контента мода: травы маны и зельев маны.
 *
 * <p>Вызов {@link #register()} выполняется из {@code MagicalMod.onInitialize()};
 * этот класс сам себя не регистрирует.
 */
public final class ModContent {
    private ModContent() {}

    public static final Block MANA_HERB_BLOCK = new ManaHerbBlock();

    /** BlockItem, чтобы растение можно было поставить и вырастить/сорвать. */
    public static final Item MANA_HERB_ITEM = new BlockItem(MANA_HERB_BLOCK, new Item.Properties());
    public static final Item MANA_POTION_SMALL = new Item(new Item.Properties());
    public static final Item MANA_POTION_MEDIUM = new Item(new Item.Properties());
    public static final Item MANA_POTION_LARGE = new Item(new Item.Properties());

    /**
     * В 1.21.11 константы вкладок в {@code CreativeModeTabs} приватные,
     * поэтому ключ вкладки «Природные блоки» создаётся вручную по id {@code minecraft:natural_blocks}.
     */
    private static final ResourceKey<CreativeModeTab> NATURAL_TAB = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath("minecraft", "natural_blocks"));

    public static void register() {
        // В 1.21.11 Registries.BLOCK/ITEM — это ResourceKey, а не Registry,
        // поэтому используется BuiltInRegistries.
        Registry.register(BuiltInRegistries.BLOCK, MagicalMod.id("mana_herb"), MANA_HERB_BLOCK);

        Registry.register(BuiltInRegistries.ITEM, MagicalMod.id("mana_herb"), MANA_HERB_ITEM);
        Registry.register(BuiltInRegistries.ITEM, MagicalMod.id("mana_potion_small"), MANA_POTION_SMALL);
        Registry.register(BuiltInRegistries.ITEM, MagicalMod.id("mana_potion_medium"), MANA_POTION_MEDIUM);
        Registry.register(BuiltInRegistries.ITEM, MagicalMod.id("mana_potion_large"), MANA_POTION_LARGE);

        ItemGroupEvents.modifyEntriesEvent(NATURAL_TAB).register(entries -> {
            entries.accept(MANA_HERB_ITEM);
            entries.accept(MANA_POTION_SMALL);
            entries.accept(MANA_POTION_MEDIUM);
            entries.accept(MANA_POTION_LARGE);
        });
    }
}
