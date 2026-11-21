package com.magicalmod.net;

import com.magicalmod.MagicalMod;
import com.magicalmod.magic.RitualActions;
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
        // C2S
        PayloadTypeRegistry.playC2S().register(CastSpellPayload.TYPE, CastSpellPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(RitualActionPayload.TYPE, RitualActionPayload.CODEC);
        // S2C
        PayloadTypeRegistry.playS2C().register(RitualStatusPayload.TYPE, RitualStatusPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(CastFxPayload.TYPE, CastFxPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(ManaUpdatePayload.TYPE, ManaUpdatePayload.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(CastSpellPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> SpellCaster.handle(player, payload.spellId()));
        });

        ServerPlayNetworking.registerGlobalReceiver(RitualActionPayload.TYPE, (payload, context) -> {
            ServerPlayer player = context.player();
            context.server().execute(() -> {
                String status = RitualActions.handle(player, payload.action());
                ServerPlayNetworking.send(player, new RitualStatusPayload(status));
            });
        });
    }

    /** Клиент просит открыть меню или выполняет действие круга. */
    public record RitualActionPayload(String action) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<RitualActionPayload> TYPE =
                new CustomPacketPayload.Type<>(MagicalMod.id("ritual_action"));

        public static final StreamCodec<RegistryFriendlyByteBuf, RitualActionPayload> CODEC =
                ByteBufCodecs.STRING_UTF8.map(RitualActionPayload::new, RitualActionPayload::action).cast();

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Сервер присылает текст статуса для меню. */
    public record RitualStatusPayload(String text) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<RitualStatusPayload> TYPE =
                new CustomPacketPayload.Type<>(MagicalMod.id("ritual_status"));

        public static final StreamCodec<RegistryFriendlyByteBuf, RitualStatusPayload> CODEC =
                ByteBufCodecs.STRING_UTF8.map(RitualStatusPayload::new, RitualStatusPayload::text).cast();

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Сервер рассылает параметры анимации заклинания. */
    public record CastFxPayload(String spellId, boolean empowered, String casterUuid,
                                double x, double y, double z) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<CastFxPayload> TYPE =
                new CustomPacketPayload.Type<>(MagicalMod.id("cast_fx"));

        public static final StreamCodec<RegistryFriendlyByteBuf, CastFxPayload> CODEC =
                StreamCodec.composite(
                        ByteBufCodecs.STRING_UTF8, CastFxPayload::spellId,
                        ByteBufCodecs.BOOL, CastFxPayload::empowered,
                        ByteBufCodecs.STRING_UTF8, CastFxPayload::casterUuid,
                        ByteBufCodecs.DOUBLE, CastFxPayload::x,
                        ByteBufCodecs.DOUBLE, CastFxPayload::y,
                        ByteBufCodecs.DOUBLE, CastFxPayload::z,
                        CastFxPayload::new
                ).cast();

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Текущая мана для HUD-полосы. */
    public record ManaUpdatePayload(float mana) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ManaUpdatePayload> TYPE =
                new CustomPacketPayload.Type<>(MagicalMod.id("mana_update"));

        public static final StreamCodec<RegistryFriendlyByteBuf, ManaUpdatePayload> CODEC =
                ByteBufCodecs.FLOAT.map(ManaUpdatePayload::new, ManaUpdatePayload::mana).cast();

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }

    /** Жест клиента → заклинание сервера. */
    public record CastSpellPayload(String spellId) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<CastSpellPayload> TYPE =
                new CustomPacketPayload.Type<>(MagicalMod.id("cast_spell"));

        public static final StreamCodec<RegistryFriendlyByteBuf, CastSpellPayload> CODEC =
                ByteBufCodecs.STRING_UTF8.map(CastSpellPayload::new, CastSpellPayload::spellId).cast();

        @Override public Type<? extends CustomPacketPayload> type() { return TYPE; }
    }
}
