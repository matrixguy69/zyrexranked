import Link from 'next/link';
import { Sword } from 'lucide-react';

export default function Footer() {
  return (
    <footer className="border-t border-white/5 bg-dark-900/80 mt-20">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
        <div className="flex flex-col md:flex-row items-center justify-between gap-6">
          <div className="flex items-center gap-2.5">
            <div className="w-7 h-7 rounded-lg bg-gradient-to-br from-brand-primary to-brand-glow flex items-center justify-center">
              <Sword size={14} className="text-white" />
            </div>
            <span className="font-bold text-base">
              <span className="gradient-text">Zyrex</span>
              <span className="text-slate-500 font-normal ml-1">Ranked</span>
            </span>
          </div>

          <div className="flex items-center gap-6 text-sm text-slate-500">
            <Link href="/" className="hover:text-slate-300 transition-colors">Home</Link>
            <Link href="/leaderboard" className="hover:text-slate-300 transition-colors">Leaderboard</Link>
            <a href="https://discord.gg/zyrex" target="_blank" rel="noreferrer" className="hover:text-slate-300 transition-colors">Discord</a>
          </div>

          <p className="text-xs text-slate-600">
            © {new Date().getFullYear()} Zyrex Network. Not affiliated with Mojang.
          </p>
        </div>
      </div>
    </footer>
  );
}
