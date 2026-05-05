package gg.zyrex.ranked.listeners;

import gg.zyrex.ranked.ZyrexRanked;
import gg.zyrex.ranked.models.Match;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {
    private final ZyrexRanked plugin;
    public ChatListener(ZyrexRanked plugin) { this.plugin = plugin; }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onChat(AsyncPlayerChatEvent e) {
        Player p = e.getPlayer();
        Match match = plugin.getMatchManager().getMatchByPlayer(p.getUniqueId());
        if (match == null) return;
        // Route match chat only to the two players + spectators
        e.setCancelled(true);
        String msg = "§8[§7Match§8] §f" + p.getName() + ": §7" + e.getMessage();
        org.bukkit.Bukkit.getPlayer(match.getPlayer1()) .ifPresent(pl -> pl.sendMessage(msg));
        org.bukkit.Bukkit.getPlayer(match.getPlayer2()) .ifPresent(pl -> pl.sendMessage(msg));
        match.getSpectators().forEach(uuid -> {
            org.bukkit.entity.Player sp = org.bukkit.Bukkit.getPlayer(uuid);
            if (sp != null) sp.sendMessage(msg);
        });
    }
}
