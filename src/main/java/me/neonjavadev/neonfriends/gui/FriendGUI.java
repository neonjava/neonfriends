package me.neonjavadev.neonfriends.gui;

import me.neonjavadev.neonfriends.NeonFriends;
import me.neonjavadev.neonfriends.manager.FriendManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class FriendGUI {

    private final Player player;
    private final NeonFriends plugin;
    private final FriendManager friendManager;

    public FriendGUI(Player player, NeonFriends plugin) {
        this.player = player;
        this.plugin = plugin;
        this.friendManager = plugin.getFriendManager();
    }

    public void open() {
        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.DARK_GRAY + "Friends List");

        List<UUID> friends = friendManager.getFriends(player.getUniqueId());

        int slot = 0;
        for (UUID friendUUID : friends) {
            if (slot >= 45)
                break; // Limit to 45 friends for now

            inv.setItem(slot++, createSkullItem(friendUUID, true));
        }

        // Fill empty background
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, filler);
        }

        // Pending requests button
        inv.setItem(49, createItem(Material.PAPER, ChatColor.YELLOW + "Pending Requests"));

        player.openInventory(inv);
    }

    public void openPending() {
        Inventory inv = Bukkit.createInventory(null, 54, ChatColor.DARK_GRAY + "Pending Requests");

        List<UUID> pending = friendManager.getPendingRequests(player.getUniqueId());

        int slot = 0;
        for (UUID requesterUUID : pending) {
            if (slot >= 45)
                break;
            inv.setItem(slot++, createSkullItem(requesterUUID, false));
        }

        // Fill empty background
        ItemStack filler = createItem(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) {
            inv.setItem(i, filler);
        }

        // Back button
        inv.setItem(49, createItem(Material.ARROW, ChatColor.RED + "Back to Friends"));

        player.openInventory(inv);
    }

    private ItemStack createSkullItem(UUID uuid, boolean isFriend) {
        ItemStack item = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) item.getItemMeta();

        String name = friendManager.getPlayerName(uuid);
        meta.setDisplayName(ChatColor.AQUA + name);

        meta.setOwningPlayer(Bukkit.getOfflinePlayer(uuid));

        List<String> lore = new ArrayList<>();
        boolean online = Bukkit.getPlayer(uuid) != null;
        lore.add(ChatColor.GRAY + "Status: " + (online ? ChatColor.GREEN + "Online" : ChatColor.RED + "Offline"));

        if (isFriend) {
            lore.add("");
            lore.add(ChatColor.RED + "Left-Click to Remove Friend");
        } else {
            lore.add("");
            lore.add(ChatColor.GREEN + "Left-Click to Accept");
            lore.add(ChatColor.RED + "Right-Click to Reject");
        }

        meta.setLore(lore);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createItem(Material material, String name) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        meta.setDisplayName(name);
        item.setItemMeta(meta);
        return item;
    }
}
