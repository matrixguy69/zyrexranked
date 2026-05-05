require('dotenv').config();
const express = require('express');
const cors = require('cors');
const helmet = require('helmet');
const rateLimit = require('express-rate-limit');
const { logger } = require('./utils/logger');
const { connectDB } = require('./utils/db');
const { connectRedis } = require('./utils/cache');

// Routes
const leaderboardRoutes = require('./routes/leaderboard');
const playerRoutes = require('./routes/player');
const matchRoutes = require('./routes/match');
const internalRoutes = require('./routes/internal');

const app = express();

// ── Security ──────────────────────────────────────────────────────
app.use(helmet());
app.use(cors({
  origin: process.env.ALLOWED_ORIGIN || '*',
  methods: ['GET', 'POST'],
}));

// ── Rate limiting ─────────────────────────────────────────────────
const limiter = rateLimit({
  windowMs: 60 * 1000,
  max: 120,
  standardHeaders: true,
  legacyHeaders: false,
  message: { error: 'Too many requests, slow down.' }
});
app.use('/api', limiter);

// ── Body parser ───────────────────────────────────────────────────
app.use(express.json({ limit: '1mb' }));

// ── Routes ────────────────────────────────────────────────────────
app.use('/api/leaderboard', leaderboardRoutes);
app.use('/api/player', playerRoutes);
app.use('/api/match', matchRoutes);
app.use('/api/internal', internalRoutes);

// Health check
app.get('/health', (req, res) => res.json({ status: 'ok', ts: Date.now() }));

// 404
app.use((req, res) => res.status(404).json({ error: 'Not found' }));

// Error handler
app.use((err, req, res, next) => {
  logger.error(err.stack);
  res.status(500).json({ error: 'Internal server error' });
});

// ── Boot ──────────────────────────────────────────────────────────
async function boot() {
  await connectDB();
  await connectRedis();
  const port = process.env.PORT || 3001;
  app.listen(port, () => {
    logger.info(`Zyrex Ranked API listening on port ${port}`);
  });
}

boot().catch(err => {
  logger.error('Boot failed:', err);
  process.exit(1);
});
