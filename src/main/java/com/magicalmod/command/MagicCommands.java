package com.magicalmod.command;

import com.magicalmod.magic.Gesture;
import com.magicalmod.magic.ManaManager;
import com.magicalmod.magic.RitualCircle;
import com.magicalmod.magic.RitualManager;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class MagicCommands {
    private MagicCommands() {}

    private static final SimpleCommandExceptionType NO_PLAYER =
            new SimpleCommandExceptionType(Component.literal("Команду можно использовать только в игре"));

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("magic")
                    .then(Commands.literal("mana").executes(MagicCommands::mana))
                    .then(Commands.literal("gestures").executes(MagicCommands::gestures))
                    .then(Commands.literal("circle")
                            .then(Commands.literal("start").executes(MagicCommands::circleStart))
                            .then(Commands.literal("join").executes(MagicCommands::circleJoin))
                            .then(Commands.literal("leave").executes(MagicCommands::circleLeave))
                            .then(Commands.literal("status").executes(MagicCommands::circleStatus))
                            .then(Commands.literal("stop").executes(MagicCommands::circleStop))
                    )
                    .executes(MagicCommands::help)
            );
        });
    }

    private static ServerPlayer player(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer p)) throw NO_PLAYER.create();
        return p;
    }

    private static int help(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSystemMessage(Component.literal(
                "§6Магия §f— /magic mana | /magic gestures | /magic circle start|join|leave|status|stop"
        ));
        return 1;
    }

    private static int mana(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ManaManager.sendMana(player(ctx));
        return 1;
    }

    private static int gestures(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSystemMessage(Component.literal("§6Жесты (выполняются подряд):"));
        for (Gesture gesture : Gesture.values()) {
            ctx.getSource().sendSystemMessage(Component.literal(
                    "§f" + gesture.spell().displayName() + " §7— " + gesture.description()
                            + " §8(" + Math.round(gesture.spell().manaCost()) + " маны)"
            ));
        }
        return 1;
    }

    private static int circleStart(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = player(ctx);
        if (RitualManager.get(p.getUUID()) != null) {
            p.displayClientMessage(Component.literal("§cКруг уже создан. /magic circle stop"), false);
            return 0;
        }
        RitualManager.start(p);
        p.displayClientMessage(Component.literal(
                "§aКруг создан! Игроки в радиусе " + Math.round(RitualCircle.RADIUS)
                        + " блоков могут сделать /magic circle join"
        ), false);
        return 1;
    }

    private static int circleJoin(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = player(ctx);
        RitualCircle circle = RitualManager.nearest(p);
        if (circle == null) {
            p.displayClientMessage(Component.literal("§cРядом нет активного круга"), false);
            return 0;
        }
        if (circle.leader().equals(p.getUUID())) {
            p.displayClientMessage(Component.literal("§cВы лидер этого круга"), false);
            return 0;
        }
        if (circle.isContributor(p.getUUID())) {
            p.displayClientMessage(Component.literal("§cВы уже в круге"), false);
            return 0;
        }
        float mana = ManaManager.get(p);
        if (mana <= 0.0F) {
            p.displayClientMessage(Component.literal("§cУ вас нет маны"), false);
            return 0;
        }
        circle.addContribution(p.getUUID(), mana);
        ManaManager.set(p, 0.0F);
        RitualManager.notifyEveryone(p, Component.literal(
                "§a" + p.getName().getString() + " §fотдал " + Math.round(mana)
                        + " маны в круг. Всего: §6" + Math.round(circle.pool())
        ));
        return 1;
    }

    private static int circleLeave(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = player(ctx);
        // мана уже отдана — возврата нет
        boolean left = RitualManager.leave(p);
        p.displayClientMessage(Component.literal(left ? "§eВы вышли из круга" : "§cВы не участвуете в круге"), false);
        return left ? 1 : 0;
    }

    private static int circleStatus(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = player(ctx);
        RitualCircle circle = RitualManager.get(p.getUUID());
        if (circle == null) circle = RitualManager.nearest(p);
        if (circle == null) {
            p.displayClientMessage(Component.literal("§cАктивных кругов рядом нет"), false);
            return 0;
        }
        ServerPlayer leader = p.level().getServer().getPlayerList().getPlayer(circle.leader());
        p.displayClientMessage(Component.literal(
                "§6Круг §f— лидер: " + (leader == null ? "?" : leader.getName().getString())
                        + ", маны: " + Math.round(circle.pool())
                        + ", участников: " + circle.contributorCount()
        ), false);
        return 1;
    }

    private static int circleStop(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = player(ctx);
        RitualCircle circle = RitualManager.get(p.getUUID());
        if (circle == null) {
            p.displayClientMessage(Component.literal("§cУ вас нет активного круга"), false);
            return 0;
        }
        RitualManager.stop(p);
        p.displayClientMessage(Component.literal("§eКруг расформирован, мана возвращена участникам"), false);
        return 1;
    }
}
