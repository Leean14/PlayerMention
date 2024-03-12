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
    private Map<UUID, Long> mentionCooldowns = new HashMap<>();
    private int mentionCooldownSeconds;
    public static String prefixPlugin = "&8[&5Player&dMention&8] ";
    private String version = getDescription().getVersion();
    private String prefix;
    @Override
    public void onEnable() {
        // Registra los eventos y configura el plugin
        Bukkit.getPluginManager().registerEvents(this, this);
        saveDefaultConfig(); // Guarda el archivo de configuración predeterminado si no existe
        loadConfigValues(); // Carga los valores de configuración
        // Imprime mensajes en la consola al iniciar el plugin
        Bukkit.getConsoleSender().sendMessage(
                ChatColor.translateAlternateColorCodes('&', prefixPlugin+"&aha sido habilitado correctamente. v"+version));
        Bukkit.getConsoleSender().sendMessage(
                ChatColor.translateAlternateColorCodes('&', prefixPlugin+"&aGracias por usar mi plugin."));
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        // Comando principal del plugin (/pm)
        if (label.equalsIgnoreCase("pm")) {
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                if (sender.hasPermission("pm.reload")) {
                    reloadConfig(); // Recarga la configuración
                    loadConfigValues(); // Carga los valores de configuración
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix+"&aConfiguración recargada correctamente."));
                    return true;
                } else {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix+noPermissionMessage));
                    return false;
                }
            } else {
                if (sender.hasPermission("pm.use")) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', prefix+"&7Estás usando la versión "+version));
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
        // Verifica si el jugador ha mencionado recientemente
        if (mentionCooldowns.containsKey(senderUUID)) {
            long lastMentionTime = mentionCooldowns.get(senderUUID);
            long currentTime = System.currentTimeMillis();
            long elapsedTime = currentTime - lastMentionTime;
            if (elapsedTime < mentionCooldownSeconds * 1000) {
                // Si el tiempo transcurrido es menor que el tiempo de espera, cancela la mención
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
                // Reproduce el sonido al jugador mencionado
                player.playSound(player.getLocation(), getMentionSound(), 1.0f, 1.0f);

                // Cambia el color del nombre del jugador mencionado
                String newMessage = message.replace("@" + playerName, mentionColor + "@" + playerName + ChatColor.RESET);
                event.setMessage(newMessage);

                // Actualiza el tiempo de la última mención para el jugador
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
        // Carga el tiempo de espera de la configuración
        mentionCooldownSeconds = config.getInt("mention_cooldown_seconds", 10);
        // Carga los mensajes de la configuración
        mentionCooldownMessage = config.getString("mention_cooldown_message", "&cDebes esperar %time% segundos entre cada mención.");
        // Carga el mensaje de permiso denegado de la configuración
        noPermissionMessage = config.getString("no_permission_message", "&cNo tienes permiso para ejecutar este comando.");
        // Carga el prefix para los mensajes
        prefix = config.getString("prefix", "&8[&5Player&dMention&8] ");

    }
    private Sound getMentionSound() {
        // Obtiene el sonido de la configuración
        String soundName = getConfig().getString("mention_sound", "ENTITY_PLAYER_LEVELUP");
        Sound sound = Sound.valueOf(soundName);
        if (sound == null) {
            getLogger().warning(prefix+"No se encontró el sonido especificado en la configuración. Usando el sonido predeterminado.");
            sound = Sound.ENTITY_PLAYER_LEVELUP;
        }
        return sound;
    }
}
