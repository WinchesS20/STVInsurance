package org.sovereigntv.stvinsurance.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.sovereigntv.stvinsurance.Main;

import java.util.ArrayList;
import java.util.List;

public class InsuranceCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;

    public InsuranceCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            plugin.getMessageUtils().sendRawMessage((Player) sender,
                    plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMessage("player-only"));
            return true;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("stvinsurance.use")) {
            plugin.getMessageUtils().sendMessage(player, "no-permission");
            return true;
        }

        plugin.getMenuManager().openMainMenu(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        return new ArrayList<>();
    }
}
