const express = require('express');
const router = express.Router();
const { getPool } = require('../utils/db');
const { cacheGet, cacheSet } = require('../utils/cache');

const VALID_GAMEMODES = ['global', 'smp', 'pot', 'uhc', 'sword', 'spearmace'];
const PAGE_SIZE = parseInt(process.env.LEADERBOARD_PAGE_SIZE) || 50;

/**
 * GET /api/leaderboard
 * GET /api/leaderboard?gamemode=smp&page=1&season=1
 */
router.get('/', async (req, res) => {
  try {
    const gamemode = req.query.gamemode || 'global';
    const page = Math.max(1, parseInt(req.query.page) || 1);
    const season = parseInt(req.query.season) || null;
    const offset = (page - 1) * PAGE_SIZE;

    if (!VALID_GAMEMODES.includes(gamemode)) {
      return res.status(400).json({ error: 'Invalid gamemode', valid: VALID_GAMEMODES });
    }

    const cacheKey = `leaderboard:${gamemode}:${season || 'current'}:${page}`;
    const cached = await cacheGet(cacheKey);
    if (cached) return res.json(cached);

    const pool = getPool();
    let rows, total;

    if (gamemode === 'global') {
      // Global = average ELO across all gamemodes
      const [countRow] = await pool.query(
        'SELECT COUNT(*) as total FROM zyrex_players WHERE total_wins + total_losses > 0'
      );
      total = countRow[0].total;

      [rows] = await pool.query(`
        SELECT 
          p.uuid, p.username, p.global_elo as elo,
          p.total_wins as wins, p.total_losses as losses,
          p.last_seen
        FROM zyrex_players p
        WHERE p.total_wins + p.total_losses > 0
        ORDER BY p.global_elo DESC
        LIMIT ? OFFSET ?
      `, [PAGE_SIZE, offset]);
    } else {
      // Per-gamemode leaderboard
      const [countRow] = await pool.query(
        'SELECT COUNT(*) as total FROM zyrex_stats WHERE gamemode = ? AND wins + losses > 0',
        [gamemode]
      );
      total = countRow[0].total;

      [rows] = await pool.query(`
        SELECT 
          s.uuid, p.username, s.elo, s.wins, s.losses,
          s.peak_elo, s.best_streak, s.win_streak,
          p.last_seen
        FROM zyrex_stats s
        JOIN zyrex_players p ON s.uuid = p.uuid
        WHERE s.gamemode = ? AND s.wins + s.losses > 0
        ORDER BY s.elo DESC
        LIMIT ? OFFSET ?
      `, [gamemode, PAGE_SIZE, offset]);
    }

    // Add rank position and tier name
    const result = rows.map((row, index) => ({
      position: offset + index + 1,
      uuid: row.uuid,
      username: row.username,
      elo: row.elo,
      wins: row.wins,
      losses: row.losses,
      wlRatio: row.losses === 0 ? row.wins : (row.wins / row.losses).toFixed(2),
      peakElo: row.peak_elo || row.elo,
      bestStreak: row.best_streak || 0,
      winStreak: row.win_streak || 0,
      rank: getRankName(row.elo),
      rankTier: getRankTier(row.elo),
      lastSeen: row.last_seen,
      avatarUrl: `https://crafatar.com/avatars/${row.uuid}?size=64&overlay`,
      bustUrl: `https://visage.surgeplay.com/bust/128/${row.username}`,
    }));

    const response = {
      gamemode,
      page,
      pageSize: PAGE_SIZE,
      totalPlayers: total,
      totalPages: Math.ceil(total / PAGE_SIZE),
      players: result,
    };

    await cacheSet(cacheKey, response, 30);
    res.json(response);
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

/**
 * GET /api/leaderboard/top/:gamemode
 * Returns top 10 for quick display
 */
router.get('/top/:gamemode', async (req, res) => {
  try {
    const { gamemode } = req.params;
    if (!VALID_GAMEMODES.includes(gamemode)) {
      return res.status(400).json({ error: 'Invalid gamemode' });
    }

    const cacheKey = `leaderboard:top10:${gamemode}`;
    const cached = await cacheGet(cacheKey);
    if (cached) return res.json(cached);

    const pool = getPool();
    let rows;

    if (gamemode === 'global') {
      [rows] = await pool.query(`
        SELECT uuid, username, global_elo as elo, total_wins as wins, total_losses as losses
        FROM zyrex_players
        ORDER BY global_elo DESC
        LIMIT 10
      `);
    } else {
      [rows] = await pool.query(`
        SELECT s.uuid, p.username, s.elo, s.wins, s.losses, s.peak_elo
        FROM zyrex_stats s
        JOIN zyrex_players p ON s.uuid = p.uuid
        WHERE s.gamemode = ?
        ORDER BY s.elo DESC
        LIMIT 10
      `, [gamemode]);
    }

    const result = rows.map((row, i) => ({
      position: i + 1,
      uuid: row.uuid,
      username: row.username,
      elo: row.elo,
      wins: row.wins,
      losses: row.losses,
      rank: getRankName(row.elo),
      rankTier: getRankTier(row.elo),
      bustUrl: `https://visage.surgeplay.com/bust/128/${row.username}`,
    }));

    await cacheSet(cacheKey, result, 30);
    res.json(result);
  } catch (err) {
    res.status(500).json({ error: 'Internal server error' });
  }
});

// ─────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────
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
