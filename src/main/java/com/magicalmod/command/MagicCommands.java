package com.magicalmod.command;

import com.magicalmod.magic.Gesture;
import com.magicalmod.magic.GuideBook;
import com.magicalmod.magic.ManaHolder;
import com.magicalmod.magic.ManaManager;
import com.magicalmod.magic.ManaPotions;
import com.magicalmod.boss.BossContent;
import com.magicalmod.content.ModContent;
import com.magicalmod.staff.StaffContent;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/** Справочные команды — групповой магией управляет меню (G). */
public final class MagicCommands {
    private MagicCommands() {}

    private static final SimpleCommandExceptionType NO_PLAYER =
            new SimpleCommandExceptionType(Component.literal("Команду можно использовать только в игре"));

    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(Commands.literal("magic")
                        .then(Commands.literal("mana").executes(MagicCommands::mana))
                        .then(Commands.literal("gestures").executes(MagicCommands::gestures))
                        .then(Commands.literal("book").executes(MagicCommands::book))
                        .then(Commands.literal("refill").executes(MagicCommands::refill))
                        .then(Commands.literal("potions").executes(MagicCommands::potions))
                        .then(Commands.literal("staffs").executes(MagicCommands::staffs))
                        .then(Commands.literal("herb").executes(MagicCommands::herb))
                        .then(Commands.literal("boss").executes(MagicCommands::boss))
                        .then(Commands.literal("level")
                                .then(Commands.argument("level", IntegerArgumentType.integer(0, ManaHolder.MAX_LEVEL))
                                        .executes(MagicCommands::setLevel)))
                        .executes(MagicCommands::help)
                )
        );
    }

    private static ServerPlayer player(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        if (!(ctx.getSource().getEntity() instanceof ServerPlayer p)) throw NO_PLAYER.create();
        return p;
    }

    private static int help(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().sendSystemMessage(Component.literal(
                "§6Магия §f— /magic mana | /magic gestures | /magic book | /magic refill | /magic potions | /magic level <0-10>. Меню: §6G"
        ));
        return 1;
    }

    private static int mana(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ManaManager.sendMana(player(ctx));
        return 1;
    }

    private static int refill(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = player(ctx);
        ManaManager.set(p, ManaManager.getMax(p));
        ManaManager.sendToClient(p);
        p.displayClientMessage(Component.literal("§bМана полностью восстановлена"), true);
        return 1;
    }

    private static int potions(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = player(ctx);
        for (int i = 0; i < ManaPotions.COUNT; i++) {
            ItemStack stack = ManaPotions.createStack(i);
            if (!p.getInventory().add(stack)) p.drop(stack, false);
        }
        ctx.getSource().sendSystemMessage(Component.literal("§bВыдано 3 зелья маны"));
        return 1;
    }

    private static int staffs(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = player(ctx);
        for (ItemStack stack : new ItemStack[]{
                new ItemStack(StaffContent.STAFF_NOVICE),
                new ItemStack(StaffContent.STAFF_ADEPT),
                new ItemStack(StaffContent.STAFF_ARCHMAGE)}) {
            if (!p.getInventory().add(stack)) p.drop(stack, false);
        }
        ctx.getSource().sendSystemMessage(Component.literal("§6Выданы 3 посоха"));
        return 1;
    }

    private static int herb(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = player(ctx);
        for (int i = 0; i < 8; i++) {
            ItemStack stack = new ItemStack(ModContent.MANA_HERB_ITEM);
            if (!p.getInventory().add(stack)) p.drop(stack, false);
        }
        ctx.getSource().sendSystemMessage(Component.literal("§aВыдано 8 травы маны"));
        return 1;
    }

    private static int boss(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = player(ctx);
        ItemStack stack = new ItemStack(BossContent.BOSS_SUMMONER);
        if (!p.getInventory().add(stack)) p.drop(stack, false);
        ctx.getSource().sendSystemMessage(Component.literal("§5Выдан призыватель Архимага Пустоты"));
        return 1;
    }

    private static int setLevel(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        ServerPlayer p = player(ctx);
        int level = IntegerArgumentType.getInteger(ctx, "level");
        ManaHolder holder = (ManaHolder) p;
        holder.magicalmod$setManaLevel(level);
        ManaManager.set(p, ManaManager.getMax(p));
        ManaManager.sendToClient(p);
        p.displayClientMessage(Component.literal(
                "§6Уровень магии: " + level + ", максимум: " + Math.round(ManaManager.getMax(p))
        ), false);
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
