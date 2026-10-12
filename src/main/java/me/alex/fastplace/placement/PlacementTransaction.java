package me.alex.fastplace.placement;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BooleanSupplier;
import me.alex.fastplace.inventory.InventoryPlan;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Hanging;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.entity.TNTPrimed;
import org.bukkit.entity.Vehicle;
import org.bukkit.event.block.BlockCanBuildEvent;
import org.bukkit.event.block.BlockMultiPlaceEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.TNTPrimeEvent;
import org.bukkit.plugin.PluginManager;
import org.bukkit.util.BoundingBox;

public final class PlacementTransaction {
    public record Result(int placed, boolean exhausted) {
    }

    public Result place(Player player, PlacementRequest request, BlockFace direction,
                        PlacementBounds bounds, BooleanSupplier active) {
        boolean creative = request.gameMode() == GameMode.CREATIVE;
        InventoryPlan inventory = creative ? null : new InventoryPlan(player.getInventory(), request.template().item(), request.hand());
        int available = creative ? request.distance() : inventory.available();
        int limit = Math.min(bounds.clamp(request, direction), available);
        if (available == 0) {
            return new Result(0, true);
        }
        Collection<Entity> obstacles = obstacles(request, direction, limit);
        List<List<BlockState>> accepted = new ArrayList<>(limit);
        boolean committed = false;
        boolean blocked = false;
        try {
            for (int distance = 1; distance <= limit; distance++) {
                if (!active.getAsBoolean()) {
                    blocked = true;
                    break;
                }
                List<BlockState> previous = placeOne(player, request, direction, distance, obstacles, active);
                if (previous == null) {
                    blocked = true;
                    break;
                }
                accepted.add(previous);
            }
            int paid = accepted.size();
            boolean shortage = false;
            if (!creative) {
                InventoryPlan.Reservation reservation = inventory.reserve(paid);
                shortage = reservation.amount() < paid;
                paid = reservation.amount();
                for (int index = accepted.size() - 1; index >= paid; index--) {
                    rollback(accepted.remove(index));
                }
                reservation.commit();
            }
            committed = true;
            if (request.template().item().getType() == Material.TNT) {
                primePoweredTnt(accepted);
            }
            boolean exhausted = shortage || (!blocked && available < request.distance() && limit == available);
            return new Result(paid, exhausted);
        } finally {
            if (!committed) {
                for (int index = accepted.size() - 1; index >= 0; index--) {
                    rollback(accepted.get(index));
                }
            }
        }
    }

    private List<BlockState> placeOne(Player player, PlacementRequest request, BlockFace direction, int distance,
                                      Collection<Entity> obstacles, BooleanSupplier active) {
        PlacementTemplate template = request.template();
        List<BlockState> previous = new ArrayList<>(template.parts().size());
        List<BlockData> data = new ArrayList<>(template.parts().size());
        PluginManager plugins = Bukkit.getPluginManager();
        for (PlacementTemplate.Part part : template.parts()) {
            if (!request.loaded(direction, distance, part)) {
                return null;
            }
            Block block = request.block(direction, distance, part);
            if (!replaceable(block) || collides(block, part, obstacles)) {
                return null;
            }
            BlockState old = block.getState();
            BlockData next = part.data(old.getType());
            if (part == template.lowestPart() && !block.canPlace(next)) {
                return null;
            }
            BlockCanBuildEvent build = new BlockCanBuildEvent(block, player, next.clone(), true, request.hand());
            plugins.callEvent(build);
            if (!build.isBuildable() || !active.getAsBoolean()) {
                return null;
            }
            previous.add(old);
            data.add(next);
        }
        boolean accepted = false;
        try {
            for (PlacementTemplate.Part part : template.writeOrder()) {
                int index = template.parts().indexOf(part);
                part.metadata().place(previous.get(index).getBlock(), data.get(index));
            }
            for (int index = 0; index < previous.size(); index++) {
                Block block = previous.get(index).getBlock();
                if (block.getType() != data.get(index).getMaterial() || !block.canPlace(block.getBlockData())) {
                    return null;
                }
            }
            Block placed = previous.getFirst().getBlock();
            Block against = template.groundRow() ? placed.getRelative(BlockFace.DOWN)
                    : placed.getRelative(direction.getOppositeFace());
            BlockPlaceEvent event = previous.size() == 1
                    ? new BlockPlaceEvent(placed, previous.getFirst(), against, template.item().clone(), player, true, request.hand())
                    : new BlockMultiPlaceEvent(previous, against, template.item().clone(), player, true, request.hand());
            plugins.callEvent(event);
            if (event.isCancelled() || !event.canBuild() || !active.getAsBoolean()) {
                return null;
            }
            for (int index = 0; index < previous.size(); index++) {
                Block block = previous.get(index).getBlock();
                if (block.getType() != data.get(index).getMaterial() || !block.canPlace(block.getBlockData())) {
                    return null;
                }
            }
            accepted = true;
            return previous;
        } finally {
            if (!accepted) {
                rollback(previous);
            }
        }
    }

