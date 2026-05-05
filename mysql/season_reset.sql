-- ============================================================
--  Zyrex Ranked — Season Reset Script
--  Usage: mysql -u zyrex -p zyrex_ranked < season_reset.sql
--  Run at the END of each season (every ~3 months)
-- ============================================================

-- Step 1: Archive current season stats
INSERT INTO zyrex_season_history (uuid, season, gamemode, final_elo, rank_name, wins, losses, peak_elo)
SELECT
    s.uuid,
    (SELECT MAX(current_season) FROM zyrex_players LIMIT 1),
    s.gamemode,
    s.elo,
    CASE
        WHEN s.elo >= 2950 THEN 'Champion'
        WHEN s.elo >= 2800 THEN 'Netherite III'
        WHEN s.elo >= 2600 THEN 'Netherite II'
        WHEN s.elo >= 2400 THEN 'Netherite I'
        WHEN s.elo >= 2300 THEN 'Diamond III'
        WHEN s.elo >= 2100 THEN 'Diamond II'
        WHEN s.elo >= 1900 THEN 'Diamond I'
        WHEN s.elo >= 1700 THEN 'Platinum'
        WHEN s.elo >= 1500 THEN 'Gold'
        WHEN s.elo >= 1300 THEN 'Silver'
        WHEN s.elo >= 1100 THEN 'Bronze'
        ELSE 'Iron'
    END,
    s.wins,
    s.losses,
    s.peak_elo
FROM zyrex_stats s;

-- Step 2: Soft reset ELO (compress 30% toward 1000)
UPDATE zyrex_stats
SET
    elo          = GREATEST(100, CAST(elo - (elo - 1000) * 0.30 AS SIGNED)),
    peak_elo     = GREATEST(100, CAST(elo - (elo - 1000) * 0.30 AS SIGNED)),
    wins         = 0,
    losses       = 0,
    win_streak   = 0,
    best_streak  = 0,
    placement_played = 0;

-- Step 3: Recalc global ELO
UPDATE zyrex_players p
SET
    global_elo   = (SELECT AVG(elo) FROM zyrex_stats WHERE uuid = p.uuid),
    total_wins   = 0,
    total_losses = 0,
    current_season = current_season + 1;

SELECT CONCAT('Season reset complete. Now in Season ', MAX(current_season)) AS result
FROM zyrex_players;
