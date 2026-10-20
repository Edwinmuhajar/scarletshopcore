package com.example.scarletshopcore;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

public class ShopListener implements Listener {

    private final ScarletShopCore plugin;

    public ShopListener(ScarletShopCore plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        if (!(event.getInventory().getHolder() instanceof ShopGUI.Holder holder)) return;

        // Semua klik di GUI shop dibatalkan (termasuk shift-click dari inventory pemain)
        event.setCancelled(true);

        if (!(event.getWhoClicked() instanceof Player player)) return;
        if (event.getClickedInventory() == null || !event.getClickedInventory().equals(event.getInventory())) return;

        int slot = event.getSlot();
        ShopGUI gui = plugin.gui();

        if (holder.isMainMenu()) {
            String categoryId = holder.categorySlots().get(slot);
            if (categoryId == null) return;
            ShopManager.Category category = plugin.shop().category(categoryId);
            if (category != null) gui.openCategory(player, category);
            return;
        }

        if (slot == ShopGUI.BACK_SLOT) {
            gui.openMain(player);
            return;
        }

        ShopManager.ShopItem item = holder.itemSlots().get(slot);
        if (item != null) {
            plugin.shop().purchase(player, item);
            gui.refreshBalance(player, holder);
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent event) {
        if (event.getInventory().getHolder() instanceof ShopGUI.Holder) {
            event.setCancelled(true);
        }
    }
}
