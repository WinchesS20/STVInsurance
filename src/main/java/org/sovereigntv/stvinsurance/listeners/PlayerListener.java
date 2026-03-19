package org.sovereigntv.stvinsurance.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.sovereigntv.stvinsurance.Main;
import org.sovereigntv.stvinsurance.data.PlayerData;

public class PlayerListener implements Listener {

    private final Main plugin;

    public PlayerListener(Main plugin) {
        this.plugin = plugin;
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        PlayerData data = plugin.getDataManager().getPlayerData(player.getUniqueId());

        if (!data.getPendingItems().isEmpty()) {
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
                if (player.isOnline()) {
                    plugin.getMessageUtils().sendMessage(player, "death-notification");
                }
            }, 60L);
        }
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        plugin.getDataManager().savePlayerData(event.getPlayer().getUniqueId());
    }
}
