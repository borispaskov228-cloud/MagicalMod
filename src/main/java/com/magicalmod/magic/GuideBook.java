package com.magicalmod.magic;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.core.component.DataComponents;

/** Книга-инструкция, выдаётся игроку при входе. */
public final class GuideBook {
    private GuideBook() {}

    private static final String MARKER = "MagicalMod";

    public static ItemStack create() {
        List<Filterable<Component>> pages = List.of(
                page("MagicalMod — книга магии\n\n1. Жесты выполняются подряд клавишами: присесть, прыжок, A (влево), D (вправо). Мышь не используется — прицел не сбивается.\n\n2. После ввода жеста нажмите ] — только тогда заклинание кастуется."),
                page("ЗАКЛИНАНИЯ:\n\nОгненный шар: присесть → прыжок → вправо → влево. Поджигает цель.\n\nЛедяной удар: прыжок → влево → вправо → присесть. Замораживает врага."),
                page("ЛЕЧЕНИЕ И МОЛНИЯ:\n\nИсцеление: присесть → присесть → прыжок. Лечит вас, даже если рядом нет врагов.\n\nМолния: D → D → A → прыжок.\n\nПодробнее: /magic gestures, /magic mana"),
                page("ГРУППОВАЯ МАГИЯ:\n\nНажмите G — меню круга. Создайте круг; участники в радиусе 16 блоков нажимают «Вступить» и отдают всю ману.\n\nСледующее заклинание лидера усиливается и выглядит иначе."),
                page("МАНА:\n\nПолоса маны находится над шкалой голода. Мана восстанавливается 1 в секунду.\n\nКлавиши можно переназначить в меню «Управление» (категория MagicalMod).")
        );

        ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
        book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(
                Filterable.passThrough(MARKER + " — инструкция"),
                MARKER,
                0,
                pages,
                true
        ));
        return book;
    }

    private static Filterable<Component> page(String text) {
        return Filterable.passThrough(Component.literal(text));
    }

    public static boolean hasBook(ServerPlayer player) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (!stack.is(Items.WRITTEN_BOOK)) continue;
            WrittenBookContent content = stack.get(DataComponents.WRITTEN_BOOK_CONTENT);
            if (content != null && content.author().equals(MARKER)) return true;
        }
        return false;
    }

    public static void give(ServerPlayer player) {
        if (hasBook(player)) return;
        ItemStack book = create();
        if (!player.getInventory().add(book)) {
            player.drop(book, false);
        }
        player.displayClientMessage(Component.literal(
                "§6Вы получили книгу «MagicalMod — инструкция»"
        ), false);
    }
}
