'use client';
import { useEffect, useState } from 'react';
import { useParams } from 'next/navigation';
import Link from 'next/link';
import { TrendingUp, Clock, Flame, Shield, ArrowLeft } from 'lucide-react';
import clsx from 'clsx';
import { GAMEMODES, getRank, getRankClass, formatElo, formatWL, timeAgo, formatDuration, eloProgress, eloToNextRank } from '@/lib/utils';

const API = process.env.NEXT_PUBLIC_API_URL || 'https://zyrexranked-production.up.railway.app';

export default function PlayerPage() {
  const { username } = useParams<{ username: string }>();
  const [player, setPlayer] = useState<any>(null);
  const [loading, setLoading] = useState(true);
  const [notFound, setNotFound] = useState(false);
  const [activeGm, setActiveGm] = useState('sword');

  useEffect(() => {
    fetch(`${API}/api/player/${username}`)
      .then(r => { if (!r.ok) throw new Error(); return r.json(); })
      .then(data => setPlayer(data))
      .catch(() => setNotFound(true))
      .finally(() => setLoading(false));
  }, [username]);

  if (loading) return (
    <div className="max-w-4xl mx-auto px-4 py-20 text-center text-slate-500 animate-pulse">Loading player...</div>
  );

  if (notFound || !player) return (
    <div className="max-w-4xl mx-auto px-4 py-20 text-center">
      <div className="text-6xl mb-4">🔍</div>
      <h1 className="text-2xl font-bold text-white mb-2">Player not found</h1>
      <p className="text-slate-500 mb-6">"{username}" hasn't played ranked yet.</p>
      <Link href="/leaderboard" className="text-brand-accent hover:text-brand-primary">← Back to Leaderboard</Link>
    </div>
  );

  const globalRank = getRank(player.globalElo);
  const stats = player.gamemodes || {};

  return (
    <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
      <Link href="/leaderboard" className="inline-flex items-center gap-1.5 text-sm text-slate-500 hover:text-white mb-6 transition-colors">
        <ArrowLeft size={14} /> Back to Leaderboard
      </Link>

      {/* Profile header */}
      <div className="glass-card p-6 mb-6">
        <div className="flex flex-col sm:flex-row items-start sm:items-center gap-5">
          <div className="relative">
            <img src={player.bustUrl} alt={player.username} width={80} height={80} className="rounded-xl"
              onError={(e) => { (e.target as HTMLImageElement).src = player.avatarUrl; }} />
            <div className="absolute -bottom-1.5 -right-1.5 w-5 h-5 rounded-full border-2 border-dark-600" style={{ backgroundColor: globalRank.color }} />
          </div>
          <div className="flex-1">
            <div className="flex items-center gap-3 flex-wrap mb-1">
              <h1 className="text-3xl font-extrabold text-white">{player.username}</h1>
              <span className={getRankClass(globalRank.tier)}>{globalRank.name}</span>
            </div>
            <div className="flex items-center gap-4 text-sm text-slate-500 flex-wrap">
              <span>Last seen {timeAgo(player.lastSeen)}</span>
              <span>Season {player.currentSeason}</span>
            </div>
          </div>
          <div className="text-right">
            <div className="text-xs text-slate-500 mb-0.5">Global ELO</div>
            <div className="text-4xl font-black" style={{ color: globalRank.color }}>{formatElo(player.globalElo)}</div>
            <div className="text-xs text-slate-500 mt-1">{player.totalWins}W &nbsp;{player.totalLosses}L</div>
          </div>
        </div>
      </div>

      {/* Gamemode tabs with images */}
      <div className="flex gap-2 flex-wrap mb-4 overflow-x-auto no-scrollbar">
        {GAMEMODES.filter(g => g.id !== 'global').map(gm => (
          <button
            key={gm.id}
            onClick={() => setActiveGm(gm.id)}
            className={clsx(
              'flex items-center gap-2 px-3 py-1.5 rounded-lg text-sm font-medium whitespace-nowrap transition-all',
              activeGm === gm.id ? 'text-white border' : 'text-slate-400 hover:text-white bg-dark-500/40'
            )}
            style={activeGm === gm.id ? { backgroundColor: `${gm.color}18`, borderColor: `${gm.color}50`, color: gm.color } : {}}
          >
            {gm.image
              ? <img src={gm.image} alt={gm.label} className="w-5 h-5 object-contain" />
              : <Shield size={14} />
            }
            {gm.label}
          </button>
        ))}
      </div>

      {/* Gamemode stats */}
      {Object.entries(stats).map(([gmId, s]: [string, any]) => {
        if (gmId !== activeGm) return null;
        const gm = GAMEMODES.find(g => g.id === gmId);
        const rank = getRank(s.elo);
        const progress = eloProgress(s.elo);
        const toNext = eloToNextRank(s.elo);
        return (
          <div key={gmId} className="grid grid-cols-1 md:grid-cols-3 gap-4 mb-6">
            <div className="glass-card p-5 md:col-span-2">
              <div className="flex items-center justify-between mb-4">
                <div className="flex items-center gap-3">
                  {gm?.image && <img src={gm.image} alt={gm.label} className="w-10 h-10 object-contain" />}
                  <div>
                    <div className="text-xs text-slate-500 mb-1">{gm?.label} ELO</div>
                    <div className="text-3xl font-black" style={{ color: rank.color }}>{formatElo(s.elo)}</div>
                    <span className={getRankClass(rank.tier)}>{rank.name}</span>
                  </div>
                </div>
                {s.winStreak >= 3 && (
                  <div className="flex items-center gap-1.5 bg-orange-500/10 border border-orange-500/30 text-orange-400 px-3 py-1.5 rounded-lg text-sm font-medium">
                    <Flame size={14} />{s.winStreak} streak
                  </div>
                )}
              </div>
              <div className="mb-4">
                <div className="flex justify-between text-xs text-slate-500 mb-1.5">
                  <span>Progress to next rank</span>
                  <span>{toNext > 0 ? `+${toNext} ELO needed` : 'Max Rank'}</span>
                </div>
                <div className="h-2 bg-dark-400 rounded-full overflow-hidden">
                  <div className="h-full rounded-full transition-all duration-700" style={{ width: `${progress}%`, backgroundColor: rank.color }} />
                </div>
              </div>
              <div className="grid grid-cols-3 gap-3">
                <div className="bg-dark-500/40 rounded-xl p-3 text-center">
                  <div className="text-2xl font-bold text-emerald-400">{s.wins}</div>
                  <div className="text-xs text-slate-500">Wins</div>
                </div>
                <div className="bg-dark-500/40 rounded-xl p-3 text-center">
                  <div className="text-2xl font-bold text-red-400">{s.losses}</div>
                  <div className="text-xs text-slate-500">Losses</div>
                </div>
                <div className="bg-dark-500/40 rounded-xl p-3 text-center">
                  <div className="text-2xl font-bold text-white">{s.wlRatio}</div>
                  <div className="text-xs text-slate-500">W/L Ratio</div>
                </div>
              </div>
            </div>
            <div className="glass-card p-5 flex flex-col gap-4">
              <div>
                <div className="text-xs text-slate-500 mb-1 flex items-center gap-1"><TrendingUp size={11} /> Peak ELO</div>
                <div className="text-xl font-bold text-white">{formatElo(s.peakElo)}</div>
                <div className="text-xs text-slate-500">{getRank(s.peakElo).name}</div>
              </div>
              <div>
                <div className="text-xs text-slate-500 mb-1 flex items-center gap-1"><Flame size={11} /> Best Streak</div>
                <div className="text-xl font-bold text-orange-400">{s.bestStreak}x</div>
              </div>
              <div>
                <div className="text-xs text-slate-500 mb-1">Placement</div>
                <div className="text-sm text-slate-300">{Math.min(s.placementPlayed, 10)}/10 matches</div>
                <div className="h-1.5 bg-dark-400 rounded-full mt-1 overflow-hidden">
                  <div className="h-full bg-brand-primary rounded-full" style={{ width: `${Math.min(100, (s.placementPlayed / 10) * 100)}%` }} />
                </div>
              </div>
            </div>
          </div>
        );
      })}

      {/* Recent matches */}
      <div className="glass-card overflow-hidden">
        <div className="px-5 py-4 border-b border-white/5">
          <h2 className="font-semibold text-white flex items-center gap-2">
            <Clock size={16} className="text-brand-accent" /> Recent Matches
          </h2>
        </div>
        {!player.recentMatches?.length ? (
          <div className="py-10 text-center text-slate-500 text-sm">No matches played yet.</div>
        ) : (
          <div className="divide-y divide-white/[0.04]">
            {player.recentMatches.map((match: any) => {
              const gm = GAMEMODES.find(g => g.id === match.gamemode);
              const isWin = match.result === 'WIN';
              const isDraw = match.result === 'DRAW';
              const isCancelled = match.result === 'CANCELLED';
              return (
                <div key={match.matchId} className="flex items-center gap-4 px-5 py-3.5 hover:bg-dark-400/20 transition-colors">
                  <div className={clsx('w-12 text-center text-xs font-bold py-1.5 rounded-lg flex-shrink-0',
                    isWin ? 'bg-emerald-500/15 text-emerald-400' :
                    isDraw ? 'bg-yellow-500/15 text-yellow-400' :
                    isCancelled ? 'bg-gray-500/15 text-gray-400' : 'bg-red-500/15 text-red-400')}>
                    {match.result}
                  </div>
                  <div className="hidden sm:flex items-center gap-1.5 flex-shrink-0">
                    {gm?.image
                      ? <img src={gm.image} alt={gm.label} className="w-6 h-6 object-contain" />
                      : <span className="text-xs text-slate-400">{gm?.label || match.gamemode}</span>
                    }
                  </div>
                  <div className="flex-1 min-w-0">
                    <span className="text-xs text-slate-500">vs</span>{' '}
                    <Link href={`/player/${match.opponent}`} className="text-sm font-medium text-white hover:text-brand-accent transition-colors">
                      {match.opponent}
                    </Link>
                  </div>
                  <div className={clsx('font-mono text-sm font-semibold flex-shrink-0',
                    match.eloDelta > 0 ? 'text-emerald-400' : match.eloDelta < 0 ? 'text-red-400' : 'text-slate-500')}>
                    {match.eloDelta > 0 ? '+' : ''}{match.eloDelta}
                  </div>
                  <div className="hidden md:block text-right flex-shrink-0">
                    <div className="text-xs text-slate-600 font-mono">{formatDuration(match.durationSeconds)}</div>
                    <div className="text-xs text-slate-600">{timeAgo(match.playedAt)}</div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
