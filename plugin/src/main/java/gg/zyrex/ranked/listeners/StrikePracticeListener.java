package gg.zyrex.ranked.listeners;

import gg.zyrex.ranked.ZyrexRanked;
import gg.zyrex.ranked.models.Gamemode;
import gg.zyrex.ranked.models.Match;
import gg.zyrex.ranked.models.RankedPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

// ─────────────────────────────────────────────────────────────────────────────
// StrikePractice Event Hook
//
// This replaces ZyrexRanked's internal match system entirely.
// StrikePractice runs all matchmaking, queues, arenas, and kits as normal.
// When a match ends, ZyrexRanked intercepts the event to record ELO.
//
// Required: Add StrikePractice as a soft-depend in plugin.yml:
//   softdepend: [StrikePractice]
//
// StrikePractice event classes (check your SP version for exact package):
//   com.AndyReckt.StrikePractice.API.Events.MatchEndEvent
//   com.AndyReckt.StrikePractice.API.Events.MatchStartEvent
// ─────────────────────────────────────────────────────────────────────────────
public class StrikePracticeListener implements Listener {

    private final ZyrexRanked plugin;

    public StrikePracticeListener(ZyrexRanked plugin) {
        this.plugin = plugin;
    }

    /**
     * Called when a StrikePractice match ends.
     *
     * StrikePractice provides: winner UUID, loser UUID, queue type (gamemode name).
     * We map SP's queue name to our Gamemode enum, then process ELO.
     *
     * NOTE: Replace the import and event class below with the exact
     * StrikePractice API event from your SP version.
     * Common locations:
     *   - com.AndyReckt.StrikePractice.API.Events.MatchEndEvent
     *   - me.andyreckt.strikepractice.api.events.MatchEndEvent
     */

    // ── Uncomment and adjust the import when you know your SP version ──
    // @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    // public void onMatchEnd(MatchEndEvent event) {
    //
    //     // Only process ranked (not unranked/casual) matches
    //     if (!event.isRanked()) return;
    //
    //     UUID winnerUuid = event.getWinner();
    //     UUID loserUuid  = event.getLoser();
    //     String queueName = event.getQueueName(); // e.g. "Pot PvP", "UHC", "Sword"
    //
    //     Gamemode gamemode = mapSPQueueToGamemode(queueName);
    //     if (gamemode == null) {
    //         plugin.getLogger().warning("Unknown SP queue: " + queueName + " — skipping ELO");
    //         return;
    //     }
    //
    //     processElo(winnerUuid, loserUuid, gamemode);
    // }

    // ─────────────────────────────────────────────────────────────────────────
    // Manual trigger (use this if SP events don't fire, or for testing)
    // Command: /rankedadmin recordmatch <winner> <loser> <gamemode>
    // ─────────────────────────────────────────────────────────────────────────
    public void processElo(java.util.UUID winnerUuid, java.util.UUID loserUuid, Gamemode gamemode) {
        // Load or get cached players
        RankedPlayer winner = getOrLoadPlayer(winnerUuid);
        RankedPlayer loser  = getOrLoadPlayer(loserUuid);

        if (winner == null || loser == null) {
            plugin.getLogger().warning("Could not load ranked data for match participants.");
            return;
        }

        // Build a synthetic Match object for ELO processing
        String matchId = java.util.UUID.randomUUID().toString();
        Match match = new Match(matchId, winnerUuid, loserUuid, gamemode,
                winner.getElo(gamemode), loser.getElo(gamemode));
        match.setState(Match.MatchState.IN_PROGRESS);
        match.setResult(Match.MatchResult.PLAYER1_WIN);

        // Process ELO
        int[] deltas = plugin.getEloManager().processMatchResult(match, winner, loser);

        // Notify players
        Player wp = Bukkit.getPlayer(winnerUuid);
        Player lp = Bukkit.getPlayer(loserUuid);

        if (wp != null) {
            String msg = plugin.getConfig().getString("messages.match-end-win", "")
                    .replace("{elo}", String.valueOf(Math.abs(deltas[0])))
                    .replace("{new_elo}", String.valueOf(winner.getElo(gamemode)));
            wp.sendMessage(plugin.getConfig().getString("messages.prefix", "") + msg);

            if (winner.getWinStreak(gamemode) >= plugin.getConfig().getInt("elo.streak-threshold", 3)) {
                wp.sendMessage(plugin.getConfig().getString("messages.streak", "")
                        .replace("{streak}", String.valueOf(winner.getWinStreak(gamemode)))
                        .replace("{bonus}", "5"));
            }
        }

        if (lp != null) {
            String msg = plugin.getConfig().getString("messages.match-end-loss", "")
                    .replace("{elo}", String.valueOf(Math.abs(deltas[1])))
                    .replace("{new_elo}", String.valueOf(loser.getElo(gamemode)));
            lp.sendMessage(plugin.getConfig().getString("messages.prefix", "") + msg);
        }

        // Save async + push to API
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getDatabaseManager().savePlayer(winner);
            plugin.getDatabaseManager().savePlayer(loser);
            plugin.getDatabaseManager().saveMatch(match);
            plugin.getApiClient().postMatchResult(match, winner, loser);
        });

        // Update tab/display names
        if (wp != null) plugin.getRankManager().updatePlayerDisplay(wp, winner);
        if (lp != null) plugin.getRankManager().updatePlayerDisplay(lp, loser);
    }

    private RankedPlayer getOrLoadPlayer(java.util.UUID uuid) {
        RankedPlayer rp = plugin.getQueueManager().getCachedPlayer(uuid);
        if (rp != null) return rp;
        // Player is offline or not cached — load from DB
        rp = plugin.getDatabaseManager().loadPlayer(uuid);
        if (rp != null) plugin.getQueueManager().cachePlayer(uuid, rp);
        return rp;
    }

    /**
     * Maps StrikePractice queue names to ZyrexRanked Gamemode enum.
     * Adjust these strings to match exactly what SP returns for your queues.
     */
    public static Gamemode mapSPQueueToGamemode(String queueName) {
        if (queueName == null) return null;
        String q = queueName.toLowerCase().trim();
        if (q.contains("crystal") || q.contains("smp"))                return Gamemode.SMP;
        if (q.contains("pot") || q.contains("neth"))                   return Gamemode.POT;
        if (q.contains("uhc"))                                          return Gamemode.UHC;
        if (q.contains("sword") || q.contains("axe") || q.contains("mace") && !q.contains("spear")) return Gamemode.SWORD;
        if (q.contains("spear") || q.contains("spearmace"))            return Gamemode.SPEARMACE;
        return null;
    }
}
