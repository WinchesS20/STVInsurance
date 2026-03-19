package org.sovereigntv.stvinsurance.data;

import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class PendingItem {

    private final ItemStack item;
    private final long deathTime;
    private final double cost;
    private final double pointsCost;
    private final String deathReason;
    private final UUID killerUuid;
    private final String killerName;

    public PendingItem(ItemStack item, long deathTime, double cost, double pointsCost,
                       String deathReason, UUID killerUuid, String killerName) {
        this.item = item != null ? item.clone() : null;
        this.deathTime = deathTime;
        this.cost = cost;
        this.pointsCost = pointsCost;
        this.deathReason = deathReason;
        this.killerUuid = killerUuid;
        this.killerName = killerName;
    }

    public PendingItem(ItemStack item, long deathTime, double cost,
                       String deathReason, UUID killerUuid, String killerName) {
        this(item, deathTime, cost, 0, deathReason, killerUuid, killerName);
    }

    public ItemStack getItem() { return item != null ? item.clone() : null; }
    public long getDeathTime() { return deathTime; }
    public double getCost() { return cost; }
    public double getPointsCost() { return pointsCost; }
    public String getDeathReason() { return deathReason; }
    public UUID getKillerUuid() { return killerUuid; }
    public String getKillerName() { return killerName; }
    public boolean isPvpDeath() { return killerUuid != null; }

    public long getTimeLeft(long redemptionTimeMs) {
        long elapsed = System.currentTimeMillis() - deathTime;
        return Math.max(0, redemptionTimeMs - elapsed);
    }

    public boolean isExpired(long redemptionTimeMs) {
        return getTimeLeft(redemptionTimeMs) <= 0;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        PendingItem other = (PendingItem) obj;
        return deathTime == other.deathTime && item != null && other.item != null && item.isSimilar(other.item);
    }

    @Override
    public int hashCode() {
        int result = item != null ? item.hashCode() : 0;
        result = 31 * result + (int) (deathTime ^ (deathTime >>> 32));
        return result;
    }
}
