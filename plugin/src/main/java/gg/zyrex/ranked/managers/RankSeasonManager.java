package gg.zyrex.ranked.managers;

import gg.zyrex.ranked.ZyrexRanked;
import gg.zyrex.ranked.models.Gamemode;
import gg.zyrex.ranked.models.RankedPlayer;
import org.bukkit.entity.Player;

import java.sql.*;
import java.util.logging.Level;

// ─────────────────────────────────────────────────────────────────────────────
// RankManager - handles scoreboard/tablist rank display
// ─────────────────────────────────────────────────────────────────────────────
public class RankManager {

    private final ZyrexRanked plugin;

    public RankManager(ZyrexRanked plugin) {
        this.plugin = plugin;
    }

    /**
     * Update the player's display name and tab-list to show their rank.
     * Call this after any ELO change.
     */
    public void updatePlayerDisplay(Player player, RankedPlayer rp) {
        String rankDisplay = plugin.getEloManager().getRankName(rp.getGlobalElo());
        String prefix = rankDisplay + " §r";
        // Update player list name
        player.playerListName(net.kyori.adventure.text.Component.text(
                rankDisplay + " " + player.getName()
        ));
        // Update display name in chat
        player.displayName(net.kyori.adventure.text.Component.text(
                rankDisplay + " §f" + player.getName()
        ));
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SeasonManager - handles season resets and history archiving
// ─────────────────────────────────────────────────────────────────────────────
class SeasonManager {

    private final ZyrexRanked plugin;

    public SeasonManager(ZyrexRanked plugin) {
        this.plugin = plugin;
    }

    /**
     * Run a full season reset.
     * - Archives all player stats to zyrex_season_history
     * - Soft-resets ELO (compresses toward 1000)
     * - Increments season number in config
     *
     * Call this manually via /rankedadmin season reset
     */
    public void performSeasonReset() {
        int currentSeason = plugin.getConfig().getInt("seasons.current-season", 1);
        plugin.getLogger().info("Starting season reset from season " + currentSeason + "...");

        try (Connection conn = plugin.getDatabaseManager().getConnection()) {
            // 1. Archive current season stats
            PreparedStatement archive = conn.prepareStatement("""
                INSERT INTO zyrex_season_history (uuid, season, gamemode, final_elo, rank_name, wins, losses, peak_elo)
                SELECT s.uuid, ?, s.gamemode, s.elo,
                       CASE
                           WHEN s.elo >= 2950 THEN 'Champion'
                           WHEN s.elo >= 2800 THEN 'Netherite III'
                           WHEN s.elo >= 2600 THEN 'Netherite II'
                           WHEN s.elo >= 2400 THEN 'Netherite I'
                           WHEN s.elo >= 2300 THEN 'Diamond III'
                           WHEN s.elo >= 2100 THEN 'Diamond II'
                           WHEN s.elo >= 1900 THEN 'Diamond I'
                           WHEN s.elo >= 1700 THEN 'Platinum'
                           WHEN s.elo >= 1500 THEN 'Gold'
                           WHEN s.elo >= 1300 THEN 'Silver'
                           WHEN s.elo >= 1100 THEN 'Bronze'
                           ELSE 'Iron'
                       END,
                       s.wins, s.losses, s.peak_elo
                FROM zyrex_stats s
            """);
            archive.setInt(1, currentSeason);
            archive.executeUpdate();

            // 2. Soft reset ELO — compress toward 1000 by factor
            double factor = plugin.getConfig().getDouble("seasons.soft-reset-factor", 0.30);
            PreparedStatement reset = conn.prepareStatement("""
                UPDATE zyrex_stats
                SET elo = GREATEST(100, CAST(elo - (elo - 1000) * ? AS SIGNED)),
                    peak_elo = GREATEST(100, CAST(elo - (elo - 1000) * ? AS SIGNED)),
                    wins = 0, losses = 0, win_streak = 0, best_streak = 0,
                    placement_played = 0
            """);
            reset.setDouble(1, factor);
            reset.setDouble(2, factor);
            reset.executeUpdate();

            // 3. Reset global ELO on players table
            conn.createStatement().execute("""
                UPDATE zyrex_players p
                SET global_elo = (SELECT AVG(elo) FROM zyrex_stats WHERE uuid = p.uuid),
                    total_wins = 0, total_losses = 0,
                    current_season = current_season + 1
            """);

            int newSeason = currentSeason + 1;
            plugin.getConfig().set("seasons.current-season", newSeason);
            plugin.saveConfig();

            plugin.getLogger().info("Season reset complete! Now in Season " + newSeason);
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Season reset failed: " + e.getMessage(), e);
        }
    }

    public int getCurrentSeason() {
        return plugin.getConfig().getInt("seasons.current-season", 1);
    }
}
