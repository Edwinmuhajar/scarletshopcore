package com.example.scarletshopcore;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/** /money, /shards, /pay, /eco */
public class EconomyCommands implements TabExecutor {

    private static final double MAX_AMOUNT = 1_000_000_000_000.0;

    private final ScarletShopCore plugin;

    public EconomyCommands(ScarletShopCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        switch (cmd.getName().toLowerCase()) {
            case "money":  return balance(sender, args, Currency.MONEY);
            case "shards": return balance(sender, args, Currency.SHARDS);
            case "pay":    return pay(sender, args);
            case "eco":    return eco(sender, args);
            default:       return false;
        }
    }

    // ---------- /money [pemain]  dan  /shards [pemain] ----------
    private boolean balance(CommandSender sender, String[] args, Currency c) {
        EconomyManager economy = plugin.economy();

        if (args.length == 0) {
            if (!(sender instanceof Player player)) {
                sender.sendMessage(plugin.msg("player-only"));
                return true;
            }
            sender.sendMessage(plugin.msg(c.key() + "-self",
                    "amount", economy.format(c, economy.get(player.getUniqueId(), c))));
            return true;
        }

        if (!sender.hasPermission("scarletshopcore.balance.others")) {
            sender.sendMessage(plugin.msg("no-permission"));
            return true;
        }
        OfflinePlayer target = findPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(plugin.msg("player-not-found", "player", args[0]));
            return true;
        }
        sender.sendMessage(plugin.msg(c.key() + "-other",
                "player", String.valueOf(target.getName()),
                "amount", economy.format(c, economy.get(target.getUniqueId(), c))));
        return true;
    }

    // ---------- /pay <pemain> <jumlah> [money|shards] ----------
    private boolean pay(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.msg("player-only"));
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(plugin.msg("pay-usage"));
            return true;
        }

        Currency c = Currency.parse(args.length > 2 ? args[2] : null, Currency.MONEY);
        if (c == null) {
            sender.sendMessage(plugin.msg("pay-usage"));
            return true;
        }

        OfflinePlayer target = findPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(plugin.msg("player-not-found", "player", args[0]));
            return true;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            sender.sendMessage(plugin.msg("pay-self"));
            return true;
        }

        Double amount = parseAmount(args[1], c, false);
        if (amount == null) {
            sender.sendMessage(plugin.msg("invalid-amount"));
            return true;
        }

        EconomyManager economy = plugin.economy();
        if (!economy.withdraw(player.getUniqueId(), c, amount)) {
            sender.sendMessage(plugin.msg("not-enough", "currency", c.key()));
            return true;
        }
        economy.deposit(target.getUniqueId(), c, amount);

        String formatted = economy.format(c, amount);
        player.sendMessage(plugin.msg("pay-sent", "amount", formatted, "player", String.valueOf(target.getName())));
        Player online = target.getPlayer();
        if (online != null) {
            online.sendMessage(plugin.msg("pay-received", "amount", formatted, "player", player.getName()));
        }
        return true;
    }

    // ---------- /eco <give|take|set> <pemain> <jumlah> [money|shards] ----------
    private boolean eco(CommandSender sender, String[] args) {
        if (!sender.hasPermission("scarletshopcore.admin")) {
            sender.sendMessage(plugin.msg("no-permission"));
            return true;
        }
        if (args.length < 3) {
            sender.sendMessage(plugin.msg("eco-usage"));
            return true;
        }

        String action = args[0].toLowerCase();
        if (!action.equals("give") && !action.equals("take") && !action.equals("set")) {
            sender.sendMessage(plugin.msg("eco-usage"));
            return true;
        }

        Currency c = Currency.parse(args.length > 3 ? args[3] : null, Currency.MONEY);
        if (c == null) {
            sender.sendMessage(plugin.msg("eco-usage"));
            return true;
        }

        OfflinePlayer target = findPlayer(args[1]);
        if (target == null) {
            sender.sendMessage(plugin.msg("player-not-found", "player", args[1]));
            return true;
        }

        Double amount = parseAmount(args[2], c, action.equals("set"));
        if (amount == null) {
            sender.sendMessage(plugin.msg("invalid-amount"));
            return true;
        }

        EconomyManager economy = plugin.economy();
        UUID id = target.getUniqueId();
        switch (action) {
            case "give": economy.deposit(id, c, amount); break;
            case "take": economy.set(id, c, economy.get(id, c) - amount); break;
            default:     economy.set(id, c, amount); break;
        }

        sender.sendMessage(plugin.msg("eco-done",
                "currency", c.key(),
                "player", String.valueOf(target.getName()),
                "amount", economy.format(c, economy.get(id, c))));
        return true;
    }

    // ---------- helper ----------
    @SuppressWarnings("deprecation")
    private OfflinePlayer findPlayer(String name) {
        Player online = Bukkit.getPlayerExact(name);
        if (online != null) return online;
        OfflinePlayer offline = Bukkit.getOfflinePlayer(name);
        return offline.hasPlayedBefore() ? offline : null;
    }

    private Double parseAmount(String input, Currency c, boolean allowZero) {
        double value;
        try {
            value = Double.parseDouble(input);
        } catch (NumberFormatException e) {
            return null;
        }
        if (Double.isNaN(value) || Double.isInfinite(value) || value > MAX_AMOUNT) return null;

        if (c == Currency.SHARDS) {
            if (value != Math.floor(value)) return null;   // shards harus bulat
        } else {
            value = Math.round(value * 100.0) / 100.0;     // money maksimal 2 desimal
        }
        if (value < 0 || (value == 0 && !allowZero)) return null;
        return value;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        String name = cmd.getName().toLowerCase();
        List<String> options = new ArrayList<>();

        if (name.equals("eco")) {
            if (args.length == 1) options = List.of("give", "take", "set");
            else if (args.length == 2) options = onlineNames();
            else if (args.length == 4) options = List.of("money", "shards");
        } else if (name.equals("pay")) {
            if (args.length == 1) options = onlineNames();
            else if (args.length == 3) options = List.of("money", "shards");
        } else if (args.length == 1) {
            options = onlineNames();
        }

        String last = args[args.length - 1].toLowerCase();
        return options.stream()
                .filter(s -> s.toLowerCase().startsWith(last))
                .collect(Collectors.toList());
    }

    private List<String> onlineNames() {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
    }
}
