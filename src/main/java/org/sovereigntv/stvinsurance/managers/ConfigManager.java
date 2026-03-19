package org.sovereigntv.stvinsurance.managers;

import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.sovereigntv.stvinsurance.Main;
import org.sovereigntv.stvinsurance.compat.SoundCompat;

import java.util.*;

public class ConfigManager {

    private final Main plugin;
    private FileConfiguration config;

    private int redemptionTime;
    private int defaultSlots;
    private int maxSlots;
    private int autoSaveInterval;
    private int expiryCheckInterval;

    private boolean useVault;
    private double baseCost;
    private double enchantmentMultiplier;
    private double maxCost;
    private Map<Material, Double> customCosts;

    private boolean usePlayerPoints;
    private double basePointsCost;
    private double pointsEnchantmentMultiplier;
    private double maxPointsCost;
    private Map<Material, Double> customPointsCosts;

    private double insuranceCost;
    private String insuranceLoreMark;

    private Set<String> allowedCategories;
    private Set<Material> additionalAllowed;
    private Set<Material> blacklisted;

    private boolean soundsEnabled;
    private Map<String, SoundData> sounds;

    public ConfigManager(Main plugin) {
        this.plugin = plugin;
        this.customCosts = new HashMap<>();
        this.customPointsCosts = new HashMap<>();
        this.allowedCategories = new HashSet<>();
        this.additionalAllowed = new HashSet<>();
        this.blacklisted = new HashSet<>();
        this.sounds = new HashMap<>();
        reload();
    }

    public void reload() {
        plugin.reloadConfig();
        config = plugin.getConfig();
        loadSettings();
        loadCosts();
        loadPointsCosts();
        loadAllowedItems();
        loadSounds();
    }

    private void loadSettings() {
        redemptionTime = config.getInt("settings.redemption-time", 7200);
        defaultSlots = config.getInt("settings.default-slots", 1);
        maxSlots = config.getInt("settings.max-slots", 5);
        autoSaveInterval = config.getInt("settings.auto-save-interval", 5);
        expiryCheckInterval = config.getInt("settings.expiry-check-interval", 60);
        insuranceCost = config.getDouble("settings.insurance-cost", 1000);
        insuranceLoreMark = config.getString("settings.insurance-lore-mark", "&c&lПредмет застрахован");
    }

