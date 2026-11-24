package com.magicalmod.boss;

import java.util.List;

import com.magicalmod.MagicalMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;

import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Правый клик — призывает Архимага Пустоты перед игроком. */
public final class BossSummonerItem extends Item {
    public BossSummonerItem() {
        super(new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM, MagicalMod.id("boss_summoner")))
                .stacksTo(1)
                .rarity(Rarity.EPIC)
                .component(DataComponents.CUSTOM_NAME,
                        Component.literal("Призыватель Архимага").withStyle(ChatFormatting.DARK_PURPLE))
                .component(DataComponents.LORE, new ItemLore(List.of(
                        Component.literal("§7Правый клик — призвать Хранителя Гримуара"),
                        Component.literal("§8Босс имеет 3 фазы и уязвим к антимагии")
                ))));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.PASS;

        ItemStack stack = player.getItemInHand(hand);
        if (player.getCooldowns().isOnCooldown(stack)) return InteractionResult.PASS;

        Vec3 look = player.getLookAngle();
        Vec3 pos = player.position().add(look.scale(5.0D));
        BossContent.spawn(serverLevel, pos);

        if (!player.isCreative()) stack.shrink(1);
        player.getCooldowns().addCooldown(stack, 200);
        player.displayClientMessage(
                Component.literal("§5Архимаг Пустоты §7был призван!"), true);
        return InteractionResult.CONSUME;
    }
}
