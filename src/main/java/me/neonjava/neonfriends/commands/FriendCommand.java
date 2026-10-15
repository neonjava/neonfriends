package me.neonjava.neonfriends.commands;

import me.neonjava.neonfriends.NeonFriends;
import me.neonjava.neonfriends.gui.FriendGUI;
import me.neonjava.neonfriends.manager.FriendManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class FriendCommand implements CommandExecutor, TabCompleter {

    private final NeonFriends plugin;
    private final FriendManager friendManager;

    public FriendCommand(NeonFriends plugin) {
        this.plugin = plugin;
        this.friendManager = plugin.getFriendManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this command.");
            return true;
        }

        Player player = (Player) sender;

        if (args.length == 0) {
            new FriendGUI(player, plugin).open();
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "add":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /friend add <player>");
                    return true;
                }
                handleAdd(player, args[1]);
                break;
            case "remove":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /friend remove <player>");
                    return true;
                }
                handleRemove(player, args[1]);
                break;
            case "accept":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /friend accept <player>");
                    return true;
                }
                handleAccept(player, args[1]);
                break;
            case "reject":
                if (args.length < 2) {
                    player.sendMessage(ChatColor.RED + "Usage: /friend reject <player>");
                    return true;
                }
                handleReject(player, args[1]);
                break;
            case "list":
                handleList(player);
                break;
            case "gui":
                new FriendGUI(player, plugin).open();
                break;
            default:
                player.sendMessage(ChatColor.RED + "Unknown subcommand. Use /friend for GUI.");
                break;
        }

        return true;
    }

    private void handleAdd(Player player, String targetName) {
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Player not found or not online.");
            return;
        }

        if (target.getUniqueId().equals(player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You cannot add yourself.");
            return;
        }

        if (friendManager.areFriends(player.getUniqueId(), target.getUniqueId())) {
            player.sendMessage(ChatColor.YELLOW + "You are already friends with " + target.getName());
            return;
        }

        if (friendManager.hasPendingRequest(player.getUniqueId(), target.getUniqueId())) {
            player.sendMessage(ChatColor.YELLOW + "You have already sent a friend request to " + target.getName());
            return;
        }

        // If target already sent request to player, accept it instead
        if (friendManager.hasPendingRequest(target.getUniqueId(), player.getUniqueId())) {
            friendManager.acceptFriendRequest(target.getUniqueId(), player.getUniqueId());
            player.sendMessage(ChatColor.GREEN + "Accepted friend request from " + target.getName());
            target.sendMessage(ChatColor.GREEN + player.getName() + " accepted your friend request!");
            return;
        }

        friendManager.sendFriendRequest(player.getUniqueId(), target.getUniqueId());
        player.sendMessage(ChatColor.GREEN + "Friend request sent to " + target.getName());
        target.sendMessage(ChatColor.YELLOW + "You received a friend request from " + player.getName()
                + ". Use /friend accept " + player.getName() + " to accept.");
    }

    private void handleRemove(Player player, String targetName) {
        // We might need to check offline players too for removal
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        UUID targetUUID = target.getUniqueId();

        if (!friendManager.areFriends(player.getUniqueId(), targetUUID)) {
            player.sendMessage(ChatColor.RED + "You are not friends with this player.");
            return;
        }

        friendManager.removeFriend(player.getUniqueId(), targetUUID);
        player.sendMessage(ChatColor.YELLOW + "Removed " + targetName + " from your friends list.");

        if (target.isOnline()) {
            ((Player) target)
                    .sendMessage(ChatColor.YELLOW + player.getName() + " removed you from their friends list.");
        }
    }

    private void handleAccept(Player player, String targetName) {
        Player target = Bukkit.getPlayer(targetName);
        if (target == null) {
            player.sendMessage(ChatColor.RED + "Player is not online.");
            return;
        }

        if (!friendManager.hasPendingRequest(target.getUniqueId(), player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You don't have a pending request from " + targetName);
            return;
        }

        friendManager.acceptFriendRequest(target.getUniqueId(), player.getUniqueId());
        player.sendMessage(ChatColor.GREEN + "Accepted friend request from " + targetName);
        target.sendMessage(ChatColor.GREEN + player.getName() + " accepted your friend request!");
    }

    private void handleReject(Player player, String targetName) {
        // Find the requester (could be offline)
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetName);
        UUID targetUUID = target.getUniqueId();

        if (!friendManager.hasPendingRequest(targetUUID, player.getUniqueId())) {
            player.sendMessage(ChatColor.RED + "You don't have a pending request from " + targetName);
            return;
        }

        friendManager.rejectFriendRequest(targetUUID, player.getUniqueId());
        player.sendMessage(ChatColor.YELLOW + "Rejected friend request from " + targetName);

        if (target.isOnline()) {
            ((Player) target).sendMessage(ChatColor.YELLOW + player.getName() + " rejected your friend request.");
        }
    }

    private void handleList(Player player) {
        List<UUID> friends = friendManager.getFriends(player.getUniqueId());
        if (friends.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "You have no friends yet.");
            return;
        }

        player.sendMessage(ChatColor.AQUA + "--- Friends List ---");
        for (UUID uuid : friends) {
            String name = friendManager.getPlayerName(uuid);
            boolean online = Bukkit.getPlayer(uuid) != null;
            player.sendMessage(ChatColor.GRAY + "- " + (online ? ChatColor.GREEN : ChatColor.RED) + name
                    + (online ? " (Online)" : " (Offline)"));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            return Arrays.asList("add", "remove", "accept", "reject", "list", "gui").stream()
                    .filter(s -> s.startsWith(args[0].toLowerCase()))
                    .collect(Collectors.toList());
        }

        if (args.length == 2) {
            String sub = args[0].toLowerCase();
            if (sub.equals("add") || sub.equals("accept") || sub.equals("reject") || sub.equals("remove")) {
                return Bukkit.getOnlinePlayers().stream()
                        .map(Player::getName)
                        .filter(name -> name.toLowerCase().startsWith(args[1].toLowerCase()))
                        .collect(Collectors.toList());
            }
        }

        return new ArrayList<>();
    }
}
