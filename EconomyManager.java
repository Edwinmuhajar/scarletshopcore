package com.example.scarletshopcore;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/** Menyimpan saldo money & shards semua pemain (data.yml). */
public class EconomyManager {

    private final ScarletShopCore plugin;
    private final File file;
    private final Map<Currency, Map<UUID, Double>> balances = new EnumMap<>(Currency.class);

    public EconomyManager(ScarletShopCore plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "data.yml");
        for (Currency c : Currency.values()) {
            balances.put(c, new HashMap<>());
        }
    }

    public void load() {
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection players = cfg.getConfigurationSection("players");
        if (players == null) return;

        for (String key : players.getKeys(false)) {
            try {
                UUID id = UUID.fromString(key);
                for (Currency c : Currency.values()) {
                    balances.get(c).put(id, players.getDouble(key + "." + c.key(), start(c)));
                }
            } catch (IllegalArgumentException ignored) {
                // UUID tidak valid, lewati
            }
        }
    }

    public void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        Set<UUID> ids = new HashSet<>();
        for (Currency c : Currency.values()) {
            ids.addAll(balances.get(c).keySet());
        }
        for (UUID id : ids) {
            for (Currency c : Currency.values()) {
                cfg.set("players." + id + "." + c.key(), get(id, c));
            }
        }
        try {
            cfg.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Gagal menyimpan data.yml: " + e.getMessage());
        }
    }

    private double start(Currency c) {
        return plugin.getConfig().getDouble("starting." + c.key(), 0.0);
    }

    public double get(UUID id, Currency c) {
        return balances.get(c).computeIfAbsent(id, k -> start(c));
    }

    public void set(UUID id, Currency c, double amount) {
        balances.get(c).put(id, round(c, Math.max(0, amount)));
    }

    public void deposit(UUID id, Currency c, double amount) {
        set(id, c, get(id, c) + amount);
    }

    /** Kurangi saldo. Return false (dan tidak mengubah apa pun) jika saldo tidak cukup. */
    public boolean withdraw(UUID id, Currency c, double amount) {
        double current = get(id, c);
        if (current < amount) return false;
        set(id, c, current - amount);
        return true;
    }

    private double round(Currency c, double value) {
        return c == Currency.SHARDS ? Math.floor(value) : Math.round(value * 100.0) / 100.0;
    }

    public String format(Currency c, double amount) {
        DecimalFormatSymbols symbols = DecimalFormatSymbols.getInstance(Locale.US);
        if (c == Currency.SHARDS) {
            String number = new DecimalFormat("#,##0", symbols).format(amount);
            return plugin.getConfig().getString("currency.shards-format", "{amount} Shards")
                    .replace("{amount}", number);
        }
        String number = new DecimalFormat("#,##0.##", symbols).format(amount);
        return plugin.getConfig().getString("currency.money-format", "${amount}")
                .replace("{amount}", number);
    }
}
