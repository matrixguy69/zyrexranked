package gg.zyrex.ranked.listeners;

import gg.zyrex.ranked.ZyrexRanked;
import gg.zyrex.ranked.models.Match;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class PlayerDeathListener implements Listener {

    private final ZyrexRanked plugin;

    public PlayerDeathListener(ZyrexRanked plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player dead = event.getEntity();
        Match match = plugin.getMatchManager().getMatchByPlayer(dead.getUniqueId());
        if (match == null || match.getState() != Match.MatchState.IN_PROGRESS) return;

        event.setDeathMessage(null); // suppress default death message
        event.setDroppedExp(0);
        event.getDrops().clear();

        // Respawn immediately to avoid respawn screen
        dead.spigot().respawn();

        // Determine winner
        Match.MatchResult result;
        if (match.getPlayer1().equals(dead.getUniqueId())) {
            result = Match.MatchResult.PLAYER2_WIN;
        } else {
            result = Match.MatchResult.PLAYER1_WIN;
        }

        plugin.getMatchManager().endMatch(match, result);
    }
}
