package org.sovereigntv.stvinsurance;

import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.sovereigntv.stvinsurance.commands.InsuranceCommand;
import org.sovereigntv.stvinsurance.commands.InsuranceAdminCommand;
import org.sovereigntv.stvinsurance.compat.VersionHelper;
import org.sovereigntv.stvinsurance.data.DataManager;
import org.sovereigntv.stvinsurance.listeners.DeathListener;
import org.sovereigntv.stvinsurance.listeners.InventoryListener;
import org.sovereigntv.stvinsurance.listeners.PlayerListener;
import org.sovereigntv.stvinsurance.managers.ConfigManager;
import org.sovereigntv.stvinsurance.managers.InsuranceManager;
import org.sovereigntv.stvinsurance.managers.MenuManager;
import org.sovereigntv.stvinsurance.utils.MessageUtils;

import net.milkbowl.vault.economy.Economy;
import org.black_ixx.playerpoints.PlayerPoints;
import org.black_ixx.playerpoints.PlayerPointsAPI;

public class Main extends JavaPlugin {

    private static Main instance;
    private ConfigManager configManager;
    private DataManager dataManager;
    private InsuranceManager insuranceManager;
    private MenuManager menuManager;
    private MessageUtils messageUtils;

    private Economy economy;
    private boolean vaultEnabled = false;

    private PlayerPointsAPI playerPointsAPI;
    private boolean playerPointsEnabled = false;

    @Override
    public void onEnable() {
        instance = this;

        getLogger().info("==================================================");
        getLogger().info("STVInsurance v" + getDescription().getVersion());
        getLogger().info("Multi-version support: 1.16.5 - 1.21.9+");
        getLogger().info("Detected server version: " + VersionHelper.getVersionString());
        getLogger().info("==================================================");

        if (VersionHelper.isBelow(1, 16)) {
            getLogger().severe("This plugin requires Minecraft 1.16.5 or higher!");
            getLogger().severe("Detected version: " + VersionHelper.getVersionString());
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        saveDefaultConfig();

        this.configManager = new ConfigManager(this);
        this.messageUtils = new MessageUtils(this);
        this.dataManager = new DataManager(this);
        this.insuranceManager = new InsuranceManager(this);
        this.menuManager = new MenuManager(this);

        dataManager.loadAllData();

        if (getServer().getPluginManager().getPlugin("Vault") != null) {
            setupEconomy();
        }

        if (getServer().getPluginManager().getPlugin("PlayerPoints") != null) {
            setupPlayerPoints();
        }

        getCommand("insurance").setExecutor(new InsuranceCommand(this));
        getCommand("insuranceadmin").setExecutor(new InsuranceAdminCommand(this));

        getServer().getPluginManager().registerEvents(new DeathListener(this), this);
        getServer().getPluginManager().registerEvents(new InventoryListener(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);

        startTasks();

        getLogger().info("STVInsurance enabled successfully!");

        if (vaultEnabled) {
            getLogger().info("Vault hooked!");
        } else {
            getLogger().warning("Vault not found. Economy disabled.");
        }

        if (playerPointsEnabled) {
            getLogger().info("PlayerPoints hooked!");
        } else {
            getLogger().warning("PlayerPoints not found. Donate currency disabled.");
        }
    }

    @Override
    public void onDisable() {
        if (dataManager != null) {
            dataManager.saveAllData();
        }

        Bukkit.getOnlinePlayers().forEach(player -> {
            if (player.getOpenInventory() != null) {
                String title = getInventoryTitle(player);
                if (title != null && title.contains("Страхов")) {
                    player.closeInventory();
                }
            }
        });

        getLogger().info("STVInsurance disabled!");
    }

    private String getInventoryTitle(org.bukkit.entity.Player player) {
        try {
            return player.getOpenInventory().getTitle();
        } catch (Exception e) {
            return "";
        }
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        vaultEnabled = economy != null;
        return vaultEnabled;
    }

    private boolean setupPlayerPoints() {
        if (getServer().getPluginManager().getPlugin("PlayerPoints") == null) {
            return false;
        }
        PlayerPoints playerPoints = (PlayerPoints) getServer().getPluginManager().getPlugin("PlayerPoints");
        if (playerPoints == null) {
            return false;
        }
        playerPointsAPI = playerPoints.getAPI();
        playerPointsEnabled = playerPointsAPI != null;
        return playerPointsEnabled;
    }

    private void startTasks() {
        int saveInterval = configManager.getAutoSaveInterval() * 60 * 20;
        Bukkit.getScheduler().runTaskTimerAsynchronously(this, () -> dataManager.saveAllData(), saveInterval, saveInterval);

        int expiryInterval = configManager.getExpiryCheckInterval() * 20;
        Bukkit.getScheduler().runTaskTimer(this, () -> insuranceManager.checkExpiredInsurances(), expiryInterval, expiryInterval);
    }

    public void reload() {
        reloadConfig();
        configManager.reload();
        dataManager.saveAllData();
        dataManager.loadAllData();
    }

    public static Main getInstance() { return instance; }
    public ConfigManager getConfigManager() { return configManager; }
    public DataManager getDataManager() { return dataManager; }
    public InsuranceManager getInsuranceManager() { return insuranceManager; }
    public MenuManager getMenuManager() { return menuManager; }
    public MessageUtils getMessageUtils() { return messageUtils; }
    public Economy getEconomy() { return economy; }
    public boolean isVaultEnabled() { return vaultEnabled; }
    public PlayerPointsAPI getPlayerPointsAPI() { return playerPointsAPI; }
    public boolean isPlayerPointsEnabled() { return playerPointsEnabled; }
}
