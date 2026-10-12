package me.alex.fastplace.placement;

import java.util.UUID;
import org.bukkit.GameMode;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.inventory.EquipmentSlot;

public final class PlacementRequest {
    private final UUID player;
    private final World world;
    private final int x;
    private final int y;
    private final int z;
    private final int distance;
    private final int heldSlot;
    private final EquipmentSlot hand;
    private final GameMode gameMode;
    private final BlockFace direction;
    private final PlacementTemplate template;

    public PlacementRequest(Player player, Block origin, int distance, EquipmentSlot hand,
                            BlockFace direction, PlacementTemplate template) {
        this.player = player.getUniqueId();
        this.world = origin.getWorld();
        this.x = origin.getX();
        this.y = origin.getY();
        this.z = origin.getZ();
        this.distance = distance;
        this.heldSlot = player.getInventory().getHeldItemSlot();
        this.hand = hand;
        this.gameMode = player.getGameMode();
        this.direction = direction;
        this.template = template;
    }

    public UUID player() {
        return player;
    }

    public int x() { return x; }
    public int y() { return y; }
    public int z() { return z; }

    public World world() {
        return world;
    }

    public int distance() {
        return distance;
    }

    public EquipmentSlot hand() {
        return hand;
    }

    public GameMode gameMode() {
        return gameMode;
    }

    public boolean handUnchanged(Player player) {
        return hand == EquipmentSlot.OFF_HAND || player.getInventory().getHeldItemSlot() == heldSlot;
    }

    public BlockFace direction() {
        return direction;
    }

    public PlacementTemplate template() {
        return template;
    }

    public boolean loaded(BlockFace direction, int distance, PlacementTemplate.Part part) {
        int targetX = x + direction.getModX() * distance + part.x();
        int targetY = y + direction.getModY() * distance + part.y();
        int targetZ = z + direction.getModZ() * distance + part.z();
        return targetY >= world.getMinHeight() && targetY < world.getMaxHeight()
                && world.isChunkLoaded(targetX >> 4, targetZ >> 4);
    }

    public Block block(BlockFace direction, int distance, PlacementTemplate.Part part) {
        return world.getBlockAt(x + direction.getModX() * distance + part.x(),
                y + direction.getModY() * distance + part.y(), z + direction.getModZ() * distance + part.z());
    }

    public Block origin() {
        return world.getBlockAt(x, y, z);
    }
}
