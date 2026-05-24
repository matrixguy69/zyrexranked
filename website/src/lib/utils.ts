export const GAMEMODES = [
  { id: 'global',    label: 'Global',       color: '#a855f7', emoji: '🌐' },
  { id: 'sword',     label: 'Sword',        color: '#fb923c', emoji: '⚔️' },
  { id: 'mace',      label: 'Mace',         color: '#f0abfc', emoji: '🔨' },
  { id: 'axe',       label: 'Axe',          color: '#ef4444', emoji: '🪓' },
  { id: 'smp',       label: 'SMP',          color: '#67e8f9', emoji: '🏠' },
  { id: 'diasmp',    label: 'Diamond SMP',  color: '#22d3ee', emoji: '💎' },
  { id: 'pot',       label: 'Pot PvP',      color: '#4ade80', emoji: '🧪' },
  { id: 'nethpot',   label: 'NethPot',      color: '#16a34a', emoji: '🍵' },
  { id: 'crystal',   label: 'Crystal PvP',  color: '#e2e8f0', emoji: '🔮' },
  { id: 'spearmace', label: 'Spear Mace',   color: '#c084fc', emoji: '🔱' },
  { id: 'uhc',       label: 'UHC',          color: '#fde047', emoji: '🍎' },
] as const;

export type GamemodeId = typeof GAMEMODES[number]['id'];

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

export function getRankClass(tier: string) {
  return `rank-${tier}`;
}

export function formatElo(elo: number) {
  return elo.toLocaleString();
}

export function formatWL(wins: number, losses: number) {
  if (losses === 0) return wins > 0 ? `${wins}.0` : '—';
  return (wins / losses).toFixed(2);
}

export function eloToNextRank(elo: number) {
  const thresholds = [1100, 1300, 1500, 1700, 1900, 2100, 2300, 2400, 2600, 2800, 2950];
  for (const t of thresholds) {
    if (elo < t) return t - elo;
  }
  return 0;
}

export function eloProgress(elo: number): number {
  const thresholds = [0, 1100, 1300, 1500, 1700, 1900, 2100, 2300, 2400, 2600, 2800, 2950];
  for (let i = thresholds.length - 1; i >= 0; i--) {
    if (elo >= thresholds[i]) {
      const next = thresholds[i + 1];
      if (!next) return 100;
      const range = next - thresholds[i];
      return Math.min(100, Math.round(((elo - thresholds[i]) / range) * 100));
    }
  }
  return 0;
}

export function timeAgo(timestamp: number | string) {
  const d = new Date(timestamp).getTime();
  const diff = Date.now() - d;
  const minutes = Math.floor(diff / 60000);
  const hours   = Math.floor(diff / 3600000);
  const days    = Math.floor(diff / 86400000);
  if (minutes < 1)  return 'just now';
  if (minutes < 60) return `${minutes}m ago`;
  if (hours < 24)   return `${hours}h ago`;
  return `${days}d ago`;
}

export function formatDuration(seconds: number) {
  const m = Math.floor(seconds / 60);
  const s = seconds % 60;
  return `${m}:${s.toString().padStart(2, '0')}`;
}

export function getGamemode(id: string) {
  return GAMEMODES.find(g => g.id === id);
}

export const SERVER_IP = process.env.NEXT_PUBLIC_SERVER_IP || 'zyrex.xyz';
