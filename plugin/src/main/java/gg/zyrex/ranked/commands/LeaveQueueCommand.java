package gg.zyrex.ranked.commands;

import gg.zyrex.ranked.ZyrexRanked;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class LeaveQueueCommand implements CommandExecutor {
    private final ZyrexRanked plugin;
    public LeaveQueueCommand(ZyrexRanked plugin) { this.plugin = plugin; }
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("§cPlayers only."); return true; }
        if (!plugin.getQueueManager().isInQueue(p.getUniqueId())) {
            p.sendMessage("§8[§dZyrex§8] §cYou are not in any queue.");
            return true;
        }
        plugin.getQueueManager().leaveQueue(p.getUniqueId());
        return true;
    }
}
