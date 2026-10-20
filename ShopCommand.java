package com.example.scarletshopcore;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/** /shop            -> menu utama
 *  /shop <kategori> -> langsung buka kategori
 *  /shop reload     -> reload config.yml & shop.yml (admin) */
public class ShopCommand implements TabExecutor {

    private final ScarletShopCore plugin;

    public ShopCommand(ScarletShopCore plugin) {
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
            if (!sender.hasPermission("scarletshopcore.admin")) {
                sender.sendMessage(plugin.msg("no-permission"));
                return true;
            }
            plugin.reloadConfig();
            plugin.shop().load();
            sender.sendMessage(plugin.msg("shop-reloaded"));
            return true;
        }

        if (!(sender instanceof Player player)) {
            sender.sendMessage(plugin.msg("player-only"));
            return true;
        }

        if (args.length > 0) {
            ShopManager.Category category = plugin.shop().category(args[0]);
            if (category != null) {
                plugin.gui().openCategory(player, category);
                return true;
            }
        }
        plugin.gui().openMain(player);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command cmd, String label, String[] args) {
        if (args.length != 1) return List.of();

        List<String> options = new ArrayList<>();
        plugin.shop().categories().forEach(c -> options.add(c.id()));
        if (sender.hasPermission("scarletshopcore.admin")) options.add("reload");

        String last = args[0].toLowerCase();
        return options.stream().filter(s -> s.toLowerCase().startsWith(last)).collect(Collectors.toList());
    }
}