    public static boolean replaceable(Block block) {
        Material material = block.getType();
        return material.isAir() || material == Material.WATER || material == Material.LAVA;
    }

    private Collection<Entity> obstacles(PlacementRequest request, BlockFace direction, int limit) {
        if (limit == 0 || request.template().parts().stream().allMatch(part -> part.collision().isEmpty())) {
            return List.of();
        }
        PlacementTemplate template = request.template();
        int endX = direction.getModX() * limit;
        int endY = direction.getModY() * limit;
        int endZ = direction.getModZ() * limit;
        BoundingBox area = new BoundingBox(request.x() + Math.min(0, endX) + template.minX() - 1,
                request.y() + Math.min(0, endY) + template.minY() - 1,
                request.z() + Math.min(0, endZ) + template.minZ() - 1,
                request.x() + Math.max(0, endX) + template.maxX() + 2,
                request.y() + Math.max(0, endY) + template.maxY() + 2,
                request.z() + Math.max(0, endZ) + template.maxZ() + 2);
        return request.world().getNearbyEntities(area, entity -> !(entity instanceof Player)
                && (entity instanceof LivingEntity || entity instanceof Vehicle || entity instanceof Hanging));
    }

    private boolean collides(Block block, PlacementTemplate.Part part, Collection<Entity> obstacles) {
        for (Entity entity : obstacles) {
            if (!entity.isValid()) {
                continue;
            }
            BoundingBox bounds = entity.getBoundingBox().shift(-block.getX(), -block.getY(), -block.getZ());
            for (BoundingBox shape : part.collision()) {
                if (shape.overlaps(bounds)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static void rollback(List<BlockState> states) {
        for (int index = states.size() - 1; index >= 0; index--) {
            states.get(index).update(true, false);
        }
    }

    private void primePoweredTnt(List<List<BlockState>> placements) {
        for (List<BlockState> placement : placements) {
            Block block = placement.getFirst().getBlock();
            if (block.getType() != Material.TNT || !block.isBlockIndirectlyPowered()) {
                continue;
            }
            TNTPrimeEvent event = new TNTPrimeEvent(block, TNTPrimeEvent.PrimeCause.REDSTONE, null, null);
            Bukkit.getPluginManager().callEvent(event);
            if (event.isCancelled() || block.getType() != Material.TNT) {
                continue;
            }
            var location = block.getLocation().add(0.5, 0.0, 0.5);
            TNTPrimed primed = block.getWorld().spawn(location, TNTPrimed.class,
                    entity -> entity.setBlockData(block.getBlockData()));
            if (primed.isValid()) {
                block.setType(Material.AIR, false);
                block.getWorld().playSound(location, Sound.ENTITY_TNT_PRIMED, 1.0f, 1.0f);
            }
        }
    }
}
