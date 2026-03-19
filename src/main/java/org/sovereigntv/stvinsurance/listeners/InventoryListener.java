package org.sovereigntv.stvinsurance.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.sovereigntv.stvinsurance.Main;
import org.sovereigntv.stvinsurance.data.PendingItem;
import org.sovereigntv.stvinsurance.data.PlayerData;
import org.sovereigntv.stvinsurance.managers.MenuManager;

import java.util.List;

public class InventoryListener implements Listener {

    private final Main plugin;

    public InventoryListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onInventoryClick(InventoryClickEvent event) {
        if (!(event.getWhoClicked() instanceof Player)) return;

        Player player = (Player) event.getWhoClicked();
        Inventory inventory = event.getInventory();

        if (!(inventory.getHolder() instanceof MenuManager.MenuHolder)) return;

        MenuManager.MenuHolder holder = (MenuManager.MenuHolder) inventory.getHolder();
        String menuType = holder.getMenuType();

        event.setCancelled(true);

        ItemStack clicked = event.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        int slot = event.getRawSlot();

        switch (menuType) {
            case MenuManager.MAIN_MENU:
                handleMainMenuClick(player, slot);
                break;
            case MenuManager.INSURE_MENU:
                handleInsureMenuClick(player, slot, clicked);
                break;
            case MenuManager.INSURED_ITEMS_MENU:
                handleInsuredItemsMenuClick(player, slot, event);
                break;
            case MenuManager.REDEEM_MENU:
                handleRedeemMenuClick(player, slot, event);
                break;
            case MenuManager.RETURNED_MENU:
                handleReturnedMenuClick(player, slot);
                break;
            case MenuManager.CONFIRM_MENU:
                handleConfirmMenuClick(player, slot);
                break;
        }
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof MenuManager.MenuHolder) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        if (!(event.getPlayer() instanceof Player)) return;

        Player player = (Player) event.getPlayer();
        Inventory inventory = event.getInventory();

