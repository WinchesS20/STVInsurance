package org.sovereigntv.stvinsurance.data;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;
import org.sovereigntv.stvinsurance.Main;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class DataManager {

    private final Main plugin;
    private final Map<UUID, PlayerData> playerDataCache;
    private final File dataFolder;

    public DataManager(Main plugin) {
        this.plugin = plugin;
        this.playerDataCache = new ConcurrentHashMap<>();
        this.dataFolder = new File(plugin.getDataFolder(), "playerdata");

        if (!dataFolder.exists()) {
            dataFolder.mkdirs();
        }
    }

    public void loadAllData() {
        playerDataCache.clear();

        File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files == null) return;

        for (File file : files) {
            try {
                String uuidStr = file.getName().replace(".yml", "");
                UUID uuid = UUID.fromString(uuidStr);
                loadPlayerData(uuid);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid data file: " + file.getName());
            }
        }

        plugin.getLogger().info("Loaded player data: " + playerDataCache.size());
    }

    public void saveAllData() {
        for (UUID uuid : playerDataCache.keySet()) {
            savePlayerData(uuid);
        }
    }

    public PlayerData getPlayerData(UUID uuid) {
        if (!playerDataCache.containsKey(uuid)) {
            loadPlayerData(uuid);
        }
        return playerDataCache.computeIfAbsent(uuid, k -> new PlayerData(uuid));
    }

    public void loadPlayerData(UUID uuid) {
        File file = new File(dataFolder, uuid.toString() + ".yml");

        if (!file.exists()) {
            playerDataCache.put(uuid, new PlayerData(uuid));
            return;
        }

        FileConfiguration config = YamlConfiguration.loadConfiguration(file);
        PlayerData data = new PlayerData(uuid);

        data.setBonusSlots(config.getInt("bonus-slots", 0));

        ConfigurationSection insuredSection = config.getConfigurationSection("insured-items");
        if (insuredSection != null) {
            for (String key : insuredSection.getKeys(false)) {
                ItemStack item = insuredSection.getItemStack(key);
                if (item != null) {
                    data.addInsuredItem(item);
                }
            }
        }

        ConfigurationSection pendingSection = config.getConfigurationSection("pending-items");
        if (pendingSection != null) {
            for (String key : pendingSection.getKeys(false)) {
                ConfigurationSection itemSection = pendingSection.getConfigurationSection(key);
                if (itemSection != null) {
                    ItemStack item = itemSection.getItemStack("item");
                    long deathTime = itemSection.getLong("death-time");
                    double cost = itemSection.getDouble("cost");
                    double pointsCost = itemSection.getDouble("points-cost", 0);
                    String deathReason = itemSection.getString("death-reason", "Неизвестно");

                    UUID killerUuid = null;
                    String killerUuidStr = itemSection.getString("killer-uuid");
                    if (killerUuidStr != null && !killerUuidStr.isEmpty()) {
                        try { killerUuid = UUID.fromString(killerUuidStr); } catch (IllegalArgumentException ignored) {}
                    }

                    String killerName = itemSection.getString("killer-name");

                    if (item != null) {
                        data.addPendingItem(new PendingItem(item, deathTime, cost, pointsCost, deathReason, killerUuid, killerName));
                    }
                }
            }
        }

        ConfigurationSection returnedSection = config.getConfigurationSection("returned-items");
        if (returnedSection != null) {
            for (String key : returnedSection.getKeys(false)) {
                ConfigurationSection itemSection = returnedSection.getConfigurationSection(key);
                if (itemSection != null) {
                    ItemStack item = itemSection.getItemStack("item");
                    long receivedTime = itemSection.getLong("received-time");
                    String fromPlayerName = itemSection.getString("from-player-name", "Неизвестный");

                    UUID fromPlayerUuid = null;
                    String fromPlayerUuidStr = itemSection.getString("from-player-uuid");
                    if (fromPlayerUuidStr != null && !fromPlayerUuidStr.isEmpty()) {
                        try { fromPlayerUuid = UUID.fromString(fromPlayerUuidStr); } catch (IllegalArgumentException ignored) {}
                    }

                    if (item != null) {
                        data.addReturnedItem(new ReturnedItem(item, receivedTime, fromPlayerName, fromPlayerUuid));
                    }
                }
            }
        }

        playerDataCache.put(uuid, data);
    }

    public void savePlayerData(UUID uuid) {
        PlayerData data = playerDataCache.get(uuid);
        if (data == null) return;

        File file = new File(dataFolder, uuid.toString() + ".yml");
        FileConfiguration config = new YamlConfiguration();

        config.set("bonus-slots", data.getBonusSlots());

        List<ItemStack> insuredItems = data.getInsuredItems();
        for (int i = 0; i < insuredItems.size(); i++) {
            config.set("insured-items." + i, insuredItems.get(i));
        }

        List<PendingItem> pendingItems = data.getPendingItems();
        for (int i = 0; i < pendingItems.size(); i++) {
            PendingItem pending = pendingItems.get(i);
            String path = "pending-items." + i;
            config.set(path + ".item", pending.getItem());
            config.set(path + ".death-time", pending.getDeathTime());
            config.set(path + ".cost", pending.getCost());
            config.set(path + ".points-cost", pending.getPointsCost());
            config.set(path + ".death-reason", pending.getDeathReason());
            config.set(path + ".killer-uuid", pending.getKillerUuid() != null ? pending.getKillerUuid().toString() : "");
            config.set(path + ".killer-name", pending.getKillerName() != null ? pending.getKillerName() : "");
        }

        List<ReturnedItem> returnedItems = data.getReturnedItems();
        for (int i = 0; i < returnedItems.size(); i++) {
            ReturnedItem returned = returnedItems.get(i);
            String path = "returned-items." + i;
            config.set(path + ".item", returned.getItem());
            config.set(path + ".received-time", returned.getReceivedTime());
            config.set(path + ".from-player-name", returned.getFromPlayerName());
            config.set(path + ".from-player-uuid", returned.getFromPlayerUuid() != null ? returned.getFromPlayerUuid().toString() : "");
        }

        try {
            config.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Failed to save player data: " + uuid);
            e.printStackTrace();
        }
    }

    public void deletePlayerData(UUID uuid) {
        playerDataCache.remove(uuid);
        File file = new File(dataFolder, uuid.toString() + ".yml");
        if (file.exists()) {
            file.delete();
        }
    }

    public Set<UUID> getAllPlayerUuids() {
        Set<UUID> uuids = new HashSet<>(playerDataCache.keySet());

        File[] files = dataFolder.listFiles((dir, name) -> name.endsWith(".yml"));
        if (files != null) {
            for (File file : files) {
                try {
                    uuids.add(UUID.fromString(file.getName().replace(".yml", "")));
                } catch (IllegalArgumentException ignored) {}
            }
        }

        return uuids;
    }

    public boolean hasPlayerData(UUID uuid) {
        if (playerDataCache.containsKey(uuid)) return true;
        return new File(dataFolder, uuid.toString() + ".yml").exists();
    }
}
