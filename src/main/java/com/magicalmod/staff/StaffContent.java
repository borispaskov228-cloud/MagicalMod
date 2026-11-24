package com.magicalmod.staff;

import java.util.List;

import com.magicalmod.MagicalMod;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;

/** Три посоха, усиливающие магию. Вызовите {@link #register()} в инициализации мода. */
public final class StaffContent {
    private StaffContent() {}

    /** +25% к силе магии. */
    public static final Item STAFF_NOVICE = new StaffItem(
            1.25F,
            "§6Посох новичка",
            Rarity.UNCOMMON,
            List.of(
                    Component.literal("§7Сила заклинаний: §a+25%"),
                    Component.literal("§8Держите в любой руке")
            ));

    /** +60% к силе магии. */
    public static final Item STAFF_ADEPT = new StaffItem(
            1.60F,
            "§bПосох адепта",
            Rarity.RARE,
            List.of(
                    Component.literal("§7Сила заклинаний: §a+60%"),
                    Component.literal("§7Кристалл изумруда усиливает чары"),
                    Component.literal("§8Держите в любой руке")
            ));

    /** +120% к силе магии. */
    public static final Item STAFF_ARCHMAGE = new StaffItem(
            2.20F,
            "§dПосох архимага",
            Rarity.EPIC,
            List.of(
                    Component.literal("§7Сила заклинаний: §a+120%"),
                    Component.literal("§7Эхо кристалла удваивает вашу магию"),
                    Component.literal("§8Держите в любой руке")
            ));

    private static boolean registered;

    /** Регистрирует все три посоха в реестре предметов (безопасно при повторном вызове). */
    public static void register() {
        if (registered) return;
        registered = true;

        // В маппингах 1.21.11 Registries.ITEM — это ResourceKey, а сам реестр — BuiltInRegistries.ITEM.
        Registry.register(BuiltInRegistries.ITEM, MagicalMod.id("staff_novice"), STAFF_NOVICE);
        Registry.register(BuiltInRegistries.ITEM, MagicalMod.id("staff_adept"), STAFF_ADEPT);
        Registry.register(BuiltInRegistries.ITEM, MagicalMod.id("staff_archmage"), STAFF_ARCHMAGE);
    }

    /**
     * Лучший множитель из двух рук: 1.25 / 1.6 / 2.2.
     * Множители НЕ складываются — берётся максимальный.
     */
    public static float multiplier(Player player) {
        if (player == null) return 1.0F;
        return Math.max(
                value(player.getItemInHand(InteractionHand.MAIN_HAND)),
                value(player.getItemInHand(InteractionHand.OFF_HAND)));
    }

    /** Есть ли посох в основной или дополнительной руке. */
    public static boolean hasStaff(Player player) {
        return multiplier(player) > 1.0F;
    }

    private static float value(ItemStack stack) {
        return stack.getItem() instanceof StaffItem staff ? staff.multiplier() : 1.0F;
    }
}
