package com.example.zitraksmode.items.katana;

/**
 * Типы атак катаны - для комбо системы
 */
public enum AttackType {
    DIAGONAL_LEFT("Diagonal Slash (Left)"),
    DIAGONAL_RIGHT("Diagonal Slash (Right)"),
    VERTICAL("Vertical Slash"),
    HORIZONTAL("Horizontal Slash");
    
    private final String displayName;
    
    AttackType(String displayName) {
        this.displayName = displayName;
    }
    
    public String getDisplayName() {
        return displayName;
    }
    
    /**
     * Получить случайный тип атаки, НЕ ПОВТОРЯЯ предыдущий
     */
    public static AttackType getRandomExcluding(AttackType exclude) {
        AttackType[] types = values();
        AttackType selected;
        
        do {
            selected = types[(int) (Math.random() * types.length)];
        } while (selected == exclude && types.length > 1);
        
        return selected;
    }
}
