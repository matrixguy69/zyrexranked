package gg.zyrex.ranked.models;

import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;

public class RankedPlayer {

    private final UUID uuid;
    private String username;

    // Per-gamemode stats
    private final Map<Gamemode, Integer> elo = new EnumMap<>(Gamemode.class);
    private final Map<Gamemode, Integer> wins = new EnumMap<>(Gamemode.class);
    private final Map<Gamemode, Integer> losses = new EnumMap<>(Gamemode.class);
    private final Map<Gamemode, Integer> placementMatches = new EnumMap<>(Gamemode.class);
    private final Map<Gamemode, Integer> winStreak = new EnumMap<>(Gamemode.class);
    private final Map<Gamemode, Integer> bestStreak = new EnumMap<>(Gamemode.class);
    private final Map<Gamemode, Integer> peakElo = new EnumMap<>(Gamemode.class);

    // Global
    private int globalElo = 1000;
    private int totalWins = 0;
    private int totalLosses = 0;
    private long lastSeen = System.currentTimeMillis();
    private int currentSeason = 1;

    public RankedPlayer(UUID uuid, String username) {
        this.uuid = uuid;
        this.username = username;

        // Initialize all gamemodes with defaults
        for (Gamemode g : Gamemode.values()) {
            elo.put(g, 1000);
            wins.put(g, 0);
            losses.put(g, 0);
            placementMatches.put(g, 0);
            winStreak.put(g, 0);
            bestStreak.put(g, 0);
            peakElo.put(g, 1000);
        }
    }

    // ── ELO ──────────────────────────────────────────────────────────
    public int getElo(Gamemode g) { return elo.getOrDefault(g, 1000); }

    public void setElo(Gamemode g, int value) {
        int floor = Math.max(value, 100);
        elo.put(g, floor);
        if (floor > getPeakElo(g)) peakElo.put(g, floor);
        recalcGlobalElo();
    }

    public void addElo(Gamemode g, int amount) { setElo(g, getElo(g) + amount); }

    private void recalcGlobalElo() {
        int sum = 0;
        for (int v : elo.values()) sum += v;
        globalElo = sum / elo.size();
    }

    // ── Wins / Losses ────────────────────────────────────────────────
    public int getWins(Gamemode g) { return wins.getOrDefault(g, 0); }
    public int getLosses(Gamemode g) { return losses.getOrDefault(g, 0); }

    public void addWin(Gamemode g) {
        wins.put(g, getWins(g) + 1);
        totalWins++;
        int streak = winStreak.getOrDefault(g, 0) + 1;
        winStreak.put(g, streak);
        if (streak > bestStreak.getOrDefault(g, 0)) bestStreak.put(g, streak);
    }

    public void addLoss(Gamemode g) {
        losses.put(g, getLosses(g) + 1);
        totalLosses++;
        winStreak.put(g, 0);
    }

    // ── Placement ────────────────────────────────────────────────────
    public int getPlacementMatchesPlayed(Gamemode g) { return placementMatches.getOrDefault(g, 0); }
    public boolean isInPlacement(Gamemode g, int totalRequired) {
        return placementMatches.getOrDefault(g, 0) < totalRequired;
    }
    public void incrementPlacement(Gamemode g) {
        placementMatches.put(g, getPlacementMatchesPlayed(g) + 1);
    }

    // ── Streak ───────────────────────────────────────────────────────
    public int getWinStreak(Gamemode g) { return winStreak.getOrDefault(g, 0); }
    public int getBestStreak(Gamemode g) { return bestStreak.getOrDefault(g, 0); }

    // ── W/L Ratio ────────────────────────────────────────────────────
    public double getWLRatio(Gamemode g) {
        int l = getLosses(g);
        return l == 0 ? getWins(g) : (double) getWins(g) / l;
    }

    // ── Misc ─────────────────────────────────────────────────────────
    public int getPeakElo(Gamemode g) { return peakElo.getOrDefault(g, 1000); }
    public UUID getUuid() { return uuid; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public int getGlobalElo() { return globalElo; }
    public int getTotalWins() { return totalWins; }
    public int getTotalLosses() { return totalLosses; }
    public long getLastSeen() { return lastSeen; }
    public void setLastSeen(long t) { this.lastSeen = t; }
    public int getCurrentSeason() { return currentSeason; }
    public void setCurrentSeason(int s) { this.currentSeason = s; }
}
