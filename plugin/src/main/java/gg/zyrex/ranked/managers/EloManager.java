package gg.zyrex.ranked.managers;

import gg.zyrex.ranked.ZyrexRanked;
import gg.zyrex.ranked.models.Gamemode;
import gg.zyrex.ranked.models.Match;
import gg.zyrex.ranked.models.RankedPlayer;

public class EloManager {

    private final ZyrexRanked plugin;

    // K-factor thresholds
    private static final int K_PLACEMENT = 40;
    private static final int K_DEFAULT   = 20;
    private static final int K_DIAMOND   = 16; // 2300+ ELO
    private static final int K_TOP100    = 10; // top 100 players

    private static final int DIAMOND_THRESHOLD = 2300;
    private static final int STREAK_THRESHOLD = 3;
    private static final int STREAK_BONUS = 5;

    public EloManager(ZyrexRanked plugin) {
        this.plugin = plugin;
    }

    /**
     * Process ELO changes after a match ends.
     * Returns int[2] = {p1Delta, p2Delta}
     */
    public int[] processMatchResult(Match match, RankedPlayer p1, RankedPlayer p2) {
        Gamemode gm = match.getGamemode();
        int elo1 = p1.getElo(gm);
        int elo2 = p2.getElo(gm);

        int placementRequired = plugin.getConfig().getInt("elo.placement-matches", 10);

        int k1 = getKFactor(p1, gm, placementRequired);
        int k2 = getKFactor(p2, gm, placementRequired);

        // Expected scores (standard Elo formula)
        double expected1 = expectedScore(elo1, elo2);
        double expected2 = expectedScore(elo2, elo1);

        // Actual scores
        double actual1, actual2;
        switch (match.getResult()) {
            case PLAYER1_WIN -> { actual1 = 1.0; actual2 = 0.0; }
            case PLAYER2_WIN -> { actual1 = 0.0; actual2 = 1.0; }
            default          -> { actual1 = 0.5; actual2 = 0.5; }
        }

        int delta1 = (int) Math.round(k1 * (actual1 - expected1));
        int delta2 = (int) Math.round(k2 * (actual2 - expected2));

        // Apply win streak bonus
        if (match.getResult() == Match.MatchResult.PLAYER1_WIN) {
            p1.addWin(gm);
            p2.addLoss(gm);
            if (p1.getWinStreak(gm) >= STREAK_THRESHOLD) {
                delta1 += STREAK_BONUS;
            }
        } else if (match.getResult() == Match.MatchResult.PLAYER2_WIN) {
            p2.addWin(gm);
            p1.addLoss(gm);
            if (p2.getWinStreak(gm) >= STREAK_THRESHOLD) {
                delta2 += STREAK_BONUS;
            }
        }

        // Update placement counter
        p1.incrementPlacement(gm);
        p2.incrementPlacement(gm);

        // Apply ELO (floor at 100)
        p1.addElo(gm, delta1);
        p2.addElo(gm, delta2);

        match.setPlayer1EloDelta(delta1);
        match.setPlayer2EloDelta(delta2);

        return new int[]{delta1, delta2};
    }

    /**
     * Calculate expected score using standard Elo formula.
     */
    private double expectedScore(int ratingA, int ratingB) {
        return 1.0 / (1.0 + Math.pow(10.0, (ratingB - ratingA) / 400.0));
    }

    /**
     * Determine K-factor based on player state.
     */
    private int getKFactor(RankedPlayer player, Gamemode gm, int placementRequired) {
        if (player.isInPlacement(gm, placementRequired)) return K_PLACEMENT;
        int elo = player.getElo(gm);
        if (elo >= DIAMOND_THRESHOLD) return K_DIAMOND;
        return K_DEFAULT;
    }

    /**
     * Get the rank name for a given ELO value.
     */
    public String getRankName(int elo) {
        if (elo >= 2950) return "§d§lChampion";
        if (elo >= 2800) return "§8Netherite III";
        if (elo >= 2600) return "§8Netherite II";
        if (elo >= 2400) return "§8Netherite I";
        if (elo >= 2300) return "§3Diamond III";
        if (elo >= 2100) return "§3Diamond II";
        if (elo >= 1900) return "§3Diamond I";
        if (elo >= 1700) return "§bPlatinum";
        if (elo >= 1500) return "§eGold";
        if (elo >= 1300) return "§fSilver";
        if (elo >= 1100) return "§6Bronze";
        return "§7Iron";
    }

    public String getRankNameClean(int elo) {
        if (elo >= 2950) return "Champion";
        if (elo >= 2800) return "Netherite III";
        if (elo >= 2600) return "Netherite II";
        if (elo >= 2400) return "Netherite I";
        if (elo >= 2300) return "Diamond III";
        if (elo >= 2100) return "Diamond II";
        if (elo >= 1900) return "Diamond I";
        if (elo >= 1700) return "Platinum";
        if (elo >= 1500) return "Gold";
        if (elo >= 1300) return "Silver";
        if (elo >= 1100) return "Bronze";
        return "Iron";
    }

    /**
     * ELO needed to reach next rank.
     */
    public int eloToNextRank(int elo) {
        int[] thresholds = {1100, 1300, 1500, 1700, 1900, 2100, 2300, 2400, 2600, 2800, 2950};
        for (int t : thresholds) {
            if (elo < t) return t - elo;
        }
        return 0; // Champion
    }

    /**
     * Soft season reset — compress ELO 30% toward 1000.
     */
    public int seasonReset(int currentElo) {
        double factor = plugin.getConfig().getDouble("seasons.soft-reset-factor", 0.30);
        return (int) (currentElo - (currentElo - 1000) * factor);
    }
}
