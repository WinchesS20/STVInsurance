package org.sovereigntv.stvinsurance.data;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class ReturnedItem {

    private final ItemStack item;
    private final long receivedTime;
    private final String fromPlayerName;
    private final UUID fromPlayerUuid;

    public ReturnedItem(ItemStack item, long receivedTime, String fromPlayerName, UUID fromPlayerUuid) {
        this.item = item != null ? item.clone() : null;
        this.receivedTime = receivedTime;
        this.fromPlayerName = fromPlayerName;
        this.fromPlayerUuid = fromPlayerUuid;
    }

    public ItemStack getItem() { return item != null ? item.clone() : null; }
    public long getReceivedTime() { return receivedTime; }
    public String getFromPlayerName() { return fromPlayerName; }
    public UUID getFromPlayerUuid() { return fromPlayerUuid; }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        return receivedTime == ((ReturnedItem) obj).receivedTime;
    }

    @Override
    public int hashCode() {
        return (int) (receivedTime ^ (receivedTime >>> 32));
    }
}
