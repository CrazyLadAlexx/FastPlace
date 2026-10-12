package me.alex.fastplace.player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.bukkit.entity.Player;

public final class PlayerSettings {
    private static final int[] LIMITS = {128, 64, 32, 16};
    private final Map<UUID, Integer> distances = new HashMap<>();
    private final int maximumDistance;

    public PlayerSettings(int maximumDistance) {
        this.maximumDistance = Math.max(1, Math.min(128, maximumDistance));
    }

    public int limit(Player player) {
        for (int limit : LIMITS) {
            if (player.hasPermission("fastplace." + limit)) {
                return Math.min(limit, maximumDistance);
            }
        }
        return 0;
    }

    public int distance(Player player) {
        return distances.getOrDefault(player.getUniqueId(), 0);
    }

    public boolean allows(Player player, int distance) {
        return distance > 0 && distance(player) == distance && limit(player) >= distance;
    }

    public void enable(Player player, int distance) {
        distances.put(player.getUniqueId(), distance);
    }

    public void disable(UUID player) {
        distances.remove(player);
    }

    public void clear() {
        distances.clear();
    }
}
