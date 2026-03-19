package org.sovereigntv.stvinsurance.managers;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.sovereigntv.stvinsurance.Main;
import org.sovereigntv.stvinsurance.compat.MaterialCompat;
import org.sovereigntv.stvinsurance.data.PendingItem;
import org.sovereigntv.stvinsurance.data.PlayerData;
import org.sovereigntv.stvinsurance.data.ReturnedItem;
import org.sovereigntv.stvinsurance.utils.MessageUtils;

import java.util.*;

public class MenuManager {

    private final Main plugin;

    public static final String MAIN_MENU = "main-menu";
    public static final String INSURE_MENU = "insure-menu";
    public static final String INSURED_ITEMS_MENU = "insured-items-menu";
    public static final String REDEEM_MENU = "redeem-menu";
    public static final String RETURNED_MENU = "returned-menu";
    public static final String CONFIRM_MENU = "confirm-menu";

    private final Map<UUID, ConfirmAction> pendingConfirmations = new HashMap<>();

    public MenuManager(Main plugin) {
        this.plugin = plugin;
    }

    public void openMainMenu(Player player) {
        ConfigurationSection section = plugin.getConfigManager().getGuiSection(MAIN_MENU);
        String title = MessageUtils.colorize(plugin.getConfigManager().getGuiTitle(MAIN_MENU));
        int size = plugin.getConfigManager().getGuiSize(MAIN_MENU);

        Inventory inv = Bukkit.createInventory(new MenuHolder(MAIN_MENU), size, title);

        if (section.getBoolean("filler.enabled", true)) {
            ItemStack filler = createItem(
                    getMaterialSafe(section.getString("filler.material", "GRAY_STAINED_GLASS_PANE")),
                    section.getString("filler.name", " "), null);
            for (int i = 0; i < size; i++) inv.setItem(i, filler);
        }

        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        int maxSlots = plugin.getInsuranceManager().getMaxSlots(player);
        int availableSlots = plugin.getInsuranceManager().getAvailableSlots(player);
        int insuredCount = data.getInsuredItems().size();
        int pendingCount = data.getPendingItems().size();
        String tierName = plugin.getInsuranceManager().getSlotTierName(player);

        ConfigurationSection insureBtn = section.getConfigurationSection("insure-button");
        if (insureBtn != null) {
            List<String> lore = new ArrayList<>();
            for (String line : insureBtn.getStringList("lore")) {
                lore.add(line.replace("%available_slots%", String.valueOf(availableSlots))
                        .replace("%max_slots%", String.valueOf(maxSlots))
                        .replace("%tier%", tierName));
            }
            inv.setItem(insureBtn.getInt("slot", 20), createItem(
                    getMaterialSafe(insureBtn.getString("material", "EMERALD")),
                    insureBtn.getString("name"), lore));
        }

        ConfigurationSection myItemsBtn = section.getConfigurationSection("my-items-button");
        if (myItemsBtn != null) {
            List<String> lore = new ArrayList<>();
            for (String line : myItemsBtn.getStringList("lore")) {
                lore.add(line.replace("%insured_count%", String.valueOf(insuredCount))
                        .replace("%pending_count%", String.valueOf(pendingCount)));
            }
            inv.setItem(myItemsBtn.getInt("slot", 22), createItem(
                    getMaterialSafe(myItemsBtn.getString("material", "CHEST")),
                    myItemsBtn.getString("name"), lore));
        }

        ConfigurationSection redeemBtn = section.getConfigurationSection("redeem-button");
        if (redeemBtn != null) {
            List<String> lore = new ArrayList<>();
            for (String line : redeemBtn.getStringList("lore")) {
                lore.add(line.replace("%pending_count%", String.valueOf(pendingCount)));
            }
            inv.setItem(redeemBtn.getInt("slot", 24), createItem(
                    getMaterialSafe(redeemBtn.getString("material", "GOLD_INGOT")),
                    redeemBtn.getString("name"), lore));
        }

        int returnedCount = data.getReturnedItems().size();
        ConfigurationSection returnedBtn = section.getConfigurationSection("returned-button");
        if (returnedBtn != null) {
            List<String> lore = new ArrayList<>();
            for (String line : returnedBtn.getStringList("lore")) {
                lore.add(line.replace("%returned_count%", String.valueOf(returnedCount)));
            }
            inv.setItem(returnedBtn.getInt("slot", 31), createItem(
                    getMaterialSafe(returnedBtn.getString("material", "ENDER_CHEST")),
                    returnedBtn.getString("name"), lore));
        }

        ConfigurationSection infoBtn = section.getConfigurationSection("info-button");
        if (infoBtn != null) {
            List<String> lore = new ArrayList<>();
            for (String line : infoBtn.getStringList("lore")) {
                lore.add(line.replace("%max_slots%", String.valueOf(maxSlots))
                        .replace("%available_slots%", String.valueOf(availableSlots))
                        .replace("%insured_count%", String.valueOf(insuredCount))
                        .replace("%tier%", tierName));
            }
            inv.setItem(infoBtn.getInt("slot", 40), createItem(
                    getMaterialSafe(infoBtn.getString("material", "BOOK")),
                    infoBtn.getString("name"), lore));
        }

        ConfigurationSection closeBtn = section.getConfigurationSection("close-button");
        if (closeBtn != null) {
            inv.setItem(closeBtn.getInt("slot", 49), createItem(
                    getMaterialSafe(closeBtn.getString("material", "BARRIER")),
                    closeBtn.getString("name"), closeBtn.getStringList("lore")));
        }

        player.openInventory(inv);
        plugin.getMessageUtils().playSound(player, "open-menu");
    }

