package gg.zyrex.ranked.commands;

import gg.zyrex.ranked.ZyrexRanked;
import gg.zyrex.ranked.models.Gamemode;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class ChallengeCommand implements CommandExecutor {
    private final ZyrexRanked plugin;
    public ChallengeCommand(ZyrexRanked plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("§cPlayers only."); return true; }
        if (args.length < 2) { p.sendMessage("§8[§dZyrex§8] §7Usage: /challenge <player> <gamemode>"); return true; }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || target.equals(p)) { p.sendMessage("§8[§dZyrex§8] §cInvalid player."); return true; }
        Gamemode gm = Gamemode.fromId(args[1]);
        if (gm == null) { p.sendMessage("§8[§dZyrex§8] §cUnknown gamemode. Use: " + Gamemode.listAll()); return true; }
        if (plugin.getMatchManager().isInMatch(p.getUniqueId()) || plugin.getMatchManager().isInMatch(target.getUniqueId())) {
            p.sendMessage("§8[§dZyrex§8] §cOne of you is already in a match."); return true;
        }
        plugin.getMatchManager().createMatch(p.getUniqueId(), target.getUniqueId(), gm);
        p.sendMessage("§8[§dZyrex§8] §aChallenge sent to §f" + target.getName() + "§a!");
        target.sendMessage("§8[§dZyrex§8] §f" + p.getName() + " §achallenged you! §7(" + gm.getDisplayName() + ")");
        return true;
    }
}
