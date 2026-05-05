package gg.zyrex.ranked.commands;

import gg.zyrex.ranked.ZyrexRanked;
import gg.zyrex.ranked.models.Gamemode;
import gg.zyrex.ranked.models.RankedPlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class QueueCommand implements CommandExecutor {

    private final ZyrexRanked plugin;

    public QueueCommand(ZyrexRanked plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("§cPlayers only."); return true; }

        if (args.length == 0) {
            p.sendMessage("§8[§dZyrex§8] §7Available gamemodes: " + Gamemode.listAll());
            p.sendMessage("§8[§dZyrex§8] §7Usage: §f/queue <gamemode>");
            return true;
        }

        Gamemode gm = Gamemode.fromId(args[0]);
        if (gm == null) {
            p.sendMessage("§8[§dZyrex§8] §cUnknown gamemode. Available: " + Gamemode.listAll());
            return true;
        }

        if (plugin.getQueueManager().isInQueue(p.getUniqueId())) {
            p.sendMessage("§8[§dZyrex§8] " + plugin.getConfig().getString("messages.already-in-queue", ""));
            return true;
        }

        if (plugin.getMatchManager().isInMatch(p.getUniqueId())) {
            p.sendMessage("§8[§dZyrex§8] " + plugin.getConfig().getString("messages.already-in-match", ""));
            return true;
        }

        plugin.getQueueManager().joinQueue(p.getUniqueId(), gm);
        return true;
    }
}
