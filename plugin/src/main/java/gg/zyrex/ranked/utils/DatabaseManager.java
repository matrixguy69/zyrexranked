package gg.zyrex.ranked.utils;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import gg.zyrex.ranked.ZyrexRanked;
import gg.zyrex.ranked.models.Gamemode;
import gg.zyrex.ranked.models.Match;
import gg.zyrex.ranked.models.RankedPlayer;

import java.sql.*;
import java.util.UUID;
import java.util.logging.Level;

public class DatabaseManager {

    private final ZyrexRanked plugin;
    private HikariDataSource dataSource;

    public DatabaseManager(ZyrexRanked plugin) {
        this.plugin = plugin;
    }

    public boolean connect() {
        try {
            HikariConfig config = new HikariConfig();
            config.setJdbcUrl(String.format("jdbc:mysql://%s:%d/%s?useSSL=false&autoReconnect=true&characterEncoding=utf8",
                    plugin.getConfig().getString("database.host"),
                    plugin.getConfig().getInt("database.port"),
                    plugin.getConfig().getString("database.database")));
            config.setUsername(plugin.getConfig().getString("database.username"));
            config.setPassword(plugin.getConfig().getString("database.password"));
            config.setMaximumPoolSize(plugin.getConfig().getInt("database.pool-size", 10));
            config.setMinimumIdle(2);
            config.setConnectionTimeout(30000);
            config.setIdleTimeout(600000);
            config.setMaxLifetime(1800000);
            config.setPoolName("ZyrexRanked-Pool");
            config.addDataSourceProperty("cachePrepStmts", "true");
            config.addDataSourceProperty("prepStmtCacheSize", "250");
            config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
            dataSource = new HikariDataSource(config);
            plugin.getLogger().info("Database connected successfully.");
            return true;
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Database connection failed: " + e.getMessage(), e);
            return false;
        }
    }

    public void disconnect() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    public Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    // ─────────────────────────────────────────────────────────────────
    // TABLE CREATION
    // ─────────────────────────────────────────────────────────────────
    public void createTables() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {

            // Players table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS zyrex_players (
                    uuid VARCHAR(36) PRIMARY KEY,
                    username VARCHAR(16) NOT NULL,
                    global_elo INT DEFAULT 1000,
                    total_wins INT DEFAULT 0,
                    total_losses INT DEFAULT 0,
                    current_season INT DEFAULT 1,
                    last_seen BIGINT DEFAULT 0,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_username (username),
                    INDEX idx_global_elo (global_elo DESC)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """);

            // Per-gamemode stats table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS zyrex_stats (
                    uuid VARCHAR(36) NOT NULL,
                    gamemode VARCHAR(20) NOT NULL,
                    elo INT DEFAULT 1000,
                    wins INT DEFAULT 0,
                    losses INT DEFAULT 0,
                    placement_played INT DEFAULT 0,
                    win_streak INT DEFAULT 0,
                    best_streak INT DEFAULT 0,
                    peak_elo INT DEFAULT 1000,
                    PRIMARY KEY (uuid, gamemode),
                    INDEX idx_elo_gamemode (gamemode, elo DESC),
                    FOREIGN KEY (uuid) REFERENCES zyrex_players(uuid) ON DELETE CASCADE
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """);

