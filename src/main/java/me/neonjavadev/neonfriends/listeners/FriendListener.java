package me.neonjavadev.neonfriends.listeners;

import me.neonjavadev.neonfriends.NeonFriends;
import me.neonjavadev.neonfriends.manager.FriendManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.util.List;
import java.util.UUID;

public class FriendListener implements Listener {

    private final NeonFriends plugin;
    private final FriendManager friendManager;

    public FriendListener(NeonFriends plugin) {
        this.plugin = plugin;
        this.friendManager = plugin.getFriendManager();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();

        // Update player name in database
        friendManager.updatePlayerName(player.getUniqueId(), player.getName());

        // Notify friends that this player joined
        List<UUID> friends = friendManager.getFriends(player.getUniqueId());

        for (UUID friendUUID : friends) {
            Player friend = Bukkit.getPlayer(friendUUID);
            if (friend != null && friend.isOnline()) {
                friend.sendMessage(
                        ChatColor.AQUA + "[Friends] " + ChatColor.WHITE + player.getName() + " has joined the server!");
            }
        }

        // Check for pending requests and notify the player
        List<UUID> pending = friendManager.getPendingRequests(player.getUniqueId());
        if (!pending.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "You have " + pending.size()
                    + " pending friend requests! Use /friend to view them.");
        }
    }
}
