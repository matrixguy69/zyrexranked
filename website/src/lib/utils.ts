const BASE = 'https://raw.githubusercontent.com/matrixguy69/zyrexranked/main/website/public';

export const GAMEMODES = [
  { id: 'global',    label: 'Global',       color: '#a855f7', image: null },
  { id: 'sword',     label: 'Sword',        color: '#67e8f9', image: `${BASE}/sword.png` },
  { id: 'axe',       label: 'Axe',          color: '#60a5fa', image: `${BASE}/axe.png` },
  { id: 'mace',      label: 'Mace',         color: '#c084fc', image: `${BASE}/mace.png` },
  { id: 'smp',       label: 'SMP',          color: '#34d399', image: `${BASE}/smp.png` },
  { id: 'diasmp',    label: 'Diamond SMP',  color: '#22d3ee', image: `${BASE}/diasmp.png` },
  { id: 'pot',       label: 'Pot PvP',      color: '#f87171', image: `${BASE}/pot.png` },
  { id: 'nethpot',   label: 'NethPot',      color: '#4ade80', image: `${BASE}/nethpot.png` },
  { id: 'crystal',   label: 'Crystal PvP',  color: '#e879f9', image: `${BASE}/crystal.png` },
  { id: 'spearmace', label: 'Spear Mace',   color: '#a78bfa', image: `${BASE}/spearmace.png` },
  { id: 'uhc',       label: 'UHC',          color: '#fbbf24', image: `${BASE}/uhc.png` },
] as const;

export type GamemodeId = typeof GAMEMODES[number]['id'];
export const SERVER_IP = process.env.NEXT_PUBLIC_SERVER_IP || 'zyrex.xyz';

export const RANKS = [
  { name: 'Champion',     tier: 'champion',  minElo: 2950, color: '#f0abfc', glow: 'rgba(240,171,252,0.3)' },
  { name: 'Netherite III',tier: 'netherite', minElo: 2800, color: '#a1a1aa', glow: 'rgba(161,161,170,0.2)' },
  { name: 'Netherite II', tier: 'netherite', minElo: 2600, color: '#a1a1aa', glow: 'rgba(161,161,170,0.2)' },
  { name: 'Netherite I',  tier: 'netherite', minElo: 2400, color: '#a1a1aa', glow: 'rgba(161,161,170,0.2)' },
  { name: 'Diamond III',  tier: 'diamond',   minElo: 2300, color: '#67e8f9', glow: 'rgba(103,232,249,0.2)' },
  { name: 'Diamond II',   tier: 'diamond',   minElo: 2100, color: '#67e8f9', glow: 'rgba(103,232,249,0.2)' },
  { name: 'Diamond I',    tier: 'diamond',   minElo: 1900, color: '#67e8f9', glow: 'rgba(103,232,249,0.2)' },
  { name: 'Platinum',     tier: 'platinum',  minElo: 1700, color: '#5eead4', glow: 'rgba(94,234,212,0.2)'  },
  { name: 'Gold',         tier: 'gold',      minElo: 1500, color: '#fde047', glow: 'rgba(253,224,71,0.2)'  },
  { name: 'Silver',       tier: 'silver',    minElo: 1300, color: '#e2e8f0', glow: 'rgba(226,232,240,0.15)'},
  { name: 'Bronze',       tier: 'bronze',    minElo: 1100, color: '#fb923c', glow: 'rgba(251,146,60,0.2)'  },
  { name: 'Iron',         tier: 'iron',      minElo: 0,    color: '#9ca3af', glow: 'rgba(156,163,175,0.1)' },
];

export function getRank(elo: number) {
  return RANKS.find(r => elo >= r.minElo) ?? RANKS[RANKS.length - 1];
}
export function getRankClass(tier: string) { return `rank-${tier}`; }
export function formatElo(elo: number) { return elo.toLocaleString(); }
export function formatWL(wins: number, losses: number) {
  if (losses === 0) return wins > 0 ? `${wins}.0` : '—';
  return (wins / losses).toFixed(2);
}
export function eloToNextRank(elo: number) {
  const thresholds = [1100,1300,1500,1700,1900,2100,2300,2400,2600,2800,2950];
  for (const t of thresholds) { if (elo < t) return t - elo; }
  return 0;
}
export function eloProgress(elo: number): number {
  const thresholds = [0,1100,1300,1500,1700,1900,2100,2300,2400,2600,2800,2950];
  for (let i = thresholds.length - 1; i >= 0; i--) {
    if (elo >= thresholds[i]) {
      const next = thresholds[i + 1];
      if (!next) return 100;
      return Math.min(100, Math.round(((elo - thresholds[i]) / (next - thresholds[i])) * 100));
    }
  }
  return 0;
}
export function timeAgo(timestamp: number | string) {
  const diff = Date.now() - new Date(timestamp).getTime();
  const m = Math.floor(diff/60000), h = Math.floor(diff/3600000), d = Math.floor(diff/86400000);
  if (m < 1) return 'just now';
  if (m < 60) return `${m}m ago`;
  if (h < 24) return `${h}h ago`;
  return `${d}d ago`;
}
export function formatDuration(seconds: number) {
  const m = Math.floor(seconds/60), s = seconds % 60;
  return `${m}:${s.toString().padStart(2,'0')}`;
}
export function getGamemode(id: string) { return GAMEMODES.find(g => g.id === id); }
