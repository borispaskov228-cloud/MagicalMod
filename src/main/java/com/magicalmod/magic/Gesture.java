package com.magicalmod.magic;

/** Жест — последовательность действий игрока. */
public enum Gesture {
    FIRE(Spell.FIREBALL, Step.CROUCH, Step.JUMP, Step.RIGHT, Step.LEFT),
    FROST(Spell.FROST, Step.JUMP, Step.LEFT, Step.RIGHT, Step.CROUCH),
    HEAL(Spell.HEAL, Step.CROUCH, Step.CROUCH, Step.JUMP),
    LIGHTNING(Spell.LIGHTNING, Step.RIGHT, Step.RIGHT, Step.LEFT, Step.JUMP);

    public enum Step {
        CROUCH, JUMP, LEFT, RIGHT;

        public String symbol() {
            return switch (this) {
                case CROUCH -> "присесть";
                case JUMP -> "прыжок";
                case LEFT -> "влево";
                case RIGHT -> "вправо";
            };
        }
    }

    private final Spell spell;
    private final Step[] steps;

    Gesture(Spell spell, Step... steps) {
        this.spell = spell;
        this.steps = steps;
    }

    public Spell spell() { return spell; }
    public Step[] steps() { return steps; }

    public String description() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < steps.length; i++) {
            if (i > 0) sb.append(" → ");
            sb.append(steps[i].symbol());
        }
        return sb.toString();
    }

    /** Совпадает ли конец введённой последовательности с этим жестом. */
    public boolean matches(java.util.List<Step> input) {
        // Точное совпадение: иначе короткая последовательность совпала бы как суффикс длинной
        if (input.size() != steps.length) return false;
        for (int i = 0; i < steps.length; i++) {
            if (input.get(i) != steps[i]) return false;
        }
        return true;
    }
}
