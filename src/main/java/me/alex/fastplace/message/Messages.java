package me.alex.fastplace.message;

import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

public final class Messages {
    private final String enabled;
    private final Component disabled;
    private final Component locked;
    private final Component usage;
    private final Component exhausted;

    public Messages(JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), "messages.yml");
        if (!file.exists()) {
            plugin.saveResource("messages.yml", false);
        }
        YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        configuration.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(
                Objects.requireNonNull(plugin.getResource("messages.yml")), StandardCharsets.UTF_8)));
        String prefix = configuration.getString("prefix", "");
        enabled = text(configuration, "enabled", prefix);
        disabled = component(text(configuration, "disabled", prefix));
        locked = component(text(configuration, "locked", prefix));
        usage = component(text(configuration, "usage", prefix));
        exhausted = component(text(configuration, "exhausted", prefix));
    }

    public void enabled(CommandSender sender, int distance) {
        sender.sendMessage(component(enabled.replace("{amount}", Integer.toString(distance))));
    }

    public void disabled(CommandSender sender) {
        sender.sendMessage(disabled);
    }

    public void locked(CommandSender sender) {
        sender.sendMessage(locked);
    }

    public void usage(CommandSender sender) {
        sender.sendMessage(usage);
    }

    public void exhausted(CommandSender sender) {
        sender.sendMessage(exhausted);
    }

    private static String text(YamlConfiguration configuration, String key, String prefix) {
        return configuration.getString(key, "").replace("{prefix}", prefix);
    }

    private static Component component(String message) {
        return LegacyComponentSerializer.legacyAmpersand()
                .deserialize(message.replace("&o", "").replace("&O", "").replace("§o", "").replace("§O", ""))
                .decoration(TextDecoration.ITALIC, false);
    }
}
