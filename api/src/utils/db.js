const mysql = require('mysql2/promise');
const { logger } = require('./logger');

let pool;

async function connectDB() {
  pool = mysql.createPool({
    host: process.env.DB_HOST,
    port: parseInt(process.env.DB_PORT) || 3306,
    database: process.env.DB_NAME,
    user: process.env.DB_USER,
    password: process.env.DB_PASS,
    waitForConnections: true,
    connectionLimit: 20,
    queueLimit: 0,
    charset: 'utf8mb4',
  });

  // Test connection
  const conn = await pool.getConnection();
  logger.info('MySQL connected successfully.');
  conn.release();
}

function getPool() {
  if (!pool) throw new Error('Database not connected');
  return pool;
}

module.exports = { connectDB, getPool };
