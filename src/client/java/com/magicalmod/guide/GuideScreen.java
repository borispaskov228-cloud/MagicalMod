package com.magicalmod.guide;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.magicalmod.boss.BossContent;
import com.magicalmod.content.ModContent;
import com.magicalmod.magic.Spell;
import com.magicalmod.staff.StaffContent;
import com.mojang.blaze3d.platform.InputConstants;

import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Гид в стиле FTB Quests: тёмная панель, вкладки слева и страницы с контентом справа.
 *
 * <p>Фон рисуется в {@link #renderBackground}, а текст и иконки — в {@link #render}
 * ПОСЛЕ виджетов. {@code super.renderBackground()} намеренно не вызывается:
 * это исключает повторный blur ("Can only blur once per frame").
 */
public class GuideScreen extends Screen {
    private static final int TITLE_H = 32;
    private static final int MAX_W = 1040;
    private static final int MAX_H = 720;
    private static final int MIN_CONTENT_W = 160;

    private static final int TEXT_COLOR = 0xFFD9D0E8;
    private static final int BULLET_COLOR = 0xFFCFC4E0;
    private static final int HINT_COLOR = 0xFF9E90C0;
    private static final int HEADER_COLOR = 0xFFFFD98A;
    private static final int CYAN = 0xFF6FC2FF;
    private static final int VIOLET = 0xFFB478FF;

    private static final String[] TABS = {
            "✦ Начало",
            "◈ Формы магии",
            "✧ Заклинания",
            "✚ Крафты",
            "◎ Круг и мана",
            "☠ Босс"
    };

    private static final int[] TAB_COLORS = {
            0xFF66E0A0, 0xFF6FC2FF, 0xFFC08CFF, 0xFFFFC857, 0xFF5AD9E0, 0xFFFF6B8A
    };

    /** Порядок совпадает с порядком {@link Spell#values()} и {@code Gesture.values()}. */
    private static final String[] FORM_NAMES = {
            "Круг (по часовой)", "Треугольник", "V-образная линия", "Зигзаг Z", "Спираль наружу",
            "Волна", "Линия вправо", "Квадрат", "Линия вниз", "Стягивающаяся спираль"
    };

    private static final String[] EFFECTS = {
            "урон + поджог", "урон + заморозка", "лечение + регенерация", "мощный урон + молния",
            "яд + иссушение", "урон + отталкивание", "урон + скорость", "поглощение + сопротивление",
            "урон + поджог + взрыв", "урон + притяжение"
    };

    private final List<List<Entry>> chapters;
    private final List<Integer> pageStarts = new ArrayList<>();

    private int activeTab;
    private int page;
    private int px;
    private int py;
    private int panelW;
    private int panelH;
    private int tabX;
    private int tabY;
    private int tabW;
    private int tabH;
    private int tabGap;
    private int contentX;
    private int contentY;
    private int contentW;
    private int contentH;

    public GuideScreen() {
        super(Component.literal("MagicalMod — Гид магии"));
        this.chapters = buildChapters();
    }

    // ---------------------------------------------------------------- geometry

    @Override
    protected void init() {
        layout();

        for (int i = 0; i < TABS.length; i++) {
            final int index = i;
            addRenderableWidget(new StylizedButton(
                    tabX, tabY + i * (tabH + tabGap), tabW, tabH,
                    Component.literal(TABS[i]),
                    b -> selectTab(index),
                    TAB_COLORS[i], this.font, () -> activeTab == index));
        }

        int closeY = Math.min(tabY + TABS.length * (tabH + tabGap) + 4, py + panelH - 34);
        addRenderableWidget(new StylizedButton(
                tabX, closeY, tabW, 26,
                Component.literal("× Закрыть"), b -> onClose(),
                0xFF8A3A5A, this.font, null));

        int footerY = py + panelH - 32;
        addRenderableWidget(new StylizedButton(
                px + panelW - 106, footerY, 28, 22,
                Component.literal("◀"), b -> changePage(-1), CYAN, this.font, null));
        addRenderableWidget(new StylizedButton(
                px + panelW - 36, footerY, 28, 22,
                Component.literal("▶"), b -> changePage(1), CYAN, this.font, null));

        paginate();
    }

    private void layout() {
        int margin = 14;
        panelW = Math.max(420, Math.min(this.width - margin * 2, MAX_W));
        panelH = Math.max(300, Math.min(this.height - margin * 2, MAX_H));
        px = (this.width - panelW) / 2;
        py = (this.height - panelH) / 2;

        tabH = 30;
        tabGap = 6;
        tabW = Math.max(90, Math.min(168, panelW / 5));
        tabX = px + 14;
        tabY = py + TITLE_H + 14;

        contentX = tabX + tabW + 16;
        contentY = tabY;
        contentW = Math.max(MIN_CONTENT_W, px + panelW - 16 - contentX);
        contentH = Math.max(120, py + panelH - 44 - contentY);
    }

    // ------------------------------------------------------------------ render

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        // super.render(): сначала renderBackground (наша панель), затем все кнопки.
        super.render(graphics, mouseX, mouseY, delta);
        drawContent(graphics, mouseX, mouseY);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
        if (panelW <= 0) return;

        // Светящаяся рамка: градиент рисуется построчно через fill, без blur и без super.
        for (int i = 0; i < 3; i++) {
            int c = mix(0xFF9B5CFF, 0xFF3FC7C7, i / 2.0F);
            graphics.fill(px - 3 + i, py - 3 + i, px + panelW + 3 - i, py - 2 + i, c);
            graphics.fill(px - 3 + i, py + panelH + 2 - i, px + panelW + 3 - i, py + panelH + 3 - i, c);
            graphics.fill(px - 3 + i, py - 3 + i, px - 2 + i, py + panelH + 3 - i, c);
            graphics.fill(px + panelW + 2 - i, py - 3 + i, px + panelW + 3 - i, py + panelH + 3 - i, c);
        }
        graphics.fill(px - 1, py - 1, px + panelW + 1, py + panelH + 1, 0xFF080611);
        graphics.fill(px, py, px + panelW, py + panelH, 0xFF12101F);

        // Шапка с градиентом.
        for (int y = 0; y < TITLE_H; y++) {
            graphics.fill(px, py + y, px + panelW, py + y + 1,
                    mix(0xFF3A1F66, 0xFF162A48, y / (float) TITLE_H));
        }
        graphics.fill(px, py + TITLE_H, px + panelW, py + TITLE_H + 2, VIOLET);
        graphics.drawCenteredString(this.font, this.title, px + panelW / 2, py + 9, 0xFFFFFFFF);

        // Небольшое «сияние» в шапке, как в RitualScreen.
        long time = System.currentTimeMillis();
        int cx = px + 22;
        int cy = py + 16;
        for (int i = 0; i < 8; i++) {
            double a = time / 500.0D + i * (Math.PI * 2.0D / 8.0D);
            int dx = (int) (Math.cos(a) * 7.0D);
            int dy = (int) (Math.sin(a) * 7.0D);
            graphics.fill(cx + dx - 1, cy + dy - 1, cx + dx + 2, cy + dy + 2, 0xFFD0A0FF);
        }
        graphics.fill(cx - 3, cy - 3, cx + 3, cy + 3, 0xFF7B3FBF);

        // Левая колонка вкладок.
        graphics.fill(tabX - 5, tabY - 7, tabX + tabW + 5, py + panelH - 14, 0xFF090812);
        graphics.fill(tabX - 5, tabY - 7, tabX + tabW + 5, tabY - 6, 0xFF4A3468);
        graphics.fill(tabX - 5, tabY - 7, tabX - 4, py + panelH - 14, 0xFF2A2040);

        // Правая область контента.
        graphics.fill(contentX - 6, contentY - 7, contentX + contentW + 6, contentY + contentH + 7, 0xFF090812);
        graphics.fill(contentX - 6, contentY - 7, contentX + contentW + 6, contentY - 6, VIOLET);
        graphics.fill(contentX - 6, contentY - 7, contentX - 5, contentY + contentH + 7, 0xFF2A2040);
        graphics.fill(contentX - 6, contentY + contentH + 6, contentX + contentW + 6, contentY + contentH + 7, 0xFF4A3468);

        // Подвал.
        graphics.fill(px + 10, py + panelH - 42, px + panelW - 10, py + panelH - 41, 0xFF2A2040);
    }

    private void drawContent(GuiGraphics graphics, int mouseX, int mouseY) {
        if (panelW <= 0 || pageStarts.isEmpty()) return;

        int footerY = py + panelH - 32;
        graphics.drawString(this.font, "Прокрутка / ◀ ▶ — страницы   ·   ESC — закрыть",
                px + 16, footerY + 7, HINT_COLOR);

        String counter = "Стр. " + (page + 1) + " / " + pageStarts.size();
        graphics.drawString(this.font, counter,
                px + panelW - 112 - this.font.width(counter), footerY + 7, 0xFFD9A6FF);

        graphics.enableScissor(contentX, contentY, contentX + contentW, contentY + contentH);
        int y = contentY + 3;
        for (Entry entry : currentPage()) {
            drawEntry(graphics, entry, y, mouseX, mouseY);
            y += measure(entry) + 3;
        }
        graphics.disableScissor();
    }

    private void drawEntry(GuiGraphics g, Entry e, int y, int mouseX, int mouseY) {
        int x = contentX;
        switch (e.kind) {
            case TITLE -> {
                g.drawString(this.font, e.text, x, y + 3, 0xFFFFFFFF);
                g.fill(x, y + 16, x + contentW, y + 18, VIOLET);
                g.fill(x + 20, y + 18, x + contentW - 20, y + 19, 0xFF5A3A88);
            }
            case HEADER -> {
                g.fill(x, y + 3, x + 3, y + 15, e.color);
                g.drawString(this.font, e.text, x + 8, y + 3, e.color);
            }
            case TEXT, BULLET, HINT -> {
                int lineY = y + 1;
                for (FormattedCharSequence line : font.split(Component.literal(e.text), contentW - 4)) {
                    g.drawString(this.font, line, x, lineY, e.color);
                    lineY += this.font.lineHeight + 1;
                }
            }
            case KEY -> {
                int keyW = this.font.width(e.text) + 14;
                g.fill(x, y + 1, x + keyW, y + 18, 0xFF090611);
                g.fill(x + 1, y + 2, x + keyW - 1, y + 17, 0xFF312050);
                g.fill(x + 1, y + 2, x + keyW - 1, y + 3, CYAN);
                g.drawString(this.font, e.text, x + 7, y + 5, 0xFFEAF6FF);
                g.drawString(this.font, e.detail, x + keyW + 9, y + 5, BULLET_COLOR);
            }
            case ROW -> {
                int textX = x;
                if (e.icon != null) {
                    slot(g, x, y);
                    g.renderItem(e.icon, x + 1, y + 1);
                    if (mouseX >= x && mouseX < x + 18 && mouseY >= y && mouseY < y + 18) {
                        g.setTooltipForNextFrame(this.font, e.icon, mouseX, mouseY);
                    }
                    textX = x + 23;
                } else {
                    g.fill(x + 2, y + 5, x + 10, y + 13, e.color);
                    g.fill(x + 3, y + 6, x + 9, y + 12, brighten(e.color, 55));
                    textX = x + 16;
                }
                g.drawString(this.font, e.text, textX, y + 1, e.color);
                g.drawString(this.font, e.detail, textX, y + 12, BULLET_COLOR);
            }
            case CRAFT -> {
                int iconX = x;
                for (ItemStack in : e.inputs) {
                    slot(g, iconX, y + 2);
                    g.renderItem(in, iconX + 1, y + 3);
                    if (mouseX >= iconX && mouseX < iconX + 18 && mouseY >= y + 2 && mouseY < y + 20) {
                        g.setTooltipForNextFrame(this.font, in, mouseX, mouseY);
                    }
                    iconX += 21;
                }
                g.drawString(this.font, "§b➜", iconX + 1, y + 7, 0xFFFFFFFF);
                int resultX = iconX + 22;
                slot(g, resultX, y + 2);
                g.renderItem(e.icon, resultX + 1, y + 3);
                if (mouseX >= resultX && mouseX < resultX + 18 && mouseY >= y + 2 && mouseY < y + 20) {
                    g.setTooltipForNextFrame(this.font, e.icon, mouseX, mouseY);
                }

                int textX = resultX + 24;
                int textW = Math.max(60, contentX + contentW - textX);
                g.drawString(this.font, e.text, textX, y + 2, e.color);
                int lineY = y + 14;
                for (FormattedCharSequence line : font.split(Component.literal(e.detail), textW)) {
                    g.drawString(this.font, line, textX, lineY, BULLET_COLOR);
                    lineY += this.font.lineHeight + 1;
                    if (lineY > y + 36) break;
                }
            }
            case RULE -> {
                g.fill(x, y + 4, x + contentW, y + 5, 0xFF3A2C58);
                g.fill(x + 10, y + 4, x + contentW - 10, y + 5, 0xFF6A4E8A);
            }
            case SPACER -> { /* пусто */ }
        }
    }

    private static void slot(GuiGraphics g, int x, int y) {
        g.fill(x, y, x + 18, y + 18, 0xFF05040B);
        g.fill(x + 1, y + 1, x + 17, y + 17, 0xFF251B3D);
        g.fill(x + 1, y + 1, x + 17, y + 2, 0xFF5A4280);
    }

    // --------------------------------------------------------------- pagination

    private void selectTab(int index) {
        if (index == activeTab) return;
        activeTab = index;
        page = 0;
        paginate();
    }

    private void changePage(int delta) {
        if (pageStarts.isEmpty()) return;
        page = Math.max(0, Math.min(pageStarts.size() - 1, page + delta));
    }

    private void paginate() {
        pageStarts.clear();
        List<Entry> source = chapters.get(activeTab);
        int used = 0;
        int start = 0;
        for (int i = 0; i < source.size(); i++) {
            int h = measure(source.get(i)) + 3;
            if (used > 0 && used + h > contentH) {
                pageStarts.add(start);
                start = i;
                used = 0;
            }
            used += h;
        }
        pageStarts.add(start);
        if (page >= pageStarts.size()) page = pageStarts.size() - 1;
    }

    private List<Entry> currentPage() {
        List<Entry> source = chapters.get(activeTab);
        int from = pageStarts.get(page);
        int to = page + 1 < pageStarts.size() ? pageStarts.get(page + 1) : source.size();
        return source.subList(from, to);
    }

    private int measure(Entry e) {
        int wrapW = Math.max(60, contentW - 4);
        return switch (e.kind) {
            case TITLE -> 22;
            case HEADER -> 19;
            case TEXT, BULLET, HINT -> font.split(Component.literal(e.text), wrapW).size()
                    * (this.font.lineHeight + 1) + 3;
            case KEY -> 20;
            case ROW -> 24;
            case CRAFT -> 40;
            case RULE -> 10;
            case SPACER -> e.spacer;
        };
    }

    // ------------------------------------------------------------------- input

    @Override
    public boolean keyPressed(KeyEvent event) {
        int key = event.key();
        if (key == InputConstants.KEY_ESCAPE) {
            onClose();
            return true;
        }
        if (key == InputConstants.KEY_LEFT || key == InputConstants.KEY_DOWN) {
            changePage(-1);
            return true;
        }
        if (key == InputConstants.KEY_RIGHT || key == InputConstants.KEY_UP) {
            changePage(1);
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (mouseX >= contentX && mouseX <= contentX + contentW
                && mouseY >= contentY && mouseY <= contentY + contentH) {
            if (scrollY > 0) changePage(-1);
            else if (scrollY < 0) changePage(1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    @Override
    public boolean isPauseScreen() { return false; }

    // ------------------------------------------------------------------- data

    private static List<List<Entry>> buildChapters() {
        List<List<Entry>> chapters = new ArrayList<>();
        chapters.add(startChapter());
        chapters.add(formsChapter());
        chapters.add(spellsChapter());
        chapters.add(craftsChapter());
        chapters.add(manaChapter());
        chapters.add(bossChapter());
        return chapters;
    }

    private static List<Entry> startChapter() {
        List<Entry> e = new ArrayList<>();
        e.add(Entry.title("ДОБРО ПОЖАЛОВАТЬ В MAGICALMOD"));
        e.add(Entry.text("Магия рисуется жестами мыши. Удерживайте клавишу рисования, ведите мышь и отпустите — мод распознает форму и кастует заклинание."));
        e.add(Entry.header("УПРАВЛЕНИЕ"));
        e.add(Entry.key("\\", "Открыть меню круга (RitualScreen)"));
        e.add(Entry.key("]", "Удерживать для рисования формы"));
        e.add(Entry.key("G", "Конфликтует с ванильным Quick Actions — не используется"));
        e.add(Entry.key("ESC", "Закрыть этот гид"));
        e.add(Entry.header("КАК РИСОВАТЬ"));
        e.add(Entry.bullet("Замкнутые формы (круг, квадрат) начинайте и заканчивайте рядом."));
        e.add(Entry.bullet("Рисуйте ровно и не слишком быстро: распознавание идёт по направлению и углам."));
        e.add(Entry.bullet("Каст стоит маны; если её не хватит, об этом сообщат прямо в игре."));
        e.add(Entry.header("ПОЛЕЗНЫЕ КОМАНДЫ"));
        e.add(Entry.bullet("/magic book — получить книгу-инструкцию."));
        e.add(Entry.bullet("/magic gestures — список всех десяти форм."));
        e.add(Entry.bullet("/magic mana — текущий запас маны."));
        e.add(Entry.hint("Совет: откройте вкладку «Формы магии» и посмотрите соответствие фигур и заклинаний."));
        return e;
    }

    private static List<Entry> formsChapter() {
        List<Entry> e = new ArrayList<>();
        e.add(Entry.title("ФОРМЫ И ЗАКЛИНАНИЯ"));
        e.add(Entry.text("Удерживайте ] и нарисуйте одну из десяти форм. Цвет строки соответствует цвету заклинания."));
        e.add(Entry.header("ОСНОВНЫЕ ФОРМЫ"));
        Spell[] all = Spell.values();
        for (int i = 0; i < 5; i++) {
            e.add(Entry.row(FORM_NAMES[i], all[i].displayName() + " — " + EFFECTS[i],
                    all[i].color() | 0xFF000000));
        }
        e.add(Entry.header("ДОПОЛНИТЕЛЬНЫЕ ФОРМЫ"));
        for (int i = 5; i < 10; i++) {
            e.add(Entry.row(FORM_NAMES[i], all[i].displayName() + " — " + EFFECTS[i],
                    all[i].color() | 0xFF000000));
        }
        e.add(Entry.hint("Заклинания]* летят зарядом: эффект срабатывает при попадании. Формы HEAL и SHIELD действуют на вас."));
        return e;
    }

    private static List<Entry> spellsChapter() {
        List<Entry> e = new ArrayList<>();
        e.add(Entry.title("КАТАЛОГ ЗАКЛИНАНИЙ"));
        e.add(Entry.text("Всего 10 заклинаний. Стоимость указана в мане, сила — базовое значение до бонусов посоха и круга."));
        Spell[] all = Spell.values();
        for (int i = 0; i < all.length; i++) {
            Spell s = all[i];
            e.add(Entry.row(
                    s.displayName() + "  [" + Math.round(s.manaCost()) + " маны]",
                    EFFECTS[i] + " · сила " + s.basePower() + " · " + (s.isSelf() ? "на себя" : "на цель"),
                    s.color() | 0xFF000000));
        }
        e.add(Entry.hint("Посохи усиливают силу: ×1.25 / ×1.60 / ×2.20. Бонусы не складываются — берётся лучший посох в двух руках."));
        return e;
    }

    private static List<Entry> craftsChapter() {
        List<Entry> e = new ArrayList<>();
        e.add(Entry.title("РЕЦЕПТЫ МАГИИ"));
        e.add(Entry.text("Все рецепты крафтятся в обычном верстаке. Слева — компоненты, справа — результат. Наведите на иконку, чтобы увидеть название."));

        e.add(Entry.header("ПОСОХИ"));
        e.add(Entry.craft("Посох новичка", "+25% силы заклинаний",
                new ItemStack[]{new ItemStack(Items.STICK, 2), new ItemStack(Items.AMETHYST_SHARD)},
                new ItemStack(StaffContent.STAFF_NOVICE), 0xFFFFC857));
        e.add(Entry.craft("Посох адепта", "+60% силы · изумруд усиливает чары",
                new ItemStack[]{new ItemStack(StaffContent.STAFF_NOVICE), new ItemStack(Items.DIAMOND),
                        new ItemStack(Items.EMERALD)},
                new ItemStack(StaffContent.STAFF_ADEPT), 0xFF6FC2FF));
        e.add(Entry.craft("Посох архимага", "+120% силы · эхо удваивает магию",
                new ItemStack[]{new ItemStack(StaffContent.STAFF_ADEPT), new ItemStack(Items.NETHERITE_INGOT),
                        new ItemStack(Items.ECHO_SHARD)},
                new ItemStack(StaffContent.STAFF_ARCHMAGE), 0xFFC08CFF));

        e.add(Entry.header("ЗЕЛЬЯ МАНЫ"));
        e.add(Entry.craft("Малое зелье маны", "+30 маны · правый клик",
                new ItemStack[]{new ItemStack(ModContent.MANA_HERB_ITEM), new ItemStack(Items.GLASS_BOTTLE)},
                new ItemStack(ModContent.MANA_POTION_SMALL), 0xFF6FC2FF));
        e.add(Entry.craft("Среднее зелье маны", "+60 маны · правый клик",
                new ItemStack[]{new ItemStack(ModContent.MANA_HERB_ITEM, 2), new ItemStack(Items.GLASS_BOTTLE)},
                new ItemStack(ModContent.MANA_POTION_MEDIUM), 0xFFB478FF));
        e.add(Entry.craft("Большое зелье маны", "+100 маны · правый клик",
                new ItemStack[]{new ItemStack(ModContent.MANA_HERB_ITEM, 3), new ItemStack(Items.AMETHYST_SHARD),
                        new ItemStack(Items.GLASS_BOTTLE)},
                new ItemStack(ModContent.MANA_POTION_LARGE), 0xFF66E0A0));

        e.add(Entry.hint("Трава маны растёт в дикой природе; команда /magic herb выдаёт 8 штук."));
        return e;
    }

    private static List<Entry> manaChapter() {
        List<Entry> e = new ArrayList<>();
        e.add(Entry.title("КРУГ МАГИИ И МАНА"));
        e.add(Entry.header("МЕНЮ КРУГА"));
        e.add(Entry.key("\\", "Открыть меню круга"));
        e.add(Entry.bullet("«Создать круг» — вы лидер. Участники в радиусе 16 блоков нажимают «Вступить»."));
        e.add(Entry.bullet("Участники отдают ману в общий пул; следующее заклинание лидера усиливается."));
        e.add(Entry.bullet("«Выйти», «Обновить», «Расформировать» — управление. Круг привязан к измерению."));
        e.add(Entry.header("ПРОКАЧКА МАНЫ"));
        e.add(Entry.bullet("База — 100 маны. Каждый уровень даёт +20, максимум 10 уровней (300 маны)."));
        e.add(Entry.bullet("За каст — 2 очка опыта, за групповой каст — 4. 10 очков = новый уровень."));
        e.add(Entry.bullet("Естественная регенерация: +3 маны каждую секунду."));
        e.add(Entry.header("ЗЕЛЬЯ И ПОЛОСА HUD"));
        e.add(Entry.bullet("Зелья маны восстанавливают 30 / 60 / 100 маны."));
        e.add(Entry.bullet("Фиолетовая полоса над голодом показывает запас, максимум и уровень; под ней — опыт."));
        e.add(Entry.key("/magic refill", "Мгновенно восстановить ману"));
        e.add(Entry.key("/magic level <0-10>", "Установить уровень магии"));
        return e;
    }

    private static List<Entry> bossChapter() {
        List<Entry> e = new ArrayList<>();
        e.add(Entry.title("АРХИМАГ ПУСТОТЫ"));
        e.add(Entry.text("Парящий босс с тремя фазами и 260 HP. Магия — лучший способ пробить его оболочку."));
        e.add(Entry.header("ФАЗА 1 · 70–100% HP"));
        e.add(Entry.bullet("Стихийные снаряды (огонь, лёд, молния), барьеры и зоны телепортации."));
        e.add(Entry.header("ФАЗА 2 · 30–70% HP"));
        e.add(Entry.bullet("Призывает 3–4 вксинов. Пока они живы, босс держит щит."));
        e.add(Entry.header("ФАЗА 3 · менее 30% HP"));
        e.add(Entry.bullet("Быстрый каст, притяжение игрока и порталы со взрывными рунами."));
        e.add(Entry.header("УЯЗВИМОСТЬ И НАГРАДА"));
        e.add(Entry.bullet("Магический урон пробивает оболочку: ×1.75 и сбивает щит, убивая приспешников."));
        e.add(Entry.bullet("Награда: ядро архимага, 8 аметистов, 4 жемчуга Эндера, 2 незеритовых лома."));
        e.add(Entry.key("/magic boss", "Выдать призывателя Архимага Пустоты"));
        e.add(Entry.iconRow(new ItemStack(BossContent.BOSS_SUMMONER),
                "Призыватель босса", "Используйте, чтобы вызвать Архимага", 0xFFFF6B8A));
        return e;
    }

    // ------------------------------------------------------------------ utils

    private static int mix(int a, int b, float t) {
        float k = Math.max(0.0F, Math.min(1.0F, t));
        int ar = (a >> 16) & 0xFF, ag = (a >> 8) & 0xFF, ab = a & 0xFF;
        int br = (b >> 16) & 0xFF, bg = (b >> 8) & 0xFF, bb = b & 0xFF;
        int r = (int) (ar + (br - ar) * k);
        int g = (int) (ag + (bg - ag) * k);
        int bl = (int) (ab + (bb - ab) * k);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    private static int brighten(int c, int amount) {
        int r = Math.min(255, ((c >> 16) & 0xFF) + amount);
        int g = Math.min(255, ((c >> 8) & 0xFF) + amount);
        int b = Math.min(255, (c & 0xFF) + amount);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    // ------------------------------------------------------------------ entry

    private enum Kind { TITLE, HEADER, TEXT, BULLET, HINT, KEY, ROW, CRAFT, RULE, SPACER }

    private static final class Entry {
        final Kind kind;
        final String text;
        final String detail;
        final int color;
        final ItemStack icon;
        final ItemStack[] inputs;
        final int spacer;

        private Entry(Kind kind, String text, String detail, int color,
                      ItemStack icon, ItemStack[] inputs, int spacer) {
            this.kind = kind;
            this.text = text == null ? "" : text;
            this.detail = detail == null ? "" : detail;
            this.color = color;
            this.icon = icon;
            this.inputs = inputs;
            this.spacer = spacer;
        }

        static Entry title(String s) {
            return new Entry(Kind.TITLE, s, null, HEADER_COLOR, null, null, 0);
        }

        static Entry header(String s) {
            return new Entry(Kind.HEADER, s, null, HEADER_COLOR, null, null, 0);
        }

        static Entry text(String s) {
            return new Entry(Kind.TEXT, s, null, TEXT_COLOR, null, null, 0);
        }

        static Entry bullet(String s) {
            return new Entry(Kind.BULLET, "• " + s, null, BULLET_COLOR, null, null, 0);
        }

        static Entry hint(String s) {
            return new Entry(Kind.HINT, "✧ " + s, null, HINT_COLOR, null, null, 0);
        }

        static Entry key(String keyName, String description) {
            return new Entry(Kind.KEY, keyName, description, CYAN, null, null, 0);
        }

        static Entry row(String label, String description, int color) {
            return new Entry(Kind.ROW, label, description, color, null, null, 0);
        }

        static Entry iconRow(ItemStack icon, String label, String description, int color) {
            return new Entry(Kind.ROW, label, description, color, icon, null, 0);
        }

        static Entry craft(String name, String description, ItemStack[] inputs,
                           ItemStack result, int color) {
            return new Entry(Kind.CRAFT, name, description, color, result, inputs, 0);
        }

        static Entry rule() {
            return new Entry(Kind.RULE, null, null, 0, null, null, 0);
        }

        static Entry spacer(int h) {
            return new Entry(Kind.SPACER, null, null, 0, null, null, h);
        }
    }

    /** Кнопка с собственным фоном, подсветкой и акцентной полосой. */
    private static final class StylizedButton extends Button {
        private final int color;
        private final Font font;
        private final Supplier<Boolean> selected;

        StylizedButton(int x, int y, int w, int h, Component message, OnPress press,
                       int color, Font font, Supplier<Boolean> selected) {
            super(x, y, w, h, message, press, b -> Component.empty());
            this.color = color;
            this.font = font;
            this.selected = selected;
        }

        @Override
        protected void renderContents(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            int x = getX(), y = getY();
            boolean on = selected != null && Boolean.TRUE.equals(selected.get());
            boolean hover = isHovered() || isFocused();
            int base = on ? color : mix(color, 0xFF12101F, 0.58F);
            if (hover) base = brighten(base, 38);

            graphics.fill(x, y, x + this.width, y + this.height, 0xFF06050C);
            graphics.fill(x + 1, y + 1, x + this.width - 1, y + this.height - 1, base);
            graphics.fill(x + 1, y + 1, x + this.width - 1, y + 2, brighten(base, 55));
            graphics.fill(x + 1, y + 1, x + 3, y + this.height - 1, color);
            if (on) {
                graphics.fill(x + 2, y + this.height - 3, x + this.width - 1, y + this.height - 1, color);
            }
            graphics.drawString(this.font, getMessage(), x + 9,
                    y + (this.height - this.font.lineHeight) / 2,
                    on || hover ? 0xFFFFFFFF : 0xFFE4E0F0);
        }
    }
}
