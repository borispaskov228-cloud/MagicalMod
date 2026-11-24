package com.magicalmod.magic;

import java.util.List;

import com.magicalmod.content.ModContent;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.Level;

/** Зелья маны: крафтовые предметы, восстанавливающие ману. */
public final class ManaPotions {
    private ManaPotions() {}

    private static final Item[] ITEMS = {
            ModContent.MANA_POTION_SMALL,
            ModContent.MANA_POTION_MEDIUM,
            ModContent.MANA_POTION_LARGE
    };
    private static final float[] AMOUNTS = {30.0F, 60.0F, 100.0F};
    public static final int COUNT = 3;

    public static ItemStack createStack(int tier) {
        ItemStack stack = new ItemStack(ITEMS[tier]);
        stack.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("§7Восстанавливает " + Math.round(AMOUNTS[tier]) + " маны"),
                Component.literal("§8Правый клик — выпить")
        )));
        return stack;
    }

    public static boolean isPotion(ItemStack stack) {
        return tier(stack) >= 0;
    }

    private static int tier(ItemStack stack) {
        if (stack.isEmpty()) return -1;
        for (int i = 0; i < ITEMS.length; i++) {
            if (stack.is(ITEMS[i])) return i;
        }
        return -1;
    }

    private static float amount(ItemStack stack) {
        return AMOUNTS[tier(stack)];
    }

    public static void register() {
        UseItemCallback.EVENT.register(ManaPotions::onUse);
    }

    private static InteractionResult onUse(Player player, Level world, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!isPotion(stack) || world.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.PASS;
        }

        float before = ManaManager.get(serverPlayer);
        ManaManager.add(serverPlayer, amount(stack));
        serverPlayer.displayClientMessage(Component.literal(
                "§b+" + Math.round(ManaManager.get(serverPlayer) - before) + " маны"
        ), true);
        player.getCooldowns().addCooldown(stack, 40);
        stack.shrink(1);
        return InteractionResult.CONSUME;
    }
}
