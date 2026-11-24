package com.magicalmod.content;

import com.magicalmod.MagicalMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

/** Светящееся магическое растение, при сломе дающее {@link ModContent#MANA_HERB_ITEM}. */
public class ManaHerbBlock extends Block {

    public ManaHerbBlock() {
        super(BlockBehaviour.Properties.of()
                // В 1.21.11 у каждого блока обязан быть задан ID ещё в Properties
                .setId(ResourceKey.create(Registries.BLOCK, MagicalMod.id("mana_herb")))
                .noCollision()
                .instabreak()
                .sound(SoundType.GRASS)
                .mapColor(MapColor.PLANT)
                .lightLevel(state -> 4));
    }

    /**
     * В 1.21.11 метод находится в {@code BlockBehaviour} (не в {@code Block})
     * и вызывается из {@code BlockState.spawnAfterBreak(...)} при разрушении блока.
     */
    @Override
    protected void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos,
                                   ItemStack stack, boolean dropExperience) {
        super.spawnAfterBreak(state, level, pos, stack, dropExperience);
        if (!level.isClientSide()) {
            Block.popResource(level, pos, new ItemStack(ModContent.MANA_HERB_ITEM));
        }
    }
}
