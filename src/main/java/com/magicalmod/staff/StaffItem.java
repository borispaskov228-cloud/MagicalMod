package com.magicalmod.staff;

import java.util.List;

import com.magicalmod.MagicalMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.level.Level;

/** Базовый посох: хранит множитель силы и даёт маленький эффект при правом клике. */
final class StaffItem extends Item {
    private final float multiplier;

    StaffItem(String idPath, float multiplier, String displayName, Rarity rarity, List<Component> lore) {
        super(new Item.Properties()
                .setId(ResourceKey.create(Registries.ITEM, MagicalMod.id(idPath)))
                .stacksTo(1)
                .rarity(rarity)
                .component(DataComponents.CUSTOM_NAME, Component.literal(displayName))
                .component(DataComponents.LORE, new ItemLore(lore)));
        this.multiplier = multiplier;
    }

    float multiplier() {
        return this.multiplier;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        // Искры вокруг держителя посоха — без новых сущностей и рендереров.
        level.addParticle(ParticleTypes.ENCHANT,
                player.getX(), player.getY() + 1.0D, player.getZ(),
                0.0D, 0.5D, 0.0D);
        player.getCooldowns().addCooldown(stack, 20);

        return InteractionResult.SUCCESS;
    }
}
