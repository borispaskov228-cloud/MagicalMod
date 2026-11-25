package com.magicalmod.content;

import java.util.ArrayList;
import java.util.List;

import com.magicalmod.MagicalMod;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BonemealableBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

/** Светящееся магическое растение, при сломе дающее {@link ModContent#MANA_HERB_ITEM}. */
public class ManaHerbBlock extends Block implements BonemealableBlock {

    public ManaHerbBlock() {
        super(BlockBehaviour.Properties.of()
                // В 1.21.11 у каждого блока обязан быть задан ID ещё в Properties
                .setId(ResourceKey.create(Registries.BLOCK, MagicalMod.id("mana_herb")))
                .noCollision()
                .instabreak()
                .randomTicks()
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

    /** Растёт только на земле и при достаточно ярком свете. */
    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).is(BlockTags.DIRT)
                && level.getMaxLocalRawBrightness(pos) >= 4;
    }

    /** Медленно расползается по траве — можно вырастить целую плантацию. */
    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (random.nextInt(6) != 0) return;
        if (countNearby(level, pos) >= 7) return;
        spread(level, pos, random, 1);
    }

    // ------------------------------------------------------------ костная мука

    @Override
    public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
        return hasFreeSpot(level, pos, 2);
    }

    @Override
    public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
        return true;
    }

    @Override
    public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
        countNearby(level, pos);
        int spawned = spread(level, pos, random, 2 + random.nextInt(2));
        if (spawned > 0) {
            level.playSound(null, pos, SoundEvents.BONE_MEAL_USE, SoundSource.BLOCKS, 0.8F, 1.0F);
        }
    }

    // ---------------------------------------------------------------- helpers

    private static boolean hasFreeSpot(LevelReader level, BlockPos pos, int radius) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                if (dx == 0 && dz == 0) continue;
                BlockPos p = pos.offset(dx, 0, dz);
                if (level.getBlockState(p).isAir()
                        && level.getBlockState(p.below()).is(BlockTags.DIRT)
                        && level.getMaxLocalRawBrightness(p) >= 7) return true;
            }
        }
        return false;
    }

    private static int countNearby(ServerLevel level, BlockPos pos) {
        int count = 0;
        for (int dx = -3; dx <= 3; dx++) {
            for (int dz = -3; dz <= 3; dz++) {
                if (level.getBlockState(pos.offset(dx, 0, dz)).is(ModContent.MANA_HERB_BLOCK)) count++;
            }
        }
        return count;
    }

    /** Рассаживает до {@code amount} новых растений рядом. Возвращает количество. */
    private static int spread(ServerLevel level, BlockPos pos, RandomSource random, int amount) {
        List<BlockPos> candidates = new ArrayList<>();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dz = -2; dz <= 2; dz++) {
                if (dx == 0 && dz == 0) continue;
                BlockPos p = pos.offset(dx, 0, dz);
                if (level.getBlockState(p).isAir()
                        && level.getBlockState(p.below()).is(BlockTags.DIRT)
                        && level.getMaxLocalRawBrightness(p) >= 7) candidates.add(p);
            }
        }
        int placed = 0;
        while (placed < amount && !candidates.isEmpty()) {
            BlockPos target = candidates.remove(random.nextInt(candidates.size()));
            if (level.getBlockState(target).isAir()) {
                level.setBlockAndUpdate(target, ModContent.MANA_HERB_BLOCK.defaultBlockState());
                placed++;
            }
        }
        return placed;
    }
}
