package com.magicalmod.mixin;

import com.magicalmod.magic.ManaHolder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerPlayer.class)
public abstract class ServerPlayerMixin implements ManaHolder {
    @Unique private float magicalmod$mana = ManaHolder.BASE_MAX_MANA;
    @Unique private int magicalmod$level = 0;
    @Unique private int magicalmod$xp = 0;

    @Inject(method = "addAdditionalSaveData", at = @At("HEAD"))
    private void magicalmod$save(ValueOutput output, CallbackInfo ci) {
        output.putFloat("magicalmod:mana", magicalmod$mana);
        output.putInt("magicalmod:mana_level", magicalmod$level);
        output.putInt("magicalmod:mana_xp", magicalmod$xp);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("HEAD"))
    private void magicalmod$load(ValueInput input, CallbackInfo ci) {
        magicalmod$level = Math.max(0, Math.min(ManaHolder.MAX_LEVEL,
                input.getIntOr("magicalmod:mana_level", 0)));
        magicalmod$xp = Math.max(0, Math.min(ManaHolder.XP_PER_LEVEL - 1,
                input.getIntOr("magicalmod:mana_xp", 0)));
        magicalmod$mana = Math.max(0.0F, Math.min(magicalmod$getMaxMana(),
                input.getFloatOr("magicalmod:mana", ManaHolder.BASE_MAX_MANA)));
    }

    @Override public float magicalmod$getMana() { return magicalmod$mana; }
    @Override public void magicalmod$setMana(float mana) {
        magicalmod$mana = Math.max(0.0F, Math.min(magicalmod$getMaxMana(), mana));
    }
    @Override public int magicalmod$getManaLevel() { return magicalmod$level; }
    @Override public void magicalmod$setManaLevel(int level) {
        magicalmod$level = Math.max(0, Math.min(ManaHolder.MAX_LEVEL, level));
    }
    @Override public int magicalmod$getManaXp() { return magicalmod$xp; }
    @Override public void magicalmod$setManaXp(int xp) {
        magicalmod$xp = Math.max(0, Math.min(ManaHolder.XP_PER_LEVEL - 1, xp));
    }
}
