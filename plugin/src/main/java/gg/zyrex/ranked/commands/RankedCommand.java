package gg.zyrex.ranked.commands;

import gg.zyrex.ranked.ZyrexRanked;
import gg.zyrex.ranked.models.Gamemode;
import gg.zyrex.ranked.models.RankedPlayer;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RankedCommand implements CommandExecutor {

    private final ZyrexRanked plugin;

    public RankedCommand(ZyrexRanked plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("§cPlayers only."); return true; }

        RankedPlayer target;
        String targetName;

        if (args.length == 0) {
            target = plugin.getQueueManager().getCachedPlayer(p.getUniqueId());
            targetName = p.getName();
        } else {
            Player tp = Bukkit.getPlayerExact(args[0]);
            if (tp != null) {
                target = plugin.getQueueManager().getCachedPlayer(tp.getUniqueId());
                targetName = tp.getName();
            } else {
                p.sendMessage("§8[§dZyrex§8] §cPlayer not found or offline.");
                return true;
            }
        }

        if (target == null) {
            p.sendMessage("§8[§dZyrex§8] §cNo ranked data found.");
            return true;
        }

        sendStats(p, target, targetName);
        return true;
    }

    private void sendStats(Player viewer, RankedPlayer rp, String name) {
        String prefix = "§8[§dZyrex§8] ";
        viewer.sendMessage("");
        viewer.sendMessage(prefix + "§d§l" + name + "§8's §fRanked Stats");
        viewer.sendMessage("§8§m────────────────────────────");
        viewer.sendMessage(prefix + "§7Global ELO: §f" + rp.getGlobalElo()
                + " §8(§f" + plugin.getEloManager().getRankName(rp.getGlobalElo()) + "§8)");
        viewer.sendMessage(prefix + "§7Total W/L: §a" + rp.getTotalWins() + "§7/§c" + rp.getTotalLosses());
        viewer.sendMessage("§8§m────────────────────────────");

        for (Gamemode gm : Gamemode.values()) {
            int elo = rp.getElo(gm);
            int wins = rp.getWins(gm);
            int losses = rp.getLosses(gm);
            int toNext = plugin.getEloManager().eloToNextRank(elo);
            String rank = plugin.getEloManager().getRankName(elo);
            viewer.sendMessage(gm.getColor() + "§l" + gm.getDisplayName() + "§r §8│ "
                    + rank + " §8(§f" + elo + "§8) §7W/L: §a" + wins + "§7/§c" + losses
                    + (toNext > 0 ? " §8│ §7Next: §f+" + toNext : " §8│ §d§lMAX"));
        }

        viewer.sendMessage("§8§m────────────────────────────");
        viewer.sendMessage("");
    }
}
