package me.neonjavadev.neonfriends;

import me.neonjavadev.neonfriends.commands.FriendCommand;
import me.neonjavadev.neonfriends.listeners.FriendListener;
import me.neonjavadev.neonfriends.listeners.GUIMenuListener;
import me.neonjavadev.neonfriends.manager.FriendManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public class NeonFriends extends JavaPlugin {

    private static NeonFriends instance;
    private FriendManager friendManager;

    @Override
    public void onEnable() {
        instance = this;

        try {
            saveDefaultConfig();

            this.friendManager = new FriendManager(this);

            FriendCommand friendCommand = new FriendCommand(this);
            getCommand("friend").setExecutor(friendCommand);
            getCommand("friend").setTabCompleter(friendCommand);

            getServer().getPluginManager().registerEvents(new FriendListener(this), this);
            getServer().getPluginManager().registerEvents(new GUIMenuListener(this), this);

            getLogger().info("NeonFriends has been enabled successfully!");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to enable NeonFriends!", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (friendManager != null) {
            friendManager.closeConnection();
        }
        getLogger().info("NeonFriends has been disabled!");
    }

    public static NeonFriends getInstance() {
        return instance;
    }

    public FriendManager getFriendManager() {
        return friendManager;
    }
}
