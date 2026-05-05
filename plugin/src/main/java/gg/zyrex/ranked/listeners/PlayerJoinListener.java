package gg.zyrex.ranked.listeners;

import gg.zyrex.ranked.ZyrexRanked;
import gg.zyrex.ranked.models.Match;
import gg.zyrex.ranked.models.RankedPlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerJoinListener implements Listener {

    private final ZyrexRanked plugin;

    public PlayerJoinListener(ZyrexRanked plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player p = event.getPlayer();
        // Load player from DB async, then cache
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
            plugin.getDatabaseManager().createPlayerIfNotExists(p.getUniqueId(), p.getName());
            RankedPlayer rp = plugin.getDatabaseManager().loadPlayer(p.getUniqueId());
            if (rp == null) rp = new RankedPlayer(p.getUniqueId(), p.getName());
            rp.setUsername(p.getName()); // update name if changed
            plugin.getQueueManager().cachePlayer(p.getUniqueId(), rp);
        });
    }
}
