const express = require('express');
const router = express.Router();
const { getPool } = require('../utils/db');
const { cacheGet, cacheSet } = require('../utils/cache');

/**
 * GET /api/player/:username
 * Full player profile with per-gamemode stats
 */
router.get('/:username', async (req, res) => {
  try {
    const { username } = req.params;
    const cacheKey = `player:${username.toLowerCase()}`;
    const cached = await cacheGet(cacheKey);
    if (cached) return res.json(cached);

    const pool = getPool();
    const [players] = await pool.query(
      'SELECT * FROM zyrex_players WHERE LOWER(username) = LOWER(?)', [username]
    );

    if (!players.length) return res.status(404).json({ error: 'Player not found' });
    const player = players[0];

    // Per-gamemode stats
    const [stats] = await pool.query(
      'SELECT * FROM zyrex_stats WHERE uuid = ?', [player.uuid]
    );

    // Recent match history (last 20)
    const [matches] = await pool.query(`
      SELECT 
        m.*,
        CASE WHEN m.player1_uuid = ? THEN p2.username ELSE p1.username END as opponent_name,
        CASE WHEN m.player1_uuid = ? THEN m.player1_elo_delta ELSE m.player2_elo_delta END as my_delta,
        CASE WHEN m.player1_uuid = ? THEN m.player1_elo_start ELSE m.player2_elo_start END as my_elo_start,
        CASE
          WHEN (m.player1_uuid = ? AND m.result = 'PLAYER1_WIN') OR (m.player2_uuid = ? AND m.result = 'PLAYER2_WIN') THEN 'WIN'
          WHEN m.result = 'DRAW' THEN 'DRAW'
          WHEN m.result = 'CANCELLED' THEN 'CANCELLED'
          ELSE 'LOSS'
        END as my_result
      FROM zyrex_matches m
      JOIN zyrex_players p1 ON m.player1_uuid = p1.uuid
      JOIN zyrex_players p2 ON m.player2_uuid = p2.uuid
      WHERE m.player1_uuid = ? OR m.player2_uuid = ?
      ORDER BY m.played_at DESC
      LIMIT 20
    `, [player.uuid, player.uuid, player.uuid, player.uuid, player.uuid, player.uuid, player.uuid]);

    // Season history
    const [seasons] = await pool.query(
      'SELECT * FROM zyrex_season_history WHERE uuid = ? ORDER BY season DESC', [player.uuid]
    );

    // Build per-gamemode map
    const gamemodeStats = {};
    for (const stat of stats) {
      gamemodeStats[stat.gamemode] = {
        elo: stat.elo,
        wins: stat.wins,
        losses: stat.losses,
        peakElo: stat.peak_elo,
        bestStreak: stat.best_streak,
        winStreak: stat.win_streak,
        placementPlayed: stat.placement_played,
        wlRatio: stat.losses === 0 ? stat.wins : (stat.wins / stat.losses).toFixed(2),
        rank: getRankName(stat.elo),
        rankTier: getRankTier(stat.elo),
      };
    }

    const response = {
      uuid: player.uuid,
      username: player.username,
      globalElo: player.global_elo,
      globalRank: getRankName(player.global_elo),
      totalWins: player.total_wins,
      totalLosses: player.total_losses,
      currentSeason: player.current_season,
      lastSeen: player.last_seen,
      createdAt: player.created_at,
      avatarUrl: `https://crafatar.com/avatars/${player.uuid}?size=128&overlay`,
      bustUrl: `https://visage.surgeplay.com/bust/256/${player.username}`,
      gamemodes: gamemodeStats,
      recentMatches: matches.map(m => ({
        matchId: m.match_id,
        gamemode: m.gamemode,
        opponent: m.opponent_name,
        result: m.my_result,
        eloDelta: m.my_delta,
        eloStart: m.my_elo_start,
        durationSeconds: m.duration_seconds,
        playedAt: m.played_at,
      })),
      seasonHistory: seasons,
    };

    await cacheSet(cacheKey, response, 60);
    res.json(response);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

/**
 * GET /api/player/:username/matches?gamemode=smp&page=1
 */
router.get('/:username/matches', async (req, res) => {
  try {
    const { username } = req.params;
    const gamemode = req.query.gamemode || null;
    const page = Math.max(1, parseInt(req.query.page) || 1);
    const limit = 20;
    const offset = (page - 1) * limit;

    const pool = getPool();
    const [players] = await pool.query('SELECT uuid FROM zyrex_players WHERE LOWER(username)=LOWER(?)', [username]);
    if (!players.length) return res.status(404).json({ error: 'Player not found' });
    const { uuid } = players[0];

    let whereExtra = '';
    const params = [uuid, uuid, uuid, uuid, uuid, uuid, uuid];
    if (gamemode) {
      whereExtra = ' AND m.gamemode = ?';
      params.push(gamemode);
    }
    params.push(limit, offset);

    const [matches] = await pool.query(`
      SELECT 
        m.*,
        CASE WHEN m.player1_uuid = ? THEN p2.username ELSE p1.username END as opponent_name,
        CASE WHEN m.player1_uuid = ? THEN m.player1_elo_delta ELSE m.player2_elo_delta END as my_delta,
        CASE WHEN m.player1_uuid = ? THEN m.player1_elo_start ELSE m.player2_elo_start END as my_elo_start,
        CASE
          WHEN (m.player1_uuid = ? AND m.result = 'PLAYER1_WIN') OR (m.player2_uuid = ? AND m.result = 'PLAYER2_WIN') THEN 'WIN'
          WHEN m.result = 'DRAW' THEN 'DRAW'
          WHEN m.result = 'CANCELLED' THEN 'CANCELLED'
          ELSE 'LOSS'
        END as my_result
      FROM zyrex_matches m
      JOIN zyrex_players p1 ON m.player1_uuid = p1.uuid
      JOIN zyrex_players p2 ON m.player2_uuid = p2.uuid
      WHERE (m.player1_uuid = ? OR m.player2_uuid = ?)${whereExtra}
      ORDER BY m.played_at DESC
      LIMIT ? OFFSET ?
    `, params);

    res.json({ matches: matches.map(m => ({
      matchId: m.match_id,
      gamemode: m.gamemode,
      opponent: m.opponent_name,
      result: m.my_result,
      eloDelta: m.my_delta,
      eloStart: m.my_elo_start,
      durationSeconds: m.duration_seconds,
      playedAt: m.played_at,
    })) });
  } catch (err) {
    res.status(500).json({ error: 'Internal server error' });
  }
});

function getRankName(elo) {
  if (elo >= 2950) return 'Champion';
  if (elo >= 2800) return 'Netherite III';
  if (elo >= 2600) return 'Netherite II';
  if (elo >= 2400) return 'Netherite I';
  if (elo >= 2300) return 'Diamond III';
  if (elo >= 2100) return 'Diamond II';
  if (elo >= 1900) return 'Diamond I';
  if (elo >= 1700) return 'Platinum';
  if (elo >= 1500) return 'Gold';
  if (elo >= 1300) return 'Silver';
  if (elo >= 1100) return 'Bronze';
  return 'Iron';
}

function getRankTier(elo) {
  if (elo >= 2950) return 'champion';
  if (elo >= 2400) return 'netherite';
  if (elo >= 1900) return 'diamond';
  if (elo >= 1700) return 'platinum';
  if (elo >= 1500) return 'gold';
  if (elo >= 1300) return 'silver';
  if (elo >= 1100) return 'bronze';
  return 'iron';
}

module.exports = router;
