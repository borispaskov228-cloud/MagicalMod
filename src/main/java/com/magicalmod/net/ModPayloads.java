package com.magicalmod.net;

import com.magicalmod.MagicalMod;
import com.magicalmod.magic.SpellCaster;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

public final class ModPayloads {
    private ModPayloads() {}

    public static void register() {
        PayloadTypeRegistry.playC2S().register(CastSpellPayload.TYPE, CastSpellPayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(CastSpellPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> SpellCaster.handle(player, payload.spellId()));
        });
    }

    public record CastSpellPayload(String spellId) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<CastSpellPayload> TYPE =
                new CustomPacketPayload.Type<>(MagicalMod.id("cast_spell"));

        public static final StreamCodec<RegistryFriendlyByteBuf, CastSpellPayload> CODEC =
                ByteBufCodecs.STRING_UTF8.map(CastSpellPayload::new, CastSpellPayload::spellId).cast();

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
}
