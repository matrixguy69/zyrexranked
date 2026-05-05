package gg.zyrex.ranked.commands;

import gg.zyrex.ranked.ZyrexRanked;
import gg.zyrex.ranked.models.Match;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

public class SpectateCommand implements CommandExecutor {
    private final ZyrexRanked plugin;
    public SpectateCommand(ZyrexRanked plugin) { this.plugin = plugin; }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("§cPlayers only."); return true; }
        if (args.length == 0) { p.sendMessage("§8[§dZyrex§8] §7Usage: /spectate <player>"); return true; }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null) { p.sendMessage("§8[§dZyrex§8] §cPlayer not found."); return true; }
        Match match = plugin.getMatchManager().getMatchByPlayer(target.getUniqueId());
        if (match == null) { p.sendMessage("§8[§dZyrex§8] §cThat player is not in a match."); return true; }
        if (plugin.getMatchManager().isInMatch(p.getUniqueId())) { p.sendMessage("§8[§dZyrex§8] §cYou are in a match."); return true; }
        match.addSpectator(p.getUniqueId());
        p.setGameMode(GameMode.SPECTATOR);
        p.teleport(target.getLocation());
        p.sendMessage("§8[§dZyrex§8] §7Now spectating §f" + target.getName() + "§7. Use /leavequeue to leave.");
        return true;
    }
}
