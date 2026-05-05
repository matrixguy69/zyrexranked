package gg.zyrex.ranked.managers;

import gg.zyrex.ranked.ZyrexRanked;
import gg.zyrex.ranked.models.Gamemode;
import gg.zyrex.ranked.models.RankedPlayer;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class QueueManager {

    private final ZyrexRanked plugin;

    // Gamemode → list of queued entries
    private final Map<Gamemode, List<QueueEntry>> queues = new ConcurrentHashMap<>();

    // UUID → cached RankedPlayer (loaded on join, kept in memory)
    private final Map<UUID, RankedPlayer> playerCache = new ConcurrentHashMap<>();

    // UUID → which gamemode queue they're in
    private final Map<UUID, Gamemode> playerQueueMap = new ConcurrentHashMap<>();

    public QueueManager(ZyrexRanked plugin) {
        this.plugin = plugin;
        for (Gamemode gm : Gamemode.values()) {
            queues.put(gm, Collections.synchronizedList(new ArrayList<>()));
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // QUEUE ENTRY
    // ─────────────────────────────────────────────────────────────────
    public static class QueueEntry {
        public final UUID uuid;
        public final int elo;
        public final long joinTime;
        public int currentRange;

        public QueueEntry(UUID uuid, int elo, int initialRange) {
            this.uuid = uuid;
            this.elo = elo;
            this.joinTime = System.currentTimeMillis();
            this.currentRange = initialRange;
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // JOIN / LEAVE
    // ─────────────────────────────────────────────────────────────────
    public boolean joinQueue(UUID uuid, Gamemode gamemode) {
        if (isInQueue(uuid)) return false;
        if (plugin.getMatchManager().isInMatch(uuid)) return false;

        RankedPlayer rp = getCachedPlayer(uuid);
        if (rp == null) return false;

        int initialRange = plugin.getConfig().getInt("queue.initial-elo-range", 100);
        QueueEntry entry = new QueueEntry(uuid, rp.getElo(gamemode), initialRange);

        queues.get(gamemode).add(entry);
        playerQueueMap.put(uuid, gamemode);

        Player p = Bukkit.getPlayer(uuid);
        if (p != null) {
            String msg = plugin.getConfig().getString("messages.queue-join", "")
                    .replace("{gamemode}", gamemode.getDisplayName())
                    .replace("{position}", String.valueOf(getQueueSize(gamemode)));
            p.sendMessage(plugin.getConfig().getString("messages.prefix", "") + msg);
        }
        return true;
    }

    public void leaveQueue(UUID uuid) {
        Gamemode gm = playerQueueMap.remove(uuid);
        if (gm != null) {
            queues.get(gm).removeIf(e -> e.uuid.equals(uuid));
            Player p = Bukkit.getPlayer(uuid);
            if (p != null) {
                p.sendMessage(plugin.getConfig().getString("messages.prefix", "")
                        + plugin.getConfig().getString("messages.queue-leave", ""));
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // MATCHMAKING LOOP (called every 2s from scheduler)
    // ─────────────────────────────────────────────────────────────────
    public void processQueue() {
        long expandInterval = plugin.getConfig().getLong("queue.expand-interval", 60) * 1000;
        int expansion = plugin.getConfig().getInt("queue.elo-range-expansion", 50);
        int maxRange = plugin.getConfig().getInt("queue.max-elo-range", 500);

        for (Gamemode gm : Gamemode.values()) {
            List<QueueEntry> queue = queues.get(gm);
            if (queue.size() < 2) continue;

            // Expand ELO ranges for long-waiting players
            long now = System.currentTimeMillis();
            synchronized (queue) {
                for (QueueEntry e : queue) {
                    long waited = now - e.joinTime;
                    int expansions = (int) (waited / expandInterval);
                    e.currentRange = Math.min(
                            plugin.getConfig().getInt("queue.initial-elo-range", 100) + expansions * expansion,
                            maxRange
                    );
                }
            }

            // Try to match pairs
            List<QueueEntry> snapshot;
            synchronized (queue) {
                snapshot = new ArrayList<>(queue);
            }

            Set<UUID> matched = new HashSet<>();
            for (int i = 0; i < snapshot.size(); i++) {
                if (matched.contains(snapshot.get(i).uuid)) continue;
                QueueEntry a = snapshot.get(i);
                Player pa = Bukkit.getPlayer(a.uuid);
                if (pa == null) { leaveQueue(a.uuid); continue; }

                for (int j = i + 1; j < snapshot.size(); j++) {
                    QueueEntry b = snapshot.get(j);
                    if (matched.contains(b.uuid)) continue;
                    Player pb = Bukkit.getPlayer(b.uuid);
                    if (pb == null) { leaveQueue(b.uuid); continue; }

                    int range = Math.max(a.currentRange, b.currentRange);
                    if (Math.abs(a.elo - b.elo) <= range) {
                        // Match found!
                        matched.add(a.uuid);
                        matched.add(b.uuid);

                        queue.removeIf(e -> e.uuid.equals(a.uuid) || e.uuid.equals(b.uuid));
                        playerQueueMap.remove(a.uuid);
                        playerQueueMap.remove(b.uuid);

                        final Gamemode finalGm = gm;
                        Bukkit.getScheduler().runTask(plugin, () -> {
                            pa.sendMessage(plugin.getConfig().getString("messages.prefix", "")
                                    + plugin.getConfig().getString("messages.match-found", ""));
                            pb.sendMessage(plugin.getConfig().getString("messages.prefix", "")
                                    + plugin.getConfig().getString("messages.match-found", ""));
                            plugin.getMatchManager().createMatch(a.uuid, b.uuid, finalGm);
                        });
                        break;
                    }
                }
            }
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // PLAYER CACHE
    // ─────────────────────────────────────────────────────────────────
    public void cachePlayer(UUID uuid, RankedPlayer player) {
        playerCache.put(uuid, player);
    }

    public RankedPlayer getCachedPlayer(UUID uuid) {
        return playerCache.get(uuid);
    }

    public void uncachePlayer(UUID uuid) {
        playerCache.remove(uuid);
    }

    // ─────────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────────
    public boolean isInQueue(UUID uuid) { return playerQueueMap.containsKey(uuid); }
    public int getQueueSize(Gamemode gm) { return queues.get(gm).size(); }
    public Gamemode getQueueGamemode(UUID uuid) { return playerQueueMap.get(uuid); }
    public Map<UUID, RankedPlayer> getPlayerCache() { return playerCache; }
}
