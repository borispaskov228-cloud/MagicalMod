package com.magicalmod.artifact;

import com.magicalmod.MagicalMod;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;

/**
 * Реестр из 20 артефактов.
 *
 * <p>Все предметы — ванильные {@link Item}, различаются только ванильными
 * компонентами данных ({@code custom_name}, {@code lore}, {@code item_model}).
 * Идентификация идёт ПО ИМЕНИ (префикс {@code [Артефакт]}), без кастомных
 * компонентов и без NBT — так не бывает ошибок дженериков.</p>
 */
public final class Artifacts {
    public static final int COUNT = 20;

    /** Префикс, по которому ArtifactUseHandler находит артефакты. */
    public static final String PREFIX = "§6[Артефакт] ";

    private static final Item[] BASE_ITEMS = {
            Items.BLAZE_ROD,            // 0
            Items.ICE,                  // 1
            Items.GOLDEN_APPLE,         // 2
            Items.AMETHYST_SHARD,       // 3
            Items.FEATHER,              // 4
            Items.SPYGLASS,             // 5
            Items.PRISMARINE_CRYSTALS,  // 6
            Items.NETHERITE_INGOT,      // 7
            Items.SLIME_BALL,           // 8
            Items.FIREWORK_ROCKET,      // 9
            Items.ENDER_PEARL,          // 10
            Items.LIGHTNING_ROD,        // 11
            Items.DIAMOND_SWORD,        // 12
            Items.RABBIT_FOOT,          // 13
            Items.WITHER_ROSE,          // 14
            Items.MILK_BUCKET,          // 15
            Items.EXPERIENCE_BOTTLE,    // 16
            Items.COOKED_BEEF,          // 17
            Items.ENDER_EYE,            // 18
            Items.COMPASS               // 19
    };

    private static final String[] NAMES = {
            "Огненный зар",
            "Ледяная печать",
            "Живой источник",
            "Магический резервуар",
            "Ветреный шаг",
            "Око ночи",
            "Дыхание бездны",
            "Щит поглощения",
            "Громовой откат",
            "Искрящий взрыв",
            "Прыжок сквозь пространство",
            "Грозовой призыв",
            "Сила титана",
            "Пружина холма",
            "Пепел увядания",
            "Чистый родник",
            "Кристалл опыта",
            "Вечный пир",
            "Тень странника",
            "Магнит вселенной"
    };

    private static final String[] LORE = {
            "Пламя обнимает всех, кто стоит рядом.",
            "Холод сковывает врагов на месте.",
            "Тело забывает о ранах.",
            "Истощённая мана возвращается волной.",
            "Ветер несёт тебя быстрее мысли.",
            "Ты видишь то, что скрыто тьмой.",
            "Вода становится воздухом.",
            "Чужой урон растворяется в щите.",
            "Один взмах — и враги отлетают.",
            "Осколки света рвутся во все стороны.",
            "Пространство уступает пять шагов.",
            "Небо отвечает громом и дождём.",
            "Рука становится тяжёлой от силы.",
            "Земля подталкивает тебя вверх.",
            "Враги медленно увядают изнутри.",
            "Всё наваждение смыто дочиста.",
            "Опыт оседает в памяти кристалла.",
            "Сытость, которая не проходит.",
            "Ты становишься невидимым.",
            "Всё вокруг тянется к тебе."
    };

    private Artifacts() {}

    /** Полное отображаемое имя артефакта, включая цвет и префикс. */
    public static String getName(int id) {
        if (id < 0 || id >= COUNT) return "";
        return PREFIX + NAMES[id];
    }

    /** Описание артефакта для лора (без цвета). */
    public static String getLore(int id) {
        if (id < 0 || id >= COUNT) return "";
        return LORE[id];
    }

    /** Создать стак артефакта с заданным id (0..19). */
    public static ItemStack createStack(int id) {
        if (id < 0 || id >= COUNT) return ItemStack.EMPTY;

        ItemStack stack = new ItemStack(BASE_ITEMS[id]);
        stack.set(DataComponents.CUSTOM_NAME, Component.literal(getName(id)));
        stack.set(DataComponents.LORE, new ItemLore(List.of(
                Component.literal("§7" + LORE[id]),
                Component.literal("§8Правый клик — активировать"),
                Component.literal("§8Артефакт " + (id + 1) + " из " + COUNT)
        )));
        // Привязываем собственную 16x16 модель к ванильному предмету.
        stack.set(DataComponents.ITEM_MODEL, MagicalMod.id("artifact_" + id));
        return stack;
    }

    /** Все 20 артефактов в порядке id. */
    public static List<ItemStack> createAll() {
        List<ItemStack> result = new ArrayList<>(COUNT);
        for (int id = 0; id < COUNT; id++) result.add(createStack(id));
        return result;
    }

    /** id артефакта по имени или -1, если это не артефакт. */
    public static int getId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return -1;
        Component name = stack.getCustomName();
        if (name == null) return -1;
        String value = name.getString();
        for (int id = 0; id < COUNT; id++) {
            if (getName(id).equals(value)) return id;
        }
        return -1;
    }

    /** Быстрая проверка «артефакт ли это» — только по имени. */
    public static boolean isArtifact(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return false;
        Component name = stack.getCustomName();
        return name != null && name.getString().contains("[Артефакт]");
    }
}
