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
        MutableComponent toc = Component.literal("MagicalMod — оглавление\n\n§n§6НАЖМИТЕ НА РАЗДЕЛ:")
                .append(Component.literal("\n"))
                .append(link("1. Как рисовать", 2))
                .append(Component.literal("\n"))
                .append(link("2. Формы и заклинания", 3))
                .append(Component.literal("\n"))
                .append(link("3. Ещё формы", 4))
                .append(Component.literal("\n"))
                .append(link("4. Круг магии", 5))
                .append(Component.literal("\n"))
                .append(link("5. Мана и клавиши", 6));

        List<Filterable<Component>> pages = List.of(
                Filterable.passThrough(toc),
                page("КАК РИСОВАТЬ\n\nУдерживайте клавишу ] и ведите мышью — на экране появится светящийся след.\n\nОтпустите клавишу: мод распознает форму и кастует заклинание. Форма жива, пока клавиша нажата.\n\nРисуйте ровно и не слишком быстро: замкнутые формы должны начинаться и заканчиваться рядом."),
                page("ФОРМЫ И ЗАКЛИНАНИЯ\n\nКруг (по часовой) — Огненный шар.\nТреугольник — Ледяной удар.\nV — Исцеление.\nЗигзаг Z — Молния.\nСпираль — Яд."),
                page("ЕЩЁ ФОРМЫ\n\nВолна — Водный поток.\nГоризонтальная линия — Ветер.\nКвадрат — Магический щит.\nВертикальная линия вниз — Метеорит.\nСтягивающаяся спираль — Притяжение.\n\nВсего 10 заклинаний."),
                page("КРУГ МАГИИ\n\nКлавиша G открывает меню.\n\n«Создать круг» — вы лидер. Участники в радиусе 16 блоков нажимают «Вступить» и отдают ману.\n\nСледующее заклинание лидера усиливается. Круг привязан к измерению."),
                page("МАНА И КЛАВИШИ\n\nПолоса над голодом показывает ману, максимум и уровень.\n\nМана восстанавливается 3/сек. За каст даётся опыт: 10 очков = уровень, +20 маны.\n\nКлавиши меняются: Управление → MagicalMod.\n/magic gestures — список форм.")
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
        return Component.literal("§n§6" + label).withStyle(
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
