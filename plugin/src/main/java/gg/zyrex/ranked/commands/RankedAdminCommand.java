package gg.zyrex.ranked.commands;

import gg.zyrex.ranked.ZyrexRanked;
import gg.zyrex.ranked.listeners.StrikePracticeListener;
import gg.zyrex.ranked.models.Gamemode;
import gg.zyrex.ranked.models.Match;
import gg.zyrex.ranked.models.RankedPlayer;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class RankedAdminCommand implements CommandExecutor {
    private final ZyrexRanked plugin;
    public RankedAdminCommand(ZyrexRanked plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("zyrex.ranked.admin")) {
            sender.sendMessage("§cNo permission."); return true;
        }
        if (args.length == 0) {
            sender.sendMessage("§8[§dZyrexAdmin§8] §7Subcommands: setelo, resetelo, endmatch, reload, season, recordmatch");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "setelo" -> {
                if (args.length < 4) { sender.sendMessage("§cUsage: /rankedadmin setelo <player> <gamemode> <elo>"); return true; }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) { sender.sendMessage("§cPlayer not online."); return true; }
                Gamemode gm = Gamemode.fromId(args[2]);
                if (gm == null) { sender.sendMessage("§cUnknown gamemode."); return true; }
                int elo;
                try { elo = Integer.parseInt(args[3]); } catch (NumberFormatException e) { sender.sendMessage("§cInvalid ELO."); return true; }
                RankedPlayer rp = plugin.getQueueManager().getCachedPlayer(target.getUniqueId());
                if (rp == null) { sender.sendMessage("§cPlayer not cached."); return true; }
                rp.setElo(gm, elo);
                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> plugin.getDatabaseManager().savePlayer(rp));
                sender.sendMessage("§aSet " + target.getName() + "'s " + gm.getDisplayName() + " ELO to " + elo);
            }
            case "reload" -> {
                plugin.reloadConfig();
                sender.sendMessage("§aConfig reloaded.");
            }
            case "endmatch" -> {
                if (args.length < 2) { sender.sendMessage("§cUsage: /rankedadmin endmatch <player>"); return true; }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) { sender.sendMessage("§cPlayer not online."); return true; }
                Match match = plugin.getMatchManager().getMatchByPlayer(target.getUniqueId());
                if (match == null) { sender.sendMessage("§cNot in a match."); return true; }
                plugin.getMatchManager().endMatch(match, Match.MatchResult.CANCELLED);
                sender.sendMessage("§aMatch cancelled.");
            }
            case "recordmatch" -> {
                // /rankedadmin recordmatch <winner> <loser> <gamemode>
                // Use this to manually record a match result from StrikePractice
                if (args.length < 4) { sender.sendMessage("§cUsage: /rankedadmin recordmatch <winner> <loser> <gamemode>"); return true; }
                Player winner = Bukkit.getPlayerExact(args[1]);
                Player loser  = Bukkit.getPlayerExact(args[2]);
                if (winner == null || loser == null) { sender.sendMessage("§cBoth players must be online."); return true; }
                Gamemode gm = Gamemode.fromId(args[3]);
                if (gm == null) { sender.sendMessage("§cUnknown gamemode."); return true; }
                StrikePracticeListener spListener = new StrikePracticeListener(plugin);
                spListener.processElo(winner.getUniqueId(), loser.getUniqueId(), gm);
                sender.sendMessage("§aRecorded ELO for " + winner.getName() + " vs " + loser.getName() + " (" + gm.getDisplayName() + ")");
            }
            case "season" -> {
                if (args.length < 2 || !args[1].equalsIgnoreCase("reset")) {
                    sender.sendMessage("§cUsage: /rankedadmin season reset"); return true;
                }
                sender.sendMessage("§eStarting season reset... this may take a moment.");
                plugin.getServer().getScheduler().runTaskAsynchronously(plugin, () -> {
                    // SeasonManager is package-private — call via reflection or make it public
                    // For now: manual SQL trigger
                    sender.sendMessage("§aTo trigger a season reset, run: mysql zyrex_ranked < season_reset.sql");
                });
            }
            default -> sender.sendMessage("§cUnknown subcommand.");
        }
        return true;
    }
}
