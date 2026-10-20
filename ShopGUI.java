package com.example.scarletshopcore;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.ItemFlag;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ShopGUI {

    public static final int BACK_SLOT = 45;

    /** Penanda inventory milik shop + data slot -> kategori/item. */
    public static class Holder implements InventoryHolder {
        private final boolean mainMenu;
        private Inventory inventory;
        private int balanceSlot;
        private final Map<Integer, String> categorySlots = new HashMap<>();
        private final Map<Integer, ShopManager.ShopItem> itemSlots = new HashMap<>();

        Holder(boolean mainMenu) {
            this.mainMenu = mainMenu;
        }

        @Override
        public Inventory getInventory() { return inventory; }

        public boolean isMainMenu() { return mainMenu; }
        public Map<Integer, String> categorySlots() { return categorySlots; }
        public Map<Integer, ShopManager.ShopItem> itemSlots() { return itemSlots; }
    }

    private final ScarletShopCore plugin;

    public ShopGUI(ScarletShopCore plugin) {
        this.plugin = plugin;
    }

    // ------------------------------------------------------------------
    //  MENU UTAMA (daftar kategori)
    // ------------------------------------------------------------------
    public void openMain(Player player) {
        ShopManager shop = plugin.shop();
        Holder holder = new Holder(true);
        Inventory inv = Bukkit.createInventory(holder, 27, shop.title());
        holder.inventory = inv;
        fill(inv, 0);

        holder.balanceSlot = 22;
        inv.setItem(22, balanceItem(player));

        Set<Integer> used = new HashSet<>();
        used.add(22);

        // 1) kategori dengan slot manual
        for (ShopManager.Category c : shop.categories()) {
            if (c.slot() >= 0 && c.slot() < inv.getSize()) {
                inv.setItem(c.slot(), c.icon().clone());
                holder.categorySlots.put(c.slot(), c.id());
                used.add(c.slot());
            }
        }
        // 2) kategori tanpa slot -> isi slot kosong mulai dari 10
        int next = 10;
        for (ShopManager.Category c : shop.categories()) {
            if (c.slot() >= 0 && c.slot() < inv.getSize()) continue;
            while (next < inv.getSize() && used.contains(next)) next++;
            if (next >= inv.getSize()) break;
            inv.setItem(next, c.icon().clone());
            holder.categorySlots.put(next, c.id());
            used.add(next);
        }

        player.openInventory(inv);
    }

    // ------------------------------------------------------------------
    //  MENU KATEGORI (daftar item)
    // ------------------------------------------------------------------
    public void openCategory(Player player, ShopManager.Category category) {
        Holder holder = new Holder(false);
        Inventory inv = Bukkit.createInventory(holder, 54, category.name());
        holder.inventory = inv;
        fill(inv, 45);

        List<ShopManager.ShopItem> items = category.items();
        for (int i = 0; i < Math.min(45, items.size()); i++) {
            inv.setItem(i, display(items.get(i)));
            holder.itemSlots.put(i, items.get(i));
        }

        inv.setItem(BACK_SLOT, named(Material.ARROW,
                plugin.getConfig().getString("gui.back-name", "&cKembali")));

        holder.balanceSlot = 49;
        inv.setItem(49, balanceItem(player));

        player.openInventory(inv);
    }

    /** Perbarui item saldo setelah pembelian. */
    public void refreshBalance(Player player, Holder holder) {
        if (holder.inventory != null) {
            holder.inventory.setItem(holder.balanceSlot, balanceItem(player));
        }
    }

    // ------------------------------------------------------------------
    //  helper
    // ------------------------------------------------------------------
    private ItemStack display(ShopManager.ShopItem shopItem) {
        EconomyManager economy = plugin.economy();
        ItemStack item = shopItem.stack().clone();
        ItemMeta meta = item.getItemMeta();

        List<String> lore = meta.hasLore() ? new ArrayList<>(meta.getLore()) : new ArrayList<>();
        lore.add("");
        lore.add(ScarletShopCore.color(plugin.getConfig().getString("gui.price-line", "&7Harga: &e{price}")
                .replace("{price}", economy.format(shopItem.currency(), shopItem.price()))));
        lore.add(ScarletShopCore.color(plugin.getConfig().getString("gui.click-line", "&aKlik untuk membeli")));
        meta.setLore(lore);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack balanceItem(Player player) {
        EconomyManager economy = plugin.economy();
        ItemStack item = new ItemStack(Material.GOLD_INGOT);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ScarletShopCore.color(plugin.getConfig().getString("gui.balance-name", "&6Saldo Kamu")));
        meta.setLore(List.of(
                ScarletShopCore.color("&7Money: &e" + economy.format(Currency.MONEY, economy.get(player.getUniqueId(), Currency.MONEY))),
                ScarletShopCore.color("&7Shards: &d" + economy.format(Currency.SHARDS, economy.get(player.getUniqueId(), Currency.SHARDS)))
        ));
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack named(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(ScarletShopCore.color(name));
        item.setItemMeta(meta);
        return item;
    }

    private void fill(Inventory inv, int fromSlot) {
        ItemStack filler = new ItemStack(Material.GRAY_STAINED_GLASS_PANE);
        ItemMeta meta = filler.getItemMeta();
        meta.setDisplayName(" ");
        filler.setItemMeta(meta);
        for (int i = fromSlot; i < inv.getSize(); i++) {
            inv.setItem(i, filler);
        }
    }
}
