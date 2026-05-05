package gg.zyrex.ranked.managers;

import gg.zyrex.ranked.ZyrexRanked;
import gg.zyrex.ranked.models.Gamemode;
import gg.zyrex.ranked.models.Match;
import gg.zyrex.ranked.models.RankedPlayer;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class MatchManager {

    private final ZyrexRanked plugin;
    private final Map<String, Match> activeMatches = new ConcurrentHashMap<>();
    // UUID → matchId
    private final Map<UUID, String> playerMatchMap = new ConcurrentHashMap<>();

    public MatchManager(ZyrexRanked plugin) {
        this.plugin = plugin;
    }

    // ─────────────────────────────────────────────────────────────────
    // CREATE MATCH
    // ─────────────────────────────────────────────────────────────────
    public Match createMatch(UUID p1Uuid, UUID p2Uuid, Gamemode gamemode) {
        RankedPlayer rp1 = plugin.getQueueManager().getCachedPlayer(p1Uuid);
        RankedPlayer rp2 = plugin.getQueueManager().getCachedPlayer(p2Uuid);

        String matchId = UUID.randomUUID().toString();
        Match match = new Match(matchId, p1Uuid, p2Uuid, gamemode,
                rp1 != null ? rp1.getElo(gamemode) : 1000,
                rp2 != null ? rp2.getElo(gamemode) : 1000);

        String arenaName = plugin.getArenaManager().claimArena(gamemode);
        match.setArenaName(arenaName);
        match.setState(Match.MatchState.STARTING);

        activeMatches.put(matchId, match);
        playerMatchMap.put(p1Uuid, matchId);
        playerMatchMap.put(p2Uuid, matchId);

        // Countdown then start
        startCountdown(match);
        return match;
    }

    private void startCountdown(Match match) {
        Player p1 = Bukkit.getPlayer(match.getPlayer1());
        Player p2 = Bukkit.getPlayer(match.getPlayer2());

        // Teleport players to arena
        Location arena1 = plugin.getArenaManager().getSpawn1(match.getArenaName(), match.getGamemode());
        Location arena2 = plugin.getArenaManager().getSpawn2(match.getArenaName(), match.getGamemode());

        if (p1 != null && arena1 != null) p1.teleport(arena1);
        if (p2 != null && arena2 != null) p2.teleport(arena2);

        // Give kits
        if (p1 != null) plugin.getArenaManager().giveKit(p1, match.getGamemode());
        if (p2 != null) plugin.getArenaManager().giveKit(p2, match.getGamemode());

        // Freeze players during countdown
        if (p1 != null) p1.setWalkSpeed(0f);
        if (p2 != null) p2.setWalkSpeed(0f);

        // 5-second countdown
        for (int i = 5; i >= 1; i--) {
            final int count = i;
            Bukkit.getScheduler().runTaskLater(plugin, () -> {
                sendCountdownTitle(match, count);
            }, (5 - i) * 20L);
        }

        // Match start
        Bukkit.getScheduler().runTaskLater(plugin, () -> {
            Player pl1 = Bukkit.getPlayer(match.getPlayer1());
            Player pl2 = Bukkit.getPlayer(match.getPlayer2());
            if (pl1 != null) {
                pl1.setWalkSpeed(0.2f);
                pl1.sendMessage(plugin.msg("messages.match-start"));
            }
            if (pl2 != null) {
                pl2.setWalkSpeed(0.2f);
                pl2.sendMessage(plugin.msg("messages.match-start"));
            }
            match.setState(Match.MatchState.IN_PROGRESS);
        }, 5 * 20L);
    }

    private void sendCountdownTitle(Match match, int count) {
        Player p1 = Bukkit.getPlayer(match.getPlayer1());
        Player p2 = Bukkit.getPlayer(match.getPlayer2());
        Title title = Title.title(
                Component.text(String.valueOf(count), NamedTextColor.YELLOW),
                Component.text(match.getGamemode().getDisplayName(), NamedTextColor.GRAY),
                Title.Times.times(Duration.ZERO, Duration.ofMillis(1100), Duration.ofMillis(200))
        );
        if (p1 != null) p1.showTitle(title);
        if (p2 != null) p2.showTitle(title);
    }

    // ─────────────────────────────────────────────────────────────────
    // END MATCH
    // ─────────────────────────────────────────────────────────────────
    public void endMatch(Match match, Match.MatchResult result) {
        if (match.getState() == Match.MatchState.ENDED) return;
        match.setState(Match.MatchState.ENDED);
        match.setResult(result);

        Gamemode gm = match.getGamemode();
        RankedPlayer rp1 = plugin.getQueueManager().getCachedPlayer(match.getPlayer1());
        RankedPlayer rp2 = plugin.getQueueManager().getCachedPlayer(match.getPlayer2());

        // Calculate ELO
        if (rp1 != null && rp2 != null && result != Match.MatchResult.CANCELLED) {
            int[] deltas = plugin.getEloManager().processMatchResult(match, rp1, rp2);

            // Rank change detection
            String oldRank1 = plugin.getEloManager().getRankNameClean(rp1.getElo(gm) - deltas[0]);
            String newRank1 = plugin.getEloManager().getRankNameClean(rp1.getElo(gm));
            String oldRank2 = plugin.getEloManager().getRankNameClean(rp2.getElo(gm) - deltas[1]);
            String newRank2 = plugin.getEloManager().getRankNameClean(rp2.getElo(gm));

            // Send results to players on main thread
            Bukkit.getScheduler().runTask(plugin, () -> {
                sendMatchResult(match.getPlayer1(), match, deltas[0], rp1.getElo(gm), oldRank1, newRank1);
                sendMatchResult(match.getPlayer2(), match, deltas[1], rp2.getElo(gm), oldRank2, newRank2);

                // Remove spectators
                for (UUID spec : match.getSpectators()) {
                    Player sp = Bukkit.getPlayer(spec);
                    if (sp != null) {
                        sp.setGameMode(GameMode.SURVIVAL);
                        sp.teleport(Bukkit.getWorlds().get(0).getSpawnLocation());
                        sp.sendMessage("§7The match you were spectating has ended.");
                    }
                }

                // Teleport players back to lobby
                teleportToLobby(match.getPlayer1());
                teleportToLobby(match.getPlayer2());
            });

            // Save async
            Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> {
                plugin.getDatabaseManager().savePlayer(rp1);
                plugin.getDatabaseManager().savePlayer(rp2);
                plugin.getDatabaseManager().saveMatch(match);
                plugin.getApiClient().postMatchResult(match, rp1, rp2);
            });
        }

        // Release arena
        plugin.getArenaManager().releaseArena(match.getArenaName(), gm);

        // Cleanup
        activeMatches.remove(match.getMatchId());
        playerMatchMap.remove(match.getPlayer1());
        playerMatchMap.remove(match.getPlayer2());
    }

    private void sendMatchResult(UUID uuid, Match match, int delta, int newElo,
                                  String oldRank, String newRank) {
        Player p = Bukkit.getPlayer(uuid);
        if (p == null) return;

        boolean isP1 = match.getPlayer1().equals(uuid);
        Match.MatchResult result = match.getResult();
        boolean won = (isP1 && result == Match.MatchResult.PLAYER1_WIN)
                   || (!isP1 && result == Match.MatchResult.PLAYER2_WIN);
        boolean draw = result == Match.MatchResult.DRAW;

        String template = won ? "messages.match-end-win" : (draw ? "messages.match-end-draw" : "messages.match-end-loss");
        String msg = plugin.getConfig().getString(template, "")
                .replace("{elo}", String.valueOf(Math.abs(delta)))
                .replace("{new_elo}", String.valueOf(newElo));
        p.sendMessage(plugin.getConfig().getString("messages.prefix", "") + msg);

        // Rank up/down notification
        if (!oldRank.equals(newRank)) {
            if (won) {
                p.sendMessage(plugin.getConfig().getString("messages.rank-up", "")
                        .replace("{rank}", plugin.getEloManager().getRankName(newElo)));
            } else {
                p.sendMessage(plugin.getConfig().getString("messages.rank-down", "")
                        .replace("{rank}", plugin.getEloManager().getRankName(newElo)));
            }
        }

        // Win streak
        RankedPlayer rp = plugin.getQueueManager().getCachedPlayer(uuid);
        if (rp != null && won && rp.getWinStreak(match.getGamemode()) >= 3) {
            p.sendMessage(plugin.getConfig().getString("messages.streak", "")
                    .replace("{streak}", String.valueOf(rp.getWinStreak(match.getGamemode())))
                    .replace("{bonus}", "5"));
        }

        // Title result
        Component titleComp = won
                ? Component.text("VICTORY", NamedTextColor.GREEN)
                : (draw ? Component.text("DRAW", NamedTextColor.YELLOW) : Component.text("DEFEAT", NamedTextColor.RED));
        Component subtitle = Component.text((delta >= 0 ? "+" : "") + delta + " ELO  •  " + newElo + " total", NamedTextColor.GRAY);
        p.showTitle(Title.title(titleComp, subtitle,
                Title.Times.times(Duration.ofMillis(200), Duration.ofSeconds(4), Duration.ofMillis(500))));
    }

    private void teleportToLobby(UUID uuid) {
        Player p = Bukkit.getPlayer(uuid);
        if (p == null) return;
        p.setWalkSpeed(0.2f);
        p.getInventory().clear();
        // Teleport to lobby world spawn
        Location lobby = Bukkit.getWorlds().get(0).getSpawnLocation();
        p.teleport(lobby);
    }

    public void endAllMatchesOnShutdown() {
        for (Match match : activeMatches.values()) {
            if (match.getState() != Match.MatchState.ENDED) {
                endMatch(match, Match.MatchResult.CANCELLED);
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // GETTERS
    // ─────────────────────────────────────────────────────────────────
    public Match getMatchByPlayer(UUID uuid) {
        String matchId = playerMatchMap.get(uuid);
        return matchId != null ? activeMatches.get(matchId) : null;
    }

    public boolean isInMatch(UUID uuid) { return playerMatchMap.containsKey(uuid); }
    public Map<String, Match> getActiveMatches() { return activeMatches; }
    public int getActiveMatchCount() { return activeMatches.size(); }
}
