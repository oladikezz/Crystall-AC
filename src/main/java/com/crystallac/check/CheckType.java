package com.crystallac.check;

public enum CheckType {
    KILLAURA("Killaura", CheckCategory.COMBAT),
    REACH("Reach", CheckCategory.COMBAT),
    AUTOCLICKER("Autoclicker", CheckCategory.COMBAT),
    AIMBOT("Aimbot", CheckCategory.COMBAT),
    SPEED("Speed", CheckCategory.MOVEMENT),
    FLY("Fly", CheckCategory.MOVEMENT),
    TIMER("Timer", CheckCategory.PACKET),
    SCAFFOLD("Scaffold", CheckCategory.WORLD),
    FASTPLACE("FastPlace", CheckCategory.WORLD),
    BADPACKETS("BadPackets", CheckCategory.PACKET),
    FASTBREAK("FastBreak", CheckCategory.WORLD),
    INVENTORY("InventoryMove", CheckCategory.INVENTORY);

    private final String displayName;
    private final CheckCategory category;

    CheckType(String displayName, CheckCategory category) {
        this.displayName = displayName;
        this.category = category;
    }

    public String getDisplayName() {
        return displayName;
    }

    public CheckCategory getCategory() {
        return category;
    }
}
