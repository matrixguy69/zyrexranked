'use client';
import Link from 'next/link';
import { usePathname } from 'next/navigation';
import { useState } from 'react';
import { Menu, X, Sword, Trophy } from 'lucide-react';
import clsx from 'clsx';

const links = [
  { href: '/', label: 'Home' },
  { href: '/leaderboard', label: 'Leaderboard' },
  { href: '/leaderboard?gamemode=smp', label: 'SMP/Crystal' },
  { href: '/leaderboard?gamemode=pot', label: 'Pot PvP' },
  { href: '/leaderboard?gamemode=uhc', label: 'UHC' },
  { href: '/leaderboard?gamemode=sword', label: 'Sword' },
  { href: '/leaderboard?gamemode=spearmace', label: 'Spear Mace' },
];

export default function Navbar() {
  const pathname = usePathname();
  const [mobileOpen, setMobileOpen] = useState(false);

  return (
    <nav className="sticky top-0 z-50 border-b border-white/5 bg-dark-900/80 backdrop-blur-xl">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16">
          {/* Logo */}
          <Link href="/" className="flex items-center gap-2.5 group">
            <div className="w-8 h-8 rounded-lg bg-gradient-to-br from-brand-primary to-brand-glow flex items-center justify-center shadow-glow-sm group-hover:shadow-glow-purple transition-all">
              <Sword size={16} className="text-white" />
            </div>
            <span className="font-bold text-lg tracking-tight">
              <span className="gradient-text">Zyrex</span>
              <span className="text-slate-400 font-normal ml-1">Ranked</span>
            </span>
          </Link>

          {/* Desktop nav */}
          <div className="hidden md:flex items-center gap-1">
            {links.slice(0, 2).map(link => (
              <Link
                key={link.href}
                href={link.href}
                className={clsx(
                  'px-3 py-1.5 rounded-lg text-sm font-medium transition-colors',
                  pathname === link.href
                    ? 'text-white bg-brand-primary/20'
                    : 'text-slate-400 hover:text-white hover:bg-white/5'
                )}
              >
                {link.label}
              </Link>
            ))}
          </div>

          {/* Server IP + Mobile */}
          <div className="flex items-center gap-3">
            <div className="hidden sm:flex items-center gap-2 bg-dark-500/60 border border-white/10 rounded-lg px-3 py-1.5">
              <div className="w-1.5 h-1.5 rounded-full bg-emerald-400 animate-pulse-slow" />
              <span className="text-xs text-slate-400 font-mono">play.zyrex.gg</span>
            </div>
            <button
              className="md:hidden p-2 rounded-lg text-slate-400 hover:text-white hover:bg-white/5"
              onClick={() => setMobileOpen(!mobileOpen)}
            >
              {mobileOpen ? <X size={20} /> : <Menu size={20} />}
            </button>
          </div>
        </div>
      </div>

      {/* Mobile menu */}
      {mobileOpen && (
        <div className="md:hidden border-t border-white/5 bg-dark-800/95 px-4 py-4 space-y-1">
          {links.map(link => (
            <Link
              key={link.href}
              href={link.href}
              onClick={() => setMobileOpen(false)}
              className="block px-3 py-2 rounded-lg text-sm text-slate-300 hover:text-white hover:bg-white/5"
            >
              {link.label}
            </Link>
          ))}
        </div>
      )}
    </nav>
  );
}
