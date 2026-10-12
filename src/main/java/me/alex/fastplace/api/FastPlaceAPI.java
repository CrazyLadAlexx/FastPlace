package me.alex.fastplace.api;

import java.util.Objects;
import me.alex.fastplace.placement.PlacementService;
import me.alex.fastplace.player.PlayerSettings;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public final class FastPlaceAPI {
    private final PlayerSettings settings;
    private final PlacementService placements;
    private boolean active = true;

    public FastPlaceAPI(PlayerSettings settings, PlacementService placements) {
        this.settings = settings;
        this.placements = placements;
    }

    public static FastPlaceAPI getInstance() {
        return Objects.requireNonNull(Bukkit.getServicesManager().load(FastPlaceAPI.class), "Fastplace is not enabled");
    }

    public int getDistance(Player player) {
        checkThread();
        return settings.distance(player);
    }

    public int getMaximumDistance(Player player) {
        checkThread();
        return settings.limit(player);
    }

    public boolean isEnabled(Player player) {
        checkThread();
        int distance = settings.distance(player);
        return distance > 0 && settings.limit(player) >= distance;
    }

    public boolean enable(Player player, int distance) {
        checkThread();
        if (distance < 1 || distance > settings.limit(player)) {
            return false;
        }
        placements.cancel(player.getUniqueId());
        settings.enable(player, distance);
        return true;
    }

    public void disable(Player player) {
        checkThread();
        placements.cancel(player.getUniqueId());
        settings.disable(player.getUniqueId());
    }

    public void close() {
        active = false;
    }

    private void checkThread() {
        if (!active) {
            throw new IllegalStateException("Fastplace is not enabled");
        }
        if (!Bukkit.isPrimaryThread()) {
            throw new IllegalStateException("Use FastPlaceAPI on the server thread");
        }
    }
}
