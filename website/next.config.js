/** @type {import('next').NextConfig} */
const nextConfig = {
  images: {
    remotePatterns: [
      { protocol: 'https', hostname: 'crafatar.com' },
      { protocol: 'https', hostname: 'visage.surgeplay.com' },
    ],
  },
  eslint: { ignoreDuringBuilds: true },
  typescript: { ignoreBuildErrors: true },
  env: {
    NEXT_PUBLIC_API_URL: process.env.NEXT_PUBLIC_API_URL || 'https://zyrexranked-production.up.railway.app',
    NEXT_PUBLIC_SERVER_IP: process.env.NEXT_PUBLIC_SERVER_IP || 'zyrex.xyz',
  },
};

module.exports = nextConfig;
