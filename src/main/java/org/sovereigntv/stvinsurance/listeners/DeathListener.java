package org.sovereigntv.stvinsurance.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;
import org.sovereigntv.stvinsurance.Main;

import java.util.ArrayList;
import java.util.List;

public class DeathListener implements Listener {

    private final Main plugin;

    public DeathListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Player killer = player.getKiller();

        List<ItemStack> drops = new ArrayList<>(event.getDrops());

        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && plugin.getInsuranceManager().isInsured(item)) {
                if (!containsItem(drops, item)) {
                    drops.add(item.clone());
                }
            }
        }

        List<ItemStack> remainingDrops = plugin.getInsuranceManager().processDeathItems(player, drops, killer);

        event.getDrops().clear();
        event.getDrops().addAll(remainingDrops);

        for (ItemStack item : player.getInventory().getContents()) {
            if (item != null && plugin.getInsuranceManager().isInsured(item)) {
                player.getInventory().remove(item);
            }
        }
    }

    private boolean containsItem(List<ItemStack> items, ItemStack target) {
        for (ItemStack item : items) {
            if (item != null && item.isSimilar(target)) return true;
        }
        return false;
    }
}
