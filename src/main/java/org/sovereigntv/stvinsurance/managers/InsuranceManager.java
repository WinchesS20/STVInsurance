package org.sovereigntv.stvinsurance.managers;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;
import org.bukkit.persistence.PersistentDataType;
import org.sovereigntv.stvinsurance.Main;
import org.sovereigntv.stvinsurance.data.PlayerData;
import org.sovereigntv.stvinsurance.data.PendingItem;
import org.sovereigntv.stvinsurance.data.ReturnedItem;
import org.sovereigntv.stvinsurance.utils.MessageUtils;

import java.util.*;

public class InsuranceManager {

    private final Main plugin;
    private final NamespacedKey insuranceKey;
    private final NamespacedKey insuranceOwnerKey;

    public InsuranceManager(Main plugin) {
        this.plugin = plugin;
        this.insuranceKey = new NamespacedKey(plugin, "insured");
        this.insuranceOwnerKey = new NamespacedKey(plugin, "insurance_owner");
    }

    public boolean insureItem(Player player, ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;

        if (!plugin.getConfigManager().isItemAllowed(item.getType())) {
            plugin.getMessageUtils().sendMessage(player, "item-not-allowed");
            return false;
        }

        if (isInsured(item)) {
            plugin.getMessageUtils().sendMessage(player, "item-already-insured");
            return false;
        }

        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        int totalInsured = data.getInsuredItems().size() + data.getPendingItems().size();
        if (totalInsured >= getMaxSlots(player)) {
            plugin.getMessageUtils().sendMessage(player, "no-available-slots");
            return false;
        }

        double insuranceCost = plugin.getConfigManager().getInsuranceCost();
        if (insuranceCost > 0 && plugin.isVaultEnabled()) {
            if (plugin.getEconomy().getBalance(player) < insuranceCost) {
                plugin.getMessageUtils().sendMessage(player, "not-enough-money-insure",
                        "%cost%", String.format("%.2f", insuranceCost));
                plugin.getMessageUtils().playSound(player, "error");
                return false;
            }
            plugin.getEconomy().withdrawPlayer(player, insuranceCost);
        }

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.set(insuranceKey, PersistentDataType.BYTE, (byte) 1);
        container.set(insuranceOwnerKey, PersistentDataType.STRING, player.getUniqueId().toString());

        List<String> lore = meta.getLore();
        if (lore == null) lore = new ArrayList<>();
        lore.add("");
        lore.add(MessageUtils.colorize(plugin.getConfigManager().getInsuranceLoreMark()));
        meta.setLore(lore);
        item.setItemMeta(meta);

        data.addInsuredItem(item);
        plugin.getDataManager().savePlayerData(player.getUniqueId());

        if (insuranceCost > 0) {
            plugin.getMessageUtils().sendMessage(player, "item-insured-cost",
                    "%cost%", String.format("%.2f", insuranceCost));
        } else {
            plugin.getMessageUtils().sendMessage(player, "item-insured");
        }
        plugin.getMessageUtils().playSound(player, "insure-item");

        return true;
    }

    public boolean uninsureItem(Player player, ItemStack item) {
        if (item == null || !isInsured(item)) return false;

        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;

        PersistentDataContainer container = meta.getPersistentDataContainer();
        container.remove(insuranceKey);
        container.remove(insuranceOwnerKey);

        List<String> lore = meta.getLore();
        if (lore != null) {
            String insuranceMark = MessageUtils.colorize(plugin.getConfigManager().getInsuranceLoreMark());
            lore.removeIf(line -> line.contains("Застрахован") || line.contains("✓") ||
                    line.equals(insuranceMark) || MessageUtils.stripColor(line).contains("Предмет застрахован"));
            if (!lore.isEmpty() && lore.get(lore.size() - 1).isEmpty()) {
                lore.remove(lore.size() - 1);
            }
            meta.setLore(lore.isEmpty() ? null : lore);
        }
        item.setItemMeta(meta);

        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        data.removeInsuredItem(item);
        plugin.getDataManager().savePlayerData(player.getUniqueId());

        plugin.getMessageUtils().sendMessage(player, "item-uninsured");
        plugin.getMessageUtils().playSound(player, "uninsure-item");

        return true;
    }

