'use client';
import { useState, useEffect, Suspense } from 'react';
import { useSearchParams, useRouter } from 'next/navigation';
import Link from 'next/link';
import { Search, Trophy, ChevronLeft, ChevronRight, Flame } from 'lucide-react';
import clsx from 'clsx';
import { GAMEMODES, getRank, formatElo, formatWL, timeAgo, getRankClass } from '@/lib/utils';

const API = process.env.NEXT_PUBLIC_API_URL || 'https://zyrexranked-production.up.railway.app';

interface Player {
  position: number;
  uuid: string;
  username: string;
  elo: number;
  wins: number;
  losses: number;
  wlRatio: string;
  peakElo: number;
  bestStreak: number;
  winStreak: number;
  rank: string;
  rankTier: string;
  lastSeen: number;
  avatarUrl: string;
}

function PositionBadge({ pos }: { pos: number }) {
  if (pos === 1) return <span className="text-yellow-400 font-bold text-lg">🥇</span>;
  if (pos === 2) return <span className="text-slate-300 font-bold text-lg">🥈</span>;
  if (pos === 3) return <span className="text-orange-400 font-bold text-lg">🥉</span>;
  return <span className="text-slate-500 font-mono text-sm w-6 text-center">{pos}</span>;
}

function PlayerRow({ player }: { player: Player }) {
  const rank = getRank(player.elo);
  const isTop3 = player.position <= 3;
  return (
    <Link
      href={`/player/${player.username}`}
      className={clsx(
        'flex items-center gap-4 px-4 py-3.5 rounded-xl transition-all duration-200 group',
        'hover:bg-dark-400/40 hover:border-white/10 border border-transparent',
        isTop3 && 'bg-dark-500/40'
      )}
      style={isTop3 ? { borderColor: `${rank.color}20` } : {}}
    >
      <div className="w-8 flex justify-center flex-shrink-0">
        <PositionBadge pos={player.position} />
      </div>
      <div className="relative flex-shrink-0">
        <img
          src={player.avatarUrl}
          alt={player.username}
          width={36}
          height={36}
          className="rounded-lg"
          onError={(e) => { (e.target as HTMLImageElement).src = `https://crafatar.com/avatars/${player.uuid}?size=36`; }}
        />
        {player.winStreak >= 3 && (
          <div className="absolute -top-1 -right-1 w-4 h-4 bg-orange-500 rounded-full flex items-center justify-center">
            <Flame size={9} className="text-white" />
          </div>
        )}
      </div>
      <div className="flex-1 min-w-0">
        <div className="flex items-center gap-2 flex-wrap">
          <span className="font-semibold text-white group-hover:text-brand-accent transition-colors truncate">
            {player.username}
          </span>
          <span className={getRankClass(player.rankTier)}>{player.rank}</span>
          {player.winStreak >= 3 && (
            <span className="text-xs text-orange-400 font-medium">🔥 {player.winStreak}</span>
          )}
        </div>
        <div className="text-xs text-slate-500 mt-0.5">Last seen {timeAgo(player.lastSeen)}</div>
      </div>
      <div className="hidden sm:flex items-center gap-6 text-sm flex-shrink-0">
        <div className="text-center hidden md:block">
          <div className="text-xs text-slate-500 mb-0.5">W/L</div>
          <div className="text-slate-300 font-medium">
            <span className="text-emerald-400">{player.wins}</span>
            <span className="text-slate-600 mx-1">/</span>
            <span className="text-red-400">{player.losses}</span>
          </div>
        </div>
        <div className="text-center hidden lg:block">
          <div className="text-xs text-slate-500 mb-0.5">Ratio</div>
          <div className="text-slate-300 font-medium">{player.wlRatio}</div>
        </div>
        <div className="text-right">
          <div className="text-xs text-slate-500 mb-0.5">ELO</div>
          <div className="font-bold" style={{ color: rank.color }}>{formatElo(player.elo)}</div>
        </div>
      </div>
    </Link>
  );
}

