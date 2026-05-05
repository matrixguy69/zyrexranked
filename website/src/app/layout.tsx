import type { Metadata } from 'next';
import { Inter } from 'next/font/google';
import './globals.css';
import Navbar from '@/components/layout/Navbar';
import Footer from '@/components/layout/Footer';

const inter = Inter({ subsets: ['latin'], variable: '--font-inter' });

export const metadata: Metadata = {
  title: 'Zyrex Ranked | Competitive PvP Leaderboards',
  description: 'Compete in ranked PvP on Zyrex Network. Track your ELO, climb the leaderboards across Crystal, Pot PvP, UHC, Sword, and Spear Mace.',
  openGraph: {
    title: 'Zyrex Ranked',
    description: 'Compete in ranked PvP on Zyrex Network.',
    images: ['/og-image.png'],
  },
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en" className={inter.variable}>
      <head>
        <link rel="icon" href="/favicon.ico" />
        <link rel="preconnect" href="https://fonts.googleapis.com" />
      </head>
      <body className="bg-dark-900 text-white antialiased min-h-screen flex flex-col">
        <div className="fixed inset-0 bg-hero-glow pointer-events-none z-0" />
        <Navbar />
        <main className="flex-1 relative z-10">{children}</main>
        <Footer />
      </body>
    </html>
  );
}
