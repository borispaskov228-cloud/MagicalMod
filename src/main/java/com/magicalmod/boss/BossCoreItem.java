package com.magicalmod.boss;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;

/** Ядро босса — редкая награда за победу. */
public final class BossCoreItem extends Item {
    public BossCoreItem() {
        super(new Item.Properties()
                .stacksTo(16)
                .rarity(Rarity.EPIC)
                .component(DataComponents.CUSTOM_NAME,
                        Component.literal("Ядро Архимага").withStyle(ChatFormatting.DARK_PURPLE))
                .component(DataComponents.LORE, new ItemLore(List.of(
                        Component.literal("§7Стабильный кристалл пустотной магии"),
                        Component.literal("§7Добывается с Хранителя Гримуара")
                ))));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        // Чисто декоративный предмет — клик ничего не делает.
        return InteractionResult.PASS;
    }
}
