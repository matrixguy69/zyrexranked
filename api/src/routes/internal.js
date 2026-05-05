const express = require('express');
const router = express.Router();
const { pluginAuth } = require('../middleware/auth');
const { cacheDel } = require('../utils/cache');

/**
 * POST /api/internal/sync
 * Called by plugin every 30s to bust leaderboard cache
 */
router.post('/sync', pluginAuth, async (req, res) => {
  try {
    const gamemodes = ['global', 'smp', 'pot', 'uhc', 'sword', 'spearmace'];
    for (const gm of gamemodes) {
      for (let page = 1; page <= 10; page++) {
        await cacheDel(`leaderboard:${gm}:current:${page}`);
      }
      await cacheDel(`leaderboard:top10:${gm}`);
    }
    res.json({ success: true, message: 'Leaderboard cache cleared' });
  } catch (err) {
    res.status(500).json({ error: 'Internal server error' });
  }
});

module.exports = router;
