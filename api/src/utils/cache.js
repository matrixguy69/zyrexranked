const { createClient } = require('redis');
const { logger } = require('./logger');

let client;

async function connectRedis() {
  client = createClient({
    socket: {
      host: process.env.REDIS_HOST || 'localhost',
      port: parseInt(process.env.REDIS_PORT) || 6379,
    },
    password: process.env.REDIS_PASS || undefined,
  });

  client.on('error', (err) => logger.warn('Redis error: ' + err.message));

  await client.connect();
  logger.info('Redis connected successfully.');
}

function getRedis() {
  if (!client) throw new Error('Redis not connected');
  return client;
}

async function cacheGet(key) {
  try {
    const val = await client.get(key);
    return val ? JSON.parse(val) : null;
  } catch { return null; }
}

async function cacheSet(key, value, ttlSeconds) {
  try {
    await client.set(key, JSON.stringify(value), { EX: ttlSeconds || parseInt(process.env.CACHE_TTL_SECONDS) || 30 });
  } catch (e) { /* non-fatal */ }
}

async function cacheDel(key) {
  try { await client.del(key); } catch { /* non-fatal */ }
}

module.exports = { connectRedis, getRedis, cacheGet, cacheSet, cacheDel };
