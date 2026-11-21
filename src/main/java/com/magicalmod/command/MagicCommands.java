package com.magicalmod.command;

import com.magicalmod.artifact.Artifacts;
import com.magicalmod.magic.Gesture;
import com.magicalmod.magic.GuideBook;
import com.magicalmod.magic.ManaManager;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Остались только справочные команды — групповой магией теперь управляет меню (G). */
public final class MagicCommands {
    private MagicCommands() {}

    private static final SimpleCommandExceptionType NO_PLAYER =
            new SimpleCommandExceptionType(Component.literal("Команду можно использовать только в игре"));

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            dispatcher.register(Commands.literal("magic")
                    .then(Commands.literal("mana").executes(MagicCommands::mana))
                    .then(Commands.literal("gestures").executes(MagicCommands::gestures))
                    .then(Commands.literal("book").executes(MagicCommands::book))
                    .then(Commands.literal("artifacts").executes(MagicCommands::artifacts))
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
                "§6Магия §f— /magic mana | /magic gestures | /magic book | /magic artifacts. Меню круга: §6G"
        ));
        return 1;
    }

    private static int mana(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ManaManager.sendMana(player(ctx));
        return 1;
    }

    private static int artifacts(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = player(ctx);
        for (ItemStack stack : Artifacts.createAll()) {
            if (!p.getInventory().add(stack)) p.drop(stack, false);
        }
        ctx.getSource().sendSystemMessage(Component.literal("§6Выдано 20 артефактов"));
        return 1;
    }

    private static int book(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        GuideBook.give(player(ctx));
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
}
