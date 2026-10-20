package com.example.scarletshopcore;

import org.bukkit.ChatColor;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;

public class ScarletShopCore extends JavaPlugin {

    private EconomyManager economy;
    private ShopManager shop;
    private ShopGUI gui;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        if (!new File(getDataFolder(), "shop.yml").exists()) {
            saveResource("shop.yml", false);
        }

        economy = new EconomyManager(this);
        economy.load();

        shop = new ShopManager(this);
        shop.load();

        gui = new ShopGUI(this);

        EconomyCommands ecoCommands = new EconomyCommands(this);
        for (String name : new String[]{"money", "shards", "pay", "eco"}) {
            register(name, ecoCommands);
        }
        register("shop", new ShopCommand(this));

        getServer().getPluginManager().registerEvents(new ShopListener(this), this);

        // Auto-save data saldo tiap 5 menit
        getServer().getScheduler().runTaskTimer(this, () -> economy.save(), 6000L, 6000L);
    }

    @Override
    public void onDisable() {
        if (economy != null) economy.save();
    }

    private void register(String name, TabExecutor executor) {
        PluginCommand cmd = getCommand(name);
        if (cmd == null) return;
        cmd.setExecutor(executor);
        cmd.setTabCompleter(executor);
    }

    public EconomyManager economy() { return economy; }
    public ShopManager shop() { return shop; }
    public ShopGUI gui() { return gui; }

    /** Ambil pesan dari config (messages.<key>), ganti placeholder {k} dengan v, beri prefix & warna. */
    public String msg(String key, String... pairs) {
        String text = getConfig().getString("messages." + key, "&cPesan tidak ditemukan: " + key);
        for (int i = 0; i + 1 < pairs.length; i += 2) {
            text = text.replace("{" + pairs[i] + "}", pairs[i + 1]);
        }
        return color(getConfig().getString("messages.prefix", "") + text);
    }

    public static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text == null ? "" : text);
    }
}
