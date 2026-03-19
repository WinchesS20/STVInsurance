package org.sovereigntv.stvinsurance.commands;

import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.sovereigntv.stvinsurance.Main;
import org.sovereigntv.stvinsurance.data.PlayerData;
import org.sovereigntv.stvinsurance.utils.MessageUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class InsuranceAdminCommand implements CommandExecutor, TabCompleter {

    private final Main plugin;

    public InsuranceAdminCommand(Main plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!sender.hasPermission("stvinsurance.admin")) {
            if (sender instanceof Player) {
                plugin.getMessageUtils().sendMessage((Player) sender, "no-permission");
            }
            return true;
        }

        if (args.length == 0) {
            sendUsage(sender);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "reload":
                handleReload(sender);
                break;
            case "info":
                handleInfo(sender, args);
                break;
            default:
                sendUsage(sender);
                break;
        }

        return true;
    }

    private void handleReload(CommandSender sender) {
        plugin.reload();
        sender.sendMessage(MessageUtils.colorize(
                plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMessage("reload-success")
        ));
    }

    private void handleInfo(CommandSender sender, String[] args) {
        if (args.length < 2) {
            sender.sendMessage(MessageUtils.colorize(
                    plugin.getConfigManager().getPrefix() + "&fИспользование: /insadmin info <игрок>"
            ));
            return;
        }

        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(MessageUtils.colorize(
                    plugin.getConfigManager().getPrefix() + plugin.getConfigManager().getMessage("admin-player-not-found")
            ));
            return;
        }

        PlayerData data = plugin.getDataManager().getPlayerData(target.getUniqueId());
        int maxSlots = plugin.getInsuranceManager().getMaxSlots(target);
        int availableSlots = plugin.getInsuranceManager().getAvailableSlots(target);
        int insuredCount = data.getInsuredItems().size();
        int pendingCount = data.getPendingItems().size();
        int returnedCount = data.getReturnedItems().size();
        String tierName = plugin.getInsuranceManager().getSlotTierName(target);

        String permissionInfo = "";
        int maxConfigSlots = plugin.getConfigManager().getMaxSlots();
        for (int i = maxConfigSlots; i >= 2; i--) {
            if (target.hasPermission("stvinsurance.slots." + i)) {
                permissionInfo = "stvinsurance.slots." + i;
                break;
            }
        }
        if (permissionInfo.isEmpty()) {
            permissionInfo = "Нет (базовый)";
        }

        sender.sendMessage(MessageUtils.colorize("&8&m─────────────────────────────"));
        sender.sendMessage(MessageUtils.colorize("§x§0§A§7§8§9§9▶ &fИнформация об игроке: &e" + target.getName()));
        sender.sendMessage(MessageUtils.colorize("&8&m─────────────────────────────"));
        sender.sendMessage(MessageUtils.colorize("&fСтатус: §x§0§A§7§8§9§9" + tierName));
        sender.sendMessage(MessageUtils.colorize("&fПраво на слоты: §x§0§A§7§8§9§9" + permissionInfo));
        sender.sendMessage(MessageUtils.colorize("&fМаксимум слотов: §x§0§A§7§8§9§9" + maxSlots));
        sender.sendMessage(MessageUtils.colorize("&fДоступно слотов: §x§0§A§7§8§9§9" + availableSlots));
        sender.sendMessage(MessageUtils.colorize("&8&m─────────────────────────────"));
        sender.sendMessage(MessageUtils.colorize("&fЗастраховано предметов: §x§0§A§7§8§9§9" + insuredCount));
        sender.sendMessage(MessageUtils.colorize("&fОжидает выкупа: §x§0§A§7§8§9§9" + pendingCount));
        sender.sendMessage(MessageUtils.colorize("&fВозвращённых предметов: §x§0§A§7§8§9§9" + returnedCount));
        sender.sendMessage(MessageUtils.colorize("&8&m─────────────────────────────"));
    }

    private void sendUsage(CommandSender sender) {
        sender.sendMessage(MessageUtils.colorize("&8&m─────────────────────────────"));
        sender.sendMessage(MessageUtils.colorize("§x§0§A§7§8§9§9▶ &fSTVInsurance - Админ команды"));
        sender.sendMessage(MessageUtils.colorize("&8&m─────────────────────────────"));
        sender.sendMessage(MessageUtils.colorize("§x§0§A§7§8§9§9/insadmin reload &f- Перезагрузить конфиг"));
        sender.sendMessage(MessageUtils.colorize("§x§0§A§7§8§9§9/insadmin info <игрок> &f- Информация об игроке"));
        sender.sendMessage(MessageUtils.colorize(""));
        sender.sendMessage(MessageUtils.colorize("&7Система слотов (права):"));
        sender.sendMessage(MessageUtils.colorize("&8 • &fstvinsurance.slots.2 &8- &72 слота"));
        sender.sendMessage(MessageUtils.colorize("&8 • &fstvinsurance.slots.3 &8- &73 слота"));
        sender.sendMessage(MessageUtils.colorize("&8 • &fstvinsurance.slots.4 &8- &74 слота"));
        sender.sendMessage(MessageUtils.colorize("&8 • &fstvinsurance.slots.5 &8- &75 слотов"));
        sender.sendMessage(MessageUtils.colorize("&8 • &7и т.д. до max-slots в конфиге"));
        sender.sendMessage(MessageUtils.colorize("&8&m─────────────────────────────"));
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("stvinsurance.admin")) {
            return new ArrayList<>();
        }

        if (args.length == 1) {
            return Arrays.asList("reload", "info").stream()
                    .filter(s -> s.toLowerCase().startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2 && args[0].equalsIgnoreCase("info")) {
            return Bukkit.getOnlinePlayers().stream()
                    .map(Player::getName)
                    .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                    .collect(Collectors.toList());
        }

        return new ArrayList<>();
    }
}
