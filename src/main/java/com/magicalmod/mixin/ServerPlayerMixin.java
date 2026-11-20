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
    @Unique
    private float magicalmod$mana = ManaHolder.MAX_MANA;

    @Inject(method = "addAdditionalSaveData", at = @At("HEAD"))
    private void magicalmod$save(ValueOutput output, CallbackInfo ci) {
        output.putFloat("magicalmod:mana", magicalmod$mana);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("HEAD"))
    private void magicalmod$load(ValueInput input, CallbackInfo ci) {
        magicalmod$mana = input.getFloatOr("magicalmod:mana", ManaHolder.MAX_MANA);
    }

    @Override
    public float magicalmod$getMana() { return magicalmod$mana; }

    @Override
    public void magicalmod$setMana(float mana) {
        magicalmod$mana = Math.max(0.0F, Math.min(ManaHolder.MAX_MANA, mana));
    }
}
