package com.magicalmod.artifact;

import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/** Обработка правого клика по артефактам (вызови register() из MagicalMod.onInitialize). */
public final class ArtifactUseHandler {
    private ArtifactUseHandler() {}

    public static void register() {
        UseItemCallback.EVENT.register(ArtifactUseHandler::onUseItem);
    }

    private static InteractionResult onUseItem(Player player, Level world, InteractionHand hand) {
        // Никакой логики на клиенте — только серверный сайд.
        if (world.isClientSide()) return InteractionResult.PASS;
        if (!(player instanceof ServerPlayer serverPlayer)) return InteractionResult.PASS;

        ItemStack stack = player.getItemInHand(hand);
        if (!Artifacts.isArtifact(stack)) return InteractionResult.PASS;

        ArtifactEffects.apply(serverPlayer, Artifacts.getId(stack), stack);
        return InteractionResult.CONSUME;
    }
}
