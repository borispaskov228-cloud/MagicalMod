package com.magicalmod.magic;

import com.magicalmod.net.ModPayloads.OpenGuidePayload;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Правый клик по книге открывает красивый GUI-гид. */
public final class GuideBookHandler {
    private GuideBookHandler() {}

    private static final String TITLE_MARKER = "MagicalMod — инструкция";

    public static void register() {
        UseItemCallback.EVENT.register(GuideBookHandler::onUse);
    }

    private static InteractionResult onUse(Player player, Level world, InteractionHand hand) {
        if (world.isClientSide()) return InteractionResult.PASS;

        ItemStack stack = player.getItemInHand(hand);
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;
        if (stack.getCustomName() == null) return InteractionResult.PASS;
        if (!stack.getCustomName().getString().startsWith(TITLE_MARKER)) return InteractionResult.PASS;

        ServerPlayNetworking.send(serverPlayer, new OpenGuidePayload());
        return InteractionResult.CONSUME;
    }
}
