package gg.zyrex.ranked.api;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import gg.zyrex.ranked.ZyrexRanked;
import gg.zyrex.ranked.models.Gamemode;
import gg.zyrex.ranked.models.Match;
import gg.zyrex.ranked.models.RankedPlayer;
import okhttp3.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;

public class ApiClient {

    private final ZyrexRanked plugin;
    private final OkHttpClient http;
    private final Gson gson = new Gson();
    private static final MediaType JSON = MediaType.get("application/json; charset=utf-8");

    public ApiClient(ZyrexRanked plugin) {
        this.plugin = plugin;
        this.http = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build();
    }

    private String apiUrl() { return plugin.getConfig().getString("api.url", ""); }
    private String apiSecret() { return plugin.getConfig().getString("api.secret", ""); }

    // ─────────────────────────────────────────────────────────────────
    // POST match result to API
    // ─────────────────────────────────────────────────────────────────
    public void postMatchResult(Match match, RankedPlayer rp1, RankedPlayer rp2) {
        JsonObject body = new JsonObject();
        body.addProperty("matchId", match.getMatchId());
        body.addProperty("gamemode", match.getGamemode().getId());
        body.addProperty("result", match.getResult().name());
        body.addProperty("durationSeconds", match.getDurationSeconds());
        body.addProperty("season", plugin.getConfig().getInt("seasons.current-season", 1));

        JsonObject p1obj = new JsonObject();
        p1obj.addProperty("uuid", rp1.getUuid().toString());
        p1obj.addProperty("username", rp1.getUsername());
        p1obj.addProperty("eloStart", match.getPlayer1EloStart());
        p1obj.addProperty("eloDelta", match.getPlayer1EloDelta());
        p1obj.addProperty("eloNew", rp1.getElo(match.getGamemode()));
        p1obj.addProperty("wins", rp1.getWins(match.getGamemode()));
        p1obj.addProperty("losses", rp1.getLosses(match.getGamemode()));
        body.add("player1", p1obj);

        JsonObject p2obj = new JsonObject();
        p2obj.addProperty("uuid", rp2.getUuid().toString());
        p2obj.addProperty("username", rp2.getUsername());
        p2obj.addProperty("eloStart", match.getPlayer2EloStart());
        p2obj.addProperty("eloDelta", match.getPlayer2EloDelta());
        p2obj.addProperty("eloNew", rp2.getElo(match.getGamemode()));
        p2obj.addProperty("wins", rp2.getWins(match.getGamemode()));
        p2obj.addProperty("losses", rp2.getLosses(match.getGamemode()));
        body.add("player2", p2obj);

        post("/api/match/report", body.toString());
    }

    // ─────────────────────────────────────────────────────────────────
    // Sync full leaderboard to API cache (every 30s)
    // ─────────────────────────────────────────────────────────────────
    public void syncLeaderboard() {
        JsonObject body = new JsonObject();
        body.addProperty("secret", apiSecret());
        // Signal API to refresh its Redis cache from MySQL
        body.addProperty("action", "refresh_leaderboard");
        post("/api/internal/sync", body.toString());
    }

    // ─────────────────────────────────────────────────────────────────
    // HTTP helper
    // ─────────────────────────────────────────────────────────────────
    private void post(String path, String jsonBody) {
        Request request = new Request.Builder()
                .url(apiUrl() + path)
                .addHeader("X-API-Secret", apiSecret())
                .addHeader("Content-Type", "application/json")
                .post(RequestBody.create(jsonBody, JSON))
                .build();

        http.newCall(request).enqueue(new Callback() {
            @Override public void onFailure(Call call, IOException e) {
                plugin.getLogger().log(Level.WARNING, "API call failed [" + path + "]: " + e.getMessage());
            }
            @Override public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    plugin.getLogger().warning("API call [" + path + "] returned HTTP " + response.code());
                }
                response.close();
            }
        });
    }
}
