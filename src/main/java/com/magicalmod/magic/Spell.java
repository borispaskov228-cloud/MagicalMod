package com.magicalmod.magic;

import java.util.Locale;

/** все заклинания мода. */
public enum Spell {
    FIREBALL("fireball", "Огненный шар", 15, 9.0F, 0xFF6600),
    FROST("frost", "Ледяной удар", 20, 6.0F, 0x66CCFF),
    HEAL("heal", "Исцеление", 25, 6.0F, 0x55FF88),
    LIGHTNING("lightning", "Молния", 35, 14.0F, 0xC488FF);

    private final String id;
    private final String displayName;
    private final float manaCost;
    private final float basePower;
    private final int color;

    Spell(String id, String displayName, float manaCost, float basePower, int color) {
        this.id = id;
        this.displayName = displayName;
        this.manaCost = manaCost;
        this.basePower = basePower;
        this.color = color;
    }

    public String id() { return id; }
    public String displayName() { return displayName; }
    public float manaCost() { return manaCost; }
    public float basePower() { return basePower; }
    public int color() { return color; }

    public static Spell byId(String id) {
        for (Spell spell : values()) {
            if (spell.id.equals(id.toLowerCase(Locale.ROOT))) return spell;
        }
        return null;
    }
}
