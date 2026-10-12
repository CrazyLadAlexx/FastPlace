package me.alex.fastplace.command;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import me.alex.fastplace.message.Messages;
import me.alex.fastplace.api.FastPlaceAPI;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

public final class FastplaceCommand implements TabExecutor {
    private final FastPlaceAPI api;
    private final Messages messages;

    public FastplaceCommand(FastPlaceAPI api, Messages messages) {
        this.api = api;
        this.messages = messages;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] arguments) {
        if (!(sender instanceof Player)) {
            messages.usage(sender);
            return true;
        }
        Player player = (Player) sender;
        int limit = api.getMaximumDistance(player);
        if (limit == 0) {
            api.disable(player);
            messages.locked(player);
            return true;
        }
        if (arguments.length == 0) {
            api.disable(player);
            messages.disabled(player);
            return true;
        }
        if (arguments.length != 1) {
            messages.usage(player);
            return true;
        }
        int distance;
        try {
            distance = Integer.parseInt(arguments[0]);
        } catch (NumberFormatException exception) {
            messages.usage(player);
            return true;
        }
        if (distance < 1 || distance > limit) {
            messages.usage(player);
            return true;
        }
        api.enable(player, distance);
        messages.enabled(player, distance);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] arguments) {
        if (!(sender instanceof Player) || arguments.length != 1) {
            return Collections.emptyList();
        }
        List<String> suggestions = new ArrayList<>();
        int limit = api.getMaximumDistance((Player) sender);
        for (int distance = 1; distance <= limit; distance++) {
            String value = Integer.toString(distance);
            if (value.startsWith(arguments[0])) {
                suggestions.add(value);
            }
        }
        return suggestions;
    }
}