    public void openInsureMenu(Player player) {
        ConfigurationSection section = plugin.getConfigManager().getGuiSection(INSURE_MENU);
        String title = MessageUtils.colorize(plugin.getConfigManager().getGuiTitle(INSURE_MENU));
        int size = plugin.getConfigManager().getGuiSize(INSURE_MENU);

        Inventory inv = Bukkit.createInventory(new MenuHolder(INSURE_MENU), size, title);

        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < 9; i++) inv.setItem(i, filler);

        ConfigurationSection infoItem = section.getConfigurationSection("info-item");
        if (infoItem != null) {
            int availableSlots = plugin.getInsuranceManager().getAvailableSlots(player);
            int maxSlots = plugin.getInsuranceManager().getMaxSlots(player);
            double insuranceCost = plugin.getConfigManager().getInsuranceCost();
            List<String> lore = new ArrayList<>();
            for (String line : infoItem.getStringList("lore")) {
                lore.add(line.replace("%available_slots%", String.valueOf(availableSlots))
                        .replace("%max_slots%", String.valueOf(maxSlots))
                        .replace("%insurance_cost%", String.format("%.2f", insuranceCost)));
            }
            inv.setItem(section.getInt("info-slot", 4), createItem(
                    getMaterialSafe(infoItem.getString("material", "PAPER")),
                    infoItem.getString("name"), lore));
        }

        ConfigurationSection backItem = section.getConfigurationSection("back-item");
        if (backItem != null) {
            inv.setItem(section.getInt("back-slot", 49), createItem(
                    getMaterialSafe(backItem.getString("material", "ARROW")),
                    backItem.getString("name"), backItem.getStringList("lore")));
        }

        ItemStack[] playerInv = player.getInventory().getContents();
        int menuSlot = 9;

        for (int i = 0; i < 36; i++) {
            ItemStack playerItem = playerInv[i];
            if (playerItem != null && playerItem.getType() != Material.AIR) {
                if (!plugin.getConfigManager().isItemAllowed(playerItem.getType())) continue;

                ItemStack displayItem = playerItem.clone();
                ItemMeta meta = displayItem.getItemMeta();
                List<String> lore = meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();

                if (!plugin.getInsuranceManager().isInsured(playerItem)) {
                    lore.add("");
                    lore.add(MessageUtils.colorize("&aКликните для страхования"));
                }
                lore.add(MessageUtils.colorize("&8Слот: " + i));
                meta.setLore(lore);
                displayItem.setItemMeta(meta);

                inv.setItem(menuSlot, displayItem);
                menuSlot++;
                if (menuSlot >= 45) break;
            }
        }

