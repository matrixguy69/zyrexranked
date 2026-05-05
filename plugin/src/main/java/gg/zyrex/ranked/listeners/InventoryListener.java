package gg.zyrex.ranked.listeners;

import gg.zyrex.ranked.ZyrexRanked;
import gg.zyrex.ranked.models.Match;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class InventoryListener implements Listener {
    private final ZyrexRanked plugin;
    public InventoryListener(ZyrexRanked plugin) { this.plugin = plugin; }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof org.bukkit.entity.Player p)) return;
        // Prevent players in matches from accessing inventory outside match
        // (StrikePractice handles this — left as hook for future use)
    }
}
