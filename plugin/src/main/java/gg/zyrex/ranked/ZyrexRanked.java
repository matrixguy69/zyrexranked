package gg.zyrex.ranked;

import gg.zyrex.ranked.api.ApiClient;
import gg.zyrex.ranked.commands.*;
import gg.zyrex.ranked.listeners.*;
import gg.zyrex.ranked.managers.*;
import gg.zyrex.ranked.utils.DatabaseManager;
import gg.zyrex.ranked.utils.ZyrexPlaceholderExpansion;
import org.bukkit.plugin.java.JavaPlugin;

public class ZyrexRanked extends JavaPlugin {

    private static ZyrexRanked instance;
    private DatabaseManager databaseManager;
    private EloManager eloManager;
    private MatchManager matchManager;
    private QueueManager queueManager;
    private RankManager rankManager;
    private ArenaManager arenaManager;
    private ApiClient apiClient;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();

        getLogger().info("=================================");
        getLogger().info("   Zyrex Ranked v" + getDescription().getVersion());
        getLogger().info("=================================");

        this.databaseManager = new DatabaseManager(this);
        if (!databaseManager.connect()) {
            getLogger().severe("Failed to connect to database! Disabling.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        databaseManager.createTables();

        this.eloManager   = new EloManager(this);
        this.rankManager  = new RankManager(this);
        this.arenaManager = new ArenaManager(this);
        this.matchManager = new MatchManager(this);
        this.queueManager = new QueueManager(this);
        this.apiClient    = new ApiClient(this);

        getCommand("ranked").setExecutor(new RankedCommand(this));
        getCommand("rankedadmin").setExecutor(new RankedAdminCommand(this));
        getCommand("challenge").setExecutor(new ChallengeCommand(this));
        getCommand("spectate").setExecutor(new SpectateCommand(this));
        getCommand("queue").setExecutor(new QueueCommand(this));
        getCommand("leavequeue").setExecutor(new LeaveQueueCommand(this));

        getServer().getPluginManager().registerEvents(new PlayerJoinListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerQuitListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerDeathListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryListener(this), this);
        getServer().getPluginManager().registerEvents(new ChatListener(this), this);

        StrikePracticeListener spListener = new StrikePracticeListener(this);
        if (getServer().getPluginManager().getPlugin("StrikePractice") != null) {
            getServer().getPluginManager().registerEvents(spListener, this);
            getLogger().info("StrikePractice detected - ELO hook active.");
        } else {
            getLogger().warning("StrikePractice not found - using internal match system.");
        }

        if (getServer().getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new ZyrexPlaceholderExpansion(this).register();
            getLogger().info("PlaceholderAPI hooked.");
        }

        getServer().getScheduler().runTaskTimerAsynchronously(this,
                () -> queueManager.processQueue(), 40L, 40L);
        getServer().getScheduler().runTaskTimerAsynchronously(this,
                () -> apiClient.syncLeaderboard(), 600L, 600L);

        getLogger().info("Zyrex Ranked enabled!");
    }

    @Override
    public void onDisable() {
        if (matchManager != null) matchManager.endAllMatchesOnShutdown();
        if (databaseManager != null) databaseManager.disconnect();
        getLogger().info("Zyrex Ranked disabled.");
    }

    public String msg(String path) {
        return getConfig().getString("messages.prefix", "§8[§dZyrex§8] ")
             + getConfig().getString(path, "");
    }

    public static ZyrexRanked getInstance()        { return instance; }
    public DatabaseManager getDatabaseManager()    { return databaseManager; }
    public EloManager getEloManager()              { return eloManager; }
    public MatchManager getMatchManager()          { return matchManager; }
    public QueueManager getQueueManager()          { return queueManager; }
    public RankManager getRankManager()            { return rankManager; }
    public ArenaManager getArenaManager()          { return arenaManager; }
    public ApiClient getApiClient()                { return apiClient; }
}
