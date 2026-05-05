/** @type {import('next').NextConfig} */
const nextConfig = {
  images: {
    remotePatterns: [
      { protocol: 'https', hostname: 'crafatar.com' },
      { protocol: 'https', hostname: 'visage.surgeplay.com' },
    ],
  },
  env: {
    NEXT_PUBLIC_API_URL: process.env.NEXT_PUBLIC_API_URL || 'https://api.zyrex.gg',
    NEXT_PUBLIC_SERVER_IP: process.env.NEXT_PUBLIC_SERVER_IP || 'play.zyrex.gg',
  },
};

module.exports = nextConfig;
