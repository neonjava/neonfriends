package me.neonjava.neonfriends.listeners;

import me.neonjava.neonfriends.NeonFriends;
import me.neonjava.neonfriends.gui.FriendGUI;
import me.neonjava.neonfriends.manager.FriendManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.UUID;

public class GUIMenuListener implements Listener {

    private final NeonFriends plugin;
    private final FriendManager friendManager;

    public GUIMenuListener(NeonFriends plugin) {
        this.plugin = plugin;
        this.friendManager = plugin.getFriendManager();
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        String title = event.getView().getTitle();
        if (!title.equals(ChatColor.DARK_GRAY + "Friends List")
                && !title.equals(ChatColor.DARK_GRAY + "Pending Requests")) {
            return;
        }

        event.setCancelled(true);

        if (event.getCurrentItem() == null || event.getCurrentItem().getType() == Material.AIR) {
            return;
        }

        Player player = (Player) event.getWhoClicked();
        ItemStack item = event.getCurrentItem();

        if (title.equals(ChatColor.DARK_GRAY + "Friends List")) {
            handleFriendsListClick(player, item, event);
        } else if (title.equals(ChatColor.DARK_GRAY + "Pending Requests")) {
            handlePendingRequestsClick(player, item, event);
        }
    }

    private void handleFriendsListClick(Player player, ItemStack item, InventoryClickEvent event) {
        if (item.getType() == Material.PAPER && item.getItemMeta().getDisplayName().contains("Pending Requests")) {
            new FriendGUI(player, plugin).openPending();
            return;
        }

        if (item.getType() == Material.PLAYER_HEAD) {
            SkullMeta meta = (SkullMeta) item.getItemMeta();
            if (meta.getOwningPlayer() != null) {
                UUID targetUUID = meta.getOwningPlayer().getUniqueId();
                String targetName = meta.getDisplayName();

                // Left click to remove
                if (event.isLeftClick()) {
                    friendManager.removeFriend(player.getUniqueId(), targetUUID);
                    player.sendMessage(
                            ChatColor.YELLOW + "Removed " + targetName + ChatColor.YELLOW + " from your friends list.");
                    player.closeInventory();
                    new FriendGUI(player, plugin).open();
                }
            }
        }
    }

    private void handlePendingRequestsClick(Player player, ItemStack item, InventoryClickEvent event) {
        if (item.getType() == Material.ARROW && item.getItemMeta().getDisplayName().contains("Back to Friends")) {
            new FriendGUI(player, plugin).open();
            return;
        }

        if (item.getType() == Material.PLAYER_HEAD) {
            SkullMeta meta = (SkullMeta) item.getItemMeta();
            if (meta.getOwningPlayer() != null) {
                UUID requesterUUID = meta.getOwningPlayer().getUniqueId();
                String requesterName = meta.getDisplayName();

                if (event.isLeftClick()) {
                    friendManager.acceptFriendRequest(requesterUUID, player.getUniqueId());
                    player.sendMessage(ChatColor.GREEN + "Accepted friend request from " + requesterName);

                    Player requester = Bukkit.getPlayer(requesterUUID);
                    if (requester != null) {
                        requester.sendMessage(ChatColor.GREEN + player.getName() + " accepted your friend request!");
                    }

                    player.closeInventory();
                    new FriendGUI(player, plugin).openPending();
                } else if (event.isRightClick()) {
                    friendManager.rejectFriendRequest(requesterUUID, player.getUniqueId());
                    player.sendMessage(ChatColor.RED + "Rejected friend request from " + requesterName);

                    player.closeInventory();
                    new FriendGUI(player, plugin).openPending();
                }
            }
        }
    }
}