    public boolean isInsured(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return false;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return false;
        return meta.getPersistentDataContainer().has(insuranceKey, PersistentDataType.BYTE);
    }

    public UUID getInsuranceOwner(ItemStack item) {
        if (item == null || item.getType() == Material.AIR) return null;
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return null;
        String uuidStr = meta.getPersistentDataContainer().get(insuranceOwnerKey, PersistentDataType.STRING);
        if (uuidStr == null) return null;
        try {
            return UUID.fromString(uuidStr);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public int getMaxSlots(Player player) {
        int maxConfigSlots = plugin.getConfigManager().getMaxSlots();
        for (int slots = maxConfigSlots; slots > 1; slots--) {
            if (player.hasPermission("stvinsurance.slots." + slots)) {
                return slots;
            }
        }
        return plugin.getConfigManager().getDefaultSlots();
    }

    public String getSlotTierName(Player player) {
        int slots = getMaxSlots(player);
        if (slots == plugin.getConfigManager().getDefaultSlots()) {
            return "Обычный игрок";
        }
        return "Донатер (" + slots + " слотов)";
    }

    public int getAvailableSlots(Player player) {
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        int totalOccupied = data.getInsuredItems().size() + data.getPendingItems().size();
        return getMaxSlots(player) - totalOccupied;
    }

    public double calculateRedemptionCost(ItemStack item) {
        int totalEnchantLevel = 0;
        if (item.hasItemMeta() && item.getItemMeta().hasEnchants()) {
            for (Map.Entry<Enchantment, Integer> entry : item.getItemMeta().getEnchants().entrySet()) {
                totalEnchantLevel += entry.getValue();
            }
        }
        return plugin.getConfigManager().calculateRedemptionCost(item.getType(), totalEnchantLevel);
    }

    public double calculateRedemptionPointsCost(ItemStack item) {
        int totalEnchantLevel = 0;
        if (item.hasItemMeta() && item.getItemMeta().hasEnchants()) {
            for (Map.Entry<Enchantment, Integer> entry : item.getItemMeta().getEnchants().entrySet()) {
                totalEnchantLevel += entry.getValue();
            }
        }
        return plugin.getConfigManager().calculateRedemptionPointsCost(item.getType(), totalEnchantLevel);
    }

    public List<ItemStack> processDeathItems(Player player, List<ItemStack> drops, Player killer) {
        List<ItemStack> insuredItems = new ArrayList<>();
        List<ItemStack> remainingDrops = new ArrayList<>();

        for (ItemStack item : drops) {
            if (item != null && isInsured(item)) {
                UUID owner = getInsuranceOwner(item);
                if (owner != null && owner.equals(player.getUniqueId())) {
                    insuredItems.add(item.clone());
                } else {
                    remainingDrops.add(item);
                }
            } else {
                remainingDrops.add(item);
            }
        }

        if (!insuredItems.isEmpty()) {
            PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
            String deathReason = getDeathReason(player, killer);
            UUID killerUuid = killer != null ? killer.getUniqueId() : null;
            String killerName = killer != null ? killer.getName() : null;

            for (ItemStack item : insuredItems) {
                removeInsuranceMark(item);
                double cost = calculateRedemptionCost(item);
                double pointsCost = calculateRedemptionPointsCost(item);
                PendingItem pending = new PendingItem(item, System.currentTimeMillis(), cost, pointsCost, deathReason, killerUuid, killerName);
                data.addPendingItem(pending);
                data.removeInsuredItem(item);
            }

            plugin.getDataManager().savePlayerData(player.getUniqueId());
            plugin.getMessageUtils().sendMessage(player, "death-notification");
        }

        return remainingDrops;
    }

    private void removeInsuranceMark(ItemStack item) {
        ItemMeta meta = item.getItemMeta();
        if (meta == null) return;

        meta.getPersistentDataContainer().remove(insuranceKey);
        meta.getPersistentDataContainer().remove(insuranceOwnerKey);

        List<String> lore = meta.getLore();
        if (lore != null) {
            String insuranceMark = MessageUtils.colorize(plugin.getConfigManager().getInsuranceLoreMark());
            lore.removeIf(line -> line.contains("Застрахован") || line.contains("✓") ||
                    line.equals(insuranceMark) || MessageUtils.stripColor(line).contains("Предмет застрахован"));
            if (!lore.isEmpty() && lore.get(lore.size() - 1).isEmpty()) {
                lore.remove(lore.size() - 1);
            }
            meta.setLore(lore.isEmpty() ? null : lore);
        }
        item.setItemMeta(meta);
    }

    public boolean redeemItem(Player player, PendingItem pendingItem) {
        double cost = pendingItem.getCost();

        if (plugin.isVaultEnabled() && plugin.getConfigManager().isUseVault()) {
            if (plugin.getEconomy().getBalance(player) < cost) {
                plugin.getMessageUtils().sendMessage(player, "not-enough-money", "%cost%", String.format("%.2f", cost));
                plugin.getMessageUtils().playSound(player, "error");
                return false;
            }
            plugin.getEconomy().withdrawPlayer(player, cost);
        }

        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(pendingItem.getItem());
        if (!overflow.isEmpty()) {
            for (ItemStack item : overflow.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
            }
        }

        data.removePendingItem(pendingItem);
        plugin.getDataManager().savePlayerData(player.getUniqueId());
        plugin.getMessageUtils().sendMessage(player, "item-redeemed", "%cost%", String.format("%.2f", cost));
        plugin.getMessageUtils().playSound(player, "redeem-item");
        return true;
    }

    public boolean redeemItemWithPoints(Player player, PendingItem pendingItem) {
        if (!plugin.isPlayerPointsEnabled()) {
            plugin.getMessageUtils().sendMessage(player, "playerpoints-not-available");
            plugin.getMessageUtils().playSound(player, "error");
            return false;
        }

        int pointsCost = (int) pendingItem.getPointsCost();
        if (plugin.getPlayerPointsAPI().look(player.getUniqueId()) < pointsCost) {
            plugin.getMessageUtils().sendMessage(player, "not-enough-points", "%cost%", String.valueOf(pointsCost));
            plugin.getMessageUtils().playSound(player, "error");
            return false;
        }

        plugin.getPlayerPointsAPI().take(player.getUniqueId(), pointsCost);

        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(pendingItem.getItem());
        if (!overflow.isEmpty()) {
            for (ItemStack item : overflow.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
            }
        }

        data.removePendingItem(pendingItem);
        plugin.getDataManager().savePlayerData(player.getUniqueId());
        plugin.getMessageUtils().sendMessage(player, "item-redeemed-points", "%cost%", String.valueOf(pointsCost));
        plugin.getMessageUtils().playSound(player, "redeem-item");
        return true;
    }

    public boolean declineRedeem(Player player, PendingItem pendingItem) {
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        if (pendingItem.isPvpDeath() && pendingItem.getKillerUuid() != null) {
            transferItemToKiller(pendingItem, player.getUniqueId());
        }
        data.removePendingItem(pendingItem);
        plugin.getDataManager().savePlayerData(player.getUniqueId());
        plugin.getMessageUtils().sendMessage(player, "redeem-declined");
        plugin.getMessageUtils().playSound(player, "item-expired");
        return true;
    }

    private void transferItemToKiller(PendingItem pending, UUID ownerUuid) {
        UUID killerUuid = pending.getKillerUuid();
        String ownerName = Bukkit.getOfflinePlayer(ownerUuid).getName();
        PlayerData killerData = plugin.getDataManager().getPlayerData(killerUuid);

        ReturnedItem returnedItem = new ReturnedItem(
                pending.getItem(), System.currentTimeMillis(),
                ownerName != null ? ownerName : "Неизвестный", ownerUuid
        );
        killerData.addReturnedItem(returnedItem);
        plugin.getDataManager().savePlayerData(killerUuid);

        Player killer = Bukkit.getPlayer(killerUuid);
        if (killer != null && killer.isOnline()) {
            plugin.getMessageUtils().sendMessage(killer, "received-from-declined",
                    "%player%", ownerName != null ? ownerName : "Неизвестный");
            plugin.getMessageUtils().playSound(killer, "item-received");
        }
    }

    public void checkExpiredInsurances() {
        long currentTime = System.currentTimeMillis();
        long expiryTime = plugin.getConfigManager().getRedemptionTime() * 1000L;

        for (UUID uuid : plugin.getDataManager().getAllPlayerUuids()) {
            PlayerData data = plugin.getDataManager().getPlayerData(uuid);
            List<PendingItem> toRemove = new ArrayList<>();

            for (PendingItem pending : data.getPendingItems()) {
                if (currentTime - pending.getDeathTime() > expiryTime) {
                    toRemove.add(pending);
                    processExpiredItem(uuid, pending);
                }
            }

            for (PendingItem pending : toRemove) {
                data.removePendingItem(pending);
            }

            if (!toRemove.isEmpty()) {
                plugin.getDataManager().savePlayerData(uuid);
            }
        }
    }

    private void processExpiredItem(UUID ownerUuid, PendingItem pending) {
        Player owner = Bukkit.getPlayer(ownerUuid);
        if (pending.getKillerUuid() != null) {
            transferItemToKiller(pending, ownerUuid);
            if (owner != null && owner.isOnline()) {
                plugin.getMessageUtils().sendMessage(owner, "item-expired-pvp",
                        "%killer%", pending.getKillerName() != null ? pending.getKillerName() : "Неизвестный");
                plugin.getMessageUtils().playSound(owner, "item-expired");
            }
        } else {
            if (owner != null && owner.isOnline()) {
                plugin.getMessageUtils().sendMessage(owner, "item-expired-self");
                plugin.getMessageUtils().playSound(owner, "item-expired");
            }
        }
    }

    public boolean claimReturnedItem(Player player, ReturnedItem returnedItem) {
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        HashMap<Integer, ItemStack> overflow = player.getInventory().addItem(returnedItem.getItem());
        if (!overflow.isEmpty()) {
            for (ItemStack item : overflow.values()) {
                player.getWorld().dropItemNaturally(player.getLocation(), item);
            }
        }
        data.removeReturnedItem(returnedItem);
        plugin.getDataManager().savePlayerData(player.getUniqueId());
        plugin.getMessageUtils().sendMessage(player, "returned-item-claimed", "%player%", returnedItem.getFromPlayerName());
        plugin.getMessageUtils().playSound(player, "redeem-item");
        return true;
    }

    private String getDeathReason(Player player, Player killer) {
        if (killer != null) return plugin.getConfigManager().getDeathReason("pvp");

        EntityDamageEvent lastDamage = player.getLastDamageCause();
        if (lastDamage != null) {
            EntityDamageEvent.DamageCause cause = lastDamage.getCause();

            if (cause == EntityDamageEvent.DamageCause.FALL)
                return plugin.getConfigManager().getDeathReason("fall");
            if (cause == EntityDamageEvent.DamageCause.FIRE || cause == EntityDamageEvent.DamageCause.FIRE_TICK)
                return plugin.getConfigManager().getDeathReason("fire");
            if (cause == EntityDamageEvent.DamageCause.LAVA)
                return plugin.getConfigManager().getDeathReason("lava");
            if (cause == EntityDamageEvent.DamageCause.DROWNING)
                return plugin.getConfigManager().getDeathReason("drowning");
            if (cause == EntityDamageEvent.DamageCause.BLOCK_EXPLOSION || cause == EntityDamageEvent.DamageCause.ENTITY_EXPLOSION)
                return plugin.getConfigManager().getDeathReason("explosion");
            if (cause == EntityDamageEvent.DamageCause.VOID)
                return plugin.getConfigManager().getDeathReason("void");
            if (cause == EntityDamageEvent.DamageCause.MAGIC || cause == EntityDamageEvent.DamageCause.POISON || cause == EntityDamageEvent.DamageCause.WITHER)
                return plugin.getConfigManager().getDeathReason("magic");
        }
        return plugin.getConfigManager().getDeathReason("other");
    }

    public NamespacedKey getInsuranceKey() { return insuranceKey; }
    public NamespacedKey getInsuranceOwnerKey() { return insuranceOwnerKey; }
}
