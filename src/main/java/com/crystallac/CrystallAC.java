package com.crystallac;

import com.crystallac.adapter.VersionAdapter;
import com.crystallac.adapter.VersionAdapterFactory;
import com.crystallac.check.CheckManager;
import com.crystallac.command.AntiCheatCommand;
import com.crystallac.data.PlayerDataManager;
import com.crystallac.listener.BukkitEventListener;
import com.crystallac.listener.PacketListenerProtocolLib;
import com.crystallac.ml.BanDatasetRepository;
import com.crystallac.ml.IncrementalModelTuner;
import com.crystallac.punishment.PunishmentManager;
import com.crystallac.punishment.ShadowBanManager;
import com.crystallac.punishment.ViolationManager;
import com.crystallac.statistical.AnomalyDetector;
import com.crystallac.statistical.GlobalBaselineTracker;
import com.crystallac.storage.AuditLogger;
import com.crystallac.storage.DatabaseManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

/**
 * Main entry point for Crystall Anti-Cheat plugin.
 */
public class CrystallAC extends JavaPlugin {

    private VersionAdapter versionAdapter;
    private DatabaseManager databaseManager;
    private AuditLogger auditLogger;
    private BanDatasetRepository banDatasetRepository;
    private IncrementalModelTuner modelTuner;
    private GlobalBaselineTracker globalBaselineTracker;
    private AnomalyDetector anomalyDetector;
    private PlayerDataManager dataManager;
    private ShadowBanManager shadowBanManager;
    private PunishmentManager punishmentManager;
    private ViolationManager violationManager;
    private CheckManager checkManager;

    @Override
    public void onEnable() {
        // 1. Save and load configuration
        saveDefaultConfig();

        // 2. Multi-version Adapter initialization
        this.versionAdapter = VersionAdapterFactory.createAdapter();
        getLogger().info("[CrystallAC] Loaded VersionAdapter for Minecraft " + versionAdapter.getVersionName());

        // 3. Database & Storage Initialization
        this.databaseManager = new DatabaseManager(getDataFolder(), getLogger());
        this.databaseManager.initialize();
        this.auditLogger = new AuditLogger(this, databaseManager);

        // 4. ML Dataset & Incremental Tuner
        this.banDatasetRepository = new BanDatasetRepository(databaseManager, getLogger());
        this.modelTuner = new IncrementalModelTuner(getConfig(), banDatasetRepository, getLogger());
        this.modelTuner.evaluateAndRetrain();

        // 5. Unsupervised Statistical Baseline & Anomaly Detection
        this.globalBaselineTracker = new GlobalBaselineTracker();
        double zThreshold = getConfig().getDouble("statistical_layer.zscore_threshold", 3.2);
        this.anomalyDetector = new AnomalyDetector(zThreshold, globalBaselineTracker);

        // 6. Player Data & State Management
        this.dataManager = new PlayerDataManager(getConfig());

        // 7. Punishments & Violations
        this.shadowBanManager = new ShadowBanManager(this);
        this.punishmentManager = new PunishmentManager(this, shadowBanManager);
        this.violationManager = new ViolationManager(this, dataManager, punishmentManager, auditLogger, anomalyDetector);

        // 8. Heuristic Checks Initialization
        this.checkManager = new CheckManager(this);

        // 9. Register Bukkit Listeners
        getServer().getPluginManager().registerEvents(new BukkitEventListener(this), this);

        // 10. Register ProtocolLib Packet Interceptor
        if (getServer().getPluginManager().getPlugin("ProtocolLib") != null) {
            new PacketListenerProtocolLib(this).register();
            getLogger().info("[CrystallAC] ProtocolLib hooks successfully attached.");
        } else {
            getLogger().warning("[CrystallAC] ProtocolLib not detected! Falling back to Bukkit event sampling.");
        }

        // 11. Register Commands
        AntiCheatCommand acCommand = new AntiCheatCommand(this);
        if (getCommand("anticheat") != null) {
            getCommand("anticheat").setExecutor(acCommand);
            getCommand("anticheat").setTabCompleter(acCommand);
        }

        // 12. Periodic per-tick task for data management
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            dataManager.tickAll();
        }, 1L, 1L);

        getLogger().info("[CrystallAC] Successfully activated! Zero-dataset heuristic core & real-time anomaly engine running.");
    }

    @Override
    public void onDisable() {
        if (auditLogger != null) {
            auditLogger.shutdown();
        }
        if (databaseManager != null) {
            databaseManager.close();
        }
        getLogger().info("[CrystallAC] Plugin disabled.");
    }

    // Getters for subsystems
    public VersionAdapter getVersionAdapter() { return versionAdapter; }
    public DatabaseManager getDatabaseManager() { return databaseManager; }
    public AuditLogger getAuditLogger() { return auditLogger; }
    public BanDatasetRepository getBanDatasetRepository() { return banDatasetRepository; }
    public IncrementalModelTuner getModelTuner() { return modelTuner; }
    public GlobalBaselineTracker getGlobalBaselineTracker() { return globalBaselineTracker; }
    public AnomalyDetector getAnomalyDetector() { return anomalyDetector; }
    public PlayerDataManager getDataManager() { return dataManager; }
    public ShadowBanManager getShadowBanManager() { return shadowBanManager; }
    public PunishmentManager getPunishmentManager() { return punishmentManager; }
    public ViolationManager getViolationManager() { return violationManager; }
    public CheckManager getCheckManager() { return checkManager; }
}
