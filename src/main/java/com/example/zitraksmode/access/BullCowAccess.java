package com.example.zitraksmode.access;

public interface BullCowAccess {

    int VARIANT_NORMAL = 0;
    int VARIANT_BALD = 1;
    int VARIANT_COPPER = 2;

    boolean zitraksmode$isBull();

    void zitraksmode$setBull(boolean bull);

    int zitraksmode$getBullVariant();

    void zitraksmode$setBullVariant(int variant);
}