        player.openInventory(inv);
    }

    public void openInsuredItemsMenu(Player player) {
        ConfigurationSection section = plugin.getConfigManager().getGuiSection(INSURED_ITEMS_MENU);
        String title = MessageUtils.colorize(plugin.getConfigManager().getGuiTitle(INSURED_ITEMS_MENU));
        int size = plugin.getConfigManager().getGuiSize(INSURED_ITEMS_MENU);

        Inventory inv = Bukkit.createInventory(new MenuHolder(INSURED_ITEMS_MENU), size, title);

        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < size; i++) inv.setItem(i, filler);

        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        int maxSlots = plugin.getInsuranceManager().getMaxSlots(player);
        List<Integer> itemSlots = section.getIntegerList("item-slots");

        ConfigurationSection emptySlotItem = section.getConfigurationSection("empty-slot-item");
        ItemStack emptyItem = emptySlotItem != null ? createItem(
                getMaterialSafe(emptySlotItem.getString("material", "RED_STAINED_GLASS_PANE")),
                emptySlotItem.getString("name"), emptySlotItem.getStringList("lore")) : null;

        ConfigurationSection lockedSlotItem = section.getConfigurationSection("locked-slot-item");
        ItemStack lockedItem = lockedSlotItem != null ? createItem(
                getMaterialSafe(lockedSlotItem.getString("material", "BLACK_STAINED_GLASS_PANE")),
                lockedSlotItem.getString("name"), lockedSlotItem.getStringList("lore")) : null;

        List<ItemStack> insuredItems = data.getInsuredItems();
        List<PendingItem> pendingItems = data.getPendingItems();
        int displayIndex = 0;

        for (int i = 0; i < itemSlots.size(); i++) {
            int slot = itemSlots.get(i);

            if (i < maxSlots) {
                if (displayIndex < insuredItems.size()) {
                    ItemStack displayItem = insuredItems.get(displayIndex).clone();
                    ItemMeta meta = displayItem.getItemMeta();
                    List<String> lore = meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
                    for (String line : section.getStringList("insured-item-lore")) {
                        lore.add(MessageUtils.colorize(line));
                    }
                    meta.setLore(lore);
                    displayItem.setItemMeta(meta);
                    inv.setItem(slot, displayItem);
                    displayIndex++;
                } else if (displayIndex - insuredItems.size() < pendingItems.size()) {
                    int pendingIndex = displayIndex - insuredItems.size();
                    PendingItem pending = pendingItems.get(pendingIndex);
                    ItemStack displayItem = pending.getItem().clone();
                    ItemMeta meta = displayItem.getItemMeta();
                    List<String> lore = meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();

                    long maxTime = plugin.getConfigManager().getRedemptionTime() * 1000L;
                    long timeLeft = maxTime - (System.currentTimeMillis() - pending.getDeathTime());

                    lore.add("");
                    lore.add(MessageUtils.colorize("&e&lОжидает выкупа"));
                    lore.add(MessageUtils.colorize("&fОсталось: &e" + formatTime(timeLeft)));
                    lore.add(MessageUtils.colorize("&fВыкуп через /insurance"));

                    meta.setLore(lore);
                    displayItem.setItemMeta(meta);
                    inv.setItem(slot, displayItem);
                    displayIndex++;
                } else {
                    if (emptyItem != null) inv.setItem(slot, emptyItem);
                    displayIndex++;
                }
            } else {
                if (lockedItem != null) inv.setItem(slot, lockedItem);
            }
        }

        ConfigurationSection backItem = section.getConfigurationSection("back-item");
        if (backItem == null) {
            backItem = plugin.getConfigManager().getGuiSection(INSURE_MENU).getConfigurationSection("back-item");
        }
        if (backItem != null) {
            inv.setItem(section.getInt("back-slot", 49), createItem(
                    getMaterialSafe(backItem.getString("material", "ARROW")),
                    backItem.getString("name", "&c&lНазад"), backItem.getStringList("lore")));
        }

        player.openInventory(inv);
    }

    public void openRedeemMenu(Player player) {
        ConfigurationSection section = plugin.getConfigManager().getGuiSection(REDEEM_MENU);
        String title = MessageUtils.colorize(plugin.getConfigManager().getGuiTitle(REDEEM_MENU));
        int size = plugin.getConfigManager().getGuiSize(REDEEM_MENU);

        Inventory inv = Bukkit.createInventory(new MenuHolder(REDEEM_MENU), size, title);

        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < size; i++) inv.setItem(i, filler);

        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        List<PendingItem> pendingItems = data.getPendingItems();
        List<Integer> itemSlots = section.getIntegerList("item-slots");

        if (pendingItems.isEmpty()) {
            ConfigurationSection emptyItem = section.getConfigurationSection("empty-item");
            if (emptyItem != null) {
                inv.setItem(emptyItem.getInt("slot", 22), createItem(
                        getMaterialSafe(emptyItem.getString("material", "HOPPER")),
                        emptyItem.getString("name"), emptyItem.getStringList("lore")));
            }
        } else {
            long currentTime = System.currentTimeMillis();
            long maxTime = plugin.getConfigManager().getRedemptionTime() * 1000L;

            for (int i = 0; i < pendingItems.size() && i < itemSlots.size(); i++) {
                PendingItem pending = pendingItems.get(i);
                ItemStack displayItem = pending.getItem().clone();
                ItemMeta meta = displayItem.getItemMeta();
                List<String> lore = meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();

                long timeLeft = maxTime - (currentTime - pending.getDeathTime());
                boolean playerPointsAvailable = plugin.isPlayerPointsEnabled();

                for (String line : section.getStringList("pending-item-lore")) {
                    if (line.contains("%points_cost%") && !playerPointsAvailable) continue;

                    lore.add(MessageUtils.colorize(line
                            .replace("%cost%", String.format("%.2f", pending.getCost()))
                            .replace("%points_cost%", String.valueOf((int) pending.getPointsCost()))
                            .replace("%time_left%", formatTime(timeLeft))
                            .replace("%death_reason%", pending.getDeathReason())
                            .replace("%killer%", pending.getKillerName() != null ? pending.getKillerName() : "—")));
                }

                meta.setLore(lore);
                displayItem.setItemMeta(meta);
                inv.setItem(itemSlots.get(i), displayItem);
            }
        }

        ConfigurationSection backItem = plugin.getConfigManager().getGuiSection(INSURE_MENU).getConfigurationSection("back-item");
        if (backItem != null) {
            inv.setItem(section.getInt("back-slot", 49), createItem(
                    getMaterialSafe(backItem.getString("material", "ARROW")),
                    backItem.getString("name", "&c&lНазад"), backItem.getStringList("lore")));
        }

        player.openInventory(inv);
    }

    public void openReturnedMenu(Player player) {
        ConfigurationSection section = plugin.getConfigManager().getGuiSection(RETURNED_MENU);
        String title = MessageUtils.colorize(plugin.getConfigManager().getGuiTitle(RETURNED_MENU));
        int size = plugin.getConfigManager().getGuiSize(RETURNED_MENU);

        Inventory inv = Bukkit.createInventory(new MenuHolder(RETURNED_MENU), size, title);

        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < size; i++) inv.setItem(i, filler);

        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
        List<ReturnedItem> returnedItems = data.getReturnedItems();
        List<Integer> itemSlots = section.getIntegerList("item-slots");

        if (returnedItems.isEmpty()) {
            ConfigurationSection emptyItem = section.getConfigurationSection("empty-item");
            if (emptyItem != null) {
                inv.setItem(emptyItem.getInt("slot", 22), createItem(
                        getMaterialSafe(emptyItem.getString("material", "HOPPER")),
                        emptyItem.getString("name"), emptyItem.getStringList("lore")));
            }
        } else {
            for (int i = 0; i < returnedItems.size() && i < itemSlots.size(); i++) {
                ReturnedItem returned = returnedItems.get(i);
                ItemStack displayItem = returned.getItem().clone();
                ItemMeta meta = displayItem.getItemMeta();
                List<String> lore = meta.getLore() != null ? new ArrayList<>(meta.getLore()) : new ArrayList<>();

                for (String line : section.getStringList("returned-item-lore")) {
                    lore.add(MessageUtils.colorize(line.replace("%from_player%", returned.getFromPlayerName())));
                }

                meta.setLore(lore);
                displayItem.setItemMeta(meta);
                inv.setItem(itemSlots.get(i), displayItem);
            }
        }

        ConfigurationSection backItem = plugin.getConfigManager().getGuiSection(INSURE_MENU).getConfigurationSection("back-item");
        if (backItem != null) {
            inv.setItem(section.getInt("back-slot", 49), createItem(
                    getMaterialSafe(backItem.getString("material", "ARROW")),
                    backItem.getString("name", "&c&lНазад"), backItem.getStringList("lore")));
        }

        player.openInventory(inv);
    }

    public void openConfirmMenu(Player player, String action, Object data, ItemStack previewItem) {
        ConfigurationSection section = plugin.getConfigManager().getGuiSection(CONFIRM_MENU);
        String title = MessageUtils.colorize(plugin.getConfigManager().getGuiTitle(CONFIRM_MENU));
        int size = plugin.getConfigManager().getGuiSize(CONFIRM_MENU);

        Inventory inv = Bukkit.createInventory(new MenuHolder(CONFIRM_MENU), size, title);

        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ", null);
        for (int i = 0; i < size; i++) inv.setItem(i, filler);

        PendingItem pendingItem = (PendingItem) data;
        String confirmButtonName;
        List<String> confirmButtonLore = new ArrayList<>();

        switch (action) {
            case "redeem":
                confirmButtonName = "&a&lВыкупить за монеты";
                confirmButtonLore.add("");
                confirmButtonLore.add("&fСтоимость: &a" + String.format("%.2f", pendingItem.getCost()));
                break;
            case "redeem-points":
                confirmButtonName = "&d&lВыкупить за донат-валюту";
                confirmButtonLore.add("");
                confirmButtonLore.add("&fСтоимость: &d" + (int) pendingItem.getPointsCost() + " поинтов");
                break;
            case "decline":
                confirmButtonName = "&c&lОтказаться от выкупа";
                confirmButtonLore.add("");
                confirmButtonLore.add(pendingItem.isPvpDeath()
                        ? "&cПредмет будет передан убийце!"
                        : "&cПредмет будет уничтожен!");
                break;
            default:
                confirmButtonName = "&a&lПодтвердить";
                break;
        }

        ConfigurationSection confirmBtn = section.getConfigurationSection("confirm-button");
        if (confirmBtn != null) {
            inv.setItem(confirmBtn.getInt("slot", 11), createItem(
                    getMaterialSafe(confirmBtn.getString("material", "LIME_WOOL")),
                    confirmButtonName, confirmButtonLore));
        }

        ConfigurationSection cancelBtn = section.getConfigurationSection("cancel-button");
        if (cancelBtn != null) {
            inv.setItem(cancelBtn.getInt("slot", 15), createItem(
                    getMaterialSafe(cancelBtn.getString("material", "RED_WOOL")),
                    cancelBtn.getString("name"), cancelBtn.getStringList("lore")));
        }

        if (previewItem != null) {
            inv.setItem(section.getInt("item-preview-slot", 13), previewItem);
        }

        pendingConfirmations.put(player.getUniqueId(), new ConfirmAction(action, data));
        player.openInventory(inv);
    }

    public ConfirmAction getPendingConfirmation(UUID uuid) {
        return pendingConfirmations.get(uuid);
    }

    public void removePendingConfirmation(UUID uuid) {
        pendingConfirmations.remove(uuid);
    }

    private Material getMaterialSafe(String name) {
        if (name == null || name.isEmpty()) return Material.STONE;
        Material mat = MaterialCompat.getMaterialSafe(name);
        return mat != null ? mat : Material.STONE;
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (name != null) meta.setDisplayName(MessageUtils.colorize(name));
        if (lore != null && !lore.isEmpty()) {
            List<String> coloredLore = new ArrayList<>();
            for (String line : lore) coloredLore.add(MessageUtils.colorize(line));
            meta.setLore(coloredLore);
        }
        item.setItemMeta(meta);
        return item;
    }

    public String formatTime(long millis) {
        if (millis <= 0) return "0сек";

        long seconds = millis / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        seconds %= 60;
        minutes %= 60;

        if (hours > 0) {
            return plugin.getConfigManager().getTimeFormat("hours")
                    .replace("%h%", String.valueOf(hours))
                    .replace("%m%", String.valueOf(minutes));
        } else if (minutes > 0) {
            return plugin.getConfigManager().getTimeFormat("minutes")
                    .replace("%m%", String.valueOf(minutes))
                    .replace("%s%", String.valueOf(seconds));
        } else {
            return plugin.getConfigManager().getTimeFormat("seconds")
                    .replace("%s%", String.valueOf(seconds));
        }
    }

    public static class MenuHolder implements org.bukkit.inventory.InventoryHolder {
        private final String menuType;

        public MenuHolder(String menuType) { this.menuType = menuType; }
        public String getMenuType() { return menuType; }

        @Override
        public Inventory getInventory() { return null; }
    }

    public static class ConfirmAction {
        private final String action;
        private final Object data;

        public ConfirmAction(String action, Object data) {
            this.action = action;
            this.data = data;
        }

        public String getAction() { return action; }
        public Object getData() { return data; }
    }
}
