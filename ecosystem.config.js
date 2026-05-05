// PM2 ecosystem config
// Usage: pm2 start ecosystem.config.js
// Place this file on your VPS root alongside api/ and website/ folders

module.exports = {
  apps: [
    {
      name: 'zyrex-api',
      script: './api/src/server.js',
      instances: 1,
      autorestart: true,
      watch: false,
      max_memory_restart: '500M',
      env: {
        NODE_ENV: 'production',
        PORT: 3001
      },
      error_file: './logs/api-error.log',
      out_file: './logs/api-out.log',
      log_date_format: 'YYYY-MM-DD HH:mm:ss'
    },
    {
      name: 'zyrex-web',
      script: 'npm',
      args: 'start',
      cwd: './website',
      instances: 1,
      autorestart: true,
      watch: false,
      max_memory_restart: '500M',
      env: {
        NODE_ENV: 'production',
        PORT: 3000
      },
      error_file: './logs/web-error.log',
      out_file: './logs/web-out.log',
      log_date_format: 'YYYY-MM-DD HH:mm:ss'
    }
  ]
};