function LeaderboardContent() {
  const searchParams = useSearchParams();
  const router = useRouter();
  const gamemode = searchParams.get('gamemode') || 'global';
  const page = parseInt(searchParams.get('page') || '1');
  const [players, setPlayers] = useState<Player[]>([]);
  const [totalPages, setTotalPages] = useState(1);
  const [totalPlayers, setTotalPlayers] = useState(0);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');

  useEffect(() => {
    setLoading(true);
    fetch(`${API}/api/leaderboard?gamemode=${gamemode}&page=${page}`)
      .then(r => r.json())
      .then(data => {
        setPlayers(data.players || []);
        setTotalPages(data.totalPages || 1);
        setTotalPlayers(data.totalPlayers || 0);
      })
      .catch(() => setPlayers([]))
      .finally(() => setLoading(false));
  }, [gamemode, page]);

  const filtered = search
    ? players.filter(p => p.username.toLowerCase().includes(search.toLowerCase()))
    : players;

  const currentGm = GAMEMODES.find(g => g.id === gamemode);

  return (
    <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
      {/* Header */}
      <div className="mb-8 flex items-center gap-4">
        {currentGm?.image && (
          <img src={currentGm.image} alt={currentGm.label} className="w-12 h-12 object-contain" />
        )}
        <div>
          <h1 className="text-3xl font-bold text-white">
            {currentGm ? currentGm.label : 'Global'} Leaderboard
          </h1>
          <p className="text-slate-500">{totalPlayers.toLocaleString()} ranked players · Season 1</p>
        </div>
      </div>

      {/* Gamemode tabs */}
      <div className="flex gap-2 flex-wrap mb-6 overflow-x-auto no-scrollbar pb-1">
        {GAMEMODES.map(gm => (
          <button
            key={gm.id}
            onClick={() => router.push(`/leaderboard?gamemode=${gm.id}&page=1`)}
            className={clsx(
              'flex items-center gap-2 px-3 py-2 rounded-lg text-sm font-medium whitespace-nowrap transition-all duration-200',
              gamemode === gm.id
                ? 'text-white border'
                : 'text-slate-400 hover:text-white bg-dark-500/40 hover:bg-dark-400/60'
            )}
            style={gamemode === gm.id ? {
              backgroundColor: `${gm.color}18`,
              borderColor: `${gm.color}50`,
              color: gm.color
            } : {}}
          >
            {gm.image
              ? <img src={gm.image} alt={gm.label} className="w-5 h-5 object-contain" />
              : <Trophy size={14} />
            }
            {gm.label}
          </button>
        ))}
      </div>

      {/* Table */}
      <div className="glass-card overflow-hidden">
        <div className="p-4 border-b border-white/5">
          <div className="relative max-w-xs">
            <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-slate-500" />
            <input
              type="text"
              placeholder="Search player..."
              value={search}
              onChange={e => setSearch(e.target.value)}
              className="w-full bg-dark-500/60 border border-white/10 rounded-lg pl-9 pr-3 py-2 text-sm text-white placeholder-slate-600 focus:outline-none focus:border-brand-primary/50"
            />
          </div>
        </div>
        <div className="flex items-center gap-4 px-4 py-2.5 text-xs text-slate-600 font-medium uppercase tracking-wider border-b border-white/5">
          <div className="w-8 text-center">#</div>
          <div className="w-9 flex-shrink-0" />
          <div className="flex-1">Player</div>
          <div className="hidden md:block w-20 text-center">W / L</div>
          <div className="hidden lg:block w-16 text-center">Ratio</div>
          <div className="w-16 text-right">ELO</div>
        </div>
        <div className="divide-y divide-white/[0.03] px-2 py-2">
          {loading ? (
            Array.from({ length: 10 }).map((_, i) => (
              <div key={i} className="flex items-center gap-4 px-4 py-3.5 animate-pulse">
                <div className="w-8 h-4 bg-dark-400 rounded" />
                <div className="w-9 h-9 bg-dark-400 rounded-lg" />
                <div className="flex-1"><div className="h-4 bg-dark-400 rounded w-32 mb-1" /><div className="h-3 bg-dark-400 rounded w-20" /></div>
                <div className="h-4 bg-dark-400 rounded w-16" />
              </div>
            ))
          ) : filtered.length === 0 ? (
            <div className="py-16 text-center text-slate-500">
              {search ? `No players matching "${search}"` : 'No ranked players yet. Be the first!'}
            </div>
          ) : (
            filtered.map(player => <PlayerRow key={player.uuid} player={player} />)
          )}
        </div>
        {!search && totalPages > 1 && (
          <div className="flex items-center justify-between px-4 py-4 border-t border-white/5">
            <span className="text-xs text-slate-600">Page {page} of {totalPages}</span>
            <div className="flex gap-2">
              <button onClick={() => router.push(`/leaderboard?gamemode=${gamemode}&page=${page-1}`)} disabled={page <= 1} className="p-2 rounded-lg bg-dark-500/40 text-slate-400 hover:text-white disabled:opacity-30 disabled:cursor-not-allowed">
                <ChevronLeft size={16} />
              </button>
              <button onClick={() => router.push(`/leaderboard?gamemode=${gamemode}&page=${page+1}`)} disabled={page >= totalPages} className="p-2 rounded-lg bg-dark-500/40 text-slate-400 hover:text-white disabled:opacity-30 disabled:cursor-not-allowed">
                <ChevronRight size={16} />
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

export default function LeaderboardPage() {
  return (
    <Suspense fallback={<div className="max-w-7xl mx-auto px-4 py-20 text-center text-slate-500">Loading leaderboard...</div>}>
      <LeaderboardContent />
    </Suspense>
  );
}
