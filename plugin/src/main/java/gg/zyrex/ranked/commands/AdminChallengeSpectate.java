package gg.zyrex.ranked.commands;

import gg.zyrex.ranked.ZyrexRanked;
import gg.zyrex.ranked.models.Gamemode;
import gg.zyrex.ranked.models.Match;
import gg.zyrex.ranked.models.RankedPlayer;
import org.bukkit.*;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

// ─────────────────────────────────────────────────────────────────────────────
// Admin command
// ─────────────────────────────────────────────────────────────────────────────
class RankedAdminCommand implements CommandExecutor {
    private final ZyrexRanked plugin;
    public RankedAdminCommand(ZyrexRanked plugin) { this.plugin = plugin; }
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!sender.hasPermission("zyrex.ranked.admin")) {
            sender.sendMessage("§cNo permission."); return true;
        }
        if (args.length == 0) {
            sender.sendMessage("§8[§dZyrexAdmin§8] §7Subcommands: setelo, resetelo, endmatch, reload, season");
            return true;
        }
        switch (args[0].toLowerCase()) {
            case "setelo" -> {
                // /rankedadmin setelo <player> <gamemode> <elo>
                if (args.length < 4) { sender.sendMessage("§cUsage: /rankedadmin setelo <player> <gamemode> <elo>"); return true; }
                Player target = Bukkit.getPlayerExact(args[1]);
                if (target == null) { sender.sendMessage("§cPlayer not found."); return true; }
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
                if (target == null) { sender.sendMessage("§cPlayer not found."); return true; }
                Match match = plugin.getMatchManager().getMatchByPlayer(target.getUniqueId());
                if (match == null) { sender.sendMessage("§cPlayer is not in a match."); return true; }
                plugin.getMatchManager().endMatch(match, Match.MatchResult.CANCELLED);
                sender.sendMessage("§aMatch ended and cancelled.");
            }
            default -> sender.sendMessage("§cUnknown subcommand.");
        }
        return true;
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Challenge command
// ─────────────────────────────────────────────────────────────────────────────
class ChallengeCommand implements CommandExecutor {
    private final ZyrexRanked plugin;
    public ChallengeCommand(ZyrexRanked plugin) { this.plugin = plugin; }
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) { sender.sendMessage("§cPlayers only."); return true; }
        if (args.length < 2) { p.sendMessage("§8[§dZyrex§8] §7Usage: /challenge <player> <gamemode>"); return true; }
        Player target = Bukkit.getPlayerExact(args[0]);
        if (target == null || target.equals(p)) { p.sendMessage("§8[§dZyrex§8] §cInvalid player."); return true; }
        Gamemode gm = Gamemode.fromId(args[1]);
        if (gm == null) { p.sendMessage("§8[§dZyrex§8] §cUnknown gamemode. Available: " + Gamemode.listAll()); return true; }
        if (plugin.getMatchManager().isInMatch(p.getUniqueId()) || plugin.getMatchManager().isInMatch(target.getUniqueId())) {
            p.sendMessage("§8[§dZyrex§8] §cOne of you is already in a match."); return true;
        }
        // For simplicity, directly create match (can add accept/deny flow)
        plugin.getMatchManager().createMatch(p.getUniqueId(), target.getUniqueId(), gm);
        p.sendMessage("§8[§dZyrex§8] §aChallenge sent to §f" + target.getName() + "§a!");
        target.sendMessage("§8[§dZyrex§8] §f" + p.getName() + " §achallenged you to a ranked §f" + gm.getDisplayName() + " §amatch!");
        return true;
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Spectate command
// ─────────────────────────────────────────────────────────────────────────────
class SpectateCommand implements CommandExecutor {
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
        p.sendMessage("§8[§dZyrex§8] §7Spectating §f" + target.getName() + "§7's match. Use /leavequeue to leave.");
        return true;
    }
}
