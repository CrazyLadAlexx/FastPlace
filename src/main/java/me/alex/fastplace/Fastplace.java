package me.alex.fastplace;

import me.alex.fastplace.command.FastplaceCommand;
import me.alex.fastplace.api.FastPlaceAPI;
import me.alex.fastplace.listener.PlacementListener;
import me.alex.fastplace.placement.PlacementService;
import me.alex.fastplace.message.Messages;
import me.alex.fastplace.player.PlayerSettings;
import org.bukkit.command.PluginCommand;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.ServicePriority;

public final class Fastplace extends JavaPlugin {
    private PlacementService placements;
    private FastPlaceAPI api;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        PlayerSettings settings = new PlayerSettings(getConfig().getInt("max-distance", 128));
        Messages messages = new Messages(this);
        placements = new PlacementService(this, settings, messages);
        api = new FastPlaceAPI(settings, placements);
        getServer().getServicesManager().register(FastPlaceAPI.class, api, this, ServicePriority.Normal);
        FastplaceCommand executor = new FastplaceCommand(api, messages);
        PluginCommand command = getCommand("fastplace");
        if (command == null) {
            throw new IllegalStateException("The fastplace command is missing from plugin.yml");
        }
        command.setExecutor(executor);
        command.setTabCompleter(executor);
        getServer().getPluginManager().registerEvents(new PlacementListener(this, settings, placements), this);
    }

    @Override
    public void onDisable() {
        getServer().getServicesManager().unregisterAll(this);
        if (api != null) {
            api.close();
        }
        if (placements != null) {
            placements.close();
        }
        getServer().getScheduler().cancelTasks(this);
    }

    public FastPlaceAPI getAPI() {
        if (api == null || !isEnabled()) {
            throw new IllegalStateException("Fastplace is not enabled");
        }
        return api;
    }
}
