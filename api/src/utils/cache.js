let client = null;
let redisAvailable = false;

async function connectRedis() {
  const host = process.env.REDIS_HOST;
  if (!host || host === 'leave-blank-for-now' || host === '') {
    console.log('[Cache] Redis not configured — running without cache.');
    return;
  }
  try {
    const { createClient } = require('redis');
    client = createClient({
      socket: { host, port: parseInt(process.env.REDIS_PORT) || 6379, connectTimeout: 5000 },
      password: process.env.REDIS_PASS || undefined,
    });
    client.on('error', () => { redisAvailable = false; });
    await client.connect();
    redisAvailable = true;
    console.log('[Cache] Redis connected.');
  } catch (e) {
    redisAvailable = false;
    console.log('[Cache] Redis unavailable — running without cache.');
  }
}

async function cacheGet(key) {
  if (!redisAvailable || !client) return null;
  try { const val = await client.get(key); return val ? JSON.parse(val) : null; }
  catch { return null; }
}

async function cacheSet(key, value, ttlSeconds) {
  if (!redisAvailable || !client) return;
  try { await client.set(key, JSON.stringify(value), { EX: ttlSeconds || 30 }); }
  catch { }
}

async function cacheDel(key) {
  if (!redisAvailable || !client) return;
  try { await client.del(key); } catch { }
}

module.exports = { connectRedis, cacheGet, cacheSet, cacheDel };
