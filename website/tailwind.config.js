/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    './src/pages/**/*.{js,ts,jsx,tsx,mdx}',
    './src/components/**/*.{js,ts,jsx,tsx,mdx}',
    './src/app/**/*.{js,ts,jsx,tsx,mdx}',
  ],
  theme: {
    extend: {
      colors: {
        // Zyrex brand
        brand: {
          primary: '#a855f7',   // purple
          secondary: '#7c3aed',
          accent: '#c084fc',
          glow: '#d946ef',
        },
        // Dark background system
        dark: {
          900: '#060608',
          800: '#0d0d14',
          700: '#12121c',
          600: '#181825',
          500: '#1e1e2e',
          400: '#252538',
          300: '#2e2e48',
          200: '#3a3a58',
        },
        // Rank tier colors
        rank: {
          champion: '#f0abfc',
          netherite: '#a1a1aa',
          diamond: '#67e8f9',
          platinum: '#5eead4',
          gold: '#fde047',
          silver: '#e2e8f0',
          bronze: '#fb923c',
          iron: '#9ca3af',
        },
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', 'sans-serif'],
        mono: ['JetBrains Mono', 'monospace'],
      },
      backgroundImage: {
        'gradient-radial': 'radial-gradient(var(--tw-gradient-stops))',
        'hero-glow': 'radial-gradient(ellipse 80% 50% at 50% -20%, rgba(168,85,247,0.25), transparent)',
        'card-shine': 'linear-gradient(135deg, rgba(255,255,255,0.05) 0%, transparent 100%)',
      },
      animation: {
        'fade-up': 'fadeUp 0.5s ease-out',
        'pulse-slow': 'pulse 3s cubic-bezier(0.4, 0, 0.6, 1) infinite',
        'shimmer': 'shimmer 2s infinite',
        'float': 'float 3s ease-in-out infinite',
      },
      keyframes: {
        fadeUp: {
          '0%': { opacity: 0, transform: 'translateY(16px)' },
          '100%': { opacity: 1, transform: 'translateY(0)' },
        },
        shimmer: {
          '0%': { backgroundPosition: '-200% 0' },
          '100%': { backgroundPosition: '200% 0' },
        },
        float: {
          '0%, 100%': { transform: 'translateY(0px)' },
          '50%': { transform: 'translateY(-8px)' },
        },
      },
      boxShadow: {
        'glow-purple': '0 0 20px rgba(168,85,247,0.4)',
        'glow-sm': '0 0 10px rgba(168,85,247,0.2)',
        'card': '0 4px 24px rgba(0,0,0,0.4)',
      },
    },
  },
  plugins: [],
};
