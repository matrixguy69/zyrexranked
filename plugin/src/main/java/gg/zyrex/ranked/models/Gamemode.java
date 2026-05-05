package gg.zyrex.ranked.models;

public enum Gamemode {
    SMP("smp", "SMP / Crystal PvP", "§b"),
    POT("pot", "Pot PvP / NethPot", "§a"),
    UHC("uhc", "UHC", "§e"),
    SWORD("sword", "Sword / Axe / Mace", "§6"),
    SPEARMACE("spearmace", "Spear Mace", "§d");

    private final String id;
    private final String displayName;
    private final String color;

    Gamemode(String id, String displayName, String color) {
        this.id = id;
        this.displayName = displayName;
        this.color = color;
    }

    public String getId() { return id; }
    public String getDisplayName() { return displayName; }
    public String getColor() { return color; }
    public String getColoredName() { return color + displayName; }

    public static Gamemode fromId(String id) {
        for (Gamemode g : values()) {
            if (g.id.equalsIgnoreCase(id)) return g;
        }
        return null;
    }

    public static String listAll() {
        StringBuilder sb = new StringBuilder();
        for (Gamemode g : values()) {
            sb.append(g.color).append(g.displayName).append("§7, ");
        }
        return sb.substring(0, sb.length() - 2);
    }
}
