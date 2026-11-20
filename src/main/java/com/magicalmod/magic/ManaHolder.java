package com.magicalmod.magic;

/** Примешивается к ServerPlayer, хранит ману игрока. */
public interface ManaHolder {
    float MAX_MANA = 100.0F;

    float magicalmod$getMana();

    void magicalmod$setMana(float mana);
}
