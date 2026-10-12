package me.alex.fastplace.listener;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import me.alex.fastplace.placement.PlacementRequest;
import me.alex.fastplace.placement.PlacementService;
import me.alex.fastplace.placement.PlacementTemplate;
import me.alex.fastplace.player.PlayerSettings;
import org.bukkit.GameMode;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Event;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerSwapHandItemsEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.plugin.java.JavaPlugin;

public final class PlacementListener implements Listener {
    private final JavaPlugin plugin;
    private final PlayerSettings settings;
    private final PlacementService placements;
    private final Map<UUID, Interaction> interactions = new HashMap<>();

    public PlacementListener(JavaPlugin plugin, PlayerSettings settings, PlacementService placements) {
        this.plugin = plugin;
        this.settings = settings;
        this.placements = placements;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() == Action.RIGHT_CLICK_BLOCK && event.useItemInHand() != Event.Result.DENY
                && settings.distance(event.getPlayer()) > 0
                && !placements.isPlacing()) {
            Interaction interaction = new Interaction(event.getClickedBlock(), event.getBlockFace(), event.getHand(), System.nanoTime());
            UUID player = event.getPlayer().getUniqueId();
            interactions.put(player, interaction);
            plugin.getServer().getScheduler().runTaskLater(plugin, () -> interactions.remove(player, interaction), 2L);
        }
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlace(BlockPlaceEvent event) {
        if (placements.isPlacing()) {
            return;
        }
        Player player = event.getPlayer();
        Interaction interaction = interactions.remove(player.getUniqueId());
        int distance = settings.distance(player);
        if (!event.canBuild() || !settings.allows(player, distance)
                || (player.getGameMode() != GameMode.SURVIVAL && player.getGameMode() != GameMode.CREATIVE)) {
            return;
        }
        BlockFace direction = event.getBlockAgainst().getFace(event.getBlockPlaced());
        if (direction == null || direction == BlockFace.SELF) {
            direction = interaction != null && interaction.matches(event) ? interaction.face : player.getFacing();
        }
        placements.queue(new PlacementRequest(player, event.getBlockPlaced(), distance, event.getHand(),
                direction, PlacementTemplate.capture(event)));
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        UUID player = event.getPlayer().getUniqueId();
        placements.cancel(player);
        settings.disable(player);
        interactions.remove(player);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        stop(event.getEntity());
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        stop(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onHeldChange(PlayerItemHeldEvent event) {
        stop(event.getPlayer());
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onSwap(PlayerSwapHandItemsEvent event) {
        stop(event.getPlayer());
    }

    private void stop(Player player) {
        placements.cancel(player.getUniqueId());
        interactions.remove(player.getUniqueId());
    }

    private static final class Interaction {
        private final Block block;
        private final BlockFace face;
        private final EquipmentSlot hand;
        private final long created;

        private Interaction(Block block, BlockFace face, EquipmentSlot hand, long created) {
            this.block = block;
            this.face = face;
            this.hand = hand;
            this.created = created;
        }

        private boolean matches(BlockPlaceEvent event) {
            return System.nanoTime() - created < 1_000_000_000L && hand == event.getHand()
                    && (block.equals(event.getBlockAgainst()) || block.equals(event.getBlockPlaced()));
        }
    }
}
