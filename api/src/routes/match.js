const express = require('express');
const router = express.Router();
const { getPool } = require('../utils/db');
const { cacheDel } = require('../utils/cache');
const { pluginAuth } = require('../middleware/auth');

/**
 * POST /api/match/report
 * Called by the Minecraft plugin after every match
 */
router.post('/report', pluginAuth, async (req, res) => {
  try {
    const { matchId, gamemode, result, durationSeconds, season, player1, player2 } = req.body;

    if (!matchId || !gamemode || !result || !player1 || !player2) {
      return res.status(400).json({ error: 'Missing required fields' });
    }

    const pool = getPool();

    // Upsert both players
    for (const p of [player1, player2]) {
      await pool.query(`
        INSERT INTO zyrex_players (uuid, username, global_elo, total_wins, total_losses)
        VALUES (?, ?, 1000, 0, 0)
        ON DUPLICATE KEY UPDATE username=VALUES(username)
      `, [p.uuid, p.username]);

      // Update stats
      const wins = p.uuid === player1.uuid && result === 'PLAYER1_WIN' ? 1
                 : p.uuid === player2.uuid && result === 'PLAYER2_WIN' ? 1 : 0;
      const losses = !wins && result !== 'DRAW' && result !== 'CANCELLED' ? 1 : 0;

      await pool.query(`
        INSERT INTO zyrex_stats (uuid, gamemode, elo, wins, losses)
        VALUES (?, ?, ?, ?, ?)
        ON DUPLICATE KEY UPDATE
          elo=VALUES(elo),
          wins=wins + ${wins},
          losses=losses + ${losses},
          peak_elo=GREATEST(peak_elo, VALUES(elo))
      `, [p.uuid, gamemode, p.eloNew, wins, losses]);

      // Update global ELO (average across all gamemodes)
      await pool.query(`
        UPDATE zyrex_players p
        SET global_elo = (
          SELECT AVG(elo) FROM zyrex_stats WHERE uuid = ?
        ),
        total_wins = total_wins + ${wins},
        total_losses = total_losses + ${losses}
        WHERE uuid = ?
      `, [p.uuid, p.uuid]);
    }

    // Invalidate leaderboard caches
    const gamemodes = ['global', gamemode];
    for (const gm of gamemodes) {
      for (let page = 1; page <= 5; page++) {
        await cacheDel(`leaderboard:${gm}:current:${page}`);
      }
      await cacheDel(`leaderboard:top10:${gm}`);
    }
    await cacheDel(`player:${player1.username.toLowerCase()}`);
    await cacheDel(`player:${player2.username.toLowerCase()}`);

    res.json({ success: true, matchId });
  } catch (err) {
    console.error(err);
    res.status(500).json({ error: 'Internal server error' });
  }
});

module.exports = router;
