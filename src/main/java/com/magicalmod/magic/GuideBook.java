package com.magicalmod.magic;

import java.util.List;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;

/** Книга-инструкция с кликабельным оглавлением. */
public final class GuideBook {
    private GuideBook() {}

    private static final String MARKER = "MagicalMod";

    public static ItemStack create() {
        MutableComponent toc = Component.literal("MagicalMod — оглавление\n\nНажмите на раздел:")
                .append(Component.literal("\n"))
                .append(link("1. Жесты и каст", 2))
                .append(Component.literal("\n"))
                .append(link("2. Огонь и лёд", 3))
                .append(Component.literal("\n"))
                .append(link("3. Лечение, молния, прокачка", 4))
                .append(Component.literal("\n"))
                .append(link("4. Круг магии", 5))
                .append(Component.literal("\n"))
                .append(link("5. Мана и клавиши", 6));

        List<Filterable<Component>> pages = List.of(
                Filterable.passThrough(toc),
                page("ЖЕСТЫ И КАСТ\n\nВводите подряд: присесть, прыжок, ',' (влево), '.' (вправо).\n\nМышь и WASD не используются — прицел не сбивается.\n\nКлавиша ] кастует введённый жест. Ввод живёт 12 секунд."),
                page("ОГОНЬ И ЛЁД\n\nОгненный шар: присесть → прыжок → '.' → ',' .\nУрон и поджог на 5 сек (10 в группе).\n\nЛедяной удар: прыжок → ',' → '.' → присесть.\nУрон, сильное замедление и заморозка врага."),
                page("ЛЕЧЕНИЕ, МОЛНИЯ, ПРОКАЧКА\n\nИсцеление: присесть → присесть → прыжок. Лечит вас всегда.\n\nМолния: '.' → '.' → ',' → прыжок.\n\nЗа каст даётся опыт: 10 очков = новый уровень, +20 к максимуму маны (до 10 уровня)."),
                page("КРУГ МАГИИ\n\nКлавиша G открывает меню.\n\n«Создать круг» — вы лидер. Участники в радиусе 16 блоков нажимают «Вступить» и отдают всю ману.\n\nСледующее заклинание лидера усиливается и выглядит особенно. Круг работает только в вашем измерении."),
                page("МАНА И КЛАВИШИ\n\nПолоса маны над голодом показывает ману, максимум и уровень с опытом.\n\nМана восстанавливается 1/сек.\n\nВсе клавиши меняются: Управление → категория MagicalMod.\n\n/magic gestures — жесты, /magic book — книга.")
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

    private static Component link(String label, int targetPage) {
        return Component.literal(label).withStyle(
                Style.EMPTY.withClickEvent(new ClickEvent.ChangePage(targetPage))
        );
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
        ItemStack hotbar = player.getInventory().getItem(8);
        if (hotbar.isEmpty()) {
            player.getInventory().setItem(8, book);
        } else if (!player.getInventory().add(book)) {
            player.drop(book, false);
        }
        player.displayClientMessage(Component.literal(
                "§6Книга «MagicalMod — инструкция» в 9-м слоте хотбара"
        ), false);
    }
}
