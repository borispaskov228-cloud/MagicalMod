package com.magicalmod.magic;

/** Примешивается к ServerPlayer, хранит ману и прогресс её прокачки. */
public interface ManaHolder {
    float BASE_MAX_MANA = 100.0F;
    int MAX_LEVEL = 10;
    int XP_PER_LEVEL = 10;

    float magicalmod$getMana();

    void magicalmod$setMana(float mana);

    int magicalmod$getManaLevel();

    void magicalmod$setManaLevel(int level);

    int magicalmod$getManaXp();

    void magicalmod$setManaXp(int xp);

    default float magicalmod$getMaxMana() {
        return BASE_MAX_MANA + magicalmod$getManaLevel() * 20.0F;
    }
}
