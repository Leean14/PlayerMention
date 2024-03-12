package com.galaxycraft.playermention;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Sound;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PlayerMention extends JavaPlugin implements Listener {

    private ChatColor mentionColor;
    private Map<UUID, Boolean> mentionStates = new HashMap<>();
    private Map<UUID, Long> mentionCooldowns = new HashMap<>();
    private int mentionCooldownSeconds;
    public static String prefixPlugin = "&8[&5Player&dMention&8] ";
    private String version = getDescription().getVersion();
    private String prefix;

    @Override
    public void onEnable() {
        // Registers the events and configures the plugin.
        Bukkit.getPluginManager().registerEvents(this, this);
        saveDefaultConfig(); // Saves the default configuration file if it does not exist.
        loadConfigValues(); // Loads the configuration values.
        // Prints messages to the console after plugin initialization.
        Bukkit.getConsoleSender().sendMessage(
                ChatColor.translateAlternateColorCodes('&', prefixPlugin+"&ahas been enabled successfully."+version));
        Bukkit.getConsoleSender().sendMessage(
                ChatColor.translateAlternateColorCodes('&', prefixPlugin+"&ahas been disabled - Thank you for using my plugin."));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // Main command of the plugin (/pm)
        if (label.equalsIgnoreCase("pm") || label.equalsIgnoreCase("playermention")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("pm.reload")) {
                    reloadConfig(); // Reloads the configuration
                    loadConfigValues(); // Loads the configuration values
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix+"&aConfiguration reloaded successfully."));
                    return true;
                } else {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix+noPermissionMessage));
                    return false;
                }
            } else if (args.length > 0 && args[0].equalsIgnoreCase("on")) {
                if (sender.hasPermission("pm.use")) {
                    mentionStates.put(((Player) sender).getUniqueId(), true); // Activa las menciones del jugador
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + "&aYour mentions have been activated."));
                    return true;
                }
            } else if (args.length > 0 && args[0].equalsIgnoreCase("off")) {
                if (sender.hasPermission("pm.use")) {
                    mentionStates.put(((Player) sender).getUniqueId(), false); // Desactiva las menciones del jugador
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix + "&aYour mentions have been deactivated."));
                    return true;
                } else {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix+noPermissionMessage));
                    return false;
                }
            } else {
                if (sender.hasPermission("pm.use")) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix+"&7You are using version "+version));
                } else {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix+noPermissionMessage));
                }
                return false;
            }
        }
        return false;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        Player sender = event.getPlayer();
        UUID senderUUID = sender.getUniqueId();
        boolean mentionsEnabled = mentionStates.getOrDefault(senderUUID, true); // By default, mentions are enabled.

        if (!mentionsEnabled) {
            return; // If mentions are disabled for the player, nothing happens
        }

        // Check if the player has recently mentioned.
        if (mentionCooldowns.containsKey(senderUUID)) {
            long lastMentionTime = mentionCooldowns.get(senderUUID);
            long currentTime = System.currentTimeMillis();
            long elapsedTime = currentTime - lastMentionTime;
            if (elapsedTime < mentionCooldownSeconds * 1000) {
                // If the elapsed time is less than the cooldown time, cancel the mention.
                event.setCancelled(true);
                String message = mentionCooldownMessage.replace("%time%", String.valueOf(mentionCooldownSeconds));
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', message));
                return;
            }
        }

        String message = event.getMessage();

        for (Player player : Bukkit.getOnlinePlayers()) {
            String playerName = player.getName();
            if (message.contains("@" + playerName)) {
                // Play the sound for the mentioned player.
                player.playSound(player.getLocation(), getMentionSound(), 1.0f, 1.0f);

                // Change the color of the mentioned player's name.
                String newMessage = message.replace("@" + playerName, mentionColor + "@" + playerName + ChatColor.RESET);
                event.setMessage(newMessage);

                // Update the timestamp of the last mention for the player.
                mentionCooldowns.put(senderUUID, System.currentTimeMillis());

            }

        }
    }

    private String mentionCooldownMessage;
    private String noPermissionMessage;
    private void loadConfigValues() {
        FileConfiguration config = getConfig();
        String colorName = config.getString("mention_color", "RED");
        mentionColor = ChatColor.valueOf(colorName.toUpperCase());
        // Get the cooldown time from the configuration.
        mentionCooldownSeconds = config.getInt("mention_cooldown_seconds", 10);
        // Load the messages from the configuration.
        mentionCooldownMessage = config.getString("mention_cooldown_message", "&cYou must wait %time% seconds between each mention.");
        // Load the denied permission message from the configuration.
        noPermissionMessage = config.getString("no_permission_message", "&cYou do not have permission to execute this command.");
        // Load the prefix for the messages.
        prefix = config.getString("prefix", "&8[&5Player&dMention&8] ");

    }
    private Sound getMentionSound() {
        // Retrieve the sound from the configuration.
        String soundName = getConfig().getString("mention_sound", "ENTITY_PLAYER_LEVELUP");
        Sound sound = Sound.valueOf(soundName);
        if (sound == null) {
            getLogger().warning(prefix+"The specified sound was not found in the configuration. Using the default sound.");
            sound = Sound.ENTITY_PLAYER_LEVELUP;
        }
        return sound;
    }
}