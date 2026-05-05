import Link from 'next/link';
import { Trophy, Sword, Users, Zap, ChevronRight, Shield } from 'lucide-react';
import { GAMEMODES, getRank } from '@/lib/utils';

export default function HomePage() {
  return (
    <div className="relative overflow-hidden">
      {/* ── Hero ─────────────────────────────────────────────────────── */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 pt-20 pb-16 text-center">
        {/* Badge */}
        <div className="inline-flex items-center gap-2 bg-brand-primary/10 border border-brand-primary/30 text-brand-accent text-xs font-semibold px-4 py-1.5 rounded-full mb-6 animate-fade-up">
          <Zap size={12} />
          Season 1 — Now Live
        </div>

        <h1 className="text-5xl sm:text-7xl font-extrabold tracking-tight mb-6 animate-fade-up" style={{ animationDelay: '0.1s' }}>
          Compete. Climb.
          <br />
          <span className="gradient-text">Dominate.</span>
        </h1>

        <p className="text-slate-400 text-lg sm:text-xl max-w-2xl mx-auto mb-10 animate-fade-up" style={{ animationDelay: '0.15s' }}>
          The most competitive ranked system on Minecraft.
          Earn ELO, climb tiers, and prove you're the best across 5 gamemodes.
        </p>

        <div className="flex flex-col sm:flex-row gap-4 justify-center animate-fade-up" style={{ animationDelay: '0.2s' }}>
          <Link
            href="/leaderboard"
            className="inline-flex items-center gap-2 bg-gradient-to-r from-brand-primary to-brand-glow text-white font-semibold px-8 py-3.5 rounded-xl hover:opacity-90 transition-opacity shadow-glow-purple"
          >
            <Trophy size={18} />
            View Leaderboard
          </Link>
          <div className="inline-flex items-center gap-2 bg-dark-500/60 border border-white/10 text-slate-300 font-medium px-8 py-3.5 rounded-xl">
            <span className="font-mono text-sm text-brand-accent">play.zyrex.gg</span>
          </div>
        </div>
      </section>

      {/* ── Stats bar ─────────────────────────────────────────────────── */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 mb-16">
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          {[
            { label: 'Total Matches', value: '12,480+', icon: Sword },
            { label: 'Ranked Players', value: '3,200+', icon: Users },
            { label: 'Gamemodes', value: '5', icon: Shield },
            { label: 'Season', value: '1', icon: Trophy },
          ].map(stat => (
            <div key={stat.label} className="glass-card p-5 text-center">
              <stat.icon size={20} className="text-brand-accent mx-auto mb-2" />
              <div className="text-2xl font-bold text-white mb-0.5">{stat.value}</div>
              <div className="text-xs text-slate-500">{stat.label}</div>
            </div>
          ))}
        </div>
      </section>

      {/* ── Gamemodes ─────────────────────────────────────────────────── */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 mb-20">
        <div className="flex items-center justify-between mb-6">
          <h2 className="text-2xl font-bold text-white">Ranked Gamemodes</h2>
          <Link href="/leaderboard" className="text-sm text-brand-accent hover:text-brand-primary flex items-center gap-1 transition-colors">
            Full Leaderboard <ChevronRight size={14} />
          </Link>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {GAMEMODES.filter(g => g.id !== 'global').map(gm => (
            <Link
              key={gm.id}
              href={`/leaderboard?gamemode=${gm.id}`}
              className="glass-card-hover p-5 group"
            >
              <div className="flex items-start justify-between mb-4">
                <div>
                  <span className="text-2xl mb-2 block">{gm.emoji}</span>
                  <h3 className="font-semibold text-white text-lg">{gm.label}</h3>
                  <p className="text-xs text-slate-500 mt-0.5">1v1 Ranked</p>
                </div>
                <ChevronRight size={18} className="text-slate-600 group-hover:text-brand-accent transition-colors mt-1" />
              </div>
              <div
                className="h-0.5 rounded-full w-12 group-hover:w-full transition-all duration-500"
                style={{ backgroundColor: gm.color }}
              />
            </Link>
          ))}
        </div>
      </section>

      {/* ── Rank tiers ────────────────────────────────────────────────── */}
      <section className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 mb-20">
        <h2 className="text-2xl font-bold text-white mb-6 text-center">Rank Tiers</h2>
        <div className="glass-card p-6">
          <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-6 gap-3">
            {[
              { name: 'Champion',   elo: '2950+', tier: 'champion',  color: '#f0abfc' },
              { name: 'Netherite', elo: '2400+', tier: 'netherite', color: '#a1a1aa' },
              { name: 'Diamond',   elo: '1900+', tier: 'diamond',   color: '#67e8f9' },
              { name: 'Platinum',  elo: '1700+', tier: 'platinum',  color: '#5eead4' },
              { name: 'Gold',      elo: '1500+', tier: 'gold',      color: '#fde047' },
              { name: 'Silver',    elo: '1300+', tier: 'silver',    color: '#e2e8f0' },
            ].map(rank => (
              <div key={rank.name} className="text-center p-3 rounded-xl bg-dark-500/40">
                <div className="w-10 h-10 rounded-full mx-auto mb-2 flex items-center justify-center"
                  style={{ backgroundColor: `${rank.color}20`, border: `1px solid ${rank.color}40` }}>
                  <Shield size={16} style={{ color: rank.color }} />
                </div>
                <div className="font-semibold text-sm" style={{ color: rank.color }}>{rank.name}</div>
                <div className="text-xs text-slate-500 mt-0.5">{rank.elo} ELO</div>
              </div>
            ))}
          </div>
        </div>
      </section>
    </div>
  );
}
