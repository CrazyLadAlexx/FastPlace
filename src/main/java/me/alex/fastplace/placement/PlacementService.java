package me.alex.fastplace.placement;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import me.alex.fastplace.message.Messages;
import me.alex.fastplace.player.PlayerSettings;
import org.bukkit.Bukkit;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;

public final class PlacementService {
    private final JavaPlugin plugin;
    private final PlayerSettings settings;
    private final Messages messages;
    private final DirectionResolver directions = new DirectionResolver();
    private final PlacementTransaction transaction = new PlacementTransaction();
    private final Map<UUID, PlacementRequest> pending = new LinkedHashMap<>();
    private final long budgetNanos;
    private final int maximumBatches;
    private BukkitTask task;
    private UUID currentPlayer;
    private boolean cancelled;
    private boolean placing;

    public PlacementService(JavaPlugin plugin, PlayerSettings settings, Messages messages) {
        this.plugin = plugin;
        this.settings = settings;
        this.messages = messages;
        double budget = plugin.getConfig().getDouble("tick-budget-millis", 4.0);
        budgetNanos = (long) (Math.max(0.1, Math.min(50.0, budget)) * 1_000_000);
        maximumBatches = Math.max(1, plugin.getConfig().getInt("max-batches-per-tick", 8));
    }

    public boolean isPlacing() {
        return placing;
    }

    public void queue(PlacementRequest request) {
        cancel(request.player());
        pending.put(request.player(), request);
        if (task == null) {
            task = Bukkit.getScheduler().runTaskTimer(plugin, this::pulse, 1L, 1L);
        }
    }

    public void cancel(UUID player) {
        pending.remove(player);
        if (player.equals(currentPlayer)) {
            cancelled = true;
        }
        stopIfIdle();
    }

    public void close() {
        cancelled = true;
        pending.clear();
        stopIfIdle();
        settings.clear();
    }

    private void pulse() {
        long started = System.nanoTime();
        int processed = 0;
        while (!pending.isEmpty() && processed < maximumBatches
                && (processed == 0 || System.nanoTime() - started < budgetNanos)) {
            var iterator = pending.entrySet().iterator();
            PlacementRequest request = iterator.next().getValue();
            iterator.remove();
            processed++;
            Player player = Bukkit.getPlayer(request.player());
            if (player == null || !valid(player, request) || !settings.allows(player, request.distance())) {
                continue;
            }
            currentPlayer = request.player();
            cancelled = false;
            placing = true;
            try {
                PlacementBounds bounds = new PlacementBounds(request.world());
                BlockFace direction = directions.resolve(player, request, bounds);
                PlacementTransaction.Result result = transaction.place(player, request, direction, bounds,
                        () -> !cancelled && settings.distance(player) == request.distance()
                                && player.isOnline() && !player.isDead() && player.getWorld() == request.world()
                                && player.getGameMode() == request.gameMode());
                if (result.exhausted() && player.isOnline()) {
                    messages.exhausted(player);
                }
            } catch (RuntimeException exception) {
                plugin.getLogger().log(Level.SEVERE, "Unable to place a fastplace line", exception);
            } finally {
                placing = false;
                currentPlayer = null;
            }
        }
        stopIfIdle();
    }

    private boolean valid(Player player, PlacementRequest request) {
        if (!player.isOnline() || player.isDead() || player.getWorld() != request.world()
                || player.getGameMode() != request.gameMode() || !request.handUnchanged(player)) {
            return false;
        }
        PlacementTemplate.Part base = request.template().lowestPart();
        if (!request.loaded(BlockFace.SELF, 0, base)
                || request.block(BlockFace.SELF, 0, base).getType() != base.material()) {
            return false;
        }
        ItemStack hand = request.hand() == EquipmentSlot.OFF_HAND
                ? player.getInventory().getItemInOffHand() : player.getInventory().getItemInMainHand();
        return hand.getType().isAir() || hand.isSimilar(request.template().item());
    }

    private void stopIfIdle() {
        if (pending.isEmpty() && task != null) {
            task.cancel();
            task = null;
        }
    }
}
