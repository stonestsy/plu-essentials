package com.plu.plugin;

import com.plu.plugin.commands.admin.*;
import com.plu.plugin.commands.user.*;
import com.plu.plugin.listeners.ClaimListener;
import com.plu.plugin.listeners.CommandSignListener;
import com.plu.plugin.listeners.LoginListener;
import com.plu.plugin.listeners.MarketListener;
import com.plu.plugin.listeners.MenuListener;
import com.plu.plugin.listeners.NpcListener;
import com.plu.plugin.listeners.PlayerListener;
import com.plu.plugin.managers.*;
import com.plu.plugin.storage.DatabaseManager;
import com.plu.plugin.web.WebServer;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class Main extends JavaPlugin {

    private static Main instance;

    private HomeManager homeManager;
    private TpaManager tpaManager;
    private KitManager kitManager;
    private GodManager godManager;
    private VanishManager vanishManager;
    private BackManager backManager;
    private SpawnManager spawnManager;
    private BanManager banManager;
    private WarpManager warpManager;
    private TaskManager taskManager;
    private IpBanManager ipBanManager;
    private WhitelistManager whitelistManager;
    private ChatLogManager chatLogManager;
    private PlayerStatsManager playerStatsManager;
    private EconomyManager economyManager;
    private GroupManager groupManager;
    private MarketManager marketManager;
    private ClaimManager claimManager;
    private LoginManager loginManager;
    private AnnouncementManager announcementManager;
    private TabManager tabManager;
    private RedeemManager redeemManager;
    private NicknameManager nicknameManager;
    private MenuManager menuManager;
    private NpcManager npcManager;
    private SignManager signManager;
    private RechargeManager rechargeManager;
    private DatabaseManager databaseManager;
    private WebServer webServer;
    private long enableTime;

    @Override
    public void onEnable() {
        instance = this;
        enableTime = System.currentTimeMillis();
        saveDefaultConfig();

        // Database (MySQL) - must be before managers that use it
        this.databaseManager = new DatabaseManager(this);
        databaseManager.connect();

        // Managers
        this.homeManager = new HomeManager(this, databaseManager);
        this.tpaManager = new TpaManager(this);
        this.kitManager = new KitManager(this);
        this.godManager = new GodManager();
        this.vanishManager = new VanishManager(this);
        this.backManager = new BackManager(this);
        this.spawnManager = new SpawnManager(this);
        this.banManager = new BanManager(this, databaseManager);
        this.warpManager = new WarpManager(this, databaseManager);
        this.taskManager = new TaskManager(this, databaseManager);
        this.ipBanManager = new IpBanManager(this, databaseManager);
        this.whitelistManager = new WhitelistManager(this, databaseManager);
        this.chatLogManager = new ChatLogManager(this, databaseManager);
        this.playerStatsManager = new PlayerStatsManager(this, databaseManager);
        this.economyManager = new EconomyManager(this, databaseManager);
        this.groupManager = new GroupManager(this, databaseManager);
        this.marketManager = new MarketManager(this, databaseManager);
        this.claimManager = new ClaimManager(this, databaseManager);
        this.loginManager = new LoginManager(this, databaseManager);
        this.announcementManager = new AnnouncementManager(this);
        this.tabManager = new TabManager(this);
        this.redeemManager = new RedeemManager(this, databaseManager);
        this.nicknameManager = new NicknameManager(this, databaseManager);
        this.menuManager = new MenuManager(this);
        this.npcManager = new NpcManager(this, databaseManager);
        this.signManager = new SignManager(this, databaseManager);
        this.rechargeManager = new RechargeManager(this, databaseManager);

        homeManager.load();
        kitManager.load();
        spawnManager.load();
        banManager.load();
        warpManager.load();
        taskManager.load();
        ipBanManager.load();
        whitelistManager.load();
        chatLogManager.load();
        playerStatsManager.load();
        economyManager.load();
        groupManager.load();
        marketManager.load();
        claimManager.load();
        loginManager.load();
        announcementManager.load();
        redeemManager.load();
        nicknameManager.load();
        menuManager.load();
        npcManager.load();
        signManager.load();
        rechargeManager.load();
        npcManager.spawnAll();
        taskManager.start();
        tabManager.start();

        // 定期把内存中的余额写入 YAML（MySQL 模式为直写，无操作）
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            economyManager.flush();
            groupManager.flush();
        }, 1200L, 1200L);

        // Listeners
        Bukkit.getPluginManager().registerEvents(new PlayerListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MarketListener(this), this);
        Bukkit.getPluginManager().registerEvents(new ClaimListener(this), this);
        Bukkit.getPluginManager().registerEvents(new LoginListener(this), this);
        Bukkit.getPluginManager().registerEvents(new MenuListener(this), this);
        Bukkit.getPluginManager().registerEvents(new NpcListener(this), this);
        Bukkit.getPluginManager().registerEvents(new CommandSignListener(this), this);

        // User commands
        registerUserCommands();
        registerAdminCommands();

        // Web admin panel
        this.webServer = new WebServer(this);
        webServer.start();

        getLogger().info("PluEssentials v" + getDescription().getVersion() + " enabled.");
    }

    @Override
    public void onDisable() {
        if (webServer != null) webServer.stop();
        if (taskManager != null) taskManager.stop();
        if (tabManager != null) tabManager.stop();
        if (homeManager != null) homeManager.save();
        if (spawnManager != null) spawnManager.save();
        if (economyManager != null) economyManager.flush();
        if (groupManager != null) groupManager.flush();
        if (npcManager != null) npcManager.despawnAll();
        if (nicknameManager != null) nicknameManager.save();
        if (menuManager != null) menuManager.save();
        if (signManager != null) signManager.save();
        if (databaseManager != null) databaseManager.disconnect();
        getLogger().info("PluEssentials disabled.");
    }

    private void registerUserCommands() {
        getCommand("home").setExecutor(new HomeCommand(this));
        getCommand("sethome").setExecutor(new SetHomeCommand(this));
        getCommand("delhome").setExecutor(new DelHomeCommand(this));
        getCommand("homes").setExecutor(new HomesCommand(this));
        getCommand("tpa").setExecutor(new TpaCommand(this));
        getCommand("tpaccept").setExecutor(new TpacceptCommand(this));
        getCommand("tpdeny").setExecutor(new TpdenyCommand(this));
        getCommand("back").setExecutor(new BackCommand(this));
        getCommand("kit").setExecutor(new KitCommand(this));
        getCommand("spawn").setExecutor(new SpawnCommand(this));
        getCommand("setspawn").setExecutor(new SetSpawnCommand(this));
        getCommand("warp").setExecutor(new WarpCommand(this));
        getCommand("money").setExecutor(new MoneyCommand(this));
        getCommand("pay").setExecutor(new PayCommand(this));
        getCommand("baltop").setExecutor(new BaltopCommand(this));
        getCommand("market").setExecutor(new MarketCommand(this));
        getCommand("claim").setExecutor(new ClaimCommand(this));
        getCommand("rules").setExecutor(new RulesCommand(this));
        getCommand("news").setExecutor(new NewsCommand(this));
        getCommand("login").setExecutor(new LoginCommand(this));
        getCommand("register").setExecutor(new RegisterCommand(this));
        getCommand("redeem").setExecutor(new RedeemCommand(this));
        getCommand("nick").setExecutor(new NickCommand(this));
        getCommand("menu").setExecutor(new MenuCommand(this));
        getCommand("charge").setExecutor(new ChargeCommand(this));
    }

    private void registerAdminCommands() {
        getCommand("fly").setExecutor(new FlyCommand(this));
        getCommand("gamemode").setExecutor(new GamemodeCommand(this));
        getCommand("heal").setExecutor(new HealCommand(this));
        getCommand("feed").setExecutor(new FeedCommand(this));
        getCommand("god").setExecutor(new GodCommand(this));
        getCommand("vanish").setExecutor(new VanishCommand(this));
        getCommand("broadcast").setExecutor(new BroadcastCommand(this));
        getCommand("clear").setExecutor(new ClearCommand(this));
        getCommand("plureload").setExecutor(new ReloadCommand(this));
        getCommand("ban").setExecutor(new BanCommand(this));
        getCommand("tempban").setExecutor(new TempbanCommand(this));
        getCommand("unban").setExecutor(new UnbanCommand(this));
        getCommand("banlist").setExecutor(new BanlistCommand(this));
        getCommand("setwarp").setExecutor(new SetwarpCommand(this));
        getCommand("delwarp").setExecutor(new DelwarpCommand(this));
        getCommand("invsee").setExecutor(new InvseeCommand(this));
        getCommand("enderchest").setExecutor(new EnderchestCommand(this));
        getCommand("ipban").setExecutor(new IpBanCommand(this));
        getCommand("ipunban").setExecutor(new IpUnbanCommand(this));
        getCommand("ipbanlist").setExecutor(new IpBanlistCommand(this));
        getCommand("whitelist").setExecutor(new WhitelistCommand(this));
        getCommand("chatlog").setExecutor(new ChatlogCommand(this));
        getCommand("seen").setExecutor(new SeenCommand(this));
        getCommand("sudo").setExecutor(new SudoCommand(this));
        getCommand("speed").setExecutor(new SpeedCommand(this));
        getCommand("healall").setExecutor(new HealallCommand(this));
        getCommand("feedall").setExecutor(new FeedallCommand(this));
        getCommand("group").setExecutor(new GroupCommand(this));
        getCommand("npc").setExecutor(new NpcCommand(this));
        getCommand("cmdsign").setExecutor(new CommandSignCommand(this));
    }

    public void reload() {
        reloadConfig();
        homeManager.load();
        kitManager.load();
        spawnManager.load();
        banManager.load();
        warpManager.load();
        taskManager.load();
        ipBanManager.load();
        whitelistManager.load();
        chatLogManager.load();
        playerStatsManager.load();
        economyManager.load();
        groupManager.load();
        marketManager.load();
        claimManager.load();
        loginManager.load();
        announcementManager.reload();
        redeemManager.load();
        nicknameManager.load();
        menuManager.load();
        npcManager.despawnAll();
        npcManager.load();
        npcManager.spawnAll();
        signManager.load();
        rechargeManager.load();
        tabManager.start();
        if (webServer != null) webServer.reload();
    }

    public long getPluginTime() {
        return System.currentTimeMillis() - enableTime;
    }

    public static Main getInstance() { return instance; }
    public HomeManager getHomeManager() { return homeManager; }
    public TpaManager getTpaManager() { return tpaManager; }
    public KitManager getKitManager() { return kitManager; }
    public GodManager getGodManager() { return godManager; }
    public VanishManager getVanishManager() { return vanishManager; }
    public BackManager getBackManager() { return backManager; }
    public SpawnManager getSpawnManager() { return spawnManager; }
    public BanManager getBanManager() { return banManager; }
    public WarpManager getWarpManager() { return warpManager; }
    public TaskManager getTaskManager() { return taskManager; }
    public IpBanManager getIpBanManager() { return ipBanManager; }
    public WhitelistManager getWhitelistManager() { return whitelistManager; }
    public ChatLogManager getChatLogManager() { return chatLogManager; }
    public PlayerStatsManager getPlayerStatsManager() { return playerStatsManager; }
    public EconomyManager getEconomyManager() { return economyManager; }
    public GroupManager getGroupManager() { return groupManager; }
    public MarketManager getMarketManager() { return marketManager; }
    public ClaimManager getClaimManager() { return claimManager; }
    public LoginManager getLoginManager() { return loginManager; }
    public AnnouncementManager getAnnouncementManager() { return announcementManager; }
    public TabManager getTabManager() { return tabManager; }
    public RedeemManager getRedeemManager() { return redeemManager; }
    public NicknameManager getNicknameManager() { return nicknameManager; }
    public MenuManager getMenuManager() { return menuManager; }
    public NpcManager getNpcManager() { return npcManager; }
    public SignManager getSignManager() { return signManager; }
    public RechargeManager getRechargeManager() { return rechargeManager; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }

    public String getPrefix() {
        return getConfig().getString("prefix", "&7[&bPluEssentials&7] ");
    }
}