            // Match history table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS zyrex_matches (
                    match_id VARCHAR(36) PRIMARY KEY,
                    player1_uuid VARCHAR(36) NOT NULL,
                    player2_uuid VARCHAR(36) NOT NULL,
                    gamemode VARCHAR(20) NOT NULL,
                    result ENUM('PLAYER1_WIN','PLAYER2_WIN','DRAW','CANCELLED') NOT NULL,
                    player1_elo_start INT,
                    player2_elo_start INT,
                    player1_elo_delta INT DEFAULT 0,
                    player2_elo_delta INT DEFAULT 0,
                    duration_seconds INT DEFAULT 0,
                    arena_name VARCHAR(64),
                    season INT DEFAULT 1,
                    played_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    INDEX idx_p1 (player1_uuid),
                    INDEX idx_p2 (player2_uuid),
                    INDEX idx_gamemode (gamemode),
                    INDEX idx_played_at (played_at DESC)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """);

            // Season history table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS zyrex_season_history (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    uuid VARCHAR(36) NOT NULL,
                    season INT NOT NULL,
                    gamemode VARCHAR(20) NOT NULL,
                    final_elo INT,
                    rank_name VARCHAR(32),
                    wins INT DEFAULT 0,
                    losses INT DEFAULT 0,
                    peak_elo INT DEFAULT 1000,
                    INDEX idx_uuid_season (uuid, season)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
            """);

            plugin.getLogger().info("Database tables created/verified.");
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create tables: " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // PLAYER OPERATIONS
    // ─────────────────────────────────────────────────────────────────
    public RankedPlayer loadPlayer(UUID uuid) {
        try (Connection conn = getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "SELECT * FROM zyrex_players WHERE uuid = ?");
            ps.setString(1, uuid.toString());
            ResultSet rs = ps.executeQuery();

            if (!rs.next()) return null;

            RankedPlayer player = new RankedPlayer(uuid, rs.getString("username"));
            player.setCurrentSeason(rs.getInt("current_season"));
            player.setLastSeen(rs.getLong("last_seen"));

            // Load per-gamemode stats
            PreparedStatement statsPs = conn.prepareStatement(
                "SELECT * FROM zyrex_stats WHERE uuid = ?");
            statsPs.setString(1, uuid.toString());
            ResultSet statsRs = statsPs.executeQuery();

            while (statsRs.next()) {
                Gamemode gm = Gamemode.fromId(statsRs.getString("gamemode"));
                if (gm == null) continue;
                player.setElo(gm, statsRs.getInt("elo"));
                for (int i = 0; i < statsRs.getInt("wins"); i++) player.addWin(gm);
                for (int i = 0; i < statsRs.getInt("losses"); i++) player.addLoss(gm);
            }
            return player;
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load player: " + e.getMessage(), e);
            return null;
        }
    }

    public void savePlayer(RankedPlayer player) {
        try (Connection conn = getConnection()) {
            // Upsert main player record
            PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO zyrex_players (uuid, username, global_elo, total_wins, total_losses, current_season, last_seen)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON DUPLICATE KEY UPDATE
                    username=VALUES(username), global_elo=VALUES(global_elo),
                    total_wins=VALUES(total_wins), total_losses=VALUES(total_losses),
                    current_season=VALUES(current_season), last_seen=VALUES(last_seen)
            """);
            ps.setString(1, player.getUuid().toString());
            ps.setString(2, player.getUsername());
            ps.setInt(3, player.getGlobalElo());
            ps.setInt(4, player.getTotalWins());
            ps.setInt(5, player.getTotalLosses());
            ps.setInt(6, player.getCurrentSeason());
            ps.setLong(7, System.currentTimeMillis());
            ps.executeUpdate();

            // Upsert per-gamemode stats
            for (Gamemode gm : Gamemode.values()) {
                PreparedStatement sps = conn.prepareStatement("""
                    INSERT INTO zyrex_stats (uuid, gamemode, elo, wins, losses, placement_played, win_streak, best_streak, peak_elo)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON DUPLICATE KEY UPDATE
                        elo=VALUES(elo), wins=VALUES(wins), losses=VALUES(losses),
                        placement_played=VALUES(placement_played), win_streak=VALUES(win_streak),
                        best_streak=VALUES(best_streak), peak_elo=VALUES(peak_elo)
                """);
                sps.setString(1, player.getUuid().toString());
                sps.setString(2, gm.getId());
                sps.setInt(3, player.getElo(gm));
                sps.setInt(4, player.getWins(gm));
                sps.setInt(5, player.getLosses(gm));
                sps.setInt(6, player.getPlacementMatchesPlayed(gm));
                sps.setInt(7, player.getWinStreak(gm));
                sps.setInt(8, player.getBestStreak(gm));
                sps.setInt(9, player.getPeakElo(gm));
                sps.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save player: " + e.getMessage(), e);
        }
    }

    public void createPlayerIfNotExists(UUID uuid, String username) {
        try (Connection conn = getConnection()) {
            PreparedStatement ps = conn.prepareStatement(
                "INSERT IGNORE INTO zyrex_players (uuid, username) VALUES (?, ?)");
            ps.setString(1, uuid.toString());
            ps.setString(2, username);
            ps.executeUpdate();

            for (Gamemode gm : Gamemode.values()) {
                PreparedStatement sps = conn.prepareStatement(
                    "INSERT IGNORE INTO zyrex_stats (uuid, gamemode) VALUES (?, ?)");
                sps.setString(1, uuid.toString());
                sps.setString(2, gm.getId());
                sps.executeUpdate();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to create player: " + e.getMessage(), e);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // MATCH OPERATIONS
    // ─────────────────────────────────────────────────────────────────
    public void saveMatch(Match match) {
        try (Connection conn = getConnection()) {
            PreparedStatement ps = conn.prepareStatement("""
                INSERT INTO zyrex_matches
                (match_id, player1_uuid, player2_uuid, gamemode, result,
                 player1_elo_start, player2_elo_start, player1_elo_delta, player2_elo_delta,
                 duration_seconds, arena_name, season)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            """);
            ps.setString(1, match.getMatchId());
            ps.setString(2, match.getPlayer1().toString());
            ps.setString(3, match.getPlayer2().toString());
            ps.setString(4, match.getGamemode().getId());
            ps.setString(5, match.getResult().name());
            ps.setInt(6, match.getPlayer1EloStart());
            ps.setInt(7, match.getPlayer2EloStart());
            ps.setInt(8, match.getPlayer1EloDelta());
            ps.setInt(9, match.getPlayer2EloDelta());
            ps.setLong(10, match.getDurationSeconds());
            ps.setString(11, match.getArenaName());
            ps.setInt(12, plugin.getConfig().getInt("seasons.current-season", 1));
            ps.executeUpdate();
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to save match: " + e.getMessage(), e);
        }
    }
}
