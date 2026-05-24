package gg.zyrex.ranked.models;

public enum Gamemode {
    SWORD      ("sword",     "Sword",         "§6"),
    MACE       ("mace",      "Mace",          "§d"),
    AXE        ("axe",       "Axe",           "§c"),
    SMP        ("smp",       "SMP",           "§b"),
    DIASMP     ("diasmp",    "Diamond SMP",   "§3"),
    POT        ("pot",       "Pot PvP",       "§a"),
    NETHPOT    ("nethpot",   "NethPot",       "§2"),
    CRYSTAL    ("crystal",   "Crystal PvP",   "§f"),
    SPEARMACE  ("spearmace", "Spear Mace",    "§5"),
    UHC        ("uhc",       "UHC",           "§e");

    private final String id;
    private final String displayName;
    private final String color;

    Gamemode(String id, String displayName, String color) {
        this.id = id;
        this.displayName = displayName;
        this.color = color;
    }

    public String getId()          { return id; }
    public String getDisplayName() { return displayName; }
    public String getColor()       { return color; }
    public String getColoredName() { return color + displayName; }

    public static Gamemode fromId(String id) {
        if (id == null) return null;
        for (Gamemode g : values())
            if (g.id.equalsIgnoreCase(id)) return g;
        return null;
    }

    // Map StrikePractice queue names to gamemodes
    public static Gamemode fromSPQueue(String queueName) {
        if (queueName == null) return null;
        String q = queueName.toLowerCase().trim();
        if (q.contains("crystal"))                          return CRYSTAL;
        if (q.contains("diasmp") || q.contains("diamond smp")) return DIASMP;
        if (q.contains("smp"))                              return SMP;
        if (q.contains("nethpot") || q.contains("neth"))   return NETHPOT;
        if (q.contains("pot"))                              return POT;
        if (q.contains("uhc"))                              return UHC;
        if (q.contains("spearmace") || q.contains("spear")) return SPEARMACE;
        if (q.contains("mace"))                             return MACE;
        if (q.contains("axe"))                             return AXE;
        if (q.contains("sword"))                           return SWORD;
        return null;
    }

    public static String listAll() {
        StringBuilder sb = new StringBuilder();
        for (Gamemode g : values())
            sb.append(g.color).append(g.displayName).append("§7, ");
        return sb.substring(0, sb.length() - 2);
    }
}
