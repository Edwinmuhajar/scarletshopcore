package com.example.scarletshopcore;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Membaca shop.yml dan memproses pembelian. */
public class ShopManager {

    public record ShopItem(String id, ItemStack stack, Currency currency, double price, List<String> commands) {
        public String displayName() {
            ItemMeta meta = stack.getItemMeta();
            String base = (meta != null && meta.hasDisplayName())
                    ? meta.getDisplayName()
                    : stack.getType().name().toLowerCase().replace('_', ' ');
            return stack.getAmount() > 1 ? stack.getAmount() + "x " + base : base;
        }
    }

    public record Category(String id, String name, ItemStack icon, int slot, List<ShopItem> items) { }

    private final ScarletShopCore plugin;
    private final Map<String, Category> categories = new LinkedHashMap<>();
    private String title = "Shop";

    public ShopManager(ScarletShopCore plugin) {
        this.plugin = plugin;
    }

    public String title() { return title; }

    public Collection<Category> categories() { return categories.values(); }

    public Category category(String id) {
        for (Category c : categories.values()) {
            if (c.id().equalsIgnoreCase(id)) return c;
        }
        return null;
    }

    // ------------------------------------------------------------------
    //  LOAD shop.yml
    // ------------------------------------------------------------------
    public void load() {
        categories.clear();
        File file = new File(plugin.getDataFolder(), "shop.yml");
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        title = ScarletShopCore.color(cfg.getString("title", "&8Shop"));

        ConfigurationSection cats = cfg.getConfigurationSection("categories");
        if (cats == null) return;

        for (String catId : cats.getKeys(false)) {
            ConfigurationSection cs = cats.getConfigurationSection(catId);
            if (cs == null) continue;

            Material iconMat = Material.matchMaterial(cs.getString("icon", "CHEST"));
            if (iconMat == null || iconMat.isAir()) iconMat = Material.CHEST;

            String name = ScarletShopCore.color(cs.getString("name", catId));
            ItemStack icon = new ItemStack(iconMat);
            ItemMeta meta = icon.getItemMeta();
            meta.setDisplayName(name);
            icon.setItemMeta(meta);

            List<ShopItem> items = new ArrayList<>();
            ConfigurationSection itemsSec = cs.getConfigurationSection("items");
            if (itemsSec != null) {
                for (String itemId : itemsSec.getKeys(false)) {
                    ShopItem item = parseItem(catId, itemId, itemsSec.getConfigurationSection(itemId));
                    if (item != null) items.add(item);
                }
            }
            if (items.size() > 45) {
                plugin.getLogger().warning("[shop.yml] Kategori '" + catId
                        + "' punya " + items.size() + " item; hanya 45 pertama yang ditampilkan.");
            }

            categories.put(catId, new Category(catId, name, icon, cs.getInt("slot", -1), items));
        }
        plugin.getLogger().info("Shop dimuat: " + categories.size() + " kategori.");
    }

    private ShopItem parseItem(String catId, String id, ConfigurationSection s) {
        if (s == null) return null;
        String path = catId + "." + id;

        Material mat = Material.matchMaterial(s.getString("material", ""));
        if (mat == null || mat.isAir() || !mat.isItem()) {
            plugin.getLogger().warning("[shop.yml] " + path + ": material tidak valid, item dilewati.");
            return null;
        }

        Currency currency = Currency.parse(s.getString("currency", "money"), null);
        double price = s.getDouble("price", -1);
        if (currency == null || price < 0) {
            plugin.getLogger().warning("[shop.yml] " + path + ": price/currency tidak valid, item dilewati.");
            return null;
        }
        if (currency == Currency.SHARDS && price != Math.floor(price)) {
            plugin.getLogger().warning("[shop.yml] " + path + ": harga shards harus bilangan bulat, item dilewati.");
            return null;
        }
        price = Math.round(price * 100.0) / 100.0;

        int amount = Math.max(1, Math.min(mat.getMaxStackSize(), s.getInt("amount", 1)));
        ItemStack stack = new ItemStack(mat, amount);
        ItemMeta meta = stack.getItemMeta();

        if (s.contains("name")) {
            meta.setDisplayName(ScarletShopCore.color(s.getString("name", "")));
        }

        List<String> lore = new ArrayList<>();
        for (String line : s.getStringList("lore")) lore.add(ScarletShopCore.color(line));
        if (!lore.isEmpty()) meta.setLore(lore);

        for (String entry : s.getStringList("enchantments")) {
            String[] parts = entry.split(":");
            NamespacedKey key = NamespacedKey.fromString(parts[0].trim().toLowerCase());
            Enchantment enchantment = key == null ? null : Enchantment.getByKey(key);
            int level = 1;
            if (parts.length > 1) {
                try {
                    level = Integer.parseInt(parts[1].trim());
                } catch (NumberFormatException ignored) {
                    // pakai level 1
                }
            }
            if (enchantment == null) {
                plugin.getLogger().warning("[shop.yml] " + path + ": enchantment '" + entry + "' tidak dikenal.");
                continue;
            }
            meta.addEnchant(enchantment, level, true);
        }
        stack.setItemMeta(meta);

        return new ShopItem(id, stack, currency, price, s.getStringList("commands"));
    }

    // ------------------------------------------------------------------
    //  PEMBELIAN
    // ------------------------------------------------------------------
    public void purchase(Player player, ShopItem item) {
        EconomyManager economy = plugin.economy();
        String price = economy.format(item.currency(), item.price());

        if (!economy.withdraw(player.getUniqueId(), item.currency(), item.price())) {
            player.sendMessage(plugin.msg("shop-no-funds", "price", price));
            return;
        }

        if (item.commands().isEmpty()) {
            Map<Integer, ItemStack> leftover = player.getInventory().addItem(item.stack().clone());
            if (!leftover.isEmpty()) {
                for (ItemStack extra : leftover.values()) {
                    player.getWorld().dropItemNaturally(player.getLocation(), extra);
                }
                player.sendMessage(plugin.msg("shop-inv-full"));
            }
        } else {
            for (String command : item.commands()) {
                String cmd = command.replace("%player%", player.getName());
                if (cmd.startsWith("/")) cmd = cmd.substring(1);
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
            }
        }

        player.sendMessage(plugin.msg("shop-bought", "item", item.displayName(), "price", price));
    }
}