    private void loadCosts() {
        useVault = config.getBoolean("redemption-cost.use-vault", true);
        baseCost = config.getDouble("redemption-cost.base-cost", 1000);
        enchantmentMultiplier = config.getDouble("redemption-cost.enchantment-multiplier", 100);
        maxCost = config.getDouble("redemption-cost.max-cost", 50000);

        customCosts.clear();
        ConfigurationSection customSection = config.getConfigurationSection("redemption-cost.custom-costs");
        if (customSection != null) {
            for (String key : customSection.getKeys(false)) {
                try {
                    customCosts.put(Material.valueOf(key.toUpperCase()), customSection.getDouble(key));
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    private void loadPointsCosts() {
        usePlayerPoints = config.getBoolean("redemption-cost-points.enabled", true);
        basePointsCost = config.getDouble("redemption-cost-points.base-cost", 100);
        pointsEnchantmentMultiplier = config.getDouble("redemption-cost-points.enchantment-multiplier", 10);
        maxPointsCost = config.getDouble("redemption-cost-points.max-cost", 5000);

        customPointsCosts.clear();
        ConfigurationSection customSection = config.getConfigurationSection("redemption-cost-points.custom-costs");
        if (customSection != null) {
            for (String key : customSection.getKeys(false)) {
                try {
                    customPointsCosts.put(Material.valueOf(key.toUpperCase()), customSection.getDouble(key));
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }

    private void loadAllowedItems() {
        allowedCategories.clear();
        additionalAllowed.clear();
        blacklisted.clear();

        allowedCategories.addAll(config.getStringList("allowed-items.categories"));

        for (String item : config.getStringList("allowed-items.additional")) {
            try { additionalAllowed.add(Material.valueOf(item.toUpperCase())); } catch (IllegalArgumentException ignored) {}
        }

        for (String item : config.getStringList("blacklisted-items")) {
            try { blacklisted.add(Material.valueOf(item.toUpperCase())); } catch (IllegalArgumentException ignored) {}
        }
    }

    private void loadSounds() {
        soundsEnabled = config.getBoolean("sounds.enabled", true);
        sounds.clear();

        String[] soundKeys = {"insure-item", "uninsure-item", "redeem-item", "open-menu", "error", "item-expired", "item-received"};
        for (String key : soundKeys) {
            String path = "sounds." + key;
            try {
                String soundName = config.getString(path + ".sound", "ENTITY_EXPERIENCE_ORB_PICKUP");
                Sound sound = SoundCompat.getSound(soundName);
                float volume = (float) config.getDouble(path + ".volume", 1.0);
                float pitch = (float) config.getDouble(path + ".pitch", 1.0);
                sounds.put(key, new SoundData(sound, volume, pitch));
            } catch (IllegalArgumentException ignored) {
                sounds.put(key, new SoundData(Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f));
            }
        }
    }

    public boolean isItemAllowed(Material material) {
        if (blacklisted.contains(material)) return false;
        if (additionalAllowed.contains(material)) return true;
        if (allowedCategories.isEmpty()) return true;

        String name = material.name();

        if (allowedCategories.contains("TOOLS") &&
                (name.endsWith("_PICKAXE") || name.endsWith("_AXE") || name.endsWith("_SHOVEL") || name.endsWith("_HOE")))
            return true;

        if (allowedCategories.contains("WEAPONS") &&
                (name.endsWith("_SWORD") || name.equals("BOW") || name.equals("CROSSBOW") || name.equals("MACE")))
            return true;

        if (allowedCategories.contains("ARMOR") &&
                (name.endsWith("_HELMET") || name.endsWith("_CHESTPLATE") || name.endsWith("_LEGGINGS") || name.endsWith("_BOOTS")))
            return true;

        if (allowedCategories.contains("TOTEMS") && material == Material.TOTEM_OF_UNDYING) return true;
        if (allowedCategories.contains("ELYTRA") && material == Material.ELYTRA) return true;
        if (allowedCategories.contains("TRIDENTS") && material == Material.TRIDENT) return true;

        return false;
    }

    public double calculateRedemptionCost(Material material, int totalEnchantmentLevel) {
        double cost = customCosts.containsKey(material) ? customCosts.get(material) : baseCost;
        cost += enchantmentMultiplier * totalEnchantmentLevel;
        return Math.min(cost, maxCost);
    }

    public double calculateRedemptionPointsCost(Material material, int totalEnchantmentLevel) {
        double cost = customPointsCosts.containsKey(material) ? customPointsCosts.get(material) : basePointsCost;
        cost += pointsEnchantmentMultiplier * totalEnchantmentLevel;
        return Math.min(cost, maxPointsCost);
    }

    public String getGuiTitle(String path) { return config.getString("gui." + path + ".title", "&8Menu"); }
    public int getGuiSize(String path) { return config.getInt("gui." + path + ".size", 54); }
    public ConfigurationSection getGuiSection(String path) { return config.getConfigurationSection("gui." + path); }

    public String getMessage(String key) { return config.getString("messages." + key, "&cMessage not found: " + key); }
    public String getPrefix() { return config.getString("messages.prefix", "&8[&6Страховка&8] &r"); }
    public String getTimeFormat(String key) { return config.getString("messages.time-format." + key, "%s%сек"); }
    public String getDeathReason(String key) { return config.getString("messages.death-reasons." + key, "Другое"); }

    public int getRedemptionTime() { return redemptionTime; }
    public int getDefaultSlots() { return defaultSlots; }
    public int getMaxSlots() { return maxSlots; }
    public int getAutoSaveInterval() { return autoSaveInterval; }
    public int getExpiryCheckInterval() { return expiryCheckInterval; }
    public boolean isUseVault() { return useVault; }
    public boolean isUsePlayerPoints() { return usePlayerPoints; }
    public double getInsuranceCost() { return insuranceCost; }
    public String getInsuranceLoreMark() { return insuranceLoreMark; }
    public boolean isSoundsEnabled() { return soundsEnabled; }
    public SoundData getSound(String key) { return sounds.get(key); }
    public FileConfiguration getConfig() { return config; }

    public static class SoundData {
        private final Sound sound;
        private final float volume;
        private final float pitch;

        public SoundData(Sound sound, float volume, float pitch) {
            this.sound = sound;
            this.volume = volume;
            this.pitch = pitch;
        }

        public Sound getSound() { return sound; }
        public float getVolume() { return volume; }
        public float getPitch() { return pitch; }
    }
}
