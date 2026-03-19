package org.sovereigntv.stvinsurance.data;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerData {

    private final UUID uuid;
    private final List<ItemStack> insuredItems;
    private final List<PendingItem> pendingItems;
    private final List<ReturnedItem> returnedItems;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        this.insuredItems = new ArrayList<>();
        this.pendingItems = new ArrayList<>();
        this.returnedItems = new ArrayList<>();
    }

    public UUID getUuid() { return uuid; }

    @Deprecated
    public int getBonusSlots() { return 0; }

    @Deprecated
    public void setBonusSlots(int bonusSlots) {}

    public List<ItemStack> getInsuredItems() { return new ArrayList<>(insuredItems); }

    public void addInsuredItem(ItemStack item) {
        if (item != null) insuredItems.add(item.clone());
    }

    public void removeInsuredItem(ItemStack item) {
        if (item == null) return;
        for (int i = 0; i < insuredItems.size(); i++) {
            ItemStack insured = insuredItems.get(i);
            if (insured.getType() == item.getType()) {
                boolean enchantMatch = true;
                if (insured.hasItemMeta() && item.hasItemMeta()) {
                    enchantMatch = insured.getItemMeta().getEnchants().equals(item.getItemMeta().getEnchants());
                } else if (insured.hasItemMeta() != item.hasItemMeta()) {
                    if (insured.hasItemMeta() && !insured.getItemMeta().getEnchants().isEmpty()) enchantMatch = false;
                    if (item.hasItemMeta() && !item.getItemMeta().getEnchants().isEmpty()) enchantMatch = false;
                }
                if (enchantMatch) {
                    insuredItems.remove(i);
                    return;
                }
            }
        }
    }

    public void removeInsuredItemByIndex(int index) {
        if (index >= 0 && index < insuredItems.size()) insuredItems.remove(index);
    }

    public void clearInsuredItems() { insuredItems.clear(); }

    public List<PendingItem> getPendingItems() { return new ArrayList<>(pendingItems); }

    public void addPendingItem(PendingItem item) {
        if (item != null) pendingItems.add(item);
    }

    public void removePendingItem(PendingItem item) {
        if (item == null) return;
        for (int i = 0; i < pendingItems.size(); i++) {
            if (pendingItems.get(i).getDeathTime() == item.getDeathTime()) {
                pendingItems.remove(i);
                return;
            }
        }
    }

    public void clearPendingItems() { pendingItems.clear(); }

    public List<ReturnedItem> getReturnedItems() { return new ArrayList<>(returnedItems); }

    public void addReturnedItem(ReturnedItem item) {
        if (item != null) returnedItems.add(item);
    }

    public void removeReturnedItem(ReturnedItem item) {
        if (item == null) return;
        for (int i = 0; i < returnedItems.size(); i++) {
            if (returnedItems.get(i).getReceivedTime() == item.getReceivedTime()) {
                returnedItems.remove(i);
                return;
            }
        }
    }

    public void removeReturnedItemByIndex(int index) {
        if (index >= 0 && index < returnedItems.size()) returnedItems.remove(index);
    }

    public void clearReturnedItems() { returnedItems.clear(); }

    public int getInsuredCount() { return insuredItems.size(); }
    public int getPendingCount() { return pendingItems.size(); }
    public int getReturnedCount() { return returnedItems.size(); }
}