        if (inventory.getHolder() instanceof MenuManager.MenuHolder) {
            MenuManager.MenuHolder holder = (MenuManager.MenuHolder) inventory.getHolder();
            if (MenuManager.CONFIRM_MENU.equals(holder.getMenuType())) {
                plugin.getServer().getScheduler().runTaskLater(plugin, () ->
                        plugin.getMenuManager().removePendingConfirmation(player.getUniqueId()), 5L);
            }
        }
    }

    private void handleMainMenuClick(Player player, int slot) {
        int insureSlot = plugin.getConfigManager().getConfig().getInt("gui.main-menu.insure-button.slot", 20);
        int myItemsSlot = plugin.getConfigManager().getConfig().getInt("gui.main-menu.my-items-button.slot", 22);
        int redeemSlot = plugin.getConfigManager().getConfig().getInt("gui.main-menu.redeem-button.slot", 24);
        int returnedSlot = plugin.getConfigManager().getConfig().getInt("gui.main-menu.returned-button.slot", 31);
        int closeSlot = plugin.getConfigManager().getConfig().getInt("gui.main-menu.close-button.slot", 49);

        if (slot == insureSlot) {
            plugin.getMenuManager().openInsureMenu(player);
        } else if (slot == myItemsSlot) {
            plugin.getMenuManager().openInsuredItemsMenu(player);
        } else if (slot == redeemSlot) {
            plugin.getMenuManager().openRedeemMenu(player);
        } else if (slot == returnedSlot) {
            plugin.getMenuManager().openReturnedMenu(player);
        } else if (slot == closeSlot) {
            player.closeInventory();
        }
    }

    private void handleInsureMenuClick(Player player, int slot, ItemStack clicked) {
        int backSlot = plugin.getConfigManager().getConfig().getInt("gui.insure-menu.back-slot", 49);

        if (slot == backSlot) {
            plugin.getMenuManager().openMainMenu(player);
            return;
        }

        if (slot >= 9 && slot < 45 && clicked != null && clicked.getType() != Material.AIR) {
            int originalSlot = -1;
            if (clicked.hasItemMeta() && clicked.getItemMeta().hasLore()) {
                List<String> lore = clicked.getItemMeta().getLore();
                for (String line : lore) {
                    String stripped = org.bukkit.ChatColor.stripColor(line);
                    if (stripped.startsWith("Слот: ")) {
                        try {
                            originalSlot = Integer.parseInt(stripped.replace("Слот: ", ""));
                            break;
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }

            if (originalSlot == -1) return;

            ItemStack playerItem = player.getInventory().getItem(originalSlot);

            if (playerItem != null && playerItem.getType() != Material.AIR) {
                if (!plugin.getConfigManager().isItemAllowed(playerItem.getType())) {
                    plugin.getMessageUtils().sendMessage(player, "item-not-allowed");
                    plugin.getMessageUtils().playSound(player, "error");
                    return;
                }

                if (plugin.getInsuranceManager().isInsured(playerItem)) {
                    plugin.getInsuranceManager().uninsureItem(player, playerItem);
                } else {
                    plugin.getInsuranceManager().insureItem(player, playerItem);
                }

                plugin.getMenuManager().openInsureMenu(player);
            }
        }
    }

    private void handleInsuredItemsMenuClick(Player player, int slot, InventoryClickEvent event) {
        int backSlot = plugin.getConfigManager().getConfig().getInt("gui.insured-items-menu.back-slot", 49);

        if (slot == backSlot) {
            plugin.getMenuManager().openMainMenu(player);
            return;
        }

        List<Integer> itemSlots = plugin.getConfigManager().getConfig().getIntegerList("gui.insured-items-menu.item-slots");
        int slotIndex = itemSlots.indexOf(slot);

        if (slotIndex >= 0) {
            PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
            List<ItemStack> insuredItems = data.getInsuredItems();

            if (slotIndex < insuredItems.size()) {
                if (event.getClick() == ClickType.RIGHT || event.getClick() == ClickType.SHIFT_RIGHT) {
                    data.removeInsuredItemByIndex(slotIndex);
                    plugin.getDataManager().savePlayerData(player.getUniqueId());
                    plugin.getMessageUtils().sendMessage(player, "item-uninsured");
                    plugin.getMessageUtils().playSound(player, "uninsure-item");
                    plugin.getMenuManager().openInsuredItemsMenu(player);
                }
            } else if (slotIndex - insuredItems.size() < data.getPendingItems().size()) {
                plugin.getMenuManager().openRedeemMenu(player);
            }
        }
    }

    private void handleRedeemMenuClick(Player player, int slot, InventoryClickEvent event) {
        int backSlot = plugin.getConfigManager().getConfig().getInt("gui.redeem-menu.back-slot", 49);

        if (slot == backSlot) {
            plugin.getMenuManager().openMainMenu(player);
            return;
        }

        List<Integer> itemSlots = plugin.getConfigManager().getConfig().getIntegerList("gui.redeem-menu.item-slots");
        int itemIndex = itemSlots.indexOf(slot);

        if (itemIndex >= 0) {
            PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
            List<PendingItem> pendingItems = data.getPendingItems();

            if (itemIndex < pendingItems.size()) {
                PendingItem pending = pendingItems.get(itemIndex);
                ItemStack clicked = event.getCurrentItem();

                if (event.getClick() == ClickType.LEFT) {
                    plugin.getMenuManager().openConfirmMenu(player, "redeem", pending, clicked);
                } else if (event.getClick() == ClickType.SHIFT_LEFT && plugin.isPlayerPointsEnabled()) {
                    plugin.getMenuManager().openConfirmMenu(player, "redeem-points", pending, clicked);
                } else if (event.getClick() == ClickType.RIGHT || event.getClick() == ClickType.SHIFT_RIGHT) {
                    plugin.getMenuManager().openConfirmMenu(player, "decline", pending, clicked);
                }
            }
        }
    }

    private void handleReturnedMenuClick(Player player, int slot) {
        int backSlot = plugin.getConfigManager().getConfig().getInt("gui.returned-menu.back-slot", 49);

        if (slot == backSlot) {
            plugin.getMenuManager().openMainMenu(player);
            return;
        }

        List<Integer> itemSlots = plugin.getConfigManager().getConfig().getIntegerList("gui.returned-menu.item-slots");
        int itemIndex = itemSlots.indexOf(slot);

        if (itemIndex >= 0) {
            PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());
            if (itemIndex < data.getReturnedItems().size()) {
                plugin.getInsuranceManager().claimReturnedItem(player, data.getReturnedItems().get(itemIndex));
                plugin.getMenuManager().openReturnedMenu(player);
            }
        }
    }

    private void handleConfirmMenuClick(Player player, int slot) {
        int confirmSlot = plugin.getConfigManager().getConfig().getInt("gui.confirm-menu.confirm-button.slot", 11);
        int cancelSlot = plugin.getConfigManager().getConfig().getInt("gui.confirm-menu.cancel-button.slot", 15);

        MenuManager.ConfirmAction action = plugin.getMenuManager().getPendingConfirmation(player.getUniqueId());
        if (action == null) {
            player.closeInventory();
            return;
        }

        if (slot == confirmSlot) {
            plugin.getMenuManager().removePendingConfirmation(player.getUniqueId());

            switch (action.getAction()) {
                case "redeem":
                    plugin.getInsuranceManager().redeemItem(player, (PendingItem) action.getData());
                    break;
                case "redeem-points":
                    plugin.getInsuranceManager().redeemItemWithPoints(player, (PendingItem) action.getData());
                    break;
                case "decline":
                    plugin.getInsuranceManager().declineRedeem(player, (PendingItem) action.getData());
                    break;
            }

            plugin.getMenuManager().openRedeemMenu(player);
        } else if (slot == cancelSlot) {
            plugin.getMenuManager().removePendingConfirmation(player.getUniqueId());
            plugin.getMenuManager().openRedeemMenu(player);
        }
    }
}
