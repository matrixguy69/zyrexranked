package gg.zyrex.ranked.utils;

import gg.zyrex.ranked.ZyrexRanked;
import gg.zyrex.ranked.models.Gamemode;
import gg.zyrex.ranked.models.RankedPlayer;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

/**
 * ZyrexRanked PlaceholderAPI Expansion
 *
 * Register in your onEnable():
 *   if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
 *       new ZyrexPlaceholderExpansion(this).register();
 *   }
 *
 * Available placeholders:
 *
 * Global:
 *   %zyrex_elo%               → Global ELO (average of all gamemodes)
 *   %zyrex_rank%              → Global rank name (e.g. "Diamond II")
 *   %zyrex_rank_colored%      → Colored rank name (e.g. "§3Diamond II")
 *   %zyrex_wins%              → Total wins across all gamemodes
 *   %zyrex_losses%            → Total losses across all gamemodes
 *
 * Per-gamemode (replace <gm> with: smp, pot, uhc, sword, spearmace):
 *   %zyrex_elo_<gm>%          → ELO for that gamemode
 *   %zyrex_rank_<gm>%         → Rank name for that gamemode
 *   %zyrex_wins_<gm>%         → Wins for that gamemode
 *   %zyrex_losses_<gm>%       → Losses for that gamemode
 *   %zyrex_streak_<gm>%       → Current win streak
 *   %zyrex_peak_<gm>%         → Peak ELO for that gamemode
 *   %zyrex_wl_<gm>%           → W/L ratio for that gamemode
 *
 * Leaderboard (top 10):
 *   %zyrex_top_name_<1-10>%   → #N player name on global leaderboard
 *   %zyrex_top_elo_<1-10>%    → #N player ELO on global leaderboard
 *   %zyrex_top_rank_<1-10>%   → #N player rank name on global leaderboard
 */
public class ZyrexPlaceholderExpansion extends PlaceholderExpansion {

    private final ZyrexRanked plugin;

    public ZyrexPlaceholderExpansion(ZyrexRanked plugin) {
        this.plugin = plugin;
    }

    @Override
    public @NotNull String getIdentifier() { return "zyrex"; }

    @Override
    public @NotNull String getAuthor() { return "ZyrexNetwork"; }

    @Override
    public @NotNull String getVersion() { return plugin.getDescription().getVersion(); }

    @Override
    public boolean persist() { return true; }

    @Override
    public boolean canRegister() { return true; }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String identifier) {
        if (player == null) return "";

        RankedPlayer rp = plugin.getQueueManager().getCachedPlayer(player.getUniqueId());

        // Load from DB if not cached (offline player lookup)
        if (rp == null) {
            rp = plugin.getDatabaseManager().loadPlayer(player.getUniqueId());
            if (rp == null) return identifier.contains("elo") || identifier.contains("wins") || identifier.contains("losses") ? "0" : "Unranked";
        }

        // ── Global placeholders ──────────────────────────────────────
        if (identifier.equals("elo"))           return String.valueOf(rp.getGlobalElo());
        if (identifier.equals("rank"))          return plugin.getEloManager().getRankNameClean(rp.getGlobalElo());
        if (identifier.equals("rank_colored"))  return plugin.getEloManager().getRankName(rp.getGlobalElo());
        if (identifier.equals("wins"))          return String.valueOf(rp.getTotalWins());
        if (identifier.equals("losses"))        return String.valueOf(rp.getTotalLosses());

        // ── Per-gamemode placeholders ────────────────────────────────
        for (Gamemode gm : Gamemode.values()) {
            String id = gm.getId();
            if (identifier.equals("elo_" + id))     return String.valueOf(rp.getElo(gm));
            if (identifier.equals("rank_" + id))    return plugin.getEloManager().getRankNameClean(rp.getElo(gm));
            if (identifier.equals("wins_" + id))    return String.valueOf(rp.getWins(gm));
            if (identifier.equals("losses_" + id))  return String.valueOf(rp.getLosses(gm));
            if (identifier.equals("streak_" + id))  return String.valueOf(rp.getWinStreak(gm));
            if (identifier.equals("peak_" + id))    return String.valueOf(rp.getPeakElo(gm));
            if (identifier.equals("wl_" + id)) {
                int l = rp.getLosses(gm);
                return l == 0 ? (rp.getWins(gm) + ".0") : String.format("%.2f", (double) rp.getWins(gm) / l);
            }
        }

        // ── Leaderboard placeholders (top 1-10) ──────────────────────
        if (identifier.startsWith("top_")) {
            String[] parts = identifier.split("_");
            if (parts.length == 3) {
                String type = parts[1]; // "name", "elo", "rank"
                int pos;
                try { pos = Integer.parseInt(parts[2]); } catch (NumberFormatException e) { return ""; }
                if (pos < 1 || pos > 10) return "";

                // Query cached leaderboard (refreshed every 30s)
                return getLeaderboardValue(type, pos);
            }
        }

        return null; // Unknown placeholder
    }

    private String getLeaderboardValue(String type, int position) {
        try (java.sql.Connection conn = plugin.getDatabaseManager().getConnection()) {
            java.sql.PreparedStatement ps = conn.prepareStatement(
                "SELECT username, global_elo FROM zyrex_players ORDER BY global_elo DESC LIMIT ? OFFSET ?"
            );
            ps.setInt(1, 1);
            ps.setInt(2, position - 1);
            java.sql.ResultSet rs = ps.executeQuery();
            if (!rs.next()) return "N/A";
            return switch (type) {
                case "name" -> rs.getString("username");
                case "elo"  -> String.valueOf(rs.getInt("global_elo"));
                case "rank" -> plugin.getEloManager().getRankNameClean(rs.getInt("global_elo"));
                default     -> "N/A";
            };
        } catch (Exception e) {
            return "N/A";
        }
    }
}
