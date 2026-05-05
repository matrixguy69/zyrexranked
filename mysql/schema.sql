-- ============================================================
--  Zyrex Ranked — MySQL Schema
--  Run once: mysql -u root -p zyrex_ranked < schema.sql
-- ============================================================

CREATE DATABASE IF NOT EXISTS zyrex_ranked CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE zyrex_ranked;

-- Players master table
CREATE TABLE IF NOT EXISTS zyrex_players (
    uuid          VARCHAR(36)  PRIMARY KEY,
    username      VARCHAR(16)  NOT NULL,
    global_elo    INT          DEFAULT 1000,
    total_wins    INT          DEFAULT 0,
    total_losses  INT          DEFAULT 0,
    current_season INT         DEFAULT 1,
    last_seen     BIGINT       DEFAULT 0,
    created_at    TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_username   (username),
    INDEX idx_global_elo (global_elo DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Per-gamemode stats
CREATE TABLE IF NOT EXISTS zyrex_stats (
    uuid              VARCHAR(36)  NOT NULL,
    gamemode          VARCHAR(20)  NOT NULL,
    elo               INT          DEFAULT 1000,
    wins              INT          DEFAULT 0,
    losses            INT          DEFAULT 0,
    placement_played  INT          DEFAULT 0,
    win_streak        INT          DEFAULT 0,
    best_streak       INT          DEFAULT 0,
    peak_elo          INT          DEFAULT 1000,
    PRIMARY KEY (uuid, gamemode),
    INDEX idx_elo_gamemode (gamemode, elo DESC),
    FOREIGN KEY (uuid) REFERENCES zyrex_players(uuid) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Match history
CREATE TABLE IF NOT EXISTS zyrex_matches (
    match_id          VARCHAR(36)  PRIMARY KEY,
    player1_uuid      VARCHAR(36)  NOT NULL,
    player2_uuid      VARCHAR(36)  NOT NULL,
    gamemode          VARCHAR(20)  NOT NULL,
    result            ENUM('PLAYER1_WIN','PLAYER2_WIN','DRAW','CANCELLED') NOT NULL,
    player1_elo_start INT,
    player2_elo_start INT,
    player1_elo_delta INT          DEFAULT 0,
    player2_elo_delta INT          DEFAULT 0,
    duration_seconds  INT          DEFAULT 0,
    arena_name        VARCHAR(64),
    season            INT          DEFAULT 1,
    played_at         TIMESTAMP    DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_p1        (player1_uuid),
    INDEX idx_p2        (player2_uuid),
    INDEX idx_gamemode  (gamemode),
    INDEX idx_played_at (played_at DESC)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- Season history (archived each reset)
CREATE TABLE IF NOT EXISTS zyrex_season_history (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    uuid        VARCHAR(36)  NOT NULL,
    season      INT          NOT NULL,
    gamemode    VARCHAR(20)  NOT NULL,
    final_elo   INT,
    rank_name   VARCHAR(32),
    wins        INT          DEFAULT 0,
    losses      INT          DEFAULT 0,
    peak_elo    INT          DEFAULT 1000,
    INDEX idx_uuid_season (uuid, season)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- ── Create a dedicated DB user (recommended) ─────────────────────────────────
-- Run these lines as root MySQL user:
-- CREATE USER 'zyrex'@'localhost' IDENTIFIED BY 'STRONG_PASSWORD_HERE';
-- GRANT SELECT, INSERT, UPDATE, DELETE ON zyrex_ranked.* TO 'zyrex'@'localhost';
-- FLUSH PRIVILEGES;
