package com.example.zitraksmode.items;

public enum LabubuType {
    // === ОБЫЧНЫЕ (БЕЛЫЕ) ===
    RED("red", LabubuRarity.COMMON),
    BLUE("blue", LabubuRarity.COMMON),
    YELLOW("yellow", LabubuRarity.COMMON),
    WHITE("white", LabubuRarity.COMMON),
    BLACK("black", LabubuRarity.COMMON),
    GREEN("green", LabubuRarity.COMMON),

    // === НЕОБЫЧНЫЕ (ЗЕЛЕНЫЕ) ===
    PURPLE("purple", LabubuRarity.UNCOMMON),
    BLACK_WHITE("black_white", LabubuRarity.UNCOMMON),
    STRIPED("striped", LabubuRarity.UNCOMMON),

    // === РЕДКИЕ (СИНИЕ) ===
    KAZAKH("kazakh", LabubuRarity.RARE),
    UKRAINIAN("ukrainian", LabubuRarity.RARE),
    JAPANESE("japanese", LabubuRarity.RARE),
    TOP_HAT("top_hat", LabubuRarity.RARE),
    FASELESS("faseless", LabubuRarity.RARE),

    // === ЭПИЧЕСКИЕ (ФИОЛЕТОВЫЕ) ===
    RUSSIAN("russian", LabubuRarity.EPIC),
    NEON("neon", LabubuRarity.EPIC),
    LITVIN("litvin", LabubuRarity.EPIC),
    ANONYMOUS("anonymous", LabubuRarity.EPIC),
    DEMON("demon", LabubuRarity.EPIC),
    ANGEL("angel", LabubuRarity.EPIC),

    // === ЛЕГЕНДАРНЫЕ (ЗОЛОТЫЕ) ===
    GALACTIC("galactic", LabubuRarity.LEGENDARY),
    RAINBOW("rainbow", LabubuRarity.LEGENDARY),
    GOLDEN("golden", LabubuRarity.LEGENDARY),
    BUDDHA("buddha", LabubuRarity.LEGENDARY),
    BOOBA("booba", LabubuRarity.LEGENDARY),
    ANIME("anime", LabubuRarity.LEGENDARY);

    public final String name;
    public final LabubuRarity rarity;

    LabubuType(String name, LabubuRarity rarity) {
        this.name = name;
        this.rarity = rarity;
    }
}