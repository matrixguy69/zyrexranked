package gg.zyrex.ranked.listeners;

import gg.zyrex.ranked.ZyrexRanked;
import gg.zyrex.ranked.models.Match;
import gg.zyrex.ranked.models.RankedPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerQuitListener implements Listener {

    private final ZyrexRanked plugin;

    public PlayerQuitListener(ZyrexRanked plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        Player p = event.getPlayer();

        // If in queue, remove
        if (plugin.getQueueManager().isInQueue(p.getUniqueId())) {
            plugin.getQueueManager().leaveQueue(p.getUniqueId());
        }

        // If in match, they forfeit — opponent wins
        Match match = plugin.getMatchManager().getMatchByPlayer(p.getUniqueId());
        if (match != null && match.getState() == Match.MatchState.IN_PROGRESS) {
            Match.MatchResult result = match.getPlayer1().equals(p.getUniqueId())
                    ? Match.MatchResult.PLAYER2_WIN
                    : Match.MatchResult.PLAYER1_WIN;
            plugin.getMatchManager().endMatch(match, result);
        }

        // Save and uncache
        RankedPlayer rp = plugin.getQueueManager().getCachedPlayer(p.getUniqueId());
        if (rp != null) {
            rp.setLastSeen(System.currentTimeMillis());
            plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                plugin.getDatabaseManager().savePlayer(rp);
                plugin.getQueueManager().uncachePlayer(p.getUniqueId());
            });
        }
    }
}
