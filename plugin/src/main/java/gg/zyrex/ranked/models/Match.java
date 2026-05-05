package gg.zyrex.ranked.models;

import java.util.UUID;

public class Match {

    public enum MatchState {
        WAITING, STARTING, IN_PROGRESS, ENDED
    }

    public enum MatchResult {
        PLAYER1_WIN, PLAYER2_WIN, DRAW, CANCELLED
    }

    private final String matchId;
    private final UUID player1;
    private final UUID player2;
    private final Gamemode gamemode;
    private final long startTime;
    private MatchState state;
    private MatchResult result;
    private long endTime;

    // ELO at match start (for history)
    private final int player1EloStart;
    private final int player2EloStart;

    // ELO changes after match
    private int player1EloDelta;
    private int player2EloDelta;

    // Spectators
    private final java.util.List<UUID> spectators = new java.util.ArrayList<>();

    // Arena
    private String arenaName;

    public Match(String matchId, UUID p1, UUID p2, Gamemode gamemode, int p1Elo, int p2Elo) {
        this.matchId = matchId;
        this.player1 = p1;
        this.player2 = p2;
        this.gamemode = gamemode;
        this.startTime = System.currentTimeMillis();
        this.state = MatchState.WAITING;
        this.player1EloStart = p1Elo;
        this.player2EloStart = p2Elo;
    }

    public String getMatchId() { return matchId; }
    public UUID getPlayer1() { return player1; }
    public UUID getPlayer2() { return player2; }
    public Gamemode getGamemode() { return gamemode; }
    public long getStartTime() { return startTime; }
    public MatchState getState() { return state; }
    public void setState(MatchState state) { this.state = state; }
    public MatchResult getResult() { return result; }
    public void setResult(MatchResult result) { this.result = result; this.endTime = System.currentTimeMillis(); }
    public long getEndTime() { return endTime; }
    public int getPlayer1EloStart() { return player1EloStart; }
    public int getPlayer2EloStart() { return player2EloStart; }
    public int getPlayer1EloDelta() { return player1EloDelta; }
    public void setPlayer1EloDelta(int d) { this.player1EloDelta = d; }
    public int getPlayer2EloDelta() { return player2EloDelta; }
    public void setPlayer2EloDelta(int d) { this.player2EloDelta = d; }
    public java.util.List<UUID> getSpectators() { return spectators; }
    public void addSpectator(UUID uuid) { spectators.add(uuid); }
    public void removeSpectator(UUID uuid) { spectators.remove(uuid); }
    public String getArenaName() { return arenaName; }
    public void setArenaName(String arenaName) { this.arenaName = arenaName; }

    public long getDurationSeconds() {
        long end = endTime > 0 ? endTime : System.currentTimeMillis();
        return (end - startTime) / 1000;
    }

    public boolean hasPlayer(UUID uuid) {
        return player1.equals(uuid) || player2.equals(uuid);
    }

    public UUID getOpponent(UUID uuid) {
        if (player1.equals(uuid)) return player2;
        if (player2.equals(uuid)) return player1;
        return null;
    }
}
